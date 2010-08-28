package uc.files.search;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import uc.LanguageKeys;
import uc.files.IDownloadable.IDownloadableFile;



/**
 * @author Quicksilver
 *
 */
public enum SearchType {
	ANY(LanguageKeys.SearchType_ANY,1),
	AUDIO(LanguageKeys.SearchType_AUDIO,2
			, "mp3", "mp2", "wav",  "au",  "rm", "mid",  "sm","flac", "ogg"),
	COMPRESSED(LanguageKeys.SearchType_COMPRESSED,3	
			, "zip", "arj", "rar", "lzh",  "gz",   "z", "arc", "pak",  "7z", "bz2"),
	DOCUMENT(LanguageKeys.SearchType_DOCUMENT,4		
			, "doc", "txt", "wri", "pdf",  "ps", "tex", "odt", "chm", "nfo"),
	EXECUTABLE(LanguageKeys.SearchType_EXECUTABLE,5
			,  "pm", "exe", "bat", "com"),
	PICTURE(LanguageKeys.SearchType_PICTURE,6
			, "gif", "jpg", "jpeg", "bmp", "pcx", "png", "wmf", "psd"),
	VIDEO(LanguageKeys.SearchType_VIDEO,7
			, "mpg", "mpeg", "avi", "asf", "mov", "mkv", "wmv", "ogm", "mp4"),
	FOLDER(LanguageKeys.SearchType_FOLDER,8),
	TTH(LanguageKeys.SearchType_TTH,9),
	VIDEO_OR_PICTURE(LanguageKeys.SearchType_VIDEOorPICTURE,VIDEO,PICTURE);
	
	
	/**
	 * 
	 * @param fileendings - the filelendings this type represents
	 * @param nmdctype - the number corresponding to this in the nmdcprotocol
	 */
	SearchType(String translation,int nmdctype,String... fileendings) {
		endings = new HashSet<String>(Arrays.asList( fileendings ));
		this.nmdctype = nmdctype; 
		this.translation = translation;
	}
	
	SearchType(String translation,SearchType... sts) {
		endings = new HashSet<String>();
		for (SearchType st: sts) {
			endings.addAll(st.endings);
		}
		this.nmdctype = -1;
		this.translation = translation;
	}
	
	
	
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
		return ANY;
	}

	

	private final Set<String> endings;
	private final int nmdctype;
	private final String translation;
	

	public boolean matches(IDownloadableFile file) {
		if (endings.isEmpty()) {
			return true;
		} else {
			return endings.contains(file.getEnding());
		}
	}
	
	
	
	public static List<SearchType> getNMDCSearchTypes() {
		return Arrays.asList(ANY,AUDIO,COMPRESSED,DOCUMENT,EXECUTABLE,PICTURE,VIDEO,FOLDER,TTH);
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

	public String toString() {
		return translation;
	}
}
