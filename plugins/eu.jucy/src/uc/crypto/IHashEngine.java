package uc.crypto;

import java.io.File;

import uc.IStoppable;
import uc.database.HashedFile;




/**
 * 
 * A hash engine is a kind of job queue for hash jobs
 * 
 *  there are two kind of jobs a HashEngine must support
 *  
 *  one is to check an interval/Block of an file while
 *  the other is hashing whole files to create interleaves..  
 * 
 * @author Quicksilver
 *
 */
public interface IHashEngine extends IStoppable {
	
	/**
	 * the id for the extension point ... see plugin.xml
	 */
	public static final String ExtensionpointID="eu.jucy.IHashEngine";
	
	/**
	 * the id of the default implementation
	 */
	public static final String defaultID="eu.jucy.hashengine";
	
	/**
	 * initialise the HashEngine ... 
	 */
	void init();
	
	/**
	 * request all hashjobs being stopped for shutdown of client...
	 */
	void stop();
	
	/**
	 * registers  listener to be notified on all completed hashFile
	 * operations .. this is used by the UI to fill the StatusBar
	 * @param listener
	 */
	void registerHashedListener(IHashedListener listener);
	
	/**
	 * registers  listener to be notified on all completed hashFile
	 * operations .. this is used by the UI to fill the StatusBar
	 * @param listener
	 */
	void unregisterHashedListener(IHashedListener listener);

	
	/**
	 * tries to verify a blocks asynchronously
	 * though with higher priority than normal hashing of files
	 * @param block - the block to be verified
	 * @param checkListener the listener to be called when verification is done..
	 */
	void checkBlock(IBlock block,VerifyListener checkListener);
	
	/**
	 * requests to hash a file ...
	 * @param f - the file to hash
	 * @param highPriority - false for normal priority -> 
	 * 			high priority for same priority as Block verification
	 * @param listener - a listener for call-back when done
	 */
	void hashFile(File f,boolean highPriority,IHashedFileListener listener);
	
	
	/**
	 * verifies the interleave hashes against the root hash
	 * 
	 * @param hashes
	 * @param root
	 * @return true if the verification was successful
	 */
	boolean verifyInterleaves(InterleaveHashes hashes, HashValue root  );
	
	
	/**
	 * called by client to delete any pending file-hash jobs
	 * 
	 * this is done for example when the shared directories change
	 * if for example a directory is removed from shared folders
	 * the this will be called to stop possibly useless hashing
	 */
	void clearFileJobs();

	
	public static interface VerifyListener {
		
		/**
		 * @param verified - true if an only if the verification was successful 
		 */
		void checked(boolean verified);
	}
	/**
	 * 
	 * a listener the HashEngine calls when it has finished to hash a file
	 * @author Quicksilver
	 */
	public static interface IHashedFileListener {
		
		/**
		 * when a file is hashed this listener is called by the IHashEngine
		 * @param f - the file that was hashed
		 * @param root - the tthRoot  of the file
		 * @param ilh - the interleave hashes of the file
		 * @param before
		 */
		void hashedFile(HashedFile hashedFile,InterleaveHashes ilh);
		
	}
	
	/**
	 * general listener for the GUI
	 * to receive notification on finished hash operations..
	 * 
	 * @author Quicksilver
	 *
	 */
	public static interface IHashedListener {
		/**
		* notifies what file was hashed what time was needed
		* and the nr of parallel hash operation that are running in parallel
		*/
		void hashed(File f,long duration, long remainingSize);
	}

}
