package uc.protocols.client;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import logger.LoggerFactory;


import org.apache.log4j.Logger;




import uc.crypto.HashValue;
import uc.files.transfer.FileTransferInformation;
import uc.protocols.AbstractADCCommand;
import uc.protocols.Compression;
import uc.protocols.TransferType;

public class ADCGET extends AbstractNMDCClientProtocolCommand {

	private static Logger logger = LoggerFactory.make(); 

	
	private Pattern file;
	private Pattern interleaves;
	private Pattern filelist;
	
	public ADCGET() {
		file = Pattern.compile(prefix + " file TTH/("+TTH+") ("+FILESIZE+") ("+FILESIZE+"|-1)(.*)"); 
		interleaves = Pattern.compile(prefix + " tthl TTH/("+TTH+") 0 -1(.*)");
		
		String filelista = "file files\\.xml\\.bz2";
		String filelistb = "list /"+TEXT_NOSPACE; 
		String filelists = "(?:(?:"+filelista+")|(?:"+filelistb+"))";
		
		filelist = Pattern.compile(prefix + " ("+filelists+") 0 -1(.*)");
	}

	@Override
	public void handle(ClientProtocol client,String command) throws IOException  {
		//client.removeCommand(this);
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
			logger.debug("invalid ADCGET received "+command + "  "+client.getUser());
		}
	}
	

	
	
	
	public static void sendADCGET(ClientProtocol client) throws IOException {
		if (!client.getOthersSupports().contains("ADCGet")) {
			client.sendError(DisconnectReason.CLIENTTOOOLD);
		}
		
		FileTransferInformation fti = client.getFti();
		if (fti.isValidForGet()) {
			logger.debug("valid fti for get");
			client.addCommand(new MaxedOut()); //TODO maxedOut received should trigger warning if received on FileList or tthl
			switch(fti.getType()) {
			case FILE:
				client.addCommand(new ADCSND(fti.getHashValue(),fti.getStartposition(),fti.getLength()));
				break;
			case FILELIST:
				client.addCommand(new ADCSND());
				break;
			case TTHL:
				if (client.getOthersSupports().contains("TTHL")) {
					client.addCommand(new ADCSND(fti.getHashValue()));
				} else {
					client.disconnect(DisconnectReason.CLIENTTOOOLD);
					return;
				}
				break;
			}
			
			 //bad workaround -> use simple connection for sending this and next command..
			
			
			//client.setSimpleConnection();
		//	SimpleConnection simple = (SimpleConnection)client.getConnection();
			logger.debug("sending ADCGET channelIsOpen? " ); //+simple.retrieveChannel().isOpen());
			String adcget="$ADCGET "
				+ fti.getType().toNMDCString() + " "
				+ (fti.getType() == TransferType.FILELIST ? "files.xml.bz2": "TTH/" + fti.getHashValue())+ " "
				+ fti.getStartposition() + " "
				+ fti.getLength() 
				+ fti.getCompression() 
				+ "|" ;

			

			client.sendUnmodifiedRaw(adcget);
		//	simple.send(adcget);
			// Request all bytes from current position to end of file
			logger.debug("read one nmdc command");
			//now read just one more command -> either MaxedOut or it is  ADCSND
		//	simple.readOneCommand(30000,true);
			
			logger.debug("finished reading one nmdc command");
			
		} else {
			logger.debug("invalid fti");
			client.disconnect(DisconnectReason.ILLEGALSTATEERROR);
		}
	}
	
//	public static void main(String[] args) {
//		String adcget = "$ADCGET file TTH/4CLZLU7TCB6C4YTHN7JNOIA7F7VQVJV5762AYJA 0 457864 ZL1" ;
//		String adcget2 = "$ADCGET file files.xml.bz2 0 -1 ZL1";
//		
//		ADCGET a = new ADCGET();
//		
//		Matcher m = a.file.matcher(adcget);
//		boolean matches = m.matches();
//		
//		System.out.println(matches);
//		if (matches) {
//			for (int i=0 ; i <= 4;i++) {
//				System.out.println(m.group(i));
//			}
//		}
//		Matcher m2 = a.filelist.matcher(adcget2);
//		boolean matches2 = m2.matches();
//		System.out.println(matches2);
//		if (matches) {
//			System.out.println(m2.group(1));
//		}
//		
//	}

}
