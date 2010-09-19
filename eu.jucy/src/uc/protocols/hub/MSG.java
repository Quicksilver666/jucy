package uc.protocols.hub;

import java.io.IOException;
import java.net.ProtocolException;
import java.util.Map;

import uc.IUser;
import uc.User;


/**
 * EMSG DCBA 7KJB \n..\s\s\s\s\s\shellos\s\s\s\s\s\s\s\s\s\
 * 
 * @author Quicksilver
 *
 */
public class MSG extends AbstractADCHubCommand {

	public MSG() {
		setPattern(getHeader()+" ("+ADCTEXT+") ?(.*)",true);
	}


	public void handle(Hub hub,String command) throws ProtocolException, IOException {
		User other = getOther(hub);
		String text = revReplaces(matcher.group(HeaderCapt+1));
		Map<Flag,String> attr = getFlagMap(matcher.group(HeaderCapt+2));
		
		boolean me = attr.containsKey(Flag.ME);
		
		if (attr.containsKey(Flag.PM)) { //TODO may be ME in pm should also be possible
			User originator = hub.getUserBySID(SIDToInt(attr.get(Flag.PM)));
			if (originator != null && originator != hub.getSelf()) { //ignore messages without originator and ignore messages from ourself
				hub.pmReceived(new PrivateMessage(originator,other , text,me));
			}
		} else {// else if(attr.containsKey(Flag.ME)) {
			hub.mcMessageReceived(other, text,me);
		}

	}
	
	public static void sendPM(Hub hub,IUser target,String message,boolean me) {
		User self = hub.getSelf();
	
		String send = "EMSG "+SIDToStr(self.getSid())+" "+SIDToStr(target.getSid())+
						" "+doReplaces(message)+" PM"+SIDToStr(self.getSid())+(me? " ME1" : "" );
	
		hub.sendUnmodifiedRaw(send+"\n");
	}
	
	public static void sendMM(Hub hub,String message,boolean me) {
		//BMSG ownSID msg
		User self = hub.getSelf();

		String send = "BMSG "+SIDToStr(self.getSid())+" "+doReplaces(message)+ (me? " ME1" : "" );
		
		hub.sendUnmodifiedRaw(send+"\n");
	}
	

}
