package uc.protocols.client;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;


public class Supports  extends AbstractNMDCClientProtocolCommand  {

	private static final String SUPPORTS = "$Supports MiniSlots XmlBZList ADCGet TTHL TTHF ZLIG |"; //+ "|";
	
//	private static final Logger logger = LoggerFactory.make(Level.DEBUG);
	
	public Supports(ClientProtocol client) {
		super(client);
		setPattern(prefix+"(.*)",true);
	}

	@Override
	public void handle(String command) throws IOException {
		
		//just grab rest of the support string.. and trim it..
		Set<String> supps = client.getOthersSupports(); 
		
		supps.addAll(Arrays.asList(space.split(matcher.group(1).trim())));
		client.setOthersSupports(supps);
		
		client.removeCommand(this);
	}

	
	public static void sendSupports(ClientProtocol client) throws IOException {
		client.sendUnmodifiedRaw(SUPPORTS);
		client.increaseLoginLevel();
	}
	
}
