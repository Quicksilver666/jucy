package uc.protocols.client;

import java.io.IOException;
import java.net.ProtocolException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import logger.LoggerFactory;

import org.apache.log4j.Logger;

import uc.crypto.HashValue;
import uc.files.transfer.FileTransferInformation;
import uc.protocols.ADCStatusMessage;
import uc.protocols.AbstractADCCommand;
import uc.protocols.Compression;
import uc.protocols.TransferType;
import uc.protocols.hub.Flag;

public class GET extends AbstractADCClientProtocolCommand  {

	private static Logger logger = LoggerFactory.make(); 
	
	private Pattern file;
	private Pattern interleaves;
	private Pattern filelist;
//	
	/*
Contexts: C

Requests that a certain file or binary data be transmitted. <start_pos> counts 0 as the first byte. <bytes> may be set to -1 to indicate that the sending client should fill it in with the number of bytes needed to complete the file from <start_pos>. <type> is a [a-zA-Z0-9]+ string that specifies the namespace for identifier and BASE requires that clients recognize the types "file" and "list". Extensions may add to the identifier names as well as add new types.

"file" transfers transfer the file data in binary, starting at <start_pos> and sending <bytes> bytes. Identifier must come from the namespace of the current session hash.

"list" transfers are used for partial file lists and have a directory as identifier. <start_pos> is always 0 and <bytes> contains the uncompressed length of the generated XML text in the corresponding SND. An optional flag "RE1" means that the client is requesting a recursive list and that the sending client should send the directory itself and all subdirectories as well. If this is too much, the sending client may choose to send only parts. The flag should be taken as a hint that the requesting client will be getting the subdirectories as well, so they might as well be sent in one go. Identifier must be a directory in the unnamed root, ending (and beginning) with "/".

Note that GET can also be used by extensions for binary transfers between hub and client.
	 */
	
	public GET() {
		file = Pattern.compile(prefix + " file TTH/("+TTH+") ("+FILESIZE+") ("+FILESIZE+"|-1)(.*)");
		interleaves = Pattern.compile(prefix + " tthl TTH/("+TTH+") 0 -1(.*)");
		
		String filelista = "file files\\.xml\\.bz2";
		String filelistb = "list /"+TEXT_NOSPACE;
		String filelists = "(?:(?:"+filelista+")|(?:"+filelistb+"))";
		
		filelist = Pattern.compile(prefix + " ("+filelists+") 0 -1(.*)");
	}

	public void handle(ClientProtocol client,String command) throws ProtocolException, IOException {
		Matcher m = null;
		FileTransferInformation fti = client.getFti();
		if ((m = file.matcher(command)).matches()) {
			
			fti.setType(TransferType.FILE);
			
			HashValue what = HashValue.createHash(m.group(1));
			fti.setHashValue(what);
			
			long startpos = Long.parseLong(m.group(2));
			fti.setStartposition(startpos);
			long length = Long.parseLong(m.group(3));
			fti.setLength(length);
			Map<String,String> flags= AbstractADCClientProtocolCommand.getCCFlagMap(m.group(4));
			Compression comp = Compression.parseAttributeMap(flags);
		
			fti.setCompression(comp);
			
			client.transfer(); //FileRequested(what, startpos, length, comp);
			
		} else if ( (m = interleaves.matcher(command)).matches()) {
			HashValue what = HashValue.createHash(m.group(1)); 
			fti.setType(TransferType.TTHL);
			fti.setHashValue(what);
			Map<String,String> flags= AbstractADCClientProtocolCommand.getCCFlagMap(m.group(2));
			Compression comp = Compression.parseAttributeMap(flags);
			
			fti.setCompression(comp);
			
			client.transfer();
		} else if ( (m = filelist.matcher(command)).matches()) {
			fti.setType(TransferType.FILELIST);
			Map<String,String> flags= AbstractADCClientProtocolCommand.getCCFlagMap(m.group(2));
			Compression comp = Compression.parseAttributeMap(flags);
			fti.setCompression(comp);
			boolean partialList = m.group(1).startsWith("list /");
			fti.setPartialList(partialList);
			fti.setBz2Compressed(m.group(1).equals("file files.xml.bz2"));
			if (partialList) {
				String path =  AbstractADCCommand.revReplaces(m.group(1).substring(5));
				fti.setPartialFileList(path, "1".equals(flags.get("RE")));
			} 
			
			
			
			client.transfer();
		}  else {
			logger.debug("invalid GET received "+command);
		}

	}
	public static void sendGET(ClientProtocol client) throws IOException {
		
		FileTransferInformation fti = client.getFti();
		if (fti.isValidForGet()) {
			logger.debug("valid fti for get");
			switch(fti.getType()) {
			case FILE:
				client.addCommand(new SND(fti.getHashValue(),fti.getStartposition(),fti.getLength()));
				break;
			case FILELIST:
				client.addCommand(new SND());
				break;
			case TTHL:
				if (client.getOthersSupports().contains("TIGR")) {
					client.addCommand(new SND(fti.getHashValue()));
				} else {
					STA.sendSTA(client, 
							new ADCStatusMessage("Client too old"
									, ADCStatusMessage.FATAL
									, ADCStatusMessage.ProtocolRequiredFeatureMissing
									,Flag.FC,"TIGR"));
					return;
				}
				break;
			}
			
			 //bad workaround -> use simple connection for sending this and next command..

		//	client.setSimpleConnection();
		//	SimpleConnection simple = (SimpleConnection)client.getConnection();
			logger.debug("sending ADCGET channelIsOpen? ");//+simple.retrieveChannel().isOpen());
			String adcget = "CGET "
				+ fti.getType().getAdcString() + " "
				+ (fti.getType() == TransferType.FILELIST ? "/" : "TTH/" + fti.getHashValue())+ " "
				+ fti.getStartposition() + " "
				+ fti.getLength() 
				+ fti.getCompression() 
				+ (fti.getType() == TransferType.FILELIST ? " RE1": "" ) 
				+ "\n" ;

			client.sendUnmodifiedRaw(adcget);
			//simple.send(adcget);
			// Request all bytes from current position to end of file
			logger.debug("read one adc command");
			//now read just one more command -> either MaxedOut or it is  ADCSND
		//	simple.readOneCommand(20000,false);
			
			logger.debug("finished reading one nmdc command");
			
		} else {
			logger.debug("invalid fti");
			client.disconnect(DisconnectReason.ILLEGALSTATEERROR);
		}
	}

}
