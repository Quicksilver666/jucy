package uc.protocols.hub;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;




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

	public ConnectToMe() {
		setPattern(prefix+" "+NMDCNICK+"(?: "+NMDCNICK+")?"+" ("+IPv4+":?"+PORT+"?)(\\D?)",true); //may be non digit is after port for specifying encryption..
	}

	@Override
	public void handle(Hub hub,String command) throws IOException {
		InetSocketAddress isa = ConnectionProtocol.inetFromString(matcher.group(1),412);
		boolean encryption = "S".equals(matcher.group(2));
		hub.ctmReceived(isa,null,encryption?CPType.NMDCS:CPType.NMDC,null);
		
	}

	
	
	public static void sendCTM(Hub hub, IUser target,CPType type) {
	
		hub.sendRaw(	"$ConnectToMe %[userNI] %[myI4]:"
				+ hub.getIdentity().getConnectionHandler().getPort(type.isEncrypted())
				+ (type.isEncrypted()?"S":"") +"|",  
						new SendContext(target,Collections.<String,String>emptyMap()));
	}
	   

}
