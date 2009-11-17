package uc.protocols.hub;

import java.io.IOException;

public class LogedIn extends AbstractNMDCHubProtocolCommand {

	
	public LogedIn(Hub hub) {
		super(hub);
	}

	@Override
	public void handle(String command) throws IOException {
		hub.getSelf().setOp(true);
	}

	@Override
	public boolean matches(String command) {
		//don`t check for the existence of a space char
		return command.startsWith(getPrefix()); 
	}
	
	

}
