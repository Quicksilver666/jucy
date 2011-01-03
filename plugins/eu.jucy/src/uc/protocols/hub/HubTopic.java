package uc.protocols.hub;

import java.io.IOException;

import uc.protocols.DCProtocol;

public class HubTopic extends AbstractNMDCHubProtocolCommand {

	
	
	public HubTopic() {
	}

	@Override
	public void handle(Hub hub,String command) throws IOException {
		command = command.substring(command.indexOf(' '));
		String topic = DCProtocol.reverseReplaces(command);
		
		hub.setTopic(topic);
		
	}

}
