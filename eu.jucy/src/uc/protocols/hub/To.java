package uc.protocols.hub;

import java.io.IOException;

import java.util.regex.Pattern;

import uc.IUser;
import uc.User;
import uc.protocols.DCProtocol;

/**
 * 
 *  sends a private message
 *  
 *  	$To: <other nick> From: <nick> $<<nick>> <message> 
 * 
 * @author Quicksilver
 *
 */
public class To extends AbstractNMDCHubProtocolCommand {

	private final Pattern normalPM ;
	private final Pattern hubMM ;
	
	private MC mc = null;
	
	public To(Hub hub) {
		super(hub);
		normalPM = Pattern.compile( prefix +" "+NICK+" From: ("+NICK+") \\$(?:<("+NICK+")> )?("+TEXT+")");
		hubMM = Pattern.compile(prefix +" "+NICK+" From: Hub \\$("+TEXT+")");
	}

	@Override
	public void handle(String command) throws IOException {
		//first check is to be sure its a really old hub
		if (hub.getOthersSupports().isEmpty() && (matcher = hubMM.matcher(command)).matches() ) {
			getMC().handle(matcher.group(1));
		} else if ((matcher = normalPM.matcher(command)).matches()) {
			User from = hub.getUserByNick(matcher.group(1));
			String senderNick = matcher.group(2);
			User sender = senderNick != null? hub.getUserByNick(senderNick) : null;
			
	
			if (from != null) {
				String message = DCProtocol.reverseReplaces(matcher.group(3));
				hub.pmReceived(new PrivateMessage(from, sender, message,false));
			}
		}
	}
	
	private MC getMC() {
		if (mc == null) {
			mc = new MC(hub);
		}
		return mc;
	}
	
	public static void sendPM(Hub hub,IUser target,String message,boolean me) {
		String mynick = hub.getSelf().getNick();
		hub.sendUnmodifiedRaw("$To: "+target.getNick()+" From: "+mynick+" $"
				+ (me? "*"+mynick+" ": "<"+mynick+"> ")
				+ DCProtocol.doReplaces(message) 
				+ (me?"*":"")
				+"|");
	}
	

	@Override
	public String getPrefix() {
		return "$To:";
	}
	
	
	/*
	 * test for the pattern of TO
	 *
	public static void main(String[] args) {
		
		String pm = "$To: hallodri From: mick $<micknick> huhuapsah";
		To to = new To(null);
		Matcher m = to.pattern.matcher(pm);
		boolean matches = m.matches();
		
		System.out.println(matches);
		if (matches) {
			System.out.println(m.group(1));
			System.out.println(m.group(2));
			System.out.println(m.group(3));
		}
	} */

	

	
	

}
