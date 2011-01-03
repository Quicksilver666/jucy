package eu.jucy.gui;


import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.part.ViewPart;

public abstract class UCView extends ViewPart {

	private UCWorkbenchPart part = new UCWorkbenchPart();
	

	protected void setControlsForFontAndColour(Control... receivingChange) {
		part.setControlsForFontAndColour(receivingChange);
	}
	
	public void dispose() {
		super.dispose();
		part.dispose();
	}
	
	
	/**
	 * creates a context popup menu on the viewer and registers it for this site
	 * with the provided ID 
	 * @param id
	 * @param viewer
	 */
	protected void createContextPopup(String id,Viewer viewer) {
		createContextPopups(getSite(),id,viewer);
	}
	
	public static void createContextPopups(IWorkbenchPartSite site,String id,Viewer viewer) {
		UCWorkbenchPart.createContextPopups(site,id,viewer,viewer.getControl());
	}

}
