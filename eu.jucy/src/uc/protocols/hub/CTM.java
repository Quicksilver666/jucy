package uc.protocols.hub;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


import uc.IUser;
import uc.protocols.ADCStatusMessage;
import uc.protocols.CPType;

public class CTM extends AbstractADCHubCommand {

	
	/**
	 * supported protocols
	 */
	public static final List<String> SUPP = Arrays.asList(CPType.ADC.toString(),CPType.ADCS.toString());
	
	public CTM(Hub hub) {
		super(hub);              //   protocol    port       token
		setPattern(getHeader()+" ("+ADCTEXT+") ("+PORT+") ("+ADCTEXT+")",true);
	}

	//DCTM UVTR WU5G ADCS/0.10 54892 2046781604
	public void handle(String command) throws ProtocolException, IOException {
		IUser other = getOther();
		if (other == null) {
			return;
		}
		String protocol = revReplaces(matcher.group(HeaderCapt+1));
		InetSocketAddress isa = null;
		if (other.getIp() != null) {
			isa = new InetSocketAddress(other.getIp(),Integer.valueOf(matcher.group(HeaderCapt+2)));
		}
		
		logger.debug("Received ctm: "+command);
		
		String token = revReplaces(matcher.group(HeaderCapt+3));
		
		if (!SUPP.contains(protocol)) {
			HashMap<Flag,String> flags = new HashMap<Flag,String>();
			flags.put(Flag.TO, token);
			flags.put(Flag.PR, protocol);
			ADCStatusMessage adcsm= new ADCStatusMessage("Protocol Unsupported "+protocol,
					ADCStatusMessage.RECOVERABLE,
					ADCStatusMessage.ProtocolTransferProtocolUnsupported,flags);
			STA.sendSTAtoUser(hub, other,adcsm);
			logger.info("command: "+command);
		} else if (isa != null) {
			hub.ctmReceived(isa,other,CPType.fromString(protocol), token);
		}	
	}
	

	public static void sendCTM(Hub hub,IUser target,CPType protocol,String token) {
		//protocols  ADC/1.0  or  ADCS/0.10
		IUser self = hub.getSelf();
		String ctm = "DCTM "+SIDToStr(self.getSid())+" "+SIDToStr(target.getSid())
					+" "+doReplaces(protocol.toString())+" "+hub.getDcc().getCh().getPort(protocol.isEncrypted())
					+" "+doReplaces(token)+"\n";
		hub.sendUnmodifiedRaw(ctm);
		logger.debug("Sending ctm: "+ctm);
	}

}
