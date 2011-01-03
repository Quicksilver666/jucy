package eu.jucy.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import logger.LoggerFactory;


import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import uc.protocols.AbstractADCCommand;



/**
 * class that has methods to replace %|line:xyz] statements in a string
 * 
 * @author Quicksilver
 *
 */
public final class ReplaceLine {
	
	private static final Logger logger = LoggerFactory.make(); 
	

	
	private static final Pattern replace = Pattern.compile("(%\\[line:(.+?)\\])");
	
	private final Map<String,String> replacementCache = new HashMap<String,String>();
	
	
	private static class SingletonHolder {
		private static ReplaceLine singleton = new ReplaceLine();
	}
	
	public static ReplaceLine get() {
		return SingletonHolder.singleton;
	}
	
	
	private ReplaceLine() {}
	

	
	
	/**
	 * replaces each %[line:message] statement with
	 * a user entered text
	 * @param command - the complete command that may or may not contain 
	 *  a line statement
	 *  
	 * @return command with replaced line statements
	 *  null if the user refused to provide one replacement
	 */
	public Map<String,String> replaceLines(String command) {
		logger.debug("line sent for replacement: "+command);
		Map<String,String> replacements = new HashMap<String,String>();
		Matcher m = replace.matcher(command);
		int currentpos = 0;
	//	Map<String,String> localReplacementsCache = new HashMap<String,String>();
		while(m.find(currentpos)) {
			String message = m.group(2);
			String fullBox = m.group(1);
			String replacement;
			if (replacements.containsKey(fullBox)) {
				replacement = replacements.get(fullBox);
			} else {
				replacement =  getReplacement(message,!(command.startsWith("$")||command.startsWith("<")));
			}
			
			if (replacement == null) {
				logger.debug("no replacement provided : breaking");
				return null;
			} 
			//command = command.replace(m.group(1), replacement);
	
			replacements.put(fullBox, replacement);
			currentpos = m.end(1)+1;
			
		}
		logger.debug("line returned with replacement: "+command);
		return replacements;
	}
	
	private String getReplacement(String message,boolean mightBeADC) {
		
		
		String recommended = replacementCache.get(message);
		if (recommended == null) {
			recommended = "";
		}
		String mes = mightBeADC? AbstractADCCommand.revReplaces(message):message;//bad hack.. though probably won't do damage..we just assume ADC as protocol..
		String ret = openBox(mes,recommended); 
		
		if (ret != null) {
			replacementCache.put(message, ret);
		} 
		return ret;
	}
	
	private String openBox(String message,String recommended) {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		
		InputDialog ip = new InputDialog(window.getShell(),message, message, recommended, null);
		
		ip.setBlockOnOpen(true);
		if (ip.open() == Dialog.OK) {
			return ip.getValue();
		} else {
			return null;
		}
	}

}
