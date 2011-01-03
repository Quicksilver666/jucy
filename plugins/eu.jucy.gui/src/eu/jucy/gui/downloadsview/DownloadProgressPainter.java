package eu.jucy.gui.downloadsview;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import eu.jucy.gui.downloadqueue.DQProgressPainter;
import eu.jucy.gui.transferview.UCProgressPainter;

import uc.files.downloadqueue.AbstractDownloadQueueEntry;
import uc.files.transfer.IFileTransfer;
import uc.protocols.client.ClientProtocol;
import uihelpers.SUIJob;

public class DownloadProgressPainter implements Listener, ISelectionChangedListener {

	public static void addPainter(TreeViewer treeViewer) {
		final Tree tree = treeViewer.getTree(); 
		DownloadProgressPainter dpp = new DownloadProgressPainter();
		tree.addListener(SWT.PaintItem,dpp);
		treeViewer.addSelectionChangedListener(dpp);
		new SUIJob(tree) { 
			@Override
			public void run() {
				tree.redraw();
				schedule(500);
			}
		}.schedule(1000);
		
	}
	
	private IFileTransfer selected;
	private AbstractDownloadQueueEntry parentOfSlected;
	
	public void selectionChanged(SelectionChangedEvent event) {
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		if (sel.getFirstElement() instanceof ClientProtocol) {
			IFileTransfer ft =  (IFileTransfer) ((ClientProtocol)sel.getFirstElement()).getFileTransfer();
			selected = ft;
			parentOfSlected =  selected.getFti().getDqe();
		} else {
			selected = null;
			parentOfSlected = null;
		}
		
	}
	
	public void handleEvent(Event event) {
		if (event.index != 1) {
			return;
		}
		TreeItem ti = (TreeItem)event.item;
		Object o = ti.getData();
		
		if (o instanceof ClientProtocol) {
			IFileTransfer ft = ((ClientProtocol)o).getFileTransfer();
			if (ft != null) {
				UCProgressPainter.drawFileTransfer(ft , event.gc,event);
			}
		} else if (o instanceof AbstractDownloadQueueEntry) {
			
			DQProgressPainter.drawADQE(
					(AbstractDownloadQueueEntry)o, 
					ti.getParent(),
					event.gc,
					event,
					o.equals(parentOfSlected)? selected:null);
			
		}
	}

}
