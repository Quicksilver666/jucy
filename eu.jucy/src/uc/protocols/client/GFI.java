package uc.protocols.client;

import java.io.IOException;
import java.net.ProtocolException;

import uc.crypto.HashValue;
import uc.files.filelist.FileListFile;
import uc.files.search.SearchResult;

import uc.protocols.hub.RES;


public class GFI extends AbstractADCClientProtocolCommand {


	
	public GFI() {

		setPattern( prefix + " file TTH/("+TTH+") ?(.*)" , true);

		
	}

	@Override
	public void handle(ClientProtocol client,String command) throws ProtocolException, IOException {
			HashValue what = HashValue.createHash(matcher.group(1));
			FileListFile ff= client.getDcc().getFilelist().get(what);
			if (ff != null) {
				String token = "GFI";
				String m2 = matcher.group(2);
				if (m2.startsWith("TO")) {
					token = m2.substring(2);
					int space = token.indexOf(' ');
					if (space != -1) {
						token = token.substring(0, space);
					}
					token = AbstractADCClientProtocolCommand.revReplaces(token);
				}
				
				SearchResult sr = new SearchResult(ff
						, client.getSelf()
						, client.getDcc().getCurrentSlots()
						, client.getDcc().getTotalSlots()
						, token);
				String res = RES.getCRESString(sr);
				
				client.sendUnmodifiedRaw(res);
			}

	}

}
