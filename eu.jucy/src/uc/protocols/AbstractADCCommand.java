package uc.protocols;





import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import logger.LoggerFactory;


import org.apache.log4j.Logger;



import uc.protocols.hub.Flag;
import uc.protocols.hub.INFField;

public abstract class AbstractADCCommand extends AbstractDCProtocolCommand  {
	
	private static Logger logger = LoggerFactory.make(); 
	
	public static Map<INFField,String> INFMap(String attributes) {
		String[] splits = IProtocolCommand.space.split(attributes);
		Map<INFField,String> flagValue = new HashMap<INFField,String>();
		for (String s: splits) {
			if (s.length() >= 2) {
				INFField fls = INFField.parse(s.substring(0, 2));
				String value = revReplaces(s.substring(2));
				if (fls != null && fls.verify(value)) {
					flagValue.put(fls,value);
				} 
			}
		}
		return flagValue;
	}
	
	public static String reverseINFMap( Map<INFField,String> attributes) {
		StringBuilder sb = new StringBuilder(); 
		for (Entry<INFField,String> e: attributes.entrySet()) {
			sb.append(' ');
			sb.append(e.getKey().name());
			sb.append(doReplaces(e.getValue()));
		}
		return sb.toString();
	}
	
	 
	
	/**
	 * 
	 * @param maps
	 * @return string containing all flags with values
	 *  lead by a space so it can be appended to any normal command
	 *  empty string if no flags.
	 */
	public static String getFlagString(Map<Flag,String> maps) {
		StringBuilder sb = new StringBuilder(); 
		for (Entry<Flag,String> e:maps.entrySet()) {
			sb.append(' ');
			sb.append(e.getKey().name());
			sb.append(doReplaces(e.getValue()));
		}
		return sb.toString();
	}
	
//	public static String getINFString(User usr,INFField... fields ) {
//		String s = "";
//		for (INFField inff:fields) {
//			if (!GH.isEmpty(s)) {
//				s += " ";
//			}
//			s+=inff.name()+ doReplaces(inff.getProperty(usr));
//		}
//		return s;
//	}
	

	
	

	public String getPrefix() {
		return getClass().getSimpleName();
	}




	public boolean matches(String command) {
		if (getPattern() != null) {
			matcher = getPattern().matcher(command);
			return matcher.matches();
		} else {
			return command.substring(1).startsWith(getPrefix()) && command.charAt(4) == ' ';
		}
	}
	
	public static String doReplaces(String s) {
		return s.replace("\\", "\\\\").replace(" ", "\\s").replace("\n","\\n");
	}
	
	/**
	 * 
	 * @param s - a string received in ADC
	 * @return s without replaces
	 * @throws IllegalArgumentException - is thrown on illegal replace
	 */
	public static String revReplaces(String s) throws IllegalArgumentException {
		StringBuilder build = new StringBuilder(s);
		for (int i=0 ; i+1 < build.length() ; i++) {
			if (build.charAt(i) == '\\') {
				String repl;
				switch(build.charAt(i+1)) {
				case '\\':
					repl = "\\";
					break;
				case 'n': 
					repl = "\n";
					break;
				case 's':
					repl = " ";
					break;
				default:
					repl= ""+build.charAt(i+1);
					logger.info("invalid replacement found: "+s);
				}
				build.replace(i, i+2, repl);
				//i++;
			}
		}
		return build.toString();
	}

}
