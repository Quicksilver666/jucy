package uc.protocols.hub;

import java.io.IOException;
import java.net.ProtocolException;
import java.util.Set;

import uc.protocols.ADCStatusMessage;

public class SUP extends AbstractADCHubCommand {	
	
	
	public static final String SUPPORTS = "HSUP ADBASE ADTIGR ADUCM0 ADBLO0\n";  //ADUCM0  for usercommands  ADADCS
	
	public SUP(Hub hub) {
		super(hub);
	}


	public void handle(String command) throws ProtocolException, IOException {
		Set<String> supps = hub.getOthersSupports();
		
		String[] com = space.split(command);
		for (int i=1; i < com.length; i++ ) {
			if (com[i].startsWith("AD")) {
				supps.add(com[i].substring(2));
			} else if (com[i].startsWith("RM")) {
				supps.remove(com[i].substring(2));
			}
		}
		if (!supps.contains("BASE")) {
			STA.sendSTAtoHub(hub, new ADCStatusMessage("BASE not supported", ADCStatusMessage.FATAL, ADCStatusMessage.ProtocolRequiredFeatureMissing));
		}
	}
	
	public static void sendSupports(Hub hub) {
		hub.sendUnmodifiedRaw(SUPPORTS);  
		
	}

}
