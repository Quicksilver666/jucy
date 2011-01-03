package uc.files;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import uc.IUser;
import uc.crypto.HashValue;
import uc.files.AbstractDownloadable.AbstractDownloadableFile;
import uc.files.search.ISearchResult;


/**
 * 
 * This class is for representing multiple SearchResults in one
 * single result ..
 * 
 * @author quicksilver
 *
 */
public class MultiUserAbstractDownloadable extends AbstractDownloadableFile implements ISearchResult {
	
	private List<IDownloadableFile> files = new ArrayList<IDownloadableFile>();
	
	public MultiUserAbstractDownloadable(IDownloadableFile file) {
		this.files.add(file);
	}
	
	public void addFile(IDownloadableFile file) {
		
		for (IDownloadableFile f: files) {
			if (f.getUser().equals(file.getUser())) {
				return; //don't add if already present..
			}
		}
		files.add(file);
	}
	
	private IDownloadableFile getMostCommon() {
		return files.get(0); //TODO most common Name is needed..
	}
	
	@Override
	public String getPath() {
		return getMostCommon().getPath();
	}

	@Override
	public HashValue getTTHRoot() throws UnsupportedOperationException {
		return files.get(0).getTTHRoot();
	}

	@Override
	public IUser getUser() {
		return files.get(0).getUser();
	}
	
	
	@Override
	public Collection<IUser> getIterable() {
		List<IUser> users = new ArrayList<IUser>();
		for (IDownloadableFile file: files) {
			users.addAll(file.getIterable());
		}
		
		return users;
	}
	
	

	public long getSize() {
		return files.get(0).getSize();
	}

	

	public int getAvailabelSlots() {
		int i = 0;
		for (IDownloadableFile f:files) {
			if (f instanceof ISearchResult) {
				ISearchResult isr = (ISearchResult)f;
				if (isr.getAvailabelSlots() > 0) {
					i++;
				}
			}
		}
		return i;
	}

	
	public int getTotalSlots() {
		return files.size();
	}

	public int nrOfUsers() {
		int i = 0;
		for (IDownloadable d:files) {
			i+= d.nrOfUsers();
		}
		return i;
	}

	public List<IDownloadableFile> getFiles() {
		return files;
	}

	public String getToken() {
		return null;
	}
	
	
	
	

}
