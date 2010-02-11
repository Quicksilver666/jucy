package uc.protocols.client;

import java.io.IOException;

import logger.LoggerFactory;

import org.apache.log4j.Logger;


import uc.DCClient;
import uc.FavHub;
import uc.IUser;


public class MyNick extends AbstractNMDCClientProtocolCommand {

	private static final Logger logger = LoggerFactory.make();
	
	public MyNick(ClientProtocol client) {
		super(client);
		setPattern(prefix+" ("+NICK+")",true);
	}

	
	@Override
	public void handle(String command) throws IOException {
		client.setProtocolNMDC(true);
		String othersnick = matcher.group(1);
		IUser other = null;
		if (client.isIncoming()) {
			
			other = getCH().getUserExpectedToConnect(othersnick,client.getOtherip());
			if (other != null) {
				client.otherIdentified(other);
				sendMyNickAndLock(client);
			} 

		} else {
			other = client.getSelf().getHub().getUserByNick(othersnick);
			if (other != null) {
				client.otherIdentified(other);
			}
		}

		//if other can't be resolved.. send error..
		if (other == null) {
			client.sendError(DisconnectReason.NICKUNKNOWN);
		}
		client.removeCommand(this);

	}
	
	/**
	 * if the connection is outgoing
	 * we send our nick and a Lock on connect..
	 * otherwise we send it after receiving MyNick
	 */
	public static void sendMyNickAndLock(ClientProtocol client) {
		client.addCommand(new Key(client));
		client.addCommand(new Direction(client));
		client.addCommand(new Supports(client));
		
		String myNickAndLock="$MyNick " + client.getSelf().getNick()
		+ "|$Lock EXTENDEDPROTOCOLABCABCABCABCABCABC Pk="
		+ DCClient.LONGVERSION.replace(' ', '_');
		
		if (!client.isIncoming()) {
			FavHub fh = client.getSelf().getHub().getFavHub();
			String s = fh.getSimpleHubaddy();
			myNickAndLock += "Ref=" + s.replace(' ', '_');
		}
		
		myNickAndLock+="|";
		
		logger.debug(myNickAndLock);
		client.sendUnmodifiedRaw(myNickAndLock);
		
			

	

		
	}

}
