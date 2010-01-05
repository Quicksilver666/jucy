package uc;

import java.net.InetAddress;

import uc.crypto.HashValue;
import uc.files.downloadqueue.AbstractDownloadQueueEntry;
import uc.files.downloadqueue.FileListDQE;
import uc.files.filelist.FileListDescriptor;
import uc.protocols.client.ClientProtocol;



/**
 * interface to wrap User
 * 
 * @author Quicksilver
 *
 */
public interface IUser  {

	/**
	 * constants for slotgrants..
	 */
	public static final long 	UNTILFOREVER = Long.MAX_VALUE,
	  							NOSLOTGRANTED = 0;
	
	/**
	 * 
	 * @return nickname of the user
	 */
	String getNick();
	
	/**
	 * 
	 * @return the IP address if known.. 
	 * null if unknown
	 */
	InetAddress getIp();
	
	/**
	 * 
	 * @return the Tag string of the user..
	 * (everything including <> brackets)
	 */
	String getTag();
	
	/**
	 * 
	 * @return descriptions.. though only the part without the brackets.
	 * (no TAG)
	 */
	String getDescription();
	
	/**
	 * 
	 * @return the EMAIL of the user..
	 */
	String getEMail();
	
	/**
	 *
	 * @return the number of shared bytes of the user..
	 */
	long getShared();
	
	/**
	 * 
	 * @return the hashValue representing 
	 * a id for the user
	 * this should be the CID in ADC
	 */
	HashValue getUserid();
	
	/**
	 * 
	 * @return the CID from the ADC protocol ..
	 * will return null if CID is not set
	 */
	HashValue getCID();
	
	/**
	 * 
	 * @return true if the user is a favorite user
	 * 
	 */
	boolean isFavUser();
	
	
	/**
	 * 
	 * @return true if the user has downloaded filelist
	 */
	boolean hasDownloadedFilelist();
	
	/**
	 * 
	 * @return true if the user has a permanent slot grant
	 */
	boolean isAutograntSlot();
	
	/**
	 * if the user has currently a slot granted
	 * if isAutoGrantSlot() returns true this will as well be true
	 */
	boolean hasCurrentlyAutogrant();
	
	/**
	 * 
	 * @return if the user is currently online
	 */
	boolean isOnline();
	
	/**
	 * 
	 * @return when the user has been seen the last time
	 * 0 if unknown
	 */
	long getLastseen();
	
	/**
	 *
	 * @return the time until when a slot is granted..
	 */
	long getAutograntSlot();
	
	/**
	 * if the user is an Operator
	 * @return
	 */
	boolean isOp();
	
	Mode getModechar();
	
	String getConnection();
	
	int getNumberOfSharedFiles();
	
	int getSlots();
	
	int getNormHubs();
	
	int getRegHubs();
	
	int getOpHubs();
	
	byte getCt();
	
	AwayMode getAwayMode();
	
	String getSupports();
	
	int getAm();
	int getAs();
	long getDs();
	long getUs();
	int getUdpPort();
	String getVersion();
	HashValue getPD();
	
	
	IHub getHub();
	

	
	/**
	 * sends a Private Message to the user.
	 * 
	 * @param message - what is sent
	 * @param me - if me should be used.. false normally
	 * @param allowStore 
	 * @return true if the message could be sent 
	 *  false if the message was stored for later sending.. 
	 *  (if allow store false -> message will just be discarded)
	 */
	PMResult sendPM(String message, boolean me, boolean allowStore);
	
	
	public static enum PMResult {
		SENT,STORED,DISCARDED;
	}
	
	/**
	 * sets the FavUser state ..
	 * FavoriteUsers are persisted and can be given AutoSlots..
	 * @param favUser 
	 */
	void setFavUser(boolean favUser);
	
	/**
	 * download the FileList of the User
	 * 
	 * @return DQ Entry standing for the filelist..
	 */
	FileListDQE downloadFilelist();
	
	/**
	 * 
	 * @return a filelist descriptor of the user.. or null if not downloaded yet..
	 */
	FileListDescriptor getFilelistDescriptor();
	
	
	/*
	 * notifies the user that something about him has changed..
	 * this will trigger in the end a refresh in the GUI..
	 *
	void notifyUserChanged(); */
	
	/**
	 * 
	 * @return sid of a user.. -1 if does not apply i.e. nmdc...
	 */
	int getSid();
	
	/**
	 * 
	 * @return one upload the user gives us... 
	 * null if none.
	 */
	ClientProtocol getUpload();
	
	/**
	 * 
	 * @return true if the user ha s something we want to download
	 * 
	 */
	boolean weWantSomethingFromUser();
	
	/**
	 * 
	 * @return tre if the user can do encrypted connection protocols..
	 */
	boolean hasSupportFoEncryption();

	
	/**
	 * @return a download queue Entry we can currently download from the user..
	 */
	AbstractDownloadQueueEntry resolveDQEToUser();

	
	/**
	 * set an IP address to the user..
	 * @param otherip
	 */
	void setIp(InetAddress otherip);

	/**
	 * adds a transfer to the user.. (currently active filetransfer..)
	 * @param clientProtocol
	 */
	void addTransfer(ClientProtocol clientProtocol);

	void deleteConnection(ClientProtocol clientProtocol);

	void removeDQE(AbstractDownloadQueueEntry abstractDownloadQueueEntry);

	void addDQE(AbstractDownloadQueueEntry abstractDownloadQueueEntry);

	void removeFromDownloadQueue();

	
	/**
	 * 
	 * @param timeMillis how long the slot should be granted in milliseconds
	 * if TimeMillis is UNTILFOREVER a permanent slotgrant will be 
	 * set
	 * 
	 */
	void increaseAutograntSlot(long timeMillis);
	
	/**
	 * removes any slotgrant from the user
	 */
	void revokeSlot();

	/**
	 * internal check.. test if this user should be persisted..
	 * @return true if should be persisted
	 */
	boolean shouldBeStored();

	/**
	 * 
	 * @return how many files we have in DownloadQueue of this user..
	 */
	int nrOfFilesInQueue();
	
	/**
	 * 
	 * @return total size in bytes of all files in Queue
	 */
	long sizeOfFilesInQueue();
	
	
	
	public static enum Mode {
		// A for active P for passive   5 for Socks
		ACTIVE('A'),PASSIVE('P'),SOCKS('5');
	
		private final char modeChar;
		
		public static Mode fromModeChar(char c) {
			for (Mode m: Mode.values()) {
				if (m.modeChar == c) {
					return m;
				}
			}
			throw new IllegalStateException();
		}
		
		public char getModeChar() {
			return modeChar;
		}
		
		Mode(char modeChar) {
			this.modeChar = modeChar;
		}
		@Override
		public String toString() {
			return ""+modeChar;
		}

		
	}
	
	public static enum AwayMode {
		NORMAL(0),AWAY(1),EXTENDEDAWAY(2);
		
		private final int value;
		private final String val;
		
		private AwayMode(int i) {
			this.value = i;
			val = Integer.toString(i);
		}
		
		public static AwayMode parse(String value) {
			try {
				int mode = Integer.valueOf(value);
				for (AwayMode am:AwayMode.values()) {
					if (am.value == mode) {
						return am;
					}
				}

			} catch(RuntimeException re) {}
			
			return NORMAL;
		}
		
		public static AwayMode parseFlag(byte flag) {
			switch(flag) {
			case 1: 
			case 4:
			case 5:
			case 8:
			case 9:
				return NORMAL;
			case 2: 
			case 6:
			case 10:
				return AWAY;
			case 3: 
			case 7:
			case 11:
				return EXTENDEDAWAY;
			}
			return NORMAL;
		}

		public int getValue() {
			return value;
		}

		public String getVal() {
			return val;
		}

	}

}
