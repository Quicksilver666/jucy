package uc.protocols.client;

import java.io.IOException;
import java.net.ProtocolException;
import java.util.Map;

import uc.DCClient;
import uc.User;
import uc.crypto.HashValue;
import uc.protocols.ADCStatusMessage;
import uc.protocols.hub.Flag;
import uc.protocols.hub.INFField;

public class INF extends AbstractADCClientProtocolCommand {

	public INF(ClientProtocol client) {
		super(client);
		setPattern(prefix +" (.*)",true);
	}

	
	public void handle(String command) throws ProtocolException, IOException {
		Map<INFField,String> fields = INFMap(matcher.group(1));

		HashValue cid = null; 
		User other = null;
		if (fields.containsKey(INFField.ID)) {
			cid		= HashValue.createHash(fields.get(INFField.ID));
			other	= getDCC().getUserForCID(cid);
		} else {
			
			STA.sendSTA(client, new ADCStatusMessage("No ID field",
					ADCStatusMessage.FATAL,
					ADCStatusMessage.ProtocolRequiredINFfieldBadMissing,
					Flag.FM,INFField.ID.name()));
			return;
		}
		
		String token = fields.get(INFField.TO);
		
		 
		
		if (other == null) {
			STA.sendSTA(client, new ADCStatusMessage("User unknown",
					ADCStatusMessage.FATAL,
					ADCStatusMessage.TransferGeneric ));
			return;
		} 
		client.otherIdentified(other);
		
		if (!client.isIncoming()) {
			sendINFOutgoing();	
		} else {
			if (token != null) {
				client.setToken(token);
			} else {
				STA.sendSTA(client, new ADCStatusMessage("No Token field",
						ADCStatusMessage.FATAL,
						ADCStatusMessage.ProtocolRequiredINFfieldBadMissing,
						Flag.FM,INFField.TO.name()));
				return;
			}
		}
		client.setDownload(false);
		
		client.onLogIn();
	}
	
	private void sendINFOutgoing() {
		String inf = "CINF ID"+client.getSelf().getCID()+" TO"+doReplaces(client.getToken()) ;
		client.sendRaw(inf+"\n");
	}
	
	public static void sendINFIncoming(ClientProtocol cp) {
		//CINF IDAFVC6C65R4ZLTP7UYDDK6QJPQHUZLAJPSZSG3DQ  no TOKEN info in incoming
		cp.sendRaw("CINF ID"+DCClient.get().getPID().hashOfHash()+"\n");
	}
	


}
