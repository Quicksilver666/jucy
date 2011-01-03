package uc.protocols.hub;

/**
 * 
 * what kind of feed message we have. ..
 * 
 * should have at least the types used by YnHub
 * like  warning..
 * or kick ... 
 * @author Quicksilver
 *
 */
public enum FeedType {
	KICK,ACTION,REPORT,EVENT,GUI,FATAL,ERROR,WARN,NONE; //
	
	public static FeedType fromString(String type) {
		for (FeedType t: FeedType.values()) {
			if (t.name().equalsIgnoreCase(type)) {
				return t;
			}
		}
		return NONE;
	}
	
	public String toString() {
		if (this == NONE) {
			return "";
		} else {
			return "["+name().toLowerCase()+"]";
		}
	}

}