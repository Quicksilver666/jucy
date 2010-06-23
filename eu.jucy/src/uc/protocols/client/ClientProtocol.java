package uc.protocols.client;

import uc.ConnectionHandler;
import uc.DCClient;
import uc.IHasUser;
import uc.IUser;



import uc.crypto.HashValue;
import uc.files.IDownloadable;
import uc.files.IHasDownloadable;
import uc.files.downloadqueue.AbstractDownloadQueueEntry;
import uc.files.transfer.AbstractFileTransfer;
import uc.files.transfer.FileTransferInformation;
import uc.files.transfer.IFileTransfer;

import uc.files.transfer.Slot;
import uc.protocols.ADCStatusMessage;
import uc.protocols.CPType;
import uc.protocols.ConnectionState;
import uc.protocols.DCProtocol;
import uc.protocols.IConnection;
import uc.protocols.TransferType;
import uc.protocols.UnblockingConnection;
import uc.protocols.hub.Flag;




import helpers.GH;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;


import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.nio.channels.ByteChannel;
import java.nio.channels.SocketChannel;

import logger.LoggerFactory;




import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.Platform;





/**
 * 
 * @author Quicksilver A class standing for a client to client
 *         connectionProtocol in NMDC
 * 
 * 
 */
public class ClientProtocol extends DCProtocol implements IHasUser, IHasDownloadable {

	private static final Logger logger = LoggerFactory.make(); 


	private volatile int timerLogintimeout = 0;

	private volatile int timerTransferCreateTimeout = 0;
	
	private volatile int awaitingADCGet = 0;
	
	private volatile boolean getAwaited = false;
	
	private final ConnectionHandler ch;

	private final boolean incoming;

	//private UnblockingConnection normal;

	private volatile String disconnectReason = null;
	
	/**
	 * if CPSM is applicable for this connection here it is stored
	 * from identifying other user to close of connection
	 */
	private ClientProtocolStateMachine cpsm; 

	private IUser self;

	private final List<String> debugMessages = Collections.synchronizedList(new ArrayList<String>()); 

	/**
	 * if we should immediately reconnect true if
	 * for example a download was successful
	 */
	private boolean immediateReconnect = false;

	private boolean newList;

	private volatile Slot slot;

	private final int myNumber = GH.nextInt(0x7FFF);

	private int othersNumber = -1; // refers to the number of the other user in
									// the competition who may download...



	private volatile InetAddress otherip;
	
	private Set<String> othersSupports = new HashSet<String>(); // The SupportString of the other side  

	
	/**
	 * information that is retrieved will be set to this ..
	 */
	private final FileTransferInformation fti;
	
	/**
	 * if a FileTransfer is running.. then it is referenced here..
	 */
	private volatile AbstractFileTransfer fileTransfer = null;
	
	//counter on what was last requested: if same file gets requested to often -> say we don't have 
	
	private long lastRequestpos = -1;
	private int lastRequestCounter= 0;
	
	//NMDC data
	private static final int loginTargetLevel = 6;
	
	/**
	 * com variable .. to make sure everything required was received
	 * things needed for Login: received: Sent/Received Direction + Sent/Received Supports + Sent/Received Key
	 * nr of things received if this reaches 6 login has finished
	 */
	private int loginLevel =   0;
	
	
	// ADC data
	
	/**
	 * the token responsible for this connection
	 */
	private volatile String token;

	/**
	 * hubaddress told to us via REF in NMDC ...
	 * -> used to help finding out who connected to us.
	 */
	private volatile String hubaddy;



	/**
	 * incoming .. constructor 
	 */
	public ClientProtocol(SocketChannel sc, ConnectionHandler ch,boolean encryption) {
		this((Object)sc,true,ch,null,null,null,null,encryption);
	}
	
	/**
	 * constructor for outgoing connections
	 * if we connect ourself we know in which hub we got the CTM and therefore
	 * know the self
	 * 
	 * @param addy -> target
	 * @param ch
	 * @param self
	 * @param protocol
	 * @param token
	 * @param encryption
	 */
	public ClientProtocol(InetSocketAddress addy, ConnectionHandler ch,IUser self,IUser other,
			CPType protocol,String token,boolean encryption) {
		this((Object)addy,false,ch,self,other,protocol,token,encryption);
	}
	
	private ClientProtocol(Object addyOrSC ,boolean incoming, ConnectionHandler ch,IUser self,IUser other,
			CPType protocol,String token,boolean encryption) {
		super(new int[]{0,0,1}); //we want bandwidth mostly C-C protocol..
		this.ch = ch;
		fti = new FileTransferInformation(ch.getDCC());
		this.incoming = incoming; 
		this.self = self; // our self the user we represent in that hub may be null
		othersNumber = 0; // others number -> NMDC data..
		immediateReconnect = false;

		setProtocolNMDC(protocol == null? null : protocol.isNmdc());
		this.token = token;
		
		Assert.isTrue(incoming || self != null );
		Assert.isTrue(incoming || protocol.isNmdc() || token != null  , " token should not be null for ADC connections");
		
		HashValue fingerPrint = other != null? other.getKeyPrint(): null;

		if (addyOrSC instanceof SocketChannel) { 
			connection = new UnblockingConnection(ch.getIdentity().getCryptoManager(), (SocketChannel)addyOrSC, this,encryption,incoming,fingerPrint);
		} else {
			connection = new UnblockingConnection(ch.getIdentity().getCryptoManager(),(InetSocketAddress)addyOrSC, this,encryption,fingerPrint);
		}
	}
	
	public DCClient getDcc() {
		return ch.getDCC();
	}

	public void start() {
		WriteLock l = ClientProtocol.this.writeLock();
		l.lock();
		try {
			connection.start();
			super.start();
		} finally {
			l.unlock();
		}
	}

	public void beforeConnect() {
		logger.debug("called beforeConnect");
		super.beforeConnect();
		//state = connecting;
		disconnectReason = null;
		if (getNMDC() == null) {
			addCommand(new MyNick(this),new Lock(this),new Error(this),new NMDCGet(this),new UGetBlock(this));
			addCommand(new SUP(this),new STA(this));
		} else if (isNMDC()) {
			addCommand(new MyNick(this),new Lock(this),new Error(this),new NMDCGet(this));
		} else {
			addCommand(new SUP(this),new STA(this));
		}
	}

	public void onConnect() throws IOException {
		logger.debug("called OnConnect");
		InetSocketAddress isa = null;
		
		isa = connection.getInetSocketAddress();
		if (isa == null) {
			throw new IOException("Socketaddress not set");
		}
		super.onConnect();
		
		otherip = isa.getAddress();
		
		Assert.isNotNull(otherip);
		
		//ch.addCons(this);
		
		
		if (!incoming) {
			if (nmdc) { //outgoing therefore the protocol is already set..
				MyNick.sendMyNickAndLock(this);
			} else {
				SUP.sendSUP(this);
			}
		}
	}

	@Override
	public void receivedCommand(String command) throws IOException,ProtocolException {
		logger.debug("received command: "+command);
		debugMessages.add("in: "+command);
		super.receivedCommand(command);
	}

	/**
	 * OnLogin() called when the other is identified and we are ready to send an GET
	 * 
	 * from here on is guaranteed the other User is set.. also direction is already
	 * determined
	 * 
	 * if we are downloading here is the place to send ADCGET else we will do
	 * nothing and just wait for the other to send something
	 */
	public void onLogIn() throws IOException {
		logger.debug("called OnLogIn ");

		if (getState() != ConnectionState.CONNECTED) {
			logger.debug("current state: "+getState()); //no login -> we already disconnected..TODO -> move into superclass..
			throw new IOException("Illegal state for login");
		}
		super.onLogIn();
		

		if (fti.isDownload()) { // Here call the connection handler to know what
								// we want... if we want something..
	
			// register state machine.. so it gets notified when this connection closes  
			cpsm = ch.getStateMachine(fti.getOther());
			if (cpsm == null) {
				disconnect(DisconnectReason.UNKNOWN);
				return;
			} 
			
		//	super.onLogIn();
			
			
			//fill in all FTI information
			AbstractDownloadQueueEntry adqe = fti.getDqe();
			if (adqe == null) {
				throw new IOException("Protocol in invalid state");
			}
			fti.setType(adqe.getType());
			
			
			if (adqe.getType() == TransferType.FILE) {
				fti.setNameOfTransferred(fti.getDqe().getFileName());
			}
			
			boolean otherSupportsCompression = othersSupports.contains("ZLIG");
			
			fti.setCompression( connection.isLocal(), otherSupportsCompression );
			WriteLock l = ClientProtocol.this.writeLock();
			l.lock();
			try {
				ch.notifyOfChange(ConnectionHandler.USER_IDENTIFIED_IN_CONNECTION, this, cpsm); //new ChangeNotification
			} finally {
				l.unlock();
			}
			logger.debug("trying to get a downlaod from dqe");
	
			if (adqe.getDownload(fti)) { //fill in interval and HashValue..
				logger.debug("got download sending ADCGET");
				try {
					if (nmdc) {
						ADCGET.sendADCGET(this);
					} else {
						GET.sendGET(this);
					}
				} catch (IOException ioe) {
					logger.debug(ioe);
					disconnect(ioe.getMessage() != null? ioe.getMessage():DisconnectReason.UNKNOWN.toString());
				}
			} else {
				logger.debug("disconnecting: IllegalState in DQE");
				//what do now? nothing filled in.. may be just disconnect..
				//probably reason for this is that the file was just finished..
				immediateReconnect = true;
				disconnect(DisconnectReason.ILLEGALSTATEERROR);
			}
			
			
		} else {
		//	super.onLogIn();
			cpsm = null;
			WriteLock l = ClientProtocol.this.writeLock();
			l.lock();
			try {
				ch.notifyOfChange(ConnectionHandler.USER_IDENTIFIED_IN_CONNECTION, this, null);
			} finally {
				l.unlock();
			}
		}
		

	}
	
	/**
	 * called to simulate an anew
	 * login used to have multiple transfers on one connection
	 */
	private void relogin() {
		logger.debug("relogin");
		DCClient.execute(new Runnable() {
			public void run() {
				WriteLock l = ClientProtocol.this.writeLock();
				l.lock();
				try {
					if (fti.isDownload()) {
						AbstractDownloadQueueEntry dqe = fti.getOther()
								.resolveDQEToUser();
						if (dqe != null) {
	
							fti.setDqe(dqe);
							fti.setType(fti.getDqe().getType());
							
							if (fti.getDqe().getType() == TransferType.FILE) {
								fti.setNameOfTransferred(fti.getDqe().getFileName());
							} 
	
							if (fti.getDqe().getDownload(fti)) { //fill in interval and HashValue..
								logger.debug("got download sending ADCGET");
								try {
									if (nmdc) {
										ADCGET.sendADCGET(ClientProtocol.this);
									} else {
										GET.sendGET(ClientProtocol.this);
									}
								} catch (IOException ioe) {
									disconnect(DisconnectReason.CONNECTIONTIMEOUT);
								}
							}
						} else {
							connection.close();
						}
					}
				} finally {
					l.unlock();
				}
			}
		});
	}
	
	

	public void onDisconnect() throws IOException {
		logger.debug("called OnDisconnect "+fti.getOther());
		super.onDisconnect();
		ch.notifyOfChange(ConnectionHandler.CONNECTION_CLOSED,this,cpsm); 
		
		if (fti.getOther() != null) {
			fti.getOther().deleteConnection(this);
		}

		//ch.removeCons(this);
		
		end(); //never reuse..
		if (slot != null) {
			ch.getSlotManager().returnSlot(slot,fti.getOther());
			if (Platform.inDevelopmentMode()) {
				logger.warn("slot returned too late: "+getUser());
			}
		}
	}
	
	/**
	 * called when other client tells that
	 * there are no free slots available.
	 * 
	 * @param additionalMessage  should be empty if none..
	 */
	void noSlotsAvailable(String additionalMessage) {
		if (GH.isNullOrEmpty(additionalMessage)) {
			disconnect(DisconnectReason.NOSLOTS);
		} else {
			disconnect(DisconnectReason.NOSLOTS+" "+additionalMessage);
		}
	}
	
	
	@Override
	protected void onUnexpectedCommandReceived(String command) {
		//super.onUnexpectedCommandReceived(command);
		logger.debug("Unexpected comamnd: "+command);
	}
	
	protected void onMalformedCommandReceived(String command) {
		logger.debug("Malformed comamnd: "+command+"  "+ getUser() != null? getUser():"");
	}



	/**
	 * @param otherwantsToDownload if the other requests to download -(does not matter for ADC)
	 */
	void setDownload(boolean otherwantsToDownload) throws IOException {
		if (othersNumber == -1) {
			throw new IllegalStateException("this may not be called before others number is set");
		}
		
		if (nmdc) {
			if (otherwantsToDownload) {
				fti.setDownload(fti.getDqe() != null && myNumber > othersNumber);
			} else {
				fti.setDownload(fti.getDqe() != null);
			}
		} else {
			ClientProtocolStateMachine cpsm = getCh().getStateMachine(fti.getOther());
			fti.setDownload(cpsm != null && cpsm.getToken().equals(token));
		}
		
		if (fti.isUpload()) {//if this is an upload we will accept ADCGET commands..
			getAwaited = true;
			if (nmdc) {
				addCommand(new ADCGET(this)); 	
			} else {
				addCommand(new GET(this));
			}
		}
		increaseLoginLevel();
	}
	
	
	
	/**
	 * if one of the 3 needed things occur this is called
	 * so onLogin can be triggered..
	 * 
	 * @throws IOException
	 */
	void increaseLoginLevel() throws IOException {
		loginLevel++;
		if (loginLevel == loginTargetLevel) {
			onLogIn();
		}
	}
	
	

	/**
	 * queues a raw message for sending to the client
	 * @param message
	 */
	void sendUnmodifiedRaw(final String message)  {
		super.sendRaw(message);
		debugMessages.add("out: "+message);
		logger.debug("sent raw: "+message);
	}
	
	void sendUnmodifiedRaw(byte[] bytes) {
		super.sendRaw(bytes);
	}
	
	/**
	 * closes the connection and prints
	 * DisconnectReason to the screen
	 * @param reason - why we closed the connection
	 */
	public  void disconnect(DisconnectReason reason) {
		disconnect(reason.toString());
	}
	
	/**
	 * the statis message already sent
	 * @param adcs - 
	 */
	public void disconnect(ADCStatusMessage adcs) {
		disconnect(adcs.toString());
	}
	
	private void disconnect(String sreason) {
		if (getUser() != null) {
			logger.debug("disconnected: "+getUser().getNick()+"  reason: "+sreason);
		}
		disconnectReason = sreason;
		if (connection != null) {
			logger.debug("closing connection: "+disconnectReason);
			if (fileTransfer != null) {
				fileTransfer.cancel();
			} else {
			//	connection.flush(500); //for up to half a second we try flushing.. so MaxedOut can get through
				logger.debug("closed Connection: "+connection.getClass().getSimpleName());
				connection.close();
			}
		}
	}

	public void otherSentError(String reason) {
		this.disconnectReason = reason;
		if (connection != null) {
			connection.close();
		}
	}

	/**
	 * sends an error containing the provided disconnect reason 
	 * to the other user afterwards disconnect(reason)
	 * is called
	 * 
	 * @param error - what error occurred
	 * @throws IOException
	 */
	void sendError(DisconnectReason error) throws IOException {
		if (!error.isError()) {
			throw new IllegalArgumentException(
					"error must be an error not just an ordinary disconnectreason"+error);
		}
		if (nmdc) {
			Error.sendError(this, error);
		} 
		disconnect(error);
	}

	
	/**
	 * called by protocol commands when the other user is identified
	 * sets the other user and tries to resolve a
	 * DownloadQueueEntry to this user.
	 * @param other
	 */
	void otherIdentified(IUser other) {
		if (isEncrypted() && other.getKeyPrint() != null) {
			connection.setFingerPrint(other.getKeyPrint());
		}
		fti.setOther(other);
		if (other != null) {
			self = other.getHub().getSelf();
			
			AbstractDownloadQueueEntry dqe = other.resolveDQEToUser();
			fti.setDqe(dqe);
			other.setIp(otherip);
			other.addTransfer(this);
		}
	}
	
	
	public IUser getUser() {
		return fti.getOther();
	}



	/**
	 * starts an download  or a download
	 * if everything in FTI is set
	 */
	void transfer() throws IOException {
		logger.debug("in transfer()");

		boolean successful = fti.setFileInterval();
		long current = fti.getStartposition();
		if (current != 0 && current == lastRequestpos) {
			if (++lastRequestCounter > 10) {
				successful = false; //user tried to re-download the same part to often -> may be we have different Hash for it -> tell him we don't have that..
				logger.debug("not available due lastrequest failed timeout "+fti);
			}
		} else {
			lastRequestpos = current;
			lastRequestCounter = 0;
		}

		if (successful) {
			getSlotAndDoTransfer();
		} else {
			//could no create fileInterval -> file is not available
			if (nmdc) {
				sendError( DisconnectReason.FILENOTAVAILABLE );
			} else {
				STA.sendSTA(this, 
						new ADCStatusMessage(DisconnectReason.FILENOTAVAILABLE.toString(),
								ADCStatusMessage.FATAL,
								ADCStatusMessage.TransferFileNotAvailable));
			}
		}

	}
	
	private void getSlotAndDoTransfer() {
		DCClient dcc = ch.getDCC();
		if (fti.isDownload() || (slot = ch.getSlotManager().getSlot(fti.getOther(),fti.getType(), fti.getFileListFile())) != null) { //get a slot for the upload
			ByteChannel source = null;
			try {
				if (fti.isUpload()) {
					getAwaited = false;
					
					logger.debug("transfer() is an upload..now sending ADCSND");
					if (nmdc) {
						ADCSND.sendADCSND(this);
					} else {
						SND.sendADCSND(this);
					}
					dcc.getUpQueue().userRequestedFile(
							fti.getOther(), fti.getNameOfTransferred(),fti.getHashValue(), true);
				} 

				fileTransfer = fti.create(this);
				if (getState().isOpen()) {
					setState(ConnectionState.TRANSFERSTARTED);
				} else {
					if (Platform.inDevelopmentMode()) {
						logger.warn("Problem transfering: "+getUser()+ "  "+getState());
					}
					throw new IOException();	
				}
				ch.notifyOfChange(ConnectionHandler.TRANSFER_STARTED,this,fileTransfer);
				
				if (fti.isDownload()) { //Register download.. with DQE..
					fti.getDqe().startedDownload(fileTransfer);
				}
				logger.debug("transfer() start transferring data..");
				source = connection.retrieveChannel();
				fileTransfer.transferData(source); 
				logger.debug("transfer() finished transferring data..");
				immediateReconnect = true; 
				
			} catch(IOException ioe) { 
				logger.debug("transfer broke with ioexception "+ioe);
			} catch(IllegalStateException ise) {
				logger.log(Platform.inDevelopmentMode()?Level.WARN:Level.DEBUG, "stupid state exception",ise);
			} finally {
				
				boolean wasDownload = false ;

				if (fileTransfer != null) {
					wasDownload = fileTransfer.isDownload();
					dcc.getUpDownQueue(fileTransfer.isUpload()).transferFinished(
							fti.getFile(),
							fti.getOther(), 
							fti.getNameOfTransferred(), 
							fti.getHashValue(), 
							fileTransfer.getBytesTransferred(),
							fileTransfer.getStartTime(),
							System.currentTimeMillis()-fileTransfer.getStartTime().getTime());
					
					ch.notifyOfChange(ConnectionHandler.TRANSFER_FINISHED,this,fileTransfer);
					if (getState().isOpen()) {
						setState(ConnectionState.TRANSFERFINISHED);
					} else	if (Platform.inDevelopmentMode()) {
						logger.warn("Problem after transfering: "+getUser()+ "  "+getState());
					}
					timerTransferCreateTimeout = 0;
					fileTransfer = null;
				}

				boolean returnSuccessful = false;
				if (source != null) {
					returnSuccessful = connection.returnChannel(source);
				}
				if (debugMessages.size() > 2000) {
					//check error... too many debug messages..
					if (Platform.inDevelopmentMode()) {
						logger.warn("Debug Messages count too large "+debugMessages.size());
					}
					connection.close();
				} else if (slot != null) { //upload
					ch.getSlotManager().returnSlot(slot,fti.getOther());
					slot = null;
					awaitingADCGet = 10;
					getAwaited = true;
				} else if (wasDownload && returnSuccessful && getState().isOpen()) { // download:
					logger.debug("relogin");
					relogin();
				}
				
				logger.debug("transfer() finished was download"+wasDownload+"  return succ:"+returnSuccessful);
			}
			
		} else {
			//being here means this is an upload and no slots are free.
			//send No slots signal
			int queuePosition = ch.getSlotManager().getPositionInQueue(fti.getOther());
			if (nmdc) {
				MaxedOut.sendMaxedOut(this,queuePosition);
			} else {
				STA.sendSTA(this, 
						new ADCStatusMessage(DisconnectReason.NOSLOTS.toString()
											,ADCStatusMessage.FATAL
											,ADCStatusMessage.TransferSlotsFull
											,Flag.QP,""+queuePosition));
				
				//here ADC no slots.. //STA maxed out..
			}
			dcc.getUpQueue().userRequestedFile(fti.getOther(), fti.getNameOfTransferred(),fti.getHashValue(), false);
		}
	}
	


	/**
	 * timer used to
	 */
	public void timer() {

		if (!isLoginDone() && ++timerLogintimeout > 60) { // 60 seconds login timeout
			logger.debug("disconnect sent from here");
			disconnect(DisconnectReason.CONNECTIONTIMEOUT);
		}
		
		// 60 seconds Timeout until a transfer needs to be created.. else we disconnect..
		if ((fileTransfer == null|| !fileTransfer.hasStarted())   && ++timerTransferCreateTimeout > 30) { 
			logger.debug("disconnect sent from here2");
			disconnect(DisconnectReason.CONNECTIONTIMEOUT);
		}
		
		//timeout for if we expect an ADCGet  from the other but don't get one..
		if (getAwaited && ++awaitingADCGet > 20) {
			logger.debug("disconnect sent from here3");
			disconnect(DisconnectReason.CONNECTIONTIMEOUT);
		}
	}


	/**
	 * @return the disconnectReason
	 */
	public String getDisconnectReason() {
		return disconnectReason;
	}
	

	public InetAddress getOtherip() {
		return otherip;
	}

	/**
	 * 
	 * @return true if on our side was the serversocket
	 */
	boolean isIncoming() {
		return incoming;
	}




	ConnectionHandler getCh() {
		return ch;
	}

	/**
	 * @return the User representing us in the hub of the other..
	 */
	IUser getSelf() {
		return self;
	}

	/**
	 *  lottery number that decides who uploads and who Downloads
	 *  who ever has the higher number will download if both are 
	 *  interested in downloading
	 */
	int getMyNumber() {
		return myNumber;
	}

	void setOthersSupports(Set<String> othersSupports) throws IOException {
		this.othersSupports = othersSupports;
		increaseLoginLevel();
	}

	void setOthersNumber(int othersNumber) {
		this.othersNumber = othersNumber;
	}

	boolean isLocal() {
		return connection.isLocal();
	}

	public FileTransferInformation getFti() {
		return fti;
	}

	public IFileTransfer getFileTransfer() {
		return fileTransfer;
	}
	
	public IConnection getConnection() {
		return connection;
	}

	boolean isImmediateReconnect() {
		return immediateReconnect;
	}

	Set<String> getOthersSupports() {
		return othersSupports;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
	
	public boolean isNewList() {
		return newList;
	}



	public void setPartialList(boolean newList) {
		this.newList = newList;
	}
	
	public String toString() {
		if (getUser() == null) {
			if (getOtherip() != null) {
				return "Connection with: "+getOtherip().toString();
			} else {
				return "unknown connection";
			}
		} else {
			return "Connection with: "+getUser().toString();
		}
	}

	public IDownloadable getDownloadable() {
		if (getFti() != null) {
			//for downloads
			if (getFti().getDqe() != null) {
				return getFti().getDqe().downloadableData();
			}
			
			//for uploads
			if (getFti().getHashValue() != null) {
				return ch.getDCC().getOwnFileList().search(getFti().getHashValue());
			}
		
		}
		
		return null;
	}
	
	public String getHubaddy() {
		return hubaddy;
	}

	public void setHubaddy(String hubaddy) {
		this.hubaddy = hubaddy;
	}
	
	

}
