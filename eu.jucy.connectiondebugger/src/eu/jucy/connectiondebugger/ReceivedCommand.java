package eu.jucy.connectiondebugger;

import uc.protocols.IProtocolCommand;

public class ReceivedCommand extends SentCommand {
	
	private final IProtocolCommand commandHandler;
	
	private final boolean wellFormed;
	
	public ReceivedCommand(IProtocolCommand commandHandler, String command,boolean wellFormed) {
		super(command,true);
		this.commandHandler = commandHandler;
		this.wellFormed = wellFormed;
	}

	public IProtocolCommand getCommandHandler() {
		return commandHandler;
	}

	public boolean isWellFormed() {
		return wellFormed;
	}
	
}