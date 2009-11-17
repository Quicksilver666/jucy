package uc.protocols.hub;

import java.io.IOException;

import uc.User;

public class Quit extends AbstractNMDCHubProtocolCommand {

	public Quit(Hub hub) {
		super(hub);
	}

	@Override
	public void handle(String command) throws IOException {
		String nick = command.split(" ",2)[1].trim();
		User usr  = hub.getUserByNick(nick);
		if (usr != null) {
			hub.userQuit(usr);
		}
	}

}
