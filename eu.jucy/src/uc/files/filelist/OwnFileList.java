package uc.files.filelist;




import helpers.GH;
import helpers.ISearchMap;
import helpers.PreferenceChangedAdapter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
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
import uc.FavFolders;
import uc.FavHub;
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
	
	private final TextIndexer pdfIndex;


	
	private final List<TopFolder> topFolders =   new CopyOnWriteArrayList<TopFolder>(); 
	
	/**
	 * maps shared dirs to whether they were online the last time...
	 * -> dirs that suddenly become unavailable must be cleared from the FileFist
	 */
	private Map<SharedDir,Boolean> lastOnlineDirs = new HashMap<SharedDir,Boolean>();
	
	private final User filelistSelf;
	/**
	 * our own FileList
	 */
	private volatile FileList fileList;
	



	/**
	 * if set to true the FileList is being refreshed -> no new refresh is started..
	 */
	private final Semaphore refresh = new Semaphore(1,false);
	
	/**
	 * variable to store if the FileList in ram is the same as the one
	 * on the HDD
	 */
	private volatile boolean defersFromFilelistOnDisc = false;
	

	
	public OwnFileList(User self,DCClient dcclient) {
		dcc = dcclient;
		this.filelistSelf = self;
		fileList = new FileList(self);
		
		TextIndexer pdfI = null;
		try {
			pdfI = new TextIndexer();
		} catch(IOException e) {
			logger.warn(e,e);
		}
		pdfIndex = pdfI;
		
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
	 * @return true if the SharedDirs changed since last call of this method
	 */
	private boolean checkSharedDirs() {
		Map<SharedDir,Boolean> dirs = new HashMap<SharedDir,Boolean>();
		for (TopFolder dir: topFolders) {
			dirs.put(dir.getSharedDir(), dir.isOnline());
		}
		boolean ret = !dirs.equals(lastOnlineDirs);
		lastOnlineDirs = dirs;
		return ret;
	}
	
	/**
	 * simply loads the dirs that are to be shared..
	 *
	 */
	private void loadSharedDirs(List<TopFolder> topLevelFolders,FileList fileList) {
		
		List<SharedDir> loadeddirs = dcc.getFavFolders().getSharedDirs();
		
		//add new dirs
		for (SharedDir dir: loadeddirs ) {
			TopFolder f = 
				new TopFolder(fileList, fileList.getRoot(), dir);
			topLevelFolders.add(f);
		}
	}

	/**
	 * used in conjunction with icon manager..
	 * shared folders are used to get Icons for
	 * File endings
	 */
	public static void loadSharedDirsForIconManager(final FavFolders favs) {
		DCClient.execute(new Runnable() {
			public void run() {
				List<SharedDir> loadeddirs = favs.getSharedDirs();
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
		
		if (PI.getBoolean(PI.fullTextSearch)) {
			pdfIndex.init(this);
		}
		
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
			} catch(InterruptedException is) {}
		}
	}
	
	/**
	 * refreshes the FileList  
	 * goes recursively through all shared dirs..
	 * and checks if hashes are up to date... or if the file needs to be hashed
	 */
	private void refresh(IProgressMonitor monitor) {
		
		if (refresh.tryAcquire()) {
			logger.debug(LanguageKeys.StartedRefreshingTheFilelist);
			try {
				FileList newFilelist  = new FileList(filelistSelf);
				newFilelist.setGenerator(DCClient.LONGVERSION);
				newFilelist.setCID(dcc.getPID().hashOfHash().toString());
				
				dcc.getHashEngine().clearFileJobs(); //if for example less files are shared this will help that no useless files are hashed
				//the folder lookup and reverse folders
				
				
				List<TopFolder> topLevelFolders = new ArrayList<TopFolder>();
				
				loadSharedDirs(topLevelFolders,newFilelist);
				
				monitor.beginTask(LanguageKeys.StartedRefreshingTheFilelist,topLevelFolders.size() +1+2+1);
				
				buildFilelist(newFilelist,topLevelFolders,monitor);
				
				if (monitor.isCanceled()) {
					return;
				}
				newFilelist.calcSharesizeAndBuildTTHMap();
				monitor.worked(1);
				
			
				//start	indexing the FileList
				ISearchMap<IFileListItem> filelistmap = new InvertedIndex<IFileListItem>(new FileListMapping());
			
				indexFilelist(filelistmap,newFilelist);
				monitor.worked(2);
				
				
				if (monitor.isCanceled()) {
					return;
				}
				
				try {
					this.filelistmap =	filelistmap;
					replaceFilelist(newFilelist,topLevelFolders);
				} catch (IOException ioe) {
					logger.error(ioe,ioe); 
				} 
				monitor.worked(1);
				
				logger.info( LanguageKeys.FinishedFilelistRefresh);
				
			//flag that refreshing is done..
			} finally {
				refresh.release();
			}
			
			checkSharedDirs(); //finally updating the SharedDirs..
			
		} else {
			logger.info( LanguageKeys.FilelistRefreshAlreadyInProgress);
		}
	}
	
	private void replaceFilelist(FileList currentFilelist,
			List<TopFolder> topLevelFolders) throws IOException {
			
		logger.debug("written filelist");

		
		User self = fileList.getUsr();
		
		self.setFilelistDescriptor(new FileListDescriptor(fileList.getUsr(),currentFilelist));
		self.setShared(currentFilelist.getSharesize());
		
		this.topFolders.clear();
		this.topFolders.addAll(topLevelFolders);
		//this.topLevelFolders	= reverselevelone;
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
	
	private void buildFilelist(FileList newFilelist,List<TopFolder> topLevelFolders,IProgressMonitor monitor) {
		Map<File,HashedFile> hashedFiles = dcc.getDatabase().getAllHashedFiles();
		logger.debug("in refreshing own filelist: found "+hashedFiles.size()+" hashed files");
		
		Pattern exclude = Pattern.compile(PI.get(PI.excludedFiles));
		Pattern include = Pattern.compile(PI.get(PI.includeFiles));
		
		//recursively go through all shared dirs..
		for (TopFolder folder : topLevelFolders) {
			SharedDir dir = folder.getSharedDir();
			if (dir.getDirectory().isDirectory()) {
	
				rekBuildFilelist(dir.getDirectory()
					, folder
					, newFilelist
					, PI.getBoolean(PI.shareHiddenFiles)
					, hashedFiles,exclude,include);
		
		
				dir.setLastShared( folder.getContainedSize());
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
			dcc.getHashEngine().hashFile(file, new IHashedFileListener() {
				public void hashedFile(File f, HashValue root, InterleaveHashes ilh,Date before) {
					dcc.getDatabase().addOrUpdateFile(f,root, ilh, before);
					new FileListFile(parent, f.getName(), f.length(), root);
					defersFromFilelistOnDisc = true; 
				}
			});	
		} else {
			//logger.debug("adding file to FileList:"+file);
			//we have a current hash.. so we can create the file with it
			HashValue tthRoot = hashedFile.getTTHRoot();
			if (tthRoot != null) {
				new FileListFile(parent, file.getName(), file.length(), tthRoot);
			} else if (Platform.inDevelopmentMode()) {
				logger.warn("tth is null "+file.getPath());
			}
		}
	}
	

	public static class SearchParameter {
		public Set<String> keys;
		public Set<String> excludes;
		public long minsize,maxsize, equalsize;
		public Collection<String> fileendings;
		public int maxResults;
		public boolean onlyFolder;
		
		/**
		 * restricts the search to directories available in the given hub
		 */
		public FavHub hub;
		
		
		public SearchParameter(Set<String> keys, Set<String> excludes,
				long minsize, long maxsize, long equalsize,
				Collection<String> fileendings,
				boolean onlyFolder) {
			super();
			this.keys = keys;
			this.excludes = excludes;
			this.minsize = minsize;
			this.maxsize = maxsize;
			this.equalsize = equalsize;
			this.fileendings = fileendings;
			this.onlyFolder = onlyFolder;
		}
		
		
	}
	
	
	/* (non-Javadoc)
	 * @see uc.files.filelist.IOwnFileList#search(java.util.Set, java.util.Set, long, long, long, java.util.Collection, int, boolean)
	 */
	public Set<IFileListItem> search(SearchParameter sp) {
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
		FileFilter fileFilter = new FileFilter(sp.minsize,sp.maxsize,
				sp.equalsize,sp.fileendings,sp.onlyFolder);
		
		Set<IFileListItem> res = filelistmap.search(sp.keys,sp.excludes,fileFilter);

		logger.debug("Found nr: "+res);
			

		
		if (sp.keys.size() == 1 ) {
			String searched = GH.getRandomElement(sp.keys); 
			if (HashValue.isHash(searched) ) {
				FileListFile found = search(HashValue.createHash(searched));
				if (found != null) {
					res.add(found);
				}
			}
		}
		
		if (pdfIndex.isCreated() && res.isEmpty() && TextIndexer.matchesSomeEnding(sp.fileendings)) {
			if (Platform.inDevelopmentMode()) {
				logger.info("searching for: "+ GH.toString(sp.keys.toArray()));
			}
			Set<HashValue> found = pdfIndex.search(sp.keys, sp.excludes,sp.fileendings);
			res = new HashSet<IFileListItem>(); //res might not support adding... empty..
			for (HashValue h : found) {
				FileListFile found2 = search(h);
				
				if (found2 != null && fileFilter.filter(found2)) { 
					if (Platform.inDevelopmentMode()) {
						logger.info("found pdf file: "+found2);
					}
					res.add(found2);
				}
			}
		}
		
		//cut the set down to max size if needed
		if (res.size() > sp.maxResults) {
			Iterator<IFileListItem> it = res.iterator();
			while (it.hasNext() && res.size() > sp.maxResults) {
				it.next();
				it.remove();
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
	public File getFile(HashValue tth)  {
		if (topFolders.isEmpty()) {
			throw new IllegalStateException("Filelist not ready");
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
	public File getFile(FileListFile file)  {
		if (file == null) {
			throw new IllegalArgumentException("file may not be null");
		}
		//determine level one folder
		FileListFolder cur = file.getParent(); 
		
		while (cur != null && !(cur instanceof TopFolder)) {
			cur = cur.getParent();
		}
		
		if (cur == null) {
			throw new IllegalStateException("Filelist not ready");
		}
		
		return ((TopFolder)cur).getRealPath(file);

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
			//before and after refresh GC is run as lots of long lived objects get free 
			//that won't be garbage collected for quite a long time otherwise
			//(before to make space for refresh and afterwards to get rid of long lived objects)
			System.gc(); 
			try {
				refresh(monitor);
			} finally {
				monitor.done();
			}
			System.gc(); 
			return Status.OK_STATUS;
		}
	}

	
	public FileList getFileList() {
		return fileList;
	}


	
	public void stop() {
		if (pdfIndex != null) {
			pdfIndex.stop();
		}
	}
	
	public static class TopFolder extends FileListFolder {

		private final SharedDir sharedDir;

		public TopFolder(FileList f, FileListFolder root,SharedDir sd) {
			super(f, root, sd.getName());
			this.sharedDir = sd;
		}
		
		public SharedDir getSharedDir() {
			return sharedDir;
		}
		
		public boolean isOnline() {
			return sharedDir.getDirectory().isDirectory();
		}
		
		public File getRealPath(FileListFile decendant) {
			String realpath = decendant.getPath();
			String pathRelativeToLevelone = realpath.substring(realpath.indexOf(File.separatorChar)+1);
			
			return new File(sharedDir.getDirectory(), pathRelativeToLevelone );
		}
		
	}
	
}



