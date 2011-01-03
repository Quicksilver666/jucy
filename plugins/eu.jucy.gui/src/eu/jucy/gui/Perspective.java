package eu.jucy.gui;


import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import eu.jucy.gui.transferview.TransfersView;


public class Perspective implements IPerspectiveFactory {

	public static final String ID = "eu.jucy.perspective";
	
	public static final String FOLDER_ID = "eu.jucy.bottomviews";

	
	public void createInitialLayout(IPageLayout layout) {
		layout.setEditorAreaVisible(true);

		IFolderLayout ifl = layout.createFolder(FOLDER_ID, IPageLayout.BOTTOM, 0.775f, layout.getEditorArea());
		
	//	ifl.addPlaceholder(TransfersView.ID);
		ifl.addView(TransfersView.ID);
		
//		layout.addStandaloneViewPlaceholder(
//				 TransfersView.ID, //"*",//
//				IPageLayout.BOTTOM, 
//				0.8f, 
//				layout.getEditorArea(), 
//				false);  
		
		//layout.getViewLayout(TransfersView.ID).setCloseable(false);
		
	}
	
}
