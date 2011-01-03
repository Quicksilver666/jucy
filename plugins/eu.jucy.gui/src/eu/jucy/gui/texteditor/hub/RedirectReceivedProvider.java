package eu.jucy.gui.texteditor.hub;

import java.util.HashMap;
import java.util.Map;

import logger.LoggerFactory;


import org.apache.log4j.Logger;
import org.eclipse.ui.AbstractSourceProvider;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.services.ISourceProviderService;

import uc.FavHub;
import uc.HubListenerAdapter;
import uc.IHub;
import uc.protocols.hub.IHubListener;


/**
 * 
 * 
 * http://www.vogella.de/articles/EclipseCommands/article.html
 * 
 * 
 * @author Quicksilver
 *
 */
public class RedirectReceivedProvider extends AbstractSourceProvider {
	
	private static final Logger logger = LoggerFactory.make();
	public final static String REDIRECT_STATE = "eu.jucy.hub.sourceprovider.redirect";
	
	public final static String ENABLED = "ENABLED";
	public final static String DISENABLED = "DISENABLED";

	private boolean enabled = false;
	private IHub current;
	
	private final IHubListener redirectReceived = new HubListenerAdapter() {

		@Override
		public void redirectReceived(FavHub target) {
			if (current != null) {
				setEnabled(current);
			}
			logger.debug("redirect receved: "+current);
		}
		
	};

	
	public RedirectReceivedProvider() {	
	
	}

	public void dispose() {}
	


	@SuppressWarnings("rawtypes")
	public Map getCurrentState() {
		Map<Object,Object> map = new HashMap<Object,Object>(1);
		String value = enabled ? ENABLED : DISENABLED;
		map.put(REDIRECT_STATE, value);
		return map;

	}

	public String[] getProvidedSourceNames() {
		return new String[]{REDIRECT_STATE};
	}
	
	public void setEnabled(IHub hub) {
		if (current != null) {
			current.unregisterHubListener(redirectReceived);
		}
		current = hub;
		enabled = false;
		if (hub != null) {
			enabled =  hub.pendingReconnect();
		 	current.registerHubListener(redirectReceived);
		}
		String value = enabled ? ENABLED : DISENABLED;
		
		fireSourceChanged(ISources.WORKBENCH, REDIRECT_STATE, value);
	}

	
	public static void init(final IWorkbenchWindow window) {
		window.getActivePage().addPartListener(new IPartListener() {
			public void partOpened(IWorkbenchPart part) {}
			
			public void partDeactivated(IWorkbenchPart part) {
				redirectChanged(null,window);
			}
			
			public void partClosed(IWorkbenchPart part) {}
			
			public void partBroughtToTop(IWorkbenchPart part) {
			
				if (part instanceof HubEditor) {
					redirectChanged(((HubEditor)part).getHub(),window);
				} 
			}
			
			public void partActivated(IWorkbenchPart part) {
				partBroughtToTop(part);
			}
		});
	}
	
	public static void redirectChanged(IHub hub,IWorkbenchWindow window) {
		ISourceProviderService service = (ISourceProviderService)window.getService(
				ISourceProviderService.class);
		
		RedirectReceivedProvider commandStateService = (RedirectReceivedProvider) service
		.getSourceProvider(RedirectReceivedProvider.REDIRECT_STATE );
		
		commandStateService.setEnabled(hub);
		
	}

}
