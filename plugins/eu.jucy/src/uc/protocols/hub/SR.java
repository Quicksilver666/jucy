package uc.protocols.hub;


import helpers.GH;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import logger.LoggerFactory;

import org.apache.log4j.Logger;


import uc.DCClient;
import uc.crypto.HashValue;
import uc.files.search.ISearchResult;
import uc.files.search.SearchResult;
import uc.protocols.DCProtocol;
import uc.user.User;

/**	    * @ sr - a string containing (from dcppWiki):
	    * $SR <source_nick> <result> <free_slots>/<total_slots><0x05><hub_name> (<hub_ip:listening_port>)   //[<0x05><target_nick>] last part stripped away from hub
	    * where: <result> is one of the following: 
        * <file_name><0x05><file_size> for file results
        * <directory> for directory results 
	    * 
	    * ex:
	    *  $SR User1 mypathmotd.txt<0x05>437 3/4<0x05>Testhub (10.10.10.10:411)
	    *  
	    *      
	    *  <result> is one of the following: 
        * <file_name><0x05><file_size> for file results
        * <directory> for directory results 
	    * The <0x05> characters used above for deliminators are the 5th character in the ASCII character set.
	    * Sent by a client when a match to a $Search is found.
	    * If the $Search was a passive one, the $SR is returned via the hub connection (TCP). In this case, <0x05><target_nick> must be included on the end of the $SR. The hub must strip the deliminator and <target_nick> before sending the $SR to <target_nick>. If the search was active, it is sent to the IP address and port specified in the $Search via UDP.
	    * The port for the hub only needs to specified if its listening port is not the default (411).
	    * On UNIX the path delimiter / must be converted to \ for compatibility.
	    * DC++ will send a maximum of 5 search results to passive users and 10 to active users. (we will do the same)
	    * For files containing TTH, the <hub_name> parameter is replaced with TTH:<base32_encoded_tth_hash> (ref: TTH_Hash) 
	   
	   * $SR <source_nick> <result> <free_slots>/<total_slots><0x05><hub_name> (<hub_ip:listening_port>)[<0x05><target_nick>]|
	    * 
	    * $SR User1 mypathmotd.txt<0x05>437 3/4<0x05>TTH:J7HKCPIH..3W (10.10.10.10:411)<0x05>User2|
	    *
	    *
	    *Malformed Command received: $SR schlumpf 1_Daten\Malvorlagen\Windowcolor - Vorlagen\Scan10017.png62253 1/3TTH:L7G5FXZNSYDE3TVFLQHP6ISIIUT6PT2WX4CBUCQ (89.59.92.112)
	    *
	    */
public class SR extends AbstractNMDCHubProtocolCommand {

	private static Logger logger = LoggerFactory.make();
	
	private static final Pattern pipesplit = Pattern.compile(Pattern.quote("|"));
	
	/**
	 * used in search results as separator
	 * this is the RegExp string for detection
	 */
	private static final String CHARFIVE = "\\x05";
	
	/**
	 * the same as char for sending 
	 */
	private static final char FIVESEP = (char) 0x05;

	/*
	 * 
	 * DEBUG ConnectionProtocol.java Line:159 		 
	 * Malformed Command received: $SR [daiz]coldshouldermulder G:_\Documentaries\BBC.Horizon.2008.How.Much.Is.Your.Dead.Body.Worth.DVBc.XviD.MP3.MVGroup.org.avi669853684 2/4TTH:FNY6UH5AOERHL4GEJQBLEY7WXBNLNJFCYA2UBWI (81.16.174.204:7777)
DEBUG ConnectionProtocol.java Line:159 		 
Malformed Command received: $SR [daiz]coldshouldermulder G:_\Documentaries\BBC.Horizon.2008.How.to.Kill.a.Human.Being.DVBC.XviD.MP3.MVGroup.org.avi669845702 2/4TTH:UI36H4BSPDUZ4PNVJU4ZHQGRZQCVRVDI6AS4FZQ (81.16.174.204:7777)
	 */
	/**
	 * pattern that matches a SR that represents a directory
	 */
	private static final Pattern 	directorySR;
	
	/**pattern that matches a SR that represents a file
	 * 
	 */
	private static final Pattern fileSR;
	

	
	static {
		String prefix = "^\\Q$SR\\E";
		String filename = "(?:.{1,255})";
		String slots = "(\\d{1,5})/(\\d{1,5})";
		directorySR = Pattern.compile(prefix + " ("+NMDCNICK+") ("+filename+") "+slots+CHARFIVE+"(?:.*) \\("+IPv4+":"+PORT+"\\)");
		fileSR = Pattern.compile(prefix +" ("+NMDCNICK+") ("+filename+")"+CHARFIVE+"("+FILESIZE+")"+" "+slots+CHARFIVE+"TTH:("+TTH+") \\("+IPv4+":"+PORT+"\\)");
	}
	
	public SR() {
		String filename = "(?:.{1,255})";
		String result =  filename+"(?:"+CHARFIVE+FILESIZE+")?";
		String hubname = "(?:TTH\\:"+TTH+"|(?:.*))";
		String slots = "(\\d{1,5})/(\\d{1,5})";
		setPattern(prefix + " "+NMDCNICK+" "+result+" "+slots+CHARFIVE+hubname+" \\("+IPv4+":"+PORT+"\\)",true);
		
	}
	
	private static void receivedSR(Hub hub,String command) {
		Matcher sr;
		String path;
		if ((sr = fileSR.matcher(command)).matches()) {
			path = transformPath(sr.group(2));
			User other = hub.getUserByNick(sr.group(1));
			if (other != null) { // file
			   ISearchResult searchResult = SearchResult.create(
					   path, 						//filename
					   HashValue.createHash(sr.group(6)),
					   other,
					   Long.parseLong(sr.group(3)),         //file size
					   Integer.parseInt(sr.group(4)),       //slots current
					   Integer.parseInt(sr.group(5)),       //slots max
					   true,null);
			   hub.getDcc().srReceived(searchResult);
			}
		} else if ( (sr = directorySR.matcher(command)).matches()) {
			path = transformPath(sr.group(2));
			User other = hub.getUserByNick(sr.group(1));
			if (other != null) { //directory..
				   ISearchResult searchResult = SearchResult.create(
						   path,							//filename
						   null,
						   other,
						   -1,         							//file size
						   Integer.parseInt(sr.group(3)),       //slots current
						   Integer.parseInt(sr.group(4)),       //slots max
						   false,null);
				   hub.getDcc().srReceived(searchResult);
				
			}
		} else {
			logger.debug("invalid sr string: "+command);
		}
	}

	@Override
	public void handle(Hub hub,String command) throws IOException {
		receivedSR(hub,command);
	}
	
	private static String transformPath(String sent) {
		return GH.replaceInvalidFilpath(sent.replace('\\', File.separatorChar)) ;
	}
	
	
	public static void main(String[] args) {
		String fileSR = "$SR guywithfile lazgapo\\ashda\\strangefile.bmp"
			+((char)0x05)+"4587 2/4"+((char)0x05)
			+"TTH:4CLZLU7TCB6C4YTHN7JNOIA7F7VQVJV5762AYJA (89.48.59.86:6999)";
	
		Matcher m = SR.fileSR.matcher(fileSR);
		boolean matches = m.matches();
		
		System.out.println(matches);
		if (matches) {
			for (int i=0 ; i <= 6;i++)
			System.out.println(m.group(i));
		}
	}
	
	/**
	 * creates the part of the SR string that is needed by active and by passive results
	 * @param sr - the file/folder that was found
	 * @param hub -  the hub in which the search appeared
	 */
	private static String getPartialSRString(SearchResult sr,Hub hub) {
		String result = sr.getPath().replace(File.separatorChar,'\\'); //replace File separaters here with NMDC separater
		if (sr.isFile()) {
		   result += FIVESEP ;
		   result += sr.getSize();

		} else { //if its a folder cut away last file sep
			result = result.substring(0, result.length()-1);
		}
		
		//partial sr string: $SR °^(AAA)Schnueffeltv2^° Musik\DJ Quicksilver - Bellissima.mp33708421 1/1TTH:YUMEHYMQI4XYDLS4YFAGH3ID2RMSWL44CU5HWXA (89.59.75.160:6999)
	
		InetSocketAddress isa = hub.getHubIPAndPort();
		
		String srpart = "$SR "
	   		+ sr.getUser().getNick() + " "
	   		+ result + " "         
	   		+ sr.getAvailabelSlots() +"/" + sr.getTotalSlots() + FIVESEP 	
	   		+ (sr.isFile()? "TTH:"+sr.getTTHRoot() : hub.getName()) + " "     //TTH of the file else
	   		+ "("+isa.getAddress().getHostAddress() + ":"+isa.getPort() +")" ;	//Hub name + ip:port
		 
		logger.debug("partial sr string: "+srpart);
		 
		return srpart;
	}
	
	public static String getUDPSrString(SearchResult sr, Hub hub) {
		return  getPartialSRString(sr,hub)+ "|";
	}
	
	
	
	
	/**
	 * sends SRs to a passive user back over the hubconnection..
	 * @param srs - the results of the search
	 * @param target - the user that will receive the searchResults
	 * @param hub - the hub that will transmit to the user..
	 */
	public static void sendSR(Set<SearchResult> srs , User target, Hub hub) {
		  // String command = "";
		   StringBuilder command = new StringBuilder();
		   if (target != null) {
			   for (SearchResult sr: srs) {
				   command.append(getPartialSRString(sr,hub));
				   command.append(FIVESEP);
				   command.append( target.getNick());
				   command.append( '|');
			   }
		   }
		   if (command.length() > 0) { // send the command if it is not empty..
			  hub.sendUnmodifiedRaw(command.toString());
		   }
	}

	
	 /**
	    * 
	    * - a string containing (from dcppWiki):
	    * $SR <source_nick> <result> <free_slots>/<total_slots><0x05><hub_name> (<hub_ip:listening_port>)   //[<0x05><target_nick>] last part stripped away from hub
	    * where: <result> is one of the following: 
	  * <file_name><0x05><file_size> for file results
	  * <directory> for directory results 
	    * 
	    * ex:
	    *  $SR User1 mypathmotd.txt<0x05>437 3/4<0x05>Testhub (10.10.10.10:411)
	    *  
	    *      
	    *  <result> is one of the following: 
	  * <file_name><0x05><file_size> for file results
	  * <directory> for directory results 
	    * The <0x05> characters used above for deliminators are the 5th character in the ASCII character set.
	    * Sent by a client when a match to a $Search is found.
	    * If the $Search was a passive one, the $SR is returned via the hub connection (TCP). In this case, <0x05><target_nick> must be included on the end of the $SR. The hub must strip the deliminator and <target_nick> before sending the $SR to <target_nick>. If the search was active, it is sent to the IP address and port specified in the $Search via UDP.
	    * The port for the hub only needs to specified if its listening port is not the default (411).
	    * On UNIX the path delimiter / must be converted to \ for compatibility.
	    * DC++ will send a maximum of 5 search results to passive users and 10 to active users. (we will do the same)
	    * For files containing TTH, the <hub_name> parameter is replaced with TTH:<base32_encoded_tth_hash> (ref: TTH_Hash) 
	    *
	 **
	  * splits the packet in searchResult strings
	  * then retrieves nick and hubip from the packet  
	  * and forwards the searchresult string then to
	  * the matching hub
	  * 
	  * @param from - socket of the sender
	  * @param cs - CharSsequence containing the packet
	  */
	public static void receivedNMDCSR(InetSocketAddress from, CharSequence cs ,ByteBuffer originalpacket,DCClient dcc) {
    	for (String sr:  pipesplit.split(cs)) {
    		logger.debug("received sr via udp: "+sr);
    		try {
    			int secondspace = sr.indexOf(' ', 5);
    			int braceOpen = sr.lastIndexOf('(');
    			if (sr.startsWith("$SR ") && -1 != secondspace && secondspace < braceOpen ) { 
    				String nick 	= sr.substring(4, secondspace );
    				String hubip	= sr.substring(braceOpen ,sr.length()-1);
    				Hub hub = dcc.hubForNickAndIP(nick,hubip);
    				if (hub != null) {
    					if (hub.getCharset().equals(DCProtocol.NMDC_CHARSET) || originalpacket == null ) {
    						receivedSR(hub, sr);
    					//	hub.searchResultReceived(sr);
    					} else {
    						receivedNMDCSR(from, hub.getCharset().decode(originalpacket) , null,dcc);
    					}
    					
    				}
    			}
    		} catch(RuntimeException re) {
    			logger.debug("Bad input in Searchresult: "+re+" "+cs);
    		}	
    	}
    }
	
	
	
	
}
