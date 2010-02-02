package uc.protocols.hub;

import java.io.IOException;







public class Hello extends AbstractNMDCHubProtocolCommand {

	private final boolean active;
	public Hello(Hub hub) {
		this(hub,true);
	}
	
	private Hello(Hub hub, boolean active) {
		super(hub);
		this.active = active;
	}

	@Override
	public void handle(String command) throws IOException {
		if (active) {
			
			hub.insertUser(hub.getSelf());
		
			
			//change Login state.
			hub.onLogIn();
			//change active commands..
			//remove all commands from login inclusive self..
			hub.clearCommands();
			//add commands needed while protocol is running
			hub.addCommand(	new LogedIn(hub),
							new Feed(hub), 
							new HubName(hub),
							new HubTopic(hub),
							new MyINFO(hub),
							new OpList(hub),
							new Quit(hub),
							new SR(hub),
							new To(hub),
							new UserIP(hub),
							new UserCommand(hub),
							new Hello(hub,false),//create not activated hello.. so other users hellos are ignored 
							new ForceMove(hub),
							new NickList(hub)); 
			if (!hub.getFavHub().isChatOnly()) {
				hub.addCommand(new ConnectToMe(hub),new RevConnectToMe(hub),new Search(hub));
			}
			
			if (Supports.useZLIB) {
				hub.addCommand(new ZOn(hub));
			}

	
			
		
			
			hub.sendUnmodifiedRaw("$Version 1,0091|");
			hub.sendUnmodifiedRaw("$GetNickList|");
			
			hub.sendMyInfo(true); 
		}
		
	}
	
	

}
