package uc.protocols.client;

import java.io.IOException;


public class Key extends AbstractNMDCClientProtocolCommand {

	
	
	public Key(ClientProtocol client) {
		super(client);
	}

	@Override
	public void handle(String command) throws IOException {
		//client.onLogIn();
		client.increaseLoginLevel();

		client.removeCommand(this);
	}

}
