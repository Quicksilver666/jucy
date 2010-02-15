package eu.jucy.testfragment;

import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import uc.crypto.BloomFilterTest;
import uc.protocol.hub.AdcHubTest;
import uc.protocol.hub.NmdcHubTest;

public class AllTests implements IApplication {

	private static final Class<?>[] TEST_CASES= new Class[] {BloomFilterTest.class,
		//NmdcHubTest.class,
		AdcHubTest.class};
	
	public Object start(IApplicationContext context) throws Exception {
		
		for (Class<?> c:TEST_CASES) {
			Result res = JUnitCore.runClasses(c);
			if (res.wasSuccessful()) {
				System.out.println("Success: "+c.getSimpleName()+"  NrOfTest: "+res.getRunCount()+"  Time: "+(res.getRunTime()/1000)+"sec");
			} else {
				for (Failure f: res.getFailures()) {
					System.out.println(f.getDescription().getDisplayName());
					f.getException().printStackTrace();
				}
				System.out.println("Failure: "+c.getSimpleName());
				break;
			}
		}
		
		return Status.OK_STATUS;
	}

	public void stop() {

	}

}
