package uc.protocols.client;

import java.io.IOException;
import java.net.ProtocolException;


import uc.ConnectionHandler;

import uc.protocols.AbstractNMDCProtocolCommand;
import uc.protocols.IProtocolCommand;

public abstract class AbstractNMDCClientProtocolCommand extends AbstractNMDCProtocolCommand implements IProtocolCommand {


	/**
	 * the client this command belongs to..
	 */
	protected final ClientProtocol client;
	

	
	
	public AbstractNMDCClientProtocolCommand(ClientProtocol client) {
		this.client = client;
	}
	
	

	public abstract void handle(String command) throws ProtocolException, IOException;

	
	
	protected ConnectionHandler getCH() {
		return client.getCh();
	}
	


}
