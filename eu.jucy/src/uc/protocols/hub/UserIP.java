package uc.protocols.hub;

import helpers.GH;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Set;
import java.util.regex.Pattern;

import logger.LoggerFactory;
import org.apache.log4j.Logger;

import uc.IUser;
import uc.IUserChangedListener.UserChange;
import uc.IUserChangedListener.UserChangeEvent;
import uc.user.User;

public class UserIP extends AbstractNMDCHubProtocolCommand {

	private static Logger logger = LoggerFactory.make();
	
	
	

	@Override
	public void handle(Hub hub,String command) throws IOException {
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
		Set<String> osup = hub.getOthersSupports();
		return osup.contains("UserIP") || osup.contains("UserIP2");
	}

}
