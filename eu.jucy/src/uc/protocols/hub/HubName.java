package uc.protocols.hub;

import java.io.IOException;

import uc.protocols.DCProtocol;

public class HubName extends AbstractNMDCHubProtocolCommand {

	private static final String SEPERATOR = " - ";
	public HubName(Hub hub) {
		super(hub);
	}

	@Override
	public void handle(String com) throws IOException {
		String command = com.substring(com.indexOf(' '));
		command = DCProtocol.reverseReplaces(command);
		int i = command.indexOf(SEPERATOR);
		if (i == -1) {
			hub.setHubname(command.trim());
			hub.setTopic("");
		} else {
			hub.setHubname(command.substring(0,i).trim());
			hub.setTopic(command.substring(i+SEPERATOR.length()).trim());
		}
		

	}

}
