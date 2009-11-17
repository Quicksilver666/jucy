package eu.jucy.adlsearch;

import uc.crypto.HashValue;
import uc.files.filelist.FileListFile;
import uc.files.filelist.FileListFolder;

/**
 * used to mark difference between normal FileListFile and 
 * FileListFile added by ADL search
 * 
 * @author Quicksilver
 *
 */
public class ADLFileListFile extends FileListFile {

	private final FileListFile original;
	
	public ADLFileListFile(FileListFolder parent, String filename, long size,
			HashValue tth, FileListFile original) {
		super(parent, filename, size, tth);
		this.original = original;
	}

	public String getPath() {
		return original.getPath();
	}
	
	
}
