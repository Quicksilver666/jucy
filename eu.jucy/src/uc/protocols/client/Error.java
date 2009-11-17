package uc.protocols.client;

import java.io.IOException;
import java.net.ProtocolException;


import uc.IUser;
import uc.files.downloadqueue.AbstractDownloadQueueEntry;


public class Error extends AbstractNMDCClientProtocolCommand {

	public Error(ClientProtocol client) {
		super(client);
		setPattern(prefix+" ([^|]*)",true);
	}

	@Override
	public void handle(String command) throws ProtocolException, IOException {
		String reason = matcher.group(1);
		client.otherSentError(reason);
		if (reason.equals("File Not Available")) {
			IUser usr = client.getFti().getOther();
			AbstractDownloadQueueEntry adqe = client.getFti().getDqe();
			if (usr != null && adqe != null) {
				adqe.removeUser(usr);
			}
		}
	}

	public static void sendError(ClientProtocol cp,DisconnectReason reason) {
		cp.sendRaw("$Error " + reason + "|");
	}
}
