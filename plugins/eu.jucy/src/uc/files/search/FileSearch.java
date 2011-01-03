package uc.files.search;



import helpers.GH;
import helpers.Observable;
import helpers.StatusObject;
import helpers.StatusObject.ChangeType;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



import org.eclipse.core.runtime.Assert;


import uc.crypto.HashValue;
import uc.crypto.UDPEncryption;
import uc.files.MultiUserAbstractDownloadable;
import uc.files.IDownloadable.IDownloadableFile;

import uc.files.search.SearchResult.ISearchResultListener;

/**
 * A command pattern for searches
 * 
 * also accumulates the search results..
 * 
 * Search in jucy works like this:
 * SearchEditor creates a Search  and puts it in DCClient to start search and listen for results..
 * 
 * UDPHandler and Hub   listen for incoming SearchResults  when UDPHandler receives a SearchResult 
 * the hub of the sender is determined over the nick(NMDC, CID in ADC) (and IP:port of the hub)... 
 *  then the search result is forwarded to this hub and treated as received by the hub
 *  
 * the hub gets information from the searchResult string and creates an SR object from it
 * 
 * this sr is then passed to the dcclient that forwards it to all ISearchResultsListener 
 * registered with the client
 * 
 * @author Quicksilver
 *
 */
public class FileSearch extends Observable<StatusObject> implements ISearchResultListener {



	/**
	 * collection containing all results..
	 */
	private final List<ISearchResult> results = Collections.synchronizedList(
			new ArrayList<ISearchResult>());
	
	private final Map<HashValue,ISearchResult> resultFiles = Collections.synchronizedMap(
			new HashMap<HashValue,ISearchResult>());
	

	
	/**
	 * type enum for search
	 */
	private final SearchType searchType;
	
	/**
	 * enum for larger .. or smaller.. or equal size
	 */
	private final ComparisonEnum comparisonEnum;
	
	/**
	 * if comparisonEnum  sets a comparison
	 * then this will be to compare against
	 * -1 if no comparison should be used
	 */
	private final long size;
	
	/**
	 *  the string the user typed in...
	 */
	private final String searchString;
	
	private static int SEARCH_NONCE_COUNTER = GH.nextInt(100);
	
	private static synchronized int getNextNonce() {
		SEARCH_NONCE_COUNTER+=GH.nextInt(100)+1;
		return SEARCH_NONCE_COUNTER;
	}
	
	private final String token;
	private final byte[] encryptionKey ;
	
	private int nrOfResults = 0;
	
	/**
	 * how long SearchResults will be accepted at most..
	 * 10 minutes default..
	 */
	public static final int MAX_SEARCH_TIME = 10*60*1000;
	private final long startOfSearch;


	/**
	 * creates a search type
	 * @param searchstring - the string of the search
	 * @param searchType - what to search for .. usually ALL or TTH
	 * @param comparisonEnum - if there is a size restriction ... if size == -1 --> comparison = null
	 * @param size
	 */
	public FileSearch(String searchstring , SearchType searchType, ComparisonEnum comparisonEnum, long size) {
		this.searchString = searchstring;
		this.searchType = searchType;
		this.comparisonEnum = comparisonEnum;
		
		Assert.isTrue(comparisonEnum != null || size == -1);
		
		this.size = size;
		
		encryptionKey = UDPEncryption.getRandomKey();
		this.token = Integer.toHexString(getNextNonce());
		startOfSearch = System.currentTimeMillis();
	}
	
	/**
	 * used for automatic search for alternatives..
	 * @param tth -the tth root to search for..
	 */
	public FileSearch(HashValue tth) {
		this(tth.toString(),SearchType.TTH,null,-1);
	}
	
	/**
	 * 
	 * @param search - string sequence used for searching
	 * @return a list of all search result containing the given string sequence
	 * case insensitive
	 */
	public List<ISearchResult> searchSubset(String search) {
		List<ISearchResult> srs = new ArrayList<ISearchResult>();
		search = search.toLowerCase();
		
		for (ISearchResult sr: results) {
			if (sr.getPath().toLowerCase().contains(search)) {
				srs.add(sr);
			}
		}

		return srs;
	}

	public ComparisonEnum getComparisonEnum() {
		return comparisonEnum;
	}


	public String getSearchString() {
		return searchString;
	}

	

	public SearchType getSearchType() {
		return searchType;
	}


	public long getSize() {
		return size;
	}

	/**
	 * test a search result if it matches this search
	 * 
	 * @param sr - a SearchResult to test
	 * @return if the SearchResult matches this search
	 */
	private boolean matches(ISearchResult sr) {
		if (sr.getToken() != null) {
			return sr.getToken().equals(token);
		}
		
		if (System.currentTimeMillis() - startOfSearch > MAX_SEARCH_TIME ) {
			return false;
		}
		if (sr.isFile() && sr.getTTHRoot().toString().equals(getSearchString())) {
			return true;
		}
		
		
		String path = sr.getPath();
		String[] words= getSearchString().split(" ");
		for (String word: words) {  //remove all strings starting with -
			if (word.startsWith("-")) {
				if (word.length() > 1 && path.toLowerCase().contains(word.substring(1).toLowerCase())) {
					return false;
				}
			} else {
				if (!path.toLowerCase().contains(word.toLowerCase()) ) {
					return false;
				}
			}
		}
		
		return true;
	}
	
	private void add(ISearchResult sr){
		synchronized(this) {
			nrOfResults++;
		}
		if (sr.isFile()) {
			IDownloadableFile f = (IDownloadableFile)sr;
			ISearchResult present = resultFiles.get(sr.getTTHRoot());
			if (present != null) {
				if (present instanceof MultiUserAbstractDownloadable) {
					((MultiUserAbstractDownloadable)present).addFile(f);
					fireListener(sr,present,ChangeType.ADDED);
					fireListener(present,this,ChangeType.CHANGED);
				} else {
					results.remove(present);
					resultFiles.remove(present.getTTHRoot());
					fireListener(present,this,ChangeType.REMOVED);
					
					MultiUserAbstractDownloadable muad = new MultiUserAbstractDownloadable((IDownloadableFile)present);
					muad.addFile(f);
					results.add(muad);
					resultFiles.put(muad.getTTHRoot(), muad);
					
				
					fireListener(muad,this,ChangeType.ADDED);
					fireListener(present,muad,ChangeType.ADDED);
					fireListener(sr,muad,ChangeType.ADDED);
					
				}
				
			} else {
				results.add(sr);
				resultFiles.put(sr.getTTHRoot(), sr);
				fireListener(sr,this,ChangeType.ADDED);
			}
		} else {
			results.add(sr);
			fireListener(sr,this,ChangeType.ADDED);
		}
	}
	private void fireListener(ISearchResult sr,Object parent,ChangeType ct ) {
		notifyObservers(new StatusObject(sr, ct, 0, parent));
	}


	public List<ISearchResult> getResults() {
		return results;
	}


	/**
	 * when a  sr is received we check if it matches our search and then add it..
	 */
	public void received(ISearchResult sr) {
		if (matches(sr)) {
			add(sr);
		}
	}
	
	public String getToken() {
		return token;
	}

	public int getNrOfResults() {
		synchronized(this) {
			return nrOfResults;
		}
	}
	
	public int getNrOfFiles() {
		return resultFiles.size();
	}

	public byte[] getEncryptionKey() {
		return encryptionKey;
	}

	
}
