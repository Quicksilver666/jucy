package uc.protocols.client;

import java.io.IOException;

import uc.crypto.HashValue;
import uc.files.transfer.FileTransferInformation;
import uc.protocols.AbstractADCCommand;
import uc.protocols.Compression;

public class SND extends AbstractADCClientProtocolCommand {

	/**
	 * ADCSND Constructor for requesting file 
	 * @param client
	 * @param expectedfile
	 */
	public SND(ClientProtocol client,HashValue expectedfile, long startpos,long length) {
		super(client);
		
		setPattern( prefix + " file TTH/"+expectedfile+" "+startpos
				+" ("+(length == -1?FILESIZE:""+length)+")("+COMPRESSION +")",false);
	}
	
	/**
	 * adcSND for requested interleaves.
	 * 
	 * @param client
	 * @param expectedfile
	 */
	public SND(ClientProtocol client,HashValue expectedinterleaves) {
		super(client);
		setPattern(prefix + " tthl TTH/"+expectedinterleaves+" 0 ("+FILESIZE+")("+COMPRESSION+")",false);
	}
	
	/**
	 * ADCSND for a FileList..
	 * 
	 * @param client
	 */
	public SND(ClientProtocol client) {
		super(client);
		String filelista = "file files\\.xml\\.bz2";
		String filelistb = "list /"+TEXT_NOSPACE;
		String filelists = "(?:(?:"+filelista+")|(?:"+filelistb+"))";
		
		setPattern(prefix + " "+filelists+" 0 ("+FILESIZE+")("+COMPRESSION+")",true);
	}
	

	public void handle(String command) throws IOException {
		client.getFti().setLength(Long.parseLong(matcher.group(1)));
		client.getFti().setCompression(Compression.parseNMDCString(matcher.group(2)));
		client.removeCommand(this);
		client.transfer();
	}
	
	
	/**
	 * sends a raw that confirms a file..
	 * @param client - to this client
	 * @param what - confirming the file with tth what
	 * @param startpos - beginning in the file
	 * @param length -  length of the transfer..
	 * @param comp - what compression will be used for transferring..
	 */
	private static void sendADCSNDforFile(ClientProtocol client,HashValue what, long startpos, long endpos ,Compression comp) {
		client.sendUnmodifiedRaw("CSND file TTH/"+what+" "+startpos+" "+ endpos +comp.toString()+"\n");
	}
	
	/**
	 * raw for confirming a FileList
	 * @param length - how large the FileList is..
	 */
	private static void sendADCSNDforFilelist(ClientProtocol client,long length,Compression comp) {
		String list = (client.isNewList()? "list "+AbstractADCCommand.doReplaces(client.getFti().getFileListSubPath()):"file files.xml.bz2");
		client.sendUnmodifiedRaw("CSND "+ list+ " 0 " + length +comp.toString() +"\n");
	}
	
	private static void sendADCSNDforInterleaves(ClientProtocol client,HashValue what,long length,Compression comp) {
		client.sendUnmodifiedRaw("CSND tthl TTH/"+what+" 0 " + length +comp.toString()+"\n");
	}
	
	public static void sendADCSND(ClientProtocol client) throws IOException {
	//	client.setSimpleConnection(); //set simple connection so no problems occur with 
		FileTransferInformation fti = client.getFti();
		switch(fti.getType()) {
		case FILE:
			sendADCSNDforFile( client,fti.getHashValue(),fti.getStartposition(),fti.getLength(),fti.getCompression());
			break;
		case FILELIST:
			sendADCSNDforFilelist(client, fti.getLength(),fti.getCompression());
			break;
		case TTHL:
			sendADCSNDforInterleaves(client, fti.getHashValue(), fti.getLength(),fti.getCompression());
			break;
		}
	}

}
