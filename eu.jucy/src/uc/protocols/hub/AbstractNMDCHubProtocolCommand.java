package uc.protocols.hub;

import java.io.IOException;


import uc.protocols.AbstractNMDCProtocolCommand;
import uc.protocols.IProtocolCommand;

/**
 * abstract protocol command
 * represents a baseclass for all NMDC and ADC commands..
 * especially for the ones in the hub
 * 
 * @author quicksilver
 *
 */
public abstract class AbstractNMDCHubProtocolCommand extends AbstractNMDCProtocolCommand implements IProtocolCommand {


	

	
	
	
	
/*	public static void main(String[] args) {
		System.out.println(Pattern.matches(BYTE, "254"));
		System.out.println(Pattern.matches(IPv4, "254.168.0.1"));
	//	System.out.println(Pattern.matches(FILENAME, "hello.odf"));
		System.out.println(Pattern.matches(TTH,"4CLZLU7TCB6C4YTHN7JNOIA7F7VQVJV5762AYJA"));
		//Pattern port = Pattern.compile(PORT);
		for (int i=-1; i < 32769; i++) {
			if (!Pattern.matches(SHORT,""+i)) {
				System.out.println(i);
			}
		}
		
	} */
	
	protected final Hub hub;
	

	

	public AbstractNMDCHubProtocolCommand(Hub hub) {
		this.hub = hub;

	}
	


	

	public abstract void handle(String command) throws IOException;
	
	

	
	
}
