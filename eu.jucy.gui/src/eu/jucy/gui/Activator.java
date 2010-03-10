package eu.jucy.gui;





import java.net.URI;
import java.net.URISyntaxException;

import logger.LoggerFactory;

import org.apache.log4j.Logger;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.repository.IRepository;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import eu.jucy.gui.update.CloudPolicy;



@SuppressWarnings("restriction")
public class Activator extends AbstractUIPlugin {

	private static Logger logger = LoggerFactory.make();
	

	private ServiceRegistration policyRegistration;
	
	//  Shared instance of bundle context
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
	
	
	private void registerP2Policy(final BundleContext context)  {	
		CloudPolicy cp = new CloudPolicy();
		policyRegistration = context.registerService(Policy.class.getName(), cp , null);
		
		if (GUIPI.getBoolean(GUIPI.allowTestRepos)) {
			try {
				String nickname = "unstable";
				URI[] repos = new URI [] {	new URI("http://jucy.eu/p2/test_updates"),
											new URI("http://jucy.eu/p2/test_extensions")};
				for (URI repo: repos) {
					ProvisioningUtil.addMetadataRepository(repo, true);
					ProvisioningUtil.addArtifactRepository(repo, true);
					ProvisioningUtil.setMetadataRepositoryProperty(repo, 
							IRepository.PROP_NICKNAME, nickname);
					ProvisioningUtil.setArtifactRepositoryProperty(repo, 
							IRepository.PROP_NICKNAME, nickname);
					
				}
			} catch (URISyntaxException e) {
				e.printStackTrace();
			} catch (ProvisionException e) {
				e.printStackTrace();
			}
		}
		
	}

	
}
