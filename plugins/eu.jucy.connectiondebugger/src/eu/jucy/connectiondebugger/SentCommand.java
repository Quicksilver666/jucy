package eu.jucy.connectiondebugger;

import java.util.Date;

import uc.protocols.ConnectionState;

import eu.jucy.connectiondebugger.SentCommandColumns.DateCol;

public class SentCommand {
	
	private final String command;
	private final Boolean incoming;
	private final long timeReceived;
	private final long nanosReceived;

	
	public SentCommand(ConnectionState cs) {
		this("-----"+cs.name()+"-----",null);
	}

	public SentCommand(String command,Boolean incoming) {
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

	public Boolean isIncoming() {
		return incoming;
	}
	
	public long getNanosReceived() {
		return nanosReceived;
	}
	
	public String toString() {
		String inc = incoming == null
							? " - "
							: (incoming? " < " : " > " );
		return DateCol.SDF.format(getTimeReceived())+inc+command;
	}
	
}