package eu.jucy.gui;






import java.net.URI;
import java.net.URISyntaxException;

import logger.LoggerFactory;

import org.apache.log4j.Logger;


import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.ui.Policy;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import eu.jucy.gui.update.CloudPolicy;
import eu.jucy.gui.update.ProvSDKMessages;


public class Activator extends AbstractUIPlugin {

	private static Logger logger = LoggerFactory.make();
	

	private ServiceRegistration<?> policyRegistration;
	
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
			@SuppressWarnings("unchecked")
			ServiceReference<IProvisioningAgent> sref = (ServiceReference<IProvisioningAgent>)context.getServiceReference(IProvisioningAgent.SERVICE_NAME);
			IProvisioningAgent agent  = context.getService(sref);
			
			IMetadataRepositoryManager manager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
			IArtifactRepositoryManager artManager = (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
			
			try {
			//	String nickname = "unstable";
				URI[] repos = new URI [] {	new URI("http://jucy.eu/p2/test_update"),
											new URI("http://jucy.eu/p2/test_extensions")};
				for (URI repo: repos) {
					
					manager.addRepository(repo);
					artManager.addRepository(repo);
					
//					ProvisioningUtil.addMetadataRepository(repo, true);
//					ProvisioningUtil.addArtifactRepository(repo, true);
//					ProvisioningUtil.setMetadataRepositoryProperty(repo, 
//							IRepository.PROP_NICKNAME, nickname);
//					ProvisioningUtil.setArtifactRepositoryProperty(repo, 
//							IRepository.PROP_NICKNAME, nickname);
					
				}
			} catch (URISyntaxException e) {
				throw new IllegalStateException(e);
			} 
//			catch (ProvisionException e) {
//				e.printStackTrace();
//			}
		}
		
	}

	public static IStatus getNoSelfProfileStatus() {
		return new Status(IStatus.WARNING, Application.PLUGIN_ID, ProvSDKMessages.ProvSDKUIActivator_NoSelfProfile);
	}
	
}
