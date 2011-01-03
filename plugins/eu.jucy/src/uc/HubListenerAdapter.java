package uc;




import uc.protocols.ConnectionProtocol;
import uc.protocols.ConnectionState;
import uc.protocols.hub.FeedType;
import uc.protocols.hub.IHubListener;
import uc.protocols.hub.PrivateMessage;


/**
 * help for implementing OperatorPlugins
 * 
 * @author Quicksilver
 *
 */
public abstract class HubListenerAdapter implements IHubListener {

	








	public void mcReceived(IUser sender, String message, boolean me) {}





	public void pmReceived(PrivateMessage pm) {}

	
	

	
	public void mcReceived(String message) {}

	
	public void statusMessage(String message, int severity) {}

	
	public void statusChanged(ConnectionState newStatus, ConnectionProtocol cp) {}

	
	public void feedReceived(FeedType ft, String message) {}

	
	public void hubnameChanged(String hubname, String topic) {}

	
	public void redirectReceived(FavHub target) {}
}
