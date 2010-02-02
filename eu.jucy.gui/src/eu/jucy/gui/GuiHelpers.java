package eu.jucy.gui;


import org.apache.log4j.Logger;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ParameterizedCommand;


import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;





import java.text.SimpleDateFormat;
import java.util.Date;

import logger.LoggerFactory;



public final class GuiHelpers {
	
	private static final Logger logger = LoggerFactory.make();


	/**
	 * never instantiated
	 *
	 */
	private GuiHelpers(){}

	
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM HH:mm:ss");
	
	
	
	
	public static String dateToString(Date date){
		return dateFormat.format(date);
	} 
	
	
	/**
	 * 
	 * @param howMuch  how much we want..
	 * @param max equals 100%
	 * @return
	 */
	public static String toPercentString(long howMuch , long max ){
		if (howMuch == 0) {
			return "0%";
		}
		float percentage= (((float)howMuch*100f) / max) ;
		String perc = Float.toString(percentage);
		
		if (percentage < 100) {
			return perc.substring(0, perc.indexOf('.')+2 )+"%";
		} else if (percentage > 100) {
			return perc.substring(0, perc.indexOf('.') )+"%";
		} else {
			return "100%";
		}
			
	}


	public static void copyTextToClipboard(String text) {
		Display d = Display.getCurrent();
		if (d != null) {
			Clipboard clipboard= new Clipboard(d);
			TextTransfer textTransfer = TextTransfer.getInstance();
			clipboard.setContents(new Object[]{text}, new Transfer[]{textTransfer});
			clipboard.dispose();
		}
	}
	
	private static int getNumberOfLines(String text) {
		int count =0;
		for (int i = 0; i < text.length(); i++) {
			if (text.charAt(i) == '\n') {
				count++;
			}
		}
		return count;
	}

	public static String getLastXLines(String text, int lines) {
		while (getNumberOfLines(text) > lines) {
			int i = text.indexOf('\n');
			text = text.substring(i+1);
		}
		return text;
	}
	
	/**
	 * little helper executes given command, logs failures
	 * 
	 * @param commandID
	 */
	public static void executeCommand(String commandId) {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		
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

}
