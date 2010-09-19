package uihelpers;


import org.eclipse.core.commands.Command;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.services.IServiceLocator;

public class CommandButton {

	/**
	 * sets the buttons text to the Name of the provided command
	 * the tootip to its description
	 * and registers a listener on the button that will execute the provided command if 
	 * button is selected 
	 * 
	 * @param commandID - which command should be executed on selection and should be used for caption of the button
	 * @param button button to be decorated
	 * @param isl the servicelocator i.e. workbenchpartsite or WorkbenchWindow
	 * @param setTooltip  if the button should get the description of the command set as tooltip too or not
	 */
	public static void setCommandToButton(final String commandId,Button button,final IServiceLocator isl,boolean setTooltip) {
		IExtensionRegistry reg = Platform.getExtensionRegistry();
		
		for (IConfigurationElement ce : reg.getConfigurationElementsFor("org.eclipse.ui.commands")) {
			if (commandId.equals(ce.getAttribute("id"))) {
				button.setText(ce.getAttribute("name"));
				if (setTooltip) {
					button.setToolTipText(ce.getAttribute("description"));
				}
				break;
			}
		}
		
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IHandlerService handlerService = (IHandlerService)isl.getService(IHandlerService.class);
				ICommandService cmdService = (ICommandService) isl.getService(
					    ICommandService.class);
				Command com = cmdService.getCommand(commandId);
				if (com.isEnabled()) {
					try {
						handlerService.executeCommand(commandId, null);
					} catch(Exception nee) {
						nee.printStackTrace();
					}
				}
			}
			
		});
		
	}
	
}
