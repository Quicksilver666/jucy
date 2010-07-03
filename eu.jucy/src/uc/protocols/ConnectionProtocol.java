package uc.protocols;





import helpers.GH;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.io.IOException;


import logger.LoggerFactory;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.Platform;


/** 
 * Base class for all Connection Protocols. Used to make implementation of 
 * plain text protocols easier
 * 
 * Does most of the hard work specially provides a nice interface a connection can call.
 * 
 * Clients should extend this to present the protocol unspecific entity. like a hub or a client
 * 
 * most protocol depending stuff could go into implementations of IProtocolCommand objects..
 * 
 * 
 * */
public abstract class ConnectionProtocol implements ReadWriteLock {

	private static Logger logger = LoggerFactory.make(); 
	
	
//	private static final ProtocolTimer mt = new ProtocolTimer();
	private final ReentrantReadWriteLock rwLock;
	

	protected volatile IConnection connection;   //Needs to be set soon... bad thing.. really..

	protected int defaultPort;
	
	private final Object charsetSynch = new Object();
	private Charset charset; 
	private static final int SOCKET_TIMEOUT = 40000; 

	private static final Map<InetAddress,IConnectionDebugger> AUTO_ATTACH= 
		Collections.synchronizedMap(new HashMap<InetAddress,IConnectionDebugger>());
	
	public static void addNotifyAttachable(InetAddress ia,IConnectionDebugger debug) {
		AUTO_ATTACH.put(ia, debug);
	}
	
	public static void removeNotifyAttachable(InetAddress ia) {
		AUTO_ATTACH.remove(ia);
	}

	private final CopyOnWriteArraySet<IConnectionDebugger> debuggers = 
		new CopyOnWriteArraySet<IConnectionDebugger>();

	private volatile boolean loginDone = false;
	



	private volatile boolean pendingDestroyed = false;


	/**
	 * maps prefixes of the commands to the commands
	 */
	protected Map<String,IProtocolCommand> commands =
				new HashMap<String,IProtocolCommand>();
	/**
 	* default command used when no prefix can be determined..
 	*/
	protected IProtocolCommand defaultCommand; 

	/**
	 * a pattern that has the prefix of a command as first capture
	 * if it not matches.. some default command is called..
	 * default value is NMDC
	 */
	protected volatile Pattern prefix = Pattern.compile("(\\$\\S+)[^|]*");

	private final Object csSynch = new Object();
	/**
	 * the current state of the protocol
	 */
	private ConnectionState state = ConnectionState.CONNECTING ; 

	private volatile long lastLogin = 0; 

	private final int[] performancePrefs;
	
	/**
	 * store for unfinished commands..
	 */
	private final StringBuffer stringbuffer = new StringBuffer();

	public ConnectionProtocol(AbstractConnection a) {
		this(a,null);
	}
	
	/**
	 * @param a which connectionProtocol is used with this connection..
	 * @param performancePrefs @see {@link Socket#setPerformancePreferences(int, int, int)}
	 *  the array must be of length 3 and specify performance preferences
	 *  as the Socket does... 
	 */
	public ConnectionProtocol(AbstractConnection a,int[] performancePrefs) {
		this(performancePrefs);
		this.connection = a;
	}
	
	/**
	 * 
	 * @param performancePrefs - @see {@link Socket#setPerformancePreferences(int, int, int)}
	 *  the array must be of length 3 and specify performance preferences
	 *  as the Socket does... 
	 */
	public ConnectionProtocol(int[] performancePrefs){
		if (performancePrefs != null && performancePrefs.length != 3) {
			throw new IllegalArgumentException();
		}
		this.performancePrefs = performancePrefs;
		this.rwLock = new ReentrantReadWriteLock();
	}
	public ConnectionProtocol() {
		this((int[])null);
	}
	
	/**
	 *  be called at the beginning.. when the protocol starts
	 *
	 */
	public void start() {
		//mt.registerCP(this);
	}
	
//	/**
//	 * 
//	 * @return true if is registered with timer...
//	 * and will register it again if not as sideeffect
//	 */
//	public boolean debugIsRegistered() {
//		return !mt.registerCP(this);
//	}
	
	public void beforeConnect() {
		stringbuffer.delete(0, stringbuffer.length()); //clear
		loginDone = false;
		setState(ConnectionState.CONNECTING);
	}
	
	
	/**
	 * retrieves a pattern needed to get capture a command
	 * first capture group must be the part of the command
	 * that is provided to receivedCommand
	 * while the whole pattern should match the whole command
	 * @see{NMDCProtocol.java} for the implementation of this
	 */
	public abstract Pattern getCommandRegexPattern(); 
	
	/**
	 * 
	 * @return the byte after which command processing shall be stopped..
	 */
	public abstract int getCommandStopByte();
	
	// These 3 Methods have to be overridden by each subclass of protocol
	
	/**
	 * OnConnect is Called once by the connection after Socket is connected..
	 * to the other side
	 *  overwriting methods must  call super.onConnect()
	 */
	public void onConnect() throws IOException {
		setState(ConnectionState.CONNECTED);
		InetAddress ia = connection.getInetSocketAddress().getAddress();
		IConnectionDebugger debug = AUTO_ATTACH.get(ia);
		if (debug != null) {
			debug.notifyAttachable(ia, this);
		}
	}
	
	/**
	 * Function is called by the protocol!! when login is done..
	 * this sets the login done flag so future commands go to commandReceived()
	 * instead of commandReceivedDuringLogin()
	 */
	public void onLogIn() throws IOException {
		synchronized(csSynch) {
			//because sometimes the connection gets closed during login -> check
			if (state != ConnectionState.CONNECTED) { 
				if (Platform.inDevelopmentMode() && ConnectionState.DESTROYED != state) {
					logger.warn("Bad state: "+state+"  "+toString(),new Throwable());
				}
				return;
			}
		
			loginDone = true;
			lastLogin = System.currentTimeMillis();
			setState(ConnectionState.LOGGEDIN);
		}
	}
	
	
	void receivedCommand(byte[] command) throws IOException, ProtocolException {
		String s = getCharset().decode(ByteBuffer.wrap(command)).toString();
		stringbuffer.append(s);
		Matcher m = null;
		while ((m = getCommandRegexPattern().matcher(stringbuffer)).find()) {
			String found = m.group(1);
			//	logger.debug("command: "+found);
			if (!GH.isNullOrEmpty(found)) {
				stringbuffer.delete(0, m.end());
				receivedCommand(found);
			}  else {
				stringbuffer.deleteCharAt(0);
				break;
			}
		}
	}


	/**
	 * called for each command that is received..
	 * 
	 * @param command  - the command as provided by the CP's set Regexp
	 * @throws IOException - especially needed when writing back .. so this can be caught
	 * by the Connection
	 * @throws ProtocolException -
	 *  
	 */
	public void receivedCommand(String command) throws IOException, ProtocolException {
		
		Matcher m = prefix.matcher(command);
		IProtocolCommand com = null;
		if (m.matches()) {
			String prefix = m.group(1);
			com = commands.get(prefix);
		} else if (defaultCommand != null){
			com = defaultCommand;
		}
		boolean matches = false;

		if (com != null ) {
			matches = com.matches(command);
		}
		
		for (IConnectionDebugger debugger:debuggers) {
			debugger.receivedCommand(com, matches, command);
		}
		
		if (com != null) {
		//	logger.debug("command found "+com.getPrefix());
			if (matches) {
				com.handle(command);
			} else {
				onMalformedCommandReceived(command);
			}
		} else {
			onUnexpectedCommandReceived(command);
		}

		
	}
	
	protected void onUnexpectedCommandReceived(String command) {
		logger.debug("Unexpected command received: "+command+"   in "+connection.getInetSocketAddress());  
	}
	
	protected void onMalformedCommandReceived(String command) {
		logger.debug("Malformed Command received: "+command+"   in "+connection.getInetSocketAddress()); 
	}
	
	/**
	 * Called when Socket is closed
	 * overwriting methods must make sure to
	 * to call super.onDisconnect() 
	 * 
	 * @throws IOException
	 */
	public void onDisconnect() throws IOException {
		//mt.deregisterCP(this);
		setState(ConnectionState.CLOSED);
	}
	
	
	/**
	 *  a timer for every protocol
	 *  it is called every second by a global timer
	 */
	public final void timer() {
	}
	
	/**
	 * sends a raw without doing any tinkering 
	 * like formatting by context..
	 * @param mes - the message to be sent..
	 */
	protected void sendRaw(String mes) {
		for (IConnectionDebugger debugger:debuggers) {
			debugger.sentCommand(mes);
		}
		
		ByteBuffer b = getCharset().encode(mes);
		
		try {
			connection.send(b);
	//		logger.debug("sent raw: "+mes);
		} catch(IOException ioe) {
			logger.warn(ioe,ioe); 
		}
	}
	
	protected void sendRaw(byte[] mes) {

		for (IConnectionDebugger debugger:debuggers) {
			debugger.sentCommand(getCharset().decode(ByteBuffer.wrap(mes)).toString());
		}

		try {
			connection.send(ByteBuffer.wrap(mes));
		//	logger.debug("sent raw: "+new String(mes));
		} catch(IOException ioe) {
			logger.warn(ioe,ioe); 
		}
	}
	
	
	
	/**
	 * @param addy - takes a  ip:port    string and gives a socketpair back..
	 * @param defaultport - used if no :  is found in the addy string 
	 */
	public static InetSocketAddress inetFromString(String addy, int defaultport) {
		int i	= addy.lastIndexOf(':');
		int port = defaultport;
		String onlyaddy = addy;
		if (i != -1) {
			port = new Integer(addy.substring(i+1));
			onlyaddy= addy.substring(0, i);
		}
		return new InetSocketAddress(onlyaddy,port);
	}

	
	
	private final CopyOnWriteArrayList<IProtocolStatusChangedListener> cscl = 
		new CopyOnWriteArrayList<IProtocolStatusChangedListener>();
	
	public void registerProtocolStatusListener(IProtocolStatusChangedListener listener) {
		if (listener == null && Platform.inDevelopmentMode()) { 
			logger.warn("registered null", new Throwable());
		}
		cscl.addIfAbsent(listener);
	}
	
	/**
	 * adds an element to the beginning of the list .. so it is notified before others..
	 * @param listener - a high priority listener.
	 */
	protected void registerListenerFirst(IProtocolStatusChangedListener listener) {
		if (listener == null) {
			logger.warn("registered null", new Throwable());
		}
		cscl.remove(listener);
		cscl.add(0, listener);
	}
	
	public void unregisterProtocolStatusListener(IProtocolStatusChangedListener listener){
		cscl.remove(listener);
	}
	
	protected void setState(ConnectionState state) {
		synchronized(csSynch) {
			if (this.state == ConnectionState.DESTROYED) {
				throw new IllegalStateException("State was already destroyed "+this);
			}
			this.state = state;
			

	
			for (IProtocolStatusChangedListener listener: cscl) {
				if (listener == null) {
					logger.warn("found Listener null", new Throwable());
				} else {
					listener.statusChanged(state, this);
				}
			}
			
			if (pendingDestroyed && this.state ==  ConnectionState.CLOSED) {
				setState(ConnectionState.DESTROYED);
			}
		}
	}
	
	/**
	 * sets state of the connection Destroyed..
	 * kind of like dispose() in swt.. this is needed so timer is no longer
	 * called and the Connection Protocol can be garbage Collected..
	 * but does nothing..
	 * 
	 *  this method will immediately post an Destroyed event to setState()
	 *  if the Connection is closed..
	 *  or send it as soon as the connection is Closed..
	 */
	public void end() {
		synchronized (csSynch) {	
			if (state == ConnectionState.CLOSED) {
				setState(ConnectionState.DESTROYED);
			} else {
				pendingDestroyed = true;
			}
		}
	}
	
	/**
	 * 
	 * @return the current state of the protocol..
	 */
	public ConnectionState getState() {
		synchronized (csSynch) {
			return state;
		}
	}


	public void clearCommands() {
		commands.clear();
	}
	
	public void addCommand(IProtocolCommand... command) {
		for (IProtocolCommand com: command ) {
			commands.put(com.getPrefix(), com);
		}
	}
	
	public void removeCommand(IProtocolCommand command) {
		commands.remove(command.getPrefix());
	}

	protected void setPrefix(Pattern prefix) {
		this.prefix = prefix;
	}

	public Charset getCharset() {
		synchronized (charsetSynch) {
			return charset;
		}
	}
	
	protected void setCharset(Charset cs) {
		this.charset = cs;
	}

	public int getSocketTimeout() {
		return SOCKET_TIMEOUT;	
	}
	/**
	 * whether this ClientProtocol is encrypted..
	 * @return
	 */
	public boolean isEncrypted() {
		return connection.usesEncryption();
	}

	/**
	 * 
	 * @return true if KEYP is in use for this connection
	 */
	public boolean isFingerPrintUsed() {
		return connection.isFingerPrintUsed();
	}
	
	int[] getPerformancePrefs() {
		return performancePrefs;
	}

	/**
	 * 
	 * @return the connection this ConnectionProtocol is associated with
	 */
	public IConnection getConnection() {
		return connection;
	}

	public boolean isLoginDone() {
		return loginDone;
	}

	public long getLastLogin() {
		return lastLogin;
	}
	
	/**
	 * @param conDebug - register Debugger that then gets notified of connections
	 */
	public void registerDebugger(IConnectionDebugger conDebug) {
		debuggers.add(conDebug);
		registerProtocolStatusListener(conDebug);
	}
	/**
	 * @param conDebug - unregister Debugger that then gets notified of connections
	 */
	public void unregisterDebugger(IConnectionDebugger conDebug) {
		debuggers.remove(conDebug);
		unregisterProtocolStatusListener(conDebug);
	}

	public WriteLock writeLock() {
		return rwLock.writeLock();
	}
	
	

	public ReadLock readLock() {
		return rwLock.readLock();
	}
	
	
	
	
}


