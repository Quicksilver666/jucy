package eu.jucy.gui.downloadsview;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


import helpers.IObservable;
import helpers.StatusObject;
import helpers.Observable.IObserver;
import helpers.StatusObject.ChangeType;



import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;


import uc.files.downloadqueue.AbstractDownloadQueueEntry;
import uc.files.downloadqueue.DownloadQueue;
import uc.files.transfer.IFileTransfer;
import uc.files.transfer.TransferChange;
import uc.protocols.client.ClientProtocol;
import uihelpers.DelayedTreeUpdater;
import uihelpers.SUIJob;
import uihelpers.TableViewerAdministrator;
import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;
import eu.jucy.gui.GUIPI;
import eu.jucy.gui.UCView;
import eu.jucy.gui.downloadsview.DownloadsColumns.FilesLeftCol;
import eu.jucy.gui.downloadsview.DownloadsColumns.StatusCol;
import eu.jucy.gui.downloadsview.DownloadsColumns.SizeLeftCol;
import eu.jucy.gui.downloadsview.DownloadsColumns.SpeedCol;
import eu.jucy.gui.downloadsview.DownloadsColumns.TimeLeftCol;
import eu.jucy.gui.downloadsview.DownloadsColumns.TotalSizeLeftCol;
import eu.jucy.gui.downloadsview.DownloadsColumns.TotalTimeLeftCol;
import eu.jucy.gui.downloadsview.DownloadsColumns.Transferrer;
import eu.jucy.gui.transferview.TransfersView;


public class DownloadsView extends UCView implements IObserver<StatusObject> {

	public static final String VIEW_ID = "eu.jucy.gui.downloadsview";
	
	private Tree tree;
	private TreeViewer treeViewer;
	private TableViewerAdministrator<Object> tva; 
	private DelayedTreeUpdater<Object> dtu;
	private DownloadQueue dq;

	private final CopyOnWriteArrayList<TransfersObserver> active = 
		new CopyOnWriteArrayList<TransfersObserver>();
	
	private class TransfersObserver implements IObserver<TransferChange> {
		
		private final IFileTransfer ift;
		private final Object adqe;
		
		public TransfersObserver(IFileTransfer ift,Object parent) {
			this.ift = ift;
			this.adqe = parent;
			active.add(this);
		}
		
		public void update(final IObservable<TransferChange> o, TransferChange arg) {
			dtu.change(((IFileTransfer)o).getClientProtocol(), adqe);
			dtu.change(adqe, dq);
			if (TransferChange.FINISHED == arg) {
				dispose();
			}
		}
		
		public void dispose() {
			ift.deleteObserver(this);
			active.remove(this);
		}
		
		@Override
		public int hashCode() {
			return getClass().hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) return false;
			return getClass().equals(obj.getClass());
		}
		
		
	} 
	
	
	@Override
	public void createPartControl(Composite parent) {
		tree = new Tree(parent,SWT.BORDER | SWT.FULL_SELECTION);
		tree.setHeaderVisible(true);
		
		treeViewer =  new TreeViewer(tree);
		treeViewer.setAutoExpandLevel(2);
		treeViewer.setContentProvider(new DownloadsContentProvider());
		dtu = new DelayedTreeUpdater<Object>(treeViewer);
		
		tva = new TableViewerAdministrator<Object>(treeViewer,
				Arrays.asList(new Transferrer(),new StatusCol(), new SpeedCol(),
						new TimeLeftCol(),new SizeLeftCol(),
				new FilesLeftCol(),new TotalTimeLeftCol(),new TotalSizeLeftCol() ),GUIPI.downloadsViewTable,0);
		
		tva.apply();
		getSite().setSelectionProvider(treeViewer);   
		dq = ApplicationWorkbenchWindowAdvisor.get().getDownloadQueue();
		
		DownloadProgressPainter.addPainter(treeViewer);
		treeViewer.setInput(dq);
		
		dq.addObserver(this);
		setControlsForFontAndColour(tree);
		
		createContextPopup(TransfersView.ID, treeViewer);
		for (AbstractDownloadQueueEntry adqe : dq.getAllRunningDQE()) {
			for (IFileTransfer ift: adqe.getRunningFileTransfers()) {
				ift.addObserver(new TransfersObserver(ift, adqe));
			}
		}
	}
	
	

	@Override
	public void dispose() {
		super.dispose();
		if (dq != null) {
			dq.deleteObserver(this);
		}
		for (TransfersObserver to : active) {
			to.dispose();
		}
	}



	@Override
	public void setFocus() {
		tree.setFocus();
	}

	public void update(IObservable<StatusObject> o, StatusObject arg) {
		if (arg.getType() == ChangeType.CHANGED) {
			switch(arg.getDetail()) {
			case DownloadQueue.DQE_FIRST_TRANSFER_STARTED:
				dtu.add(arg.getValue(), dq);
				new SUIJob(tree) {
					@Override
					public void run() {
						treeViewer.expandAll();
					}
				}.schedule(1000);
				break;
			case DownloadQueue.DQE_LAST_TRANSFER_FINISHED:
				dtu.remove(arg.getValue(), dq);
				break;
			case DownloadQueue.DQE_TRANSFER_STARTED:
				IFileTransfer ft = (IFileTransfer)arg.getDetailObject();
				dtu.add(ft.getClientProtocol(), arg.getValue());
				ft.addObserver(new TransfersObserver(ft,arg.getValue()));
				break;
			case DownloadQueue.DQE_TRANSFER_FINISHED:
				IFileTransfer ft2 = (IFileTransfer)arg.getDetailObject();
				dtu.remove(ft2.getClientProtocol(), arg.getValue());
				break;
			case DownloadQueue.FILEDQE_BLOCKSTATUSCHANGED:
				dtu.change(arg.getValue(),dq);
				break;
			}
		}
	}
	
	/**
	 * 
	 * standard content provider will fetch ClientProtocols 
	 * and StateMachines 
	 * 
	 * @author Quicksilver
	 *
	 */
	public static class DownloadsContentProvider implements  ITreeContentProvider  {


		public void dispose() {}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof AbstractDownloadQueueEntry) {
				List<ClientProtocol> cps = new ArrayList<ClientProtocol>();
				for (IFileTransfer ift: ((AbstractDownloadQueueEntry)parentElement).getRunningFileTransfers()) {
					cps.add(ift.getClientProtocol());
				}
				return cps.toArray();
			}
			return new Object[]{};
		}

		public Object getParent(Object element) {
			return null;
		}

		public boolean hasChildren(Object element) {
			return 	element instanceof AbstractDownloadQueueEntry || 
					element instanceof DownloadQueue;
		}

		public Object[] getElements(Object inputElement) {
			return ((DownloadQueue)inputElement).getAllRunningDQE().toArray();
		}
	}
	

}
