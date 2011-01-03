package uc.protocols.hub;

import java.io.IOException;

import uc.LanguageKeys;




public class ValidateDenide extends AbstractNMDCHubProtocolCommand {



	@Override
	public void handle(Hub hub,String command) throws IOException {
		
		hub.statusMessage(LanguageKeys.STA22NickTaken+": "+command.split(" ",2)[1].trim(),0 );
				
		//		"Hub says: Access denied, nick \""
		//		+ command.split(" ",2)[1].trim()
		//		+ "\" in use!" );
	}
	


}
