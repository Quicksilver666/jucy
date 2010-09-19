package uc.protocols.hub;

import java.io.IOException;

import uc.User;

public class Quit extends AbstractNMDCHubProtocolCommand {


	@Override
	public void handle(Hub hub,String command) throws IOException {
		String nick = command.split(" ",2)[1].trim();
		User usr  = hub.getUserByNick(nick);
		if (usr != null) {
			hub.userQuit(usr);
		}
	}

}
