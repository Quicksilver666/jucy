package eu.jucy.gui.favhub;


import org.eclipse.swt.widgets.Composite;
import uc.FavHub;

public interface IFavHubAdvanced {
	
	public static final String ExtensionPointID =  "eu.jucy.gui.favhub.FavHubAdvanced";
	
	ICompControl fillComposite(Composite fill,FavHub favHub);
	
	
	public static interface ICompControl {
		void okPressed(FavHub favHub);
	}

}
