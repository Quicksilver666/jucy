package uc.protocols.client;

import java.io.IOException;
import java.net.ProtocolException;
import java.util.Map;

import logger.LoggerFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import uc.protocols.ADCStatusMessage;
import uc.protocols.hub.AbstractADCHubCommand;
import uc.protocols.hub.Flag;

public class STA extends AbstractADCClientProtocolCommand {

	private static final Logger logger = LoggerFactory.make(Level.DEBUG);

	
	public STA(ClientProtocol client) {
		super(client);
		setPattern(prefix+" ([012])(\\d{2}) ("+ADCTEXT+")(?: (.*))?",true);
	}


	public void handle(String command) throws ProtocolException, IOException {
		logger.debug(command);
		int severity = Integer.valueOf(matcher.group(1));
		int errorCode = Integer.valueOf(matcher.group(2));
		String message = revReplaces(matcher.group(3));
		
		Map<Flag,String> flagMap = AbstractADCHubCommand.getFlagMap(matcher.group(4));
		
		
		ADCStatusMessage sm  = new ADCStatusMessage(message,severity,errorCode); 
		switch(errorCode) {
		case 53:
			if ("Slots full".equals(message)) {
				message = "";
			}
			if (flagMap.containsKey(Flag.QP)) {
				message += flagMap.get(Flag.QP);
			}
			
			client.noSlotsAvailable(message);
			break;
		}
		
		if (severity == ADCStatusMessage.FATAL) {
			client.otherSentError(sm.getMessage() == null ? message: sm.getMessage());
		} else {
			logger.debug("message received: "+sm.toString());
		}
	}
	
	public static void sendSTA(ClientProtocol client,ADCStatusMessage sm) {
		client.sendUnmodifiedRaw("CSTA "+sm.toADCString()+"\n");
		if (sm.getSeverity() == ADCStatusMessage.FATAL) {
			client.disconnect(sm);
		}
	}
	
	

}
