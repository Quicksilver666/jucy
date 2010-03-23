package uc;


import helpers.GH;
import helpers.SizeEnum;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import logger.LoggerFactory;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.Platform;

import uc.IUserChangedListener.UserChange;
import uc.IUserChangedListener.UserChangeEvent;
import uc.crypto.HashValue;
import uc.files.downloadqueue.AbstractDownloadQueueEntry;
import uc.files.downloadqueue.FileListDQE;
import uc.files.filelist.FileListDescriptor;
import uc.protocols.TransferType;
import uc.protocols.client.ClientProtocol;
import uc.protocols.client.DisconnectReason;
import uc.protocols.hub.Hub;
import uc.protocols.hub.INFField;



/**
 * Represents a user in the system...
 * 
 * @author Quicksilver
 *
 */
public class User implements IUser , IHasUser {

	private static Logger logger = LoggerFactory.make();
	
	/**
	 * known ADC support strings
	 */
	public static final String 	ADCS_SUPPORT = "ADC0",	
								ADCS_UDP	= "SUD1" , //SUDP -> SecureUDP
								TCP4 = "TCP4",UDP4 = "UDP4",TCP6 = "TCP6",UDP6 = "UDP6",
								KEYP = "KEY0";
	

	private static final int SID_NOT_SET = -1; 
	
	public static final int FLAG_ENC = 0x10;  //flagbit for encryption in NMDC
	public static final int LASTSEEN_UNKNOWN = 0;
	
	public static final byte ct_BOT = 1, ct_RGISTERED = 2, ct_OPERATOR = 4, 
	ct_SUPERUSER = 8, ct_OWNER = 16,ct_HUB = 32;
	

	private final DCClient dcc;
	
	private boolean online;
	
	/**
	 * our own ID for that user (unique for all users we know)
	 * 
	 */
	private final HashValue userid; 

	private String nick = "";
	private long shared;
	private String description = "";
	private String tag;
	private String connection = "";
	private String eMail = "";
	
	/**
	 * nmdc flag value...
	 * shows kind of user..
	 * normal / server.. /online
	 * though important can be used to show if encryption supported..
	 */
	private byte flag;
	
	private Inet4Address i4;
	private Inet6Address i6;
	
	/**
	 * replacement for Op field: 
	 * Client (user) type, 
	 * 1  = bot, 
	 * 2  = registered user, 
	 * 4  = operator, 
	 * 8  = super user, 
	 * 16 = hub owner, 
	 * 32 = hub (used when the hub sends an INF about itself). 
	 * Multiple types are specified by adding the numbers together.
	 */
	private byte ct;  
	

	
	/**
	 * AW  	 integer  	1=Away, 2=Extended away, not interested in hub chat 
	 * (hubs may skip sending broadcast type MSG commands to clients with this flag)
	 * or zero for normal we assume.. 
	 */
	private AwayMode awayMode = AwayMode.NORMAL;
	

	private Mode modechar = Mode.PASSIVE;
	private short slots;
	
	private short normHubs;
	private short regHubs;
	private short opHubs;
	
	
	// ADC only values
	private int sid = SID_NOT_SET; //-1 for unset



	/**
	 * ADC CID of the user
	 */
	private HashValue cid; 
	private int numberOfSharedFiles;
	
	/**
	 * see KEYP extension
	 */
	private HashValue keyPrint;
	
	/**
	 * 
	 *  Minimum simultaneous upload connections in automatic 
	 *  slot manager mode 
	 */
	private byte am;
	
	/**
	 * AS  	 integer  	Automatic slot allocator speed limit, bytes/sec. 
	 * The client keeps opening slots as long as its total upload speed 
	 * doesn't exceed this value. 
	 */
	private int as;
	
	/**
	 * Maximum download speed, bytes/second 
	 */
	private long ds;
	
	/**
	 * Maximum upload speed, bytes/second 
	 */
	private long us;
	
	/**
	 * su field from ADC
	 * 
	 * Comma-separated list of feature FOURCC's. 
	 * This notifies other clients of extended capabilities of the connecting client. 
	 */
	private String supports = "";
	
	/**
	 * Client identification, version (client-specific, 
	 * a short identifier then a dotted version number is recommended) 
	 */
	private String version;
	
	
	private short udpPort;
	private short udp6Port;
	
	//the hub the user is in if online .. the hub the user was last in if offline
	private volatile Hub hub ;
	
	
	/**
	 * special field to denote special stuff not every user has..
	 * only the ones with which we had closer contact..
	 */
	private volatile ExtendedInfo extended;
	
	
	
	/**
	 * 
	 * @param nick
	 * @param userid
	 */
	protected User(DCClient dcc,String nick,HashValue userid) {
		this.userid	= userid;
		this.nick	= nick;
		this.dcc = dcc;
	}
	
	
	/**
	 * properties that can be set to Users can be set using this..
	 * will not trigger any user change notifications..
	 * 
	 * @param usr
	 * @param val
	 * @throws UnknownHostException
	 * @throws NumberFormatException
	 * @throws IllegalArgumentException
	 */
	public void setProperty(INFField inf,String val) throws  NumberFormatException, IllegalArgumentException {
		if (!GH.isEmpty(val)) {
			switch(inf) {
			case ID: 
				cid = HashValue.createHash(val); 
			break;
			case I4:
				try {
					InetAddress ia = InetAddress.getByName(val);
					if (ia instanceof Inet4Address) {
						i4 = (Inet4Address)ia;
						updateModecharBasedOnIPPort();
					}
				} catch (UnknownHostException uhe) {
					logger.debug("Could not resolve address: "+val+ " for user "+this);
				}
				break;
			case I6:
				try {
					InetAddress ia = InetAddress.getByName(val);
					if (ia instanceof Inet6Address) {
						i6 = (Inet6Address)ia;
						updateModecharBasedOnIPPort();
					}
				} catch (UnknownHostException uhe) {
					logger.debug("Could not resolve address: "+val+ " for user "+this);
				}
				break;
			case SS:
				setShared(Long.parseLong(val));
				break;
			case SF:
				numberOfSharedFiles = Integer.parseInt(val);
				break;
			case SL:
				slots = IntToShort(val);
				break;
			case EM:
				eMail = val;
				break;
			case NI:
				if (getHub() != null) {
					getHub().internal_userChangedNick(this,val);
				} else {
					nick = val;
				}
				break;
			case DE:
				description = val;
				break;
			case HN:
				normHubs = IntToShort(val);
				break;
			case HR:
				regHubs = IntToShort(val);
				break;
			case HO:
				opHubs = IntToShort(val);
				break;
			case CT:
				ct = Byte.parseByte(val);
				break;
			case AW: //Away mode
				awayMode = AwayMode.parse(val);
				break;
			case SU:
				supports = val.intern();
				updateModecharBasedOnIPPort();
				break;
			case AM:
				am = (byte) Integer.parseInt(val);
				break;
			case AS:
				as = Integer.parseInt(val);
				break;
			case DS:
				ds = Long.parseLong(val);
				break;
			case US:
				us =  Long.parseLong(val);
				break;
			case U4:
				udpPort = (short) Integer.parseInt(val);
				break;
			case U6:
				udp6Port = (short) Integer.parseInt(val);
				break;
			case PD: //PID is never sent to us, only interesting for the hub
				break;
			case RF: //Referrer field is also only interesting for the hub
				break;
			case TO: //INFfield for ctm token
				break;
			case VE:
				version = val.intern();
				break;
			case KP:
				try {
					keyPrint = HashValue.createHash(val);
				} catch(IllegalArgumentException iae) {
					if (Platform.inDevelopmentMode()) {
						logger.warn("hash could not be parsed: "+val+"  "+iae);
					}
				}
				break;
			}
		} else {
			 deletePropery(inf);
		}
	}
	
	private void deletePropery(INFField inf) {
		switch(inf) {
		case ID: 
			cid = null; 
		break;
		case I4:
			i4 = null;
			updateModecharBasedOnIPPort();
			break;
		case I6:
			i6 = null;
			updateModecharBasedOnIPPort();
			break;
		case SS:
			setShared(0);
			break;
		case SF:
			numberOfSharedFiles = 0;
			break;
		case SL:
			slots = 0;
			break;
		case EM:
			eMail = "";
			break;
		case NI:
			if (getHub() != null) {
				getHub().internal_userChangedNick(this,"");
			} else {
				nick = "";
			}
			break;
		case DE:
			description = "";
			break;
		case HN:
			normHubs = 0;
			break;
		case HR:
			regHubs = 0;
			break;
		case HO:
			opHubs = 0;
			break;
		case CT:
			ct = 0;
			break;
		case AW: //Away mode
			awayMode = AwayMode.NORMAL;
			break;
		case SU:
			supports = "";
			break;
		case AM:
			am = 0;
			break;
		case AS:
			as = 0;
			break;
		case DS:
			ds =0;
			break;
		case US:
			us =  0;
			break;
		case U4:
			udpPort = 0;;
			break;
		case U6:
			udp6Port = 0;
			break;
		case PD: //PID is never sent to us, only interesting for the hub
			break;
		case RF: //Referrer field is also only interesting for the hub
			break;
		case TO: //INFfield for ctm token
			break;
		case VE:
			version = "";
			break;
		case KP:
			keyPrint = null;
			break;
		}
	}
	
	/**
	 * updates Modechar in ADC based on info gained with set Property..
	 * 
	 */
	private void updateModecharBasedOnIPPort() {
		if (isADCUser() && hub != null) {
			Identity id =   hub.getIdentity();
			boolean ipv4 = i4 != null && supports.contains(TCP4) && id.isIPv4Used() ;
			boolean ipv6 = i6 != null && supports.contains(TCP6) && id.isIPv6Used();
			setModechar( ipv4 || ipv6? Mode.ACTIVE : Mode.PASSIVE);
		}
	}
	
	
	
	
	public boolean isUDPActive() {
		if (isADCUser()) {
			Identity id =   hub.getIdentity();
			boolean active 	= i4 != null && udpPort != 0  && supports.contains(UDP4) && id.isIPv4Used() ;
			active 			= active ||  (i6 != null && udp6Port != 0  && supports.contains(UDP6) && id.isIPv6Used());
			return active;
		} else {
			return getModechar() == Mode.ACTIVE;
		}
	}


	public boolean isTCPActive() {
		if (isADCUser()) {
			Identity id =   hub.getIdentity();
			String sup = getSupports();
			boolean active 	= i4 != null && sup.contains(TCP4) && id.isIPv4Used() ;
			active 			= active ||  (i6 != null && sup.contains(TCP6) && id.isIPv6Used());
			return active; 
		} else {
			return getModechar() == Mode.ACTIVE;
		}
	}


	public void removeFromDownloadQueue() {
		if (extended != null && extended.dqes != null) { 
			for (AbstractDownloadQueueEntry dqe: extended.dqes) {
				dqe.removeUser(this);
			}
		}
	}
	
	public synchronized boolean hasDownloadedFilelist() { 
		return extended != null && extended.fileListDescriptor != null && extended.fileListDescriptor.isValid();
	}
	
	/**
	 * method that decides if this user should be persisted..
	 * 
	 * @return if the user should be persisted in the database..
	 */
	public boolean shouldBeStored(){
		return extended != null && (extended.favUser ||weWantSomethingFromUser()||hasCurrentlyAutogrant());
	}
	
	
	/**
	 *  returns the highest priority DownloadQueue item for a user
	 *  
	 * @return the highest priority DQE of the user or null if none available
	 * none available means currently not available either because there are none
	 * or because all priorities are zero or 
	 * too many people are downloading on all files
	 * 
	 */
	private AbstractDownloadQueueEntry getHighestPriorityOfUser() {
		if (extended == null || extended.dqes == null) { //if there are no entries...
			return null;
		}
		AbstractDownloadQueueEntry ret = null;
		
		for (AbstractDownloadQueueEntry dqe : extended.dqes) {
			logger.debug(dqe);
			//must not be stopped and must have room for download..determines if downloadable
			boolean isDownloadable= dqe.getPriority() != 0  && dqe.isDownloadable();
			//if we have a MerkleTree or a Filelist we immediately return
			if (isDownloadable && (dqe.getType() == TransferType.FILELIST || dqe.getType() == TransferType.TTHL  )) {
				logger.debug("immediate return: "+dqe);
				return dqe;
			}

			//dqe must be downloadable and better than best entry.. 
			if (isDownloadable && (ret == null ||  ret.compareTo(dqe) < 0 )) { 
				ret = dqe;
			}
		}
		
		
		return ret;
	}
	
	/**
	 * ask if we need something from this user
	 * @return true if the user holds at least one DQE..
	 */
	public boolean weWantSomethingFromUser(){
		if (extended == null || extended.dqes == null) {
			return false;
		}
		return !extended.dqes.isEmpty();
	}
	
	/**
	 * get DQE by other user .. used to find out if we need something from this user.. 
	 * return null if we want nothing
	 * will also return null if we already download from this user..
	 * @return the dqe that we want from that user or null if we want nothing
	 */
	public AbstractDownloadQueueEntry resolveDQEToUser(){
		//check if we not already have a running download for that user..
		if (getDownload() != null) { 
			logger.debug("getDownload() null");
			return null;
		}
		//check if we want something from that user
		//if we want nothing.. we are done
		if (!weWantSomethingFromUser()) {
			return null;
		} else { //else we get the highest priority item from the Queue for that user and return it	
			return getHighestPriorityOfUser();
		}
	}
	
	/**
	 * checks if SID is 0 / unchanged
	 */
	public boolean isADCUser() {
		return sid != SID_NOT_SET;
	}
	

	
	/**
	 * 
	 * @param message  the pm message
	 * @return if the message could be sent..
	 * false means message was stored (if allowstore true else discarded..).. 
	 */
	public PMResult sendPM(final String message,final boolean me,boolean allowStore) {
		if (isOnline()) {
			getHub().sendPM(User.this, message,me);
			return PMResult.SENT;
		} else {
			if (allowStore) {
				return dcc.getStoredPM().storePM(this, message, me)?
						PMResult.STORED:
						PMResult.DISCARDED;
			} else {
				return PMResult.DISCARDED;
			}
		}
	}
	
	public String toString(){
		return nick + " "+ shared +" "+getTag() + (hub != null?" From: "+hub.getHubname():"");
	}

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((userid == null) ? 0 : userid.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof IUser))
			return false;

		final IUser other = (IUser) obj;

		if (!userid.equals(other.getUserid()))
			return false;
		return true;
	}
	

	/**
	 * @return the online
	 */
	public boolean isOnline() {
		return online;
	}

	/**
	 * 
	 * @param hub - the hub where the user connected to
	 */
	public void userConnected(Hub hub) {
		this.hub = hub;
		online = true;
		notifyUserChanged(UserChange.CONNECTED,UserChangeEvent.NotApplicable);
	}

	
	/**
	 * @return the connection  ex DSL
	 */
	public String getConnection() {
		if (GH.isEmpty(connection)) {
			if (getUs() == 0) {
				return "";
			} else {
				return SizeEnum.toShortSpeedString(getUs());
			}
		} else {
			return connection;
		}
	}

	/**
	 * @param connection the connection to set
	 */
	public void setConnection(String connection) {
		this.connection = connection;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}



	/**
	 * @return the dqes
	 */
	public void addDQE(AbstractDownloadQueueEntry dqe) {
		makeSureExtendedExists();
		if (extended.dqes == null ) {
			extended.dqes	=	new CopyOnWriteArraySet<AbstractDownloadQueueEntry>(); 
		}
		if (!extended.dqes.contains(dqe)) {
			extended.dqes.add(dqe);
			if (extended.dqes.size() == 1) {
				//if first entry .. add user to the database as he might not yet be in there..
			//	DCClient.get().getDatabase().addUpdateOrDeleteUser(this);
				notifyUserChanged(UserChange.CHANGED, UserChangeEvent.DOWNLOADQUEUE_ENTRY_PRE_ADD_FIRST);
			}
			//after adding it .. we can set it to the DQE -> 
			dqe.addUser(this);
/*			if (extended.dqes.size() == 1) {
				//if this is the first DQE.. tell the ConnectionHandler that this user became interesting
				DCClient.get().getCh().onInterestingUserArrived(this);
				logger.debug("user "+getNick()+" became interesting");

			} */
			notifyUserChanged(UserChange.CHANGED, UserChangeEvent.DOWNLOADQUEUE_ENTRY_ADDED);
		}

	}
	
	/**
	 * 
	 * @return how many DQEs we have in queue for this user..
	 */
	public int nrOfFilesInQueue() {
		if (extended == null || extended.dqes == null) {
			return 0;
		} else {
			return extended.dqes.size();
		}
	}
	
	public long sizeOfFilesInQueue() {
		if (extended == null || extended.dqes == null) {
			return 0;
		} else {
			long total = 0;
			for (AbstractDownloadQueueEntry adqe:extended.dqes) {
				total += adqe.getSize() - adqe.getDownloadedBytes();
			}
			return total;
		}
	}
	
	public void removeDQE(AbstractDownloadQueueEntry dqe) {
		if (extended != null && extended.dqes != null && extended.dqes.contains(dqe)) {
			extended.dqes.remove(dqe);
			dqe.removeUser(this);
			notifyUserChanged(UserChange.CHANGED,  UserChangeEvent.DOWNLOADQUEUE_ENTRY_REMOVED);
			if (extended.dqes.isEmpty()) {
				extended.dqes = null;
				checkExtendedForDeletion();
				//if size is empty.. user may be deleted now..
			//	DCClient.get().getDatabase().addUpdateOrDeleteUser(this);
				notifyUserChanged(UserChange.CHANGED,  UserChangeEvent.DOWNLOADQUEUE_ENTRY_POST_REMOVE_LAST);
			}

			//delete transfers that download this dqe..
			ClientProtocol cp = getDownload();
			if (cp != null &&  dqe.equals(cp.getFti().getDqe())) {
				if (dqe.getType() != TransferType.TTHL) {
					closeAllDownloads(DisconnectReason.FILEREMOVED);
				}
			}
		}
	}


	

	/**
	 * @return the eMail
	 */
	public String getEMail() {
		return eMail;
	}




	/**
	 * @return the modechar
	 */
	public Mode getModechar() {
		return modechar;
	}
	

	/**
	 * @param modechar the modechar to set
	 */
	public void setModechar(Mode modechar) {
		this.modechar = modechar;
	}

	/**
	 * @return the nick
	 */
	public String getNick() {
		return nick;
	}

	/**
	 * @return the normHubs
	 */
	public int getNormHubs() {
		return normHubs;
	}



	/**
	 * @return the op
	 */
	public boolean isOp() {
		return ct >= 4; 
	}

	/**
	 * @param op the op to set
	 */
	public final void setOp(boolean op) {
		if (op) {
			setProperty(INFField.CT, ""+ct_OPERATOR);
		} else {
			setProperty(INFField.CT,"");
		}
		notifyUserChanged(UserChange.CHANGED, UserChangeEvent.INF);
	}

	/**
	 * @return the opHubs
	 */
	public int getOpHubs() {
		return opHubs;
	}

	/*
	 * @param opHubs the opHubs to set
	 *
	public void setOpHubs(int opHubs) {
		if (opHubs > Short.MAX_VALUE) {
			opHubs = Short.MAX_VALUE;
		}
		this.opHubs = (short)opHubs;
	} */

	/**
	 * @return the regHubs
	 */
	public int getRegHubs() {
		return regHubs;
	}

	/*
	 * @param regHubs the regHubs to set
	 *
	public void setRegHubs(int regHubs) {
		if (regHubs > Short.MAX_VALUE) {
			regHubs = Short.MAX_VALUE;
		}
		this.regHubs = (short)regHubs;
	} */

	/**
	 * @return the shared
	 */
	public long getShared() {
		return shared;
	}

	/**
	 * @param shared the shared to set
	 */
	public void setShared(long shared) {
		if (this.shared != shared) {
			long diff = shared - this.shared;
			this.shared = shared;
			if (hub != null) {
				hub.increaseSharesize(diff);
			}
		}
	}

	/**
	 * @return the slots
	 */
	public int getSlots() {
		return slots;
	}

	/*
	 * @param slots the slots to set
	 *
	public void setSlots(int slots) {
		if (slots > Short.MAX_VALUE) {
			slots = Short.MAX_VALUE;
		}
		this.slots =(short)slots;
	} */

	/**
	 * @return the tag
	 */
	public String getTag() {
		if (tag != null) {
			return tag;
		} else {
			String v = getVersion();
			return "<"+( v == null? "   " : v) +
					",M:"+ (isADCUser()? (isTCPActive()?"A":"P") : getModechar())+
					",H:"+getNormHubs()+"/"+getRegHubs()+"/"+getOpHubs()+
					",S:"+getSlots()+ ">"; 
		}
	}

	/**
	 * @param tag the tag to set
	 */
	public void setTag(String tag) {
		this.tag = tag;
	}

	/**
	 * @return the userid
	 */
	public HashValue getUserid() {
		return userid;
	}


	/**
	 * @return the lastSeen
	 */
	public synchronized long getLastseen() {
		if (extended == null) {
			return LASTSEEN_UNKNOWN;
		} else {
			return extended.lastseen;
		}
		
		/*
		if (lastseen != 0) {
			return new Date(lastseen);
		} else {
			return null;
		} */
	}


	/*
	 * @param lastseen the lastSeen to set
	 *
	private void setLastseen(Date lastseen) {
		this.lastseen = new Date(lastseen.getTime());
	} */



	/**
	 * @return the favUser
	 */
	public boolean isFavUser() {
		return extended != null && extended.favUser;
	}


	/**
	 * sets FavUser option and persists..
	 */
	public void setFavUser(boolean favUser) {
		makeSureExtendedExists();
		boolean oldFav = extended.favUser;
		extended.favUser = favUser;
		if (!favUser) {
			setAutograntSlot(NOSLOTGRANTED); 
		}
	//	DCClient dcc = DCClient.get();
		if (oldFav != favUser) {
		//	dcc.getDatabase().addUpdateOrDeleteUser(this); //change persistence
			
			notifyUserChanged(UserChange.CHANGED, 
					favUser?UserChangeEvent.FAVUSER_ADDED:UserChangeEvent.FAVUSER_REMOVED);
		}

		
		//dcc.getPopulation().addFavsAndSlotGranted(favUser, this);

	}

	private void makeSureExtendedExists() {
		if (extended == null) {
			extended = new ExtendedInfo();
		}
	}
	
	private void checkExtendedForDeletion() {
		if (extended.couldBedeleted()) {
			extended = null;
		}
	}


	/**
	 * @return the autograntSlot 
	 * only true if the user has a permanent AutoGrant slot
	 */
	public boolean isAutograntSlot() {
		return extended != null &&  extended.autograntSlot == UNTILFOREVER ;
	}

	public boolean hasCurrentlyAutogrant() {
		return extended != null && extended.autograntSlot > System.currentTimeMillis();
	}


	/**
	 * @param autograntSlot the autograntSlot to set
	 */
	public void increaseAutograntSlot(long howLongSlotIsGranted) {
		makeSureExtendedExists();
		long grantuntil = howLongSlotIsGranted == UNTILFOREVER? UNTILFOREVER: System.currentTimeMillis()+howLongSlotIsGranted;
		long autograntSlot = Math.max(grantuntil , extended.autograntSlot);
		setAutograntSlot(autograntSlot);
		
	}
	

	public void revokeSlot() {
		setAutograntSlot(NOSLOTGRANTED);
	}
	
	/**
	 * sets the value of the autogrant slot to the date of its enspireing..
	 * @param grantedUntil
	 * @param persist - true only if the change should be persisted (false for loading)
	 */
	public void setAutograntSlot(long grantedUntil) {
		boolean hadAutograntBefore = hasCurrentlyAutogrant();
		makeSureExtendedExists();
		extended.autograntSlot = grantedUntil;
		
		boolean hasNowAutoGrant = hasCurrentlyAutogrant();
		
		
		int detail = hadAutograntBefore == hasNowAutoGrant? UserChangeEvent.SLOTGRANT_CHANGED:
						hasNowAutoGrant? UserChangeEvent.SLOT_GRANTED: UserChangeEvent.SLOTGRANT_REVOKED;
		
		notifyUserChanged(UserChange.CHANGED, detail);
	}
	
	/**
	 * if true sets an autoGrant to forever
	 * if false deletes autoGrant
	 */
	public void setAutoGrantSlot(boolean autogrant) {
		if (autogrant) {
			setAutograntSlot(UNTILFOREVER);
		} else {
			setAutograntSlot(NOSLOTGRANTED);
		}
		
	}

	/**
	 * @return the download
	 */
	public ClientProtocol getDownload() {
		synchronized(this) {
			if(extended == null || extended.transfers == null) {
				return null;
			}
		
			for (ClientProtocol nmdcc : extended.transfers) {
				if (nmdcc.getFileTransfer()!= null && !nmdcc.getFileTransfer().isUpload()) {
					return nmdcc;
				}
			}
		}
		return null;
	}

	/**
	 * @param download the download to set
	 */
	public synchronized void addTransfer(ClientProtocol nmdcc) {
		makeSureExtendedExists();
		if (extended.transfers == null) {
			extended.transfers =  new CopyOnWriteArraySet<ClientProtocol>();
		}
		extended.transfers.add( nmdcc );
		
	}

	/**
	 * @return the upload
	 */
	public synchronized ClientProtocol getUpload() {
		if (extended == null || extended.transfers == null) {
			return null;
		}

		for (ClientProtocol nmdcc:extended.transfers) {
			if (nmdcc.getFileTransfer()!= null && nmdcc.getFileTransfer().isUpload()) {
				return nmdcc;
			}
		}
		return null;
	}
	
	/**
	 * closes all Downloads from this user with
	 * the provided reason
	 * @param reason - why the downloads should be closed
	 */
	public void closeAllDownloads(DisconnectReason reason) {
		ClientProtocol nmdcc;
		while (null != (nmdcc = getDownload())) {
			nmdcc.disconnect(reason);
			deleteConnection(nmdcc);//connection would delete this.. but if we don't delete now this will deadlock
		}
	}

	/**
	 * closes all Uploads from this user with
	 * the provided reason
	 * @param reason - why the Uploads should be closed
	 */
	private void closeAllUploads(DisconnectReason reason) {
		ClientProtocol nmdcc;
		while (null != (nmdcc = getUpload())) {
			deleteConnection(nmdcc);//connection would delete this.. but if we don't delete now this will deadlock
			nmdcc.disconnect(reason);
		}
	}

	/**
	 * only deletes connection from the user.. nothing more connection is not stopped..
	 * @param nmdcc
	 */
	public void deleteConnection(ClientProtocol nmdcc) { 
		synchronized(this) {
			if (extended != null && extended.transfers != null ){
				extended.transfers.remove(nmdcc);
				if (extended.transfers.isEmpty()) {
					extended.transfers = null;
					checkExtendedForDeletion();
				}
			}
		}
	}
	
	/**
	 * convenience method for 
	 * @return the DownloadQueEntry for the FileList 
	 */
	public FileListDQE downloadFilelist() {
		logger.debug("requested filelist of: "+ getNick());
		return FileListDQE.get(this,dcc.getDownloadQueue());
	}

	/**
	 * @return the hub
	 */
	public Hub getHub() {
		return hub;
	}

	/*
	 * @param hub the hub to set
	 *
	public void setHub(Hub hub) {
		this.hub = hub;
	} */

	/**
	 * @return the FileList for the filelist of the user..
	 */
	public synchronized FileListDescriptor getFilelistDescriptor() {
		if (extended == null) {
			return null;
		} else {
			return extended.fileListDescriptor;
		}
	}

	/**
	 * @param fileListDescriptor the filelistDescriptor to set
	 */
	public synchronized void setFilelistDescriptor(FileListDescriptor fileListDescriptor) {
		makeSureExtendedExists();
		extended.fileListDescriptor = fileListDescriptor;
	}
	
	

	/**
	 * called when user quits from hub 
	 * @param true if a quit was sent.. false if we just disconnected..
	 */
	public void disconnected(boolean quitSent) {
		notifyUserChanged(quitSent?UserChange.QUIT:UserChange.DISCONNECTED , UserChangeEvent.NotApplicable);
		
		if (quitSent) {//on Quit disconnect all Uploads
			closeAllUploads(DisconnectReason.USERQUIT);
		}
		synchronized(this) {
			online = false;
			setShared(0); //subtracts the shared size from the hub..
			ct = 0;  //clear op field if user reconnect otherwise he might still be seen as Operator..
			if (extended != null) {
				extended.lastseen = System.currentTimeMillis();
			}
		}
	}
	/**
	 * notifies all change listeners that are directly 
	 * registered with this user..
	 * @param type - what change type.
	 * normal change just uses CUserChange.CHANGED
	 */
	public void notifyUserChanged(UserChange type,int detail) {
		dcc.getPopulation().internal_userChanged(this, type,detail);
	}

	public synchronized Inet4Address getIp() {
		return i4;
	}
	
	public synchronized Inet6Address getI6() {
		return i6;
	}


	public final void setIp(InetAddress ip) {
		synchronized(this) {
			boolean i4addy = ip instanceof Inet4Address;
			setProperty(i4addy?INFField.I4:INFField.I6, ip.getHostAddress());
		}
		notifyUserChanged(UserChange.CHANGED,UserChangeEvent.INF);
	}
	



	public long getAutograntSlot() {
		if (extended == null) {
			return NOSLOTGRANTED;
		} else {
			return extended.autograntSlot;
		}
	}

	public IUser getUser() {
		return this;
	}

	/**
	 * 
	 * @return the CID of a user.. null in nmdc
	 */
	public HashValue getCID() {
		return cid;
	}

//	void setCID(HashValue cid) {
//		this.cid = cid;
//	}

	public int getNumberOfSharedFiles() {
		return numberOfSharedFiles;
	}

/*	public void setNumberOfSharedFiles(int numberOfSharedFiles) {
		this.numberOfSharedFiles = numberOfSharedFiles;
	} */

	/**
	 * warn internal.. should not be uses..
	 */
	public void internal_setNick(String nick) {
		this.nick = nick;
	}

	public byte getCt() {
		return ct;
	}

	public AwayMode getAwayMode() {
		return awayMode;
	}
	

	public byte getFlag() {
		return flag;
	}

	public void setFlag(byte flag) {
		this.flag = flag;
	}

	public boolean testFlag(int testFor) {
		return (testFor & flag) != 0;
	}
	
	public String getSupports() {
		return supports;
	}

/*	public void setSupports(String supports) {
		this.supports = supports;
	} */
	
	/**
	 * checks whether the user has support for encryption
	 * @return true if Encryption is supported..
	 */
	public boolean hasSupportForEncryption() {
		return supports.contains(ADCS_SUPPORT) || testFlag(FLAG_ENC);
	}
	
	public boolean hasSupportForUDPEncryption() {
		return supports.contains(ADCS_UDP);
	}

	public int getUdpPort() {
		return (udpPort & 0xffff);
	}
	
	public int getUDP6Port() {
		return (udp6Port & 0xffff);
	}

/*	public void setUdpPort(int udpPort) {
		this.udpPort = (short)udpPort ;
	} */

	public int getAm() {
		return am & 0xff;
	}

/*	public void setAm(int am) {
		this.am = (byte)am  ;
	} */

	public int getAs() {
		return as;
	}

/*	public void setAs(int as) {
		this.as = as;
	} */

	public long getDs() {
		return ds;
	}

	/*public void setDs(long ds) {
		this.ds = ds;
	} */

	/**
	 * @return Upload Speed in Byte / s
	 */
	public long getUs() {
		return us;
	}

/*	public void setUs(long us) {
		this.us = us;
	} */

	public String getVersion() {
		return version;
	}

	/*public void setVersion(String version) {
		this.version = version;
	} */
	
	public int getSid() {
		return sid;
	}

	public void setSid(int sid) {
		this.sid = sid;
	}

	/**
	 * the PrivateID of ADC ..
	 * @return always null as its always unknown to us 
	 */
	public HashValue getPD() {
		return null;
	}
	
	public HashValue getKeyPrint() {
		return keyPrint;
	}
	
	
	
	public DCClient getDcc() {
		return dcc;
	}


	/**
	 * takes value that might potantially be larger than short..
	 * and cuts it down to short.max in worst case..
	 * @param val
	 * @return
	 */
	private static short IntToShort(String val) {
		int i = Integer.parseInt(val);
		if (i > Short.MAX_VALUE) {
			i = Short.MAX_VALUE;
		}
		return (short)i;
	}
	

	

	private static class ExtendedInfo {
		private long lastseen = LASTSEEN_UNKNOWN;
		private boolean favUser;
		private volatile long autograntSlot; // until when an slot is granted
		private volatile CopyOnWriteArraySet<AbstractDownloadQueueEntry>  dqes	= null;
		private Set<ClientProtocol> transfers			= null;
		private FileListDescriptor fileListDescriptor = null;
		
		private boolean couldBedeleted() {
			return !favUser && autograntSlot < System.currentTimeMillis()
				&& dqes == null && transfers == null && fileListDescriptor == null;
		}
		
	}
	
	
	
	
}

	