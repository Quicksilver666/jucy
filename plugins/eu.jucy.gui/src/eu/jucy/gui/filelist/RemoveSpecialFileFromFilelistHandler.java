package eu.jucy.gui.filelist;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import uc.files.filelist.OwnFileList.SpecialFileListFile;

public class RemoveSpecialFileFromFilelistHandler extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		IStructuredSelection sel = (IStructuredSelection)HandlerUtil.getCurrentSelectionChecked(event);
		for (Object o:sel.toArray()) {
			((SpecialFileListFile)o).remove();
		}
		return null;
	}

}
