package uc.protocols.hub;

import helpers.GH;


import java.io.IOException;
import java.net.ProtocolException;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import logger.LoggerFactory;

import uc.crypto.BloomFilter;
import uc.protocols.ADCStatusMessage;
import uc.protocols.Compression;
import uc.protocols.DCProtocol;


public class GET extends AbstractADCHubCommand {

	private static final Logger logger = LoggerFactory.make(Level.DEBUG);

	public GET() {
		setPattern(prefix +" blom / 0 ("+FILESIZE+")("+COMPRESSION+") ?(.*)",true);
	}

	public void handle(Hub hub,String command) throws ProtocolException, IOException {
		//"BK" and h in the flag "BH".
		int m = Integer.parseInt(matcher.group(1)) *8;
		Compression comp = Compression.parseNMDCString(matcher.group(2));
		
		Map<Flag,String> flags = getFlagMap(matcher.group(3));
		if (flags.containsKey(Flag.BH) && flags.containsKey(Flag.BK)) {
			int h = Integer.parseInt(flags.get(Flag.BH));
			int k = Integer.parseInt(flags.get(Flag.BK));
			if (m > 5*1024*1024) { //sanity check
				STA.sendSTAtoHub(hub, 
						new ADCStatusMessage("Too large Bloom filter requested. m= "+m, ADCStatusMessage.FATAL, ADCStatusMessage.TransferGeneric));
				throw new IOException("invalid length requested by hub: "+m);
			}
			
			long starttime = System.currentTimeMillis();
			BloomFilter blom = null;
			
			try {
				blom = BloomFilter.create(hub.getSelf().getFilelistDescriptor().getFilelist(), 
					m, h, k);
			} catch (IllegalArgumentException iae) {
				STA.sendSTAtoHub(hub, 
						new ADCStatusMessage("bad value provided: "+iae.getMessage(), 
								ADCStatusMessage.FATAL, ADCStatusMessage.TransferGeneric));
				throw new IOException(iae);
			}
			
			byte[] bloomBytes = comp.compress(blom.getBytes());

			
			byte[] snd = ("HSND blom / 0 "+(m/8)+comp.toString()+" BK"+k+" BH"+h+"\n").getBytes(DCProtocol.ADC_CHARENCODING);
			byte[] complete = GH.concatenate(snd,bloomBytes);

			long duration = System.currentTimeMillis() - starttime;
			logger.debug("Sending Bloomfilter: lenght: "+complete.length+" h:"+h+" k:"+k+" m:"+m+"  duration: "+duration+" ms");
			
			hub.sendUnmodifiedRaw(complete);
			
			
		}
	}
	
	

}
