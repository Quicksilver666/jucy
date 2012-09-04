package uc.database;

import helpers.GH;
import uc.crypto.HashValue;

/**
 * default implementation of ILogEntry..
 * 
 * @author Quicksilver
 *
 */
public class LogEntry implements ILogEntry {



	private final long date;
	private final HashValue entityID;
	private final String message;
	private final String name;
	
	

	
	public LogEntry(long date, HashValue entityID, String message, String name) {
		super();
		this.date = date;
		this.entityID = entityID;
		if (message.length() > MAX_MESSAGELENGTH) {
			message = message.substring(0, MAX_MESSAGELENGTH);
		}
		this.message = message;
		this.name = name;
	}
	
	
	public long getDate() {
		return date;
	}

	public HashValue getEntityID() {
		return entityID;
	}

	public String getMessage() {
		return message;
	}

	public String getName() {
		return name;
	}


	@Override
	public int compareTo(ILogEntry o) {
		return GH.compareTo(date, o.getDate());
	}

	
}
