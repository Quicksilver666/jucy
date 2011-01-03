package uc.protocols.hub;

import java.io.IOException;







public class Hello extends AbstractNMDCHubProtocolCommand {

	private final boolean active;
	public Hello() {
		this(true);
	}
	
	private Hello(boolean active) {
		this.active = active;
	}

	@Override
	public void handle(Hub hub,String command) throws IOException {
		if (active) {
			hub.insertUser(hub.getSelf());
			hub.onLogIn();
			
			//create not activated hello.. so other users hellos are ignored 
			hub.addCommand(new Hello(false));

			
			hub.sendUnmodifiedRaw("$Version 1,0091|");
			hub.sendUnmodifiedRaw("$GetNickList|");
			
			hub.sendMyInfo(true); 
		}
		
	}
	
	

}
