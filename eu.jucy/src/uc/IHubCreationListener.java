package uc;




/**
 * listener so the GUI can get information on created hubs..
 * @author Quicksilver
 *
 */
public interface IHubCreationListener {

	
	/**
	 * notifies on when a hub is created
	 * 
	 * @param fh - the FavHub used to create the hub..
	 * @param showInUI tells if the hub should be shown to the user
	 */
	void hubCreated(FavHub fh, boolean showInUI);
	
}
