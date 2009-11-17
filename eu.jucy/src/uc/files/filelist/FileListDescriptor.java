package uc.files.filelist;

import java.io.File;
import java.lang.ref.WeakReference;

import logger.LoggerFactory;

import org.apache.log4j.Logger;

import uc.DCClient;
import uc.User;

public class FileListDescriptor {

	private static final Logger logger = LoggerFactory.make();
	
	private final User usr;
	
	private final File fileListPath;
	
	/**
	 * only a weak reference to the filelist is kept so the memory can be reused
	 * 
	 */
	private WeakReference<FileList> filelistreferent = null;


	private final boolean ownFilelist;

	/**
	 * constructor used for normal filelists..
	 * 
	 * @param usr the usr
	 * @param filelist  a path to the filelist on the disc
	 */
	public FileListDescriptor(User usr, File fileListPath){
		this.usr = usr;	
		this.fileListPath = fileListPath;
		ownFilelist = false;
	}
	
	/**
	 * constructor used for our own filelist
	 * 
	 * @param fielelist - the list that is already open 
	 * @param usr the usr
	 * @param fileList  a path to the filelist on the disc
	 */
	public FileListDescriptor(User usr, FileList fileList) {
		this.usr = usr;	
		this.fileListPath = null;
		filelistreferent = new WeakReference<FileList>( fileList);
		ownFilelist = true;
	}
	
	/**
	 * 
	 * 
	 * @return the FileList described by this descriptor..
	 */
	public FileList getFilelist() {
		return loadFileList(false);
	}
	
	/**
	 * same as getFilelist just with the difference that here 
	 * FilelistProcessors are told that opening was just after download.
	 * 
	 * @return the FileList described by this descriptor..
	 */
	public void processFilelistAfterDownload() {
		loadFileList(true);
	}
	
	
	private synchronized FileList loadFileList(boolean onDownload) {
		FileList fileList = null;
		if (filelistreferent == null || (fileList=filelistreferent.get()) == null) {
			fileList = new FileList(usr);
			filelistreferent = new WeakReference<FileList>( fileList);
			boolean readSuccess = fileList.readFilelist(fileListPath);
			if (!readSuccess) {
				logger.info("Problems reading Filelist of user "+usr.getNick());
			}
			
			for (IFilelistProcessor ifp: DCClient.get().getFilelistProcessors()) {
				ifp.processFilelist(fileList,onDownload);
			}
		}

		return fileList;
		
	}
	
	public void delete() {
		if (fileListPath != null && !fileListPath.delete()) {
			fileListPath.deleteOnExit();
		}
	}
	
	/**
	 * 
	 * @return if the descriptor will return a FileList
	 * on calling the
	 */
	public boolean isValid() {
		return ownFilelist ||fileListPath.isFile();
	}
	
	/**
	 * @return the usr
	 */
	public User getUsr() {
		return usr;
	}

	public File getFileListPath() {
		return fileListPath;
	}
	
}
