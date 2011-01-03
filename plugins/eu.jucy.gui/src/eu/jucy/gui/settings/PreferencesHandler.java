package eu.jucy.gui.settings;

import java.util.ArrayList;
import java.util.List;

import logger.LoggerFactory;


import org.apache.log4j.Logger;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.ui.IWorkbenchWindow;

import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.handlers.HandlerUtil;





public class PreferencesHandler extends AbstractHandler implements IHandler {

	private static final Logger logger = LoggerFactory.make();
	
	public Object execute(ExecutionEvent event) throws ExecutionException {

		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		
		PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(
				window.getShell(), 
				PersonalInformationPreferencePage.ID, 
				getValidPreferenceIDs(), null);
		dialog.open();

		return null;
	}

	@Override
	public void dispose() {
	}
	
	/**
	 * remove the unwanted preferences..
	 */
	private static String[] getValidPreferenceIDs() {
		List<String> validIds = new ArrayList<String>(); 
		IConfigurationElement[] ce= Platform.getExtensionRegistry().getConfigurationElementsFor("org.eclipse.ui.preferencePages");
		for (IConfigurationElement cele:ce) {
			String s = cele.getAttribute("id");
			validIds.add(s);
			logger.debug(s);
		}
		logger.debug("current size: "+validIds.size());
		validIds.remove("org.eclipse.help.ui.browsersPreferencePage");
		validIds.remove("org.eclipse.help.ui.contentPreferencePage");
	//	validIds.remove("org.eclipse.update.internal.ui.preferences.MainPreferencePage");
		
		validIds.remove("org.eclipse.equinox.security.ui.category");
		validIds.remove("org.eclipse.equinox.security.ui.storage");
		logger.debug("current size: "+validIds.size());
		
	
		
		return validIds.toArray(new String[0]);
	}

/*	public static class OpenPrefsAction extends Action implements IWorkbenchAction {

		private IWorkbenchWindow window;

		public OpenPrefsAction(IWorkbenchWindow window) {
			super("", AbstractUIPlugin.imageDescriptorFromPlugin(
					Application.PLUGIN_ID, IImageKeys.SETTINGS));
			
			Action a = (Action)ActionFactory.PREFERENCES.create(window);
			setText(a.getText());
			setId(a.getId());
			setActionDefinitionId(a.getActionDefinitionId());
			setAccelerator(a.getAccelerator());
		}
		
		@Override
		public void run() {
			PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(
					window.getShell(), 
					PersonalInformationPreferencePage.ID, 
					getValidPreferenceIDs(), null);
			dialog.open();
		}

		
		
		public void dispose() {}
	} */


}
