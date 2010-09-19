package uc.protocols.hub;

import java.io.IOException;

import uc.protocols.DCProtocol;

public class HubName extends AbstractNMDCHubProtocolCommand {

	private static final String SEPARATOR = " - ";
	public HubName() {
	}

	@Override
	public void handle(Hub hub,String com) throws IOException {
		String command = com.substring(com.indexOf(' '));
		command = DCProtocol.reverseReplaces(command);
		int i = command.indexOf(SEPARATOR);
		if (i == -1) {
			hub.setHubname(command.trim());
			hub.setTopic("");
		} else {
			hub.setHubname(command.substring(0,i).trim());
			hub.setTopic(command.substring(i+SEPARATOR.length()).trim());
		}
	}

}
