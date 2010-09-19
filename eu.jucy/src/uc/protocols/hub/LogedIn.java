package uc.protocols.hub;

import java.io.IOException;

public class LogedIn extends AbstractNMDCHubProtocolCommand {

	


	@Override
	public void handle(Hub hub,String command) throws IOException {
		hub.getSelf().setOp(true);
	}

	@Override
	public boolean matches(String command) {
		//don`t check for the existence of a space char
		return command.startsWith(getPrefix()); 
	}
	
	

}
