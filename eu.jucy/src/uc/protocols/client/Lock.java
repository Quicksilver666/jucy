package uc.protocols.client;

import java.io.IOException;

import logger.LoggerFactory;
import org.apache.log4j.Logger;


import uc.protocols.DCProtocol;

public class Lock extends AbstractNMDCClientProtocolCommand {

	
	private static final Logger logger = LoggerFactory.make();
	
	
	public Lock(ClientProtocol client) {
		super(client);
	}

	@Override
	public void handle(String command) throws IOException {
		logger.debug(command + "  "+ client.isIncoming());
		
		Supports.sendSupports(client);
		Direction.sendDirectionString(client);

	
		client.sendUnmodifiedRaw(DCProtocol.generateKey(command,client.getCharset()));
		client.increaseLoginLevel();
		
		client.removeCommand(this);
		
	}
	


}
