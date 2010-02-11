package uc.protocols.hub;

import java.io.IOException;
import java.util.concurrent.TimeUnit;


import uc.FavHub;



/**
 * example:
 * 
 * $ForceMove <newAddr>|
 * $To:<victimNick> From: <senderNick> $<<senderNick>> You are being re-directed to <newHub> because: <reasonMsg>
 * 
 * $ForceMove stallions.i-h-net.net:5555 
 * 
 * @author Quicksilver
 *
 */
public class ForceMove extends AbstractNMDCHubProtocolCommand {

	
	
	public ForceMove(Hub hub) {
		super(hub);
		setPattern(prefix+" ("+TEXT_NOSPACE+").*",true);
	}

	@Override
	public void handle(String command) throws IOException {
		final FavHub address = new FavHub(matcher.group(1));
		//schedule this ... so later coming reasons can be received..
		hub.getDcc().getSchedulerDir().schedule(
				new Runnable() {
					public void run() {
						synchronized(hub) {
							hub.redirectReceived(address);
						}
					}
				}, 500, TimeUnit.MILLISECONDS);

	}

}
