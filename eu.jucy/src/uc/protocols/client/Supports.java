package uc.protocols.client;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import uc.FavHub;
import uc.IUser;

public class Supports  extends AbstractNMDCClientProtocolCommand  {

	private static final String Supports = "$Supports MiniSlots XmlBZList ADCGet TTHL TTHF ZLIG"; //+ "|";
	
	
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
		String supports = Supports;
		IUser self = client.getSelf();
		if (self != null && !client.isIncoming()) {
			FavHub fh = self.getHub().getFavHub();
			String s = fh.getHubaddy();
			supports += " Ref=" + s;
		}
		client.sendRaw(supports + " |");

		
		client.increaseLoginLevel();
	}
	
}
