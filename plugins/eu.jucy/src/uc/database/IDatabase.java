package uc.database;



import java.io.File;
import java.util.BitSet;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uc.DCClient;
import uc.IUser;
import uc.crypto.HashValue;
import uc.crypto.InterleaveHashes;
import uc.files.downloadqueue.DQEDAO;

/**
 * interface defining a plug-in used for persistence of 
 * DownloadQueueEntrys
 *  and interleaves of hashed files
 * 
 * @author quicksilver
 *
 */
public interface IDatabase {

	/**
	 * the id for the extension point ... must match xml
	 */
	public static final String  ExtensionpointID	= "eu.jucy.IDatabase" ;
	
	/**
	 * the id of the default implementation
	 */
	public static final String defaultID	=	"eu.jucy.database" ;
	
	/**
	 * initialises the database...
	 * @param storagepath - a path where UC stores its data see PI.getStoragePath()
	 * @throws an exception .. if the backing storage is not available 
	 * or something unexpected happens..
	 */
	void init(File storagepath,DCClient dcc) throws Exception;
	
	/**
	 * called when a file was hashed to add it to or update it in the database..
	 * user changes are not handled
	 * 
	 * @param file - the path of the file on the disc
	 * @param inter - the interleave hashes..
	 * @param hashed - when the file was hashed last time
	 */
	void addOrUpdateFile(HashedFile hf , InterleaveHashes inter);
	
	

	
	/**
	 * retrieves the interleaves for a hashed file
	 * @param tthroot - the TTHRoot 
	 * @return InterleaveHashes that match the provided TTHroothash
	 */
	InterleaveHashes getInterleaves(HashValue tthroot);
	
	
	/**
	 * retrieves all files that have been hashed..
	 * so the own FileList can be build..
	 * this is a lot faster than starting a query for each File
	 * @return  mapping of all Files to their hash information
	 */
	Map<File,HashedFile> getAllHashedFiles();
	
	/**
	 * 
	 * @return single hashed file form the database
	 */
	HashedFile getHashedFile(File f);
	
	
	/**
	 * deletes all hashed files from the Database
	 * making room for a rebuild.
	 */
	void deleteAllHashedFiles();
	
	/**
	 * deletes all hashed files that are currently not used
	 * (don't exist on disc) 
	 * 
	 * @return all files deleted from DB..
	 */
	Map<File,HashedFile> pruneUnusedHashedFiles();
	//End hashing part...
	
	//Start DQE and user part
	
	/**
	 * stores or updates an already persistent DQE
	 * in the database
	 * 
	 * @param dqe - the dqe to be updated
	 * @param add true if added .. false for update..
	 */
	void addOrUpdateDQE(DQEDAO dqe,boolean add);
	
	/**
	 * removes a dqe from persistence..
	 * @param dqe - 
	 */
	void deleteDQE(DQEDAO dqe);
	
	/**
	 * requests the user to be persisted..
	 * 
	 * @param usr - the user .. where user.shouldBeStored() decides
	 * whether the user should be persisted.. -> if false delete
	 * if true -> add or update  (adds dqe)
	 */
	void addUpdateOrDeleteUser(IUser usr);
	
	
	/**
	 * loads all users from the Database..
	 * (users that are favorite or have a favSlot.. and users )
	*
	 * retrieves all DQEs from the DB
	 *
	 */
	Set<DQEDAO> loadDQEsAndUsers();
	
	
	/**
	 * add restoreinfo to a DQE
	 * 
	 * @param hash - which dqe
	 * @param restoreInfo - what restore info bits..
	 */
	void addRestoreInfo(HashValue hash,BitSet restoreInfo);
	
	/**
	 * deletes a user from the provided DQE..
	 * 
	 * @param usr - the user ..
	 * @param dqe - the DQE where to delete from
	 */
	void deleteUserFromDQE(IUser usr,DQEDAO dqe );
	
	/**
	 * same just with adding...
	 * @param usr
	 * @param dqe
	 */
	void addUserToDQE(IUser usr,HashValue hash);
	
	
	/**
	 * on normal close this will be called..
	 * so the database can shut down..
	 */
	void shutdown();
	
	/**
	 * adds a Log entry to the database.
	 * @param logentry
	 */
	void addLogEntry(ILogEntry logentry);
	
	/**
	 * load logentrys from the database
	 * @param entityID - by the given entityID
	 * @param max - only the latest max entrys..
	 * @param offset - start from element? 
	 * @return the entrys matching the given id..
	 */
	List<ILogEntry> getLogentrys(HashValue entityID, int max, int offset);
	
	/**
	 * @param entityID - logentrys of which entity should be counted.. if null all are counted..
	 * @return the total number of logentrys..
	 */
	int countLogentrys(HashValue entityID);
	
	/**
	 * 
	 * @return all log entitys that have messages saved in the db..
	 */
	List<DBLogger> getLogentitys();
	
	
	/**
	 * 
	 * @param entityID - log entrys of whom to prune.. null for everyone
	 * @param before -  log entrys before this date should be deleted
	 */
	void pruneLogentrys(HashValue entityID,Date before);
	
}
