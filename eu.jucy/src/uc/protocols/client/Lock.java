package uc.protocols.client;

import java.io.IOException;


import uc.protocols.DCProtocol;

public class Lock extends AbstractNMDCClientProtocolCommand {

	
	
	
	
	public Lock(ClientProtocol client) {
		super(client);
	}

	@Override
	public void handle(String command) throws IOException {
		Supports.sendSupports(client);
	
		Direction.sendDirectionString(client);

	
		client.sendRaw(DCProtocol.generateKey(command,client.getCharset()));
		client.increaseLoginLevel();
		
		client.removeCommand(this);
		
	}
	


}
