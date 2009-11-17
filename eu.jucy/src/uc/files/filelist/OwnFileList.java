package uc.files.filelist;




import helpers.GH;
import helpers.ISearchMap;
import helpers.PreferenceChangedAdapter;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;



import logger.LoggerFactory;



import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;


import eu.jucy.language.LanguageKeys;





import uc.DCClient;
import uc.InfoChange;
import uc.PI;
import uc.User;
import uc.FavFolders.SharedDir;
import uc.crypto.HashValue;
import uc.crypto.InterleaveHashes;
import uc.crypto.IHashEngine.IHashedFileListener;
import uc.files.filelist.FileListMapping.FileFilter;
import uihelpers.IconManager;


/**
 * 
 * special FileList with facility for searching
 * @author Quicksilver
 *
 */
public class OwnFileList implements IOwnFileList  {
	
	private static final Logger logger = LoggerFactory.make();
	

	private final DCClient dcc;
	private volatile ISearchMap<IFileListItem> filelistmap;
	
	private TextIndexer pdfIndex;

	/**
	 * mapping from the top Folders in the FileList
	 * to the real directories on the hdd.
	 */
	private volatile Map<FileListFolder,SharedDir> reverselevelone = new HashMap<FileListFolder,SharedDir> ();
	
	/**
	 * maps shared dirs to whether they were online the last time...
	 * -> dirs that suddenly become unavailable must be cleared from the FileFist
	 */
	private Map<SharedDir,Boolean> lastOnlineDirs = new HashMap<SharedDir,Boolean>();
	
	/**
	 * our own FileList
	 */
	private volatile FileList fileList;
	



	/**
	 * if set to true the FileList is being refreshed -> no new refresh is started..
	 */
	private volatile boolean refreshing = false;
	
	/**
	 * variable to store if the FileList in ram is the same as the one
	 * on the HDD
	 */
	private volatile boolean defersFromFilelistOnDisc = false;
	

	
	public OwnFileList(User self,DCClient dcclient) {
		dcc = dcclient;
		fileList = new FileList(self);
		
		try {
			pdfIndex = new TextIndexer();
		} catch(IOException e) {
			logger.warn(e,e);
		}
		
		new PreferenceChangedAdapter(PI.get(),PI.sharedDirs2) {
			
			@Override
			public void preferenceChanged(String preference, String oldValue,String newValue) {
				dcc.getSchedulerDir().schedule(new Runnable() {
					public void run() {
						refresh(false);
					}
				} , 500, TimeUnit.MILLISECONDS);	
				
			}
		};
	}
	
	
	
	
	/**
	 * side effect: lastOnlineDirs state variable needed by this method
	 * is updated..
	 * 
	 * @return true if the SharedDirs changed
	 */
	private boolean checkSharedDirs() {
		Map<SharedDir,Boolean> dirs = new HashMap<SharedDir,Boolean>();
		for (SharedDir dir: reverselevelone.values()) {
			dirs.put(dir, dir.getDirectory().isDirectory());
		}
		boolean ret = !dirs.equals(lastOnlineDirs);
		lastOnlineDirs = dirs;
		return ret;
	}
	
	/**
	 * simply loads the dirs that are to be shared..
	 *
	 */
	private  void loadSharedDirs(Map<SharedDir,FileListFolder> levelone,
			Map<FileListFolder,SharedDir> reverselevelone,FileList fileList) {
		
		List<SharedDir> loadeddirs = dcc.getFavFolders().getSharedDirs();
		
		//add new dirs
		for (SharedDir dir: loadeddirs ) {
			FileListFolder f = 
				new FileListFolder(fileList, fileList.getRoot(), dir.getName());
			levelone.put(dir, f);
			reverselevelone.put(f,dir);
		}
	}

	/**
	 * used in conjunction with iCon manager..
	 * shared folders are used to get Icons for
	 * File endings
	 */
	public static void loadSharedDirsForIconManager(final DCClient dcc) {
		DCClient.execute(new Runnable() {
			public void run() {
				List<SharedDir> loadeddirs = dcc.getFavFolders().getSharedDirs();
				for (SharedDir sd: loadeddirs) {
					if (sd.getDirectory().isDirectory()) {
						IconManager.loadImageSources(sd.getDirectory());
					}
				}
			}
		});
	}
	
	/**
	 * called on creating for faster 
	 */
	public void initialise() {
		refresh(true);
		//updates the FileList every 60 Minutes or every 5 Minutes if hashing..
		dcc.getSchedulerDir().scheduleAtFixedRate(new Runnable(){
			int counter = 0;
			public void run() {
				if (++counter * 5 >= PI.getInt(PI.filelistRefreshInterval)  || 
						defersFromFilelistOnDisc || checkSharedDirs()) {
					refresh(false);
					counter = 0;
				}
			}
		}, 5 * 60 , 5 * 60 , TimeUnit.SECONDS);
		
		dcc.notifyChangedInfo(InfoChange.Sharesize);
		
		pdfIndex.init(this);
		
	}
	
	/**
	 * 
	 * @param wait - if the method should block until the refresh is done
	 */
	public void refresh(boolean wait) {
		RefreshJob rj = new RefreshJob();
		rj.schedule();
		if (wait) {
			try {
				rj.join();
			}catch(InterruptedException is) {}
		}
	}
	
	/**
	 * refreshes the FileList  
	 * goes recursively through all shared dirs..
	 * and checks if hashes are up to date... or if the file needs to be hashed
	 */
	private void refresh(IProgressMonitor monitor) {
		
		if (!refreshing) { //check for refreshing.. only refresh if no other thread is refreshing
			refreshing = true; //flag that we are refreshing..
			
			logger.debug(LanguageKeys.StartedRefreshingTheFilelist);
			//logger.info("started Refreshing")
			
			try {
				FileList newFilelist  = new FileList(fileList.getUsr());
				
				dcc.getHashEngine().clearFileJobs(); //if for example less files are shared this will help that no useless files are hashed
				//the folder lookup and reverse folders
				
				Map<SharedDir,FileListFolder> levelone = new HashMap<SharedDir,FileListFolder>();
				Map<FileListFolder,SharedDir> reverselevelone = new HashMap<FileListFolder,SharedDir>();
				
				loadSharedDirs(levelone,reverselevelone,newFilelist);
				
				monitor.beginTask(LanguageKeys.StartedRefreshingTheFilelist,levelone.size() +1+2+1);
			
				//monitor.subTask("checking folders for changed Files");
				
				buildFilelist(newFilelist,levelone,monitor);
				
				if (monitor.isCanceled()) {
					return;
				}
				
			//	monitor.subTask("building TTH-Map");
			
				newFilelist.calcSharesizeAndBuildTTHMap();
				monitor.worked(1);
				
				
			//	monitor.subTask("indexing Filelist");
			
				//start	indexing the FileList
				ISearchMap<IFileListItem> filelistmap = new InvertedIndex<IFileListItem>(new FileListMapping());
			
				indexFilelist(filelistmap,newFilelist);
				monitor.worked(2);
				
				//end indexing the FileList...
				if (monitor.isCanceled()) {
					return;
				}
				//monitor.subTask("finalize work / replacing old Filelist");
				try {
		
					this.filelistmap 	= filelistmap;
					replaceFilelist(newFilelist,reverselevelone);
				} catch (IOException ioe) {
					logger.error(ioe,ioe); //if this fails we can't upload the new list.. therefore error level
				} 
				monitor.worked(1);
				
				logger.info( LanguageKeys.FinishedFilelistRefresh);
				
			//flag that refreshing is done..
			} finally {
				refreshing = false;
			}
			
			checkSharedDirs(); //finally updating the SharedDirs..
			
		} else {
			logger.info( LanguageKeys.FilelistRefreshAlreadyInProgress);
		}
	}
	
	private void replaceFilelist(FileList currentFilelist,
			Map<FileListFolder,SharedDir> reverselevelone) throws IOException {
			
		logger.debug("written filelist");

		
		User self = fileList.getUsr();
		
		self.setFilelistDescriptor(new FileListDescriptor(fileList.getUsr(),currentFilelist));
		self.setShared(currentFilelist.getSharesize());
		
		
		this.reverselevelone	= reverselevelone;
		FileList oldFilelist 	= fileList; 
		fileList 				= currentFilelist;
		
		if (oldFilelist.getSharesize() != fileList.getSharesize()) {
			dcc.notifyChangedInfo(InfoChange.Sharesize);
		}
		defersFromFilelistOnDisc = false; // now its the same again in ram as on disc..
	}
	

	
	/**
	 * creates an index for searching the FileList.. 
	 * @param tthsearches - mapping of HashValues to FileList files
	 * @param filelistmap - mapping for substring search of FilelList files..
	 * @param toIndex - the FileList that needs indexing..
	 */
	private static void indexFilelist(
			ISearchMap<IFileListItem> filelistmap, FileList toIndex) {
		
		for (IFileListItem item : toIndex) {
			filelistmap.put(item);//file.getPath()	 , file);
		}
	}
	
	private void buildFilelist(FileList newFilelist,Map<SharedDir,FileListFolder> levelone,IProgressMonitor monitor) {
		Map<File,HashedFile> hashedFiles = dcc.getDatabase().getAllHashedFiles();
		logger.debug("in refreshing own filelist: found "+hashedFiles.size()+" hashed files");
		
		Pattern exclude = Pattern.compile(PI.get(PI.excludedFiles));
		Pattern include = Pattern.compile(PI.get(PI.includeFiles));
		
		//recursively go through all shared dirs..
		for (Entry<SharedDir,FileListFolder> entry : levelone.entrySet()) {
			SharedDir dir = entry.getKey();
			if (dir.getDirectory().isDirectory()) {
	
				rekBuildFilelist(dir.getDirectory()
					, entry.getValue()
					, newFilelist
					, PI.getBoolean(PI.shareHiddenFiles)
					, hashedFiles,exclude,include);
		
		
				dir.setLastShared( entry.getValue().getContainedSize());
			}
			monitor.worked(1);
			if (monitor.isCanceled()) {
				break;
			}
		}
	}
	
	/**
	 * goes recursively through the file system and checks if a file needs to be hashed..
	 * 
	 * @param folder - a folder on the HDD
	 * @param listfolder - the corresponding FilelistFolder
	 */
	private void rekBuildFilelist(File file, FileListFolder listfolder, FileList current, boolean shareHidden, 
			Map<File,HashedFile> hashedFiles, Pattern exclude,Pattern include) {
		
		logger.debug("refreshing folder: "+file);

		
		for (File child : GH.getFiles(file, !shareHidden) ) {
			if (child.isDirectory()) {
				FileListFolder subfolder = new FileListFolder(current, listfolder , child.getName());
				rekBuildFilelist(	child, subfolder,current,shareHidden,hashedFiles ,exclude, include);
			} else if (child.isFile() ) {
				Matcher inc = include.matcher(child.getName());
				if (inc.find()) {
					Matcher exc = exclude.matcher(child.getName());
					if (!exc.find()) {
						rekAddFile(child,listfolder, hashedFiles);
					} 
				}
			}
		}
	}
	
	
	private void rekAddFile(final File file, final FileListFolder parent, Map<File,HashedFile> hashedFiles) {

		HashedFile hashedFile =  hashedFiles.get(file);
			
		if (hashedFile == null || hashedFile.getLastChanged().getTime() != file.lastModified()) {
			logger.debug("found file needs hashing: "+file);
			dcc.getHashEngine().hashFile(file, new IHashedFileListener(){
				public void hashedFile(File f, HashValue root, InterleaveHashes ilh,Date before) {
					dcc.getDatabase().addOrUpdateFile(f,root, ilh, before);
					new FileListFile(parent, f.getName(), f.length(), root);
					defersFromFilelistOnDisc = true; 
				}
			});
				
		} else {
		//	logger.debug("adding file to filelist:"+file);
			//we have a current hash.. so we can create the file with it
			final HashValue tthRoot = hashedFile.getTTHRoot();
			
			if (tthRoot != null) {
				new FileListFile(parent, file.getName(), file.length(), tthRoot);
				

				
				//logger.debug("added File to list:"+f);
			}
		}
	}
	

	
	
	
	/* (non-Javadoc)
	 * @see uc.files.filelist.IOwnFileList#search(java.util.Set, java.util.Set, long, long, long, java.util.Collection, int, boolean)
	 */
	public Set<IFileListItem> search(Set<String> keys,Set<String> excludes, long minsize, long maxsize,long equalsize,Collection<String> fileendings , int maxResults, boolean onlyFolder) {
	/*	if (logger.isDebugEnabled()) {
			String s = "";
			for (String part:keys) {
				s += part+",";
			}
			//logger.debug("Search("+s+","+sizerestricted+","+maxsize+","+size+","+""+","+maxResults);
		} */
		

		//search for everything
		if (filelistmap == null) {
			return Collections.<IFileListItem>emptySet();
		}
		Set<IFileListItem> res = filelistmap.search(keys,excludes, new FileFilter(minsize,maxsize,equalsize,fileendings,onlyFolder));

		logger.debug("Found nr: "+res);
			
		//cut the set down to maxsize if needed
		if (res.size() > maxResults) {
			Iterator<IFileListItem> it = res.iterator();
			while (it.hasNext() && res.size() > maxResults) {
				it.next();
				it.remove();
			}
		} 
		
		if (keys.size() == 1 ) {
			String searched = keys.toArray(new String[1])[0];
			if (HashValue.isHash(searched) ) {
				FileListFile found = search(HashValue.createHash(searched));
				if (found != null) {
					res.add(found);
				}
			}
		}
		
		if (pdfIndex.isCreated()&& res.isEmpty() && TextIndexer.matchesSomeEnding(fileendings)) {
			if (Platform.inDevelopmentMode()) {
				logger.info("searching for: "+ GH.toString(keys.toArray()));
			}
			Set<HashValue> found = pdfIndex.search(keys, excludes,fileendings);
			res  = new HashSet<IFileListItem>(); //res might not support adding... empty..
			for (HashValue h : found) {
				FileListFile found2 = search(h);
				
				if (found2 != null && minsize <= found2.getSize() && found2.getSize() <= maxsize) { 
					if (Platform.inDevelopmentMode()) {
						logger.info("found pdf file: "+found2);
					}
					res.add(found2);
				}
			}
		}

		return res;
	}
	
	/* (non-Javadoc)
	 * @see uc.files.filelist.IOwnFileList#search(uc.crypto.HashValue)
	 */
	public FileListFile search(HashValue tth) {
		return fileList.search(tth);
	}
	
	/* (non-Javadoc)
	 * @see uc.files.filelist.IOwnFileList#getFile(uc.crypto.HashValue)
	 */
	public File getFile(HashValue tth) throws FilelistNotReadyException {
		if (reverselevelone.isEmpty()) {
			throw new FilelistNotReadyException();
		}
		FileListFile f = search(tth);
		if (f != null) {
			return getFile(f);
		} else {
			return null;
		}
	}
	
	/* (non-Javadoc)
	 * @see uc.files.filelist.IOwnFileList#getFile(uc.files.filelist.FileListFile)
	 */
	public File getFile(FileListFile file) throws FilelistNotReadyException{
		if (file == null) {
			throw new IllegalArgumentException("file may not be null");
		}
		//determine level one folder
		FileListFolder cur = file.getParent(); 
		
		while (cur != null && cur.getParent() != fileList.getRoot() ){
			cur = cur.getParent();
		}
		if (cur == null) {
			throw new FilelistNotReadyException();
		}
		
		SharedDir sd = reverselevelone.get(cur);
		
		String realpath=file.getPath();
		String pathRelativeToLevelone = realpath.substring(realpath.indexOf(File.separatorChar)+1);
		
		return new File(sd.getDirectory(), pathRelativeToLevelone );
	}
	
	/* (non-Javadoc)
	 * @see uc.files.filelist.IOwnFileList#getSharesize()
	 */
	public long getSharesize() {
		return fileList.getSharesize();
	}
	

	/* (non-Javadoc)
	 * @see uc.files.filelist.IOwnFileList#getNumberOfFiles()
	 */
	public int getNumberOfFiles() {
		return fileList.getNumberOfFiles();
	}


	/**
	 * indicates that the FileList can currently not be used..
	 * used for a faster startup..
	 * 
	 * @author Quicksilver
	 */
	public static class FilelistNotReadyException extends Exception {
		private static final long serialVersionUID = 1L;
	}
	
	
	class RefreshJob extends Job {

		public RefreshJob() {
			super("Refreshing Filelist"); //TODO translatable
			setPriority(Job.LONG);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				refresh(monitor);
			} finally {
				monitor.done();
			}
			return Status.OK_STATUS;
		}
	}
	

	
	
	/*public static class PartialFileList extends OwnFileList {
		private final List<SharedDir> dirs;
		private final OwnFileList parent;
		
		public PartialFileList(List<SharedDir> dirs, OwnFileList parent) {
			this.dirs    = dirs;
			this.parent  = parent;
			
		}

		@Override
		public File getFile(FileListFile file) throws FilelistNotReadyException {
			// TODO Auto-generated method stub
			return super.getFile(file);
		}

		@Override
		public File getFile(HashValue tth) throws FilelistNotReadyException {
			// TODO Auto-generated method stub
			return super.getFile(tth);
		}

		@Override
		public int getNumberOfFiles() {
			// TODO Auto-generated method stub
			return super.getNumberOfFiles();
		}

		@Override
		public long getSharesize() {
			// TODO Auto-generated method stub
			return super.getSharesize();
		}

		@Override
		public void initialise() {
			// TODO Auto-generated method stub
			super.initialise();
		}

		@Override
		public void refresh(boolean wait) {
			// TODO Auto-generated method stub
			super.refresh(wait);
		}

		@Override
		public FileListFile search(HashValue tth) {
			// TODO Auto-generated method stub
			return super.search(tth);
		}

		@Override
		public Set<IFileListItem> search(Set<String> keys,
				Set<String> excludes, long minsize, long maxsize,
				long equalsize, Collection<String> fileendings, int maxResults,
				boolean onlyFolder) {
			// TODO Auto-generated method stub
			return super.search(keys, excludes, minsize, maxsize, equalsize, fileendings,
					maxResults, onlyFolder);
		}
		
		
		
		
	} */
	
	public FileList getFileList() {
		return fileList;
	}


	
	public void stop() {
		if (pdfIndex != null) {
			pdfIndex.stop();
		}
	}
	
}



