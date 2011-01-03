package eu.jucy.op.coop;

import uc.IHub;
import uc.protocols.hub.IPMFilter;
import uc.protocols.hub.PrivateMessage;

public class CoOpMessages implements IPMFilter {

	public static final String PREFIX = "$COP ";
	public static final String POSTFIX = " COP$";

	@Override
	public boolean vetoPM(IHub hub, PrivateMessage pm) {
		if (pm.getMessage().startsWith(PREFIX) && pm.getMessage().endsWith(POSTFIX)) {
			
			
			return true;
		} else {
			return false;
		}
	}

}
