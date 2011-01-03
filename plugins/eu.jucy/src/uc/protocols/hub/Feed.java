package uc.protocols.hub;

import java.io.IOException;

import uc.protocols.DCProtocol;


public class Feed extends AbstractNMDCHubProtocolCommand {

	public Feed() {
	}

	@Override
	public void handle(Hub hub,String command) throws IOException {
		String[] mes = command.split(" ",3);
		if (mes.length == 3) {
			FeedType f = FeedType.fromString(mes[1]);
			String message = DCProtocol.reverseReplaces(mes[2]);
			if (f == FeedType.NONE) {
				message = "["+mes[1]+"] "+message;
			}
			
			hub.feedReceived(f, message);		
		}

	}

}
