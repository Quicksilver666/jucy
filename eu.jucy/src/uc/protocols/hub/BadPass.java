package uc.protocols.hub;

import java.io.IOException;

import eu.jucy.language.LanguageKeys;

public class BadPass extends AbstractNMDCHubProtocolCommand {

	public BadPass(Hub hub) {
		super(hub);
	}

	@Override
	public void handle(String command) throws IOException {
		hub.reconnect(-1); //no reconnect..
		hub.statusMessage(LanguageKeys.STA23InvalidPassword,0);
	}

	@Override
	public boolean matches(String command) {
		//don`t check for the existence of a space char
		return command.startsWith(getPrefix()); 
	}


	
	
}
