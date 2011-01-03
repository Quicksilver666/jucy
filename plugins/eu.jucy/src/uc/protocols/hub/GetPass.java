package uc.protocols.hub;

import java.io.IOException;

import uc.protocols.DCProtocol;

public class GetPass extends AbstractNMDCHubProtocolCommand {

	public GetPass() {
	}

	@Override
	public void handle(Hub hub,String command) throws IOException {
		hub.passwordRequested();
		hub.addCommand(new BadPass());
	}
	

	@Override
	public boolean matches(String command) {
		//don`t check for the existence of a space char
		return command.startsWith(getPrefix()); 
	}

	public static void sendPass(Hub hub,String pass) {
		hub.sendUnmodifiedRaw("$MyPass " + DCProtocol.doReplaces(pass) + "|" );
	}
}
