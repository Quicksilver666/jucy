package eu.jucy.ui.searchspy;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;



public class SearchSpyEditorInput implements IEditorInput {

	
	public boolean exists() {
		return false;
	}

	
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	
	public String getName() {
		return Lang.SearchSpy;
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

	
	public boolean equals(Object obj) {
		return getClass().equals(obj.getClass());
	}

	
	public int hashCode() {
		return  getClass().hashCode();
	}
	
	

}
