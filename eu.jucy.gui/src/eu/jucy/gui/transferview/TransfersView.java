package eu.jucy.gui.transferview;



import java.io.File;
import java.util.Arrays;



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
import eu.jucy.gui.UCView;
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
import uc.files.transfer.IFileTransfer;
import uc.files.transfer.TransferChange;
import uc.protocols.client.ClientProtocol;
import uc.protocols.client.ClientProtocolStateMachine;
import uihelpers.DelayedUpdate;
import uihelpers.SUIJob;
import uihelpers.TableViewerAdministrator;
import uihelpers.ToolTipProvider;

public class TransfersView extends UCView  implements IObserver<StatusObject> {

	public static Logger logger = LoggerFactory.make();
	

	public static final String ID = "eu.jucy.gui.Transfers";
	


	private TableViewer tableViewer;
//	private volatile boolean running = false;
//	private final List<StatusObject> sobjects = 
//		Collections.synchronizedList(new ArrayList<StatusObject>());
	
	private Table table;
	

	private TableViewerAdministrator<Object> tva; 
	private DelayedUpdate<Object> update; 
	

	@Override
	public void createPartControl(final Composite parent) {
		
		table = new Table(parent,SWT.BORDER | SWT.FULL_SELECTION);
		
		tableViewer =  new TableViewer(table);
		
		table.setHeaderVisible(true);
		
		UCProgressPainter.AddToTable(table);
		
		
		tva = new TableViewerAdministrator<Object>(tableViewer,
				Arrays.asList(new UserColumn(),new HubColumn(),
						new StatusColumn(), new TimeLeftColumn(),
						new SpeedColumn(), new FilenameColumn(),
						new SizeColumn(), new IPColumn()),GUIPI.transfersViewTable,2);
						
		tva.apply();
		
		update = new DelayedUpdate<Object>(tableViewer);
		
		TransferContentProvider tcp = new TransferContentProvider();
		tableViewer.setContentProvider(tcp);
		setControlsForFontAndColour(table);
		
		getSite().setSelectionProvider(tableViewer);   

		//makeActions();
		createContextPopup(ID, tableViewer);
		
		ConnectionHandler ch = ApplicationWorkbenchWindowAdvisor.get().getCh();
		tableViewer.setInput(ch);
		ch.addObserver(this);
		for (Object o : ch.getActive()) {
			if (o instanceof ClientProtocol) {
				IFileTransfer ift = ((ClientProtocol)o).getFileTransfer();
				if (ift != null) {
					ift.addObserver(transferListener);
				}
			}
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
						s = "File: "+f.getPath(); //TODO translation
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
					s+=" Files in Queue: "+other.nrOfFilesInQueue(); //TODO Translation
					s+="\n Size in Queue: "+SizeEnum.getReadableSize(other.sizeOfFilesInQueue());
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
		ApplicationWorkbenchWindowAdvisor.get().getCh().deleteObserver(this);
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
			//	logger.debug("adding cpsm1: "+cpsm.getUser());
				update.add(cpsm);
			}
			break;
			
		case ConnectionHandler.TRANSFER_STARTED:
			update.remove(arg.getValue());
			update.add(arg.getValue()); //do structural change.. sorting..
			((IFileTransfer)arg.getDetailObject()).addObserver(transferListener);
			break;
		case ConnectionHandler.TRANSFER_FINISHED:
			update.remove(arg.getValue());
			update.add(arg.getValue()); //do structural change.. sorting..
			((IFileTransfer)arg.getDetailObject()).deleteObserver(transferListener);
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
		
//		
//		sobjects.add(arg);
//		if (!running) {
//			running = true;
//			new SUIJob(table) {
//				@Override
//				public void run() {
//					running = false;
//					synchronized(sobjects) {
//						for (StatusObject so: sobjects) {
//							switch(so.getDetail()) {
//							case ConnectionHandler.USER_IDENTIFIED_IN_CONNECTION:
//								if (so.getDetailObject() != null) {
//									tableViewer.remove(so.getDetailObject());
//								}
//								tableViewer.add(so.getValue());
//								break;
//							case ConnectionHandler.CONNECTION_CLOSED:
//								tableViewer.remove(so.getValue());
//								ClientProtocolStateMachine cpsm = (ClientProtocolStateMachine)so.getDetailObject();
//								if (cpsm != null) {
//									logger.debug("adding cpsm "+cpsm.isActive());
//								}
//								if (cpsm != null && cpsm.isActive()) { 
//									logger.debug("adding cpsm1: "+cpsm.getUser());
//									tableViewer.add(cpsm);
//								}
//								break;
//								
//							case ConnectionHandler.TRANSFER_STARTED:
//								tableViewer.remove(so.getValue());
//								tableViewer.add(so.getValue()); //do structural change.. sorting..
//								((IFileTransfer)so.getDetailObject()).addObserver(transferListener);
//								break;
//							case ConnectionHandler.TRANSFER_FINISHED:
//								tableViewer.remove(so.getValue());
//								tableViewer.add(so.getValue()); //do structural change.. sorting..
//								((IFileTransfer)so.getDetailObject()).deleteObserver(transferListener);
//								break;
//								
//							case ConnectionHandler.STATEMACHINE_CREATED:
//								logger.debug("adding cpsm2: "+((ClientProtocolStateMachine)so.getDetailObject()).getUser());
//								tableViewer.add(so.getDetailObject());
//								break;
//							case ConnectionHandler.STATEMACHINE_CHANGED:
//								tableViewer.refresh(so.getDetailObject());
//								break;
//							case ConnectionHandler.STATEMACHINE_DESTROYED:
//								tableViewer.remove(so.getDetailObject());
//								break;
//							}
//						}
//						sobjects.clear();
//					}
//				}
//			
//			}.schedule();
//		}
//	}

	
/*	public void update(IObservable<StatusObject> o, StatusObject arg) {
		sobjects.add(arg);
		if (!running) {
			running = true;
			UIJob uij = new UIJob("ui") { 
				
				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					running = false;
					
					if (table.isDisposed()) {
						return Status.OK_STATUS;
					}
					synchronized(sobjects) {
						for (StatusObject so: sobjects) {
							if (so.getValue() == null) {
								tableViewer.refresh();
							} else {
								switch(so.getType()) {
								case ADDED:
									tableViewer.add(so.getValue());
									logger.debug("added"+so.getValue().getClass().getSimpleName());
									break;
								case REMOVED:
									tableViewer.remove(so.getValue());
									break;
								case CHANGED:
									tableViewer.refresh(so.getValue());
									if (so.getValue() instanceof ClientProtocol) {
										tableViewer.remove(so.getValue());
										tableViewer.add(so.getValue());
										
										ClientProtocol cp = (ClientProtocol)so.getValue();
										AbstractFileTransfer ft = cp.getFileTransfer();
										if (ft != null) {
											ft.addObserver(new TransferListener(cp, ft)); 
										} else {
											if (Platform.inDevelopmentMode()) {
												logger.warn("FT null: "+cp.getFti().getOther());
											}
										}
									}
									
									break;
								}
							}
						}
						sobjects.clear();
					}
					return Status.OK_STATUS;
				}	
			};
			uij.setSystem(true);
			uij.schedule();
		}
	} */
	
	private final IObserver<TransferChange> transferListener = new IObserver<TransferChange>() {
		public void update(final IObservable<TransferChange> o, TransferChange arg) {
			new SUIJob() {
				@Override
				public void run() { 
					if (!table.isDisposed()) {
						tableViewer.refresh(((AbstractFileTransfer)o).getClientProtocol());
					} else {
						o.deleteObserver(transferListener);
					}
				}
			}.schedule();
		}
	};




/*	private final class TransferListener implements IObserver<TransferChange>  {
		private final ClientProtocol cp;
		private final AbstractFileTransfer ft;

		private TransferListener(ClientProtocol cp, AbstractFileTransfer ft) {
			this.cp = cp;
			this.ft = ft;
		}

		@Override
		public boolean equals(Object obj) {
			return getClass().equals(obj.getClass());
		}

		@Override
		public int hashCode() {
			return getClass().hashCode();
		}

		
		
		public void update(IObservable<TransferChange> o, TransferChange change) {
			new SUIJob() {
				@Override
				public void run() { 
					if (!table.isDisposed()) {
						tableViewer.refresh(cp);
					}
				}
			}.schedule();
			
			if (change == TransferChange.FINISHED) {
				ft.deleteObserver(this);
			}
			
		} */

	/*	public void transferChanged(TransferChange change,IFileTransfer ift) {
			new SUIJob() {
				@Override
				public void run() { 
					if (!table.isDisposed()) {
						tableViewer.refresh(cp);
					}
				}
			}.schedule();
			
			if (change == TransferChange.FINISHED) {
				ft.unregisterTransferListener(this);
			}
		} */
	//}

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
			ConnectionHandler ch = ((ConnectionHandler)inputElement) ;
			return ch.getActive().toArray();
		}


		public void dispose() {}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

	}


}
