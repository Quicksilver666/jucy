package uc.protocols;

import java.net.InetAddress;

public interface IConnectionDebugger {
	
	/**
	 * called when ever a command was received from  protocol
	 * 
	 * @param commandHandler - the handler that will be used to handle the command
	 *  if this is null means that no command handler could be determined -> unexpected command received
	 * 
	 * @param wellformed - false if the command had correct prefix but was not parseable
	 * @param command - string representation of the whole command..
	 */
	void receivedCommand(IProtocolCommand commandHandler,boolean wellFormed,String command);
	
	void sentCommand(String sent);

	/**
	 * called when ever a connection with before specified IP was created..
	 * @param ia - the address it should have been attached to
	 * @param attachedTo - the protocol it was attached to
	 * @return true if auto-attachment should cease false for further listening..
	 */
	void notifyAttachable(InetAddress ia,ConnectionProtocol attacheableTo);
}
