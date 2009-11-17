package uc.protocols.hub;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ProtocolException;
import java.util.HashMap;
import java.util.Map;

import logger.LoggerFactory;


import org.apache.log4j.Logger;

import uc.FavHub;
import uc.crypto.BASE32Encoder;
import uc.crypto.HashValue;
import uc.crypto.Tiger;

public class GPA extends AbstractADCHubCommand {

	private static Logger logger = LoggerFactory.make();

	
	private static Map<FavHub,String> randoms = new HashMap<FavHub,String>();
	
	public GPA(Hub hub) {
		super(hub);
		setPattern("IGPA ("+BASE32CHAR+"{10,})",true);
	}

	public void handle(String command) throws ProtocolException, IOException {
		randoms.put(hub.getFavHub(), matcher.group(1));
		hub.passwordRequested();
	}
	
	public static void sendPass(Hub hub,String pass) {
		try {
			String rand = randoms.get(hub.getFavHub());
			randoms.remove(hub.getFavHub());
			if (rand != null) {
				byte[] passbytes = pass.getBytes("UTF-8");
				byte[] random = BASE32Encoder.decode(rand);
				
				byte[] all = new byte[passbytes.length+random.length];
				System.arraycopy(passbytes, 0, all, 0, passbytes.length);
				System.arraycopy(random, 0, all, passbytes.length, random.length);
				
				HashValue hash = Tiger.tigerOfBytes(all); //TODO send warning to hub if to few random bytes..
				
				hub.sendUnmodifiedRaw("HPAS "+hash.toString()+"\n");
			}
		} catch (UnsupportedEncodingException usee) {
			logger.error("This should never happen "+usee, usee);
		}
	}

}
