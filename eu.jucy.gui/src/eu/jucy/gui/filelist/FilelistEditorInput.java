package eu.jucy.gui.filelist;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;



import uc.files.filelist.FileListDescriptor;

public class FilelistEditorInput implements IEditorInput {

private final FileListDescriptor filelist;
//private final DownloadQueue dq;
	
	public FilelistEditorInput(FileListDescriptor filelist) {
		Assert.isNotNull(filelist);
		//Assert.isNotNull(dq);
		this.filelist = filelist;
	//	this.dq=dq;
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

	/*
	 * @return the dq
	 *
	public DownloadQueue getDq() {
		return dq;
	} */



}
