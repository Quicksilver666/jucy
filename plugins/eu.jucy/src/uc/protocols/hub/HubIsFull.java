package uc.protocols.hub;

import java.io.IOException;

import uc.LanguageKeys;



public class HubIsFull extends AbstractNMDCHubProtocolCommand {

	public HubIsFull() {}

	@Override
	public void handle(Hub hub,String command) throws IOException {
		hub.statusMessage(LanguageKeys.STA11HubFull,0 );
	}

	@Override
	public boolean matches(String command) {
		//don`t check for the existence of a space char
		return command.startsWith(getPrefix()); 
	}
}
