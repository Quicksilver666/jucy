package eu.jucy.op;

import java.util.HashMap;
import java.util.Map;

import uc.crypto.HashValue;
import uc.files.filelist.FileListFile;

public class BlackWhiteList {

	private String name = "";
	
	private final Map<HashValue,ListFile> blacklist = new HashMap<HashValue,ListFile>();
	
	private final Map<HashValue,ListFile> whitelist = new HashMap<HashValue,ListFile>();
	
	
	/**
	 * 
	 * @param f - file which's TTH root should be checked
	 * @return true if the file is in the whitelist
	 */
	public boolean checkWhitelist(FileListFile f) {
		return whitelist.containsKey(f.getTTHRoot());
	}
	
	/**
	 * 
	 * @param f - file which's TTH root should be checked
	 * @return true if the file is in the blacklist
	 */
	public boolean checkBlacklist(FileListFile f) {
		return blacklist.containsKey(f.getTTHRoot());
	}
	
	public void addFile(FileListFile f , boolean blacklistTheFile) {
		ListFile l = new ListFile(f.getSize(),f.getTTHRoot(),f.getName());
		if (blacklistTheFile) {
			blacklist.put(l.tthRoot, l);
		} else {
			whitelist.put(l.tthRoot, l);
		}
	}
	
	
	
	
	public static class ListFile {

		private final long size;
		private final HashValue tthRoot;
		private final String filename;
		
		public ListFile(long size, HashValue tthRoot, String filename) {
			this.size = size;
			this.tthRoot = tthRoot;
			this.filename = filename;
		}

		public long getSize() {
			return size;
		}

		public HashValue getTthRoot() {
			return tthRoot;
		}

		public String getFilename() {
			return filename;
		}
		
	}
}
