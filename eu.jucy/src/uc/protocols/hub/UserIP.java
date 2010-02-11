package uc.protocols.hub;

import helpers.GH;

import java.io.IOException;
import java.net.InetAddress;
import java.util.regex.Pattern;

import logger.LoggerFactory;
import org.apache.log4j.Logger;

import uc.IUser;
import uc.User;
import uc.listener.IUserChangedListener.UserChange;
import uc.listener.IUserChangedListener.UserChangeEvent;

public class UserIP extends AbstractNMDCHubProtocolCommand {

	private static Logger logger = LoggerFactory.make();
	
	
	public UserIP(Hub hub) {
		super(hub);
	}

	@Override
	public void handle(String command) throws IOException {
		logger.debug("USERIP: "+command);
		String[] b = command.split(" ", 2)[1].split(Pattern.quote("$$"));
		for (String userIPString : b) {
			userIPString = userIPString.trim();
			if (!GH.isEmpty(userIPString)) {
				String a[] = userIPString.split(" ");
				String nick = a[0];
				User usr;
				if (nick.equals(hub.getSelf().getNick())) {
					usr = hub.getSelf();
				} else {
					usr = hub.getUserByNick(nick);
				}
				
				if (usr != null && a.length > 1) {
					usr.setIp(InetAddress.getByName(a[1].trim()));
					usr.notifyUserChanged(UserChange.CHANGED,UserChangeEvent.INF);
				}
			}
		}
	}
	
	public static void sendUserIPRequest(Hub hub,IUser usr) {
		if (supportsUserIp(hub)) {
			String userip = "$UserIP "+usr.getNick()+"|";
			hub.sendUnmodifiedRaw(userip);
		}
	}
	
	public static boolean supportsUserIp(Hub hub) {
		return hub.getOthersSupports().contains("UserIP") || hub.getOthersSupports().contains("UserIP2");
	}

}
