package eu.jucy.connectiondebugger;

import java.util.Date;

import eu.jucy.connectiondebugger.SentCommandColumns.DateCol;

public class SentCommand {
	
	private final String command;
	private final boolean incoming;
	private final long timeReceived;
	private final long nanosReceived;


	public SentCommand(String command,boolean incoming) {
		super();
		this.command = command;
		this.incoming= incoming;
		timeReceived = System.currentTimeMillis();
		nanosReceived = System.nanoTime();
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
	
	public long getNanosReceived() {
		return nanosReceived;
	}
	
	public String toString() {
		return DateCol.SDF.format(getTimeReceived())+(incoming?" < ":" > ")+command;
	}
	
}