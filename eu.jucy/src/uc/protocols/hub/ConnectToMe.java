package uc.protocols.hub;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;



import uc.DCClient;
import uc.IUser;
import uc.protocols.CPType;
import uc.protocols.ConnectionProtocol;
import uc.protocols.SendContext;

/**
 * command that requests to connect
 * 
 * either
 * 
 * 		$ConnectToMe <receiver's nick> <ip:port>
 * 
 * or sometimes 
 * 
 * 		$ConnectToMe <receiver's nick> <sender's nick> <ip:port>
 * 
 * @author Quicksilver
 *
 */
public class ConnectToMe extends AbstractNMDCHubProtocolCommand {

	public ConnectToMe(Hub hub) {
		super(hub);
		setPattern(prefix+" "+NMDCNICK+"(?: "+NMDCNICK+")?"+" ("+IPv4+":?"+PORT+"?)(\\D?)",true); //may be non digit is after port for specifying encryption..
	}

	@Override
	public void handle(String command) throws IOException {
		InetSocketAddress isa = ConnectionProtocol.inetFromString(matcher.group(1),412);
		boolean encryption = "S".equals(matcher.group(2));
		hub.ctmReceived(isa,null,encryption?CPType.NMDCS:CPType.NMDC,null);
		
	}

	
	
	public static void sendCTM(Hub hub, IUser target,CPType type) {
		DCClient dcc = hub.getDcc();
		hub.sendRaw(	"$ConnectToMe %[userNI] %[myI4]:"
				+ dcc.getCh().getPort(type.isEncrypted())
				+ (type.isEncrypted()?"S":"") +"|",  
						new SendContext(target,Collections.<String,String>emptyMap()));
	}
	   

}
