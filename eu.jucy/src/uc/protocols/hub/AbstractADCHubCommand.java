package uc.protocols.hub;



import helpers.GH;

import java.util.HashMap;
import java.util.Map;

import logger.LoggerFactory;


import org.apache.log4j.Logger;


import uc.User;
import uc.protocols.AbstractADCCommand;
import uc.protocols.IProtocolCommand;

public abstract class AbstractADCHubCommand extends AbstractADCCommand implements IProtocolCommand {

	
	/**
	 * how many captures are used up in the prefix area..
	 */
	public static final int HeaderCapt = 4;
	static Logger logger = LoggerFactory.make();
	
	
	/**
	 * pattern matching the prefix.. (no space)
	 */
	protected final String prefix = "^[BCDEFHIU]"+getPrefix(); 
	
	
	protected final Hub hub;
	

	/**
	 * 
	 * @return a pattern for the header
	 * uses 4 Capturing groups!!
	 * SID is captured by one of these and easily accessible
	 * via getOther(s)SID() and getOther()
	 * 
	 */	
	protected String getHeader() {
		String pref = getPrefix();
		
		String bHeader   = 	"(?:B"+pref+" ("+SID+"))";
		String cihHeader = 	"(?:[CIH]"+pref+"())";
		String deHeader  = 	"(?:[DE]"+pref+" ("+SID+") "+SID+")";
		String fHeader   = 	"(?:F"+pref+" ("+SID+") "+ADCTEXT+")";
		
		return "^(?:"+bHeader+"|"+cihHeader+"|"+deHeader+"|"+fHeader+")";
		
	}
	
	public int getOthersSID() {
		return SIDToInt(getOtherSID());
	}
	
	public String getOtherSID() {
		for (int i =1; i <= 4; i++) {
			String s = matcher.group(i);
			if ( s != null) {
				return s;
			}
		}
		return null;
	}
	
	public User getOther() {
		String sid = getOtherSID();
		if (GH.isEmpty(sid)) {
			return null;
		} else {
			return hub.getUserBySID(SIDToInt(sid));
		}
	}
	
	public AbstractADCHubCommand(Hub hub) {
		this.hub = hub;
	}
	

	
	public static int SIDToInt(String sid) {
		char a = sid.charAt(0);
		char b = sid.charAt(1);
		char c = sid.charAt(2);
		char d = sid.charAt(3);
		
		int sidInt = a + 256*b+ 256*256*c + 256*256*256*d;  
		
		return sidInt;
	}
	
	public static String SIDToStr(int sid) {
		
		char a = (char)	(  sid & 0xff);
		char b = (char)	( (sid & 0xff00)/256);
		char c = (char)	( (sid & 0xff0000)/(256*256));
		char d = (char) ( (sid & 0xff000000)/(256*256*256));
		
		String strSid = Character.toString(a)+Character.toString(b)+
						Character.toString(c)+Character.toString(d);
		
		return strSid;
	}
	
	
	/**
	 * creates a map from a  space separated list of
	 * attributes 
	 * 
	 * @param attributes - list with attributes
	 * @return map keys are 2 chars long prefixes .. values are the letters after
	 * the key without protocol replaces
	 */
	public static Map<Flag,String> getFlagMap(String attributes) {
		String[] splits = space.split(attributes);
		Map<Flag,String> flagValue = new HashMap<Flag,String>();
		for (String s: splits) {
			if (s.length() >= 2) {
				Flag fls = Flag.parse(s.substring(0, 2));
				String value = revReplaces(s.substring(2));
				if (fls != null && fls.verify(value)) {
					flagValue.put(fls,value);
				}
			}
		}
		return flagValue;
	}
	
	
	
}
