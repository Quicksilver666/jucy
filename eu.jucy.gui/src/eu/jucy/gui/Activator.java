package eu.jucy.gui;





import logger.LoggerFactory;

import org.apache.log4j.Logger;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;


@SuppressWarnings("restriction")
public class Activator extends AbstractUIPlugin {

	private static Logger logger = LoggerFactory.make();
	

	private ServiceRegistration policyRegistration;
	
	// XXX Shared instance of bundle context
	public static BundleContext bundleContext;
	//private ServiceRegistration policyRegistration;
	public Activator() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		bundleContext = context;
		registerP2Policy(context);
		logger.debug("registred P2 policy");
	
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		if (policyRegistration != null)  {
			policyRegistration.unregister();
			policyRegistration = null;
		}
		bundleContext = null;
	}
	
	
	
	private void registerP2Policy(final BundleContext context) {	
		CloudPolicy cp = new CloudPolicy();
		policyRegistration = context.registerService(Policy.class.getName(), cp , null);
		
		/*UIJob uj = new UIJob("add Repos") {

			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
					try {
						CloudPolicy cp = new CloudPolicy(monitor,new URL("http://jucy.eu/p2update").toURI());
						
					} catch(Exception e) {
						logger.warn(e, e);
					}
					return Status.OK_STATUS;
				}
				
		};
		uj.schedule();
		try {
			uj.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} */
		
	
		
	}

	
}
