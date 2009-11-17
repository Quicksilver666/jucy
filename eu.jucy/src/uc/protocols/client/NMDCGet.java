package uc.protocols.client;

import java.io.IOException;
import java.net.ProtocolException;

public class NMDCGet extends AbstractNMDCClientProtocolCommand {

	public NMDCGet(ClientProtocol client) {
		super(client);
	}

	/**
	 * Syntax

		$Get <file>$<offset>|
		
		Description
		
		This command is used to request a file from the uploading client.
		
		• <file> is the full file name and the path to it, as per the source's $SR or FileList. • <offset> is the starting point of the download (counted from 1, not from 0)
		
		The $Error with "File Not Found" is sent when the file is not available. NMDC disconnects the user if no directory is provided. (i.e. $Error on "$Get nonexistent_directory\nonexistent_file$1|" and disconnect on "$Get nonexistent_file$1|")
		
		When the file is available, the source must respond with $FileLength.
		
		The FileList is retrieved with "$Get MyList.DcLst$1|" 
		
		ex. $Get Video\Serien\Dr. House\Staffel 03\Dr.House.S03E10.avi$1
		
		obsolete because of GetZBlock .. which is obsolete because of ADCGet ..
	 */
	@Override
	public void handle(String command) throws ProtocolException, IOException {
		client.disconnect(DisconnectReason.CLIENTTOOOLD);
	}

	@Override
	public String getPrefix() {
		return "$Get";
	}
	
	

}
