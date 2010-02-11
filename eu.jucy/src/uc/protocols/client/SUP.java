package uc.protocols.client;

import java.io.IOException;
import java.net.ProtocolException;
import java.util.Set;

import uc.IUser;
import uc.protocols.ADCStatusMessage;
import uc.protocols.hub.Flag;


/**
 * 
 * The client SUP
 * 
 *  EX CSUP ADBAS0 ADBZIP ADZLIG
 *  Connecting sends first..
 *  and Gets SUP and CINF just with CID and TO as response..
 * 
 * @author Quicksilver
 *
 */
public class SUP extends AbstractADCClientProtocolCommand {

	//ADTIGR -> we support TTHs..
	//BZIP -> Filelist in Bzip format
	//ZLIG -> ZLIB Get support
	public static final String SUPPORTS = "CSUP ADBASE ADTIGR ADBZIP ADZLIG\n"; 
	
	private boolean first = true;
	
	public SUP(ClientProtocol client) {
		super(client);
		setPattern(prefix +" (.*)",true);
	}

	

	public void handle(String command) throws ProtocolException, IOException {
		client.setProtocolNMDC(false);
		Set<String> supports = client.getOthersSupports();
		for (String sup: space.split(matcher.group(1))) {
			if (sup.length() > 2) {
				if (sup.startsWith("AD")) {
					supports.add(sup.substring(2));
				} else if (sup.startsWith("RM")) {
					supports.remove(sup.substring(2));
				}
			}
		}
		client.setOthersSupports(supports);	
		
		if (first && client.isIncoming()) {
			sendSUP(client);
			INF.sendINFIncoming(client);
		}
		
		first = false;
	}
	
	public static void sendSUP(ClientProtocol client) {
		client.addCommand(new INF(client));
	
		client.sendUnmodifiedRaw(SUPPORTS);
		
		IUser usr = client.getSelf();
		if (usr != null && !client.isIncoming()) { //send a STA about the referrer 
			String hubaddress = usr.getHub().getFavHub().getSimpleHubaddy();
			ADCStatusMessage adcm = new ADCStatusMessage("",0,0,Flag.RF,hubaddress);
			STA.sendSTA(client, adcm);
		}
	}

}
