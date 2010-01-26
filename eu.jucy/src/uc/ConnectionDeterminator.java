package uc;

import helpers.GH;
import helpers.Observable;
import helpers.PreferenceChangedAdapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import logger.LoggerFactory;

import net.sbbi.upnp.impls.InternetGatewayDevice;
import net.sbbi.upnp.messages.UPNPResponseException;


import org.apache.log4j.Logger;

import eu.jucy.language.LanguageKeys;

import uc.crypto.HashValue;
import uc.files.downloadqueue.AbstractDownloadQueueEntry;
import uc.files.search.FileSearch;
import uc.files.search.SearchType;
import uc.protocols.AbstractConnection;
import uc.protocols.IProtocolCommand;
import uc.protocols.Socks;
import uc.protocols.hub.Hub;


/**
 * Class for determining public IP 
 * also helps with UPnP.. so everything needed to get our client active.
 * To make this easier this does also some self diagnostic and tries to find out
 * what works wrong
 * 
 * @author Quicksilver
 *
 */
public class ConnectionDeterminator extends Observable<String> implements IConnectionDeterminator {

	private static Logger logger = LoggerFactory.make();
	

	
	/*private static class CDHolder {
		private static final ConnectionDeterminator singleton = new ConnectionDeterminator();
	}*/
	
	private final Set<FavHub> hubsPresentingLocalIP = new HashSet<FavHub>();
	
/*	public static ConnectionDeterminator get() {
		return CDHolder.singleton;
	} */
	
//	private volatile boolean leftCreation = false;
	private PortMapping udpActive = null;
	
	
	
	private static final int MAXConnections = 3;
	
	/**
	 * used to check on IP correct..
	 */
	private volatile int connectionsFailedInARow = 0;
	
	//TCP state..
	private final TCPState tcp = new TCPState(PI.inPort);
	private final TCPState tls = new TCPState(PI.tlsPort);
	
	private class TCPState {
		
		private final String setting;
		public TCPState(String settingPort) {
			this.setting = settingPort;
		}
		
		private volatile boolean tcpTested = false;
		
		/**
		 * working if 0 or 1
		 * so rather the higher this is -> the less it works..
		 */
		private volatile int connectionsWorking = 0; 
		
		private PortMapping tcpActive = null;
		
		public int getPort() {
			return PI.getInt(setting);
		}
		
		public boolean isConnectionWorking() {
			return connectionsWorking <= 1;
		}
	}
	
	
	//UDP state
	private volatile boolean udpReceived = false;
	private volatile boolean udpTested = false;
	
	//UPnP
	private static final int LEASETIME = 3600;
	private volatile boolean upnpTried = false;
	private volatile int upnpMappingCreated = 0;
	
	private volatile boolean upnpDeviceFound = false;
	private volatile String upnpErrorDescription = null; // null means none..
	


	private volatile boolean natPresent = false;
	
	
	private final DCClient dcc;
	private PreferenceChangedAdapter pca;

	private volatile InetAddress current;
	
	public ConnectionDeterminator(DCClient dcc) {
		this.dcc = dcc;
		
		pca = new PreferenceChangedAdapter(PI.get(),PI.inPort,PI.passive,PI.udpPort,PI.tlsPort) {
			@Override
			public void preferenceChanged(String key, String oldValue,String newValue) {
				if (key.equals(PI.inPort) || key.equals(PI.tlsPort)) {
					TCPState s = key.equals(PI.inPort) ? tcp:tls;
    				s.tcpTested = false;
    				connectionsFailedInARow  = 0 ;
    				notifyObservers();
    			} else if(key.equals(PI.passive)) {
    				notifyObservers();
    			} else if (key.equals(PI.udpPort)) {
    				udpReceived = false;
    				udpTested = false;
    				notifyObservers();
    			}
			}
		};

	}
	
	/* (non-Javadoc)
	 * @see uc.IConnectionDeterminator#start()
	 */
	public Future<?> start() {
		return DCClient.getScheduler().submit(new Runnable() {
			public void run() {
				init();
			}
		});
	}
	
	/* (non-Javadoc)
	 * @see uc.IConnectionDeterminator#init()
	 */
	public void init() {
		pca.reregister();
		//init settings so url connections don'T Block too long...
		System.getProperties().setProperty("sun.net.client.defaultConnectTimeout", "10000") ;
		System.getProperties().setProperty("sun.net.client.defaultReadTimeout", "10000" ) ;
		try {
			current = getLocalAddress();	
		} catch(IOException ioe) {
			logger.debug(ioe,ioe);
		}
		try {
			natPresent = current == null || GH.isLocaladdress(current);
			if (natPresent) {
				current = getIP();
			} 
		} catch(IOException ioe) {
			logger.debug(ioe,ioe);
		}

		if (current == null) {
			logger.warn("Ip address detection failed.");
			try {
				current = InetAddress.getByName("127.0.0.1");
			} catch(IOException ioe) {
				logger.debug(ioe,ioe);
			}
		}
		notifyObservers();
		
		//periodical checking if active port mappings are still active..
		dcc.getSchedulerDir().scheduleAtFixedRate(new Runnable() {
			public void run() {
				if (udpActive != null && !udpActive.isValid()) {
					createPortmapping(false, PI.getInt(PI.udpPort),null);
				}
				if (tcp.tcpActive != null && !tcp.tcpActive.isValid()) {
					createPortmapping(true, tcp.getPort(),tcp);
				}
				if (tls.tcpActive != null && !tls.tcpActive.isValid()) {
					createPortmapping(true, tls.getPort(),tls);
				}
				
			}
		} , LEASETIME , 60, TimeUnit.SECONDS);
	}
	
	public void stop() {
		pca.dispose();
	}
	
	/* (non-Javadoc)
	 * @see uc.IConnectionDeterminator#isUdpReceived()
	 */
	public boolean isUdpReceived() {
		return udpReceived;
	}

	/* (non-Javadoc)
	 * @see uc.IConnectionDeterminator#isSearchStarted()
	 */
	public boolean isSearchStarted() {
		return udpTested;
	}
	
	
	/* (non-Javadoc)
	 * @see uc.IConnectionDeterminator#isNATPresent()
	 */
	public boolean isNATPresent() {
		return natPresent;
	}
	
	/* (non-Javadoc)
	 * @see uc.IConnectionDeterminator#getState()
	 */
	public CDState getState() {
		Boolean tcpWorking = tcp.tcpTested ? tcp.isConnectionWorking() : null ;
		Boolean tlsTCPWorking = tls.tcpTested ? tls.isConnectionWorking(): null;
		Boolean udpWorking = udpTested || udpReceived ? udpReceived : null ;
		Boolean upnpWorking = upnpTried ? upnpMappingCreated != 0: null;
		return new CDState(natPresent,tcpWorking,tlsTCPWorking,udpWorking,upnpWorking,upnpDeviceFound,upnpErrorDescription);
	}
	
	
	
	/* (non-Javadoc)
	 * @see uc.IConnectionDeterminator#connectionReceived(boolean)
	 */
	public void connectionReceived(boolean encrypted) {
		TCPState s = encrypted ? tls: tcp;
		if (connectionsFailedInARow != 0 || s.connectionsWorking != 0 ) {
			connectionsFailedInARow = 0;
			s.connectionsWorking = 0;
			notifyObservers();
		}
	
		if (!s.tcpTested) {
			s.tcpTested = true;
			notifyObservers();
		}
	
	}
	
	/* (non-Javadoc)
	 * @see uc.IConnectionDeterminator#connectionTimedOut(uc.IUser, boolean)
	 */
	public void connectionTimedOut(IUser usr,boolean encryption) {
		TCPState s = encryption ? tls: tcp;
		
		s.tcpTested = true;
		connectionsFailedInARow++; 
		logger.debug("connections failed: "+connectionsFailedInARow);
		if (connectionsFailedInARow % MAXConnections == 0) { 
			requestUserIP();
			s.connectionsWorking++;
			notifyObservers();
				
			if (s.connectionsWorking == 2  ) { //Try UPnP if connections are not working..
				createPortmapping(true,  s.getPort() ,s);
			}
		}
		
	}
	
	/* (non-Javadoc)
	 * @see uc.IConnectionDeterminator#searchStarted(uc.files.search.FileSearch)
	 */
	public void searchStarted(FileSearch search) {
		
		if (search.getSearchType().equals(SearchType.TTH) && HashValue.isHash(search.getSearchString())) {
			HashValue hash = HashValue.createHash(search.getSearchString()) ; 
			AbstractDownloadQueueEntry adqe = dcc.getDownloadQueue().get(hash);
			if (adqe != null && adqe.getNrOfOnlineUsers() > 0 ) {
				udpTested = true;
				if (!udpReceived) {
					dcc.getSchedulerDir().schedule(new Runnable() {
						public void run() {
							notifyObservers();
							if (!udpReceived) {
								createPortmapping(false, PI.getInt(PI.udpPort),null);
							}
						}
					},10,TimeUnit.SECONDS);
				} 
				
				
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see uc.IConnectionDeterminator#udpPacketReceived(java.net.InetSocketAddress)
	 */
	public void udpPacketReceived(InetSocketAddress from) {
		connectionsFailedInARow = 0;
		if (!udpReceived) {
			udpReceived = true;
			notifyObservers();
		}
		
	}
	
	/* (non-Javadoc)
	 * @see uc.IConnectionDeterminator#userIPReceived(java.net.InetAddress, uc.FavHub)
	 */
	public synchronized void userIPReceived(InetAddress ourIPAddress, FavHub whoTold) {
		if (!GH.isLocaladdress(ourIPAddress)) {
			if (!ourIPAddress.equals(current)) {
				current = ourIPAddress;
				logger.info("New Public IP: "+current.getHostAddress());
				connectionsFailedInARow = 0;
				notifyObservers();
			}
			hubsPresentingLocalIP.remove(whoTold);
		} else {
			hubsPresentingLocalIP.add(whoTold);
		}
	}
	
	private synchronized void requestUserIP() {
		List<Hub> hubs = new ArrayList<Hub>(dcc.getHubs().values());
		Collections.shuffle(hubs);
		boolean ipRequested = false;
		for (Hub h: hubs) {
			if (h.supportsUserIP() && !hubsPresentingLocalIP.contains(h.getFavHub())) {
				h.requestUserIP();
				ipRequested = true;
				break;
			}
		}
		if (!ipRequested) {
			requestIPOverWeb();
		}
	}
	
	/* (non-Javadoc)
	 * @see uc.IConnectionDeterminator#getPublicIP()
	 */
	public InetAddress getPublicIP()  {
		String ip = PI.get(PI.externalIp);
		if (GH.isEmpty(ip)) {
			return getDetectedIP();
		} else {
			try {
				return InetAddress.getByName(ip);
			} catch(UnknownHostException uhe) {
				logger.warn(uhe.toString() + " : "+ip,uhe);
				return getDetectedIP();
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see uc.IConnectionDeterminator#getDetectedIP()
	 */
	public InetAddress getDetectedIP() {
		return current;
	}
	
	private void requestIPOverWeb() {
		try {
			InetAddress ia = getIP();
			userIPReceived(ia,null);
		} catch (IOException ioe) {
			logger.warn("detection over web failed: "+ioe,ioe);
		}
	}
	
	/**
	 * 
	 * @return IP as determined by the web...
	 * @throws IOException
	 */
	private static InetAddress getIP() throws IOException {
    	BufferedReader br = null;
  
    	try {
    		
    		List<String> urls =  new ArrayList<String>(Arrays.asList(
    				"http://checkip.dyndns.org:8245/",
    				"http://www.ipchicken.com/", 
    			//	"http://www.myip.dk/",
    				"http://www.ip-adress.com/ip_lokalisieren/",
    				"http://www.myip.ch/",
    			//	"http://www.cmyip.com/",  TODO test again .. seems offline
    			//	"http://www.showmyip.com/",
    				"http://ipswift.com/"
    				)); 
    		Collections.shuffle(urls);
    		urls.add(0, "http://jucy.eu/ip.php"); //own address always first..
    		
    		Pattern p = Pattern.compile("("+IProtocolCommand.IPv4+")");
    		
    		try {
    			for (String url:urls) {
    				URL u = new URL(url);
    				URLConnection uc;
    				InetAddress ia = AbstractConnection.getBindAddress(PI.get(PI.bindAddress));
    				if (!Socks.isEnabled() && ia != null) { //if no proxy is enabled use this trick to bind the address..
    	  				Proxy proxy = new Proxy(Proxy.Type.DIRECT, 
        					    new InetSocketAddress( 
        					        ia, 0));
    	  				uc = u.openConnection(proxy);
    				} else {
    					uc = u.openConnection();
    				}
  

    				
			         //   
			        br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
			 

			        String line;
			       
			        while ((line = br.readLine()) != null) {
			        	Matcher m = p.matcher(line);
				        if (m.find()) {
				        	String ip = m.group(1);
				        	logger.info(String.format(LanguageKeys.CDDeterminedIPOverWeb,ip));
				        	return InetAddress.getByName(ip);
				        }
			        }
			        logger.debug("no ip found for url: "+url);
	    		}
    		} catch(IOException ioe) {
    			logger.info(ioe);
    		}

	        throw new IOException("IP address detection failed");
    	} finally {
    		GH.close(br);
    	}
    }
	
	/**
	 * mechanism for retrieving own public IF .. if the computer has 
	 * an interface with WAN access
	 * @return The first public IPv4! address that can be found
	 *  or a local if none
	 *  
	 * @throws IOException
	 */
	private static InetAddress getLocalAddress() throws IOException {

		InetAddress addy = null;
		for (Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();e.hasMoreElements();) {
			NetworkInterface next = e.nextElement();
			for (Enumeration<InetAddress> x = next.getInetAddresses(); x.hasMoreElements();) {
				addy = x.nextElement();
				if (!GH.isLocaladdress(addy) && addy instanceof Inet4Address) {
					return addy;
				}
			}
		}
		return InetAddress.getLocalHost();
	}
	
	
	
	public boolean isExternalIPSetByHand() {
		return !GH.isEmpty(PI.get(PI.externalIp));
	}



	/**
	 * little token that hold information on the connection
	 * 
	 * @author Quicksilver
	 *
	 */
	public static class CDState {
		
		private final boolean natPresent;
		

		/**
		 * ternary logic null == unknown
		 */
		private final Boolean tcpWorking;
		

		private final Boolean tlsTcpWorking;
		
		/**
		 * ternary logic null == unknown
		 */
		private final Boolean udpWorking;
		
		/**
		 * ternary logic null == unknown
		 */
		private final Boolean uPnPWorking;
		
		private final boolean upnpDeviceFound;
		private final String upnpErrorDescription;

		
		public CDState(boolean natPresent, Boolean tcpWorking,Boolean tlsTcpWorking,Boolean udpWorking,Boolean uPnPWorking,
				boolean upnpDeviceFound,String upnpErrorDescription) {
			this.tlsTcpWorking = tlsTcpWorking;
			this.natPresent = natPresent;
			this.tcpWorking = tcpWorking;
			this.udpWorking = udpWorking;
			this.uPnPWorking = uPnPWorking;
			this.upnpDeviceFound = upnpDeviceFound;
			this.upnpErrorDescription = upnpErrorDescription;
		}
		
		/**
		 * 
		 * @return all is working - 1 some components working - 2-3 nothing working
		 */
		public int getWarningState() {
			return (Boolean.FALSE.equals(getTcpWorking()) ? 1 : 0)  +
			(Boolean.FALSE.equals(getTLSWorking()) ? 1 : 0) +
			(Boolean.FALSE.equals(getUdpWorking()) ? 1 : 0);
		}
		
		/**
		 * gives a description for a user
		 * that should help  if something is wrong
		 * and tell what he can do or not do
		 * will be empty if no problem exists.
		 */
		public String getProblemSolution() {
			if (Boolean.FALSE.equals(tcpWorking) || Boolean.FALSE.equals(udpWorking)||
					Boolean.FALSE.equals(tlsTcpWorking) ) {
				
				String s = "";
				if (natPresent) {
					String ports = "";
					
					boolean beforeExists = false;
					if (Boolean.FALSE.equals(tcpWorking)) {
						ports += String.format(LanguageKeys.CDCheckTCP,PI.getInt(PI.inPort));
						beforeExists = true;
					}
					

					if (Boolean.FALSE.equals(tcpWorking) && Boolean.FALSE.equals(udpWorking)) {
						ports += " & ";
					}
					
					if (Boolean.FALSE.equals(udpWorking)) {
						ports += String.format(LanguageKeys.CDCheckUDP,PI.getInt(PI.udpPort));
						beforeExists = true;
					}
					
					if (beforeExists && Boolean.FALSE.equals(tlsTcpWorking) ) {
						ports += " & ";
					}
					
					if (Boolean.FALSE.equals(tlsTcpWorking)) {
						ports += String.format(LanguageKeys.CDCheckTCP,PI.getInt(PI.tlsPort));
					}
					
					
					s = String.format(LanguageKeys.CDCheckForwardedPorts,ports);
					
					if (Boolean.FALSE.equals(uPnPWorking)) {
						if (!GH.isNullOrEmpty(upnpErrorDescription)) {
							s += "\n"+upnpErrorDescription;
						} else if (upnpDeviceFound) {
							s += LanguageKeys.CDUPnPNotWorking;
						} else {
							s += LanguageKeys.CDUPnPNotPresent;
						}
					}
				}
				s += LanguageKeys.CDCheckFirewall;
				
				return s;
			}
			return "";
		}
		
		public Boolean getTcpWorking() {
			return tcpWorking;
		}
		
		public Boolean getTLSWorking() {
			return tlsTcpWorking;
		}
		

		public Boolean getUdpWorking() {
			return udpWorking;
		}
		public boolean isNatPresent() {
			return natPresent;
		}

		public Boolean getUPnPWorking() {
			return uPnPWorking;
		}

		
	}
	
	
	/**
	 * creates Portmapping if allowed/possible
	 * 
	 * @param tcp
	 * @param port
	 * @param state
	 */
	private void createPortmapping(final boolean tcp,final int port,final TCPState state) {
		if (tcp? (state.tcpActive == null || !state.tcpActive.isValid()) : (udpActive == null || !udpActive.isValid())) {
			if (PI.getBoolean(PI.allowUPnP)  && natPresent && dcc.isActive()) {
				new Thread(new Runnable() {
					public void run() {
						createPortmapping(port ,
								tcp,state);
					}
				},"CreatePortMapping").start();
			
			}
		}
	}
	
	/**
	 * try to map a port to the current computer
	 * @param portnumber - which port to map
	 * @param tcp - true means TCP/false a UDP mapping
	 * @return true if the mapping was created successful
	 */
	private boolean createPortmapping(int portnumber, boolean tcp,TCPState state) {
		upnpTried = true;
		
		boolean mapped = false;
		int discoveryTimeout = 1000; // 5 secs to receive a response from devices
		try {
			logger.debug( "checking devices");
			InternetGatewayDevice[] IGDs = InternetGatewayDevice.getDevices( discoveryTimeout );
			if ( IGDs != null && IGDs.length > 0 ) {
				upnpDeviceFound = true;
				InternetGatewayDevice igd = IGDs[0];


				logger.debug( "Found device " + igd.getIGDRootDevice().getModelName() );
				// now let's open the port
				String localHostIP = InetAddress.getLocalHost().getHostAddress();
				logger.debug("localhost: "+localHostIP);
				final Thread t = Thread.currentThread();
				
				ScheduledFuture<?> fut = dcc.getSchedulerDir().schedule(new Runnable() {
					@SuppressWarnings("deprecation")
					public void run() {
						logger.debug("interrupting");
						t.stop(new IllegalStateException()); // sadly no other chance to do this..
					}
				},40,TimeUnit.SECONDS);
				
				logger.debug("Cancelled Portmapping: "+cancelPortmapping(tcp,state));
				try {
				
					mapped = igd.addPortMapping( "jucy port forward "+portnumber, 
							null, portnumber, portnumber,
							localHostIP, LEASETIME, getTCPStr(tcp ));
				
				} catch(IllegalStateException ie) {
					mapped = false;
				}
				
				fut.cancel(false);

				if (mapped) {
					setActive(tcp,new PortMapping(portnumber,tcp,igd),state);
					upnpMappingCreated++;
					logger.info("Created Portmapping via UPnP");
					upnpErrorDescription = null;
				} else {
					logger.info("Unable to create Port mapping via UPnP");
				}
				logger.debug("mapped: "+mapped+" ");

			} else {
				upnpDeviceFound = false;
			}
		} catch ( IOException ex ) {
			String err = "Could not operate UPnP device: "+ (ex.getMessage() != null?ex.getMessage():ex.toString());
			logger.debug(err , ex);
			upnpErrorDescription = err;
			// some IO Exception occurred during communication with device
		} catch( UPNPResponseException respEx ) {
			String err = "Could not operate UPnP device: "+ 
			(respEx.getMessage() != null?respEx.getMessage():respEx.toString());
			logger.debug(err, respEx);
			upnpErrorDescription = err;
		}
		
		notifyObservers();
		
		return mapped;

	}
	
	private static String getTCPStr(boolean tcp) {
		return tcp ? "TCP" : "UDP";
	}
	

	private void setActive(boolean tcp,PortMapping pm,TCPState state) {
		if (tcp) {
			state.tcpActive = pm;
		} else {
			udpActive = pm;
		}
	}
	
	private PortMapping getPM(boolean tcp,TCPState ts) {
		return tcp? ts.tcpActive : udpActive;
	}
	
	private boolean cancelPortmapping(boolean tcp,TCPState ts) throws  UPNPResponseException , IOException{
		PortMapping pm = getPM(tcp,ts);
		if (pm != null) {
			boolean cancel = pm.cancel();
			if (cancel) {
				setActive(tcp, null,ts);
			}
			return cancel;
		}
		return false;
	}
	
	
	private static class PortMapping {
		private final int portnumber;
		private final boolean tcp;
		private final InternetGatewayDevice igd;
		private final long creationTime;
		
		public PortMapping(int portnumber, boolean tcp,InternetGatewayDevice igd) {
			this.portnumber = portnumber;
			this.tcp = tcp;
			this.igd = igd;
			this.creationTime = System.currentTimeMillis();
		}
		
		public boolean cancel() throws  UPNPResponseException , IOException{
			boolean unmapped = igd.deletePortMapping( null, portnumber, getTCPStr(tcp));
			return unmapped;
		}
	
		private boolean isValid() {
			long timedif = System.currentTimeMillis() - creationTime;
			timedif /= 1000;
			return timedif < LEASETIME ;
			
		}
	}
	
}
