package uc.protocols.hub;

import java.io.IOException;

import uc.protocols.DCProtocol;

public class HubTopic extends AbstractNMDCHubProtocolCommand {

	
	
	public HubTopic(Hub hub) {
		super(hub);
	}

	@Override
	public void handle(String command) throws IOException {
		command = command.substring(command.indexOf(' '));
		String topic = DCProtocol.reverseReplaces(command);
		
		hub.setTopic(topic);
		
	}

}
