package uc.protocols.client;






import helpers.GH;

import java.util.HashMap;
import java.util.Map;

import uc.DCClient;
import uc.protocols.AbstractADCCommand;
import uc.protocols.IProtocolCommand;


public abstract class AbstractADCClientProtocolCommand extends AbstractADCCommand 
			implements IProtocolCommand<ClientProtocol> {

	protected final String prefix = "^C"+getPrefix();
//	/**
//	 * the client this command belongs to..
//	 */
//	protected final ClientProtocol client;
	
	
//	public AbstractADCClientProtocolCommand(ClientProtocol client) {
//		this.client = client;
//	}
//
	protected DCClient getDCC(ClientProtocol client) {
		return client.getCh().getDCC();
	}
	
	/**
	 * creates a map from a  space separated list of
	 * attributes 
	 * 
	 * @param attributes - list with attributes
	 * @return map keys are 2 chars long prefixes .. values are the letters after
	 * the key without protocol replaces
	 */
	public static Map<String,String> getCCFlagMap(String attributes) {
		Map<String,String> flagValue = new HashMap<String,String>();
		if (!GH.isNullOrEmpty(attributes)) {
			String[] splits = space.split(attributes);
			for (String s: splits) {
				if (s.length() >= 2) {
					String fls = s.substring(0, 2);
					String value = revReplaces(s.substring(2));
					flagValue.put(fls,value);
					
				}
			}
		}
		return flagValue;
	}
}
