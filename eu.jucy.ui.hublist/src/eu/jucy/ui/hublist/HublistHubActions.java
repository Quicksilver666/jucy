package eu.jucy.ui.hublist;

import logger.LoggerFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;
import eu.jucy.gui.Lang;
import eu.jucy.hublist.Column;
import eu.jucy.hublist.HublistHub;


public abstract class HublistHubActions extends Action implements
		IWorkbenchAction ,ISelectionChangedListener, ISelectionListener{

	private static final String partialID="eu.jucy.ui.hublist.HublistHubAction.";
	
	private static Logger logger = LoggerFactory.make();
	static {
		logger.setLevel(Level.ALL);
	}
	
	
	private IStructuredSelection selection;
	
	
	
	public HublistHubActions() {}
	
	
	
	public void dispose() {

	}

	
	public void selectionChanged(SelectionChangedEvent event) {
		selectionChanged(null,event.getSelection());
	}



	
	public void selectionChanged(IWorkbenchPart part, ISelection incoming) {
		if (incoming instanceof IStructuredSelection) {
			selection = (IStructuredSelection)incoming;
			
			setEnabled(	!selection.isEmpty()  && (selection.getFirstElement() instanceof HublistHub ));
			
		} else {
			setEnabled(false);
		}
	}


	public void run() {
		logger.debug("starting hubaction "+ selection.size());
		for (Object o: selection.toList()) {
			logger.debug("found hublist hub:"+o);
			if (o instanceof HublistHub) {
				doWith((HublistHub)o);
			}
		}
	}
	
	
	protected abstract void doWith(HublistHub hub);

	
	public static class AddToFavoritesAction extends HublistHubActions {
		
		public static final String ID = partialID+"AddToFavorites";
		
		public AddToFavoritesAction() {
			setId(ID);
			setText(Lang.AddToFavorites);
		}
		
		
		protected void doWith(HublistHub hub) {
			hub.addToFavorites();
		}
		
	}

	public static class ConnectAction extends HublistHubActions {
		
		public static final String ID = partialID+"Connect";
		
		public ConnectAction() {
			setId(ID);
			setText(Lang.Connect);
		}
		
		
		protected void doWith(HublistHub hub) {
			hub.connect(ApplicationWorkbenchWindowAdvisor.get());
		}
	}
	
	public static class CopyAddressAction extends HublistHubActions {
		
		public static final String ID = partialID+"CopyAddress";
		
		public CopyAddressAction() {
			setId(ID);
			setText(Lang.CopyAddressToClipboard);
		}
		
		
		protected void doWith(HublistHub hub) {
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			Clipboard clipboard= new Clipboard(window.getShell().getDisplay());
			String textData = hub.getAttribute(Column.ADDRESS);
			
			logger.debug("copy: "+textData);
			
			TextTransfer textTransfer = TextTransfer.getInstance();
			clipboard.setContents(new Object[]{textData}, new Transfer[]{textTransfer});
			clipboard.dispose();
		}
	}
	
	
	
}
