package eu.jucy.gui.representation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.internal.presentations.util.ISystemMenu;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.presentations.IPresentablePart;



import eu.jucy.gui.Application;
import eu.jucy.gui.CloseEditorHandler;
import eu.jucy.gui.IImageKeys;
import eu.jucy.gui.IUCEditor;


@SuppressWarnings("restriction")
public class UCEditorContributionItem extends CompoundContributionItem {


	
	public UCEditorContributionItem() {
		super();
	}

	@Override
	protected IContributionItem[] getContributionItems() {
		IWorkbenchPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().getActivePart();
		if (part instanceof IUCEditor) {
			List<IContributionItem> contribi = new ArrayList<IContributionItem>();
			((IUCEditor)part).getContributionItems(contribi);
			return contribi.toArray( new IContributionItem[]{});
		} else {
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			CommandContributionItemParameter ccip = 
				new CommandContributionItemParameter(window, null, CloseEditorHandler.COMMAND_ID,SWT.PUSH);
			ccip.icon = AbstractUIPlugin.imageDescriptorFromPlugin(
					Application.PLUGIN_ID, IImageKeys.STOPICON);
			
			CommandContributionItem cci = new CommandContributionItem(ccip);
			return new IContributionItem[]{cci};
		}
	}
	
	public static class UCEditorSystemMenu implements ISystemMenu {

		private MenuManager menuManager;
		
		public UCEditorSystemMenu() {
			menuManager = new MenuManager();
			menuManager.add(new UCEditorContributionItem());
		}
		
		public void dispose() {
			menuManager.dispose();
			menuManager.removeAll();
		}

		public void show(Control parent, Point displayCoordinates,
				IPresentablePart currentSelection) {
			IWorkbenchPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().getActivePart();
			if (part instanceof IUCEditor) {
				((IUCEditor)part).tabMenuBeforeShow();
			}
			Menu aMenu = menuManager.createContextMenu(parent);
			menuManager.update(true);
			aMenu.setLocation(displayCoordinates.x, displayCoordinates.y);
			aMenu.setVisible(true);
		}
		
	}

}
