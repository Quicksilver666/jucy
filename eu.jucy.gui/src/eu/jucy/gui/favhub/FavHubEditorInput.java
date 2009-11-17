package eu.jucy.gui.favhub;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import eu.jucy.gui.Lang;




public class FavHubEditorInput implements IEditorInput {



	public FavHubEditorInput(){
	}
	
	public boolean exists() {
		return false;
	}

	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorInput#getName()
	 */
	public String getName() {
		return Lang.FavoriteHubs;
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
	
	public int hashCode(){
		return -1;
	}
	
	public boolean equals(Object o){
		if(o==null)
			return false;
		return this.getClass() == o.getClass();
	}



}
