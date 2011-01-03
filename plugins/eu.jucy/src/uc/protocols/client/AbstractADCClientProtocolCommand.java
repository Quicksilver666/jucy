package uc.protocols.client;






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
}
