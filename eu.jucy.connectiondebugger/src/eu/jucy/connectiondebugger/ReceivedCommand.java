package eu.jucy.connectiondebugger;

import java.io.IOException;
import java.net.ProtocolException;

import uc.protocols.ConnectionProtocol;
import uc.protocols.IProtocolCommand;

public class ReceivedCommand extends SentCommand {
	
	public static final Unknown UNKNOWN = new Unknown();
	
	private final IProtocolCommand<?> commandHandler;
	
	
	
	private final boolean wellFormed;
	
	public ReceivedCommand(IProtocolCommand<?> commandHandler, String command,boolean wellFormed) {
		super(command,true);
		this.commandHandler = commandHandler == null?UNKNOWN:commandHandler;
		this.wellFormed = wellFormed;
	}

	public IProtocolCommand<?> getCommandHandler() {
		return commandHandler;
	}

	public boolean isWellFormed() {
		return wellFormed;
	}
	
	public static class Unknown implements IProtocolCommand<ConnectionProtocol> {
		public String getPrefix() {
			return getClass().getSimpleName();
		}

		public void handle(ConnectionProtocol protocol,String command) throws ProtocolException,
				IOException {
			throw new UnsupportedOperationException();
		}

		public boolean matches(String command) {
			throw new UnsupportedOperationException();
		}
	}
	
}