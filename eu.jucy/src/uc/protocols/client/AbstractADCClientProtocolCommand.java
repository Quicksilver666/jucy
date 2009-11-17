package uc.protocols.client;





import uc.DCClient;
import uc.protocols.AbstractADCCommand;


public abstract class AbstractADCClientProtocolCommand extends AbstractADCCommand {

	protected final String prefix = "^C"+getPrefix();
	/**
	 * the client this command belongs to..
	 */
	protected final ClientProtocol client;
	
	
	public AbstractADCClientProtocolCommand(ClientProtocol client) {
		this.client = client;
	}

	protected DCClient getDCC() {
		return client.getCh().getDCC();
	}
}
