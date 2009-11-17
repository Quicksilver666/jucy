package uc.protocols.hub;

import java.io.IOException;
import java.net.ProtocolException;
import java.util.Map;


import logger.LoggerFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import uc.User;


/**
 * 
QUI sid

Contexts: F

States: IDENTIFY, VERIFY, NORMAL

The client identified by <sid> disconnected from the hub. If the SID belongs 
to the client receiving the QUI, it means that it should take action according 
to the reason (i.e. redirect or not reconnect in case of ban). The hub must not 
send data after the QUI to the client being disconnected.

The following flags may be present:
ID 	SID of the initiator of the disconnect (for example the one that issued a kick).
TL 	Time Left until reconnect is allowed, in seconds. -1 = forever.
MS 	Message.
RD 	Redirect server URL.
DI 	Any client that has this flag in the QUI message should have its 
transfers terminated by other clients connected to it, as it is unwanted in the system. 
 * 
 * ADC Quit command..
 * also does force move ...
 * @author Quicksilver
 *
 */
public class QUI extends AbstractADCHubCommand {

	private static Logger logger = LoggerFactory.make();
	
	static {
		logger.setLevel( Level.INFO);
	}
	
	public QUI(Hub hub) {
		super(hub);
		setPattern(prefix+" ("+SID+") ?(.*)",true);
	}


	public void handle(String command) throws ProtocolException, IOException {
		logger.debug("QUI: "+command);
		
		int sid =  SIDToInt(matcher.group(1)); // getOthersSID();
		if (hub.getSelf().getSid() == sid) {
			Map<Flag,String> flags = getFlagMap(matcher.group(2));
			
			if (flags.containsKey(Flag.MS)) {
				User other = null;
				if (flags.containsKey(Flag.ID)) {
					int osid = SIDToInt(flags.get(Flag.ID));
					other = hub.getUserBySID(osid);
				}
				
				hub.statusMessage((other == null? "": other.getNick()+": ")+ flags.get(Flag.MS),0);
			}
			
			if (flags.containsKey(Flag.RD)) { //A redirect for us
				hub.redirectReceived(flags.get(Flag.RD));  
			} else if (flags.containsKey(Flag.TL)) { // a kick? 
				int reconnectTime = Integer.valueOf(flags.get(Flag.TL));
				hub.reconnect(reconnectTime);
			}
			
		} else {
			User usr = hub.getUserBySID(sid);
			if (usr != null) {
				hub.userQuit(usr);  //"DI" flag is ignored as we always disconnect transfers
			}
		}
	}

}
