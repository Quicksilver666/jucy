package uc.files.filelist;

import uc.files.IDownloadable;

public interface IFileListItem  extends IDownloadable {

	/**
	 * 
	 * @return the parent folder this item is in
	 * null for the root folder.
	 */
	FileListFolder getParent();
	
	
	/**
	 * 
	 * @return true if this item was originally in the filelist
	 * and was not added by some processor plugin
	 */
	boolean isOriginal();
	
	/**
	 * 
	 * @return the size of this item
	 * contained size for folder
	 */
	long getSize();
	
}
