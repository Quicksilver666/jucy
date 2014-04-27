package uc.files;

import java.io.File;


import uc.IUser;
import uc.IHasUser.IMultiUser;
import uc.crypto.HashValue;
import uc.files.downloadqueue.AbstractDownloadQueueEntry;


/**
 * 
 * @author Quicksilver <p>
 * 
 *  This interface symbolises a file that can be downloaded ... either from search or a filelist
 *
 */
public interface IDownloadable  extends IMultiUser, IHasDownloadable {
	
	/**
	 * @return the user that has this file
	 * 
	 */
	IUser getUser();


	
	/**
	 * 
	 * @return usually one..
	 * though returns the number of users
	 * getIterable() will return..
	 */
	int nrOfUsers();
	
	/**
	 * path must use  java.io.File.separator for separation
	 * @return the path of the downloadable inclusive its name
	 */
	String getPath();

	
	/**
	 * Returns the name of the file or directory denoted by this IDownloadable 
	 * 
	 * This is just the last name in the pathname's name sequence. 
	 * If the pathname's name sequence is empty or an error occurs, then the empty string is returned.
	 * 
	 * @return the name ...
	 * 
	 */
	String getName();
	
	
	/**
	 * 
	 * @return true if this IDownloadable represents a file
	 *  false for a folder /otherwise..
	 */
	boolean isFile();
	
	
	/**
	 * 
	 * @return the path with stripped off name
	 * 
	 * getOnlyPath()+File.separator+ getName() should equal getPath()
	 */
	public String getOnlyPath();
	
	
	/**
	 * if it is a folder it throws an UnsupportedOperationException
	 * @return if this is a file
	 * return the TTH ROOT
	 * 
	 * @throws UnsupportedOperationException 
	 */
	HashValue getTTHRoot() throws UnsupportedOperationException;
	
	
	/**
	 * adds the current IDownloadable
	 * to the DownloadQueue and returns
	 * the DQE so other modifications can be done..
	 * target is the default path with the provided name of the file
	 * @return a DownloadQueueEntry .. so listeners can be added..
	 */
	AbstractDownloadQueueEntry download();
	
	/**
	 * 
	 * @param target - where the file/folder should be downloaded to 
	 * the target should denote the file/folder itself not the parent folder..
	 * 
	 * @return  a DownloadQueueEntry for adding listeners and actions 
	 * on completed download..
	 */
	AbstractDownloadQueueEntry download(File target);
	
	
	
	public static interface IDownloadableFile extends IDownloadable {
		

		
		/**
		 * 
		 * @return the Length of the file  or -1 if unknown
		 * 
		 */
		long getSize();
		
		
		/**
		 * 
		 * @return the file ending without the dot... empty string if can not be determined
		 */
		String getEnding(); 
		
	}
	
	public static interface IDownloadableFolder extends IDownloadable {}
}
