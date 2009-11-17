package eu.jucy.gui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

public class FindHandler extends AbstractHandler implements IHandler {

	/**
	 * command id
	 */
	public static final String ID = "eu.jucy.gui.find";
	
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		IEditorPart part = HandlerUtil.getActiveEditorChecked(event);
		ISearchableEditor ise = (ISearchableEditor)part;

		FindDialog dialog = new FindDialog(part.getSite().getShell(), ise);
		
		dialog.open();
		
		return null;
	}

}
