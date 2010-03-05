package loader;


import java.security.Provider;
import java.security.Security;


import logger.LoggerFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;


public class Activator extends Plugin {

	private static Logger logger = LoggerFactory.make(Level.DEBUG);
	
	public Activator() {
		super();
	}

	

	public void start(BundleContext bc) throws Exception {
		logger.debug("Bouncycastle installed ");
		super.start(bc);
		int i = 0;
		for (Provider p :Security.getProviders()) {
			i++;
			logger.debug(i+": "+p.getName());
			if (p.getName().contains("JSSE")) {
				break;
			}
		//	System.out.println(i+": "+p.getName());
			logger.debug(i+": "+p.getName());
		}
		Security.insertProviderAt(new BouncyCastleProvider(), i);
		//Security.addProvider(new BouncyCastleProvider());
	}

	
	@Override
	public void stop(BundleContext context) throws Exception {
		logger.debug("Bouncycastle uninstalled");
		super.stop(context);
		Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
	}
	
	

}
