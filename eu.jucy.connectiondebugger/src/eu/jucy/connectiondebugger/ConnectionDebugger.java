package eu.jucy.connectiondebugger;

import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import logger.LoggerFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;


import helpers.GH;
import helpers.Observable;
import helpers.StatusObject;
import helpers.StatusObject.ChangeType;

import uc.protocols.ConnectionProtocol;
import uc.protocols.IConnectionDebugger;
import uc.protocols.IProtocolCommand;

public class ConnectionDebugger extends Observable<StatusObject> implements IConnectionDebugger {

	private static final Logger logger = LoggerFactory.make(Level.DEBUG);
	private int storeCommand = 500;
	
	private final List<SentCommand> lastCommands = 
		Collections.synchronizedList(new LinkedList<SentCommand>());
	
	private final Map<IProtocolCommand,Integer> commandCounter = 
		new HashMap<IProtocolCommand,Integer>();
	
	
	public boolean autoAttached(InetAddress ia, ConnectionProtocol attachedTo) {
		
		logger.info("Attached : "+ia+"  connection: "+attachedTo);
		return false;
	}

	public void receivedCommand(IProtocolCommand commandHandler,
			boolean wellFormed, String command) {
		if (commandHandler != null) {
			synchronized(commandCounter) {
				GH.incrementMappedCounter(commandCounter, commandHandler, 1);
			}
		}
		
		ReceivedCommand rc = new ReceivedCommand(commandHandler, command, wellFormed);
		add(rc);
	}
	
	public void sentCommand(String sent) {
		SentCommand sc = new SentCommand(sent,false);
		add(sc);
	}
	
	private void add(SentCommand sc) {
		lastCommands.add(sc);
		notifyObservers(new StatusObject(sc,ChangeType.ADDED));
		while (lastCommands.size() > storeCommand) {
			SentCommand removed = lastCommands.remove(0);
			notifyObservers(new StatusObject(removed,ChangeType.REMOVED));
		}
	}
	
	public void clear() {
		lastCommands.clear();
		synchronized(commandCounter) {
			commandCounter.clear();
		}
	}

	public List<SentCommand> getLastCommands() {
		return lastCommands;
	}

	
	
}
