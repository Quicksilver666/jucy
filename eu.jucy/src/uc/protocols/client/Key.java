package uc.protocols.client;

import java.io.IOException;

import logger.LoggerFactory;


import org.apache.log4j.Logger;


public class Key extends AbstractNMDCClientProtocolCommand {

	private static final Logger logger = LoggerFactory.make();
	

	@Override
	public void handle(ClientProtocol client,String command) throws IOException {
		logger.debug(command);
		//client.onLogIn();
		client.increaseLoginLevel();

		client.removeCommand(this);
	}

}
