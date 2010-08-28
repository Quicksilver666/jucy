package eu.jucy.op.ui;



import java.io.File;

import org.eclipse.core.commands.ExecutionEvent;

import uc.DCClient;
import uc.IUser;
import uc.files.downloadqueue.AbstractDownloadFinished;
import uc.files.filelist.FileList;

import eu.jucy.gui.itemhandler.UserHandlers;
import eu.jucy.op.Activator;

public class LoadFileListToDBHandler extends UserHandlers {
	
	
	
	@Override
	protected void doWithUser(final IUser usr, ExecutionEvent event) {
		if (usr.getShared() > 0 || usr.getNumberOfSharedFiles() > 0) {
			usr.downloadFilelist().addDoAfterDownload( new AbstractDownloadFinished() { 
				public void finishedDownload(File f) { 
					DCClient.execute(new Runnable() {
						@Override
						public void run() {
							FileList fl = usr.getFilelistDescriptor().getFilelist();
							Activator.getStorage().insertFileList(fl, System.currentTimeMillis());
						}
					});
				}
			}); 
		}
	}


	
	

}
