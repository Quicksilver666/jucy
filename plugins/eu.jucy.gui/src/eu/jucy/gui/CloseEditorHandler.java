package eu.jucy.gui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;



/**
 * closes a hub by closing the matching editor...
 * (not closes a hub if another editor is open on the hub..)
 * 
 * @author Quicksilver
 *
 */
public class CloseEditorHandler extends AbstractHandler implements IHandler {

	public static final String COMMAND_ID = "eu.jucy.gui.close";

	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart part = HandlerUtil.getActiveEditorChecked(event);
		part.getSite().getPage().closeEditor(part, false);
		return null;
	}
}
