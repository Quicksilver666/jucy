package uc.protocols.hub;

import helpers.GH;

import java.io.IOException;

import logger.LoggerFactory;


import org.apache.log4j.Logger;

import uc.protocols.DCProtocol;

public class Lock extends AbstractNMDCHubProtocolCommand {

	private static Logger logger = LoggerFactory.make();
	

	
	public Lock(Hub hub) {
		super(hub);
	}
	
	@Override
	public void handle(String command) throws IOException {
		logger.debug("Lock received: "+command);
		//Lock will be accepted only once .. clear all other commands.. (so ADC detector is gone or other stuff is gone)
		hub.clearCommands();
		//add next accepted commands
	
		hub.addCommand(	new HubName(hub),
						new Supports(hub),
						new Hello(hub),
						new LogedIn(hub), 
						new GetPass(hub),
						new HubIsFull(hub),
						new ValidateDenide(hub),
						new ForceMove(hub));
		if (Supports.useZLIB) {
			hub.addCommand(new ZOn(hub));
		}


		//hub.setNmdchub(true);
		hub.setOnceConnected();
		
		//now handle the lock and send the key, supports and validate nick back	
	
		
		byte[] key = DCProtocol.generateKey(command,hub.getCharset());

		
		String validateNick = "$ValidateNick "+hub.getSelf().getNick()+ "|";
		
		if (command.contains("EXTENDEDPROTOCOL")) {
			
			hub.sendUnmodifiedRaw(GH.concatenate(
					Supports.HUBSUPPORTS.getBytes(hub.getCharset().name()),
					key,
					validateNick.getBytes(hub.getCharset().name())));
			
		//	hub.sendUnmodifiedRaw(Supports.HUBSUPPORTS+key+validateNick);//done so all are sent in the first packet..
		} else {
			hub.addCommand(new NickList(hub));
			hub.sendUnmodifiedRaw(GH.concatenate(key,validateNick.getBytes(hub.getCharset().name())));
			
		}
		logger.debug("Lock received -> responded with Key, ValidateNick and Supports: "+new String(key,hub.getCharset().name()));
	}

	
	
}
