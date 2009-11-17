package uc.protocols.hub;

import java.io.IOException;

import eu.jucy.language.LanguageKeys;


public class HubIsFull extends AbstractNMDCHubProtocolCommand {

	public HubIsFull(Hub hub) {
		super(hub);
	}

	@Override
	public void handle(String command) throws IOException {
		hub.statusMessage(LanguageKeys.STA11HubFull,0 );
	}

	@Override
	public boolean matches(String command) {
		//don`t check for the existence of a space char
		return command.startsWith(getPrefix()); 
	}
}
