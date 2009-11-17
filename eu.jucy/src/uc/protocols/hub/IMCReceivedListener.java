package uc.protocols.hub;

import uc.IUser;


public interface IMCReceivedListener {
	
	/**
	 * for usual messages
	 * @param sender the sender
	 * @param message  the message 
	 * 
	 * ex: <Niclas> hello|     --> sender Niclas     message hello
	 **/
	void mcReceived(IUser sender , String message);
	
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

}
