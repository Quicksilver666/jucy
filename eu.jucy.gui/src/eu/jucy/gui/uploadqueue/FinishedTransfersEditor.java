package eu.jucy.gui.uploadqueue;

import helpers.IObservable;
import helpers.SizeEnum;
import helpers.StatusObject;
import helpers.Observable.IObserver;


import java.util.Arrays;




import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import org.eclipse.swt.widgets.Table;




import uc.files.IUploadQueue;
import uc.files.UploadQueue.TransferRecord;
import uihelpers.SUIJob;
import uihelpers.TableViewerAdministrator;


import eu.jucy.gui.GUIPI;
import eu.jucy.gui.Lang;

import eu.jucy.gui.UCEditor;
import eu.jucy.gui.UserColumns.NameUserCol;
import eu.jucy.gui.itemhandler.CommandDoubleClickListener;
import eu.jucy.gui.itemhandler.OpenDirectoryHandler;
import eu.jucy.gui.uploadqueue.FinishedTransfersColumn.DurationCol;
import eu.jucy.gui.uploadqueue.FinishedTransfersColumn.FinishedCol;
import eu.jucy.gui.uploadqueue.FinishedTransfersColumn.NameTransfCol;
import eu.jucy.gui.uploadqueue.FinishedTransfersColumn.SizeCol;
import eu.jucy.gui.uploadqueue.FinishedTransfersColumn.SpeedCol;
import eu.jucy.gui.uploadqueue.FinishedTransfersColumn.StartedCol;

public class FinishedTransfersEditor extends UCEditor implements IObserver<StatusObject> {

	public static final String ID = "eu.jucy.gui.FinishedUploads";
	public static final String ID2 = "eu.jucy.gui.FinishedDownloads";
	public static final String PopupID = "eu.jucy.gui.FinishedTransfers";

	

	
	private Label averageSpeed;
	private Label totalSize;
	private Label totalElements;
	private Table table;
	private TableViewer tableViewer;
	
	private TableViewerAdministrator<TransferRecord> tva;
	
	
	private IUploadQueue transfers;
	
	
	@SuppressWarnings("unchecked")
	@Override
	public void createPartControl(Composite arg0) {
		transfers = ((FinishedTransfersEditorInput)getEditorInput()).getInput();
		
		final GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 4;
		arg0.setLayout(gridLayout);

		tableViewer = new TableViewer(arg0, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
		table = tableViewer.getTable();
		table.setHeaderVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
		
		
	
		tva = new TableViewerAdministrator<TransferRecord>(tableViewer,
				Arrays.asList(NameUserCol.<TransferRecord>get() ,new NameTransfCol(),new SizeCol(),
						new DurationCol(), new SpeedCol(),new StartedCol(),new FinishedCol()),
				GUIPI.finishedTransfersTable,5);
		tva.apply();
		//tableViewer.setComparator(new StartedCol().getComparator(false));

		final Label label = new Label(arg0, SWT.NONE);
		label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

		totalElements = new Label(arg0, SWT.BORDER);
		totalElements.setLayoutData(new GridData(SWT.DEFAULT, SWT.DEFAULT));
	

		totalSize = new Label(arg0, SWT.BORDER);
		totalSize.setLayoutData(new GridData(SWT.DEFAULT, SWT.DEFAULT));
	

		averageSpeed = new Label(arg0, SWT.BORDER);
		averageSpeed.setLayoutData(new GridData(SWT.DEFAULT, SWT.DEFAULT));
	
		
		tableViewer.setContentProvider(new FinishedTransfersProvider());
		transfers.addObserver(this);
		tableViewer.setInput(transfers);
		updateLabels();
		//makeActions();
		getSite().setSelectionProvider(tableViewer);
		createContextPopup(PopupID, tableViewer);
		tableViewer.addDoubleClickListener(new CommandDoubleClickListener(OpenDirectoryHandler.ID));
		
		setControlsForFontAndColour(tableViewer.getTable());
	}
	
//	private void makeActions() {
//		final IWorkbenchWindow window = getSite().getWorkbenchWindow();
//		PathMenuManager menuManager = new PathMenuManager();
//		actions.addAll(UserActions.createUserActions(tableViewer, window, menuManager,false));
//		
//		Menu menu = menuManager.createContextMenu(tableViewer.getControl());
//    	tableViewer.getControl().setMenu(menu);
//    	
//    	
//    	
//    	final OpenDirectoryAction oda = new OpenDirectoryAction(tableViewer);
//    	actions.add(oda);
//    	tableViewer.addDoubleClickListener(new IDoubleClickListener() {
//			public void doubleClick(DoubleClickEvent event) {
//				if (oda.isEnabled()) {
//					oda.run();
//				}
//			}
//    		
//    	});
//	}
	
	
	
	@Override
	public void dispose() {
		transfers.deleteObserver(this);
		

		
		super.dispose();
		
	}

	


	public void update(IObservable<StatusObject> arg0, final StatusObject so) {
		//final StatusObject so = (StatusObject)arg1;
		if (so.getValue() instanceof TransferRecord) {
			new SUIJob() {
				@Override
				public void run() {
					switch(so.getType()) {
					case ADDED:
						tableViewer.add(so.getValue());
						break;
					case CHANGED:
						tableViewer.refresh(so.getValue());
						break;
					case REMOVED:
						tableViewer.remove(so.getValue());
						break;
					}
					updateLabels();
				}
				
			}.schedule();
		}
	}



	private void updateLabels() {
		IUploadQueue uq = transfers;
		totalElements.setText(String.format(
				Lang.TotalElements, 
				uq.getUploadRecordsSize()));
		
		totalSize.setText(String.format(Lang.TotalSize, 
				SizeEnum.getReadableSize(uq.getTotalSize())));
		
		averageSpeed.setText(String.format(Lang.AverageSpeed, 
				SizeEnum.toSpeedString(uq.getTotalDuration(), uq.getTotalSize())));
		
		averageSpeed.pack(true);
		totalSize.pack(true);
		totalElements.pack(true);
		
		totalElements.getParent().layout();
		
		
	}

	@Override
	public void setFocus() {
		table.setFocus();
	}
	
	/*
	public static class OpenDirectoryAction extends Action implements IWorkbenchAction, ISelectionChangedListener {
		private static final String ID = "eu.jucy.uploadqueue.OpenDirectory";
		
		private IStructuredSelection selection;
		private final IPostSelectionProvider provider;
		public OpenDirectoryAction(IPostSelectionProvider  provider) {
			this.provider = provider;
			setId(ID);
			provider.addPostSelectionChangedListener(this);
		}
		
		public void selectionChanged(IWorkbenchPart part, ISelection incoming) {
			if (incoming instanceof IStructuredSelection) {
				selection = (IStructuredSelection)incoming;
				boolean enabled = selection.getFirstElement() instanceof TransferRecord;
				setEnabled(enabled &&   ((TransferRecord)selection.getFirstElement()).getFile() != null);
			} else {
				setEnabled(false);
			}
		}
		
		public void selectionChanged(SelectionChangedEvent event) {
			selectionChanged(null,event.getSelection());
		}
		
		public void dispose() {
			provider.removePostSelectionChangedListener(this);
		}
		
		public void run() {
			TransferRecord tr = (TransferRecord)selection.getFirstElement();
			File folder = tr.getFile().getParentFile();
			logger.debug("launching Program");
			Program.launch(folder.getPath());
			logger.debug("launched Program");
			
		}
	} */

}
