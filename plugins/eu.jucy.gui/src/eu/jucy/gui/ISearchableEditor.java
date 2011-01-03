package eu.jucy.gui;


/**
 * Defines objects that can be searched ..
 * 
 * example for this are FileList and TextFields
 * used by Find command to determine which editors can be searched..
 * 
 * 
 * @author Quicksilver
 *
 */
public interface ISearchableEditor {

	/**
	 * searches for the search string and presents the 
	 * first result to the user
	 * @param searchstring
	 */
	void search(String searchstring);
	
	/**
	 * presents the next result of the last search  
	 */
	void next();
	
}
