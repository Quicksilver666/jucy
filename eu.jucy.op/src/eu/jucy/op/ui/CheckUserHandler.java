package eu.jucy.op.ui;


import org.eclipse.core.commands.ExecutionEvent;

import uc.DCClient;
import uc.IUser;
import eu.jucy.gui.itemhandler.UserHandlers;
import eu.jucy.op.Activator;
import eu.jucy.op.fakeshare.FileListStorage;

public class CheckUserHandler extends UserHandlers {

	@Override
	protected void doWithUser(final IUser usr, ExecutionEvent event) {
		if (usr.hasDownloadedFilelist()) {
			
			DCClient.execute(
				new Runnable() {
					public void run() {
						FileListStorage fls = Activator.getStorage();
						fls.createKnownTTHDistribution(usr);
						fls.createKnownUserConnections(usr);
					}
				});
			
		}
	}

	
}
