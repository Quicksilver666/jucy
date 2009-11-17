package uc.protocols.client;

import java.io.IOException;
import java.net.ProtocolException;

public class UGetBlock extends AbstractNMDCClientProtocolCommand {

	public UGetBlock(ClientProtocol client) {
		super(client);
	}

	@Override
	public void handle(String command) throws ProtocolException, IOException {
		client.disconnect(DisconnectReason.CLIENTTOOOLD);
	}

}
