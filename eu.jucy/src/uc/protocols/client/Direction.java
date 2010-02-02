package uc.protocols.client;

import java.io.IOException;

/**
 * 
 * $Direction <Download/Upload> number|
 * 
 * @author Quicksilver
 *
 */
public class Direction extends AbstractNMDCClientProtocolCommand {

	public static void sendDirectionString(ClientProtocol client) throws IOException {
		client.sendUnmodifiedRaw("$Direction " + (client.getFti().getDqe() != null ? "Download" : "Upload") + " "
				+ client.getMyNumber() + "|"); //here manipulating the n
	}
	
	public Direction(ClientProtocol client) {
		super(client);
		setPattern(prefix+" ((?:Download)|(?:Upload)) ("+SHORT+")",true);
	}

	@Override
	public void handle(String command) throws IOException {
		client.setOthersNumber( Integer.parseInt(matcher.group(2)));	
		client.setDownload(matcher.group(1).equals("Download"));
		
		client.increaseLoginLevel();
		client.removeCommand(this);
	}
	

	
}
