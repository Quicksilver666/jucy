package uc.protocols.hub;

import helpers.GH;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import logger.LoggerFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import uc.Command;
import uc.protocols.DCProtocol;


/**
 * 
 * * INFO  UserCommand.java Line:73 UserCommand not parseable: $UserCommand 1 3 • Tv-Geek • \Week listning\List shows in database$<%[mynick]> +sl&#124
 * 
 * example:
 * $UserCommand 1 2 Misc\ Drop user $$To: °^Goose^° From: %[mynick] $<%[mynick]> #drop %[nick]&#124;
 * $UserCommand 255 7  //delete all
 * $UserCommand 0 7    // add separator
 * 
 * first number: what:  255 = clear    , 0 = separator , 1 = usual command  , 2 = usual command restricted to single user
 * 
 * second number:
 * 1 = Hub - ignored?
 * 2 = User 
 * 4 = Search
 * 8 = FileList 
 * 
 * @author Quicksilver
 *
 */
public class UserCommand extends AbstractNMDCHubProtocolCommand {

	
	private static Logger logger = LoggerFactory.make();
	
	private final Pattern delete;
	
	private final Pattern normalCommand;
	
	private final Pattern separator;

	public UserCommand() {
		normalCommand = Pattern.compile( prefix +" ([12]) ("+SHORT+") ("+TEXT_NODOLLAR+")\\$([^|]*?)"+ESCAPED_PIPE+"?");
		delete = Pattern.compile(prefix + " 255 ("+SHORT+").*");
		separator = Pattern.compile(prefix +" 0 ("+SHORT+")("+TEXT+")");
	}

	@Override
	public void handle(Hub hub,String command) throws IOException {
		logger.debug("usercommand received: "+command);
		Matcher m = null;
		
		if ((m = normalCommand.matcher(command)).matches()) {
			boolean multi = Integer.parseInt(m.group(1)) == 1;
			int where = Integer.parseInt(m.group(2));
			String path = m.group(3);
			String com = m.group(4)+"&#124;";
			String toSend = DCProtocol.reverseReplaces(com);
			hub.addUserCommand(new Command(path,multi,where,toSend,hub.getFavHub().getHubaddy()));
		} else if ((m = delete.matcher(command)).matches() ) {
			int where = Integer.parseInt(m.group(1));
			hub.deleteUserCommands(where);
		} else if ((m = separator.matcher(command)).matches()) {
			int where = Integer.parseInt(m.group(1));
			String path = m.group(2);
			path = path.trim();
			Command com = hub.getLastUserCommand();
			if (GH.isEmpty(path) && com != null) {
				path = com.getParentPath()+"\\"; //  
				logger.debug("path of sep: "+path);
			}
			hub.addUserCommand(new Command(where,path,hub.getFavHub().getHubaddy()));
		} else {
			logger.log(hub.isOpHub()?Level.INFO:Level.DEBUG, "UserCommand not parseable: "+command);
		}
	}
	

//	/**
//	 * test for the pattern of UserCommand
//	 */
//	public static void main(String[] args) {
//		
//		String pm = "$UserCommand 1 3 • Tv-Geek • \\Week listning\\List shows in database$<%[mynick]> +sl&#124";//"$UserCommand 1 2 Misc\\ Drop user $$To: °^Goose^° From: %[mynick] $<%[mynick]> #drop %[nick]&#124;";
//		String x = "$UserCommand 255 3" ;
//		UserCommand uc = new UserCommand();
//		Matcher m = uc.normalCommand.matcher(pm);
//		Matcher m2 = uc.delete.matcher(x);
//		boolean matches = m.matches(), matches2 = m2.matches();
//		
//		System.out.println(matches+" "+matches2);
//		if (matches) {
//			System.out.println(m.group(1));
//			System.out.println(m.group(2));
//			System.out.println(m.group(3));
//			System.out.println(m.group(4));
//		}
//	}
	

}
