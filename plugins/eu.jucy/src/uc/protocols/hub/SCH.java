package uc.protocols.hub;

import helpers.GH;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


import uc.Identity;
import uc.crypto.BASE32Encoder;
import uc.crypto.HashValue;
import uc.crypto.TigerHashValue;
import uc.crypto.UDPEncryption;
import uc.files.filelist.OwnFileList.SearchParameter;
import uc.files.search.FileSearch;
import uc.files.search.SearchType;
import uc.user.User;

public class SCH extends AbstractADCHubCommand {

	//BSCH 4I3EXFV5JCIHE ++bementem ++a ++patikába  invalid command..
	/*BSCH 4I3EXFV5JCIHE TR6SG5KS63UDJFMFUCLJAI6FDHNDGVNHMMXHDIGAI
	 * 
	 * DEBUG ConnectionProtocol.java Line:160 		 Malformed Command received: BSCH 4I3EXFV5JCIHE TRNSUDFZ3BPWD6YBMTKE5IXVR34CZKTZG5XNTZHDQ
TRACE Hub.java Line:463 		 
	 */
	public SCH() {
		setPattern(getHeader()+" (.*)",true); 
	}

	public void handle(Hub hub,String command) throws ProtocolException, IOException {
		if (hub.getFavHub().isChatOnly()) {
			return;
		}
		User usr = getOther(hub);
		if (usr == null || usr.equals(hub.getSelf())) {
			return;
		}
		
		
		String flagstr = matcher.group(HeaderCapt+1);
		Map<Flag,String> flags = getFlagMap(flagstr);
		

		
		Set<String> includes = new HashSet<String>();
		Set<String> excludes = new HashSet<String>();
		Set<String> endings  = new HashSet<String>();
		
		for (String s:flagstr.split(" ")) { //AN,NO,EX
			String val = s.length() > 2? revReplaces( s.substring(2)):"";
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
		
		
		
		boolean active = usr.isUDPActive();
		
		InetSocketAddress ias  = null;
		if (active) {
			if (hub.isIPv6() && usr.getSupports().contains(User.UDP6)) {
				ias = new InetSocketAddress(usr.getI6(),usr.getUDP6Port());
			} else if (hub.isIPv4() && usr.getSupports().contains(User.UDP4)) {
				ias = new InetSocketAddress(usr.getIp(),usr.getUdpPort());
			} else {
				throw new IllegalStateException();
			}
		}
		
		byte[] encryptionKey = null;
		if (flags.containsKey(Flag.KY)) {
			encryptionKey = BASE32Encoder.decode(flags.get(Flag.KY));
		}
		
		
		if (flags.containsKey(Flag.TR)) {
			HashValue hash = HashValue.createHash(flags.get(Flag.TR));
			hub.searchReceived(hash, !active, usr,  ias, flags.get(Flag.TO),encryptionKey);
		} else {
			SearchParameter sp = new SearchParameter(
					includes,excludes,minsize,maxsize,equalssize,endings, onlyDirectories);
			
			hub.searchReceived(sp,!active, usr , ias, flags.get(Flag.TO),encryptionKey);
		}
	}
	
	public static void sendSearch(Hub hub,FileSearch search) {
		//BSCH KAZ4 TRTFMBXT6AFYHJJOGE4OPLFSEWTYJEFFYY6RDWNIA TOauto
		StringBuilder sch= new StringBuilder();
		Identity id = hub.getIdentity();
		if (id.isActive() || id.isIPv6Used()) {
			sch.append("BSCH ").append(SIDToStr(hub.getSelf().getSid()));
			if (hub.isEncrypted() && UDPEncryption.isUDPEncryptionSupported()) {
				appendToSB(sch,Flag.KY,BASE32Encoder.encode(search.getEncryptionKey()));
			}
		} else {
			sch.append("FSCH ").append(SIDToStr(hub.getSelf().getSid()))
				.append(" +").append(User.TCP4);
		}
			

		String[] splits =  space.split(search.getSearchString());
		for (String s: splits) {
			if (s.matches(TigerHashValue.TTHREGEX) && splits.length == 1) {
				appendToSB(sch,Flag.TR,s);
				break;
			} else if (!GH.isEmpty(s)) {
				if (s.startsWith("-") && s.length() > 1) {
					appendToSB(sch,Flag.NO,s.substring(1));
				} else {
					appendToSB(sch,Flag.AN,s);
				}
			}
		}
		for (String s:search.getSearchType().getEndings()) { //add allowed endings
			appendToSB(sch,Flag.EX,s);
		}
		
		if (search.getSize() >= 0) {
			sch.append(' ').append(search.getComparisonEnum().getAdcCC());
			sch.append(Long.toString(search.getSize()));
		}
		
		if (search.getSearchType() == SearchType.FOLDER) {
			appendToSB(sch,Flag.TY,"2");
		}
		if (search.getToken() != null) {
			appendToSB(sch,Flag.TO,search.getToken());
		}
		sch.append('\n');
		hub.sendUnmodifiedRaw(sch.toString());
	}
	
	private static void appendToSB(StringBuilder sb,Flag f,String s) {
		sb.append(' ').append(f.name()).append(doReplaces(s));
	}
	
	

}
