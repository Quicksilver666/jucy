package uc.files;

import helpers.GH;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

import uc.IUser;
import uc.PI;
import uc.crypto.HashValue;
import uc.files.downloadqueue.AbstractDownloadQueueEntry;
import uc.files.downloadqueue.FileDQE;
import uc.files.downloadqueue.AbstractDownloadFinished;
import uc.files.filelist.FileListFolder;


/**
 * 
 * a class that should help with the implementation of IDownloadableFile 
 * and other methods that are useful
 * 
 * important contract
 * getPath() must return a path separated with File.separator 
 * 
 * As the name implies represents a file that can be downloaded.
 * i.e. SearchResult, FileList entry ...
 * 
 * @author Quicksilver
 * 
 */
public abstract class AbstractDownloadable implements IDownloadable , Comparable<AbstractDownloadable>{



	
	public IDownloadable getDownloadable() {
		return this;
	}


	public boolean isFile() {
		return false;
	}
	
	
	public abstract String getPath();


	public abstract IUser getUser();
	
	

	/**
	 * @return an Iterable for iterating over users 
	 * the Downloadable may have.. (usually only one..)
	 
	 */
	public Collection<IUser> getIterable() {
		return Collections.singleton(getUser());
	}

	public int nrOfUsers() {
		return 1;
	}

	/*
	 * (non-Javadoc)
	 * @see uc.files.IDownloadable#getOnlyPath()
	 */
	public String getOnlyPath() {
		String path= getPath();
		int i = path.lastIndexOf(File.separatorChar, path.length()-2);
		if (i == -1) {
			return "";
		} else {
			return path.substring(0,i);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see uc.files.IDownloadable#getName()
	 */
	public String getName() {
		String path = getPath();
		int i = path.lastIndexOf(File.separatorChar, path.length()-2);
		if (i == -1) {
			return path;
		} else {
			return path.substring(i+1);
		}
	}
	
	
	
	
	
	/*
	 * (non-Javadoc)
	 * @see uc.files.IDownloadable#download()
	 */
	public AbstractDownloadQueueEntry download() {
		String dir = PI.get(PI.downloadDirectory);
		File d = new File(dir);
		return download(new File(d,getName())); 
	}



	public int compareTo(AbstractDownloadable arg0) {
		int comp = Boolean.valueOf(isFile()).compareTo(arg0.isFile());
		if (comp == 0) {
			comp = getName().compareTo(arg0.getName());
		}
		return comp;
	}


	public abstract HashValue getTTHRoot() throws UnsupportedOperationException;


	



	public static abstract class AbstractDownloadableFile extends AbstractDownloadable implements IDownloadableFile {

	
		public AbstractDownloadQueueEntry download(File target) {
			return FileDQE.get(this,target);
		}
		
		/*
		 * (non-Javadoc)
		 * @see uc.files.AbstractDownloadable#isFile()
		 */
		@Override
		public boolean isFile() {
			return true;
		}

		/*
		 * (non-Javadoc)
		 * @see uc.files.IDownloadable.IDownloadableFile#getEnding()
		 */
		public String getEnding() {
			return GH.getFileEnding(getName());
		}
		
	}
	
	public static abstract class AbstractDownloadableFolder extends AbstractDownloadable implements IDownloadableFolder {
		
		/**
		 * does the same as the implementation for file.. though will always return 
		 * null
		 */
		public AbstractDownloadQueueEntry download(final File target) {
			for (IUser usr: getIterable()) {
			//User usr = getUser();
				if (usr.hasDownloadedFilelist()) {
					FileListFolder folder = usr.getFilelistDescriptor().getFilelist().getRoot().getByPath(getPath());
					if (folder != null) {
						folder.download(target);
					}
					
				} else {
					usr.downloadFilelist().addDoAfterDownload(new AbstractDownloadFinished() {
						@Override
						public boolean equals(Object obj) {
							return obj != null && getClass().equals(obj.getClass());
						}
	
						@Override
						public int hashCode() {
							return getClass().hashCode(); 
						}
	
					
						public void finishedDownload(File f) {
							download(target);
						}
						
					});
				}
			}
			return null;
		}
		
	}
	
	
	

}
