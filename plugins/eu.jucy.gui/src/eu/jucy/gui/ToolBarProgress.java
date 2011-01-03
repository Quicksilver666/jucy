package eu.jucy.gui;


import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchWindow;
import org.eclipse.ui.internal.progress.ProgressRegion;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

@SuppressWarnings("restriction")
public class ToolBarProgress extends WorkbenchWindowControlContribution {

	@Override
	protected Control createControl(Composite parent) {
        
        final WorkbenchWindow window = (WorkbenchWindow) PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        ProgressRegion progressRegion = new ProgressRegion();
        progressRegion.createContents(parent, window);
        progressRegion.getControl().setVisible(true);
        
		return progressRegion.getControl();
	}

}
