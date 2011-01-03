package eu.jucy.gui.statusline;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;

import uc.PI;

public class SetSpeedHandler extends AbstractHandler implements IHandler {

	public static final String 	SPEED= "SPEED",
								UPLIMIT = "UPLIMIT",
								COMMAND_ID = "eu.jucy.gui.setspeedlimit";
	
	public Object execute(ExecutionEvent event) throws ExecutionException {
		int i = Integer.parseInt(event.getParameter(SPEED));
		boolean up = Boolean.parseBoolean( event.getParameter(UPLIMIT) );
		
		PI.put(up?PI.uploadLimit:PI.downloadLimit, i);
		
		return null;
	}

}
