package eu.jucy.gui.texteditor.hub;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.ui.handlers.HandlerUtil;

import uc.IHub;

public class FollowRedirectHandler extends AbstractHandler implements IHandler {


	public Object execute(ExecutionEvent event) throws ExecutionException {
		IHub hub = ((HubEditor)HandlerUtil.getActiveEditorChecked(event)).getHub();

		if (hub.pendingReconnect()) {
			hub.followLastRedirect(true);
		}
		return null;
	}

}
