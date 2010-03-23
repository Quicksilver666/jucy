package uc.files.downloadqueue;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import uc.IUser;
import uc.User;
import uc.crypto.HashValue;
import uc.crypto.InterleaveHashes;
import uc.files.AbstractDownloadable.AbstractDownloadableFile;
import uc.files.IDownloadable.IDownloadableFile;



/**
 * stores all Data needed for persistence of 
 * TTHL and FileDQEs.  
 * 
 * @author quicksilver
 *
 */
public class DQEDAO extends AbstractDownloadableFile implements IDownloadableFile {
	
	private final Date added;
	
	private final HashValue hashValue;
	
	private final Set<IUser> users;
	
	private final int priority;
	
	private final long size;
	
	private final File target;
	
	/**
	 * the interleave hashes in case of a file DQE ..
	 * null otherwise
	 */
	private final InterleaveHashes ih;
	
	private DQEDAO(AbstractDownloadQueueEntry adqe, File target,InterleaveHashes ih, IDownloadableFile file) {
		added = adqe.getAdded();
		this.hashValue = file.getTTHRoot();
		users = new CopyOnWriteArraySet<IUser>(adqe.getUsers());
		priority = adqe.getPriority();
		this.target = target;
		this.ih = ih ;
		this.size = file.getSize();
	}
	
	public DQEDAO(HashValue tthRoot, Date added, int priority,File target,InterleaveHashes ih, long size) {
		this.added = new Date(added.getTime());
		this.hashValue = tthRoot;
		this.priority = priority;
		this.target = target;
		this.ih = ih;
		this.users = new CopyOnWriteArraySet<IUser>();
		this.size = size;
	}
	
	public DQEDAO(TTHLDQE dqe) {
		this(dqe, dqe.getTargetPath(),null,dqe.downloadableData());
	}
	
	public DQEDAO(FileDQE dqe,InterleaveHashes ih) {
		this(dqe,dqe.getTargetPath(),ih,dqe.downloadableData());

	}
	
	public static DQEDAO get(AbstractDownloadQueueEntry adqe) {
		if (adqe instanceof TTHLDQE) {
			return new DQEDAO((TTHLDQE) adqe);
		} else if (adqe instanceof FileDQE) {
			return new DQEDAO((FileDQE)adqe,((FileDQE)adqe).getIh());
		} else {
			return null;
		}
		
	}
	
	/*
	 * restores the download queue entry from this persistence entry
	 * the entry is added to the DQE
	 *
	public void restore() {
		FileDQE.restore(this);
	} */

	public Date getAdded() {
		return new Date(added.getTime());
	}

	public HashValue getTTHRoot() {
		return hashValue;
	}

	/*public Set<User> getUsers() {
		return users;
	} */
	
	@Override
	public Collection<IUser> getIterable() {
		return users;
	}

	@Override
	public int nrOfUsers() {
		return users.size();
	}

	public void addUser(User usr) {
		users.add(usr);
	}

	public int getPriority() {
		return priority;
	}

	public File getTarget() {
		return target;
	}

	public InterleaveHashes getIh() {
		return ih;
	}


	public long getSize() {
		return size;
	}

	@Override
	public String getPath() {
		return target.toString();
	}

	@Override
	public User getUser() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return "DQEDAO [hashValue=" + hashValue + ", size=" + size
				+ ", target=" + target + "]";
	}
	
	
	

}
