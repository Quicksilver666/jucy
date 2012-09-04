package uc.database;

import uc.crypto.HashValue;

/**
 * a single entry for logging to the database
 * 
 * @author Quicksilver
 *
 */
public interface ILogEntry extends Comparable<ILogEntry> {

	public static final int MAX_MESSAGELENGTH = 131072;
	
	/**
	 * 
	 * @return the message being logged..
	 */
	String getMessage();
	
	/**
	 * @return  the time the Log even occured
	 */
	long getDate();
	
	/**
	 * 
	 * @return an ID for the one that issued the Log message..
	 */
	HashValue getEntityID();
	
	/**
	 * @return a human readable Name for where the Log message came from
	 * (may be not unique)
	 */
	String getName();
	
}
