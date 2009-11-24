package eu.jucy.gui.filelist;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;



import uc.files.IDownloadable;
import uc.files.filelist.FileListDescriptor;

public class FilelistEditorInput implements IEditorInput {

	private final FileListDescriptor filelist;
	private final IDownloadable initialSelection;

	


	public FilelistEditorInput(FileListDescriptor filelist,IDownloadable initialSelection) {
		Assert.isNotNull(filelist);
		this.filelist = filelist;
		this.initialSelection = initialSelection;
	}
	
	public boolean exists() {
		return false;
	}

	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	public String getName() {
		return filelist.getUsr().getNick();
	}
	
	public FileListDescriptor getFilelistDescriptor() {
		return filelist;
	}

	public IPersistableElement getPersistable() {
		return null;
	}

	public String getToolTipText() {
		String tt = filelist.getUsr().getNick();
		if (filelist.getUsr().getHub()!= null) {
			tt += " - "+filelist.getUsr().getHub().getHubname();
		}
		return tt;
	}

	@SuppressWarnings("unchecked")
	public Object getAdapter(Class adapter) {
		return null;
	}

	public IDownloadable getInitialSelection() {
		return initialSelection;
	}



}
