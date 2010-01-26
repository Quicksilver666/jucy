package uc.protocols.hub;



import helpers.GH;

import java.security.GeneralSecurityException;
import java.util.Collections;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.nio.channels.UnresolvedAddressException;
import java.nio.charset.Charset;


import logger.LoggerFactory;


import org.apache.log4j.Logger;
import org.eclipse.core.runtime.Platform;



import eu.jucy.language.LanguageKeys;


import uc.Command;
import uc.DCClient;
import uc.FavHub;
import uc.IHub;
import uc.IOperatorPlugin;
import uc.IUser;
import uc.InfoChange;
import uc.PI;
import uc.User;
import uc.IUser.Mode;
import uc.crypto.HashValue;
import uc.crypto.UDPEncryption;
import uc.database.DBLogger;
import uc.files.filelist.FileListDescriptor;
import uc.files.filelist.FileListFile;
import uc.files.filelist.FileListFolder;
import uc.files.filelist.IFileListItem;
import uc.files.filelist.OwnFileList.SearchParameter;
import uc.files.search.FileSearch;
import uc.files.search.SearchResult;


import uc.listener.IPMReceivedListener;
import uc.listener.IUserChangedListener;
import uc.listener.IUserChangedListener.UserChangeEvent;
import uc.protocols.ADCStatusMessage;
import uc.protocols.AbstractConnection;
import uc.protocols.CPType;
import uc.protocols.Compression;
import uc.protocols.ConnectionProtocol;
import uc.protocols.ConnectionState;
import uc.protocols.IConnection;
import uc.protocols.SendContext;
import uc.protocols.UnblockingConnection;

import uc.protocols.DCProtocol;
import uc.protocols.hub.IFeedListener.FeedType;


public class Hub extends DCProtocol implements IHub {
	
	private static Logger logger = LoggerFactory.make();
	
	private static ConnectionInjector inject = new ConnectionInjector();
	
	public static void setConnectionInjector(ConnectionInjector ci) {
		inject = ci;
	}
	
	/**
	 * 
	 * Used for inserting different connections for Unit-tests..
	 *
	 */
	public static class ConnectionInjector {
		public IConnection getConnection(String addy, ConnectionProtocol connectionProt,boolean encryption,HashValue fingerPrint) {
			return new UnblockingConnection(addy,connectionProt, encryption,fingerPrint);
		}
	}
	
	public static final int 	MaxActiveResults = 10, 
								MaxPassiveResults = 5;
	
	private DBLogger mcLoggerDB; 
	private DBLogger feedLoggerDB;

	
	private static enum ProtocolPrefix {
		DCHUB(false,true),NMDC(false,true),
		NMDCS(true,true),DCHUBS(true,true),
		ADC(false,false),ADCS(true,false);
		
		ProtocolPrefix(boolean encrypted,boolean nmdc) {
			this.encrypted = encrypted;
			this.nmdc = nmdc;
		}
		
		public static boolean matchesPrefix(String address) {
			int index = address.indexOf("://");
			if (index >= 0) {
				String prefix  = address.substring(0, index).toUpperCase();
				try {
					ProtocolPrefix p = valueOf(prefix);
					return p != null;
				} catch (Exception e) {}
			}
			return false;
		}
		
		public static ProtocolPrefix parse(String whole) {
			int index = whole.indexOf("://");
			if (index >= 0) {
				String prefix = whole.substring(0, index).toUpperCase();
				logger.debug("prefix: "+prefix);
				try {
					ProtocolPrefix p = valueOf(prefix);
					if (p != null) {
						return p;
					}
				} catch (Exception e) {
					logger.debug(e,e);
				}
			}
			return DCHUB;
		}
		
		private final boolean encrypted,nmdc; 
	}
	

	
	private final User self;
	
	/**
	 * token used to create this hub..
	 */
	private final FavHub favHub;
	
	
	private volatile String hubaddy;
	private volatile HashValue fingerPrint;
	
	/**
	 * when a client receives signal for redirection
	 * the received address is stored here.
	 */
	private volatile String redirectaddy;
	
	/**
	 * contains only the hubs name .. not the topic
	 */
	private String hubname	=	"";
	
	private final Set<String> othersSupports = new HashSet<String>(); // The SupportString of the other side 
	
	/**
	 * string containing only the name .. if possible not the whole topic
	 */
	private String topic = "";
	
	/**
	 * summed share over all users in the hub..
	 * this is manipulated by user.setShared();
	 * never directly..
	 */
	private volatile long totalshare = 0;


	/**
	 * true if we needed to sent a password..
	 */
	private volatile boolean registered = false;
	
	/**
	 * on each startup request userip if hub supports
	 * it and hasn't sent it to us
	 */
	private volatile boolean userIPReceived = false;
	
	
	/**
	 * if we connected at least once to the hub ..
	 * this will be used so we can't 
	 * be used for DDoS  
	 * if we don't connect at least once
	 * 
	 * set to True when a Lock is received..
	 */
	private volatile boolean onceConnected = false;
	
	private volatile boolean weWantToReconnect = true;  // Variable that tells if the client should automatically reconnect... i.e. set false on badpass or kick..
	private volatile boolean reconnectRunning = false;
	private volatile long waitTime = 10+ GH.nextInt(60); //how long to wait until reconnect
	private volatile int unsuccessfulConnectionsInARow = 0;
	
	private final Object loginTimeoutSynch = new Object();
	private int timerLogintimeout;
	
	private static final long noCommandReceivedTimeout = 45*1000;
	private final Object lastReceivedSynch = new Object();
	private long lastCommandReceived;
	
	{
		synchronized (lastReceivedSynch) {
			lastCommandReceived = System.currentTimeMillis()+45 * 1000 ;
		}
	}
	

	private final Map<HashValue,User> users = new ConcurrentHashMap<HashValue,User>(20,0.75f,2); //userid to User

	private final Map<Integer,User> userBySid =  new ConcurrentHashMap<Integer,User>(20,0.75f,2);  //SID to user
	
	private List<IOperatorPlugin> opPlugins = null;

	
	/**
	 *all usercomamnds
	 */
	private final List<Command> userCommands = new ArrayList<Command>();
	
	/**
	 * map needed for completion proposals
	 */
	private final SortedMap<String,IUser> userPrefix = Collections.synchronizedSortedMap(new TreeMap<String,IUser>());

	
	private final Map<IUser,Date> lastAwaySent = new WeakHashMap<IUser,Date>(); 
	
	private final DCClient dcc;

	

	public DCClient getDcc() {
		return dcc;
	}


	public User getUser(HashValue id){
		return users.get(id) ;
	}

	
	/**
	 * the new constructor
	 * @param favHub
	 * @param a
	 */
	public Hub(FavHub favHuba,DCClient dccx) throws IllegalArgumentException{
		super();
		this.dcc = dccx;
		defaultPort = 411;
		
		this.favHub = favHuba;
		
		String nickname = favHub.getNick() != null && !GH.isEmpty(favHub.getNick()) ? favHub.getNick() : PI.get(PI.nick); 
	
		setHubaddy(favHub.getHubaddy());
		
		
		logger.debug("creating hub: "+hubaddy);

		
		/*
		 * create a user that represents our self therefore 
		 * some methods must be overwritten
		 *  so this user proxy works properly
		 *  
		 */
		self = new User(dcc,nickname,nmdc?nickToUserID(nickname,this ): CIDToUserID(dcc.getPID().hashOfHash(), favHub) ){

			@Override
			public HashValue getCID() { 
				return getPD().hashOfHash();
			}

			@Override
			public String getConnection() {
				return dcc.getConnection();
			}

			@Override
			public String getDescription() {
				if (GH.isEmpty(favHub.getUserDescription())) {
					return PI.get(PI.description);
				} else {
					return favHub.getUserDescription();
				}
			}

			@Override
			public String getEMail() {
				if (GH.isEmpty(favHub.getEmail())) {
					return PI.get(PI.eMail);
				} else {
					return favHub.getEmail();
				}
			}

			@Override
			public InetAddress getIp() {
				return dcc.getConnectionDeterminator().getPublicIP();
			}
			

			@Override
			public long getShared() {
				return favHub.isChatOnly() ? 0L :dcc.getFilelist().getSharesize();
			}

			@Override
			public int getSlots() {
				return dcc.getTotalSlots();
			}

			@Override
			public String getTag() { //only used for NMDC..
				return dcc.getTag();
			}
			
			@Override
			public void setSid(int sid) {
				userBySid.remove(this.getSid());
				super.setSid(sid);
				userBySid.put(sid, this);
			}

			@Override
			public Mode getModechar() {
				return dcc.getMode();
			}

			@Override
			public int getNormHubs() {
				return dcc.getNumberOfHubs(false)[0];
			}

			@Override
			public int getRegHubs() {
				return dcc.getNumberOfHubs(false)[1];
			}
			
			@Override
			public int getOpHubs() {
				return dcc.getNumberOfHubs(false)[2];
			}

			@Override
			public HashValue getPD() {
				return dcc.getPID(); 
			}


			@Override
			public String getVersion() {
				return DCClient.VERSION;
			}

			@Override
			public String getSupports() {
				List<String> sup = new ArrayList<String>();
				if (dcc.isActive()) {
					if (dcc.isIPv4Used()) {
						sup.add(User.TCP4);
						sup.add(User.UDP4);
					} else {
						sup.add(User.TCP6);
						sup.add(User.UDP6);
					}
				}
				if (dcc.currentlyTLSSupport()) {
					sup.add(User.ADCS_SUPPORT);
					if (AbstractConnection.getFingerPrint() != null) {
						sup.add(User.KEYP);
					}
				}
				if (UDPEncryption.isUDPEncryptionSupported() && connection.usesEncryption()) {
					sup.add(User.ADCS_UDP);
				}

				return GH.concat(sup, ",", "");
			}

			@Override
			public int getNumberOfSharedFiles() {
				return favHub.isChatOnly()? 
					0 : dcc.getFilelist().getNumberOfFiles();
			}
			
			
			@Override
			public int getUdpPort() {
				return dcc.getUdphandler().getPort();
			}

			@Override
			public long getUs() { 
				//UP Speed ... as is in settings..
				long speedLimit = PI.getInt(PI.uploadLimit)*1024;
				if (speedLimit == 0) {
					return PI.getLong(PI.connectionNew);
				}
				return speedLimit; 
			}

			/**
		     * Flag indicates our state .. 
		     * AFK which has a value of 2   or 3 for long time AFK .. 1 is normal
		     * 
		     * @return always 1 in normal state.. 2 in away state..
		     * add 16 if tls support for nmdc is enabled..
		     * 
		     */
		    public byte getFlag() {
		    	int flag = dcc.isAway()? 2:1; 
		    	flag += dcc.currentlyTLSSupport()? User.FLAG_ENC :0;
		    	return (byte)flag;
		    }

			@Override
			public synchronized FileListDescriptor getFilelistDescriptor() {
				return dcc.getFilelistself().getFilelistDescriptor();
			}
			
			@Override
			public boolean hasDownloadedFilelist() {
				return true;
			}

			//override needed so listeners are called
		/*	@Override
			public void setCt(byte ct) {
				super.setCt(ct);
				setWeAreOp(this.isOp());
			}*/
			


			//Override needed so listeners are called
			/*@Override
			public void setOp(boolean op) {
				super.setOp(op);
				setWeAreOp(this.isOp());
			} */

			@Override
			public void setProperty(INFField inf, String val)throws  NumberFormatException,IllegalArgumentException {
				super.setProperty(inf, val);
				
				switch(inf) {
				case CT: setWeAreOp(super.isOp()); break;
				case I4: 
				case I6: 
					dcc.getConnectionDeterminator().userIPReceived(super.getIp(), favHub); //super call important -> otherwise it gets the IP from COnnection determinator..
					userIPReceived = true;
					break;
				}
			}

			@Override
			public HashValue getKeyPrint() {
				return AbstractConnection.getFingerPrint();
			}
		    
		};
		
		connection = inject.getConnection(hubaddy, this, ProtocolPrefix.parse(favHub.getHubaddy()).encrypted,fingerPrint);
		//connection = new UnblockingConnection(hubaddy,this, ProtocolPrefix.parse(favHub.getHubaddy()).encrypted);
	
	}
	
	public synchronized void start() {
		super.start();
		try {
			connection.start();
		} catch(UnresolvedAddressException uae) {
			statusMessage(LanguageKeys.AddressCouldNotBeResolved,0); 
		}
	}
	
	/**
	 * @return - not the full "topic" string .. only the name
	 */
	public String getName() {
		return hubname;
	}

	public void beforeConnect() {
		logger.debug("beforeConnect()");
		topic = ""; //delete topic
		registered = false;
		
		
		self.setProperty(INFField.CT, ""); //delete our OP status...

	
		userIPReceived = false;

		synchronized(loginTimeoutSynch) {
			timerLogintimeout = 0;
		}
		userCommands.clear();
		
		super.beforeConnect();
		clearCommands();
		if (nmdc) {
			addCommand(new Lock(this)); 
		} else {
			addCommand(new SUP(this),new SID(this),new INF(this),
					new MSG(this),new STA(this),new GPA(this),
					new CMD(this),new GET(this));
		}
	
		
//		statusMessage(String.format(LanguageKeys.ConnectingTo,hubaddy)+ 
//				(Platform.inDevelopmentMode()&& connection.getInetSocketAddress() != null
//						?" "+connection.getInetSocketAddress().getAddress().getHostAddress()
//								:""),0); //should be in gui...
		
		logger.debug("end beforeConnect()");
		
	}
	
	public void onConnect() throws IOException { //not needed... in NMDC Client hub protocol...
		logger.debug("onConnect()");
		super.onConnect();
	//	statusMessage(LanguageKeys.Connected,0);
		unsuccessfulConnectionsInARow = 0;
		
		if (!nmdc) { //in ADC clients sends the first command not the hub
			SUP.sendSupports(this);
		}
	}

	protected void onUnexpectedCommandReceived(String command) {
		if (!getFavHub().isChatOnly()) {
			super.onUnexpectedCommandReceived(command);
		}
	}
	
	/**
	 * 
	 * @param usr the user that sent the message.. null if undetermined
	 * @param message - the message.. if user is not null the <nick> is striped off
	 */
	void mcMessageReceived(User usr, String message, boolean me) {
		
		if (!me && usr != null) {
			for (IMCReceivedListener listener : hubl) {
				listener.mcReceived(usr, message);
			}
		} else {
			if (usr != null) { //ADC version ...has to do this on its own..
				message = "*"+usr.getNick()+" "+message+"*";
			}
			for (IMCReceivedListener listener : hubl) {
				listener.mcReceived( message);
			}
		}

		logMainchatMessage(usr,message);
	}
	
	private void logMainchatMessage(final User usr,final String message) {
		if (PI.getBoolean(PI.logMainChat)) {
			if (mcLoggerDB == null) {
				mcLoggerDB = new DBLogger(favHub, true);
			}
			String mes = (usr != null ? "<" + usr.getNick()+ "> " : "")+ message;
			mcLoggerDB.addLogEntry(mes, System.currentTimeMillis());
		}
	}
	
	/**
	 * pumps status messages to main chat..
	 * may be this should be done with a logger.
	 * @param message
	 */
	public void statusMessage(String message, int severity) {
		for (IMCReceivedListener listener : hubl) {
			listener.statusMessage( message , severity);
		}
	}
	
	@Override
	public void onDisconnect() throws IOException {
		super.onDisconnect();
		disconnectAllUsers();
	
        if (dcc.isRunningHub(favHub) && checkReconnect()) { //only reconnect if reconnect was requested by user.. or we already connected once... for security..
        	logger.debug("scheduling reconnect");
        	reconnectRunning = true;
        }
	}
	
	private boolean checkReconnect() {
		return weWantToReconnect && (onceConnected|| waitTime <= 0 || favHub.getHubaddy().endsWith(hubaddy) );
	}
	
	
	
	public synchronized void increaseSharesize(long difference) {
		totalshare	+= difference;
	}
	
	public void onLogIn() throws IOException {
		super.onLogIn();
		//now information if we are registered or operator may have changed
		dcc.notifyChangedInfo(InfoChange.Hubs); 
		DCClient.getScheduler().schedule(
		new Runnable() {
			public void run() {
				logger.debug("requesting userip1");
				if (!userIPReceived && dcc.isActive() && getState()== ConnectionState.LOGGEDIN) {
					logger.debug("requesting userip2");
					requestUserIP();
				}
			}
		},5, TimeUnit.SECONDS);
		
		if (!nmdc) {
			addCommand(new QUI(this),new RES(this));
			if (!favHub.isChatOnly()) {
				addCommand(new CTM(this),new RCM(this),new SCH(this));
			}
		}
	}
	
	/**
	 * queues a raw message for sending to the hub
	 * @param message
	 */
	public void sendRaw(String message)  {
		sendRaw(message,new SendContext());
	}
	
	/**
	 * queues a raw message for sending to the hub
	 * 
	 * @param context - information to fill out %[attribs]
	 */
	public synchronized void sendRaw(String message,SendContext context) {
		context.setHub(this);
		String mes = context.format(message);
		sendUnmodifiedRaw(mes);
	}
	
	/**
	 * sends a raw without doing any tinkering 
	 * like formatting by context..
	 * @param mes - the message to be sent..
	 */
	synchronized void sendUnmodifiedRaw(String mes) {
		try {
			connection.send(mes);
			logger.debug("sent raw: "+mes);
		} catch(IOException ioe) {
			logger.warn(ioe,ioe); 
		}
	}
	
	synchronized void sendUnmodifiedRaw(byte[] mes) {
		try {
			connection.send(ByteBuffer.wrap(mes));
			logger.debug("sent raw: "+new String(mes));
		} catch(IOException ioe) {
			logger.warn(ioe,ioe); 
		}
	}
	
	void passwordRequested() {
		FavHub favHub = getFavHub();
		if (favHub.getPassword() != null && !GH.isEmpty(favHub.getPassword())) {
			sendPassword(favHub.getPassword());
		} else {
			statusMessage(LanguageKeys.HubRequestedPassword,0);
		}
	}
	
	/**
	 * sends the password if no login was done 
	 * @param pass - the password
	 * @param base32Rand - random sent by the hub ... null for nmdc
	 */
	public synchronized void sendPassword(String pass) {
		if (getState() == ConnectionState.CONNECTED) {
			statusMessage(LanguageKeys.SendingPassword,0);
			if (nmdc) {
				GetPass.sendPass(this, pass);
			} else {
				GPA.sendPass(this, pass);
			}
			setRegistered(true);
		}
	}
	
	public void requestUserIP() {
		if (nmdc) {
			UserIP.sendUserIPRequest(this,getSelf());
		} else {
			//INF.sendIPRequestingINF(this);
		}
	}
	
	public boolean supportsUserIP() {
		if (nmdc) {
			return UserIP.supportsUserIp(this);
		} else {
			return false;
		}
	}


	/**
	 * called by produce user to insert the users in the users map
	 * and by hello to insert the self in the users map
	 * 
	 * minimum requirements that this works is that the user
	 * has his nick set
	 */
	void insertUser(User user) {
		users.put(user.getUserid(),user);
		if (!nmdc) {
			userBySid.put(user.getSid()  , user);
		}
		userPrefix.put(GetPrefix(user), user);
		if (user.getHub() != this) { //increase ShareSize if needed.. (potential problem user came from another hub we are in? -> need to create unique ID independent of CID)
			increaseSharesize(user.getShared());
		}
		user.userConnected(this);
	}
	
	public void internal_userChangedNick(User user,String newNick) {
		if (nmdc) {
			throw new IllegalStateException("In nmdc nick change is not allowed");
		}
		userPrefix.remove(GetPrefix(user));
		user.internal_setNick(newNick);
		userPrefix.remove(GetPrefix(user));
	}
	
	   
	/**
	 * called when a PM is received 
	 * the PM is then forwarded to interested listeners..
	 * 
	 * @param from - who sent the PM (name of the window)
	 * @param sender - <%[nick of sender]> message who sent 
	 * it originally.. usually the same as from but differs 
	 * for chat rooms  
	 * @param message - what was typed..
	 */
	void pmReceived(PrivateMessage pm /*  User from, User sender,String message*/) {
		for(IPMReceivedListener listener: hubl ) {
			listener.pmReceived(pm);
		}

		if (dcc.isAway() &&  pm.fromEqualsSender()) { // send away notification
			Date sent = lastAwaySent.get(pm.getFrom());
			if (sent == null || sent.before(new Date(System.currentTimeMillis()-60 *60 *1000))) {
				lastAwaySent.put(pm.getFrom(), new Date());
				sendPM(pm.getFrom(), dcc.getAwayMessage(),false);
			}
		}
		logPMMessage(pm);
	}
	
	/**
	 *  logs a message 
	 *  
	 * @param from
	 * @param sender
	 * @param message
	 */
	private void logPMMessage(PrivateMessage pm) {
		if (PI.getBoolean(PI.logPM)) {
			new DBLogger(pm.getFrom()).addLogEntry(pm.toString(), pm.getTimeReceived());
		}
	}
	
	
	   
	/**
	 * prints a message to the feed label
	 * when used with standard UI (message is forwarded to the 
	 * HubListener)
	 * 
	 */
	void feedReceived(FeedType ft,String message) {
		for (IFeedListener feedListener: hubl) {
			feedListener.feedReceived(ft, message);
		}
		logFeed(ft,message);
	}
	
	private void logFeed(FeedType ft,String message) {
		if (PI.getBoolean(PI.logFeed)) {
			if (feedLoggerDB == null) {
				feedLoggerDB = new DBLogger(favHub,false);
			}
			feedLoggerDB.addLogEntry(ft+message, System.currentTimeMillis());
		}
	}

	/**
	 * 
	 * @param target - the one that should receive the PM
	 * @param message - the message to send..
	 */
	public void sendPM(IUser target, String message,boolean me){
		PrivateMessage pm = new PrivateMessage(target, getSelf(),message,me);
		if (nmdc) {
			To.sendPM(this, target, message,me);
		} else {
			MSG.sendPM(this, target, message,me);
		}
		
		pmReceived(pm);
		
	}

	public void sendMM(String message,boolean me) {
		logger.debug("Sending Mainchat message "+message);
		if (nmdc) {
			MC.sendMainchatMessage(this, message,me);
		}  else {
			MSG.sendMM(this, message, me);
		}
	}

	/**
	 * sends a CTM message 
	 * 
	 * @param target - to whom
	 * @param token - token for ADC - null for NMDC
	 * @param protocol - protocol used in ADC - null for NMDC
	 */
	public void sendCTM(IUser target, CPType protocol,String token) {
		logger.debug("called sendCTM("+target.getNick()+")");
		
		if (!favHub.isChatOnly()) {
			for (ICTMListener ctml : ctmrl) { 
				//notifies listeners that PM was sent  --> 
				//needed for example so ConnectionHandler knows who is expected to connect... 
				ctml.ctmSent(target, protocol, token);
			}
			if (nmdc) {
				ConnectToMe.sendCTM(this, target,protocol);
			} else {
				CTM.sendCTM(this,target,protocol, token);
			}
		}
	}

	public void sendRCM(IUser target,CPType protocol,String token) {
		if (!favHub.isChatOnly()) {		
			Mode m = target.getModechar();
			if (m == Mode.ACTIVE) {
				if (nmdc) {
					RevConnectToMe.sendRCM(this, target);
				} else {
					RCM.sendRCM(this, target, protocol, token);
				}
			}
		}
		//here may be tell user that he can't connect there..
	}

	/**
	 * send a search to the hub.
	 * @param search - the pattern containing all information needed to search
	 */
	public void search(FileSearch search) {
		if (!favHub.isChatOnly()) {
			if (nmdc) {
				Search.sendSearch(this, search);
			} else {
				SCH.sendSearch(this, search);
			}
		}
	}
	   

	/**
	 * allows the UDP handler to pump a command into the normal command handling routine..
	 * @param sr
	 */
	public void searchResultReceived(String sr) {
		try {
			receivedCommand(sr);
		} catch (IOException pe) {
			logger.debug(pe);
		}
		logger.debug("sr received: "+sr);
	}
	   


	InetSocketAddress getHubIPAndPort() {
		InetSocketAddress isa = connection.getInetSocketAddress();
		return isa;

	}




	/**
	 * when a CTM is received this methods calls anyone interested in 
	 * specially the connection handler
	 * @param isa - the InetSocketaddress of the other
	 * @param other - who we should connect to  (null for namdc)
	 * @param protocol What should be used in the connection..
	 */
	void ctmReceived(InetSocketAddress isa,IUser other,CPType protocol,String token) {
		/*
		 * here check could be checking against common ips used to spam against..
		 * "jucy.eu" "dcpp.net" "hublist.org" "hubtracker.com" "dchublist.com" 
		 * "adchublist.com" "adcportal.com"
		 * 
		 * Though Ref field is better.. to punish hubs... -> therefore no blocking of ips..
		 */
		for (ICTMListener ctmr: ctmrl) {
			ctmr.ctmReceived(getSelf(), isa,other,protocol,token);
		}
	}


	void userQuit(User usr) {
		finalizeUser(usr,true);
	}
	
	/**
	 * 
	 * @param keys - search strings
	 * @param sizerestricted - if restrictions on size should be taken into account
	 * @param maxsize - is it maximum or minimum restriction
	 * @param size - what restriction
	 * @param st - search type restriction
	 * @param passive - if the user is passive
	 * @param searcher - if passive this is a User object .. if active this is an 
	 * InetSocketAddress address or a User Object (which holds an UDP socket and an InetAddress)
	 */
	void searchReceived(SearchParameter sp, boolean passive, User searcherusr,InetSocketAddress searcherip,String token) {
		
		sp.maxResults = passive? MaxPassiveResults:MaxActiveResults;
		sp.hub = favHub;
		Set<IFileListItem> found = dcc.getFilelist().search(sp);
		dcc.searchReceived(sp.keys, searcherusr != null? searcherusr:searcherip, found.size());
		
		if (!found.isEmpty()) {
			sendFoundBack(found,passive,searcherusr,searcherip,token);
		}
	}
	
	/**
	 * search Received for TTHRoot
	 * @param hash - the TTH root that was searched
	 * @param passive - if the searcher was active or passive
	 * @param searcher - if passive a User object if active an InetSocketAddress
	 * @param token ... ADC token .. needed to send the search back
	 */
	void searchReceived(HashValue hash,boolean passive,User searcherusr,InetSocketAddress searcherip,String token) {
		FileListFile found = dcc.getFilelist().search(hash);
		
		dcc.searchReceived(Collections.singleton(hash.toString()), searcherusr !=null ? searcherusr:searcherip,found == null? 0:1);
		
		if (found != null) {
			sendFoundBack(Collections.<IFileListItem>singleton( found),passive,searcherusr,searcherip,token);
		}
	}
	
	private void sendFoundBack(Set<IFileListItem> found,boolean passive,User searcherusr,InetSocketAddress searcherip,String token) {

		Set<SearchResult> srs = new HashSet<SearchResult>();
		for (IFileListItem ff: found) {
			if (ff.isFile()) {
				srs.add(new SearchResult((FileListFile)ff,getSelf(),dcc.getCurrentSlots(),
						dcc.getTotalSlots(),token));
			} else {
				srs.add(new SearchResult((FileListFolder)ff,getSelf(),dcc.getCurrentSlots(),
						dcc.getTotalSlots(),token));
			}
		}

		if (passive) {
			sendSearchResultbackPassive(srs,searcherusr); 
		} else {
			sendSearchResultbackActive(srs,searcherusr , searcherip);
		}
	}
	
	/**
	 * 
	 * @param srs - the searchresult that contains all infos that should be send
	 * @param target - the one that initiated the search that should receive the sr
	 */
	private void sendSearchResultbackPassive(Set<SearchResult> srs, User target) {
		if (nmdc) {
			SR.sendSR(srs, target, this);
		} else {
			RES.sendSR(srs, target, this);
		}
	}
	
	private void sendSearchResultbackActive(Set<SearchResult> srs, User target,InetSocketAddress searcherIp) {
    	try {
    		for (SearchResult sr: srs) {
    			String command = getUDPSRPacket(sr); 
    			byte[] packet = command.getBytes(getCharset().name());
    			if (target != null && target.hasSupportForUDPEncryption()) {
    				byte[] key = UDPEncryption.tokenStringToKey(sr.getToken());
    				packet = UDPEncryption.encryptMessage(packet, key);
    				if (Platform.inDevelopmentMode()) {
    					logger.warn("sending encrypted packet to: "+target);
    				}
    			}
    			dcc.getUdphandler().sendPacket(ByteBuffer.wrap(packet), searcherIp);
    		}
    	} catch (UnsupportedEncodingException cee) {
    		throw new IllegalStateException();
    	} catch (GeneralSecurityException e) {
    		logger.warn(e,e);
		}
	}
	
	public String getUDPSRPacket(SearchResult sr) {
		if (nmdc) {
			return SR.getUDPSrString(sr, this);
		} else {
			return RES.getUDPRESString(sr, this);
		}
	}

	/**
	 * 
	 * @param usr the usr to be disconnected..
	 * @param quit if it is a quit
	 */
	private void disconnectAllUsers() {
		for (User usr : new ArrayList<User>(users.values())) {
			finalizeUser(usr,false);
		}
		totalshare = 0;
	}

	private void finalizeUser(User usr,boolean quit) {
		users.remove(usr.getUserid());
		if (!nmdc) {
			userBySid.remove(usr.getSid());
		}
		userPrefix.remove(GetPrefix(usr));
		
		usr.disconnected(quit);
	}

	/**
	 * retrieves a user by nick
	 */
	public User getUserByNick(String nick) {
		if (nick == null) {
			throw new IllegalArgumentException();
		}
		if (nmdc) {
			return getUser(DCProtocol.nickToUserID(nick,this ));
		} else {
			for (User usr: users.values()) {
				if (nick.equals(usr.getNick())) {
					return usr;
				}
			}
			return null;
		}
	}
	
	public User getUserByCID(HashValue cid) {
		if (nmdc) {
			throw new IllegalArgumentException("only in adc possible, nmdc has no CID");
		} 
		return getUser(DCProtocol.CIDToUserID(cid, favHub));
	}
	
	User getUserBySID(int sid) {
		return userBySid.get(sid);
	}

	public User getSelf() {
		return self;
	}

	/**
	 * 
	 * notify all listeners registered with the hub
	 * and the user itself of a change
	 * may only be called by the user  -> use notiyfyUserChanged( if needed)
	 * 
	 * @param usr - who has changed
	 * @param type - and how it has changed
	 */
	public void internal_notifyUserChanged(UserChangeEvent uce) {
		for (IUserChangedListener listener : ucl) {
			listener.changed(uce);
		}
	}

	
	/**
	 * disconnects and reconnects after the specified time
	 * @param reconnectTime - if -1 never 
	 */
	private void disconnect(int reconnectTime) {
		weWantToReconnect	=	true;
		waitTime = reconnectTime < 0? Integer.MAX_VALUE:reconnectTime;
		connection.close();
	}

	/**
	 * reconnects the hub ..
	 * closing the connection
	 * if needed..
	 */
	public void reconnect(int waitTime) {
		if (getState() == ConnectionState.CLOSED) {
			weWantToReconnect = true;
			reconnectRunning = true;
			this.waitTime = waitTime < 0? Integer.MAX_VALUE:waitTime;
		} else {
			disconnect(waitTime);
		}
		
	}

	void redirectReceived(String addy) {
		redirectaddy = addy ;
		statusMessage(String.format(LanguageKeys.RedirectReceived,addy),0);
		if (PI.getBoolean(PI.autoFollowRedirect)) {
			followLastRedirect();
		}
	}

	/**
	 * if there is a reconnect pending..
	 * this function will 
	 */
	public void followLastRedirect() {
		if (redirectaddy != null) {
			setHubaddy(redirectaddy);
			redirectaddy = null;
			reconnect(0);
		}
	}
	
	private void setHubaddy(String hubaddy) {
		logger.debug(this.hubaddy);
		
		if (!ProtocolPrefix.matchesPrefix(hubaddy)) {
			hubaddy = "dchub://"+hubaddy;
		}
		
		int pos = hubaddy.indexOf(':')+3;
		fingerPrint = null;
		ProtocolPrefix p = ProtocolPrefix.parse(hubaddy);
		this.hubaddy = hubaddy.substring(pos);
		int postfix = this.hubaddy.indexOf('/');
		if (postfix >= 0) {
			String kp = this.hubaddy.substring(postfix);
			int i = kp.indexOf("pk=");
			if ( i >= 0  ) {
				kp = kp.substring(i+3);
				try {
					fingerPrint = HashValue.createHash(kp);
				} catch(IllegalArgumentException iae) {
					logger.warn(iae,iae);
				}
			}
			this.hubaddy = this.hubaddy.substring(0, postfix);
			logger.debug("hubaddy2: "+this.hubaddy);
		}
		setProtocolNMDC(p.nmdc);
		if (p.nmdc) {
			this.defaultCommand = new MC(this);
		} else {
			this.defaultCommand = null;
		}
		if (connection != null) {
			connection.setFingerPrint(fingerPrint);
		}
		
		
		
//		if (hubaddy.startsWith(NMDCPrefix) || hubaddy.startsWith(NMDCSPrefix)) {
//			this.hubaddy = hubaddy.substring(pos);
//			setProtocolNMDC(true);
//			this.defaultCommand = new MC(this);
//		} else if (hubaddy.startsWith(ADCPrefix) ||hubaddy.startsWith(ADCSPrefix) ) {
//			this.hubaddy = hubaddy.substring(pos);
//			int postfix = this.hubaddy.indexOf('/');
//			if (postfix > 0) {
//				this.hubaddy = this.hubaddy.substring(0, postfix);
//				logger.debug("hubaddy2: "+this.hubaddy);
//			}
//			setProtocolNMDC(false);
//			this.defaultCommand = null;
///*		} else if (hubaddy.startsWith(ADCSPrefix)) {
//			this.hubaddy = hubaddy.substring(ADCSPrefix.length());
//			
//			int postfix = this.hubaddy.indexOf('/');
//			if (postfix > 0) {
//				this.hubaddy = this.hubaddy.substring(0, postfix);
//				logger.info("hubaddy2: "+this.hubaddy);
//			}
//			setProtocolNMDC(false);
//			this.defaultCommand = null;
//			*/
//		} else {
//			setHubaddy("dchub://"+hubaddy);
//		}
		//setCharSet(); //charset must be set especially for ADC hub..
	}
	
	/**
	 * override to set charset to accommodate
	 * for Russian and other non European charsets instead of 
	 * default in NMDC
	 */
	protected void setCharSet() {
		if (GH.isEmpty(favHub.getCharset())) {
			super.setCharSet();
		} else {
			charset = Charset.forName(favHub.getCharset());
			if (connection != null) {
				connection.refreshCharsetCoders();
			}
		}
	}

	/**
	 * disconnects from the hub
	 * and removes the hub from the client
	 * called when the user closes the editorpart associated with this hub
	 */
	public synchronized void close() {
		weWantToReconnect	=	false;
		dcc.internal_unregisterHub(favHub);
		connection.close();
		end();
	}

	/**
	 * function to send a MyINFO about our self to the hub
	 * @param if force is true the MyINFO will be sent no matter what ignoring 
	 * all previous info
	 * if force is false it will only be sent if there is any new information
	 *  
	 */
	public void sendMyInfo(boolean force) {
		if (nmdc) {
			MyINFO.sendMyINFO(this,force);
		} else {
			INF.sendINF(this, force);
		}
	}
	




	public synchronized void timer() {
		
		super.timer();
		synchronized(loginTimeoutSynch) {
			if (timerLogintimeout % 10 == 0) {
				logger.trace("timer signal: "+timerLogintimeout);
			}
			
			if (!isLoginDone() && ++timerLogintimeout == 40) { //40 seconds connection/ login timeout
				switch(getState()) {
				case CONNECTED:
					statusMessage(LanguageKeys.LoginTimeout,0);
					break;
				case CONNECTING: 
					statusMessage(LanguageKeys.ConnectionTimeout,0);
					break;
				case CLOSED:
				case DESTROYED:
					break;
				default: 
					throw new IllegalStateException("timeout occured although current state is: "+getState());
				}
				connection.close();
			}
		}
		if (isLastReceivedTimeOut()) {
			synchronized(lastReceivedSynch) {
				logger.debug("no command received for long time - checking connection: " + (System.currentTimeMillis()-lastCommandReceived) );
			}
			setLastReceived() ; //reset timer..
			
			if (nmdc) {
				MyINFO.sendMyINFO(this,true); 
				//by sending a MyINFO we check if the connection of the hub is fine
			} else {
				//in ADC we can't send normal INF with force as this would clear all info..
				STA.sendSTAtoHub(this, new ADCStatusMessage("Ping Connection",0,0));
			}
		}
		
		
		if (reconnectRunning && --waitTime < 0 ) {
			reconnectRunning = false;
			unsuccessfulConnectionsInARow++; //will be cleared on successful connect.. so ok
			
			waitTime = Math.min(30,unsuccessfulConnectionsInARow) *(10 + GH.nextInt(30)); //reset the wait time..
			if (dcc.isRunningHub(favHub)) { //reconnect if the hub didn't do this already..
				logger.debug("reconnecting");
				statusMessage(LanguageKeys.Reconnecting,0);
				connection.reset(hubaddy);
			}
			
		}
	}
	    

	private void setLastReceived() {
		synchronized(lastReceivedSynch) {
			lastCommandReceived = System.currentTimeMillis();
		}
	}
	
	private boolean isLastReceivedTimeOut() {
		synchronized(lastReceivedSynch) {
			return System.currentTimeMillis()-lastCommandReceived > noCommandReceivedTimeout;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((favHub == null) ? 0 : favHub.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Hub other = (Hub) obj;
		if (favHub == null) {
			if (other.favHub != null)
				return false;
		} else if (!favHub.equals(other.favHub))
			return false;
		return true;
	}

	/**
	 * 
	 * @return the users only for the gui
	 */
	public Map<HashValue, User> getUsers() {
		return Collections.unmodifiableMap(users);
	}

	
	private final CopyOnWriteArrayList<IHubListener> hubl	=  
			new CopyOnWriteArrayList<IHubListener>();

	private final CopyOnWriteArrayList<IUserChangedListener> ucl	= 
			new CopyOnWriteArrayList<IUserChangedListener>();

	private final CopyOnWriteArrayList<ICTMListener> ctmrl	= 
		new CopyOnWriteArrayList<ICTMListener>();

//	private final CopyOnWriteArrayList<IUserCommandListener> ucoml	= 
//		new CopyOnWriteArrayList<IUserCommandListener>();


	
	public void registerCTMListener(ICTMListener  listener) {
		ctmrl.addIfAbsent(listener);
	}
	
	public void unregisterCTMListener(ICTMListener listener) {
		ctmrl.remove(listener);
	}
	
	public void registerHubListener(IHubListener listener) {
		super.registerProtocolStatusListener(listener);
		hubl.addIfAbsent(listener);
		ucl.addIfAbsent(listener);
	}
	
	public void unregisterHubListener(IHubListener listener) {
		super.unregisterProtocolStatusListener(listener);
		hubl.remove(listener);
		ucl.remove(listener);
	}
	
	public void registerUserChangedListener(IUserChangedListener listener) {
		ucl.addIfAbsent(listener);
	}
	public void unregisterUserChangedListener(IUserChangedListener listener) {
		ucl.remove(listener);
	}

//	public void registerUCListener(IUserCommandListener listener) {
//		ucoml.addIfAbsent(listener);
//	}
//
//	public void unregisterUCListener(IUserCommandListener listener) {
//		ucoml.remove(listener);
//	}


	/**
	 * @return the totalshare
	 */
	public long getTotalshare() {
		return totalshare;
	}

	/**
	 * @return the hubaddy
	 */
	public String getHubaddy() {
		return hubaddy;
	}

	/**
	 * @return the HubName without Topic
	 */
	public String getHubname() {
		return hubname;
	}

	public FavHub getFavHub() {
		return favHub;
	}



	/**
	 * 
	 * 
	 * sets the hubname without topic .. 
	 * and notifies all listeners
	 * 
	 * 
	 * @param hubname - only hubname without topic..
	 */
	void setHubname(String hubname) {
		if (!hubname.equals(this.hubname)) {
			this.hubname = hubname;
			
			for (IHubnameChangedListener ihcl: hubl) {
				ihcl.hubnameChanged(hubname,topic);
			}
		
		}
	}
	
	void setTopic(String topic) {
		if (!topic.equals(this.topic)) {
			this.topic = topic;
	
			for (IHubnameChangedListener ihcl: hubl) {
				ihcl.hubnameChanged(hubname,topic);
			}
		}
	}




	Set<String> getOthersSupports() {
		return othersSupports;
	}

	/*void addOthersSupports(String othersSupports) {
		this.othersSupports = othersSupports;
	} */

	

	public boolean isOpHub() {
		return getSelf().isOp();
	}

	private void setWeAreOp(boolean weAreOp) {
		if (weAreOp && opPlugins == null) {
			opPlugins = new ArrayList<IOperatorPlugin>();
			for (IOperatorPlugin iop : dcc.getOperatorPlugins()) {
				if (iop.init(this)) {
					opPlugins.add(iop);
					registerHubListener(iop);
				}
			}
		}
	}

	@Override
	public void receivedCommand(String command) throws IOException,
	ProtocolException {
		logger.debug("receivedCommand("+command+")");
		setLastReceived();
		super.receivedCommand(command);

	}
	

	public String getRedirectaddy() {
		return redirectaddy;
	}


	public boolean pendingReconnect() {
		return redirectaddy != null;
	}


	public List<Command> getUserCommands() {
		return Collections.unmodifiableList(userCommands);
	}
		
	/**
	 * adds a command received by $UserCommand
	 * to the hub..
	 * @param command - protocol independent description of the command
	 */
	void addUserCommand(Command command) {
		if (command.isSeparator()) {
			userCommands.add(command);
		} else if (userCommands.contains(command)) {
			int i = userCommands.indexOf(command);
			userCommands.remove(i);
			userCommands.add(i, command);
		} else {
			userCommands.add(command);
		}
	}
	
	/**
	 * 
	 * @return the last Command received.. possibly null if none..
	 * (Separators are not considered)
	 */
	Command getLastUserCommand() {
		if (!userCommands.isEmpty()) {
			for (int i = userCommands.size()-1; i >= 0;i--) {
				if (!userCommands.get(i).isSeparator()) {
					return userCommands.get(i);
				}
			}
		}
		
		return null;
	}
	
	void removeUserCommand(Command com) {
		userCommands.remove(com);
	}



	/**
	 * deletes all user commands in the specified place..
	 * @param where - the place to delete UserCommands
	 */
	void deleteUserCommands(int where) {
		for (Iterator<Command> iter = userCommands.iterator(); iter.hasNext();) {
			if (iter.next().delete(where)) {
				iter.remove();
			}
		}
	}



	public boolean isRegistered() {
		return registered;
	}

	public void setRegistered(boolean registered) {
		this.registered = registered;
	}
	
	private static String GetPrefix(IUser usr) {
		return usr.getNick().toLowerCase();
	}

	public SortedMap<String, IUser> getUserPrefix() {
		return userPrefix;
	}

	public String getTopic() {
		return topic;
	}

	void setOnceConnected() {
		this.onceConnected = true;
	}
	

	void enableDecompression() throws IOException {
		connection.setIncomingDecompression(Compression.ZLIB_FAST);
	}
	
	
}
