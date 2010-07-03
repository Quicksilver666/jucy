package eu.jucy.gui.transferview;



import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;



import helpers.IObservable;
import helpers.SizeEnum;
import helpers.StatusObject;
import helpers.Observable.IObserver;
import logger.LoggerFactory;


import org.apache.log4j.Logger;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;



import org.eclipse.jface.viewers.TableViewer;

import org.eclipse.swt.SWT;

import org.eclipse.swt.widgets.Composite;


import org.eclipse.swt.widgets.Table;



import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;
import eu.jucy.gui.GUIPI;
import eu.jucy.gui.Lang;
import eu.jucy.gui.UCView;
import eu.jucy.gui.itemhandler.CommandDoubleClickListener;
import eu.jucy.gui.itemhandler.OpenDirectoryHandler;
import eu.jucy.gui.transferview.TransferColumns.FilenameColumn;
import eu.jucy.gui.transferview.TransferColumns.HubColumn;
import eu.jucy.gui.transferview.TransferColumns.IPColumn;
import eu.jucy.gui.transferview.TransferColumns.SizeColumn;
import eu.jucy.gui.transferview.TransferColumns.SpeedColumn;
import eu.jucy.gui.transferview.TransferColumns.StatusColumn;
import eu.jucy.gui.transferview.TransferColumns.TimeLeftColumn;
import eu.jucy.gui.transferview.TransferColumns.UserColumn;



import uc.ConnectionHandler;
import uc.IUser;



import uc.files.transfer.AbstractFileTransfer;
import uc.protocols.client.ClientProtocol;
import uc.protocols.client.ClientProtocolStateMachine;
import uihelpers.DelayedTableUpdater;
import uihelpers.SUIJob;
import uihelpers.TableViewerAdministrator;
import uihelpers.ToolTipProvider;

public class TransfersView extends UCView  implements IObserver<StatusObject> {

	public static Logger logger = LoggerFactory.make();
	

	public static final String ID = "eu.jucy.gui.Transfers";
	
	

	private TableViewer tableViewer;
	
	private Table table;
	

	private TableViewerAdministrator<Object> tva; 
	private DelayedTableUpdater<Object> update; 
	

	

	@Override
	public void createPartControl(final Composite parent) {
		
		table = new Table(parent,SWT.BORDER | SWT.FULL_SELECTION);
		
		tableViewer =  new TableViewer(table);
		
		table.setHeaderVisible(true);
		
		UCProgressPainter.addToTable(table);
		
		
		tva = new TableViewerAdministrator<Object>(tableViewer,
				Arrays.asList(new UserColumn(),new HubColumn(),
						new StatusColumn(), new TimeLeftColumn(),
						new SpeedColumn(), new FilenameColumn(),
						new SizeColumn(), new IPColumn()),GUIPI.transfersViewTable,2);
						
		tva.apply();
		
		update = new DelayedTableUpdater<Object>(tableViewer);
		
		TransferContentProvider tcp = new TransferContentProvider();
		tableViewer.setContentProvider(tcp);
		setControlsForFontAndColour(table);
		
		getSite().setSelectionProvider(tableViewer);   

		//makeActions();
		createContextPopup(ID, tableViewer);
		tableViewer.addDoubleClickListener(new CommandDoubleClickListener(OpenDirectoryHandler.ID));
		
		List<ConnectionHandler> chs = ApplicationWorkbenchWindowAdvisor.get().getAllConnectionHandler();
		tableViewer.setInput(chs);
		
		//TOOD change model -> registering with every connection Handler is bad..
		for (ConnectionHandler ch:chs) {
			ch.addObserver(this);
			ch.addTransferObserver(transferListener2);
//			for (Object o : ch.getActive()) {
//				if (o instanceof ClientProtocol) {
//					IFileTransfer ift = ((ClientProtocol)o).getFileTransfer();
//					if (ift != null) {
//						ift.addObserver(transferListener);
//					}
//				}
//			}
		}
			
		new ToolTipProvider<Object>(table) {
			@Override
			protected String getToolTip(Object o) {
				IUser other = null;
				String s= null;
				if (o instanceof ClientProtocol) {
					ClientProtocol cp = (ClientProtocol)o;
					other = cp.getUser();
					File f = cp.getFti().getFile();
					if (f != null) {
						s = String.format(Lang.FileTFTooltip, f.getPath());
					}
				} 
				if (o instanceof ClientProtocolStateMachine ) {
					ClientProtocolStateMachine ccspm =  (ClientProtocolStateMachine)o;
					other = ccspm.getUser();
				}
				if (s == null) {
					s = "";
				} else {
					s += "\n";
				}
				if (other != null && other.nrOfFilesInQueue() > 0) {
					s+= String.format(Lang.FileTFTooltip2, other.nrOfFilesInQueue(),
							SizeEnum.getReadableSize(other.sizeOfFilesInQueue()));
				}
				return s;
			}
		};
	}

	@Override
	public void setFocus() {
		table.setFocus(); 
	}
	
	
	
	@Override
	public void dispose() {
		update.clear();
		List<ConnectionHandler> chs = ApplicationWorkbenchWindowAdvisor.get().getAllConnectionHandler();
		for (ConnectionHandler ch:chs) {
			ch.deleteObserver(this);
			ch.deleteTransferObserver(transferListener2);
		}
		super.dispose();
	}


	public void update(IObservable<StatusObject> o, StatusObject arg) {
	//	logger.debug("addin so: "+arg.getDetail()+"   "+arg	+"\n" +(arg.getDetailObject() != null?arg.getDetailObject().getClass().getSimpleName():"") );
		switch(arg.getDetail()) {
		case ConnectionHandler.USER_IDENTIFIED_IN_CONNECTION:
			if (arg.getDetailObject() != null) {
				update.remove(arg.getDetailObject());
			}
			update.add(arg.getValue());
			break;
		case ConnectionHandler.CONNECTION_CLOSED:
			update.remove(arg.getValue());
			ClientProtocolStateMachine cpsm = (ClientProtocolStateMachine)arg.getDetailObject();
			if (cpsm != null && cpsm.isActive()) { 
				update.add(cpsm);
			}
			break;
			
		case ConnectionHandler.TRANSFER_STARTED:
			update.remove(arg.getValue());
			update.add(arg.getValue()); //do structural change.. sorting..
	//		((IFileTransfer)arg.getDetailObject()).addObserver(transferListener);
			break;
		case ConnectionHandler.TRANSFER_FINISHED:
			update.remove(arg.getValue());
			update.add(arg.getValue()); //do structural change.. sorting..
	//		((IFileTransfer)arg.getDetailObject()).deleteObserver(transferListener);
			break;
			
		case ConnectionHandler.STATEMACHINE_CREATED:
		//	logger.debug("adding cpsm2: "+((ClientProtocolStateMachine)arg.getDetailObject()).getUser());
			update.add(arg.getDetailObject());
			break;
		case ConnectionHandler.STATEMACHINE_CHANGED:
			update.change(arg.getDetailObject());
			break;
		case ConnectionHandler.STATEMACHINE_DESTROYED:
			update.remove(arg.getDetailObject());
			break;
		}
	}
		
	
//	private final IObserver<TransferChange> transferListener = new IObserver<TransferChange>() {
//		public void update(final IObservable<TransferChange> o, TransferChange arg) {
//			new SUIJob() {
//				@Override
//				public void run() { 
//					if (!table.isDisposed()) {
//						tableViewer.refresh(((AbstractFileTransfer)o).getClientProtocol());
//					} else {
//						o.deleteObserver(transferListener);
//					}
//				}
//			}.schedule();
//		}
//	};

	private final IObserver<StatusObject> transferListener2 = new IObserver<StatusObject>() {
		@Override
		public void update(IObservable<StatusObject> o, final StatusObject arg) {
			new SUIJob(table) {
				@Override
				public void run() { 
					tableViewer.refresh(((AbstractFileTransfer)arg.getValue()).getClientProtocol());
				}
			}.schedule();
			
		}
	};



	/**
	 * 
	 * standard content provider will fetch ClientProtocols 
	 * and StateMachines 
	 * 
	 * @author Quicksilver
	 *
	 */
	public static class TransferContentProvider implements  IStructuredContentProvider  {


		public Object[] getElements(Object inputElement) {
			List<?> lch = (List<?>)inputElement; 
			HashSet<Object> active = new HashSet<Object>();
			for (Object ch:lch) {
				active.addAll(((ConnectionHandler)ch).getActive());
			}
			return active.toArray();
		}


		public void dispose() {}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

	}


}
