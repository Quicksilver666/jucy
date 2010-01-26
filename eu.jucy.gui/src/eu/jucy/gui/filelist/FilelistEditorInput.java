package eu.jucy.gui.filelist;

import org.eclipse.core.runtime.Assert;


import eu.jucy.gui.UCEditorInput;



import uc.files.IDownloadable;
import uc.files.filelist.FileListDescriptor;

public class FilelistEditorInput extends UCEditorInput {

	private final FileListDescriptor filelist;
	private final IDownloadable initialSelection;

	


	public FilelistEditorInput(FileListDescriptor filelist,IDownloadable initialSelection) {
		Assert.isNotNull(filelist);
		this.filelist = filelist;
		this.initialSelection = initialSelection;
	}
	


	public String getName() {
		return filelist.getUsr().getNick();
	}
	
	public FileListDescriptor getFilelistDescriptor() {
		return filelist;
	}



	public String getToolTipText() {
		String tt = filelist.getUsr().getNick();
		if (filelist.getUsr().getHub()!= null) {
			tt += " - "+filelist.getUsr().getHub().getHubname();
		}
		return tt;
	}


	public IDownloadable getInitialSelection() {
		return initialSelection;
	}



	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((filelist == null) ? 0 : filelist.hashCode());
		return result;
	}



	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		FilelistEditorInput other = (FilelistEditorInput) obj;
		if (filelist == null) {
			if (other.filelist != null)
				return false;
		} else if (!filelist.equals(other.filelist))
			return false;
		return true;
	}

	

}
