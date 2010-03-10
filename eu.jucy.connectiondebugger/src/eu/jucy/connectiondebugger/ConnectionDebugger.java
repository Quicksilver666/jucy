package eu.jucy.connectiondebugger;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.Collection;
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
import uc.protocols.ConnectionState;
import uc.protocols.IConnectionDebugger;
import uc.protocols.IProtocolCommand;

public class ConnectionDebugger extends Observable<StatusObject> implements IConnectionDebugger {

	private static final Logger logger = LoggerFactory.make(Level.DEBUG);
	private int storeCommand = 3000;
	
	private InetAddress ia;
	private ConnectionProtocol current;
	
	private CryptoInfo currentCI;
	
	private final List<SentCommand> lastCommands = 
		Collections.synchronizedList(new LinkedList<SentCommand>());
	
	private final Map<IProtocolCommand,CommandStat> commandCounter = 
		new HashMap<IProtocolCommand,CommandStat>();
	
	private long trafficTotal;
	


	public void notifyAttachable(InetAddress ia, ConnectionProtocol attacheTo) {
		if (current != null) {
			current.unregisterDebugger(this);
		}
		init(attacheTo);
		logger.info("Attached : "+ia+"  connection: "+attacheTo);
	}
	
	public void init(InetAddress ia) {
		this.ia = ia;
		ConnectionProtocol.addNotifyAttachable(ia, this);
	}
	public void init(ConnectionProtocol cp) {
		current = cp;
		current.registerDebugger(this);
		if (current.isEncrypted()) {
			currentCI = new CryptoInfo();
			cp.getConnection().getCryptoInfo(currentCI);
			notifyObservers(new StatusObject(currentCI,ChangeType.CHANGED));
		}
	}
	
	public void dispose() {
		if (current != null) {
			current.unregisterDebugger(this);
		}
		if (ia != null) {
			ConnectionProtocol.removeNotifyAttachable(ia);
		}
	}

	public void receivedCommand(IProtocolCommand commandHandler,
			boolean wellFormed, String command) {
		
		ReceivedCommand rc = new ReceivedCommand(commandHandler, command, wellFormed);
		
		CommandStat cs;
		synchronized(commandCounter) {
			cs = commandCounter.get(rc.getCommandHandler());
			if (cs == null) {
				cs = new CommandStat(rc.getCommandHandler());
				commandCounter.put(rc.getCommandHandler(), cs);
				notifyObservers(new StatusObject(cs,ChangeType.ADDED));
			}
		
			synchronized(cs) {
				cs.frequency++;
				cs.lastCommand = command;
				try {
					int traf = command.getBytes(current.getCharset().name()).length + 1; //\n/| char is one byte in size and not counted here otherwise..
					trafficTotal += traf;
					cs.trafficCommand += traf;
				} catch (UnsupportedEncodingException e) {
					throw new IllegalStateException();
				}
			}
		}
		notifyObservers(new StatusObject(cs,ChangeType.CHANGED));
		
		
		
		
		add(rc);
	}
	
	public long getTrafficTotal() {
		synchronized (commandCounter) {
			return trafficTotal;
		}
	}

	public Collection<CommandStat> getCommandCounter() {
		return Collections.unmodifiableCollection(commandCounter.values());
	}

	public void sentCommand(String sent) {
		if (!GH.isEmpty(sent)) {
			SentCommand sc = new SentCommand(sent.substring(0, sent.length()-1),false);
			add(sc);
		}
	}
	
	
	public void statusChanged(ConnectionState newStatus, ConnectionProtocol cp) {
		if (newStatus == ConnectionState.CONNECTED) {
			if (cp.isEncrypted()) {
				currentCI = new CryptoInfo();
			
				cp.getConnection().getCryptoInfo(currentCI);
				logger.info("Connected: "+newStatus);
				notifyObservers(new StatusObject(currentCI,ChangeType.CHANGED));
			}
		}
		add(new SentCommand(newStatus));
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
	
	
	

	public static class CommandStat {
		private  final String commandName;
		

		private int frequency;
		private String lastCommand = "";
		private long trafficCommand;
		
		public CommandStat(IProtocolCommand com) {
			commandName = com.getClass().getSimpleName();
		}
		
		
		public String getCommandName() {
			return commandName;
		}

		public synchronized int getFrequency() {
			return frequency;
		}
		
		public synchronized String getLastCommand() {
			return lastCommand;
		}
		
		public synchronized long getTrafficTotal() {
			return trafficCommand;
		}
		

	}
	
}
