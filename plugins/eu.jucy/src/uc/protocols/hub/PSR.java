package uc.protocols.hub;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProtocolException;

import uc.DCClient;
import uc.files.downloadqueue.FileDQE;


public class PSR extends AbstractADCHubCommand {

	public PSR() {
		this(false);
	}
	
	private PSR(boolean udp) {
		String passive = getHeader();
		String active = "UPSR ("+CID+")";
		setPattern((udp?active:passive) +" (.*)",true);
	}
	
	public void handle(Hub hub,String command) throws ProtocolException, IOException {
		logger.warn("TCP PSR: "+command);
	}
	
	public void sendPSR(FileDQE res) {
		
	}
	
	public static void receivedPSR(InetSocketAddress from, CharSequence cs,DCClient dcc ) {
		//logger.warn("UDPPSR: "+cs);
		//UDPPSR: UPSR VKKPT7SXUB3TD4RXVH5TIDWDVC4HSSHMU54MXPI NI[daiz]fayenor HI81.16.174.121:7777 U46264 TR4T277EY7RFFWWDYTJT5F5P2EGU2TVOQGH2I26PI PC1 PI0,351
		//UDPPSR: UPSR VKKPT7SXUB3TD4RXVH5TIDWDVC4HSSHMU54MXPI NI[daiz]fayenor HI81.16.174.121:7777 U46264 TRSLIXSIL6IG5D6XWYWXYTOEHKAOQCPR3Z2DZ3W6Q PC1 PI0,348
	}
	
}
