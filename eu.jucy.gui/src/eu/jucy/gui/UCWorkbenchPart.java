package eu.jucy.gui;

import helpers.PreferenceChangedAdapter;

import java.util.concurrent.CopyOnWriteArrayList;


import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPartSite;


public class UCWorkbenchPart {

	
	public static final String USER_ADDITIONS_ID= "useradditions"; 
	public static final String POST_USER_ADDITIONS_ID= "postuseradditions"; 
	
	public static Color background;
	public static Color fontColour;
	public static Font font;
	
	private static CopyOnWriteArrayList<UCWorkbenchPart> active = new CopyOnWriteArrayList<UCWorkbenchPart>();
	
	static {
		setFontAndColour();
		new PreferenceChangedAdapter(GUIPI.get(),GUIPI.windowColor,GUIPI.windowFontColor,GUIPI.editorFont) {
			@Override
			public void preferenceChanged(String preference,String oldValue, String newValue) {
				setFontAndColour();
				for (UCWorkbenchPart ucme:active) {
					ucme.updateColourOrFont();
				}
			}
			
		};
	}
	
	private static void setFontAndColour() {
		background = GUIPI.getColor(GUIPI.windowColor);
		fontColour = GUIPI.getColor(GUIPI.windowFontColor);
		font = GUIPI.getFont(GUIPI.editorFont);
	}
	
	public void setControlsForFontAndColour(Control... receivingChange) {
		controls = receivingChange;
		updateColourOrFont();
	}
	
	
	
	private Control[] controls = new Control[0];
	
	public UCWorkbenchPart() {
		active.add(this);
	}
	
	/**
	 * overwrite this method to get notified on changed colours or fonts..
	 * but call super  as this changes the font and text on all set controls..
	 */
	protected void updateColourOrFont() {
		for (Control c: controls) {
			c.setBackground(background);
			c.setForeground(fontColour);
			c.setFont(font);
		}
	}
	
	public void dispose() {
		active.remove(this);
	}
	
	public static void createContextPopups(IWorkbenchPartSite site,String id,ISelectionProvider sp,Control c) {
		MenuManager menuManager = new MenuManager();
		menuManager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		menuManager.add(new GroupMarker(USER_ADDITIONS_ID));
		menuManager.add(new GroupMarker(POST_USER_ADDITIONS_ID));
		
		site.registerContextMenu(id,menuManager, sp);
		Menu menu = menuManager.createContextMenu(c);
		c.setMenu(menu);
	}
	
	public static void createContextPopups(IWorkbenchPartSite site,String id,Viewer viewer) {
		createContextPopups(site,id,viewer,viewer.getControl());
	}
	
}
