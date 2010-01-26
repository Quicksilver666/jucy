package uc.protocols;





import java.util.HashMap;
import java.util.Map;

import java.util.concurrent.CopyOnWriteArrayList;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.Socket;
import java.nio.charset.Charset;
import java.io.IOException;


import logger.LoggerFactory;

import org.apache.log4j.Level;
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
 * */
public abstract class ConnectionProtocol {

	private static Logger logger = LoggerFactory.make(Level.DEBUG); 
	
	
	private static final ProtocolTimer mt = new ProtocolTimer();

	

	protected volatile IConnection connection;   //Needs to be set soon... bad thing.. really..

	protected int defaultPort;
	protected Charset charset; 
	private int socketTimeout = 40000; 


	

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

	/**
	 * the current state of the protocol
	 */
	protected volatile ConnectionState state = ConnectionState.CONNECTING ; 

	private volatile long lastLogin;





	private final int[] performancePrefs;
	


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
	}
	public ConnectionProtocol() {
		this((int[])null);
	}
	
	/**
	 * must be called at the beginning.. when the protocol starts
	 * overwriting method must call super (though this implementation only registers the timer)
	 * so if the timer is unwanted not calling this is possible
	 */
	public void start() {
		mt.registerCP(this);
	}
	
	/**
	 * 
	 * @return true if is registered with timer...
	 * and will register it again if not as sideeffect
	 */
	public boolean debugIsRegistered() {
		return !mt.registerCP(this);
	}
	
	public void beforeConnect() {
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
	public void onConnect() throws IOException{
		setState(ConnectionState.CONNECTED);
	}
	
	/**
	 * Function is called by the protocol!! when login is done..
	 * this sets the login done flag so future commands go to commandReceived()
	 * instead of commandReceivedDuringLogin()
	 */
	public void onLogIn() throws IOException {
		if (state != ConnectionState.CONNECTED) {
			if (Platform.inDevelopmentMode()) {
				logger.info("Bad state: "+state);
			}
			return;
		}
		loginDone = true;
		lastLogin = System.currentTimeMillis();
		setState(ConnectionState.LOGGEDIN);
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
		if (m.matches()) {
			String prefix = m.group(1);
			IProtocolCommand com = commands.get(prefix);
			if (com != null) {
			//	logger.debug("command found "+com.getPrefix());
				if (com.matches(command)) {
					com.handle(command);
				} else {
					onMalformedCommandReceived(command);
	
					//throw new ProtocolException("Malformed Command received: "+command); better ignore malformed commands... instead of throwing exceptions
				}
			} else {
				onUnexpectedCommandReceived(command);
			}
			
		} else {

			//if no prefix is determined.. use a default command..
			if (defaultCommand != null && defaultCommand.matches(command)) {
				defaultCommand.handle(command);
			}
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
	public void timer() {
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
		if (this.state == ConnectionState.DESTROYED) {
			throw new IllegalStateException("State was already destroyed");
		}
		this.state = state;
		
		if (state == ConnectionState.DESTROYED) {
			mt.deregisterCP(this);
		}

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
		if (this.state == ConnectionState.CLOSED) {
			setState(ConnectionState.DESTROYED);
		} else {
			pendingDestroyed = true;
		}
	}
	
	/**
	 * 
	 * @return the current state of the protocol..
	 */
	public ConnectionState getState() {
		return state;
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
		return charset;
	}

	public int getSocketTimeout() {
		return socketTimeout;
	}
	/**
	 * whether this ClientProtocol is encrypted..
	 * @return
	 */
	public boolean isEncrypted() {
		return connection.usesEncryption();
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
}


