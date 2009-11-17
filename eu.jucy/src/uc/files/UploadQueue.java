package uc.files;


import helpers.Observable;
import helpers.StatusObject;
import helpers.StatusObject.ChangeType;


import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import logger.LoggerFactory;


import org.apache.log4j.Logger;


import uc.DCClient;
import uc.IHasUser;
import uc.IUser;
import uc.crypto.HashValue;



/**
 * a queue that keeps an eye on who wants to
 * download what from us, also who has downloaded what from us
 * and in Future.. who should get  a slot next from us
 * 
 * Though UploadQueue now also manages the Transfers..
 * may be better would be moving this both into a new class
 * and let upload Queue really only be UploadQueue and not Finished 
 * transfers for down as well as up
 *  
 * 
 * @author Quicksilver
 *
 */
public class UploadQueue extends Observable<StatusObject> implements IUploadQueue {
	
	private static Logger logger = LoggerFactory.make(); 

	
	private static final int MAX_TRANSFERRECORD_SIZE = 10000;
	
	
	private final Map<IUser,UploadInfo> requestedFiles = new HashMap<IUser,UploadInfo>();
	
	private final Map<IUser,UserInfo> recordsPerUser = new HashMap<IUser,UserInfo>(); 
	
	
	private final List<TransferRecord> transferRecords = new LinkedList<TransferRecord>();
	
	
	//for average speed
	
	private volatile long totalDuration = 0;
	private volatile long totalSize = 0;


	private final DCClient dcc;

	public UploadQueue(DCClient dcc) {
		this.dcc = dcc;
	}
	
	/* (non-Javadoc)
	 * @see uc.files.IUploadQueue#start()
	 */
	public void start() {
		dcc.getSchedulerDir().scheduleAtFixedRate(new Runnable() {
			public void run() {
				clean();
			}
			
		}, 24 * 3600, 1*3600, TimeUnit.SECONDS); //hours no longer exists.. 
	}

	
	/* (non-Javadoc)
	 * @see uc.files.IUploadQueue#userRequestedFile(uc.IUser, java.lang.String, uc.crypto.HashValue, boolean)
	 */
	public void userRequestedFile(IUser usr,String nameOfTransferred,HashValue hash,boolean gotASlot) {
		if (nameOfTransferred == null) {
			nameOfTransferred = hash.toString();
		}
		UploadInfo present;
		boolean added;
		synchronized(requestedFiles) {
			present = requestedFiles.get(usr);
			if (present == null) {
				present = new UploadInfo(usr,nameOfTransferred,hash,gotASlot);
				requestedFiles.put(usr, present);
				added = true;
			} else {
				present.requested(nameOfTransferred, hash, gotASlot);
				added = false;
			}
		}
		notifyObservers(new StatusObject(present, added? ChangeType.ADDED : ChangeType.CHANGED));
	}
	
	/* (non-Javadoc)
	 * @see uc.files.IUploadQueue#transferFinished(java.io.File, uc.IUser, java.lang.String, uc.crypto.HashValue, long, java.util.Date, long)
	 */
	public void transferFinished(File file,IUser usr,String nameOfTransferred,HashValue hashOfTransferred,long bytesServedToUser,Date startTime,long timeNeeded) {
		UploadInfo present = null;
		synchronized(requestedFiles) {
			present = requestedFiles.get(usr);
			if (present != null) {
				present.uploaded(bytesServedToUser);
			}
		}
		if (present != null) {
			notifyObservers(new StatusObject(present,ChangeType.CHANGED));
		}

		logger.debug(timeNeeded+" time needed" );
		TransferRecord up = null ;
		boolean isNew = true;
		
		UserInfo record = recordsPerUser.get(usr);
		if (record != null) {
			for (TransferRecord ur:record.records) {
				if (ur.matches(usr, hashOfTransferred)) {
					up = ur;
					isNew = false;
					break;
				}
			}
		}
		if (up == null) {
			up = new TransferRecord(file,nameOfTransferred,hashOfTransferred,bytesServedToUser,startTime,timeNeeded,usr);
			if (record == null) {
				record = new UserInfo();
				recordsPerUser.put(usr, record);
			}
			record.records.add(up);
		}
		
		
		TransferRecord removed = null;
		
		synchronized (transferRecords) {
			if (isNew) {
				transferRecords.add(up);
			} else {
				up.update(nameOfTransferred, bytesServedToUser, startTime, timeNeeded);
			}
			totalDuration += timeNeeded;
			totalSize += bytesServedToUser;
			record.totalsize+=bytesServedToUser;
			record.totalTime+= timeNeeded;
			if (transferRecords.size() >= MAX_TRANSFERRECORD_SIZE) { //Delete what is too much.
				removed = transferRecords.remove(0);
				List<TransferRecord> removerecords = recordsPerUser.get(removed.user).records;
				removerecords.remove(removed);
			/*	if (removerecords.isEmpty()) {
					recordsPerUser.remove(removed.user);
				} */
			}
		}
		if (removed != null) {
			notifyObservers(new StatusObject(removed,ChangeType.REMOVED));
		}
		notifyObservers(new StatusObject(up,isNew?ChangeType.ADDED:ChangeType.CHANGED));
	}
	

	/**
	 * cleans once a day old entries.. just for long running clients to prevent 
	 * a memory leak..
	 */
	private void clean() {
		Date oneDayOld = new Date(System.currentTimeMillis()-24*3600 *1000);
		synchronized(requestedFiles) {
			logger.debug("in clean");
			for (UploadInfo ui: new ArrayList<UploadInfo>(requestedFiles.values())) {
				if (ui.getLastRequest().before(oneDayOld)) {
					logger.debug("removing: "+ui.user);
					requestedFiles.remove(ui.user);
				} 
			}
		}
	}
	


	
	/* (non-Javadoc)
	 * @see uc.files.IUploadQueue#getTransferRecords()
	 */
	public List<TransferRecord> getTransferRecords() {
		synchronized(transferRecords) {
			return new ArrayList<TransferRecord>(transferRecords);
		}
	}
	

	/* (non-Javadoc)
	 * @see uc.files.IUploadQueue#getUploadRecordsSize()
	 */
	public int getUploadRecordsSize() {
		synchronized(transferRecords) {
			return transferRecords.size();
		}
	}
	
	/* (non-Javadoc)
	 * @see uc.files.IUploadQueue#getTotalDuration()
	 */
	public long getTotalDuration() {
		return totalDuration;
	}


	/* (non-Javadoc)
	 * @see uc.files.IUploadQueue#getTotalSize()
	 */
	public long getTotalSize() {
		return totalSize;
	}

	/* (non-Javadoc)
	 * @see uc.files.IUploadQueue#getUploadInfos()
	 */
	public List<UploadInfo> getUploadInfos() {
		synchronized(requestedFiles) {
			return  new ArrayList<UploadInfo>(requestedFiles.values());
		}
	}


	/* (non-Javadoc)
	 * @see uc.files.IUploadQueue#getTotalTransferredOf(uc.IUser)
	 */
	public long getTotalTransferredOf(IUser usr) {
		synchronized (recordsPerUser) {
			UserInfo ui = recordsPerUser.get(usr); 
			if (ui != null) {
				return ui.totalsize;
			} else {
				return 0;
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see uc.files.IUploadQueue#getTimeNeededOf(uc.IUser)
	 */
	public long getTimeNeededOf(IUser usr) {
		synchronized (recordsPerUser) {
			UserInfo ui = recordsPerUser.get(usr); 
			if (ui != null) {
				return ui.totalTime;
			} else {
				return 0;
			}
		}
	}

	public static class UploadInfo implements IHasUser {
		
		private final IUser user;



		/**
		 * the last file he requested
		 */
		private volatile String requested;
		
	

		/**
		 * root-hash of the file or interleaves
		 * requested, null if FileList
		 */
		private HashValue hash;
		
		/**
		 * time of the first request by that user
		 */
		private final long firstrequest;
		
		/**
		 * time this was requested last..
		 */
		private long lastRequest;
		
		/**
		 * how often he requested a file since then
		 */
		private int numberOfRequestsSinceThen;
		
		/**
		 * how many was uploaded total on bytes to the user
		 */
		private long uploadedTotal;
		
		/**
		 *  if the last request got a slot
		 */
		private boolean slot;
		
		
		public UploadInfo(IUser usr,String requested,HashValue hash, boolean slot) {
			this.user = usr;
			this.requested = requested;
			this.hash = hash;
			this.firstrequest = System.currentTimeMillis();
			lastRequest = firstrequest;
			numberOfRequestsSinceThen = 1;
			this.slot = slot;
		}
		
		public synchronized void requested(String requested, HashValue hash, boolean slot) {
			this.requested = requested;
			this.hash = hash;
			lastRequest = System.currentTimeMillis();
			numberOfRequestsSinceThen++;
			this.slot = slot;
		}
		
		public synchronized void uploaded(long bytes) {
			this.uploadedTotal += bytes; 
		}
		
		/**
		 * @return the user.. 
		 */
		public IUser getUser() {
			return user;
		}
		
		public synchronized String getRequested() {
			return requested;
		}

		public synchronized HashValue getHash() {
			return hash;
		}

		public Date getFirstrequest() {
			return new Date(firstrequest);
		}

		public synchronized Date getLastRequest() {
			return new Date(lastRequest);
		}

		public synchronized int getNumberOfRequestsSinceThen() {
			return numberOfRequestsSinceThen;
		}

		public synchronized long getUploadedTotal() {
			return uploadedTotal;
		}

		public synchronized boolean isSlot() {
			return slot;
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
			UploadInfo other = (UploadInfo) obj;
			if (user == null) {
				if (other.user != null)
					return false;
			} else if (!user.equals(other.user))
				return false;
			return true;
		}
		
	}
	
	public static class TransferRecord implements IHasUser {
		
		/**
		 * what was uploaded.
		 */
		private volatile String name;
		
		/**
		 * null if FileList.. else
		 * the RootHash of the file or TTHL
		 */
		private final HashValue root;
		
		private volatile long size;
		
		private final Date starttime;
		
		private volatile Date endTime;
		

		private volatile long timeNeeded;


		/**
		 * to whom we uploaded
		 */
		private final IUser user;
		
		/**
		 * file transferred.. may be null
		 */
		private final File file;
		

	

		public TransferRecord(File file,String name,HashValue what, long size,Date starttime,long timeNeeded, IUser target) {
			this.file = file;
			this.name = name;
			this.root = what;
			this.size = size;
			this.timeNeeded=timeNeeded;
			this.user = target;
			this.starttime = new Date(starttime.getTime());
			this.endTime = new Date(starttime.getTime()+timeNeeded);
		}
		
		public void update(String name,long size,Date starttime,long timeNeeded) {
			this.name = name;
			this.size += size;
			this.endTime = new Date(starttime.getTime()+timeNeeded);
			this.timeNeeded+= timeNeeded;
		}
		
		public boolean matches(IUser usr,HashValue what) {
			if (usr.equals(user)) {
				if (what == null || root == null) {
					return what == root;
				} else {
					return what.equals(root);
				}
			}
			return false;
		}

		public String getName() {
			return name;
		}

		public long getSize() {
			return size;
		}

		public IUser getUser() {
			return user;
		}
		
		/**
		 * start time on first segment of the file
		 * @return
		 */
		public Date getStarttime() {
			return new Date(starttime.getTime());
		}
		
		/**
		 * end time of last segment of the file
		 * @return
		 */
		public Date getEndTime() {
			return new Date(endTime.getTime());
		}
		
		public long getTimeNeeded() {
			return timeNeeded;
		}
		
		public File getFile() {
			return file;
		}
			
	}
	
	private static class UserInfo {
		private volatile long totalsize = 0;  //totalsize of all records of this user..
		private volatile long totalTime =0; //how long it took for all the records..
		private final List<TransferRecord> records = new CopyOnWriteArrayList<TransferRecord>();
	}

	
}
