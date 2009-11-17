package uc.files.search;

import uc.files.IDownloadable;

/**
 * interface showing that this is a search result ..
 * 
 */
public interface ISearchResult extends IDownloadable {
	
	int getAvailabelSlots();
	int getTotalSlots();
	
}