package eu.jucy.connectiondebugger;


import helpers.IObservable;
import helpers.StatusObject;
import helpers.Observable.IObserver;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


import org.eclipse.swt.widgets.Composite;



import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import uc.IUser;
import uc.protocols.ConnectionProtocol;
import uihelpers.DelayedTableUpdater;
import uihelpers.TableViewerAdministrator;

import eu.jucy.connectiondebugger.SentCommandColumns.CommandCol;
import eu.jucy.connectiondebugger.SentCommandColumns.DateCol;
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

	/**
	 * maps secondary IDs of view to some input object
	 * (view is used very much like an editor here..)
	 */
	private static final Map<String,Object> VIEW_INPUT = new HashMap<String,Object>();
	
	public static void addInput(String secondaryID,Object o) {
		VIEW_INPUT.put(secondaryID, o);
	}
	
	
	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "eu.jucy.connectiondebugger.debuggerview";

	private TableViewer viewer;
	private TableViewerAdministrator<SentCommand> tva;
	private DelayedTableUpdater<SentCommand> update;
	
	private Action action1;
	private Action action2;

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
		public Object[] getElements(Object parent) {
			return ((ConnectionDebugger)parent).getLastCommands().toArray();
		}
	}



	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		tva = new TableViewerAdministrator<SentCommand>(viewer, 
				Arrays.asList(new DateCol(),new CommandCol()), ID, 0);
		
		tva.apply();
		update = new DelayedTableUpdater<SentCommand>(viewer);
		
		
		viewer.setContentProvider(new ViewContentProvider());
		
		String secID = getViewSite().getSecondaryId();
		Object o = VIEW_INPUT.get(secID);
		
		if (o instanceof ConnectionProtocol) {
			((ConnectionProtocol)o).registerDebugger(debugger);
		} else if (o instanceof IUser && ((IUser)o).getIp() != null) {
			ConnectionProtocol.addAutoAttach(((IUser)o).getIp(), debugger);
		} 
		VIEW_INPUT.remove(secID);
		
		viewer.setInput(debugger);
		debugger.addObserver(this);
		
		makeActions();
		contributeToActionBars();
	}
	
	

//	private void hookContextMenu() {
//		MenuManager menuMgr = new MenuManager("#PopupMenu");
//		menuMgr.setRemoveAllWhenShown(true);
//		menuMgr.addMenuListener(new IMenuListener() {
//			public void menuAboutToShow(IMenuManager manager) {
//				DebuggerView.this.fillContextMenu(manager);
//			}
//		});
//		Menu menu = menuMgr.createContextMenu(viewer.getControl());
//		viewer.getControl().setMenu(menu);
//		getSite().registerContextMenu(menuMgr, viewer);
//	}

	public void update(IObservable<StatusObject> o, StatusObject arg) {
		if (arg.getValue() instanceof SentCommand) {
			update.put(arg.getType(), (SentCommand)arg.getValue());
		}
	}



	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(action1);
		manager.add(new Separator());
		manager.add(action2);
	}

	
	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(action1);
		manager.add(action2);
	}

	private void makeActions() {
		action1 = new Action() {
			public void run() {
				showMessage("Action 1 executed");
			}
		};
		action1.setText("Action 1");
		action1.setToolTipText("Action 1 tooltip");
		action1.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
		
		action2 = new Action() {
			public void run() {
				showMessage("Action 2 executed");
			}
		};
		action2.setText("Action 2");
		action2.setToolTipText("Action 2 tooltip");
		action2.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
	}


	private void showMessage(String message) {
		MessageDialog.openInformation(
			viewer.getControl().getShell(),
			"Connection Debugger",
			message);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}
}