package eu.jucy.op;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

import logger.LoggerFactory;

import org.apache.log4j.Logger;

import uc.IHub;
import uc.IOperatorPlugin;
import uc.IUser;
import uc.OperatorPluginAdapter;
import uc.files.downloadqueue.AbstractDownloadQueueEntry;
import uc.files.downloadqueue.FileListDQE;
import uc.files.downloadqueue.AbstractDownloadQueueEntry.IDownloadFinished;
import uc.protocols.ConnectionProtocol;
import uc.protocols.ConnectionState;

public class OperatorPlugin extends OperatorPluginAdapter implements
		IOperatorPlugin {


	public static Logger logger = LoggerFactory.make();
	
	
	
	private static OperatorPlugin  singleton;
	
	public static OperatorPlugin get() {
		if (singleton == null) {
			throw new IllegalStateException("OP plugin Not yet created");
		}
		return singleton;
	}
	
	
	private Pattern protectedUsr;
	

	private final Set<IHub> controlledHubs = new HashSet<IHub>();

	/**
	 * map checked user to resultObjects..
	 */
	private final Map<IUser,Object> checkedUser = 
		Collections.synchronizedMap(new WeakHashMap<IUser,Object>()); 
	
	
	private final Queue<IUser> needsCheck = new ConcurrentLinkedQueue<IUser>(); // new LinkedList<IUser>();
	
	private final Map<IUser,AbstractDownloadQueueEntry> inCheck = 
		Collections.synchronizedMap(new HashMap<IUser,AbstractDownloadQueueEntry>());
	
	
	public OperatorPlugin() {
		update();
		if (singleton != null) {
			throw new IllegalStateException("Op plugin already initialized");
		}
		singleton = this;
	}

	private final void update() {
		protectedUsr = Pattern.compile(PI.get(PI.protectedUsersRegEx));
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see UC.IOperatorPlugin#init(UC.IHub)
	 */
	public boolean init(IHub hub) {
		String check = hub.getFavHub().get(PI.fh_checkUsers);
		if (Boolean.parseBoolean(check) && PI.getBoolean(PI.checkUsers)) {
			controlledHubs.add(hub);
			return true;
		}
		return false;
	}
	
	/*
	 * (non-Javadoc)
	 * @see UC.OperatorPluginAdapter#statusChanged(UC.protocols.ConnectionState, UC.protocols.ConnectionProtocol)
	 */
	@Override
	public void statusChanged(ConnectionState newStatus, ConnectionProtocol cp) {
		if (ConnectionState.DESTROYED.equals(newStatus)) {
			controlledHubs.remove(cp);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see UC.OperatorPluginAdapter#changed(UC.IUser, UC.listener.IUserChangedListener.UserChange)
	 */
	@Override
	public void changed(IUser changed, UserChange typeOfChange) {
		super.changed(changed, typeOfChange);
		
		switch(typeOfChange) {
		case CONNECTED:
			if (shouldCheckUsr(changed)) {
				needsCheck.offer(changed);
				changed.notifyUserChanged();
				if (inCheck.size() < PI.getInt(PI.parallelChecks)) {
					addCheckItem();
				} 
			}
			break;
		case QUIT:
		case DISCONNECTED:
			AbstractDownloadQueueEntry adqe;
			if ((needsCheck.remove(changed) | null != (adqe=inCheck.remove(changed))) && adqe != null) { 
				adqe.remove();
			}
			
		}
	}
	
	public boolean isInCheck(IUser usr) {
		return inCheck.containsKey(usr);
	}
	
	/**
	 * polls next user and starts checking process..
	 */
	private void addCheckItem() {
		final IUser usr = needsCheck.poll();;
		if (usr != null) {
			FileListDQE fdqe = usr.downloadFilelist();
			inCheck.put(usr,fdqe);
			fdqe.addDoAfterDownload(new IDownloadFinished() {
				public void finishedDownload(File f) {
					logger.debug("do after download: "+usr.getNick());  //this is called after the filelist check..
					inCheck.remove(usr);
					addCheckItem();
					usr.notifyUserChanged();
				}
			});
			usr.notifyUserChanged();
		}
	}
	
	/**
	 * 
	 * @param usr - usr that might need checking
	 * @return true if the User needs to be checked..
	 */
	private boolean shouldCheckUsr(IUser usr) {
		return 	!usr.isOp() && ! (usr.getShared() <= 0) &&
				!protectedUsr.matcher(usr.getNick()).matches() && 
				!checkedUser.containsKey(usr);
	}
	
	
	public void checkedUser(IUser checked) {
		checkedUser.put(checked, new Object()); //TODO check information object..
		checked.notifyUserChanged();
	}
	
	public CheckState getCheckState(IUser who) {
		if (controlledHubs.contains(who.getHub())) {
			if (inCheck.containsKey(who)) {
				return CheckState.CHECKING;
			}
			if (needsCheck.contains(who)) {
				return CheckState.SCHEDULED_FOR_CHECK;
			}
	
			if (checkedUser.containsKey(who)) {
				return CheckState.CHECKED;
			}
		
		}
		
		return CheckState.UNCHECKED;
	}


}
