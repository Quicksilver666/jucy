/**
 * 
 */
package uc.listener;

import uc.protocols.ConnectionState;
import uc.protocols.client.ClientProtocol;

/**
 * 
 * 
 * @author Quicksilver
 *
 */
public interface ITransferChangedListener {

	
	void TransferChanged(ClientProtocol nmdccc, ConnectionState transferState);
	

	
}
