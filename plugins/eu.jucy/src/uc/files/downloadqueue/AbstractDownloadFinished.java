package uc.files.downloadqueue;

import java.io.File;

/**
 * call back interface
 * for finished transfers
 * with this interface jobs can register to receive notification 
 * when a download is finished..
 *  
 * @author Quicksilver
 *
 **/
public abstract class AbstractDownloadFinished {
	
	protected boolean execute = true;
	
	
	
	
	public boolean isExecute() {
		return execute;
	}

	public void setExecute(boolean execute) {
		this.execute = execute;
	}


	public String getId() {
		return getClass().getName();
	}
	
	/**
	 * 
	 * @param f - where the file is now
	 */
	public abstract void finishedDownload(File f);
	
	
	/**
	 * shows this to the user as option to disable 
	 * if returned String is not null
	 * 
	 * @return description shown to suer..
	 */
	public String showToUser() {
		return null;
	}
	
	public int hashCode() {
		return getClass().hashCode();
	};
	
	public boolean equals(Object o) { 
		return o != null && getClass().equals(o.getClass());
	}
	
}