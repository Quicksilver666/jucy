package uc.protocols.client;

import java.io.IOException;
import java.net.ProtocolException;

public class UGetBlock extends AbstractNMDCClientProtocolCommand {


	@Override
	public void handle(ClientProtocol client,String command) throws ProtocolException, IOException {
		client.disconnect(DisconnectReason.CLIENTTOOOLD);
	}

}
