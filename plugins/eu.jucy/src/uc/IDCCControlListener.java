package uc;

import java.util.concurrent.Semaphore;




/**
 * listener give information to the GUI or other kind of UI  can get information on created hubs..
 * @author Quicksilver
 *
 */
public interface IDCCControlListener {

	
	/**
	 * notifies on when a hub is created
	 * 
	 * @param fh - the FavHub used to create the hub..
	 * @param showInUI tells if the hub should be shown to the user
	 * @param sem .. must release one permit on the semaphore after finishing..
	 *  but method should not block..
	 */
	void hubCreated(FavHub fh, boolean showInUI,Semaphore sem);
	
	
	void requireRestart();
	
}
