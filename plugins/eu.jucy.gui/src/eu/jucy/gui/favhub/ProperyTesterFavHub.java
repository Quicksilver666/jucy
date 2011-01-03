package eu.jucy.gui.favhub;

import org.eclipse.core.expressions.PropertyTester;

import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;

import uc.FavHub;

public class ProperyTesterFavHub extends PropertyTester {

	public ProperyTesterFavHub() {}

	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		
		FavHub fh = (FavHub) receiver;
		
		if ("isConnected".equals(property)) {
			return expectedValue.equals(fh.isConnected(ApplicationWorkbenchWindowAdvisor.get()));
		}
		
		throw new IllegalStateException();
	}

}
