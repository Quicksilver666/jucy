package eu.jucy.gui.itemhandler;

import java.io.File;

import logger.LoggerFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.program.Program;
import org.eclipse.ui.handlers.HandlerUtil;

import uc.files.UploadQueue.TransferRecord;

public class OpenDirectoryHandler extends AbstractHandler {

	public static final String ID = "eu.jucy.uploadqueue.OpenDirectory";
	
	private static Logger logger = LoggerFactory.make(Level.DEBUG);
	
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection sel = (IStructuredSelection)HandlerUtil.getCurrentSelectionChecked(event);
		TransferRecord tr = (TransferRecord)sel.getFirstElement();
		File folder = tr.getFile().getParentFile();
		logger.debug("launching Program");
		Program.launch(folder.getPath());
		logger.debug("launched Program");
		
		return null;
	}

}
