
package uc;

import helpers.GH;
import helpers.Observable;
import helpers.PreferenceChangedAdapter;
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
import java.util.Set;
import java.util.HashSet;
import java.util.Map;

import logger.LoggerFactory;
import org.apache.log4j.Logger;


import uc.crypto.HashValue;
import uc.listener.IUserChangedListener;
import uc.protocols.AbstractConnection;
import uc.protocols.CPType;
import uc.protocols.MultiStandardConnection;
import uc.protocols.MultiStandardConnection.ISocketReceiver;
import uc.protocols.client.ClientProtocolStateMachine;
import uc.protocols.client.ClientProtocol;
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
	implements ICTMListener , IUserChangedListener {  

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

//	private final Map<IUser,List<DebugTest>> debugList = new HashMap<IUser,List<DebugTest>>();
//	
//	private class DebugTest {
//		public DebugTest(int action,Object o) {
//			this.action = action;
//			this.object = o == null? "null": ""+System.identityHashCode(o);
//		}
//		private final int action;
//		private String object;
//		
//		public String toString() {
//			return action+" "+object;
//		}
//	}
//	
//	private void checkAdd(IUser u,int action,Object o) {
//		int before = -1;
//		List<DebugTest> old = null;
//		synchronized(debugList) {
//			old = debugList.get(u);
//			if (old == null) {
//				old = new ArrayList<DebugTest>();
//				debugList.put(u, old);
//			} else {
//				before = old.get(old.size()-1).action;
//			}
//			old.add(new DebugTest(action, o));
//		}
//		boolean allok = true;
//		switch(before) {
//		case -1:
//			allok = action == STATEMACHINE_CREATED || action == USER_IDENTIFIED_IN_CONNECTION;
//			break;
//		case USER_IDENTIFIED_IN_CONNECTION:
//			allok = action == TRANSFER_STARTED || action == CONNECTION_CLOSED;
//			break;
//		case TRANSFER_STARTED:
//			allok = action == TRANSFER_FINISHED;
//			break;
//		case TRANSFER_FINISHED:
//			allok = action == TRANSFER_STARTED ||  action == CONNECTION_CLOSED;
//			break;
//		case CONNECTION_CLOSED:
//			allok = action == USER_IDENTIFIED_IN_CONNECTION ||  action == STATEMACHINE_DESTROYED || action == STATEMACHINE_CHANGED;
//			break;
//		}
//		if (!allok) {
//			String prnt = "strange order: "+u.getNick();
//			for (int i = Math.max(0, old.size()-10);i < old.size(); i++) {
//				prnt +=";"+old.get(i);
//			}
//			logger.info(prnt);
//		}
//	}
	
	//private final Object synchStatus = new Object();

//	private final Observable<StatusObject> observable = new Observable<StatusObject>();

	private final PreferenceChangedAdapter pca;

	
	private class ServerSocketInfo {
		private final boolean encrypted;
		public ServerSocketInfo(boolean encrypted) {
			this.encrypted = encrypted;
		}
		private ServerSocketChannel ssc;
		private SelectionKey selKey;
		private volatile boolean running;
		
		int getPort() {
			return PI.getInt(encrypted ? PI.tlsPort: PI.inPort);
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


	private final DCClient dcc;


	/**
	 * 
	 */
	public ConnectionHandler(DCClient dcc) {
		this.dcc = dcc;
		
		//register a port changed listener with the settings
		pca = new PreferenceChangedAdapter(PI.get(),PI.inPort,PI.bindAddress,PI.tlsPort) {
			@Override
			public void preferenceChanged(String preference, String oldValue,String newValue) {
				if (PI.inPort.equals(preference) || PI.bindAddress.equals(PI.bindAddress)) {
					normal.changePort();
				} 
				if (PI.tlsPort.equals(preference) || PI.bindAddress.equals(preference)) {
					if (AbstractConnection.isTLSInitialized()) {
						tls.changePort();
					}
				}
			}
		};
	}
	
	void start() {
		register(normal);
		if (AbstractConnection.isTLSInitialized()) {
			register(tls);
		}
		pca.reregister();
		
		dcc.getPopulation().registerUserChangedListener(this);
	}
	
	void stop() {
		normal.close();
		tls.close();
		pca.dispose();
		dcc.getPopulation().unregisterUserChangedListener(this);
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
	
	
	/*
	 * on disconnect this is called by the connection to unregister itself..
	 * @param nmdcc - the protocol with a connection that just disconnected..
	 *
	public void removeCons(ClientProtocol nmdcc){
		logger.debug("called removeCons("+nmdcc+")");
	//	cons.remove(nmdcc);
		nmdcc.unregisterProtocolStatusListener(this);
	}
	*
	 * adds client client protocol to the list of collection
	 * @param nmdcc - an nmdcc protocol that has just finished connecting
	 *
	public void addCons(ClientProtocol nmdcc){
		logger.debug("called addCons("+nmdcc+")");
		nmdcc.registerProtocolStatusListener(this);
	} */
	

	

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
		expectedToConnect.add(new ExpectedInfo(target,protocol,token));
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
	public IUser getUserExpectedToConnect(String nick, InetAddress ip){
		ArrayList<IUser> possibleUsers = new ArrayList<IUser>(1);
		synchronized(expectedToConnect) {
			for (ExpectedInfo ei : expectedToConnect) {
				IUser usr = ei.getUser();
				if (nick.equals(usr.getNick())) {
					if (ip != null && ip.equals(usr.getIp())) {
						return usr;
					}
					possibleUsers.add(usr);
				}
			}
		}
		if (possibleUsers.isEmpty()) {
			return null;
		} else {
			return possibleUsers.get(GH.nextInt(possibleUsers.size()));
		}
	}
	
	/**
	 * TODO fix this... user determination should be done by token not by CID
	 * 
	 * @param id
	 * @return
	 */
	public ExpectedInfo getUserExpectedToConnect(HashValue id) {
		synchronized(expectedToConnect) {
			for (ExpectedInfo ei : expectedToConnect) {
				IUser usr = ei.getUser();
				if (id.equals(usr.getCID())) {
					return ei;
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
			ClientProtocol ctcp = new ClientProtocol( isa ,this , self,
					protocol,token , protocol.isEncrypted());
			clientProtocolCreated(ctcp);
		
	}
	
	
	/**
	 * starts created protocol and registers listener to it..
	 */
	private void clientProtocolCreated(ClientProtocol ctcp) {
		//ctcp.registerProtocolStatusListener(this);
		ctcp.start();
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
			DCClient.execute(new Runnable() { // done because if called by UI thread this will block.. TODO rem.. should never be called by UIThread..
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
						dcc.getConnectionDeterminator().connectionReceived(ssi.encrypted);
						created.configureBlocking(false);
						ClientProtocol cp = new ClientProtocol(created,ConnectionHandler.this,ssi.encrypted);
						clientProtocolCreated(cp);
					} catch(IOException ioe) {
						logger.error(ioe,ioe);
					}
				}

				public void setKey(SelectionKey key) {
					ssi.selKey = key;
				}

			});
			logger.info("Started "+(ssi.encrypted?"encrypted":"normal")+ " connection handler on TCP-Port: "+ssi.ssc.socket().getLocalPort());

		} catch(ClosedByInterruptException ie) {
			logger.debug("Connection handler socket closed by interruption");
			ssi.close();
		} catch(BindException be) {
			logger.error("TCP port in use! Change TCP port!",be);
			ssi.close();
		} catch (IOException e) {
			logger.warn("error in serversock "+e,e);
			ssi.close();
		} 

	}
	
	public void notifyOfChange(int detail,ClientProtocol cp,Object other) {
//		if (Platform.inDevelopmentMode()) {
//			IUser usr = null;
//			if (other instanceof ClientProtocolStateMachine) {
//				 usr = ((ClientProtocolStateMachine)other).getUser();
//			} else if (cp != null) {
//				usr = cp.getUser();
//			}
//			if (usr != null) {
//				checkAdd(usr,detail,cp);
//			}
//		}
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

	

	/*
	public void addObserver(IObserver<StatusObject> o) {
		observable.addObserver(o);
	}

	public void deleteObserver(IObserver<StatusObject> o) {
		observable.deleteObserver(o);
	} */

	/*
	 * listener implementation for status changes..
	 * 
	 * forwards events to the listeners registered with ConnectionHandler
	 * 
	 * 
	 *
	public void statusChanged(ConnectionState newStatus, ConnectionProtocol cp) {
		synchronized (synchStatus) {
			logger.debug("status changed called by client protocol: "+newStatus);
			if (cp instanceof ClientProtocol) {
				synchronized(active) {
					ClientProtocol conp = (ClientProtocol)cp;
					ClientProtocolStateMachine cpsm = conp.getStateMachine();
					switch(newStatus) {
					case CLOSED:
					case DESTROYED:
						active.remove(cp);
						notifyObservers(new StatusObject(cp,ChangeType.REMOVED));
						
				//		if (conp.getUser()!= null && conp.getUser().getNick().contains("list"))
				//			logger.info(newStatus+": "+conp.getUser());
						
						if (cpsm  != null) {
							active.add(cpsm);
							notifyObservers(new StatusObject(cpsm,ChangeType.ADDED));
					//		if (cpsm.getUser().getNick().contains("list"))
					//			logger.info(newStatus+" Added cpsm: "+cpsm.getUser());
						}
						break;
					case LOGGEDIN:
						if (cpsm  != null) {
							active.remove(cpsm);
							notifyObservers(new StatusObject(cpsm,ChangeType.REMOVED));
				//			if (cpsm.getUser().getNick().contains("list"))
				//				logger.info(newStatus+" Removed cpsm: "+cpsm.getUser());
						}
						active.add(cp);
						notifyObservers(new StatusObject(cp,ChangeType.ADDED));
				//		if (conp.getUser()!= null &&conp.getUser().getNick().contains("list"))
				//			logger.info(newStatus+": "+conp.getUser());
						break;
					case TRANSFERSTARTED:
						notifyObservers(new StatusObject(cp,ChangeType.CHANGED));
			//			if (conp.getUser()!= null &&conp.getUser().getNick().contains("list"))
			//				logger.info(newStatus+": "+conp.getUser());
						break;
					}
				}
			}
		}
	} */
	
/*	public void notifyObservers(StatusObject arg) {
		setChanged();
		super.notifyObservers(arg);
	} */

	
	public ClientProtocolStateMachine getStateMachine(IUser usr) {
		return interesting.get(usr);
	}
	

	
	public void removeStatemachine(IUser usr,ClientProtocolStateMachine cpsm) {
		if (usr == null) {
			throw new IllegalArgumentException();
		}
		//ClientProtocolStateMachine ccps = 
		interesting.remove(usr);
		/*if (ccps != null) {
			active.remove(ccps);
			notifyObservers(new StatusObject(ccps,ChangeType.REMOVED));
		} else {
			notifyObservers(new StatusObject(null,ChangeType.REMOVED)); //Trigger refresh.. //TODO shows error
			logger.debug("State machine was null: "+usr.getNick());
		} */
		notifyOfChange(STATEMACHINE_DESTROYED, null, cpsm);
		deleteObserver(cpsm);
	}
	
	public void addStatemachine(IUser usr,ClientProtocolStateMachine  ccps) {
		
		interesting.put(usr, ccps);
		addObserver(ccps);
		//active.add(ccps);
		//notifyObservers(new StatusObject(ccps,ChangeType.ADDED));
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



	/**
	 * bundles of information holding info for us of users that are expected
	 * to connect to us
	 * 
	 * @author Quicksilver
	 *
	 */
	public static class ExpectedInfo {
		
		private final IUser user;
		private final CPType protocol;
		private final String token;
		
		public ExpectedInfo(IUser user,CPType protocol, String token) {
			this.protocol = protocol;
			this.token = token;
			this.user = user;
		}

		public IUser getUser() {
			return user;
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
			if (user == null) {
				if (other.user != null)
					return false;
			} else if (!user.equals(other.user))
				return false;
			return true;
		}
		
		
	}

}



