package uc;

import helpers.GH;
import helpers.IObservable;
import helpers.Observable;
import helpers.PreferenceChangedAdapter;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import uc.IStoppable.IStartable;
import uc.IUser.Mode;
import uc.crypto.CryptoManager;
import uc.crypto.HashValue;
import uc.crypto.Tiger;
import uc.protocols.Socks;




/**
 * TODO implement Identity concept --> rewrite DCClient to have one base-identity..
 * -> make it so multiple identities are allowed/possible...
 * 
 * 
 * @author Quicksilver
 *
 */
public abstract class Identity extends Observable<ChangedAttribute>  {
	
	private static final String[] IDENTITY_SETTINGS = 
		new String[] {PI.allowTLS,PI.allowUPnP,PI.bindAddress,
		PI.externalIp,PI.inPort,PI.tlsPort,PI.passive,PI.uUID,
		
		PI.socksProxyEnabled,PI.socksProxyHost,PI.socksProxyPassword,
		PI.socksProxyPort,PI.socksProxyUsername};

	

	
	private final String name;
	private final String certFileName;
	
	public String getCertFileName() {
		return certFileName;
	}

	private final HashValue pid;
	/**
	 * stores all hubs running using this identity 
	 *  used to start/stop included stuff based on 
	 *  if in use..
	 */
	protected final List<FavHub> runningHubs = new  ArrayList<FavHub>();
	
	
	// -> missing which dirs are included.. for own filelist..
	
	
	private final ICryptoManager cryptoManager;
	
	private final ConnectionHandler connectionHandler;
	private final IConnectionDeterminator connectionDeterminator;


	/**
	 * default identity
	 * containing the usual settings..
	 */
	public Identity(DCClient dcc,String name,String certFileName) {
		this.certFileName = certFileName;
		this.name = name;
		pid = loadPID();
		cryptoManager = new CryptoManager(dcc, this);
		connectionHandler = new ConnectionHandler(dcc,this);
		connectionDeterminator = new ConnectionDeterminator(dcc, this);
	}
	
	
	

	protected HashValue loadPID() {
		String uuid = get(PI.uUID);
    	if (GH.isNullOrEmpty(uuid)) {
    		SecureRandom sr = new SecureRandom();
        	byte[] b = new byte[1024];
        	sr.nextBytes(b);
        	HashValue hash = Tiger.tigerOfBytes(b);
        	uuid = hash.toString();
    		put(PI.uUID, uuid);
    	}
		return  HashValue.createHash(get(PI.uUID));
	}
	
	
	
	/**
	 * must be added before hub is started
	 * @param fh favHub added..
	 */
	void addRunningHub(FavHub fh) {
		synchronized(runningHubs) {
			if (runningHubs.isEmpty()) {
				start();
			}
			runningHubs.add(fh);
		}
	}
	
	void removeRunningHub(FavHub fh) {
		synchronized(runningHubs) {
			if (runningHubs.remove(fh) && runningHubs.isEmpty()) {
				stop();
			}
		}
	}
	
	protected void start() {
		connectionHandler.start();
		connectionDeterminator.start();
	}
	
	protected void stop() {
		connectionHandler.stop();
		connectionDeterminator.stop();
	}
	
	public ConnectionHandler getConnectionHandler() {
		return connectionHandler;
	}
	
	public IConnectionDeterminator getConnectionDeterminator() {
		return connectionDeterminator;
	}
	
	public boolean isDefault() {
		return false;
	}
	
	
	/**
	 * place holder  should return true if
	 * IPv4 is used 
	 * @return true
	 */
	public boolean isIPv4Used() {
		return connectionDeterminator.getPublicIP() != null;
	}
	
	/**
	 * 
	 * @return true if IPv6 connections are possible..
	 */
	public boolean isIPv6Used() {
		return connectionDeterminator.getIp6FoundandWorking() != null;
	}
	

	
	/**
	 * check if TLS was enabled at start of Program .. and is now in a usable state.
	 * @return true if TLS is fully initialized and running..(for this identity..)
	 * 
	 */
	public boolean currentlyTLSSupport() {
		return  connectionHandler.isTLSRunning() && getBoolean(PI.allowTLS);
	}
	
	public abstract void put(String pref,String value);
	
	
	public void put(String pref,boolean value) {
		put(pref,Boolean.toString(value));
	}
	
	public int getInt(String pref) {
		return Integer.parseInt(get(pref));
	}
	
	public abstract String get(String pref);
	
	
	public boolean getBoolean(String pref) {
		return Boolean.parseBoolean(get(pref));
	}
	
	public boolean isActive() {
		return !getBoolean(PI.passive);
	}
	
    public Mode getMode() {
    	return isActive() ? Mode.ACTIVE : 
    					(Socks.isEnabled()? Mode.SOCKS:Mode.PASSIVE) ; 
    }
	
	public String getName() {
		return name;
	}

	public HashValue getPID() {
		return pid;
	}
	public HashValue getCID() {
		return getPID().hashOfHash();
	}
	
	public ICryptoManager getCryptoManager() {
		return cryptoManager;
	}
		
	
	public abstract static class FilteredChangedAttributeListener implements IObserver<ChangedAttribute> {

		private final String[] attribs;
		protected FilteredChangedAttributeListener(String... attributes ) {
			this.attribs = attributes;
		}
		
		public void update(IObservable<ChangedAttribute> o, ChangedAttribute arg) {
			for (String s: attribs) {
				if (s.equals(arg.getAttribute())) {
					preferenceChanged(arg.getAttribute(),arg.getOldValue(),arg.getNewValue());
					break;
				}
			}
		}
		
		protected abstract void preferenceChanged(String pref,String oldValue,String newValue);
		
	}
	
	public static class DefaultIdentity extends Identity implements IStartable {

		public DefaultIdentity(DCClient dcc) {
			super(dcc, "<default>",".keystore");
			new PreferenceChangedAdapter(PI.get(),IDENTITY_SETTINGS) {
				@Override
				public void preferenceChanged(String preference, String oldValue,String newValue) {
					notifyObservers(new ChangedAttribute(preference, oldValue, newValue));
				}
			};
		}
		public String get(String pref) {
			return PI.get(pref);
		}
		
		/**
		 * must be added before hub is started
		 * @param fh favHub added..
		 */
		void addRunningHub(FavHub fh) {
			synchronized(runningHubs) {
				runningHubs.add(fh);
			}
		}
		
		void removeRunningHub(FavHub fh) {
			synchronized(runningHubs) {
				runningHubs.remove(fh);
			}
		}
		
		@Override
		public void put(String pref, String value) {
			PI.put(pref, value);
		}
		
		public boolean isDefault() {
			return true;
		}
		@Override
		public void start() {
			super.start();
		}
		@Override
		public void stop() {
			super.stop();
		}
		
		
		
	}
	
	public static class OtherIdentity extends Identity {

		private final Map<String,String> idSettings = 
			Collections.synchronizedMap(new HashMap<String, String>());
		
		public OtherIdentity(DCClient dcc) {
			super(dcc,"","");
		}
		
		public String get(String pref) {
			return idSettings.get(pref);
		}

	

		@Override
		public void put(String pref, String value) {
			String oldValue = idSettings.get(pref);
			if (!oldValue.equals(value)) {
				idSettings.put(pref, value);
				notifyObservers(new ChangedAttribute(pref, oldValue, value));
			}
		}
		
	}
	
	
//	
//	private int tcpPort,tlsPort; //Connection handler stuff
//	private ConnectionHandler handler; // own connection handler for these ports needed..
//	
//	private CryptoManager cryptoManager; // all the cryptostuff..
//	
//	private IUser filelistSelf;   //own CID/PID -> stuff like email and Nick could be packed here.. (Personal Information Pref page basically) self or rather filelist self that is
//	
//	private OwnFileList filelist; // FILELIST used for searching here..
	
//External IP settable per identity..or whole connection settings.. also bind address..
}
