package uc.files.search;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import uc.files.IDownloadable.IDownloadableFile;



/**
 * @author Quicksilver
 *
 */
public enum SearchType {
	Any( new String[] {} ,1),
	Audio(new String[] {"mp3", "mp2", "wav", "au", "rm", "mid", "sm","flac","ogg"},2),
	Compressed(new String[]{"zip", "arj", "rar", "lzh", "gz", "z", "arc", "pak","7z","bz2"},3),
	Document(new String[]{"doc", "txt", "wri", "pdf", "ps", "tex","odt","chm","nfo"},4),
	Executable(new String[]{ "pm" , "exe", "bat", "com"},5),
	Picture(new String[]{"gif", "jpg", "jpeg", "bmp", "pcx", "png", "wmf", "psd"},6),
	Video(new String[]{"mpg", "mpeg", "avi", "asf", "mov","mkv","wmv","ogm","mp4"},7),
	Folder(new String[] {},8),
	TTH(new String[] {},9),
	VideoOrPicture(Video,Picture);
	
	
	/**
	 * 
	 * @return the type as in the nmdc protocol
	 * 1 for any
	 * 2 for audio ..
	 * 3 compressed
	 * 4 docs
	 * 5 executables
	 * 6 pics
	 * 7 vids
	 * 8 folders
	 * 9 for TTHroot
	 */
	public static SearchType getNMDC(int type) {
		if (1 <= type && type < 10) {
			return SearchType.values()[type-1];
			/*
			for (SearchType st: values()) {
				if (st.nmdctype == type) {
					return st;
				}
			} */
		}
		return Any;
	}

	

	private final Set<String> endings;
	private final int nmdctype;
	
	/**
	 * 
	 * @param fileendings - the filelendings this type represents
	 * @param nmdctype - the number corresponding to this in the nmdcprotocol
	 */
	SearchType(String[] fileendings,int nmdctype) {
		endings = new HashSet<String>(Arrays.asList( fileendings ));
		this.nmdctype = nmdctype; 
	}
	
	SearchType(SearchType... sts) {
		endings = new HashSet<String>();
		for (SearchType st: sts) {
			endings.addAll(st.endings);
		}
		this.nmdctype = -1;
	}
	
	public boolean matches(IDownloadableFile file) {
		if (endings.isEmpty()) {
			return true;
		} else {
			return endings.contains(file.getEnding());
		}
	}
	
	
	
	public static List<SearchType> getNMDCSearchTypes() {
		return Arrays.asList(Any,Audio,Compressed,Document,Executable,Picture,Video,Folder,TTH);
	}


	/**
	 * 
	 * @return a hashset conataining all endings
	 */
	public Set<String> getEndings() {
		return endings;
	}
	
	/**
	 * 
	 * @return the number for this type in the nmdc protocol
	 */
	public int getNMDC() {
		return nmdctype;
	}

	
}
