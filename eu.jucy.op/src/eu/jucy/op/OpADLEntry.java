package eu.jucy.op;



import helpers.GH;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uc.IHub;
import uc.files.filelist.FileListFile;
import uc.files.search.SearchType;
import eu.jucy.adlsearch.ADLSearchEntry;
import eu.jucy.op.CounterFactory.WorkingCounter;

public class OpADLEntry  extends ADLSearchEntry {

	
	private String counter = "";
	private int incrementBy = 0;
	
	private boolean breakAfterRaw;
	private String raw = "";
	
	
	private boolean regExp = false;
	private boolean caseSensitive = true;
	
	/**
	 * a black and white list pair
	 * against which this file should be checked
	 * 
	 * TODO
	 * empty for no list..
	 */
	private String listName = ""; 
	
	
	private String comment = "";
	
	/**
	 * further restriction for matches..
	 */
	private SearchType searchType = SearchType.Any;
	
	//cache variable only active during searching..
	


	private Pattern p;
	

	

	@Override
	public String[] toStringAR() {
		String[] data = super.toStringAR();
		
		if (data.length != ArrayLength) {
			throw new IllegalStateException();
		}
		
		String[] allData = new String[data.length+9];
		System.arraycopy(data, 0, allData, 0, data.length);
		

		int i = data.length;
		allData[i+0] = counter;
		allData[i+1] = ""+incrementBy;
		allData[i+2] = raw;
		allData[i+3] = ""+breakAfterRaw;
		allData[i+4] = ""+regExp;
		allData[i+5] = ""+caseSensitive;
		allData[i+6] = comment;
		allData[i+7] = listName;
		allData[i+8] = searchType.name();
		
		
		return allData;
	}
	
	public static OpADLEntry fromStringAR(String[] data) {
		OpADLEntry oae = new OpADLEntry();
		oae.setDataFromPrefs(data);
		
		int i = ArrayLength;
		
		oae.counter = data[i+0];
		oae.incrementBy = Integer.parseInt(data[i+1]);
		oae.raw = data[i+2];
		oae.breakAfterRaw = Boolean.parseBoolean(data[i+3]);
		oae.regExp = Boolean.parseBoolean(data[i+4]);
		oae.caseSensitive = Boolean.parseBoolean(data[i+5]);
		oae.comment = data[i+6];
		oae.listName = data[i+7];
		if (data.length > i+8) {
			oae.searchType = SearchType.valueOf(data[i+8]);
		}
		
		return oae;
	}
	

	
	
	
	@Override
	public void finishedSearch() {
		p = null;
		super.finishedSearch();
	}

	@Override
	public boolean matches(FileListFile file) {
		boolean matches = searchType.matches(file);
		if (matches) {
			if (isRegExp()) {
				Matcher m = getPattern().matcher(getToMatch(file,false));
				matches = m.matches();
			} else {
				matches = super.matches(file);
			}
		}
		return matches;
	}
	
	public boolean execute(FileListFile f , Map<String,WorkingCounter>  counters , IHub hub/*,IUser usr*/) {
		
		if (incrementBy != 0 && !GH.isNullOrEmpty(counter)) {
			WorkingCounter count = counters.get(counter);
			if (count != null) {
				count.addFile(f, incrementBy);
			}
		}
		
		if (!GH.isNullOrEmpty(raw)) {
			hub.sendRaw(raw, new OpADLSendContext(hub,f,comment));
			if (breakAfterRaw) {
				return true;
			}
		}
		
		return false;
	}

	public String getCounter() {
		return counter;
	}
	
	private Pattern getPattern() {
		if (p == null) {
			p = Pattern.compile(getSearchString(), 
					Pattern.DOTALL |(caseSensitive?  0:Pattern.UNICODE_CASE|Pattern.CASE_INSENSITIVE));
		}
		return p;
	}

	public void setCounter(String counter) {
		this.counter = counter;
	}

	public int getIncrementBy() {
		return incrementBy;
	}

	public void setIncrementBy(int incrementBy) {
		this.incrementBy = incrementBy;
	}

	public String getRaw() {
		return raw;
	}

	public void setRaw(String raw) {
		this.raw = raw;
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

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public boolean isBreakAfterRaw() {
		return breakAfterRaw;
	}

	public void setBreakAfterRaw(boolean breakAfterRaw) {
		this.breakAfterRaw = breakAfterRaw;
	}
	
	public SearchType getFileType() {
		return searchType;
	}

	public void setFileType(SearchType searchType) {
		this.searchType = searchType;
	}
	
}
