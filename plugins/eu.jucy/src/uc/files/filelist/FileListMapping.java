package uc.files.filelist;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import helpers.IFilter;
import helpers.ISubstringMapping2;

public class FileListMapping implements ISubstringMapping2<IFileListItem> {


	public String getMappingString(IFileListItem item) {
		return item.getName();
	}

	
	
	public static class FileFilter implements IFilter<IFileListItem>{

		private final long minSize;
		private final long maxSize;
		private final long equalsize;
		
		private final Collection<String> endings;
		
		private final boolean folder;
		
		/**
		 * 
		 * @param minsize  minimum filesize. 0 if not used
		 * @param maxsize  maximum Filesize . MaxLong if not used
		 * @param equalsize  equals filesize   -1 if not used
		 * @param endings
		 * @param folder
		 */
		public FileFilter(long minsize, long maxsize , long equalsize,Collection<String> endings, boolean folder) {
			this.minSize = minsize;
			this.maxSize = maxsize;
			
			this.equalsize = equalsize;
			this.endings = endings;
			this.folder = folder;
		}



		public boolean filter(IFileListItem item) {
			if (folder) {
				return true;
			} else {
				FileListFile f = (FileListFile)item;
				if (endings.isEmpty() || endings.contains(f.getEnding()) ) {
					if (f.getSize() < minSize || f.getSize() > maxSize || (equalsize != -1 && f.getSize() == equalsize) ) {
						//if (maxSize ? f.getSize() > size :f.getSize() < size) {
						return false;
						//}
					}
					return true;
				}
	
				return false;
			}
		}
		
		/**
		 * Either only takes files or it takes Folders..
		 */
		public Set<IFileListItem> mapItems(Set<IFileListItem> nodeItems) {
			Set<IFileListItem> found = new HashSet<IFileListItem>();
			if (folder) {
				for (IFileListItem f: nodeItems) { //just add all folders
					if (!f.isFile()) {
						found.add(f);
					}
				}
			} else {
				for (IFileListItem f: nodeItems) { //add all files or add containing files of folders
					if (f.isFile()) {
						found.add(f);
					} else {
						for (FileListFile file: ((FileListFolder)f)) {
							found.add(file);
						}
					}
				}
			}
			return found;
		}
	}
	
	
	
}
