package uc.protocols.hub;

import helpers.GH;

import java.io.IOException;
import java.net.ProtocolException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import uc.protocols.ADCStatusMessage;

public class SUP extends AbstractADCHubCommand {	
	
	//"+(Hub.ZLIF?" ADZLIF":"")+ "
	//public static final String SUPPORTS = "HSUP ADBASE ADTIGR ADUCMD ADBLOM\n";  // ADUCM0  for usercommands  ADADCS
	
	private static final String[] SUPPORTS = new String[] {"TIGR","BLOM"};

	public void handle(Hub hub,String command) throws ProtocolException, IOException {
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
		List<String> supports = hub.getSupports(true);
		supports.addAll(Arrays.asList(SUPPORTS));
		Collections.sort(supports);
		hub.sendUnmodifiedRaw(String.format("HSUP AD%s\n",GH.concat(supports, " AD")));  
	}

}
