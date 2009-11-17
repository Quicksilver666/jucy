package uc.protocols.hub;

import helpers.GH;

import java.io.IOException;
import java.util.regex.Pattern;


import uc.User;
import uc.crypto.HashValue;
import uc.protocols.DCProtocol;

public class NickList extends AbstractNMDCHubProtocolCommand {

	
	
	
	
	public NickList(Hub hub) {
		super(hub);
	}

	@Override
	public void handle(String command) throws IOException {
		String[] b = command.split(" ", 2)[1].split(Pattern.quote("$$"));//cut away prefix.. and separate to nicks
		
		boolean sendGetinfo = !hub.getOthersSupports().contains("NoGetINFO");
		for (String nick : b) {
			nick = nick.trim();
			if (!GH.isEmpty(nick)) {
				HashValue userid = DCProtocol.nickToUserID(nick, hub);
				User usr = hub.getUser(userid);
				if (usr == null) {
					usr = hub.getDcc().getPopulation().get(nick, userid);
					if (sendGetinfo) {
						hub.sendUnmodifiedRaw("$GetINFO "+usr.getNick()+" "+hub.getSelf().getNick()+"|");
					}
					hub.insertUser(usr);
				}
			}

		}

	}

}
