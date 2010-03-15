package uc;


import uc.protocols.hub.IHubListener;


/**
 * interface for extended steering of the client as needed  
 * by Operator Clients
 * 
 * the plug-in will be notified on all User-Changes in the OP Hub.
 * 
 * @author Quicksilver
 *
 */
public interface IOperatorPlugin extends 	IHubListener,IUserChangedListener {

	public static final String PointID = "eu.jucy.OpPlugin";
	
	/**
	 * notifies us that we were announced operator status in provided hub
	 * for the first time since hub start..
	 * 
	 * gives the plug-in the chance to decide if it wants to be used with that hub
	 * 
	 * @param hub - the hub that told us we are OP
	 * @return true if we want to register with that hub as operator plug-in
	 */
	boolean init(IHub hub);
	
}
