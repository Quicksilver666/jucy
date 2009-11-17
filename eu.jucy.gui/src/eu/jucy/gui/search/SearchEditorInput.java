package eu.jucy.gui.search;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import uc.crypto.HashValue;

import eu.jucy.gui.Lang;




public class SearchEditorInput implements IEditorInput {

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
	
	public boolean exists() {
		return false;
	}

	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	public String getName() {
		return Lang.Search;
	}

	public IPersistableElement getPersistable() {
		return null;
	}

	public String getToolTipText() {
		return getName();
	}

	@SuppressWarnings("unchecked")
	public Object getAdapter(Class adapter) {
		return null;
	}



	public HashValue getInitialsearch() {
		return initialsearch;
	}
	public String getAlternate() {
		return alternate;
	}
	
	

}
