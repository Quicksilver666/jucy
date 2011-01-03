package uc.files.downloadqueue;


import java.io.File;
import java.util.Date;


import uc.IUser;
import uc.crypto.HashValue;
import uc.files.IDownloadable.IDownloadableFile;
import uc.protocols.TransferType;


/**
 * TTHLDQE and FileDQE share a lot of common functionality..
 * -IDwonloadableFile
 * - target path
 * 
 * the common functionality is implemented here..
 * 
 * @author Quicksilver
 *
 */
public abstract class AbstractFileDQE extends AbstractDownloadQueueEntry {

	
	protected volatile File target;
	
	protected final IDownloadableFile file;
	
	protected AbstractFileDQE(DownloadQueue dq,TransferType type,File target,IDownloadableFile file, int priority,Date added) {
		super(dq,type,priority,added);
		this.target = target;
		this.file = file;
	}

	@Override
	public synchronized File getTargetPath() {
		return target;
	}
	
	public HashValue getTTHRoot() {
		return file.getTTHRoot();
	}
	
	
	@Override
	public IDownloadableFile downloadableData() {
		return file;
	}
	
	/**
	 * changes old path to new
	 * and updates the database..
	 */
	@Override
	public synchronized void setTargetPath(File target) {
		if (!this.target.equals(target)) {
			if (dq.containsDQE(this)) {
				dq.removeFile(this);
				this.target = target;
				dq.addDownloadQueueEntry(this);
				updateDB();
			} else {
				this.target = target;
			}
		}
	}
	
	

	@Override
	public long getSize() {
		return file.getSize();
	}

	@Override
	public HashValue getID() {
		return file.getTTHRoot();
	}

	@Override
	public void setPriority(int priority) {
		super.setPriority(priority);
		updateDB();
	}
	
	protected void updateDB() {
		dq.getDatabase().addOrUpdateDQE(DQEDAO.get(this),false);
	}
	
	@Override
	protected void addUserSuper(IUser usr) {
		dq.getDatabase().addUserToDQE(usr,file.getTTHRoot());
	}
			
	

}
