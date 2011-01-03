package uc.protocols.hub;


import helpers.GH;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ProtocolException;
import java.util.Map;

import org.eclipse.core.runtime.Platform;


import uc.IUser;
import uc.protocols.ADCStatusMessage;



/**
 * ISTA 000 \n\s\s\s\s\s\s\s\s\s\s\s\s\s\s\s\s\s\s\s\ssome\stext\s\s\s\s\s\s\s\s\s\s\s\
 * 
Severity values:
0 	Success (used for confirming commands), error code must be "00", and an additional flag "FC" contains the FOURCC of the command being confirmed if applicable.
1 	Recoverable (error but no disconnect)
2 	Fatal (disconnect)

Error codes:
00 	Generic, show description
x0 	Same as 00, but categorized according to the rough structure set below
10 	Generic hub error
11 	Hub full
12 	Hub disabled
20 	Generic login/access error
21 	Nick invalid
22 	Nick taken
23 	Invalid password
24 	CID taken
25 	Access denied, flag "FC" is the FOURCC of the offending command. Sent when a user is not allowed to execute a particular command
26 	Registered users only
27 	Invalid PID supplied
30 	Kicks/bans/disconnects generic
31 	Permanently banned
32 	Temporarily banned, flag "TL" is an integer specifying the number of seconds left until it expires (This is used for kick as well…).
40 	Protocol error
41 	Transfer protocol unsupported, flag "TO" the token, flag "PR" the protocol string. The client receiving a CTM or RCM should send this if it doesn't support the C-C protocol.
42 	Direct connection failed, flag "TO" the token, flag "PR" the protocol string. The client receiving a CTM or RCM should send this if it tried but couldn't connect.
43 	Required INF field missing/bad, flag "FM" specifies missing field, "FB" specifies invalid field.
44 	Invalid state, flag "FC" the FOURCC of the offending command.
45 	Required feature missing, flag "FC" specifies the FOURCC of the missing feature.
46 	Invalid IP supplied in INF, flag "I4" or "I6" specifies the correct IP.
47 	No hash support overlap in SUP between client and hub.
50 	Client-client / file transfer error
51 	File not available
52 	File part not available
53 	Slots full
54 	No hash support overlap in SUP between clients. 
 * 
 * @author Quicksilver
 *
 */
public class STA extends AbstractADCHubCommand {

	public STA() {
		setPattern(getHeader()+" ([012])(\\d{2}) ("+ADCTEXT+") ?(.*)",true);
	}


	public void handle(Hub hub,String command) throws ProtocolException, IOException {
		logger.debug("STA received: "+command);
		if (GH.isEmpty(getOtherSID())) { //STA from a hub
			int severity = Integer.valueOf(matcher.group(HeaderCapt+1));
			int errorCode = Integer.valueOf(matcher.group(HeaderCapt+2));
			String message = revReplaces(matcher.group(HeaderCapt+3));
			
			Map<Flag,String> flags =  getFlagMap(matcher.group(HeaderCapt+4));
	
			if (!message.contains("SCH Invalid Context")) {  //workaround: Ignore damn invalid context messages by DSHub
				hub.statusMessage(message,severity);
			}
			
			
			
			ADCStatusMessage staMessage = new ADCStatusMessage(message,severity,errorCode,flags);
			String sendMessage = staMessage.getTypeMessage();
			
			switch(errorCode) {
			case 32: //Temp banned for some time
				long waitTime = Long.valueOf(flags.get(Flag.TL));
				hub.reconnect((int)waitTime);
				break;
			case 41:
				logger.debug("other does not support protocol");
				//TODO make sure to use other protocol both support..
				//maybe not send message to status?
				break;
			case 46: // Invalid IP
				InetAddress ip = null;
				if (flags.containsKey(Flag.I4)) {
					ip = InetAddress.getByName(flags.get(Flag.I4));
				} else if (flags.containsKey(Flag.I6)) {
					ip = InetAddress.getByName(flags.get(Flag.I6));
				}
				if (ip != null) {
					hub.getSelf().setIp(ip);
				}
				break;
			}
			if (sendMessage != null) {
				hub.statusMessage(sendMessage,severity);
			}
		} else {
			//TODO handle STA from a client
			
		}
	}
	
	
	public static void sendSTAtoHub(Hub hub,ADCStatusMessage sm) {
		hub.sendUnmodifiedRaw("HSTA "+sm.toADCString()+"\n");
		if (sm.getSeverity() == ADCStatusMessage.FATAL) {
			hub.reconnect(120);
		}
	}
	
	public static void sendSTAtoUser(Hub hub,IUser target,ADCStatusMessage sm) {
		hub.sendUnmodifiedRaw("DSTA "+SIDToStr(hub.getSelf().getSid())+" "+SIDToStr(target.getSid())+" "
				+sm.toADCString()+"\n");
		if (Platform.inDevelopmentMode()) {
			logger.warn(sm.toString()+"  "+target+"  sev:"+sm.getSeverity(),new Throwable());
		}
	}
	

}
