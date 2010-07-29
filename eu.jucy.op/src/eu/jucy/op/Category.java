package eu.jucy.op;

import java.util.HashMap;
import java.util.Map;

import uc.crypto.HashValue;
import uc.files.IDownloadable.IDownloadableFile;


/**
 * bad -> should be two lists..
 * name one blacklist..
 * name one whitelist.. -> IDEA use FilelIst instead of this...
 *  has nearly all methods needed..inclusive storage..
 * @author Quicksilver
 *
 */
public class Category {

	private String name = "";
	
	
	
	/**
	 * files that definitely fall into that category (a blacklist)
	 */
	private final Map<HashValue,ListFile> verifiedList = new HashMap<HashValue,ListFile>();
	
	/**
	 * files that definitely don't fall into that category (a whitelist)
	 */
	private final Map<HashValue,ListFile> falsifiedList = new HashMap<HashValue,ListFile>();
	
	
	public Category() {}
	
	/**
	 * 
	 * @param f - file which's TTH root should be checked
	 * @return true if the file is in the whitelist
	 */
	public boolean isInWhitelist(IDownloadableFile f) {
		return falsifiedList.containsKey(f.getTTHRoot());
	}
	
	/**
	 * 
	 * @param f - file which's TTH root should be checked
	 * @return true if the file is in the blacklist
	 */
	public boolean isInBlacklist(IDownloadableFile f) {
		return verifiedList.containsKey(f.getTTHRoot());
	}
	
	public void addFile(IDownloadableFile f , boolean blacklistTheFile) {
		ListFile l = new ListFile(f.getSize(),f.getTTHRoot(),f.getName());
		if (blacklistTheFile) {
			verifiedList.put(l.hash, l);
			falsifiedList.remove(l.hash);
		} else {
			falsifiedList.put(l.hash, l);
			verifiedList.remove(l.hash);
		}
	}
	
	
	
	
	public static class ListFile {

		private final long size;
		private final HashValue hash;
		private final String filename;
		
		public ListFile(long size, HashValue hash, String filename) {
			this.size = size;
			this.hash = hash;
			this.filename = filename;
		}

		public long getSize() {
			return size;
		}

		public HashValue getHashValue() {
			return hash;
		}

		public String getFilename() {
			return filename;
		}


		
		
		
	}
}
