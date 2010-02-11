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

	public static final String DOWNLOAD = "Download" , UPLOAD = "Upload";
	public static void sendDirectionString(ClientProtocol client) throws IOException {
		client.sendUnmodifiedRaw(String.format("$Direction %s %d|", 
				(client.getFti().getDqe() != null ? DOWNLOAD : UPLOAD),
				client.getMyNumber() ));
//		
//		client.sendUnmodifiedRaw("$Direction " + (client.getFti().getDqe() != null ? DOWNLOAD : UPLOAD) + " "
//				+ client.getMyNumber() + "|"); 
	}
	
	public Direction(ClientProtocol client) {
		super(client);
		setPattern(prefix+" ((?:"+DOWNLOAD+")|(?:"+UPLOAD+")) ("+SHORT+")",true);
	}

	@Override
	public void handle(String command) throws IOException {
		client.setOthersNumber( Integer.parseInt(matcher.group(2)));	
		client.setDownload(matcher.group(1).equals(DOWNLOAD));
		
		client.increaseLoginLevel();
		client.removeCommand(this);
	}
	

	
}
