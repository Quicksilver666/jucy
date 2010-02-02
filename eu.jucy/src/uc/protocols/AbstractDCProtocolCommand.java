package uc.protocols;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractDCProtocolCommand implements IProtocolCommand {

	protected static final Map<String,Pattern> staticPattern = 
		Collections.synchronizedMap(new HashMap<String, Pattern>());
	

	/**
	 * a pattern that should be used by matches instead of the prefix
	 * to match the command.
	 * if this is set
	 * matches is guaranteed to use this pattern.
	 */
	private Pattern pattern;
	
	protected Matcher matcher;
	
	
	protected void setPattern(String patt,boolean cache) {
		if (cache) {
			Pattern present = staticPattern.get(patt);
			if (present == null) {
				present = Pattern.compile(patt);
				staticPattern.put(patt, present);
			}
			this.pattern = present;
		} else {
			this.pattern = Pattern.compile(patt);
		}
	}
	


	protected Pattern getPattern() {
		return pattern;
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}


	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		return getClass().equals(obj.getClass());
	}
	
}
