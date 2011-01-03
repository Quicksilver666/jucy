package uc.protocols.hub;

import uc.IHub;

/**
 * 
 * A pm filter lets you veto  PMS for going to
 * be displayed to the user, this can be either used as spam filter
 * or what ever purpose is needed...
 * 
 * @author Quicksilver
 *
 */
public interface IPMFilter {
	
	public static final String POINT_ID = "eu.jucy.pmfilter";
	
	/**
	 * A PM filter may veto a PM to go to any further listeners/logs
	 * @param pm A Private Message that has just arrived 
	 * @return true if this pm should be discarded 
	 */
	boolean vetoPM(IHub hub,PrivateMessage pm);
	
	
//	public static enum VETOResult {
//		OK,VETO,VETO_BUT_LOG;
//	}
}
