package uc.files.filelist;

import uc.files.IDownloadable;

public interface IFileListItem  extends IDownloadable {

	/**
	 * 
	 * @return the parent folder this item is in
	 * null for the root folder.
	 */
	FileListFolder getParent();
	
}
