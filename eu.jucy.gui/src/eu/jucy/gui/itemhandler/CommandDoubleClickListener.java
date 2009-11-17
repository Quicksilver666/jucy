package eu.jucy.gui.itemhandler;

import logger.LoggerFactory;

import org.apache.log4j.Logger;
import org.eclipse.core.commands.Command;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.services.IServiceLocator;


public class CommandDoubleClickListener implements IDoubleClickListener {
	
	private static final Logger logger = LoggerFactory.make();
	
	private final String commandID;
	
	public CommandDoubleClickListener(String commandID) {
		this.commandID = commandID;
	}
	
	public void doubleClick(DoubleClickEvent event) {
		
		IServiceLocator isl = PlatformUI.getWorkbench();
		IHandlerService hs = (IHandlerService)isl.getService(IHandlerService.class);
		ICommandService cmdService = (ICommandService) isl.getService(ICommandService.class);
		Command com = cmdService.getCommand(commandID);
		
		try {
			if (com.isEnabled()) {
				hs.executeCommand(commandID, null);
			}
		} catch (Exception e) {
			logger.error(e,e);
		} 

	}

}
