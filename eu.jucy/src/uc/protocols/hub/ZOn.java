package uc.protocols.hub;

import java.io.IOException;
import java.net.ProtocolException;

import logger.LoggerFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;




public class ZOn extends AbstractNMDCHubProtocolCommand {

	private static final Logger logger = LoggerFactory.make(Level.DEBUG);

	
	public ZOn() {
		setPattern(prefix,true);
	}

	@Override
	public void handle(Hub hub,String command) throws IOException {
		logger.debug("toggle line decompression "+hub.getFavHub().getSimpleHubaddy());
		hub.enableDecompression();
	}


	public static class ZON extends AbstractADCHubCommand {

		public ZON() {
			setPattern(getHeader(),true);
		}
		
		public void handle(Hub hub,String command) throws ProtocolException,IOException {
			logger.debug("toggle line decompression "+hub.getFavHub().getSimpleHubaddy());
			hub.enableDecompression();
			
		}
		
	}
	
}
