package uc.protocols.client;

import java.io.IOException;
import java.net.ProtocolException;



import uc.protocols.AbstractNMDCProtocolCommand;
import uc.protocols.IProtocolCommand;

public abstract class AbstractNMDCClientProtocolCommand extends AbstractNMDCProtocolCommand implements IProtocolCommand<ClientProtocol> {


//	/**
//	 * the client this command belongs to..
//	 */
//	protected final ClientProtocol client;
	

	
	
	public AbstractNMDCClientProtocolCommand() {

	}
	
	

	public abstract void handle(ClientProtocol client,String command) throws ProtocolException, IOException;

	
	
//	protected ConnectionHandler getCH() {
//		return client.getCh();
//	}
	


}
