package uc.files.downloadqueue;


import helpers.GH;
import helpers.Observable;
import helpers.PreferenceChangedAdapter;
import helpers.StatusObject;
import helpers.StatusObject.ChangeType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

import logger.LoggerFactory;

import org.apache.log4j.Logger;
import org.eclipse.ui.PlatformUI;

import eu.jucy.language.LanguageKeys;



import uc.DCClient;
import uc.PI;
import uc.crypto.HashValue;
import uc.database.IDatabase;
import uc.files.IDownloadable;
import uc.files.IDownloadable.IDownloadableFile;
import uc.files.filelist.FileList;
import uc.protocols.TransferType;


public class DownloadQueue extends Observable<StatusObject> {
	
	public static final int FILEDQE_BLOCKSTATUSCHANGED = 1,
							DQE_TRANSFER_STARTED = 2,
							DQE_TRANSFER_FINISHED = 3,
							DQE_FIRST_TRANSFER_STARTED = 4,
							DQE_LAST_TRANSFER_FINISHED = 5;
	
	
	private static final Logger logger =  LoggerFactory.make();
	
	private final DCClient dcc;

	private final PreferenceChangedAdapter pfa;
	
	public DownloadQueue(DCClient dcclient) {
		this.dcc = dcclient;
		
		pfa = new PreferenceChangedAdapter(PI.get(),PI.tempDownloadDirectory){
			@Override
			public void preferenceChanged(String preference,String oldvalue,String newValue) {
				if (!tthRoots.isEmpty()) { //restart if the DownloadQueue is not empty..
					dcc.logEvent("restarting: " + PlatformUI.getWorkbench().restart());
				}
			}
		};
	}
	
	
	
	
	private final DownloadQueueFolder root =
		new DownloadQueueFolder(null, ""); //A tree structure for viewing all the DQEs.
	
	//the DQEs mapped to its hash or Userid in case of a FileList
	private Map<HashValue,AbstractDownloadQueueEntry> tthRoots 
		= Collections.synchronizedMap(new HashMap<HashValue,AbstractDownloadQueueEntry>());  
	
	private final CopyOnWriteArrayList<File> recommendedFolders = new CopyOnWriteArrayList<File>(); 
	
	
	/**
	 * loads the download queue from the disc ..
	 * or database
	 */
	public Future<?> loadDQ() {
		//move files if needed
		String oldDir = PI.get(PI.changedTempDownloadDirectory);
		if (GH.isNullOrEmpty(oldDir)) {
			oldDir = PI.get(PI.tempDownloadDirectory);
			PI.put(PI.changedTempDownloadDirectory,oldDir);
			logger.debug("written oldDir: "+oldDir);
		}
		logger.debug("current oldDir: "+oldDir);
		
		if (!PI.get(PI.tempDownloadDirectory).equals(oldDir)) {
			File old = new File(oldDir);
			File actual = PI.getTempDownloadDirectory();
			dcc.logEvent("Moving files to new Temp Download directory");
			move(old,actual);
			dcc.logEvent("Finished Moving files");
			//delete moving notification
			PI.put(PI.changedTempDownloadDirectory,PI.get(PI.tempDownloadDirectory));
		}
		
		
		//load persistence data from the db
		final Set<DQEDAO> dqes = getDatabase().loadDQEsAndUsers();
		synchronized(dqes) {
			return dcc.getSchedulerDir().submit(new Runnable() {
				public void run() {
					synchronized(dqes) {
						logger.debug("Start adding");
						try {
							for (DQEDAO dqedao : dqes) {
								FileDQE.restore(dqedao,DownloadQueue.this);
							}
						} catch(Exception e) {
							logger.warn(e,e);
						}
						logger.debug("end adding");
					}
				}
			});
		}
	}
	
	/**
	 * deletes all FileList files from the list..
	 * 
	 */
	public void stop() {
		pfa.dispose();
		List<AbstractDownloadQueueEntry> adqes;
		synchronized(tthRoots) {
			adqes = new ArrayList<AbstractDownloadQueueEntry>(tthRoots.values());
		}
		for (AbstractDownloadQueueEntry adqe: adqes) {
			if (TransferType.FILELIST.equals(adqe.getType())) {
				adqe.remove();
			}
		}
		
	}

	

	
	/**
	 * Adds a DownloadqueueEntry to the queue 
	 * 
	 * @param dqe  the item
	 * 
	 * may only be called by the DoenloadQueEntry..
	 */
	void addDownloadQueueEntry(AbstractDownloadQueueEntry dqe) {
		logger.debug("called addDownloadQueueEntry("+dqe+")");
		//test existence of the DQE then add
		
		AbstractDownloadQueueEntry existing = tthRoots.get(dqe.getID());
		
		
		if (existing == null) {
			// if it doesn't exist  add the dqe to "filesystem" and to the tth-dqe mapping
			tthRoots.put( dqe.getID() , dqe );
			synchronized(root) {
				adddqeToTree(dqe, dqe.getTargetPath().getPath() , root);
			}
			
			notifyObservers(new StatusObject(dqe,ChangeType.ADDED));
			
			logger.debug("dqe was dded to tree");
			
		} else {
			throw new IllegalArgumentException("dqe already existed in the queue");
		}
		
		
		
	}
	

	/**
	 * tests if the provided id is already present
	 * @param id - the TTH for a FileDQE or the userId for a FileList
	 * @return true if already present
	 */
	public boolean containsDQE(HashValue id) {
		return tthRoots.containsKey(id);
	}
	
	/**
	 * checks if exactly the provided dqe is in the Queue
	 * @param adqe
	 * @return true if its in the DownloadQueue
	 */
	public boolean containsDQE(AbstractDownloadQueueEntry adqe) {
		return adqe.equals(tthRoots.get(adqe.getID()));
	}

	
	/**
	 * makes a substring search in all DQE (case insensitive)
	 * 
	 * @param search - on search
	 * @return all DQEs that match search
	 */
	public List<AbstractDownloadQueueEntry> search(String search) {
		search = search.toLowerCase();
		List<AbstractDownloadQueueEntry> adqes = new ArrayList<AbstractDownloadQueueEntry>();
		for (AbstractDownloadQueueEntry adqe: tthRoots.values()) {
			if (adqe.getTargetPath().getPath().toLowerCase().contains(search)) {
				adqes.add(adqe);
			}
		}
		return adqes;
	}


	private void adddqeToTree(AbstractDownloadQueueEntry dqe , String path , DownloadQueueFolder current ){
		int i = path.indexOf(java.io.File.separatorChar,1);
		if (i == -1) {
			current.add(dqe);
		} else {

			DownloadQueueFolder next = (DownloadQueueFolder)current.getFolder(path.substring(0, i));
			if ( next == null) {
				next = new DownloadQueueFolder(current,path.substring(0, i) );
			}
			adddqeToTree(dqe, path.substring(i+1), next );
			
		}
	}
	
	
	/**
	 *  removes a file/DownloadqueueEntry by its tthRoot
	 *  may only be called by the dqe's remove() method!!!
	 * @param dqe the dqe that calls this method  and is removed..
	 */
	void removeFile(AbstractDownloadQueueEntry  dqe ){
		if (dqe == null) {
			throw new IllegalArgumentException("called with null argument");
		}
		tthRoots.remove(dqe.getID());
		synchronized(root) {
			removedqeFromTree(dqe,dqe.getTargetPath().getPath(),root);
		}
		notifyObservers(new StatusObject(dqe,ChangeType.REMOVED));
	}
	
	/**
	 * recursive method for removing the file from the treestructured Folder/filesystem...
	 *  called from above
	 *  
	 * @param dqe
	 * @param path
	 * @param current
	 */
	private void removedqeFromTree(AbstractDownloadQueueEntry dqe, String path, DownloadQueueFolder current ){
		int i = path.indexOf(java.io.File.separatorChar,1);
		if(i == -1){
			//first remove the dqe itself
			current.removeFromDQE(dqe.getFileName());

			//then remove all now empty parent folders
			DownloadQueueFolder above;
			while (current.getChildren().length==0) {
				above = current.getParent();
				if (above!=null) {
					above.removeFromFolder(current.getName());
					current = above;
				} else {
					break;
				}
			}
		} else {
			
			DownloadQueueFolder next = (DownloadQueueFolder)current.getFolder(path.substring(0, i));

			if( next == null) {
				return;
			} else {
				removedqeFromTree(dqe, path.substring(i+1), next );
			}
		}
		
	}
	
	/**
	 * matches a FileList..
	 * the User is added to each file he shares..
	 * @param f
	 */
	public void match(FileList f) {
		int count = 0 ;
		synchronized(tthRoots) {
			for (HashValue h: tthRoots.keySet()) {
				if (f.search(h) != null) {
					get(h).addUser(f.getUsr());
					count++;
				}
			}
		}
		dcc.logEvent(String.format(LanguageKeys.MatchedXFilesWithUserY,count,f.getUsr().getNick()));
	}
	
	/**
	 * Retrieves recommended Target paths
	 * specialized for the provided downloadable
	 * 
	 * @param downloadable a file for which we want a path recommended
	 * @return all paths where the file is recommended be downloaded to..
	 *  may be empty.. 
	 *  
	 *  
	 */
	public List<File> getPathRecommendation(IDownloadable downloadable) {
		List<File> found = new ArrayList<File>();
		
		if (downloadable.isFile()) {
			IDownloadableFile idf= (IDownloadableFile)downloadable;
			if (containsDQE(idf.getTTHRoot())) {
				found.add(get(idf.getTTHRoot()).getTargetPath() );
			}
		//	try {
				File f = dcc.getFilelist().getFile(idf.getTTHRoot());
				if (f != null) {
					found.add(f);
				}
		//	} catch(FilelistNotReadyException fnre) {
		//		logger.debug(fnre,fnre);
		//	} 
		//	for (File path : recommendedFolders) {
		//		found.add(new File(path,downloadable.getName()));
		//	}
		} //else {
		
			for (File path : recommendedFolders) {
				found.add(new File(path,downloadable.getName()));
			}
		//}
		
		return found;
	}
	
	public void addPathForRecommendation(File path) {
		recommendedFolders.addIfAbsent(path);
		if (recommendedFolders.size() > 5) {
			recommendedFolders.remove(0);
		}
	}




	public IDatabase getDatabase() {
		return dcc.getDatabase();
	}
	
	DCClient getDcc() {
		return dcc;
	}

	/**
	 * @return the root
	 */
	public DownloadQueueFolder getRoot() {
		return root;
	}

	public AbstractDownloadQueueEntry get(HashValue arg0) {
		return tthRoots.get(arg0);
	}
	
	/**
	 * calculates how much bytes are left for downloading in total...
	 */
	public long getTotalSize() {
		long totalsize = 0; 
		synchronized (tthRoots) {
			for (AbstractDownloadQueueEntry adqe: tthRoots.values()) {
				if (adqe instanceof AbstractFileDQE) {
					AbstractFileDQE afdqe = (AbstractFileDQE)adqe;
					totalsize += afdqe.getSize();
				}
			}
		}
		return totalsize;
	}
	
	/**
	 * @return the total number of files found..
	 */
	public int getTotalNrOfFiles() {
		synchronized(tthRoots) {
			return tthRoots.size();
		}
	}
	
	public List<AbstractFileDQE> getAllFileDQE() {
		List<AbstractFileDQE> files = new ArrayList<AbstractFileDQE>();
		synchronized(tthRoots) {
			for (AbstractDownloadQueueEntry adqe: tthRoots.values()) {
				if (adqe instanceof AbstractFileDQE) {
					files.add((AbstractFileDQE)adqe);
				}
			}
		}
		return files;
	}
	
	public Set<AbstractDownloadQueueEntry> getAllDQE() {
		return new HashSet<AbstractDownloadQueueEntry>(tthRoots.values());
	}
	
	/**
	 * 
	 * @return add DQE that have a transfer running..
	 */
	public Set<AbstractDownloadQueueEntry> getAllRunningDQE() {
		HashSet<AbstractDownloadQueueEntry> hs = new HashSet<AbstractDownloadQueueEntry>();
		synchronized(tthRoots) {
			for (AbstractDownloadQueueEntry adqe: tthRoots.values()) {
				if (adqe.getNrOfRunningDownloads() > 0) {
					hs.add(adqe);
				}
			}
		}
		return hs;
	}
	

	private static void move(File sourceFolder , File targetFolder) {
		if (sourceFolder.isDirectory()) {
			for (File f:sourceFolder.listFiles()) {
				if (f.isFile()) {
					logger.debug("sourcefile: "+f);
					try {
						AbstractDownloadQueueEntry.moveFile(f, new File(targetFolder,f.getName()));
					} catch(IOException ioe) {
						logger.warn(ioe, ioe);
					}
				}
			}
		}
	}
	
}
