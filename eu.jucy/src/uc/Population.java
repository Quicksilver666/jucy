package uc;


import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import logger.LoggerFactory;


import org.apache.log4j.Logger;

import uc.crypto.HashValue;
import uc.crypto.TigerHashValue;
import uc.files.filelist.FileListDescriptor;
import uc.listener.IUserChangedListener;
import uc.listener.IUserChangedListener.UserChange;
import uc.listener.IUserChangedListener.UserChangeEvent;
import uc.protocols.hub.Hub;

/**
 * Population on one side holds all online users (weak links)
 * basically all users that are alive in some way in the system.
 * So also offline users that may not be garbage collected from some reason..
 * 
 * 
 * @author Quicksilver
 *
 */
public final class Population {

	private static Logger logger = LoggerFactory.make();

	
	
	/**
	 *  this field  only contain weak references to the users..
	 * (users with DQEs won`t be removed then.. or otherwise referenced and therefore important )
	 */
	private final Map<HashValue,WeakReference<User>> allUsers	= 
		Collections.synchronizedMap( new WeakHashMap<HashValue,WeakReference<User>>());
	
	
	
	/**
	 * maps listeners to IDs of users..
	 * this is static so it we can map users that don`t exist currently
	 * additional it will cost less storage.. because the listeners 
	 * will be sparely used..(one field less for the User-object)  
	 * 
	 */
	private final Map<HashValue,CopyOnWriteArrayList<IUserChangedListener>> idToUCListener = 
		Collections.synchronizedMap(new HashMap<HashValue,CopyOnWriteArrayList<IUserChangedListener>>());
	
	private final CopyOnWriteArrayList<IUserChangedListener> ucListeners =
		new CopyOnWriteArrayList<IUserChangedListener>();
	

	
	 //additional set for users that may not get deleted by garbage collector
	private final Set<User> favs = new CopyOnWriteArraySet<User>();
	private final Set<User> slotGranted = new CopyOnWriteArraySet<User>();
	
	private volatile ScheduledFuture<?> slotGrantCheck;
	
	
	private final DCClient dcc;
	Population(DCClient dcc) {
		this.dcc = dcc;
	}
	
	
	
	
	/**
	 * delegate to the weak storage HashMap
	 * 
	 * @param userid
	 * @return
	 */
	private User getUser(HashValue userid) {
		WeakReference<User> userRef = allUsers.get(userid);
		if (userRef != null) {
			return userRef.get();
		} else {
			return null;
		}
	}
	
	/**
	 * retrieves a user..that is either online
	 * or  potentially retrieves a persisted
	 * user .. otherwise just
	 * creates a user-obj for nick and userid.
	 * 
	 * @param nick - nick of the user
	 * @param userid - id of the user (in NMDC created from nick and hub address)
	 * @return a User possibly stored.. else created
	 */
	public User get(String nick, HashValue userid) {
		synchronized (allUsers) {
			User usr = getUser(userid);
			
			if (usr == null) {
				usr = new User(dcc,nick,userid);
				allUsers.put(userid, new WeakReference<User>(usr));
			}
			return usr;	
		}
	}
	
	/**
	 * retrieve a user by his ID though 
	 * @param userid 
	 * @return user with given ID , or null if not present
	 */
	public IUser get(HashValue userid) {
		return getUser(userid);
	}
	

	
	public Set<User> getFavsAndSlotGranted() {
		refresh();
		logger.debug("retrieving favs: "+favs.size());
		Set<User> favsAndSlotranted = new HashSet<User>(favs);
		favsAndSlotranted.addAll(slotGranted);
		return favsAndSlotranted;
	}
	
	public Set<IUser> getSlotGranted() {
		return Collections.<IUser>unmodifiableSet(slotGranted);
	}
	
	public Set<IUser> getFavUsers() {
		return Collections.<IUser>unmodifiableSet(favs);
	}
	
	
	
	
	/**
	 * tries to remove users without current slot grants
	 * from the list
	 * periodically called for checking..
	 */
	private boolean refresh() {
		boolean containsUserWithoutPermanentslot = false;
		for (User u: slotGranted) {
			if (!u.hasCurrentlyAutogrant()) {
				u.notifyUserChanged(UserChange.CHANGED, UserChangeEvent.SLOTGRANT_REVOKED);
			} else {
				containsUserWithoutPermanentslot = containsUserWithoutPermanentslot || !u.isAutograntSlot();
			}
		}
		return containsUserWithoutPermanentslot;
	}
	
	
	
    /**
     * Settings.getStoragePath()+File.separator+"Filelists"+File.separator+usr.getNick()+"."+usr.getUserid()+".xml.bz2"
     * @param path the file that points to the filelistfile..
     * note the naming convention is used to find a user for this filelist
     * 
     * username.userid.xml.bz2
     * 
     * @return a User with a fileDescriptor pointing to the file
     */
    public  User getUserForFilelistfile(File path) {
    	logger.debug(path);
    	String name = path.getName();
    	User usr ;
    	Pattern p = Pattern.compile("(.*)\\.("+TigerHashValue.TTHREGEX+")\\.xml(?:.bz2)?");
    	Matcher m = p.matcher(name);
    	
    	
    	if (m.matches()) {
    		
    		HashValue userid = HashValue.createHash(m.group(2));
    		
    		usr = get(m.group(1),userid);	
    		
    	} else {
    		//not a legal FileList name found .. so we create an unknown user for the filelist
    		usr = get("unknown",TigerHashValue.ZEROBYTEHASH); //zero byte hash .. for the unknown user //  splits.length > 1? splits[0]:
    	}
    	
    	usr.setFilelistDescriptor(new FileListDescriptor(usr,path));
    	
    	return usr;
    }
    
    
    /*
     * 
     * @param favUser - if true user should
     * be set to FavUser
     * set 
     * if false removed..
     * used by user so a persistent link to the user is created
     * so he is not deleted
     *
    void addFavsAndSlotGranted(boolean add,User usr) {
		if (add) {
			if (favsAndSlotGranted.add(usr)) { //user may not be deleted by GC
				notifyObservers(new StatusObject(usr,ChangeType.ADDED));
			}
			logger.debug("user set added to favset");
		} else {
			//no longer FavUsr -> AutoGrant was removed automatically..
			if (favsAndSlotGranted.remove(usr)) {
				notifyObservers(new StatusObject(usr,ChangeType.REMOVED));
			}
		}
    } */
    
    
    
	/**
	 * registers the provided listener to be notified when 
	 * the state of the user changes
	 * @param listener - the listener to add
	 * multiple entries of the same listener will be ignored
	 */
	public void registerUserChangedListener(IUserChangedListener listener,HashValue userid) {
		if (listener == null) {
			throw new IllegalArgumentException("provided listener was null");
		}
		CopyOnWriteArrayList<IUserChangedListener> listeners = idToUCListener.get(userid);
		if (listeners == null) {
			listeners = new CopyOnWriteArrayList<IUserChangedListener>();
			idToUCListener.put(userid, listeners);
		}
		listeners.addIfAbsent(listener);
	}
	
	/**
	 * registers the provided listener to be notified when 
	 * the state of any user changes
	 * @param listener - the listener to add
	 * multiple entries of the same listener will be ignored
	 */
	public void registerUserChangedListener(IUserChangedListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("provided listener was null");
		}
		ucListeners.addIfAbsent(listener);
	}
	
	/**
	 * removes the provided listener so he will no longer be notified
	 * @param listener - the listener to be removed.
	 */
	public void unregisterUserChangedListener(IUserChangedListener listener,HashValue userid) {
		if (listener == null) {
			throw new IllegalArgumentException("provided listener was null");
		}
		
		CopyOnWriteArrayList<IUserChangedListener> listeners = idToUCListener.get(userid);
		if (listeners != null) {
			listeners.remove(listener);
			if (listeners.isEmpty()) {
				idToUCListener.remove(userid);
			}
		}
	}
	/**
	 * removes the provided listener so he will no longer be notified
	 * @param listener - the listener to be removed.
	 */
	public void unregisterUserChangedListener(IUserChangedListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("provided listener was null");
		}
		ucListeners.remove(listener);
	}
	
	/**
	 * called by user to notify listeners of a userchange
	 * nobody else may call..
	 */
	void internal_userChanged(User usr,UserChange type,int detail) {
		switch(detail) {
		case UserChangeEvent.FAVUSER_ADDED:
			favs.add(usr);
			break;
		case UserChangeEvent.FAVUSER_REMOVED:
			favs.remove(usr);
			break;
		case UserChangeEvent.SLOT_GRANTED:
			slotGranted.add(usr);
			if (slotGrantCheck == null && refresh()) {
				slotGrantCheck = dcc.getSchedulerDir().schedule(new Runnable() {
					/**
					 * checks every 60 seconds while a user wit non permanent slot
					 * is present for changes... 
					 */
					public void run() {
						if (refresh()) {
							slotGrantCheck = dcc.getSchedulerDir().schedule(this, 60, TimeUnit.SECONDS);
						} else {
							slotGrantCheck = null;
						}
					}
				}, 60, TimeUnit.SECONDS);
			}
			break;
		case UserChangeEvent.SLOTGRANT_REVOKED:
			slotGranted.remove(usr);
			break;
		}
			
		
		
		UserChangeEvent uce = new UserChangeEvent(usr, type,detail);
		List<IUserChangedListener> listeners = idToUCListener.get(usr.getUserid());
		if (listeners != null) {
			for (IUserChangedListener ucl: listeners) {
				ucl.changed(uce);
			}
		}
		for (IUserChangedListener ucl: ucListeners) {
			ucl.changed(uce);
		}
		Hub h = usr.getHub();
		if (h != null) {
			h.internal_notifyUserChanged(uce);
		}
	}
	

	
}
