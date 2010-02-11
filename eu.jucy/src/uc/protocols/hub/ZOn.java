package uc.protocols.hub;

import java.io.IOException;
import java.net.ProtocolException;

import logger.LoggerFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.Platform;



public class ZOn extends AbstractNMDCHubProtocolCommand {

	private static final Logger logger = LoggerFactory.make();
	static {
		logger.setLevel(Platform.inDevelopmentMode()? Level.DEBUG :Level.INFO);
	}
	
	public ZOn(Hub hub) {
		super(hub);
		setPattern(prefix,true);
	}

	@Override
	public void handle(String command) throws IOException {
		logger.debug("toggle line decompression "+hub.getFavHub().getSimpleHubaddy());
		hub.enableDecompression();
	}


	public static class ZON extends AbstractADCHubCommand {

		public ZON(Hub hub) {
			super(hub);
			setPattern(getHeader(),true);
		}
		
		public void handle(String command) throws ProtocolException,IOException {
			logger.debug("toggle line decompression "+hub.getFavHub().getSimpleHubaddy());
			hub.enableDecompression();
			
		}
		
	}
	
}
