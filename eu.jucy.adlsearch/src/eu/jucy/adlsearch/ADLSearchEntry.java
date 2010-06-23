package eu.jucy.adlsearch;

import helpers.GH;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;




import uc.files.filelist.FileListFile;
import uc.files.filelist.FileListFolder;

public class ADLSearchEntry {

	protected static final int ArrayLength = 9;
	public static enum ADLSearchType {
		Filename(Lang.ADL_Filename),Directory(Lang.ADL_Directory),FullPath(Lang.ADL_FullPath); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		ADLSearchType(String name) {
			this.name = name;
		}
		
		private final String name;
		
		public String toString() {
			return name;
		}
		
	}
	
	//permanent information

	private String searchString = ""; //$NON-NLS-1$
	
	private ADLSearchType aDLSearchType = ADLSearchType.Filename;
	
	private long minSize = -1;
	
	private long maxSize = -1;
	
	private String targetFolder = "ADLSearch"; //$NON-NLS-1$
	
	private boolean active = true;
	
	private boolean downloadMatches = false;
	
	private boolean regExp = false;
	private boolean caseSensitive = true;
	private Pattern p;
	
	//not persisted information
	
	//these help searching through whole path by making full path creation faster
	protected FileListFolder lastParentFolder = null;
	protected String lastParentPath = null;
	

	
	private List<String> onSearch = null;
	
	
	public ADLSearchEntry() {}
	
	
	public boolean canBeUsed() {
		return active && !getOnSearch().isEmpty() && !GH.isEmpty(targetFolder);
	}
	
	
	public boolean isActive() {
		return active ;
	}



	public String getSearchString() {
		return searchString;
	}



	public void setSearchString(String searchString) {
		this.searchString = searchString;
		onSearch = null;
	}



	public long getMinSize() {
		return minSize;
	}



	public void setMinSize(long minsize) {
		this.minSize = minsize;
	}



	public long getMaxSize() {
		return maxSize;
	}



	public void setMaxSize(long maxSize) {
		this.maxSize = maxSize;
	}



	public String getTargetFolder() {
		return targetFolder;
	}



	public void setTargetFolder(String targetFolder) {
		this.targetFolder = targetFolder;
	}



	public boolean isDownloadMatches() {
		return downloadMatches;
	}



	public void setDownloadMatches(boolean downloadMatches) {
		this.downloadMatches = downloadMatches;
	}



	public void setActive(boolean active) {
		this.active = active;
	}
	
	
	

	public ADLSearchType getSearchType() {
		return aDLSearchType;
	}

	public void setSearchType(ADLSearchType aDLSearchType) {
		this.aDLSearchType = aDLSearchType;
	}
	
	
	public String[] toStringAR() {
		return new String[]{""+active , //$NON-NLS-1$
				aDLSearchType.name(),
				""+downloadMatches, //$NON-NLS-1$
				""+maxSize, //$NON-NLS-1$
				""+minSize, //$NON-NLS-1$
				searchString,
				targetFolder
				,""+regExp
				,""+caseSensitive};
	} 
	
	private List<String> getOnSearch() {
		if (onSearch == null) {
			String[] all = searchString.split(Pattern.quote(" ")); //$NON-NLS-1$
			onSearch = new ArrayList<String>(all.length);
			for (String s:all) {
				if (/*!s.startsWith("-") &&*/ !GH.isEmpty(s)) {
					onSearch.add(s.toLowerCase());
				}
			}
		}
		return onSearch;
	}
	
	/**
	 * checks if this ADLEntry matches the given file
	 * the ADL entry must be active when this is called.
	 * 
	 *  
	 * @param file
	 * @return
	 */
	public boolean matches(FileListFile file) {
		long size = file.getSize();

		if (size < minSize || (maxSize != -1 && size > maxSize )) {
			return false;
		}
		
		if (regExp) {
			Matcher m = getPattern().matcher(getToMatch(file,false));
			return m.matches();
		} else {
			String toMatch = getToMatch(file,true);
			for (String check:getOnSearch()) {
				if (!toMatch.contains(check)) {
					return false;
				}
			}
		}
		return true;
	}
	
	private Pattern getPattern() {
		if (p == null) {
			p = Pattern.compile(getSearchString(), 
					Pattern.DOTALL |(caseSensitive?  0:Pattern.UNICODE_CASE|Pattern.CASE_INSENSITIVE));
		}
		return p;
	}
	
	/**
	 * retrieves depending on path directory of filename..
	 * the string from the file ..
	 * 
	 * @param file - the file from which we want the string to search
	 * @return the string to match against
	 */
	protected String getToMatch(FileListFile file,boolean allLowercase) {
		String toMatch;
		switch(aDLSearchType) {
		case Filename:
			toMatch = file.getName();
			if (allLowercase) {
				toMatch = toMatch.toLowerCase();
			}
			break;
		case FullPath:
			if (file.getParent() != lastParentFolder) {
				lastParentFolder = file.getParent();
				lastParentPath = file.getParent().getPath()+java.io.File.separator;
				if (allLowercase) {
					lastParentPath = lastParentPath.toLowerCase();
				}
			}
			toMatch = lastParentPath+  (allLowercase? file.getName().toLowerCase():file.getName());
			break;
		case Directory:
			if (file.getParent() != lastParentFolder) {
				lastParentFolder = file.getParent();
				lastParentPath = file.getParent().getPath()+java.io.File.separator;
				if (allLowercase) {
					lastParentPath = lastParentPath.toLowerCase();
				}
			}
			toMatch = lastParentPath;
			break;
		default:
			throw new IllegalStateException();
		}
		return toMatch;
	}
	
	
	
	/**
	 * clears caching variables for faster c
	 */
	public void finishedSearch() {
		lastParentFolder = null;
		lastParentPath = null;
		onSearch = null;
		p = null;
	}
	
	
	
	public static ADLSearchEntry fromString(String[] s) {
		ADLSearchEntry adlse = new ADLSearchEntry();
		adlse.setDataFromPrefs(s);
		return adlse;
	}
	
	protected void setDataFromPrefs(String[] s) {
		active = Boolean.valueOf(s[0]);
		aDLSearchType = ADLSearchType.valueOf(s[1]);
		downloadMatches = Boolean.valueOf(s[2]);
		maxSize = Long.valueOf(s[3]);
		minSize = Long.valueOf(s[4]);
		searchString = s[5];
		targetFolder = s[6];
		if (s.length >= 9 ) {
			regExp = Boolean.parseBoolean(s[7]);
			caseSensitive = Boolean.parseBoolean(s[8]);
		}
	}
	
	public boolean isRegExp() {
		return regExp;
	}

	public void setRegExp(boolean regExp) {
		this.regExp = regExp;
	}
	


	public boolean isCaseSensitive() {
		return caseSensitive;
	}

	public void setCaseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}

	
}
