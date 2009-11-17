/**
 * 
 */
package uc.listener;

import eu.jucy.language.LanguageKeys;
import uc.IUser;


/**
 * @author Quicksilver
 *
 */
public interface IUserChangedListener {
	
	public static enum UserChange {
		CONNECTED(LanguageKeys.UserConnected),
		DISCONNECTED(LanguageKeys.UserDisconnected),
		QUIT(LanguageKeys.UserLeft),
		CHANGED;
		
		private final String languageKey;
		
		UserChange(String key) {
			languageKey = key;
		}
		UserChange() {
			this(null);
		}
		
		public String toString() {
			if (languageKey == null) {
				return name();
			}
			return languageKey;
		}
		
	}

	
	/**
	 * fired by hub on receive of a MyINFO or a Quit to signal that the presented user has changed..
	 * 
	 * @param changed - the user that changed...
	 */
	void changed(UserChangeEvent uce); //IUser changed, UserChange typeOfChange);
	
	

	public static class UserChangeEvent {
		
		
		public static final int 
		NotApplicable = 0, //for connected, quit,disconnected...
		INF = 1,			//change of an INF field
		
		SLOT_GRANTED 		=	1025, // last bit one means added 0 = removed.. 
		SLOTGRANT_REVOKED 	=	1024,
		SLOTGRANT_CHANGED	=	1026,
		
		FAVUSER_ADDED		=	2049,
		FAVUSER_REMOVED		=	2048,
		
		DOWNLOADQUEUE_ENTRY_PRE_ADD_FIRST = 4097,	//called if soon the first DQE is to be added..
		DOWNLOADQUEUE_ENTRY_POST_REMOVE_LAST = 4096,	//called if recently the last DQE was removed
		
		DOWNLOADQUEUE_ENTRY_ADDED = 4099,
		DOWNLOADQUEUE_ENTRY_REMOVED = 4098
		;
		
		private final IUser changed;
		private final UserChange type;
		private final int detail;
		
		public UserChangeEvent(IUser changed, UserChange type, int detail) {
			super();
			this.changed = changed;
			this.type = type;
			this.detail = detail;
		}
		
		public UserChangeEvent(IUser changed, UserChange type) {
			this(changed,type,NotApplicable);
		}
		
		public IUser getChanged() {
			return changed;
		}
		public UserChange getType() {
			return type;
		}
		public int getDetail() {
			return detail;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((changed == null) ? 0 : changed.hashCode());
			result = prime * result + detail;
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			UserChangeEvent other = (UserChangeEvent) obj;
			if (changed == null) {
				if (other.changed != null)
					return false;
			} else if (!changed.equals(other.changed))
				return false;
			if (detail != other.detail)
				return false;
			if (type == null) {
				if (other.type != null)
					return false;
			} else if (!type.equals(other.type))
				return false;
			return true;
		}
		
		
	}
	
}
