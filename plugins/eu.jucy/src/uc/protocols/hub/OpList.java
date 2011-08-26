package uc.protocols.hub;

import helpers.GH;

import java.io.IOException;
import java.util.regex.Pattern;

import logger.LoggerFactory;


import org.apache.log4j.Logger;


import uc.crypto.HashValue;
import uc.protocols.DCProtocol;
import uc.user.User;

public class OpList extends AbstractNMDCHubProtocolCommand {

	private static Logger logger = LoggerFactory.make();
	


	/**
	 * parses a oplist..
	 * $OpList nick $$ nick2 $$ nick3 | ..usw
	 */
	@Override
	public void handle(Hub hub,String command) throws IOException {
		String[] b = command.split(" ", 2)[1].split(Pattern.quote("$$"));//cut away prefix.. and separate to nicks
		if (b.length > Hub.MAX_USERS) {
			logger.warn("oplist too long: "+b.length);
			return;
		}
		logger.debug("Oplist: "+b.length);
		
		for (String nick : b) {
			nick = nick.trim();
			if (!GH.isEmpty(nick)) {
				HashValue userid = DCProtocol.nickToUserID(nick, hub);
				User usr = hub.getUser(userid);
				if (usr == null) {
					usr = hub.getDcc().getPopulation().get(nick, userid);
					usr.setProperty(INFField.CT, Byte.toString(User.CT_OPERATOR));
					hub.insertUser(usr);
				} else {
					usr.setOp(true);
				}
			}

		}
	}

}
