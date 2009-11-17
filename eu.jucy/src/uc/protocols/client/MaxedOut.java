package uc.protocols.client;

import java.io.IOException;

import logger.LoggerFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class MaxedOut extends AbstractNMDCClientProtocolCommand {

	private static final Logger logger = LoggerFactory.make(); 
	
	static {
		logger.setLevel(Level.INFO);
	}
	
	public MaxedOut(ClientProtocol client) {
		super(client);
		setPattern(prefix+"(.*)",true); //just use the prefix as pattern -> prevents check for the space.
		//.* is for some special clients that might send some number afterwards ? $MaxedOut 3| ??
	}

	@Override
	public void handle(String command) throws IOException {
		logger.debug("received maxedOut "+command);
		client.noSlotsAvailable( matcher.group(1).trim());
	}	
	
	public static void sendMaxedOut(ClientProtocol client) {
		client.sendRaw("$MaxedOut|");
	}
	
	public static void sendMaxedOut(ClientProtocol client,int position) {
		if (position < 0) {
			sendMaxedOut(client);
		} else {
			client.sendRaw("$MaxedOut "+position+"|");
		}
	}

}
