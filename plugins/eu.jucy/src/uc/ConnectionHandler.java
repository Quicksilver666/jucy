
package uc;

import helpers.GH;
import helpers.Observable;
import helpers.StatusObject;
import helpers.StatusObject.ChangeType;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;


import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import logger.LoggerFactory;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.Platform;


import uc.IStoppable.IStartable;
import uc.Identity.FilteredChangedAttributeListener;
import uc.crypto.HashValue;
import uc.protocols.AbstractConnection;
import uc.protocols.CPType;
import uc.protocols.MultiStandardConnection;
import uc.protocols.MultiStandardConnection.ISocketReceiver;
import uc.protocols.client.ClientProtocolStateMachine;
import uc.protocols.client.ClientProtocol;
import uc.protocols.client.ClientProtocolStateMachine.CPSMManager;
import uc.protocols.hub.ICTMListener;


/**
 *
 * connection handler manages the TCP ServerSocket that receives incoming
 * TCP connections  it also creates outgoing connections to users that are 
 * interesting for us (shares something we want) and to users that explicitly 
 * request us to connect to them i.e. by sending an CTM
 * 
 *  @author <b>Quicksilver </b> 
 *
 */
public class ConnectionHandler extends Observable<StatusObject>
	implements ICTMListener , IUserChangedListener ,IStartable {  

	private static final Logger logger = LoggerFactory.make();
	
	public static final int USER_IDENTIFIED_IN_CONNECTION 	= 1,	//add connection   rem ProtocolStatemachine if present 
							TRANSFER_STARTED				= 2,	//connection remove -> add Transfer
							//Transfer changes are given directly over a different listener..
							TRANSFER_FINISHED				= 3,	//remove Transfer -> addConnection
							CONNECTION_CLOSED				= 4,	//rem connection -> add CPSM if present
							STATEMACHINE_CREATED			= 5,	// no connection just the statemachine got created yet
							STATEMACHINE_DESTROYED			= 6,	// no connection . statemachine finished
							STATEMACHINE_CHANGED			= 7;	//statemachine changed..
	
	

							
	private final Set<Object> active = 
		Collections.synchronizedSet(new HashSet<Object>()); 

	private final Observable<StatusObject> fileTransferObservable = new Observable<StatusObject>(); 

	
	
	



	private final FilteredChangedAttributeListener pca;

	
	private class ServerSocketInfo {
		private final boolean encrypted;
		public ServerSocketInfo(boolean encrypted) {
			this.encrypted = encrypted;
		}
		private ServerSocketChannel ssc;
		private SelectionKey selKey;
		private volatile boolean running;
		
		int getPort() {
			return identity.getInt(encrypted ? PI.tlsPort: PI.inPort);
		}
		
		void changePort() {
			close();
			register(this);
		}
		
		void close() {
			if (selKey != null) {
				selKey.cancel();
			}
			GH.close(ssc);
			running = false;
		}
	}
	
	private final ServerSocketInfo 
		normal = new ServerSocketInfo(false),
		tls = new ServerSocketInfo(true);
	



	/**
	 * users that are interesting
	 * ie have something... that we want from   them
	 * (if we are active this should contain the same users as 
	 * (expectedToConnect - all passive user that want to connect to us))
	 * 
	 * the mapping goes to the NMDCC that is responsible for makeing a connect to the user
	 *  
	 */
	private final Map<IUser,ClientProtocolStateMachine> interesting = 
		Collections.synchronizedMap(new HashMap<IUser,ClientProtocolStateMachine>()); 


	/** 
	 * 
	 * maps userid to a User that we expect to connect to us (because we have sent a CTM to them) 
	 * 
	 * this is a minor pack-ratting problem .. no users are ever removed from this Set
	 * though dumps show that this is no problem..
	 * 
	 */
	private final Set<ExpectedInfo> expectedToConnect = 
		Collections.synchronizedSet(new HashSet<ExpectedInfo>());

	private ScheduledFuture<?> expectedRefresher;

	private final DCClient dcc;

	private final CPSMManager cpsmManager;
	
	private final Identity identity;


	public Identity getIdentity() {
		return identity;
	}

	/**
	 * 
	 */
	public ConnectionHandler(DCClient dcclient,Identity identityx) {
		this.dcc = dcclient;
		this.identity = identityx;
		cpsmManager = new CPSMManager(dcc);
		
		//register a port changed listener with the settings
		pca = new FilteredChangedAttributeListener(PI.inPort,PI.bindAddress,PI.tlsPort) {
			@Override
			public void preferenceChanged(String preference, String oldValue,String newValue) {
				if (PI.inPort.equals(preference) || PI.bindAddress.equals(preference)) {
					normal.changePort();
				} 
				if ((PI.tlsPort.equals(preference) || PI.bindAddress.equals(preference)) && 
						identity.getCryptoManager().isTLSInitialized()) {
					tls.changePort();
				}
			}
		};
	}
	
	public void start() {
		register(normal);
		if (identity.getCryptoManager().isTLSInitialized()) {
			register(tls);
		}
		identity.addObserver(pca);
		cpsmManager.start();
		dcc.getPopulation().registerUserChangedListener(this);
		expectedRefresher = dcc.getSchedulerDir().scheduleWithFixedDelay(new Runnable() {
			public void run() {
				synchronized(expectedToConnect) {
					for (Iterator<ExpectedInfo> it = expectedToConnect.iterator();it.hasNext();) {
						if (it.next().isOld()) {
							it.remove();
						}
					}
				}
			}
		}, 60, 60, TimeUnit.SECONDS);
		
		
	}
	
	public void stop() {
		normal.close();
		tls.close();
		identity.deleteObserver(pca);
		cpsmManager.stop();
		dcc.getPopulation().unregisterUserChangedListener(this);
		if (expectedRefresher != null) {
			expectedRefresher.cancel(false);
			expectedToConnect.clear();
		}
		//TOOD close all running transfers -> CPSM should stay..?
		
	}
	
	
	
	
	
	public void changed(UserChangeEvent uce) {
		IUser usr = uce.getChanged();
		switch (uce.getType()) {
		case CONNECTED:
			if (usr.weWantSomethingFromUser()) {
				logger.debug("connected user interesting");
				onInterestingUserArrived(usr);
			}
			break;
		case CHANGED:
			switch(uce.getDetail()) {
			case UserChangeEvent.DOWNLOADQUEUE_ENTRY_PRE_ADD_FIRST:
				logger.debug("preadd first user interesting");
				onInterestingUserArrived(usr);
				break;
			}
			break;
		}
	}

	public boolean isTLSRunning() {
		return tls.running;
	}
	


	/**
	 * function to add a user to the expected to connect list..
	 * when ever we send a CTM to a user we put im in here
	 * so we know that he is expected to connect..
	 * 
	 * @param usr the user we expect to connect
	 * @param protocol  the protocol for this connection  null in NMDC
	 * @param token - the token for this connection - null in NMDC
	 * 
	 */
	public void ctmSent(IUser target, CPType protocol, String token) {
		ExpectedInfo ei = new ExpectedInfo(target,protocol,token);
		synchronized(expectedToConnect) {
			expectedToConnect.remove(ei);
			expectedToConnect.add(ei);
		}
	}
	
	
	/**
	 * NMDC function
	 * 
	 * tries to verify user against given IP
	 * if no nick matches the IP a random user is returned..
	 * 
	 * @param nick - the nick the user sent in CTM
	 * @param ip the IP the connection has
	 * @return the user that matches nick and if possible also IP
	 */
	public IUser getUserExpectedToConnect(String nick, InetAddress ip) {
		if (ip == null) {
			throw new IllegalArgumentException();
		}
		ArrayList<ExpectedInfo> possibleUsers = new ArrayList<ExpectedInfo>(1);
		
		synchronized(expectedToConnect) {
			for (ExpectedInfo ei : expectedToConnect) {
				IUser usr = ei.getUser();
				if (nick.equals(usr.getNick())) {
					if (ip.equals(usr.getIp()) && usr.resolveDQEToUser() != null && !ei.isRemoved()) {
						possibleUsers.clear();
						possibleUsers.add(ei);
						break;
					} else {
						possibleUsers.add(ei);
					}
				}
			}
			ExpectedInfo found = GH.getRandomElement(possibleUsers);
			if (found != null) {
				found.remove();
				return found.getUser();
			} else {
				if (Platform.inDevelopmentMode()) {
					logger.warn("not found CTM info for "+nick+"  user was not expected to connect!");
				}
				return null;
			}
		}
	}
	
	/**
	 *  user determination should be done by token not by CID
	 * 
	 * @param id
	 * @return
	 */
	public ExpectedInfo getUserExpectedToConnect(HashValue cid,String token) {
		if (token == null) {
			throw new IllegalArgumentException();
		}
		synchronized(expectedToConnect) {
			for (ExpectedInfo ei : expectedToConnect) {
				if (token.equals(ei.token)) {
					if (cid == null || cid.equals(ei.user.getCID())) {
						ei.remove();
						return ei;
					}
				}
			}
		}
	
		return null;
	}


	
	
	/**
	 * CHListens here for ctms that were received..
	 * and opens a Socket to the appropriate address
	 */
	public void ctmReceived(IUser self , InetSocketAddress isa,IUser other,CPType protocol,String token) {
		if (dcc.getFilelist().isInitialized()) {
			ClientProtocol ctcp = new ClientProtocol( isa ,this , self,other,
					protocol,token , protocol.isEncrypted());
			ctcp.start();
		}
	}
	
	

	
	
	
	/**
	 * whenever a user connects this method is called to check if
	 * we want something of the user.
	 * <p>
	 * method is also called when the first DownloadQueueEntry is added
	 * to the users DQE list.
	 *   
	 * @param usr the user that should be added
	 *  if he is interesting (has something we want)
	 * 
	 */
	public void onInterestingUserArrived(final IUser usr) {
		if (usr.weWantSomethingFromUser() && !interesting.containsKey(usr)) {
			dcc.executeDir(new Runnable() { 
				public void run() {
					new ClientProtocolStateMachine(usr,ConnectionHandler.this);
				}
			});
		}
	}
	


	/**
	 * 
	 * @return the local port of the ConnectionHandler
	 */
	public int getPort(boolean tlsPort) {
		ServerSocketInfo  ssi = tlsPort? tls:normal;
			
		if (ssi.ssc != null && ssi.ssc.isOpen()) {
			return ssi.ssc.socket().getLocalPort();
		} else {
			return ssi.getPort();
		}
	}
	
	/**
	 * this handles the incoming connections
	 */
	public void register(final ServerSocketInfo  ssi) {

		//while(ssi.running) {
		try {
			ssi.running = true;
			ssi.ssc	=  ServerSocketChannel.open();
			AbstractConnection.bindSocket( ssi.ssc, ssi.getPort());

			ssi.ssc.configureBlocking(false);
			
			MultiStandardConnection.get().register(ssi.ssc, new ISocketReceiver() {

				public void socketReceived(ServerSocketChannel port,SocketChannel created) {
					try {
						identity.getConnectionDeterminator().connectionReceived(ssi.encrypted);
						created.configureBlocking(false);
						ClientProtocol cp = new ClientProtocol(created,ConnectionHandler.this,ssi.encrypted);
						cp.start();
					} catch(IOException ioe) {
						logger.error(ioe,ioe);
					}
				}

				public void setKey(SelectionKey key) {
					ssi.selKey = key;
				}

			});
			dcc.logEvent(String.format(
							ssi.encrypted	?LanguageKeys.StartedEncConnectionHandler
											:LanguageKeys.StartedConnectionHandler
							, ssi.ssc.socket().getLocalPort()));//  "Started "+(ssi.encrypted?"encrypted":"normal")+ " connection handler on TCP-Port: "+ssi.ssc.socket().getLocalPort());

		} catch(ClosedByInterruptException ie) {
			logger.debug("Connection handler socket closed by interruption");
			ssi.close();
		} catch(BindException be) {
			logger.error(String.format(LanguageKeys.TCPPortInUse, ssi.getPort()),be);// "TCP port %d in use! Change TCP port!",be);
			ssi.close();
		} catch (IOException e) {
			logger.warn("error in serversock "+e,e);
			ssi.close();
		} 

	}
	
	public void notifyOfChange(int detail,ClientProtocol cp,Object other) {
		StatusObject so =  new StatusObject(cp,ChangeType.CHANGED,detail,other );
		switch(detail) {
		case USER_IDENTIFIED_IN_CONNECTION:
			active.add(cp);
			if (other != null) {
				active.remove(other);
			}
			break;
		case CONNECTION_CLOSED:
			active.remove(cp);
			ClientProtocolStateMachine cpsm = (ClientProtocolStateMachine)other;
			if (cpsm != null && cpsm.isActive()) {
				active.add(other);
			}
			break;
		case STATEMACHINE_CREATED:
			active.add(other);
			break;
		case STATEMACHINE_DESTROYED:
			active.remove(other);
			break;
		}
		notifyObservers(so);
	}



	
	public ClientProtocolStateMachine getStateMachine(IUser usr) {
		return interesting.get(usr);
	}
	

	
	public void removeStatemachine(IUser usr,ClientProtocolStateMachine cpsm) {
		if (usr == null) {
			throw new IllegalArgumentException();
		}
		//ClientProtocolStateMachine ccps = 
		interesting.remove(usr);
		notifyOfChange(STATEMACHINE_DESTROYED, null, cpsm);
		deleteObserver(cpsm);
	}
	
	public void addStatemachine(IUser usr,ClientProtocolStateMachine  ccps) {
		interesting.put(usr, ccps);
		addObserver(ccps);
		notifyOfChange(STATEMACHINE_CREATED, null, ccps);
	}
	
	/**
	 * returns active elements for the GUI ..
	 * a mix of ClientProtocols and StateMachine..
	 * @return
	 */
	public Set<Object> getActive() {
		return active;
	}
	
	public int getNrOfRunningDownloads() {
		int i = 0;
		synchronized (active) {
			for (Object o: active) {
				if (o instanceof ClientProtocol) {
					ClientProtocol cp =(ClientProtocol)o;
					if (cp.getFti().isDownload()) {
						i++;
					}
				}
			}
		}
		return i;
	}

	public DCClient getDCC() {
		return dcc;
	}
	
	
	
	public ISlotManager getSlotManager() {
		return dcc.getSlotManager();
	}

	public CPSMManager getCpsmManager() {
		return cpsmManager;
	}

	public void addTransferObserver(helpers.Observable.IObserver<StatusObject> o) {
		fileTransferObservable.addObserver(o);
	}

	public void deleteTransferObserver(IObserver<StatusObject> o) {
		fileTransferObservable.deleteObserver(o);
	}

	public void notifyTransferObservers(StatusObject arg) {
		fileTransferObservable.notifyObservers(arg);
	}

	/**
	 * bundles of information holding info for us of users that are expected
	 * to connect to us
	 * 
	 * @author Quicksilver
	 *
	 */
	public static class ExpectedInfo {
		
		private static final long OLD_MILLIS = 5 *60 *1000;
		
		private final IUser user;
		private final CPType protocol;
		private final String token;
		private final long created;
		private boolean removed = false;
		
		public ExpectedInfo(IUser user,CPType protocol, String token) {
			this.protocol = protocol;
			this.token = token;
			this.user = user;
			this.created = System.currentTimeMillis();
		}

		public IUser getUser() {
			return user;
		}
		
		public boolean isOld() {
			return System.currentTimeMillis() - created > OLD_MILLIS; 
		}
		
		public synchronized boolean isRemoved() {
			return removed;
		}
		
		public synchronized void remove() {
			removed = true;
		}

		public CPType getProtocol() {
			return protocol;
		}

		public String getToken() {
			return token;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((protocol == null) ? 0 : protocol.hashCode());
			result = prime * result + ((token == null) ? 0 : token.hashCode());
			result = prime * result + ((user == null) ? 0 : user.hashCode());
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
			ExpectedInfo other = (ExpectedInfo) obj;
			if (protocol != other.protocol)
				return false;
			if (token == null) {
				if (other.token != null)
					return false;
			} else if (!token.equals(other.token))
				return false;
			if (user == null) {
				if (other.user != null)
					return false;
			} else if (!user.equals(other.user))
				return false;
			return true;
		}


		
		
		
	}
	


}



