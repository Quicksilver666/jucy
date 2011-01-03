package uc.protocols.client;


import helpers.GH;
import helpers.IObservable;
import helpers.PreferenceChangedAdapter;
import helpers.StatusObject;
import helpers.Observable.IObserver;



import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import logger.LoggerFactory;


import org.apache.log4j.Logger;





import uc.ConnectionHandler;
import uc.DCClient;
import uc.IHasUser;
import uc.IHub;
import uc.IUser;
import uc.IUserChangedListener;
import uc.Identity;
import uc.LanguageKeys;
import uc.PI;
import uc.files.IDownloadable;
import uc.files.IHasDownloadable;
import uc.files.transfer.FileTransferInformation;



/**
 * a StateMachine that will have some overview over the running connections of an user
 * 
 * it will do Bookkeeping of the states of an user of whom we want to download
 * also this will be used as model to show information  
 * 
 * @author Quicksilver
 *
 */
public class ClientProtocolStateMachine implements IObserver<StatusObject> ,IHasUser , IUserChangedListener, IHasDownloadable, Runnable {
	
	
	private static final Logger logger = LoggerFactory.make(); 
	private static volatile int MaxDOWNLOADS;
	
	static {
		new PreferenceChangedAdapter(PI.get(),PI.maxSimDownloads) {
			@Override
			public void preferenceChanged(String preference, String oldValue,
					String newValue) {
				updateMaxDown();
			}
		};
		
		updateMaxDown();
	}
	
	private static void updateMaxDown() {
		MaxDOWNLOADS = PI.getInt(PI.maxSimDownloads);
		if (MaxDOWNLOADS <= 0) {
			MaxDOWNLOADS = Integer.MAX_VALUE;
		}
	}
	
	
	/**
	 * when ever the protocol waits for some x seconds .. this will be used for counting
	 * so it can be shown in the gui
	 */
	private volatile int sleepCounter;
	
	private volatile boolean running = true;
	
	
	/**
	 * a task that is to be executed when the sleep counter reaches zero..
	 */
	private volatile Runnable sleepTask; 
	
	
	
	/**
	 * 
	 * antipattern? remmove?
	 *
	 */
	public static class CPSMManager {
		private final CopyOnWriteArrayList<ClientProtocolStateMachine> all =   
			new CopyOnWriteArrayList<ClientProtocolStateMachine>();
		
		private final DCClient dcc;
		private ScheduledFuture<?> task;
		public CPSMManager(DCClient dcc) {
			this.dcc = dcc;
		}
		
		public void start() {
			task = dcc.getSchedulerDir().scheduleAtFixedRate(new Runnable() {
				public void run() {
					for (ClientProtocolStateMachine m: all) {
						synchronized(m) {
							if (--m.sleepCounter < 0 && m.sleepTask != null) {
								Runnable r = m.sleepTask;
								m.sleepTask	= null; 
								dcc.executeDir( r ); 
							}
							
							if (m.sleepCounter % 10 == 0) {
								logger.debug("sleepCounter: "+ m.sleepCounter+"  "+m.user);
							}	
						}
					}
				}
			}
			, 1, 1 , TimeUnit.SECONDS);
		}
		
		public void stop() {
			if (task != null) {
				task.cancel(false);
			}
		}

		public boolean remove(Object o) {
			return all.remove(o);
		}

		public boolean addIfAbsent(ClientProtocolStateMachine element) {
			return all.addIfAbsent(element);
		}

		public int size() {
			return all.size();
		}	
	}
	


	
	private final IUser user;
	

	/**
	 * the connection handler..
	 * so we can unregister us there when the user is no longer interesting..
	 */
	private final ConnectionHandler ch;
	
	/**
	 * currently active client protocol
	 * null if none..
	 */
	private ClientProtocol current = null;
	
	/**
	 * describes what file has been downloaded last
	 */
	private FileTransferInformation lastDownload = null;
	
	/**
	 * here the current state is stored
	 */
	private volatile CPSMState state = CPSMState.IDLE;
	
	private final String token;
	


	/**
	 * 
	 * @param usr - the user from which we want to download.
	 */
	public ClientProtocolStateMachine(IUser usr,ConnectionHandler ch) {
		this.user = usr;
		this.ch = ch;
		logger.debug("created statemachine: "+ch.getCpsmManager().size());
		token = Integer.toHexString(Math.abs(GH.nextInt()));
		ch.getDCC().getPopulation().registerUserChangedListener(this, usr.getUserid());
		ch.addStatemachine(usr, this);
	}
	
	
	
	
	public void update(IObservable<StatusObject> o, StatusObject arg) {
		if (arg.getDetailObject() == this) { //check if notification is for us
			logger.debug("calling statemachine: "+user);
			switch(arg.getDetail()) {
			case ConnectionHandler.USER_IDENTIFIED_IN_CONNECTION:
				current = (ClientProtocol)arg.getValue(); 
				sleepTask = null; //clear sleeptask..
				ch.getCpsmManager().remove(this);
				break;
			case ConnectionHandler.CONNECTION_CLOSED:
			case ConnectionHandler.STATEMACHINE_CREATED:
				if (current != null) {
					lastDownload = current.getFti();
					setState(CPSMState.CLOSED);
					sleepCounter = ((ClientProtocol)current).isImmediateReconnect()? 0 : 60 ;
				} else {
					sleepCounter = 0;
				}
				logger.debug("sleep counter is set to: "+sleepCounter);
				sleepTask = this;
				ch.getCpsmManager().addIfAbsent(this);
				break;
			}
			if (!user.weWantSomethingFromUser() || !user.isOnline()) { //remove immediately after disconnect..if nothing more is needed..by this user..
				logger.debug("removeing statemachine1: "+user);
				remove();
			} 
			
		}
		
	}

	public synchronized void run() {
		final IHub hub = user.getHub();
		Identity id = hub != null? hub.getIdentity(): null;
		if (getState() == CPSMState.WAITING_FOR_CONNECT && id != null && id.isActive() ) { 
			
			id.getConnectionDeterminator().connectionTimedOut(user,
				user.hasSupportForEncryption() &&  id.currentlyTLSSupport());
			
		}

		logger.debug("StateMachine. executing sleeptask: "+ch.getCpsmManager().size());
		//TODO user.resolveDQE returns null if user has download running.. this is unclear..
		if (null == user.resolveDQEToUser() || ch.getNrOfRunningDownloads() >= MaxDOWNLOADS) {
			logger.debug("no DQE found.." +user);
			if (user.weWantSomethingFromUser()) { // if we want nothing ..we break out of this
				//if we want something.. but not now..just go to sleep
				sleepCounter = 10;
				sleepTask	= this;
				setState(CPSMState.IDLE);

			} else {
				remove();
			}

		} else {
			// if we need something send a ctm or rcm
			logger.debug("StateMachine found item: "+user + " dqe: ");
	
			if (hub != null && user.isOnline()) {
				hub.requestConnection(user, token);

				//we expect a connect.. if don't get one.. redo this
				sleepCounter = 60;
				sleepTask 	= this;
				setState(CPSMState.WAITING_FOR_CONNECT);

			} else {
				setState(CPSMState.USER_OFFLINE);
				remove();
			}
		}

	}


	public IDownloadable getDownloadable() {
		FileTransferInformation fti = getLastDownload();
		if (fti != null && fti.getDqe() != null) {
			return fti.getDqe().getDownloadable();
		}
		return null;
	}

	/*
	 * when ever the status of the connection changes..
	 * delete the sleepTask..
	 * and set a new appropriate task
	 * usually used for timeout.. and reconnecting..
	 *
	public synchronized void statusChanged(ConnectionState newStatus, ConnectionProtocol cp) {
		logger.debug("called statusChanged on CPStatemeachine");
		sleepTask = null;
		
		switch(newStatus) {
		case LOGGEDIN:
			logger.debug("called LOGeDIN");
			if (cp instanceof ClientProtocol) {
				current = (ClientProtocol)cp; 
			}								 
			break;
		case TRANSFERSTARTED:
			logger.debug("called TRANSFERRUNNING");
			break;
		case DESTROYED:
			if (current != null) {
				lastDownload = current.getFti();
				setState(CPSMState.CLOSED);
			}
			
			logger.debug("called CLOsED");
			//normally sleep 60 seconds.. but connect immediately if wished
			if (cp instanceof ClientProtocol) {
				sleepCounter = ((ClientProtocol)cp).isImmediateReconnect()? 0 : 60 ;
			} else {
				sleepCounter = 0;
			}
			logger.debug("sleep counter is set to: "+sleepCounter);
			
			
			sleepTask = new Runnable() {
				public void run() {
					synchronized(ClientProtocolStateMachine.this) {
						
						if (getState() == CPSMState.WAITING_FOR_CONNECT && ch.getDCC().isActive()) { 
							//if this is called in waiting for connect it means no 
							//successful connection was created..
							//TODO may be wrong if an upload was running..
							ch.getDCC().getConnectionDeterminator().connectionTimedOut(user,
									user.hasSupportFoEncryption() && ch.getDCC().currentlyTLSSupport());
						}
						
						logger.debug("StateMachine. executing sleeptask: "+all.size());
						if (null == user.resolveDQEToUser() || ch.getNrOfRunningDownloads() >= MaxDOWNLOADS) {
							logger.debug("no DQE found.." );
							if (user.weWantSomethingFromUser()) { // if we want nothing ..we break out of this
								//if we want something.. but not now..just go to sleep
								sleepCounter = 10;
								sleepTask	= this;
								setState(CPSMState.IDLE);
								
							} else {
								remove();
							}

						} else {
							 // if we need something send a ctm or rcm
							logger.debug("StateMachine found item");
							IHub othershub = user.getHub();
							if (othershub != null && user.isOnline()) {
								boolean nmdc = othershub.isNMDC();
								boolean encryption = user.hasSupportFoEncryption() && 
															ch.getDCC().currentlyTLSSupport();
								
								CPType protocol = CPType.get(encryption, nmdc); 
								

								if (ch.getDCC().isActive()) {
									logger.debug("sending CTM "+user+ 
											"  " + protocol+"  "+
											ch.getDCC().getConnectionDeterminator().
												getPublicIP().getHostAddress());
									
									othershub.sendCTM(user, protocol ,token);
								} else {
									logger.debug("sending RCM "+user);
									othershub.sendRCM(user, protocol ,token);
								}
								
								//we expect a connect.. if don't get one.. redo this
								sleepCounter = 60;
								sleepTask 	= this;
								setState(CPSMState.WAITING_FOR_CONNECT);
								
							} else {
								setState(CPSMState.USER_OFFLINE);
								remove();
							}
						}
					}
				}
			};
			break;	
		}
		
		if (!user.weWantSomethingFromUser() || !user.isOnline()) { //remove immediately after disconnect..if nothing more is needed..by this user..
			remove();
		}
		
	} */
	
	
	









	/**
	 * just removes user when he disconnects..
	 */
	public void changed(UserChangeEvent uce) {
		switch (uce.getType()) {
		case CHANGED:
			switch(uce.getDetail()) {
			case UserChangeEvent.DOWNLOADQUEUE_ENTRY_POST_REMOVE_LAST:
				remove();
				break;
			}
			break;
		case QUIT:
			remove();
			break;
		}
	} 

	/**
	 * disable this StateMachine
	 * usage is calling when here should no longer be done any downloads with the user..
	 *
	 */
	private synchronized void remove() {
		if (ch.getCpsmManager().remove(this) ) {
			running = false;
			ch.removeStatemachine(user,this);
			ch.getDCC().getPopulation().unregisterUserChangedListener(this, user.getUserid());
			logger.debug("removed statemachine");
		}
	}
	
	public  boolean isActive() {
		return running;
	}

	/**
	 * the user for this StateMachine
	 */
	public IUser getUser() {
		return user;
	}

	public ClientProtocol getCurrent() {
		return current;
	}

	public void clearTime() {

		sleepCounter = 0;
		if (sleepTask == null) {
			logger.warn("sleep task failure");
			/*DCClient.execute(new Runnable() {
				public void run() {
					statusChanged(ConnectionState.CLOSED, null);
				}
			});*/
		} 

	}
	
	/**
	 * 
	 * @return true if a ConnectionProtocol is running
	 * for this StateMachine
	 */
	public boolean hasConnectionProtocol() {
		return current != null;
	}

	/**
	 * 
	 * @return last FileTransfer that was done,,
	 */
	public FileTransferInformation getLastDownload() {
		return lastDownload;
	}
	
	public static enum CPSMState {
		IDLE(LanguageKeys.Idle),
		USER_OFFLINE(LanguageKeys.UserOffline),
		WAITING_FOR_CONNECT(LanguageKeys.WaitingForConnect),
		CLOSED(LanguageKeys.Closed);
		
		CPSMState(String languageKey) {
			this.languageKey = languageKey;
		}
		
		private final String languageKey;
		
		public String toString() {
			return languageKey;
		}
	}


	public String getToken() {
		return token;
	}

	public CPSMState getState() {
		return state;
	}

	private void setState(CPSMState state) {
		this.state = state;
		ch.notifyOfChange(ConnectionHandler.STATEMACHINE_CHANGED, null, this);
		
		//ch.notifyObservers(new StatusObject(this,ChangeType.CHANGED));
	}

	public int getSleepCounter() {
		return sleepCounter;
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
		final ClientProtocolStateMachine other = (ClientProtocolStateMachine) obj;
		if (user == null) {
			if (other.user != null)
				return false;
		} else if (!user.equals(other.user))
			return false;
		return true;
	}
	
}