package uc.protocols.hub;

import java.io.IOException;
import java.net.ProtocolException;
import java.util.HashMap;


import uc.IUser;
import uc.protocols.ADCStatusMessage;
import uc.protocols.CPType;

public class RCM extends AbstractADCHubCommand {

	
	/**
	 * 
	 * @param hub
	 */
	public RCM(Hub hub) {
		super(hub);		//         protocol      token
		setPattern(getHeader()+" ("+ADCTEXT+") ("+ADCTEXT+")",true);
	}


	public void handle(String command) throws ProtocolException, IOException {
		//DSTA for bad RCM with unsupported protocol
		// otherwise RCM ..
		IUser sender =  getOther();
		String protocol = revReplaces(matcher.group(HeaderCapt+1));
		String token = revReplaces(matcher.group(HeaderCapt+2));
		
		
		if (sender != null ) {
			if (!hub.getDcc().isActive()) {
				// -> send error not active
				ADCStatusMessage adcsm = new ADCStatusMessage("Client also not active",
						ADCStatusMessage.RECOVERABLE,
						ADCStatusMessage.ProtocolGeneric);
				
				STA.sendSTAtoUser(hub, sender, adcsm);
			} else if (CTM.SUPP.contains(protocol)) {
				hub.sendCTM(sender,CPType.fromString(protocol),token);
			} else {
				HashMap<Flag,String> flags = new HashMap<Flag,String>();
				flags.put(Flag.TO, token);
				flags.put(Flag.PR, protocol);
				
				ADCStatusMessage adcsm = new ADCStatusMessage("Protocol Unsupported",
						ADCStatusMessage.RECOVERABLE,
						ADCStatusMessage.ProtocolTransferProtocolUnsupported,
						flags);
				
				STA.sendSTAtoUser(hub, sender, adcsm);
			}	
		}
	}
	
	public static void sendRCM(Hub hub,IUser target,CPType protocol,String token) {
		IUser self = hub.getSelf();
		String rcm = "DRCM "+SIDToStr(self.getSid())+" "+SIDToStr(target.getSid())
					+" "+doReplaces(protocol.toString())
					+" "+doReplaces(token)+"\n";
		hub.sendUnmodifiedRaw(rcm);
		
		logger.debug("sending rcm: "+rcm);
	}

}
