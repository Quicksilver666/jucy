package eu.jucy.gui;






import logger.LoggerFactory;


import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;


import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;
import org.eclipse.ui.IPartListener;

import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;

import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.menus.IMenuService;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import eu.jucy.gui.IUCEditor.ITopicChangedListener;
import eu.jucy.gui.texteditor.hub.HubEditor;
import eu.jucy.gui.texteditor.hub.HubEditorInput;
import eu.jucy.gui.texteditor.hub.RedirectReceivedProvider;

import uc.DCClient;
import uc.FavHub;
import uc.IHubCreationListener;
import uc.PI;
import uihelpers.SUIJob;


public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor  {

	public static final String TrayMenuID = "eu.jucy.gui.TrayMenu";

	private static final Logger logger = LoggerFactory.make();

	private static DCClient dcc;
	private static Thread uiThread;
	private static volatile Job shutdownJob;
	
	public static void waitForShutdownJob() {
		if (Thread.currentThread() ==  uiThread) {
			Display display = Display.getCurrent();
			while (shutdownJob != null) {
				if (!display.readAndDispatch ()) display.sleep ();
			}
		} else {
			if (shutdownJob != null) {
				try {
					shutdownJob.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	/**
	 * -> opens new HubEditors when ever a hub is created..
	 */
	private IHubCreationListener hubCreationListener;
	
	/**
	 * may only be called from ui thread
	 * this returns the DCClient for which this WorkbenchWindow is responsible
	 */
	public static DCClient get() {
		if (Thread.currentThread() != uiThread) {
			throw new IllegalStateException("IllegalThread Access");
		}
		return dcc;
	}
	
    public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
        super(configurer);
    }

    public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer) {
        return new ApplicationActionBarAdvisor(configurer);
    }
    
    public void preWindowOpen() {
    	uiThread = Thread.currentThread();
    	dcc = new DCClient();
        IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
        configurer.setInitialSize(new Point(1024, 768));
        configurer.setShowMenuBar(true);
        configurer.setShowCoolBar(true);
        configurer.setShowStatusLine(true);   
        configurer.setShowProgressIndicator(false);
        configurer.setTitle(DCClient.LONGVERSION);
    }
    
    
    
    public void postWindowOpen() {
    	super.postWindowOpen();
		
    	IWorkbenchWindow window = getWindowConfigurer().getWindow();
    	
    	if (logger.isDebugEnabled()) {
    		GUIPI.get().addPreferenceChangeListener(new IPreferenceChangeListener() {
				public void preferenceChange(PreferenceChangeEvent event) {
					String pref= event.getKey();
					logger.debug("pref changed: "+pref);	
				}
			});
    	}

    	IStatusLineManager manager = getWindowConfigurer().getActionBarConfigurer().getStatusLineManager();
    	GuiAppender.get().initialize(manager);
    	
    	
    	
    	//handle tray icon
    	trayItem = initTaskItem(window);
    	if (trayItem != null) {
    		hookMinimized(window);
    		hookMenu(trayItem,window);
    	}
    	if (GUIPI.getBoolean(GUIPI.minimizeOnStart)) {
    		mininmize(window);
    	}
    	
		Dialog.setDefaultImage(trayImage);
		

    	
    	registerListeners(window);
    	
    	new Job("Starting "+DCClient.LONGVERSION) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					dcc.start(monitor); //start FileList refresh and client in general 
				} catch(Exception e) {
					logger.error(e, e);
				}
				return Status.OK_STATUS;
			}
    		
    	}.schedule();
    	
    	//check for updates..
    	if (PI.getBoolean(PI.checkForUpdates) && 
    			System.currentTimeMillis() - PI.getLong(PI.lastCheckForUpdates) > 24 * 3600 * 1000 &&
    			!Platform.inDevelopmentMode()) {
    		eu.jucy.gui.update.UpdateHandler.checkForUpdates();
    		PI.put(PI.lastCheckForUpdates, System.currentTimeMillis());
    	} 
    	
    	UIThreadDeadLockChecker.start();

    }
    
    
    
    @Override
	public void postWindowClose() {
		dcc.unregister(hubCreationListener);
		dcc.getHashEngine().unregisterHashedListener(GuiAppender.get());
		shutdownJob = new Job("Shutdown") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				dcc.stop(true);
				shutdownJob = null;
				return Status.OK_STATUS;
			}
		};
		shutdownJob.schedule();
		
		super.postWindowClose();
	}

	private void registerListeners(final IWorkbenchWindow window) {
    	dcc.getHashEngine().registerHashedListener(GuiAppender.get());
    	
    	hubCreationListener = new IHubCreationListener() {
			public void hubCreated(final FavHub fh, boolean showInUI, final Runnable callback) {
				if (showInUI) {
					new SUIJob() {
						public void run() {
				       		try {	
				        		window.getActivePage().openEditor(new HubEditorInput(fh), HubEditor.ID);
				        	} catch(PartInitException pie) {
				        		MessageDialog.openError(window.getShell(), "Error", "Error open hub:" + pie.getMessage());
				        	}	
				        	DCClient.execute(callback);
						}
					}.schedule();
				}
			}
    		
    	};
    	
    	dcc.register(hubCreationListener);
    	
    	//listener for setting topic string on the top of the window
    	final ITopicChangedListener listener = new ITopicChangedListener() {
			public void topicChanged(IUCEditor editor) {
				String text = DCClient.LONGVERSION+" -["+editor.getTopic()+"]";
				window.getShell().setText(text);
			}
    	};
 
    	window.getPartService().addPartListener(new IPartListener() {
    		
			public void partActivated(IWorkbenchPart part) {
				partBroughtToTop(part);
			}
			
			public void partBroughtToTop(IWorkbenchPart part) {
				if (part instanceof IUCEditor) {
					IUCEditor editor = (IUCEditor)part;
					editor.registerTopicChangedListener(listener);
					editor.fireTopicChangedListeners();
					editor.partActivated();
				}	
			}
			
			public void partClosed(IWorkbenchPart part) {}
			
			public void partDeactivated(IWorkbenchPart part) {
				if (part instanceof IUCEditor) {
					IUCEditor editor = (IUCEditor)part;
					editor.unregisterTopicChangedListener(listener);
					deleteTopText();
				}
			}
			
			private void deleteTopText() {
				String text = DCClient.LONGVERSION; 
				window.getShell().setText(text);
			}
			
			public void partOpened(IWorkbenchPart part) {}
    		
    	});
    	
    	RedirectReceivedProvider.init(window);
    }
    
    private TrayItem trayItem;
    private Image trayImage;
    private Listener enlarge = null;
    
    private void hookMinimized(final IWorkbenchWindow window) {
    	if (enlarge == null) {
    		enlarge = new Listener() {
        		public void handleEvent(Event e) {
        			//using ahead provided window! -> 
        			//problems in Linux due to not window available when minimized?
        			IHandlerService ihs = (IHandlerService)window.getService(IHandlerService.class);
        			try {
						ihs.executeCommand(EnlargeShellHandler.CommandID, null);
					} catch (Exception e1) {
						logger.warn(e1, e1);
					} 
        		}
        	};
    	}
    	
    	window.getShell().addShellListener(new ShellAdapter(){
    		public void shellIconified(ShellEvent e){
    			mininmize(window);
    		}
    	});
    	
    	trayItem.removeListener(SWT.DefaultSelection, enlarge);//prevent from infinite amount of listeners..
    	trayItem.addListener(SWT.DefaultSelection, enlarge);
    	
    }
    
    private void mininmize(IWorkbenchWindow window) {
    	
    	Shell shell = window.getShell();
    	if (!shell.getMinimized()) {
    		shell.setMinimized(true);
    	}
    	if (GUIPI.getBoolean(GUIPI.minimizeToTray) && shell.isVisible()) {
			shell.setVisible(false);
		}
		if (!dcc.isAway() && GUIPI.getBoolean(GUIPI.setAwayOnMinimize)) {
			dcc.setAway(true);
		}
    }
    
    private void hookMenu(TrayItem trayItem,IWorkbenchWindow window) {
    	Shell shell = new Shell(Display.getCurrent());
    	
    	MenuManager manager = new MenuManager("TrayMenu",TrayMenuID);
    	manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    	
    
    	IMenuService service = (IMenuService)window.getService(IMenuService.class);
    	service.populateContributionManager(manager, "popup:"+TrayMenuID);
    	
    

    	
    	final Menu menu = manager.createContextMenu(shell);
    	
    	//final Menu menu = new Menu(shell, SWT.POP_UP);
    	trayItem.addListener(SWT.MenuDetect, new Listener() {
    		public void handleEvent(Event event) {
    			menu.setVisible(true);
    		}
    	});
    }
    
    
    
    
    
    
    private TrayItem initTaskItem(IWorkbenchWindow window){
    	trayImage = AbstractUIPlugin.imageDescriptorFromPlugin(Application.PLUGIN_ID, IImageKeys.TRAYICON).createImage();
    	
    	if (Platform.OS_MACOSX.equals(Platform.getOS())) { //MacOsX uses grey tray icons..
    		Image img = new Image(null,trayImage,SWT.IMAGE_GRAY);
    		trayImage.dispose();
    		trayImage = img;
    	}
    	
    	try {
	    	final Tray tray = window.getShell().getDisplay().getSystemTray();
	    	if( tray == null ) {
	    		return null;
	    	}
	    	TrayItem trayItem = new TrayItem(tray,SWT.NONE);
	    	trayItem.setImage(trayImage);
	    	trayItem.setToolTipText(DCClient.LONGVERSION+" - Direct Connect Client");
	    	
	    	trayItem.setVisible(true);

	    
	    	return trayItem;
	    
    	} catch(NoClassDefFoundError err) {
    		if (Platform.OS_MACOSX.equals(Platform.getOS())) {
    			logger.warn("Tray icon not loaded. Probably you are running a too old MacOSX version.");
    		} else {
    			logger.error(err, err);
    		}
    	} catch(UnsatisfiedLinkError err) {
    		if (Platform.OS_MACOSX.equals(Platform.getOS())) {
    			logger.warn("Tray icon not loaded. Probably you are running a too old MacOSX version.");
    		} else {
    			logger.error(err, err);
    		}
    	} 
    	return null;
    }
    
 
    
    
    public void dispose() {
    	disposeTray();
    }
    
    void disposeTray() {
    	if (trayItem != null && !trayItem.isDisposed()) {
    		trayItem.setVisible(false);
    		trayImage.dispose();
    		trayItem.dispose();
    	}
    }
    
    
    
}

/*
 *add menu 
 *
Tray tray = display.getSystemTray();
if(tray != null) {
	TrayItem item = new TrayItem(tray, SWT.NONE);
	item.setImage(image);final Menu menu = new Menu(shell, SWT.POP_UP);
	MenuItem menuItem = new MenuItem(menu, SWT.PUSH);
	menuItem.setText("Button A");
	menuItem = new MenuItem(menu, SWT.PUSH);
	menuItem.setText("Button B");
	menuItem = new MenuItem(menu, SWT.PUSH);
	menuItem.setText("Show Tooltip");
	item.addListener (SWT.MenuDetect, new Listener () {
		public void handleEvent (Event event) {
			menu.setVisible (true);
		}
	});
}

Recent releases of SWT (post-M5) finally support balloons/tooltips through the tray icon. Balloons are quite common in platforms with tray icons, so this is a nice feature to finally have. To add a tooltip, simply use the TrayItem.setToolTip(ToolTip) method, and set the visibility of the tooltip to true when you want to show it.

final ToolTip tip = new ToolTip(shell, SWT.BALLOON | SWT.ICON_INFORMATION);
tip.setMessage("Balloon Message Goes Here!");
Tray tray = display.getSystemTray();
if (tray != null) {
	TrayItem item = new TrayItem(tray, SWT.NONE);
	item.setImage(image);
	tip.setText("Balloon Title goes here.");
	item.setToolTip(tip);
	final Menu menu = new Menu(shell, SWT.POP_UP);
	MenuItem menuItem = new MenuItem(menu, SWT.PUSH);
	menuItem.setText("Button A");
	menuItem = new MenuItem(menu, SWT.PUSH);
	menuItem.setText("Button B");
	menuItem = new MenuItem(menu, SWT.PUSH);
	menuItem.setText("Show Tooltip");
	// Add tooltip visibility to menu item.
	menuItem.addListener (SWT.Selection, new Listener () {			
		public void handleEvent (Event e) {
			tip.setVisible(true);
		}
	});
	// Add menu detection listener to tray icon.
	item.addListener (SWT.MenuDetect, new Listener () {
		public void handleEvent (Event event) {
			menu.setVisible (true);
		}
	});
}
 
 */