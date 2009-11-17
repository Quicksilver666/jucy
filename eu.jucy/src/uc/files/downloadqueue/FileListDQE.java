package uc.files.downloadqueue;

import helpers.GH;

import java.io.File;
import java.util.Date;

import uc.PI;
import uc.User;
import uc.crypto.HashValue;
import uc.files.IDownloadable.IDownloadableFile;
import uc.files.filelist.FileListDescriptor;
import uc.files.transfer.AbstractWritableFileInterval;
import uc.files.transfer.FileTransferInformation;
import uc.files.transfer.AbstractWritableFileInterval.FileListWriteInterval;
import uc.protocols.TransferType;

public class FileListDQE extends AbstractDownloadQueueEntry {
	
	private volatile long size = 0;
	
	/**
	 * whom this filelist belongs to 
	 */
	private final User owner;
	
	/**
	 * for partial filelist..
	 */
	private final String fileListPath; //TODO 
	


	private FileListDQE(DownloadQueue dq,User from) {
		this(dq,from,"/");
	}
	
	private FileListDQE(DownloadQueue dq,User from,String path) { //TODO may be sort of merge.. merge with last filelist on download..
		super(dq,TransferType.FILELIST,254,new Date());
		owner = from;
		addUser(from);
		this.fileListPath = path;
	}
	
	
	public static FileListDQE get(User from,DownloadQueue dq) {
		FileListDQE dqe = (FileListDQE)dq.get(from.getUserid());
		if (dqe == null) {
			dqe = new FileListDQE(dq,from);
			dq.addDownloadQueueEntry(dqe);
		} 
		return dqe;
	}

	@Override
	public boolean getDownload(FileTransferInformation fti) {
		if (isDownloadable()) {
			fti.setStartposition(0);
			fti.setLength(-1);
			fti.setType(type);
			return true;
		}
		return false;
	}

	/**
	 * always zero.. as a FileList is always 
	 * downloaded in one run..
	 */
	@Override
	public long getDownloadedBytes() {
		return 0;
	}

	@Override
	public HashValue getID() {
		return owner.getUserid();
	}

	@Override
	public AbstractWritableFileInterval getInterval(FileTransferInformation fti) {
		size = fti.getLength(); //store the size .. so it can be seen in the gui
		return new FileListWriteInterval(this,fti.getLength());
	}

	@Override
	public File getTargetPath() {
		return new File(PI.getFileListPath(),
					GH.replaceInvalidFilename(
						owner.getNick()+"."+owner.getUserid()+".xml.bz2"
					)
				);
	}
	

	@Override
	public boolean isDownloadable() {
		return !isFinished();
	}
	
	/**
	 * as this is a FileList also remove the FileList when the user
	 * is removed.
	 */
	public void removeUser(User usr) {
		if (users.contains(usr)) {
			users.remove(usr);
			usr.removeDQE(this);
			remove();
		}
	}
	
	/**
	 * called when download is finished
	 * handles finishing tasks for the FileList.. and removing
	 * 
	 */
	public void downloadedFilelist() {
		finished = new Date();
		storeToDestination();
		FileListDescriptor fd = new FileListDescriptor(owner,getTargetPath());
		owner.setFilelistDescriptor(fd);
		fd.processFilelistAfterDownload(); 
		

		executeDoAfterDownload();
		//remove.. from the queue
		remove();
	}

	public long getSize() {
		return size;
	}


	@Override
	public IDownloadableFile downloadableData() {
		return null;
	}
	
	
	@Override
	public void setTargetPath(File target) {
		throw new UnsupportedOperationException("FileList can only be downloaded to FileList dir ");
	}

	public String getFileListPath() {
		return fileListPath;
	}
	
}
