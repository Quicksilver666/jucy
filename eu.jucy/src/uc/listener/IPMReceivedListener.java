package uc.listener;


import uc.protocols.hub.PrivateMessage;

public interface IPMReceivedListener {

	void pmReceived(PrivateMessage pm);
	
}
