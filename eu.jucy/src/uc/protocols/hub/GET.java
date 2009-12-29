package uc.protocols.hub;

import helpers.GH;


import java.io.IOException;
import java.net.ProtocolException;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import logger.LoggerFactory;

import uc.crypto.BloomFilter;
import uc.protocols.Compression;
import uc.protocols.DCProtocol;


public class GET extends AbstractADCHubCommand {

	private static final Logger logger = LoggerFactory.make(Level.DEBUG);

	public GET(Hub hub) {
		super(hub);
		setPattern(prefix +" blom / 0 ("+FILESIZE+")("+COMPRESSION+") ?(.*)",true);
	}

	public void handle(String command) throws ProtocolException, IOException {
		//"BK" and h in the flag "BH".
		int m = Integer.parseInt(matcher.group(1)) *8;
		Compression comp = Compression.parseNMDCString(matcher.group(2));
		
		Map<Flag,String> flags = getFlagMap(matcher.group(3));
		if (flags.containsKey(Flag.BH) && flags.containsKey(Flag.BK)) {
			int h = Integer.parseInt(flags.get(Flag.BH));
			int k = Integer.parseInt(flags.get(Flag.BK));
			
			long starttime = System.currentTimeMillis();
			BloomFilter blom = 
			BloomFilter.create(hub.getSelf().getFilelistDescriptor().getFilelist(), 
					m, h, k);
			
			byte[] bloomBytes = comp.compress(blom.getBytes());

			
			byte[] snd = ("HSND blom / 0 "+(m/8)+comp.toString()+"\n").getBytes(DCProtocol.ADCCHARENCODING);
			byte[] complete = GH.concatenate(snd,bloomBytes);

			long duration = System.currentTimeMillis() - starttime;
			logger.debug("Sending Bloomfilter: lenght: "+complete.length+" h:"+h+" k:"+k+" m:"+m+"  duration: "+duration+" ms");
			
			hub.sendUnmodifiedRaw(complete);
			
		}
	}
	
	

}
