/**
 * 
 */
package eu.jucy.gui.search;

import java.util.Collections;



import helpers.GH;


import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import org.eclipse.ui.IWorkbenchWindow;


import uc.crypto.HashValue;

import eu.jucy.gui.GuiHelpers;
import eu.jucy.gui.OpenEditorHandler;

public class OpenSearchEditorHandler extends OpenEditorHandler {
	
	
	
	public static final String COMMAND_ID = "eu.jucy.gui.OpenSearchEditor";
	public static final String INITIAL_SEARCH= "eu.jucy.gui.initialsearch";
	
	public OpenSearchEditorHandler() {
		super(SearchEditor.ID, new SearchEditorInput());
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String search = event.getParameter(INITIAL_SEARCH);
		
		if (!GH.isNullOrEmpty(search)) {
			if (HashValue.isHash(search)) {
				input =  new SearchEditorInput(HashValue.createHash(search));
			} else {
				input =  new SearchEditorInput(search);
			}
		} else {
			input = new SearchEditorInput();
		}
		return super.execute(event);
	}
	
	/**
	 * 
	 * @param window  where to open it
	 * @param initialsearch - if initially there should be something on search
	 * may be null for nothing
	 */
	public static void openSearchEditor(IWorkbenchWindow window,String initialsearch) {
		GuiHelpers.executeCommand(window
				,OpenSearchEditorHandler.COMMAND_ID
				, Collections.singletonMap(OpenSearchEditorHandler.INITIAL_SEARCH, initialsearch));
		
//		IHandlerService handlerService = (IHandlerService) window.getService(IHandlerService.class);
//		ICommandService comservice = (ICommandService)window.getService(ICommandService.class);
//		
//		try {
//			
//			Command com = comservice.getCommand(OpenSearchEditorHandler.COMMAND_ID);
//			ParameterizedCommand p = ParameterizedCommand.generateCommand(
//					com, Collections.singletonMap(OpenSearchEditorHandler.INITIAL_SEARCH, initialsearch));
//			
//			handlerService.executeCommand(p, null);
//			
//		} catch (Exception e1) {
//			logger.warn(e1,e1);
//		}
	}
	
}