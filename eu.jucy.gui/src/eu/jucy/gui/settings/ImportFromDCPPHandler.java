package eu.jucy.gui.settings;

import java.io.File;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.jucy.gui.Lang;



public class ImportFromDCPPHandler extends AbstractHandler implements IHandler {

	
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		
		FileDialog fd = new FileDialog(window.getShell(),SWT.OPEN);
		fd.setFilterExtensions(new String[]{"Favorites.xml"});
		fd.setFileName("Favorites.xml");
		File standardpath = new File("c:\\Programs\\DC++\\");
		if (standardpath.isDirectory()) {
			fd.setFilterPath(standardpath.getPath());
		}
		fd.setText(Lang.ImportFromDCPPDescription);
		
		String fs = fd.open();
		if (fs != null) {
			File f = new File(fs);
			if (f.isFile()) {
				try {
					DCPPFavImporter.importFavs(f);
				} catch (Exception e) {
					String mes = e.getMessage();
					MessageDialog.openError(window.getShell(), "Error", "Favourites could not be read." +(mes != null? "\n"+mes:""));		
				}
			}
		}

		return null;
	}

	
	


}
