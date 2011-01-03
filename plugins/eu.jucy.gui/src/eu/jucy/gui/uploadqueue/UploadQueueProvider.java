/**
 * 
 */
package eu.jucy.gui.uploadqueue;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import uc.files.IUploadQueue;

public class UploadQueueProvider implements IStructuredContentProvider {

	
	public void dispose() {}

	
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

	
	public Object[] getElements(Object inputElement) {
		return ((IUploadQueue)inputElement).getUploadInfos().toArray();
	}
	
}