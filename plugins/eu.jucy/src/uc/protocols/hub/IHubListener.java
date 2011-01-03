package uc.protocols.hub;


import uc.FavHub;
import uc.IUser;
import uc.protocols.IProtocolStatusChangedListener;

/**
 * collection of all listeners needed 
 * to watch the hub .. used by UI 
 * @author Quicksilver
 *
 */
public interface IHubListener extends IProtocolStatusChangedListener {
	
	/**
	 * private message on hub received.. 
	 * @param pm
	 */
	void pmReceived(PrivateMessage pm);
	
	/**
	 * for usual messages
	 * @param sender the sender
	 * @param message  the message 
	 * 
	 * ex: <Niclas> hello|     --> sender Niclas     message hello
	 **/
	void mcReceived(IUser sender , String message,boolean me);
	
	/**
	 * if no user can be determined... or malformed input ..
	 * @param message the whole message
	 * 
	 * ex: *Niclas* says hello|   --> message *Niclas* says hello    statusmessage false
	 * 
	 * @param statusMessage  false for a normal message 
	 * 						true if it is a message that just shows the status of the hub
	 * 						or some other message that was not send from the hub
	 * 
	 */
	void mcReceived(String message );
	
	
	/**
	 * 
	 * @param message - the message to be printed
	 * @param severity - a severity taken from ADC  
	 *  0 normal 
	 *  1 warn
	 *  2 fatal
	 */
	void statusMessage(String message, int severity);
	
	/**
	 * tells that either the hubname or the topic have changed
	 * @param hubname
	 * @param topic
	 */
	public void hubnameChanged(String hubname,String topic); 

	void feedReceived(FeedType ft,String message);
	
	/**
	 * hub received request for redirection..
	 * 
	 * @param target  to whcih hub the redirect goes..
	 */
	void redirectReceived(FavHub target);
		
	}
