package uc.protocols.hub;

import helpers.GH;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import logger.LoggerFactory;

import org.apache.log4j.Logger;

import uc.User;
import uc.IUser.AwayMode;
import uc.IUser.Mode;
import uc.IUserChangedListener.UserChange;
import uc.IUserChangedListener.UserChangeEvent;
import uc.crypto.HashValue;
import uc.protocols.DCProtocol;
import uc.protocols.SendContext;

/**
 * $MyINFO $ALL -[`°v».......((Op-Chat)).......«v°`] •Operator & Admin Chat•$ $$$$ 
 * ERROR ? Line:? 		 java.lang.ArrayIndexOutOfBoundsException: 3
 * 
 * $MyINFO $ALL <nick> <description>$ $<connection><flag>$<e-mail>$<sharesize>$|
 * 
 * <nick> Nickname // without spaces
• <description> User description: user defined text field. DC++ has added an automated (and forced) "tag" to the description field. See below.
• <connection> User connection to the internet.
• Default NMDC1 connections types 28.8Kbps, 33.6Kbps, 56Kbps, Satellite, ISDN, DSL, Cable, LAN(T1), LAN(T3)
• Default NMDC2 connections types Modem, DSL, Cable, Satellite, LAN(T1), LAN(T3)
• <flag> User status as ascii char (byte)
• Values:
• 1 normal
• 2, 3 away
• 4, 5 server
• 6, 7 server away
• 8, 9 fireball
• 10, 11 fireball away

• <e-mail> User email adress
• <sharesize> Share size in bytes 

 * 
 * @author Quicksilver
 *
 */

public class MyINFO extends AbstractNMDCHubProtocolCommand {

	private static Logger logger = LoggerFactory.make();
	
	private static final Pattern FLOATNUMBER = Pattern.compile("\\d+\\.?\\d*");
	
	private final Pattern description = Pattern.compile("^<([^,<]*).*,M\\:([AP5]),H:(\\d+)/(\\d+)/(\\d+)\\,S:(\\d+).*$");
	
	
	private static final Pattern desc = Pattern.compile("("+TEXT_NODOLLAR+")(<"+TEXT_NODOLLAR+">)");
	
	
	private static final Pattern myinfo = Pattern.compile(
			"\\$MyINFO \\$ALL ("+NMDCNICK+") ("+TEXT_NODOLLAR+")\\$.\\$("+TEXT_NODOLLAR+
			")(.)\\$("+TEXT_NODOLLAR+")\\$("+FILESIZE+")\\$",Pattern.DOTALL); //DOTALL needed as flag might be newline value
	
	private static Map<Hub,String> lastSent = Collections.synchronizedMap(new WeakHashMap<Hub,String>());  
	


	@Override
	public void handle(Hub hub,String command) throws IOException {
		//logger.debug("foundMyINFO: "+command+"  "+hub.getHubname());
		boolean connected = false;
		User current;
	
		Matcher m = myinfo.matcher(command);
		if (m.matches()) {
			String nick = m.group(1);
			HashValue userid = DCProtocol.nickToUserID(nick,hub );
			current = hub.getUser(userid);  // look if the user is known
			if (current == null) { 
				connected = true;
				current = hub.getDcc().getPopulation().get(nick, userid);  
			} 
			
			String description = m.group(2);
			String userDescription;
			String tag;
			Matcher descriptionMatcher = desc.matcher(description); 
			if (descriptionMatcher.matches()) {
				userDescription = descriptionMatcher.group(1);
				tag = descriptionMatcher.group(2);
			} else {
				userDescription = description; 
				tag = "";
			}
		//	current.setDescription(userDescription);
			current.setProperty(INFField.DE,userDescription);
					
			current.setTag(tag);
			
			String connection = m.group(3);
			if (FLOATNUMBER.matcher(connection).matches()) {
				current.setProperty(INFField.US, ""+((long)(Float.parseFloat(connection)*1000000L/8 ))) ;
			} else {
				current.setConnection(connection.intern());
			}
			
			byte flag = m.group(4).getBytes()[0]; 
			current.setFlag(flag);
			current.setProperty(INFField.AW, AwayMode.parseFlag(flag).getVal());


			current.setProperty(INFField.EM, m.group(5));
			current.setProperty(INFField.SS, m.group(6));
		}  else {
			logger.debug("MyINFO not matched: "+command); 
			String[] a = command.split(" ",3)[2].split(Pattern.quote("$"));
			
			String[] nickAndDescription = a[0].split(" ",2);
			String nick	= nickAndDescription[0].trim();
		     
		        
			HashValue userid= DCProtocol.nickToUserID(nick,hub ); // calc userid
			current = hub.getUser(userid);  // look if the user is known
			if (current == null) { 
				connected = true;
				current= hub.getDcc().getPopulation().get(nick, userid);  
			}
		  
			if (nickAndDescription.length > 1) {
				int split= nickAndDescription[1].lastIndexOf('<');
				if (split == -1) {
					current.setProperty(INFField.DE,nickAndDescription[1]);
					current.setTag("");
				} else {
					current.setProperty(INFField.DE,nickAndDescription[1].substring(0, split));
					current.setTag(nickAndDescription[1].substring(split));
				}
			}
		        	
	
			if (a.length > 2 && !GH.isEmpty(a[2])) {
				current.setConnection(  a[2].substring( 0,(a[2].length()-1) ).intern() );
				byte flag = a[2].substring( (a[2].length()-1) ).getBytes()[0];
				current.setFlag(flag);
				current.setProperty(INFField.AW, AwayMode.parseFlag(flag).getVal());
			}
		        // a[3]
			if (a.length > 3) {
				current.setProperty(INFField.EM, a[3].trim());
			}
		        //a[4]
			if (a.length >= 5) {
				String shared = a[4].trim();
				if (GH.isEmpty(shared) || !shared.matches(FILESIZE)) {
					shared = "0";
				}	  
				current.setProperty(INFField.SS, shared);
			} else {
				current.setProperty(INFField.SS, "0");
			}
		}
		
		String tag = current.getTag();
		Matcher my = description.matcher(tag);

		if (my.matches()) {
			current.setProperty(INFField.VE,my.group(1));
			current.setModechar(Mode.fromModeChar(my.group(2).charAt(0)));
			current.setProperty(INFField.HN, my.group(3));
			current.setProperty(INFField.HR, my.group(4));
			current.setProperty(INFField.HO, my.group(5));
			current.setProperty(INFField.SL, my.group(6));
			

		} else {
			current.setModechar(Mode.ACTIVE); // set active so connects can still happen if we are passive..
		}
		
		if (connected) {
			hub.insertUser(current);
		} else {
			current.notifyUserChanged(UserChange.CHANGED,UserChangeEvent.INF);
		}
	}

	@Override
	public boolean matches(String command) {
		return command.startsWith("$MyINFO $ALL ");
	}
	
	
	
	
	public static void sendMyINFO(Hub hub, boolean force) {
		SendContext sc = new SendContext();
		sc.setHub(hub);
		long speed = hub.getSelf().getUs();
		String speeds = toBperStoMBit(speed); //String.format("%5.3f", speed).trim();
		
		String unformatted ="$MyINFO $ALL %[myNI] %[myDE] %[myTAG]$ $" 
					+ speeds+ ((char)hub.getSelf().getFlag())+"$" 
					+ "%[myEM]$%[mySS]$|";
		String message = sc.format(unformatted);
		if (force || !message.equals(lastSent.get(hub))) {
			lastSent.put(hub, message);
			hub.sendRaw(message);
		}
	//	logger.debug("foundMyINFO: "+message+"  "+hub.getHubname());
	}
	
	private static String toBperStoMBit(long speed) {
		double speedM = speed * 8 / (1000d*1000d);
		if (speedM >= 10) {
			return  Integer.toString((int)speedM);
		} else {
			int thousandTimes = (int)(speedM * 1000);
			String s = String.format("%d.%03d",thousandTimes/1000 ,thousandTimes%1000);
			return s;
		}
	}
	
}
