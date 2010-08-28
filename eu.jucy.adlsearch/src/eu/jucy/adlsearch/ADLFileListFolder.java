package eu.jucy.adlsearch;

import uc.files.filelist.FileListFolder;

public class ADLFileListFolder extends FileListFolder {

	public ADLFileListFolder(FileListFolder parent, String foldername) {
		super(parent, foldername);
	}

	@Override
	public boolean isOriginal() {
		return false;
	}

	
	
}
