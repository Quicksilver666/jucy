package uc.protocols.hub;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.util.Map;
import java.util.Set;



import uc.DCClient;
import uc.User;
import uc.crypto.HashValue;
import uc.files.search.ISearchResult;
import uc.files.search.SearchResult;


/**
 * 
 * 
<h4 id="_res">5.3.7. RES</h4>
<div class="literalblock">
<div class="content">
<pre><tt>RES</tt></pre>
</div></div>
<div class="para"><p>Contexts: F, T, C, U</p></div>
<div class="para"><p>Search result, made up of fields syntactically and structurally similar to the
INF ones. Clients must provide filename, session hash, size and token, but are
encouraged to supply additional fields if available. Passive results should be
limited to 5 and active to 10.</p></div>
<div class="tableblock">
<table cellpadding="4" cellspacing="0" frame="border" rules="all">
<tbody valign="top">
  <tr>

    <td align="left">
    FN
    </td>
    <td align="left">
    Full filename including path in share
    </td>
  </tr>
  <tr>
    <td align="left">
    SI
    </td>

    <td align="left">
    Size, in bytes
    </td>
  </tr>
  <tr>
    <td align="left">
    SL
    </td>
    <td align="left">
    Slots currently available
    </td>

  </tr>
  <tr>
    <td align="left">
    TO
    </td>
    <td align="left">
    Token
    </td>
  </tr>
</tbody>

</table>
</div>
<h4 id="_ctm">5.3.8. CTM</h4>
<div class="literalblock">
<div class="content">

 *
 */
public class RES extends AbstractADCHubCommand {

	
	
	private static RES UDPRES = new RES(null);
	
	public RES(Hub hub) {
		super(hub);  
		String passive = getHeader();
		String active = "URES ("+CID+")";
		setPattern((hub != null?passive:active) +" (.*)",true);
	}


	public void handle(String command) throws ProtocolException, IOException {
		User usr = null;
		Map<Flag,String> flags;
		if (hub != null) {
			usr = hub.getUserBySID(getOthersSID());
			flags = getFlagMap(matcher.group(HeaderCapt+1));
		} else {
			HashValue cid = HashValue.createHash(matcher.group(1));
			usr = hub.getDcc().getUserForCID(cid);
			if (usr == null) {
				logger.debug("Unknown user in RES "+cid);
				return;
			}
			flags = getFlagMap(matcher.group(2));
		}
		
		//TR for tigerhash
		if (usr != null && flags.containsKey(Flag.FN)) {
			String path = flags.get(Flag.FN);
			path = path.replace('/', File.separatorChar);
			ISearchResult sr = null;
			String token = flags.get(Flag.TO);
			if (path.endsWith(File.separator)) { //folder
				long size = flags.containsKey(Flag.SI) ? Long.valueOf(flags.get(Flag.SI)): -1 ;
				int availableSlots = flags.containsKey(Flag.SL)?Integer.valueOf(flags.get(Flag.SL)): 0 ;
				
				
				sr  = SearchResult.create(path, null, usr, size, availableSlots, usr.getSlots(), false,token);
				
			} else {
				if (flags.containsKey(Flag.SI) && flags.containsKey(Flag.TR) ) {
					long size = Long.valueOf(flags.get(Flag.SI));
					int availableSlots = flags.containsKey(Flag.SL)?Integer.valueOf(flags.get(Flag.SL)): 0 ;
					HashValue hash = HashValue.createHash(flags.get(Flag.TR));
					sr  = SearchResult.create(path,hash, usr, size, availableSlots, usr.getSlots(), true,token);
				}
			}
			
			if (sr != null) {
				DCClient.get().srReceived(sr);
			}
		}
	}
	
	public static String getUDPRESString(SearchResult sr, Hub hub) {
		return "URES "+hub.getSelf().getCID()+" "+getPartialRES(sr)+"\n";
		
	}
	
	/**
	 * sends SRs to a passive user back over the hubconnection..
	 * @param srs - the results of the search
	 * @param target - the user that will receive the searchResults
	 * @param hub - the hub that will transmit to the user..
	 */
	public static void sendSR(Set<SearchResult> srs , User target, Hub hub) {
		for (SearchResult sr: srs) {
			hub.sendUnmodifiedRaw("DRES "+SIDToStr(hub.getSelf().getSid())
										+" "+SIDToStr(target.getSid())
										+" "+getPartialRES(sr)+"\n");
			
		}
	}
	
	private static String getPartialRES(SearchResult sr) {
		String path=doReplaces(sr.getPath().replace(File.separatorChar, '/'));
		if (!path.startsWith("/")) {
			path = "/"+path;
		}
		String s = "FN"+ path
		+ " SI"+sr.getSize()
		+ " SL"+sr.getAvailabelSlots();
		
		if (sr.getToken() != null) {
			s+= " TO"+doReplaces(sr.getToken());
		}
		
		if (sr.isFile()) {
			s+= " TR"+sr.getTTHRoot();	
		} 
		return s;
	}
	
	public static void receivedADCRES(InetSocketAddress from, CharSequence cs ) {
		int index;
		String cur = cs.toString();
		while (-1 != (index = cur.indexOf('\n'))) {
			String command = cur.substring(0, index);
			cur = cur.substring(index+1);
			if (UDPRES.matches(command)) {
				try {
					UDPRES.handle(command);
				} catch (IOException ioe) {
					logger.debug(ioe,ioe);
				} catch(RuntimeException re) {
					logger.debug(re, re);
				}
			}
		}
	}
	

}
