package uc.protocols.hub;

import helpers.GH;

import java.io.IOException;
import java.util.regex.Pattern;

import uc.User;
import uc.crypto.HashValue;
import uc.listener.IUserChangedListener.UserChange;
import uc.listener.IUserChangedListener.UserChangeEvent;
import uc.protocols.DCProtocol;

public class OpList extends AbstractNMDCHubProtocolCommand {

	public OpList(Hub hub) {
		super(hub);
	}

	/**
	 * parses a oplist..
	 * $OpList nick $$ nick2 $$ nick3 | ..usw
	 */
	@Override
	public void handle(String command) throws IOException {
		String[] b = command.split(" ", 2)[1].split(Pattern.quote("$$"));//cut away prefix.. and separate to nicks
		for (String nick : b) {
			nick = nick.trim();
			if (!GH.isEmpty(nick)) {
				boolean connected = false;
				HashValue userid = DCProtocol.nickToUserID(nick, hub);
				User usr = hub.getUser(userid);
				if (usr == null) {
					connected = true;
					usr = hub.getDcc().getPopulation().get(nick, userid);
				}
				
				usr.setOp(true);
				if (connected) {
					hub.insertUser(usr);
				} else {
					usr.notifyUserChanged(UserChange.CHANGED,UserChangeEvent.INF);
				}
			}

		}
	}

}
