package uc.files.search;



import helpers.StatusObject.ChangeType;

import org.eclipse.core.runtime.Assert;

import uc.IUser;
import uc.User;
import uc.crypto.HashValue;
import uc.files.AbstractDownloadable.AbstractDownloadableFile;
import uc.files.IDownloadable.IDownloadableFile;
import uc.files.filelist.FileListFile;
import uc.files.filelist.FileListFolder;



/**
 * a representative type for Search results.. is created from search info on UDP and the user
 * 
 * important  path must use java.io.separator for separation...
 * @author Quicksilver
 *
 */
public class SearchResult extends AbstractDownloadableFile implements ISearchResult,IDownloadableFile  {
	
	private final String path;
	private final HashValue tthRoot;
	private final IUser usr; //the one that has sent/sends the sr
	private final long size;
	private final int availabelSlots;
	private final int totalSlots;
	private final String token;
	
	public String getToken() {
		return token;
	}



	/**
	 * determines if the SR is a folder or a file
	 * true means file
	 */
	private final boolean file; 
	
	
	/**
	 * Search result for a received command..
	 * 
	 * @param path - a path with filename separated by java.io.file.separator 
	 * @param tthRoot - the roothash of the file   null if its a directory..
	 * @param usr - the user that sent / sends the searchresult   (the other if received and ourselfes if we send)
	 * @param size - the reported size of the file
	 * @param availabelslots - how many slots the user reports to have open
	 * @param totalslots - how many slots the user has in total
	 * @param boolean file  true if a file false  if a 
	 */
	public static ISearchResult create(String path,HashValue tthRoot, IUser usr,long size,int availabelslots,int totalslots, boolean file,String token) {
		SearchResult sr =  new SearchResult(path,tthRoot, usr,size,availabelslots,totalslots, file, token);
		if (file) {
			return sr;
		} else {
			return new FolderSearchResult(sr);
		}
	}
	
	/**
	 * Search result for a received command..
	 * 
	 * @param path - a path with filename separated by java.io.file.separator 
	 * @param tthRoot - the roothash of the file   null if its a directory..
	 * @param usr - the user that sent / sends the searchresult   (the other if received and ourselfes if we send)
	 * @param size - the reported size of the file
	 * @param availabelslots - how many slots the user reports to have open
	 * @param totalslots - how many slots the user has in total
	 * @param boolean file  true if a file false  if a 
	 */
	private SearchResult(String path,HashValue tthRoot, IUser usr,long size,int availabelslots,int totalslots, boolean file,String token) {
		this.path		=	path;
		this.tthRoot	=	tthRoot;
		this.usr		=	usr;
		this.size		=	size;
		this.availabelSlots=availabelslots;
		this.totalSlots	=	totalslots;
		this.file		= file;
		this.token 		= token;
		Assert.isTrue(file? tthRoot != null : true );
	}
	
	
	
	/**
	 * constructor for outgoing search results..
	 * creates a searchresult for creation as
	 * @param file
	 * @param self
	 * @param availabelslots
	 * @param totalslots
	 */
	public SearchResult(FileListFile file,User self,int availabelslots,int totalslots,String token) {
		this(file.getPath(),file.getTTHRoot(),self,file.getSize(),availabelslots, totalslots,true, token);
	}
	
	
	/**
	 * constructor for outgoing search results..
	 * 
	 * @param folder
	 * @param self
	 * @param availabelslots
	 * @param totalslots
	 */
	public SearchResult(FileListFolder folder,User self,int availabelslots,int totalslots,String token) {
		this(folder.getPath(),null,self, folder.getContainedSize(),availabelslots,totalslots,false,token);
	}
	
	
	/**
	 * @return the availabelSlots
	 */
	public int getAvailabelSlots() {
		return availabelSlots;
	}

	
	/**
	 * @return the path
	 */
	public String getPath() {
		return path;
	}

	
	/**
	 * @return the size
	 */
	public long getSize() {
		return size;
	}

	
	/**
	 * @return the totalSlots
	 */
	public int getTotalSlots() {
		return totalSlots;
	}

	
	/**
	 * @return the tthRoot
	 */
	public HashValue getTTHRoot() {
		return tthRoot;
	}

	
	/**
	 * @return the usr
	 */
	public IUser getUser() {
		return usr;
	}
	
	public static class FolderSearchResult extends AbstractDownloadableFolder implements ISearchResult, IDownloadableFolder {

		private final SearchResult sr;
		
		public FolderSearchResult(SearchResult sr) {
			this.sr = sr;
		}
		
		@Override
		public String getPath() {
			return sr.getPath();
		}

		@Override
		public HashValue getTTHRoot() throws UnsupportedOperationException {
			return sr.getTTHRoot();
		}

		@Override
		public IUser getUser() {
			return sr.getUser();
		}

		public int getAvailabelSlots() {
			return sr.getAvailabelSlots();
		}


		public int getTotalSlots() {
			return sr.getTotalSlots();
		}

		public String getToken() {
			return sr.getToken();
		}
		
		
		
	}

	
	public static interface ISearchResultListener {
		void received(ISearchResult sr);
	}
	
	public static interface IExtSearchResultListener {
		void received(ISearchResult sr,Object parent,ChangeType ct);
	}



	public boolean isFile() {
		return file;
	}
	
}
