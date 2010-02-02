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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((original == null) ? 0 : original.hashCode());
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
		ADLFileListFile other = (ADLFileListFile) obj;
		if (original == null) {
			if (other.original != null)
				return false;
		} else if (!original.equals(other.original))
			return false;
		return true;
	}
	
	
	
}
