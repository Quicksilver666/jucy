package eu.jucy.gui.texteditor.hub;

import org.eclipse.core.runtime.Assert;


import eu.jucy.gui.UCEditorInput;


import uc.FavHub;


public class HubEditorInput extends UCEditorInput {
	
	private final FavHub favHub;

	public HubEditorInput(FavHub favHub){
		super();
		Assert.isNotNull(favHub);
	
		this.favHub = favHub;

	}



	public FavHub getFavHub(){
		return favHub;
	}


	public String getName() {
		String modified=getToolTipText();
		
		if (modified.length() > 20 ) {
			modified=modified.substring(0, 17)+"...";
		}
		return modified ;

	}
	

	public String getToolTipText() {
		return (favHub.getHubname()!= null? favHub.getHubname(): "")
				+(favHub.getHubaddy() != null ?"("+favHub.getHubaddy()+")":"" );
	}

	
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if(! (obj.getClass() == this.getClass()))
			return false;
		return ((HubEditorInput)obj).favHub.equals(favHub);
	}
	
	public int hashCode(){
		return favHub.hashCode();
	}

}
