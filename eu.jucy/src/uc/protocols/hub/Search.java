package uc.protocols.hub;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import logger.LoggerFactory;


import org.apache.log4j.Logger;




import uc.DCClient;
import uc.IUser;
import uc.User;
import uc.crypto.HashValue;
import uc.files.filelist.OwnFileList.SearchParameter;
import uc.files.search.FileSearch;
import uc.files.search.SearchType;
import uc.protocols.ConnectionProtocol;
import uc.protocols.DCProtocol;

public class Search extends AbstractNMDCHubProtocolCommand {

	private static Logger logger = LoggerFactory.make();
	

	
	private static final Pattern 	//splits search terms
			dollarsplit		= Pattern.compile(Pattern.quote("$"));
	
	/**
	 * search for a TTH
	 */
	private final Pattern tthSearch;
	
	/**
	 * usual search
	 */
	private final Pattern normalSearch;
	
	
	public Search(Hub hub) {
		super(hub);
		String filesearchending	= "(?:([1-8])\\?(.*))";	//capture for the search term
		String tthending 		= "(?:(9)\\?TTH:("+TTH+"))";		//capture for TTH
		
		String activewho  		= "(?:"+IPv4+":"+PORT+")";
		String passivewho 		= "(?:Hub:"+NICK+")"; 
		String who 				= "(?:"+activewho+"|"+passivewho+")";

		
		tthSearch = Pattern.compile(prefix+" ("+who+") "+"([TF])\\?([TF])\\?("+FILESIZE+")\\?"+tthending);
		normalSearch = Pattern.compile(prefix+" ("+who+") "+"([TF])\\?([TF])\\?("+FILESIZE+")\\?"+filesearchending);
		
	}

	
	/**
	* $Search 17.12.85.3:3746 F?T?0?9?TTH:TO32WPD6AQE7VA7654HEAM5GKFQGIL7F2BEKFNA  active
	* $Search Hub:SomeNick F?T?0?9?TTH:TO32WPD6AQE7VA7654HEAM5GKFQGIL7F2BEKFNA  passive
	* <search string>  ::= <size restricted>?<is max size>?<size>?<data type>?<search pattern>
	* 
	* called from command received when a search command was received from the hub
	* 
    * @param command - the search command
    */
	@Override
	public void handle(String command) throws IOException {
		logger.debug("searchreceived("+command+")");
		Matcher m = null;
		boolean passive;

		//Set<FilelistFile> found;
		if ((m = normalSearch.matcher(command)).matches()) {
			boolean sizerestricted	= m.group(2).equals("T");
			boolean maxsize 		= m.group(3).equals("T"); 
			long size 				= Long.parseLong(m.group(4));
			SearchType searchType	= SearchType.getNMDC(Integer.parseInt(m.group(5)));
			String[] searchkeys		= DCProtocol.reverseReplaces(dollarsplit.split(m.group(6)));
			HashSet<String> keys = new HashSet<String>(Arrays.<String>asList(searchkeys));
			keys.remove("");
			keys.remove(".");
			
			passive = m.group(1).startsWith("Hub:");
			User searcherUsr = null;
			InetSocketAddress searcherIp = null;
			if (passive) {
				searcherUsr = hub.getUserByNick(m.group(1).substring(4));
			} else {
				searcherIp = ConnectionProtocol.inetFromString(m.group(1), 412);
			}
			
			long minSize = sizerestricted && !maxsize ? size : 0 ;//  (maxsize?   0  :	size	   ) : 		0; 
			long maxSize = sizerestricted &&  maxsize ? size : Long.MAX_VALUE; //    (maxsize? size : Long.MAX_VALUE) : Long.MAX_VALUE; 
			
			SearchParameter sp = new SearchParameter(keys,Collections.<String>emptySet(),minSize,
					maxSize,-1,searchType.getEndings(),searchType.equals(SearchType.Folder));
			
			hub.searchReceived(sp, passive, searcherUsr,searcherIp,null,null);
			
			
		} else if ((m = tthSearch.matcher(command)).matches()) {
			HashValue tth = HashValue.createHash(m.group(6));
			
			passive = m.group(1).startsWith("Hub:");
			
			User searcherUsr = null;
			InetSocketAddress searcherIp = null;
			if (passive) {
				searcherUsr = hub.getUserByNick(m.group(1).substring(4));
			} else {
				searcherIp = ConnectionProtocol.inetFromString(m.group(1), 412);
			}
			
			hub.searchReceived(tth, passive, searcherUsr,searcherIp,null,null);
			
			
		} else {
			logger.debug("illegal search received: "+command);
			return;
		}
		
	}
	
	
/*	public static void main(String[] args) {
		String search =  "$Search 89.48.12.197:3333 F?T?0?9?TTH:4CLZLU7TCB6C4YTHN7JNOIA7F7VQVJV5762AYJA";
		//String search2 = "$Search 89.48.12.197:3333 F?T?0?5?norm$als$earch";
		Search sr = new Search(null);
		Matcher m = sr.tthSearch.matcher(search);
		boolean matches = m.matches();
		
		System.out.println(matches);
		if (matches) {
			for (int i=0 ; i <= 6;i++)
			System.out.println(m.group(i));
		}
		
	} */
	
	
	/** ex: nmdcsearch from dcpp wiki
	 * $Search 64.78.55.32:412 T?T?500000?1?Gentoo$2005  active
	 * $Search Hub:SomeNick T?T?500000?1?Gentoo$2005   passive
	 * $Search 17.12.85.3:3746 F?T?0?9?TTH:TO32WPD6AQE7VA7654HEAM5GKFQGIL7F2BEKFNA  active
	 * $Search Hub:SomeNick F?T?0?9?TTH:TO32WPD6AQE7VA7654HEAM5GKFQGIL7F2BEKFNA  passive
	 * 
	 * $Search <ip>:<port> <searchstring>               active
	 * $Search Hub:<requestornick> <searchstring>      passive   <requestornick> is the Nick of the Passive User doing the Search.
	 * 
	 * <searchstring>  ::= <sizerestricted>?<ismaxsize>?<size>?<datatype>?<searchpattern>
	 * 
	 * 
	 * Search sends a search to the hub..
	 * registering listener is done somewhere else! @see Search
	 * 
	 * @param search - the search command pattern
	 */
	public static void sendSearch(Hub hub,FileSearch search) {
		DCClient dcc = hub.getDcc();
		IUser self = hub.getSelf();
		String command="$Search "+ 
		(dcc.isActive() ?  
				self.getIp().getHostAddress()
				+":"+self.getUdpPort()+" ":
		"Hub:%[myNI] ");
		if (search.getSize() == -1) {
			command +=  "F?F?0"  ;
		} else {
			command += "T?"+search.getComparisonEnum().getNMDCC()+"?"+search.getSize();
		}
		command += "?"+search.getSearchType().getNMDC()+"?";  


		String[] words = DCProtocol.doReplaces(search.getSearchString()).split(" ");
		StringBuilder searchstring= new StringBuilder();
		for (String word: words) {  //remove all strings starting with -
			if (!word.startsWith("-")) {
				searchstring.append(word).append('$');  
			}
		}
		//if the search is too short we don't send it...
		if (searchstring.length() > 3 ) {
			searchstring.deleteCharAt(searchstring.length()-1);
			//searchstring =  searchstring.substring(0, searchstring.length()-1); //cut away the last $ produced in loop above
			//finish the command by adding the produced search pattern
			command += (search.getSearchType() == SearchType.TTH ?  "TTH:":"");
			command +=  searchstring+"|";
			hub.sendRaw(command); //command is finished...so send it..
			logger.debug("sending search: "+command);
		}  
	}
	

}
