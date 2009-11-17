package eu.jucy.gui.texteditor.hub;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;


import uc.FavHub;


public class HubEditorInput implements IEditorInput {
	
private FavHub favHub;

	public HubEditorInput(FavHub favHub){
		super();
		Assert.isNotNull(favHub);
	
		this.favHub=favHub;

	}

	public boolean exists() {
		return false;
	}

	public FavHub getFavHub(){
		return favHub;
	}
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	public String getName() {
		String modified=getToolTipText();
		
		if (modified.length() > 20 ) {
			modified=modified.substring(0, 17)+"...";
		}
		return modified ;

	}
	
	

	public IPersistableElement getPersistable() {
		return null;
	}

	public String getToolTipText() {
		return (favHub.getHubname()!=null? favHub.getHubname(): "")
				+(favHub.getHubaddy() != null ?"("+favHub.getHubaddy()+")":"" );
	}

	@SuppressWarnings("unchecked")
	public Object getAdapter(Class adapter) {
		return null;
	}
	
	public boolean equals(Object obj){
		if(super.equals(obj))
			return true;
		if(! (obj.getClass() == this.getClass()))
			return false;
		return ((HubEditorInput)obj).favHub.equals(favHub);
	}
	
	public int hashCode(){
		return favHub.hashCode();
	}



}
