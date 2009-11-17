package uc.files.filelist;

import java.io.File;
import java.util.Date;

import uc.crypto.HashValue;


/**
 * file fore persistence of hashed files..
 * 
 * @author quicksilver
 *
 */
public class HashedFile {

	
	private final Date lastChanged; //when the file on the disc was changed last..
	
	private final HashValue tthRoot; //the root hash of the File..
	
	private final File path; //the path of the file on the disc..
	
	public HashedFile(Date date,HashValue hash,File path) {
		this.lastChanged = new Date(date.getTime());
		this.tthRoot = hash;
		this.path = path;
	}

	public Date getLastChanged() {
		return new Date(lastChanged.getTime());
	}

	public HashValue getTTHRoot() {
		return tthRoot;
	}

	public File getPath() {
		return path;
	}
	
	public String toString() {
		return path +" "+tthRoot+" "+lastChanged.getTime();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final HashedFile other = (HashedFile) obj;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		return true;
	}
	
	
	
	
}
