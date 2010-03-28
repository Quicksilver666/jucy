package eu.jucy.gui.itemhandler;

import logger.LoggerFactory;


import org.apache.log4j.Logger;
import org.eclipse.core.expressions.PropertyTester;

import uc.IHasUser;
import uc.IUser;

public class PropertyTesterUser extends PropertyTester {

	private static final Logger logger = LoggerFactory.make();
	
	public PropertyTesterUser() {
		
	}

	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		logger.debug("receiver: "+receiver.getClass().getName());
		logger.debug("properties: "+property);
		logger.debug("args: "+args.length);
		logger.debug("expectedValue: "+expectedValue.getClass().getName());  
		 
		
		IUser usr = ((IHasUser)receiver).getUser();
		if ("isFavUser".equals(property)) {
			return expectedValue.equals(usr.isFavUser());
		} else if ("hasDownloadedFilelist".equals(property)) {
			return expectedValue.equals(usr.hasDownloadedFilelist());
		} else if ("hasFilesInQueue".equals(property)) {
			return expectedValue.equals(usr.nrOfFilesInQueue() > 0);
		} else if ("hasSlotGranted".equals(property)) {
			return expectedValue.equals(usr.hasCurrentlyAutogrant());
		} else if ("isHubKnown".equals(property)) {
			return expectedValue.equals(usr.getHub() != null);
		} else if ("isIPKnown".equals(property)) {
			return expectedValue.equals(usr.getIp() != null);
		} else if ("isShareing".equals(property)) {
			return expectedValue.equals(usr.getShared() > 0 || usr.getNumberOfSharedFiles() > 0);
		}
		
		throw new IllegalStateException();
	}

}
