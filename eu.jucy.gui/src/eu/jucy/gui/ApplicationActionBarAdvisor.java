package eu.jucy.gui;



import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.StatusLineManager;

import org.eclipse.jface.action.ToolBarManager;

import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;



import eu.jucy.gui.statusline.StatusComposite;





public class ApplicationActionBarAdvisor extends ActionBarAdvisor {

	public static final String ShownParts = "shownParts";
	/**
	 * constant for the view menu
	 */
	public static final String ViewMenu = "view";  
	
	public static final String ActionMenu = "action";
	

	private IWorkbenchAction showHelpAction;
	private IWorkbenchAction resetPerspectiveAction;
		

							

    public ApplicationActionBarAdvisor(IActionBarConfigurer configurer) {
        super(configurer);
     
     
    }

    protected void makeActions(IWorkbenchWindow window) {
    	   showHelpAction = ActionFactory.HELP_CONTENTS.create(window);
    	   register(showHelpAction); 
    	   resetPerspectiveAction = ActionFactory.RESET_PERSPECTIVE.create(window);
    	   register(resetPerspectiveAction);
    }

    
    protected void fillMenuBar(IMenuManager menuBar) {
    	MenuManager fileMenu= new MenuManager(Lang.FileMen,IWorkbenchActionConstants.M_FILE);
    	fileMenu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

    	MenuManager actionMenu = new MenuManager( Lang.ActionMen,ActionMenu);
    	actionMenu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    	
    	MenuManager viewMenu= new MenuManager(Lang.View,ViewMenu);
    	viewMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
    	viewMenu.add(new Separator(ShownParts));
    	
    	
    	MenuManager helpMenu = new MenuManager(Lang.Help, IWorkbenchActionConstants.M_HELP);
    	helpMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));


    	menuBar.add(fileMenu);
    	menuBar.add(actionMenu);
    	menuBar.add(viewMenu);
    	menuBar.add(helpMenu);
	
    
    }
    
    protected void fillCoolBar(final ICoolBarManager coolBar){
    	final IToolBarManager toolBar = new ToolBarManager(coolBar.getStyle());
    	coolBar.add(toolBar);
    
    	toolBar.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    }

	@Override
	protected void fillStatusLine(IStatusLineManager statusLine) {
		statusLine.insertBefore( StatusLineManager.MIDDLE_GROUP, new StatusComposite());

		super.fillStatusLine(statusLine);
	}
    
    
    

}
