package uihelpers;



import logger.LoggerFactory;

import org.apache.log4j.Logger;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;






/**
 * CommandAction is a bridge
 * it bridges commands  to action
 * and allows handling of a command like an action
 * 
 * 
 * @author Quicksilver
 *
 * @deprecated 
 */
public class CommandAction extends Action implements IWorkbenchAction {

	private static final Logger logger = LoggerFactory.make();
	
	private final IWorkbenchWindow window;
	
	private final String commandId;

	public CommandAction(IWorkbenchWindow window, String commandId) {
		this.window = window;
		this.commandId = commandId;
		setActionDefinitionId(commandId);
		setId(commandId);
		
		IExtensionRegistry reg = Platform.getExtensionRegistry();

		for (IConfigurationElement ce : reg.getConfigurationElementsFor("org.eclipse.ui.commands")) {
			if (commandId.equals(ce.getAttribute("id"))) {
				setText(ce.getAttribute("name"));
				setDescription(ce.getAttribute("description"));
			}
		}
	}

	@Override
	public void run() {
		ICommandService cmdService = (ICommandService) window.getService(
			    ICommandService.class);
		IHandlerService handlerService = (IHandlerService)window.getService(IHandlerService.class);
		
		Command com = cmdService.getCommand(commandId);
		
		ParameterizedCommand paCom= new ParameterizedCommand(com,null);
		
		ExecutionEvent e = handlerService.createExecutionEvent(paCom, null);
		
		try {
			com.executeWithChecks(e);
		} catch(Exception exc) {
			logger.warn(exc, exc);
		}
	}

	public void dispose() {}

	
	
	
}
