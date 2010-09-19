package uc.protocols.hub;

import java.io.IOException;
import java.util.Set;

import org.eclipse.core.runtime.Platform;


public class Supports extends AbstractNMDCHubProtocolCommand {

	public static final boolean useZLIB = Platform.inDevelopmentMode() && false;
	
	/**
	 * supports string for the NMDC protocol
	 */
	public static final String HUBSUPPORTS 
		= "$Supports UserCommand NoGetINFO NoHello UserIP2 TTHSearch Feed "+(useZLIB?"ZPipe0 ":"")+"|"; //(Platform.inDevelopmentMode()?"ZPipe0 ":"")
	
	


	@Override
	public void handle(Hub hub,String command) throws IOException {
		Set<String> supports = hub.getOthersSupports();		
		
		String[] com = command.split(" ");
		for (int i = 1; i < com.length ;i++) {
			supports.add(com[i]);
		}
		hub.removeCommand(this);
	}

	
	public static void sendSupports(Hub hub) { 
		hub.sendUnmodifiedRaw(HUBSUPPORTS);
	}
}
