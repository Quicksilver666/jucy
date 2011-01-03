package eu.jucy.gui.search;


import uc.crypto.HashValue;

import eu.jucy.gui.Lang;
import eu.jucy.gui.UCEditorInput;


public class SearchEditorInput extends UCEditorInput {

	private final HashValue initialsearch;
	
	private final String alternate;
	
	public SearchEditorInput() {
		this((HashValue)null);
	}
	/**
	 * 
	 * @param initialsearch - search for alternates
	 */
	public SearchEditorInput( HashValue initialsearch) {
		this.initialsearch = initialsearch;
		this.alternate = null;
	}
	
	public SearchEditorInput(String initialSearch) {
		this.initialsearch = null;
		this.alternate = initialSearch;
	}

	public String getName() {
		return Lang.Search;
	}

	public HashValue getInitialsearch() {
		return initialsearch;
	}
	public String getAlternate() {
		return alternate;
	}
	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}
	@Override
	public boolean equals(Object obj) {
		return obj == this;
	}

	

}
