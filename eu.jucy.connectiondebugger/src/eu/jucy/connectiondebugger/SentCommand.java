package eu.jucy.connectiondebugger;

import java.util.Date;

public class SentCommand {
	private final String command;
	private final boolean incoming;
	private final long timeReceived;
	public SentCommand(String command,boolean incoming) {
		super();
		this.command = command;
		this.incoming= incoming;
		timeReceived = System.currentTimeMillis();
	}
	
	public Date getTimeReceived() {
		return new Date(timeReceived);
	}
	
	public String getCommand() {
		return command;
	}

	public boolean isIncoming() {
		return incoming;
	}
	
	
	
}