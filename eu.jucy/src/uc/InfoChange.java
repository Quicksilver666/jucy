package uc;

import java.util.Set;


/**
 * 
 * Enum that describes what kind of MyInfo change
 * occurred
 * 
 * @author Quicksilver
 *
 */
public enum InfoChange {

	CurrentSlots(), //if the current amount of slots changed (not total slots)
	Hubs(20), //change of normal to OP-hub or change on amount of hubs
	Sharesize(60),   //different share size
	Slots(10), //change in total slots..
	Misc(10) ; //change in miscellaneous stuff 
	 	//,Description .. email .. and alike user initiated.. therefore faster updated than it would need..
	
	
	private final long delay;
	private final boolean separateRefresh;
	

	private InfoChange(long secondsDelay) {
		delay = secondsDelay * 1000;
		this.separateRefresh = false;
	}
	private InfoChange() {
		delay = 0;
		separateRefresh = true;
	}

	public long getDelay() {
		return delay;
	}
	
	public boolean isSeparateRefresh() {
		return separateRefresh;
	}
	
	public static interface IInfoChanged {
		void infoChanged(Set<InfoChange> type);
	}
	
	
}
