package eu.jucy.testfragment;

import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import uc.crypto.BloomFilterTest;
import uc.protocol.hub.AdcHubTest;

public class AllTests implements IApplication {

	public Object start(IApplicationContext context) throws Exception {
		
		
		Result res = JUnitCore.runClasses(BloomFilterTest.class,
	              AdcHubTest.class
	              );

		if (res.wasSuccessful()) {
			System.out.println("Success");
		} else {
			for (Failure f: res.getFailures()) {
				System.out.println(f.getDescription().getDisplayName());
				f.getException().printStackTrace();
				break;
			}
			System.out.println("Failure");
		}
		return Status.OK_STATUS;
	}

	public void stop() {

	}

}
