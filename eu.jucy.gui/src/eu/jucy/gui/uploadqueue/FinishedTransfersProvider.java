package eu.jucy.gui.uploadqueue;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import uc.files.IUploadQueue;

public class FinishedTransfersProvider implements IStructuredContentProvider {

	
	public Object[] getElements(Object inputElement) {
		return ((IUploadQueue)inputElement).getTransferRecords().toArray();
	}

	
	public void dispose() {}

	
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

}
