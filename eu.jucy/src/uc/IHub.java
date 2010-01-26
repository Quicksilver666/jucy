package uc;

import java.util.List;


import uc.files.search.FileSearch;
import uc.protocols.CPType;
import uc.protocols.SendContext;


/**
 * interface representing the hub to everyone..
 * so no direct access to the moddel is given to everyone.
 * 
 * 
 * @author Quicksilver
 *
 */
public interface IHub {
	
	/**
	 * 
	 * @return the user we present ourselfes in the hub..
	 */
	IUser getSelf();

	
	void search(FileSearch search);
	
	/**
	 * the hub will disconnect if connected and then 
	 * connect again - 
	 * @param waittime is the seconds the hub will wait before reconnecting 
	 */
	void reconnect(int waittime);
	
	
	/**
	 * this will close the hub .. and make it unusable for the future
	 */
	void close();
	
	
	/**
	 * 
	 * @return true if folloRedirect will 
	 * bring client to connect to a new address 
	 */
	boolean pendingReconnect();
	
	/**
	 * if a redirect is pending on calling this function the hub
	 * will follow the redirect.
	 */
	void followLastRedirect();
	
	/**
	 * 
	 * @return a set of all commands currently
	 * available from the hub
	 * 
	 */
	List<Command> getUserCommands();
	
	
	/**
	 * 
	 * @return the name of the hub  (without topic)
	 */
	String getName();
	
	/**
	 * sends raw message to the hub. Context provides info
	 * for filling out %[] tags
	 * @param message raw message to the hub
	 * @param context - information for formatting the message
	 */
	void sendRaw(String message,SendContext context);
	
	/**
	 * sends a Main-chat Message to the hub
	 * @param message - what is sent.. 
	 */
	void sendMM(String message,boolean me);
	
	/**
	 * retrieves a user by his nick
	 * 
	 * @param nick - the nick of the user on search
	 * @return null if none found else the user
	 */
	IUser getUserByNick(String nick);
	
	/**
	 * @return FavHub token used to create that hub
	 */
	FavHub getFavHub();
	
	
	/**
	 * 
	 * @param target - to which user
	 * @param protocol which protocol should be uses
	 * @param token - token (ADC only)
	 */
	 void sendCTM(IUser target, CPType protocol,String token);

	 
		/**
		 * 
		 * @param target - to which user
		 * @param protocol which protocol should be uses
		 * @param token - token (ADC only)
		 */
	 void sendRCM(IUser target,CPType protocol,String token);

	 /**
	  * 
	  * @return true if the hub is an nmdc hub.. false for adc..
	  */
	boolean isNMDC();
	
	/**
	 * @return time of last login
	 */
	long getLastLogin();
}
