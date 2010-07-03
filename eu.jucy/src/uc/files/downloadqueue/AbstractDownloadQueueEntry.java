package uc.files.downloadqueue;

import helpers.GH;
import helpers.IObservable;
import helpers.StatusObject;
import helpers.Observable.IObserver;
import helpers.StatusObject.ChangeType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import logger.LoggerFactory;
import org.apache.log4j.Logger;

import uc.DCClient;
import uc.IUser;
import uc.PI;
import uc.crypto.HashValue;



import uc.files.IDownloadable;
import uc.files.IHasDownloadable;
import uc.files.IDownloadable.IDownloadableFile;
import uc.files.transfer.AbstractFileTransfer;
import uc.files.transfer.AbstractWritableFileInterval;
import uc.files.transfer.FileTransferInformation;
import uc.files.transfer.IFileTransfer;
import uc.files.transfer.TransferChange;
import uc.protocols.TransferType;

/**
 * base class for all kinds of DownloadQueueEntrys
 * 
 * implements comparable for ordering DownloadQueueEntrys
 * by priority..
 * 
 * @author quicksilver
 *
 */
public abstract class AbstractDownloadQueueEntry implements Comparable<AbstractDownloadQueueEntry>, IObserver<TransferChange>, IHasDownloadable {
	
	private static final Logger logger = LoggerFactory.make();
	

	
	private final Object sync = new Object(); //object to sync on for adding and removing user..
	
	/**
	 * all running transfer will be listed here
	 */
	protected final List<IFileTransfer> runningFileTransfers = 
		new CopyOnWriteArrayList<IFileTransfer>();
	
		//Collections.synchronizedList(new ArrayList<AbstractFileTransfer>());
	
	
	/**
	 * users that are associated with this DownloadQueuEntry
	 * because we determined that they have the file/TTHL and 
	 * want it from them
	 */
	protected final Set<IUser> users = new CopyOnWriteArraySet<IUser>();//   Collections.synchronizedSet(new HashSet<User>());
	
	
	
	private final Set<IUser> removedUsers = new CopyOnWriteArraySet<IUser>() ; //   Collections.synchronizedSet(new HashSet<User>());
	
	
	private volatile int priority = 255/2;     //the higher the priority.. the sooner it will be uploaded..

	/**
	 * what kind of DQE this is..
	 */
	protected final TransferType type;
	
	/**
	 * Actions that have to be taken when the file has finished its download..
	 */
	protected final Set<IDownloadFinished> downloadFinished = 
		new CopyOnWriteArraySet<IDownloadFinished>();
	
	
	private final Date added;          //the time this was added
	
	/**
	 * when the file finished downloading..
	 */
	protected volatile Date finished = null;
	
	protected final DownloadQueue dq;
	


	/**
	 * base constructor for the DownloadQueueEntry
	 * @param type
	 */
	protected AbstractDownloadQueueEntry(DownloadQueue dq,TransferType type, int priority,Date added) {
		this.dq = dq;
		this.type = type;
		this.added = added;
		this.priority = priority;
	}
	
	/**
	 * 
	 * @return if this DownloadQueueEntry can
	 * currently be downloaded (if a transfer can be initiated)
	 * and getDownload will probably return true
	 */
	public abstract boolean isDownloadable();
	
	/**
	 * requests a download for this file
	 * so DQE recognizes that some download will soon start for this file
	 * 
	 * 
	 * @param fti - where information will be filled
	 * information that will be filled is  
	 * start position and length of the download
	 * additional a HashValue will be filled in if applicable
	 * 
	 * 
	 * @return true if a download can be started and information was filled
	 * into fti  false if no Download can be started..
	 */
	public abstract boolean getDownload(FileTransferInformation fti);
	
	/**
	 * when the download is confirmed a FileInterval for writing
	 * will be created with this method
	 * @param fti - the FTI containing information about 
	 *  the interval range
	 * @return an interval that the soon to be started transfer can write too
	 */
	public abstract AbstractWritableFileInterval getInterval(FileTransferInformation fti);
	
	/**
	 * registers a transfer with this DownloadQueueEntry
	 * @param ft - the fileTransfer that uses this DownloadQueueEntry
	 * (currently this are only downloads later hopefully uploads too)
	 */
	public void startedDownload(AbstractFileTransfer ft) {
		logger.debug("startedDownload()");
		runningFileTransfers.add(ft);
		ft.addObserver(this);
		if (runningFileTransfers.size() == 1) {
			dq.notifyObservers(new StatusObject(this,ChangeType.CHANGED,
					DownloadQueue.DQE_FIRST_TRANSFER_STARTED,ft));
		}
		
		dq.notifyObservers(new StatusObject(this,ChangeType.CHANGED,
				DownloadQueue.DQE_TRANSFER_STARTED,ft));
	}
	
	public final void update(IObservable<TransferChange> ft, TransferChange arg) {
		if (arg == TransferChange.FINISHED) {
			runningFileTransfers.remove(ft); 
			ft.deleteObserver(this);
			dq.notifyObservers(new StatusObject(this,ChangeType.CHANGED,
					DownloadQueue.DQE_TRANSFER_FINISHED,ft));
			
			if (runningFileTransfers.isEmpty()) {
				dq.notifyObservers(new StatusObject(this,ChangeType.CHANGED,
						DownloadQueue.DQE_LAST_TRANSFER_FINISHED,ft));
			}
		}
	}
	
	/**
	 * adds a user to this dqe
	 * 
	 * @param usr the user to be added
	 */
	public void addUser(IUser usr){
		logger.debug("1addding User to DQE:"+usr.getNick()+"  "+getClass().getSimpleName());
		if (!users.contains(usr)) {
			users.add(usr);
			removedUsers.remove(usr);
			synchronized(sync) {
				usr.addDQE(this); 
				logger.debug("2addding User to DQE:"+usr.getNick()+"  "+getClass().getSimpleName());
				addUserSuper(usr);
			}
			dq.notifyObservers(new StatusObject(this,ChangeType.CHANGED));
			
		}
	}
	
	protected void addUserSuper(IUser usr) {}
	
	/**
	 * removes a user from this dqe
	 * @param usr the user to be removed
	 */
	public  void removeUser(IUser usr) {
		if (users.contains(usr)) {
			users.remove(usr);
			removedUsers.add(usr);
			synchronized(sync) {
				usr.removeDQE(this);
				if (type == TransferType.FILELIST) {
					remove();
				} 
				DQEDAO dqedoa = DQEDAO.get(this);
				if (dqedoa != null) {
					dq.getDatabase().deleteUserFromDQE(usr,dqedoa);
				}
			}
			
			dq.notifyObservers(new StatusObject(this,ChangeType.CHANGED));
			
		}
	}
	
	public int getNrOfRunningDownloads() {
		int count = 0;
		for (IFileTransfer aft : runningFileTransfers) {
			if (!aft.isUpload()) {
				count++;
			}
		}
		
		return count;
	}

	/**
	 * @return the number of bytes that were already downloaded for this
	 * DownloadQueueEntry
	 */
	public abstract long getDownloadedBytes();
	
	/**
	 * 
	 * @return how many bytes the download is in total
	 * getDownloaded() / getSize() should be a rough percentage on how 
	 * much has been downloaded so far.
	 */
	public abstract long getSize();
	
	/**
	 * the path where this DownloadQueueEntry will be transfered to 
	 * when the download has finished 
	 * @return
	 */
	public abstract File getTargetPath();
	
	
	public abstract void setTargetPath(File target);
	
	/**
	 * @return the temporary path where the file should 
	 * be downloaded to and where the file is stored while in
	 * progress
	 * 
	 */
	public File getTempPath() { 
		String id = getID().toString();
		File[] files = PI.getTempDownloadDirectory().listFiles();
		if (files != null) {
			for (File f : files) {
				if (f.isFile() && f.getName().contains(id)) {
					return f;
				}
			}
		}
		return new File(PI.getTempDownloadDirectory(), 
				GH.replaceInvalidFilename(getFileName())+"."+id +".dctmp");
	}
	
	/**
	 * convenience method for the name of the file
	 * @return the last part of the path ..
	 */
	public String getFileName() {
		return getTargetPath().getName();
	}
	
	/**
	 * convenience method for returning the parent folder
	 * of the file
	 * @return the parent folder of the path where the file will be 
	 * stored when the download has completed..
	 */
	public File getFolder() {
		return getTargetPath().getParentFile();
	}
	
	/**
	 * 
	 * @return an id for this DQE that will be used
	 * for unique identification
	 * in case of a filelist this will be the
	 * id of the user
	 * for file or TTHL the TTH root of the file
	 */
	public abstract HashValue getID();
	
	/**
	 * 
	 * @return if the download has finished on the DQE
	 */
	public boolean isFinished() {
		return finished != null;
	}
	
	/**
	 * 
	 * @return an IDownlodable if this is a file or TTHl
	 * or null if it is a filelist
	 */
	public abstract IDownloadableFile downloadableData();
	
	
	
	
	public IDownloadable getDownloadable() {
		return downloadableData();
	}

	/**
	 * stores the file to destination after it is finished
	 * if needed the file is copied and deleted as this may 
	 * help sometimes
	 */
	public  boolean  storeToDestination()  {
		synchronized(this) {
			logger.debug("storeToDestination called");		
		}

		File source	=  getTempPath();
		logger.debug("sourcefile"+source);
		File dest	= getTargetPath();
		dest = new File(dest.getParentFile(),GH.replaceInvalidFilename(dest.getName())); // if in last name there is a invalid filename..
		
		logger.debug("destfile"+dest);
		
		try {
			moveFile(source,dest,dq.getDcc());
		} catch(IOException ioe) {
			logger.warn(ioe,ioe);
			return false;
		}
		synchronized(this) {
			logger.debug("storeToDestination finished");		
		}
		return true;
	}
	
	


	/**
	 * removes the file from the download queue and deletes the tempfile
	 */
	public final synchronized void remove() {
		
		for (IUser usr: users) {
			removeUser(usr);
		}
		
		//remove from the persistence..
		DQEDAO del = DQEDAO.get(this);
		if (del != null) {
			dq.getDatabase().deleteDQE(del);
		}
		
		dq.removeFile(this);
		
		for (IFileTransfer ft : new ArrayList<IFileTransfer>(runningFileTransfers)) {
			ft.cancel();
		}
		
		if (getTempPath().isFile()) {
			if (!getTempPath().delete()) {
				getTempPath().deleteOnExit();
				scheduledDeletion(getTempPath(),dq.getDcc());
			}
		}
	}
	
	/**
	 * @return the time this DQE has completed downloading
	 */
	public Date getFinished() {
		return new Date(finished.getTime());
	}
	
	
	/**
	 * @return the time this DQE was created
	 */
	public Date getAdded() {
		return new Date(added.getTime());
	}
	
	
	public void addDoAfterDownload(IDownloadFinished action) {
		downloadFinished.add(action);
	}
	
	protected void executeDoAfterDownload() {
		//execute tasks..
		for (final IDownloadFinished runnable : downloadFinished) {
			DCClient.execute(new Runnable() {
				public void run() {
					runnable.finishedDownload(getTargetPath());
				}
			});
		}
	}
	/**
	 * call back interface
	 * for finished transfers
	 * with this interface jobs can register to receive notification 
	 * when a download is finished..
	 *  
	 * @author Quicksilver
	 *
	 **/
	public static interface IDownloadFinished {
		
		/**
		 * 
		 * @param f - where the file is now
		 */
		void finishedDownload(File f);
	}
	
	/**
	 * tries to move a file..
	 * @param source - from 
	 * @param dest - to
	 * @return if moving was successful.
	 */
	public static boolean moveFile(final File source, File dest,DCClient dcc) throws IOException {
		
		if (dest.isFile()) {
			if (!dest.delete()) {
				throw new IOException("Could not move to destination undeletable file present");
			}
		}
		if (!dest.getParentFile().isDirectory()) {
			if (!dest.getParentFile().mkdirs()) {; //create parent of target ..if not exists..
				throw new IOException("Could not move to destination. Could not create needed Folders. "+dest);
			}
		}
		
		
		logger.debug("renameing "+source+" to "+dest);
		boolean renameWorked = source.renameTo(dest);

		if (!renameWorked) { 
			//rename didn't work .. so  copy
			try {
				GH.copy(source, dest);
				//delete the temp file finally   and if we can't .. mark for deletion on exit ...
				if (!source.delete()) {
					source.deleteOnExit();
					scheduledDeletion(source,dcc);
				}
			} catch (IOException ioe) {
				throw ioe;
			} 
		}
		
		return true;
	}

	/**
	 * creates a scheduler for an undeleted file that tries to delete it
	 * @param toDelete
	 */
	private static void scheduledDeletion(final File toDelete,DCClient dcc) {
		final ScheduledExecutorService ses = dcc.getSchedulerDir();
		if (!ses.isShutdown()) {
			ses.schedule(new Runnable() {
				public void run() {
					if (toDelete.isFile() && !toDelete.delete() && !ses.isShutdown()) {
						ses.schedule(this,30,TimeUnit.SECONDS);
					} 
				}
			}, 30, TimeUnit.SECONDS);
		}
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((getID() == null) ? 0 : getID().hashCode());
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
		final AbstractDownloadQueueEntry other = (AbstractDownloadQueueEntry) obj;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		
		if (getID() == null) {
			if (other.getID() != null)
				return false;
		} else if (!getID().equals(other.getID()))
			return false;
		
		return true;
	}

	/**
	 * compares DQEs for what should be downloaded first
	 * TODO may be also discriminate DQEs that can not be written to currently
	 */
	public int compareTo(AbstractDownloadQueueEntry arg0) {
		int i = type.compareTo(arg0.type);
		if (i != 0) {
			return i;
		} 
		i =  Integer.valueOf(priority).compareTo(Integer.valueOf(arg0.priority));
	
		if (i != 0) {
			return i;
		}
		return Long.valueOf(getDownloadedBytes()).compareTo(Long.valueOf(arg0.getDownloadedBytes()));
		
	}

	public TransferType getType() {
		return type;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
		dq.notifyObservers(new StatusObject(this,ChangeType.CHANGED));
	}
	
	DCClient getDCC() {
		return dq.getDcc();
	}
	



	public Set<IUser> getUsers() {
		return Collections.unmodifiableSet(users);
	}
	
	public int getNrOfUsers() {
		return users.size();
	}
	
	public int getNrOfOnlineUsers() {
		int count = 0;
		for (IUser u:users) {
			count += u.isOnline() ? 1 : 0 ; 
		}
		
		return count;
	}

	public Set<IUser> getRemovedUsers() {
		return Collections.unmodifiableSet(removedUsers);
	}
	
	
	
	public List<IFileTransfer> getRunningFileTransfers() {
		return Collections.unmodifiableList(runningFileTransfers);
	}

	public String toString() {
		return getFileName()+" Users: "+getNrOfUsers();
	}
	
	/**
	 * 
	 * @return miilliseconds expected until finishing..
	 * 
	 */
	public long getTimeRemaining() {
		long leftBytes = getSize()-getDownloadedBytes();
		long totalSpeed = 0;
		for (IFileTransfer ft:runningFileTransfers) {
			totalSpeed += ft.getSpeed();
		}
		if (totalSpeed == 0) {
			return Long.MAX_VALUE;
		} else {
			return leftBytes / totalSpeed;
		}
	}
	
	
	/**
	 * determines the highest common folder of a list of Files..
	 * 
	 * @param dqes
	 * @return
	 */
	public static File getCommonParent(List<AbstractDownloadQueueEntry> dqes) {
		HashSet<File> folders = new HashSet<File>();
		for (AbstractDownloadQueueEntry dqe: dqes) {
			folders.add(dqe.getFolder());
		}
		return getCommonParent(folders);
	}
	
	private static File getCommonParent(Set<File> files) {
		File currentCommon = null;
		for (File f: files) {
			if (currentCommon == null) {
				currentCommon = f;
			} else {
				currentCommon = commonParent(currentCommon,f);
				if (currentCommon == null) {
					return null;
				}
			}
		}
		return currentCommon;
	}
	
	private static boolean isParent(File folder, File possiblechild) {
		if (folder.equals(possiblechild)) {
			return true;
		} else {
			File between = possiblechild.getParentFile();
			if (between == null) {
				return false;
			} else {
				return isParent(folder,between);
			}
		}
	}
	
	private static File commonParent(File f1, File f2) {
		if (f1.equals(f2)) {
			return f1;
		} else if (isParent(f1,f2)) {
			return f1;
		} else if (isParent(f2,f1)) {
			return f2;
		} else {
			File parf1 =f1.getParentFile();
			File parf2 = f2.getParentFile();
			if (parf1 != null && parf2 != null) {
				return commonParent(parf1, parf2);
			} else {
				return null;
			}
		}
	}

	DownloadQueue getDq() {
		return dq;
	}
	
}
