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

	public MSG(Hub hub) {
		super(hub);
		setPattern(getHeader()+" ("+ADCTEXT+") ?(.*)",true);
	}


	public void handle(String command) throws ProtocolException, IOException {
		User other = getOther();
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
	//	} else {
	//		hub.mcMessageReceived(other, text,false);
	//	}
		
		/*if (matcher.groupCount() == 5) { // PM
			User originator = hub.getUserBySID( SIDToInt(matcher.group(5)));
			hub.pmReceived(originator,other , text);
		} else if (matcher.groupCount() == 4) { //ME1 msg
			hub.mcMessageReceived(other, text,true);
		} else if (matcher.groupCount() == 3) { //4  MCM
			hub.mcMessageReceived(other, text,false);
		} */
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
