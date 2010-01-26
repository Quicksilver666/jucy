package uc.protocols.hub;

import helpers.GH;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import uc.User;
import uc.crypto.HashValue;
import uc.crypto.TigerHashValue;
import uc.files.filelist.OwnFileList.SearchParameter;
import uc.files.search.FileSearch;
import uc.files.search.SearchType;

public class SCH extends AbstractADCHubCommand {

	//BSCH 4I3EXFV5JCIHE ++bementem ++a ++patikába  invalid command..
	/*BSCH 4I3EXFV5JCIHE TR6SG5KS63UDJFMFUCLJAI6FDHNDGVNHMMXHDIGAI
	 * 
	 * DEBUG ConnectionProtocol.java Line:160 		 Malformed Command received: BSCH 4I3EXFV5JCIHE TRNSUDFZ3BPWD6YBMTKE5IXVR34CZKTZG5XNTZHDQ
TRACE Hub.java Line:463 		 
	 */
	public SCH(Hub hub) {
		super(hub);
		setPattern(getHeader()+" (.*)",true); 
	}

	public void handle(String command) throws ProtocolException, IOException {
		User usr = getOther();
		if (usr == null) {
			return;
		}
		
		
		String flagstr = matcher.group(HeaderCapt+1);
		Map<Flag,String> flags = getFlagMap(flagstr);
		

		
		Set<String> includes = new HashSet<String>();
		Set<String> excludes = new HashSet<String>();
		Set<String> endings  = new HashSet<String>();
		
		for (String s:flagstr.split(" ")) { //AN,NO,EX
			String val = revReplaces(s.substring(2));
			if (s.startsWith("AN")) {
				includes.add(val);
			} else if(s.startsWith("NO")) {
				excludes.add(val);
			} else if (s.startsWith("EX")) {
				endings.add(val);
			}
		}
		long minsize = 0,maxsize = Long.MAX_VALUE,equalssize = -1;
		if (flags.containsKey(Flag.LE)) {
			minsize = Long.valueOf(flags.get(Flag.LE));
		}
		if (flags.containsKey(Flag.GE)) {
			maxsize = Long.valueOf(flags.get(Flag.GE));
		}
		if (flags.containsKey(Flag.EQ)) {
			equalssize = Long.valueOf(flags.get(Flag.EQ));
		}
		boolean onlyDirectories = false;
		if (flags.containsKey(Flag.TY)) {
			onlyDirectories = flags.get(Flag.TY).equals("2");  //only files is currently ignored..
		}
		InetAddress ia = usr.getIp();
		
		boolean passive = !(ia != null  && usr.getUdpPort() != 0);
		InetSocketAddress ias  = null;
		if (!passive) {
			ias = new InetSocketAddress(ia,usr.getUdpPort());
		}
		
		
		
		if (flags.containsKey(Flag.TR)) {
			HashValue hash = HashValue.createHash(flags.get(Flag.TR));
			hub.searchReceived(hash, passive, usr,  ias, flags.get(Flag.TO));
		} else {
			SearchParameter sp = new SearchParameter(
					includes,excludes,minsize,maxsize,equalssize,endings, onlyDirectories);
			
			hub.searchReceived(sp,passive, usr , ias, flags.get(Flag.TO));
		}
	}
	
	public static void sendSearch(Hub hub,FileSearch search) {
		//BSCH KAZ4 TRTFMBXT6AFYHJJOGE4OPLFSEWTYJEFFYY6RDWNIA TOauto
		String sch;
		if (hub.getDcc().isActive()) {
			sch = "BSCH "+SIDToStr(hub.getSelf().getSid());
		} else {
			sch = "FSCH "+SIDToStr(hub.getSelf().getSid())+" +"+(hub.getDcc().isIPv4Used()?User.TCP4:User.TCP6);
		}
			

		String[] splits =  space.split(search.getSearchString());
		for (String s: splits) {
			if (s.matches(TigerHashValue.TTHREGEX) && splits.length == 1) {
				sch+= " TR"+s;
				break;
			} else if (!GH.isEmpty(s)) {
				if (s.startsWith("-") && s.length() > 1) {
					sch+= " NO"+ doReplaces(s.substring(1)) ;
				} else {
					sch+= " AN"+ doReplaces(s) ;
				}
			}
		}
		for (String s:search.getSearchType().getEndings()) { //add allowed endings
			sch+=" EX"+doReplaces(s);
		}
		
		if (search.getSize() >= 0) {
			sch+=" "+search.getComparisonEnum().getAdcCC()+search.getSize();
		}
		
		if (search.getSearchType() == SearchType.Folder) {
			sch+=" TY2";
		}
		if (search.getToken() != null) {
			sch+=" TO"+doReplaces(search.getToken());
		}
		
		hub.sendUnmodifiedRaw(sch+"\n");
	}
	
	
	

}
