package uc.files.downloadqueue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DQE folder is made for creating a file system like 
 * Structure a  for all the DQEs
 * 
 * @author Quicksilver
 */

public class DownloadQueueFolder {
	
		private final Map<String,DownloadQueueFolder> childsFold 	= 
			Collections.synchronizedMap(new HashMap<String,DownloadQueueFolder>());
		private final Map<String,AbstractDownloadQueueEntry>  childdqe = 
			Collections.synchronizedMap(new HashMap<String,AbstractDownloadQueueEntry>());
		
		private final DownloadQueueFolder parent;
		private final String name;
		
		public DownloadQueueFolder(DownloadQueueFolder parent, String name){
			this.parent	= parent;
			this.name	= name;
			if (parent != null) {
				parent.add(this);
			}
	
		}
		
		public DownloadQueueFolder getFolder(String name){
			return childsFold.get(name);
		}

		public AbstractDownloadQueueEntry getDQE(String name){
			return childdqe.get(name);
		}
		
		public Object[] getChildren() {
			ArrayList<Object> children = new ArrayList<Object>();
			synchronized(childsFold){
				children.addAll(childsFold.values());
			}
			synchronized(childdqe){
				children.addAll(childdqe.values());
			}
			return children.toArray();
		}
		
		/**
		 * recursively calculates all AbstractDownloadQueueEntrys
		 * in this folder..
		 * @return all entries contained by this  or any child folder
		 */
		public List<AbstractDownloadQueueEntry> getAllDQEChildren() {
			List<AbstractDownloadQueueEntry> adq = 
				new ArrayList<AbstractDownloadQueueEntry>();
			
			synchronized(childsFold) {
				for (DownloadQueueFolder dqf:childsFold.values()) {
					adq.addAll(dqf.getAllDQEChildren());
				}
			}
			
			synchronized(childdqe) {
				adq.addAll(childdqe.values());
			}
			return adq;
			
		}
		
		public void add(DownloadQueueFolder child){
			childsFold.put(child.name , child );
		}
		
		public void add(AbstractDownloadQueueEntry child) {
			childdqe.put( child.getFileName() ,child);	
		}

		public String getName() {
			return name;
		}

		public DownloadQueueFolder getParent() {
			return parent;
		}
		
		/**
		 * 
		 * @return true if this has exactly one 
		 * Folder as child buit no files
		 */
		public boolean oneFolderChildNothingElse() {
			return childsFold.size() == 1 && childdqe.isEmpty();
		}
		
		public void removeFromDQE(String what) {
			childdqe.remove(what);
		}
		
		public void removeFromFolder(String what) {
			childsFold.remove(what);
		}
		
		public boolean hasChildFolders() {
			return !childsFold.isEmpty();
		}
		
		public File getPath() {
			if (parent != null) {
				File parentPath = parent.getPath();
				return parentPath == null ? new File(name) : new File(parentPath,name);
			} else {
				return null;
			}
		}
		
		/**
		 * 
		 * @return not just the path of this item
		 * though the path that would be shown in the GUI..
		 */
		public File getShownPath() {
			File path = getPath();
			DownloadQueueFolder current = this;
			while (current.oneFolderChildNothingElse()) {
				current = (DownloadQueueFolder)current.childsFold.values().toArray()[0];
				path = new File(path,current.name);
			}
			return path;
		}
}
