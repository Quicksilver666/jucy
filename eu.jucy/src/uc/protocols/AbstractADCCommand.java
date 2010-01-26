package uc.protocols;


import helpers.GH;


import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;


import uc.User;
import uc.protocols.hub.Flag;
import uc.protocols.hub.INFField;

public abstract class AbstractADCCommand extends AbstractDCProtocolCommand implements IProtocolCommand {
	
	
	public static Map<INFField,String> INFMap(String attributes) {
		String[] splits = space.split(attributes);
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
	
	public static String ReverseINFMap( Map<INFField,String> attributes) {
		String inf = "";
		for (Entry<INFField,String> e: attributes.entrySet()) {
			inf += " "+ e.getKey().name() + doReplaces(e.getValue());
		}
		return inf;
	}
	
	 
	
	/**
	 * 
	 * @param maps
	 * @return string containing all flags with values
	 *  lead by a space so it can be appended to any normal command
	 *  empty string if no flags.
	 */
	public static String getFlagString(Map<Flag,String> maps) {
		String s = "";
		for (Entry<Flag,String> e:maps.entrySet()) {
			s += " "+ e.getKey().name()+doReplaces(e.getValue());
		}
		return s;
	}
	
	public static String getINFString(User usr,INFField... fields ) {
		String s = "";
		for (INFField inff:fields) {
			if (!GH.isEmpty(s)) {
				s += " ";
			}
			s+=inff.name()+ doReplaces(inff.getProperty(usr));
		}
		return s;
	}
	

	
	

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
		for (int i=0 ; i < s.length()-1 ; i++) {
			if (s.charAt(i) == '\\') {
				String repl;
				switch(s.charAt(i+1)) {
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
					throw new IllegalArgumentException();
				}
				s = s.substring(0, i)+ repl+ s.substring(i+2);
				//i++;
			}
		}
		return s;
	}

}
