package uc.protocols.client;

import java.io.IOException;


import uc.DCClient;
import uc.IUser;


public class MyNick extends AbstractNMDCClientProtocolCommand {

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
		client.sendRaw("$MyNick " + client.getSelf().getNick()
				+ "|$Lock EXTENDEDPROTOCOLABCABCABCABCABCABC Pk="
				+ DCClient.LONGVERSION.replace(" ", "_")+ "|");
		client.addCommand(new Key(client));
		client.addCommand(new Direction(client));
		client.addCommand(new Supports(client));
		
	}

}
