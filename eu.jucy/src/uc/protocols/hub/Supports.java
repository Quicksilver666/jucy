package uc.protocols.hub;

import helpers.GH;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;



public class Supports extends AbstractNMDCHubProtocolCommand {

	
//	/**
//	 * supports string for the NMDC protocol
//	 */
//	public static final String HUBSUPPORTS 
//		= "$Supports UserCommand NoGetINFO NoHello UserIP2 TTHSearch Feed |"; //(Platform.inDevelopmentMode()?"ZPipe0 ":"")
//	
	
	private static final String[] SUPPORTS = new String[] {
		//	"UserCommand"
			"NoGetINFO"
			,"NoHello"
		//	,"UserIP2"
			,"TTHSearch"
		//	,"Feed"
	};
	


	@Override
	public void handle(Hub hub,String command) throws IOException {
		Set<String> supports = hub.getOthersSupports();		
		
		String[] com = command.split(" ");
		for (int i = 1; i < com.length ;i++) {
			supports.add(com[i]);
		}
		hub.removeCommand(this);
	}

	
	public static String getSupports(Hub hub) { 
		List<String> supports = hub.getSupports(true);
		supports.addAll(Arrays.asList(SUPPORTS));
		Collections.sort(supports);
		return String.format("$Supports %s |", GH.concat(supports, " "));
	}
}
