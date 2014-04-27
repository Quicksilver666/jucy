package eu.jucy.connectiondebugger;


import helpers.IObservable;
import helpers.StatusObject;
import helpers.Observable.IObserver;


import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import logger.LoggerFactory;


import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Composite;


import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;


import uc.IUser;
import uc.protocols.ConnectionProtocol;
import uc.protocols.IConnection;
import uihelpers.DelayedTableUpdater;
import uihelpers.SUIJob;
import uihelpers.TableViewerAdministrator;

import eu.jucy.connectiondebugger.ConnectionDebugger.CommandStat;
import eu.jucy.connectiondebugger.SentCommandColumns.CommandCol;
import eu.jucy.connectiondebugger.SentCommandColumns.CommandName;
import eu.jucy.connectiondebugger.SentCommandColumns.DateCol;
import eu.jucy.connectiondebugger.SentCommandColumns.Frequency;
import eu.jucy.connectiondebugger.SentCommandColumns.LastCommand;
import eu.jucy.connectiondebugger.SentCommandColumns.TrafficTotal;
import eu.jucy.gui.UCView;


/**
 * This sample class demonstrates how to plug-in a new
 * workbench view. The view shows data obtained from the
 * model. The sample creates a dummy model on the fly,
 * but a real implementation would connect to the model
 * available either in this or another plug-in (e.g. the workspace).
 * The view is connected to the model using a content provider.
 * <p>
 * The view uses a label provider to define how model
 * objects should be presented in the view. Each
 * view can present the same model objects using
 * different labels and icons, if needed. Alternatively,
 * a single label provider can be shared between views
 * in order to ensure that objects of the same type are
 * presented in the same way everywhere.
 * <p>
 */

public class DebuggerView extends UCView implements IObserver<StatusObject> {

	private static final Logger logger = LoggerFactory.make(Level.DEBUG);
	
	/**
	 * maps secondary IDs of view to some input object
	 * (view is used very much like an editor here..)
	 */
	private static final Map<String,Object> VIEW_INPUT = new HashMap<String,Object>();
	
	public static void addInput(String secondaryID,Object o) {
		logger.debug("adding Input: "+secondaryID+"  "+o);
		VIEW_INPUT.put(secondaryID, o);
	}
	
	
	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "eu.jucy.connectiondebugger.debuggerview";

	private CTabFolder folder;
	
	private TableViewer commandsViewer;
	private TableViewerAdministrator<SentCommand> tva;
	private DelayedTableUpdater<SentCommand> update;
	
	
	private TableViewer statsViewer;
	private TableViewerAdministrator<CommandStat> tvaStats;
	private DelayedTableUpdater<CommandStat> updateStats;
	
	
	private CryptoComposite cryptoComposite;

	private final ConnectionDebugger debugger = new ConnectionDebugger();
	
	
	
	/*
	 * The content provider class is responsible for
	 * providing objects to the view. It can wrap
	 * existing objects in adapters or simply return
	 * objects as-is. These objects may be sensitive
	 * to the current input of the view, or ignore
	 * it and always show the same content 
	 * (like Task List, for example).
	 */
	 
	class ViewContentProvider implements IStructuredContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}
		public void dispose() {
		}
		public Object[] getElements(Object inputElement) {
			return ((ConnectionDebugger)inputElement).getLastCommands().toArray();
		}
	}
	
	class StatsContentProvider implements IStructuredContentProvider {

		public Object[] getElements(Object inputElement) {
			return ((ConnectionDebugger)inputElement).getCommandCounter().toArray();
		}
		
		public void dispose() {}
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
		
	}



	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		
		folder = new CTabFolder(parent, SWT.BORDER);
		folder.setSimple(false);
		createCommandsTab(folder);
		createStatsTab(folder);
		createCryptoTab(folder);
				
		folder.setSelection(0);
		
		String secID = getViewSite().getSecondaryId();
		Object o = VIEW_INPUT.get(secID);
		
		
		debugger.addObserver(this);
		
		if (o instanceof ConnectionProtocol) {
			logger.debug("attaching to connectionProtocol: "+o);
			IConnection con = ((ConnectionProtocol)o ).getConnection();
			setPartName("Connection: "+ con.getInetSocketAddress());
			debugger.init( (ConnectionProtocol)o );
			
		} else if (o instanceof IUser && ((IUser)o).getIp() != null) {
			logger.debug("attaching to ip of user: "+o);
			setPartName("Connection: "+ ((IUser)o).getNick());
			debugger.init(((IUser)o).getIp());
		} else {
			logger.debug("no attachment done: "+secID +"  "+o);
		}
		VIEW_INPUT.remove(secID);
		
		getSite().setSelectionProvider(commandsViewer); 
		createContextPopup(ID, commandsViewer);		
		
		setControlsForFontAndColour(commandsViewer.getTable()
				,statsViewer.getTable()
				,cryptoComposite.getTable()
				,cryptoComposite.getText());

	}
	
	private void createCommandsTab(CTabFolder parent) {
	
		commandsViewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL |SWT.FULL_SELECTION);
		commandsViewer.getTable().setHeaderVisible(true);
		
		tva = new TableViewerAdministrator<SentCommand>(commandsViewer, 
				Arrays.asList(new DateCol(),new CommandCol()), ID+".command", -1);
		
		tva.apply();
		update = new DelayedTableUpdater<SentCommand>(commandsViewer);
		
		
		commandsViewer.setContentProvider(new ViewContentProvider());
		commandsViewer.setInput(debugger);
		
		CTabItem connectionItem = new CTabItem(folder, SWT.NONE);
		connectionItem.setText("Commands");
		connectionItem.setControl(commandsViewer.getControl());
	}
	
	
	
	
	private void createStatsTab(CTabFolder parent) { 
		statsViewer  = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL |SWT.FULL_SELECTION);
		statsViewer.getTable().setHeaderVisible(true);
		tvaStats = new TableViewerAdministrator<CommandStat>(statsViewer, 
				Arrays.asList(new CommandName(),new Frequency(),new TrafficTotal(debugger),new LastCommand()), ID+".stat", -1);
		tvaStats.apply();
		updateStats = new DelayedTableUpdater<CommandStat>(statsViewer);
		
		statsViewer.setContentProvider(new StatsContentProvider());
		statsViewer.setInput(debugger);
		
		CTabItem statsItem = new CTabItem(folder, SWT.NONE);
		statsItem.setText("Stats");
		statsItem.setControl(statsViewer.getControl());
		
	}
	
	private void createCryptoTab(CTabFolder parent) {
		cryptoComposite = new CryptoComposite(parent, SWT.BORDER);
	
		CTabItem statsItem = new CTabItem(folder, SWT.NONE);
		statsItem.setText("Crypto");
		statsItem.setControl(cryptoComposite);
	}
	

	public void update(IObservable<StatusObject> o, final StatusObject arg) {
		if (arg.getValue() instanceof SentCommand) {
			update.put(arg.getType(), (SentCommand)arg.getValue());
		}
		if (arg.getValue() instanceof CommandStat ) {
			updateStats.put(arg.getType(), (CommandStat)arg.getValue());
		}
		if (arg.getValue() instanceof CryptoInfo) {
			
			new SUIJob(cryptoComposite) {
				@Override
				public void run() {
					cryptoComposite.setCryptoInfo((CryptoInfo)arg.getValue());
				}
			}.schedule();
		}
		
	}

	

//
//	private void contributeToActionBars() {
//		IActionBars bars = getViewSite().getActionBars();
//		fillLocalPullDown(bars.getMenuManager());
//		fillLocalToolBar(bars.getToolBarManager());
//	}
//
//	private void fillLocalPullDown(IMenuManager manager) {
//		manager.add(action1);
//		manager.add(new Separator());
//		manager.add(action2);
//	}
//
//	
//	private void fillLocalToolBar(IToolBarManager manager) {
//		manager.add(action1);
//		manager.add(action2);
//	}
//
//	private void makeActions() {
//		action1 = new Action() {
//			public void run() {
//				showMessage("Action 1 executed");
//			}
//		};
//		action1.setText("Action 1");
//		action1.setToolTipText("Action 1 tooltip");
//		action1.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
//			getImageDescriptor(ISharedImages.IMG_OBJ_FOLDER));
//		
//		action2 = new Action() {
//			public void run() {
//				showMessage("Action 2 executed");
//			}
//		};
//		action2.setText("Action 2");
//		action2.setToolTipText("Action 2 tooltip");
//		action2.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
//				getImageDescriptor(ISharedImages.IMG_OBJ_FILE));
//	}
//
//
//	private void showMessage(String message) {
//		MessageDialog.openInformation(
//			viewer.getControl().getShell(),
//			"Connection Debugger",
//			message);
//	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		commandsViewer.getControl().setFocus();
	}



	@Override
	public void dispose() {
		super.dispose();
		debugger.deleteObserver(this);
		debugger.dispose();
	}
	
	
	
}