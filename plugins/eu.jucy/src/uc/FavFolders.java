package uc;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import logger.LoggerFactory;


import org.apache.log4j.Logger;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import uihelpers.ComplexListEditor;
import uihelpers.ComplexListEditor.IPrefSerializer;



/**
 * 
 * FavFolders  are special folders that are either shared (and all subfolders of it) (SharedDir)
 * of Folders that are preferred targets for downloads (FavDir).
 * 
 * @author Quicksilver
 *
 */
public class FavFolders {

	private static final Logger logger = LoggerFactory.make();
	
		
	private final List<SharedDir> sharedDirs;
	
	FavFolders() {
		sharedDirs = new CopyOnWriteArrayList<SharedDir>(SharedDir.loadSharedDirs());
		logger.debug("loaded Favdirs and SharedFolders");
	}

	
	public List<SharedDir> getSharedDirs() {
		return Collections.unmodifiableList(sharedDirs);
	}
	
	
	public void storeSharedDirs(List<SharedDir> shared) {
		sharedDirs.clear();
		sharedDirs.addAll(shared);
		String val = ComplexListEditor.createList(shared, new SharedDirTranslater());
		PI.put(PI.sharedDirs2, val);
		
		/*
		IEclipsePreferences pref = PI.get();
		try {
			sharedDirs.clear();
			sharedDirs.addAll(shared);
			
			Preferences shareddirs = pref.node(PI.sharedDirs);
			shareddirs.removeNode();
			shareddirs = pref.node(PI.sharedDirs);
			

			for (SharedDir sd:shared) {
				Preferences itemnode = shareddirs.node(sd.getName());
				sd.storeDir(itemnode);
			}
		
			pref.flush();
		} catch (BackingStoreException bse){
			logger.warn(bse,bse);
		} */
		
	}
	
	public static void storeFavDirs(List<FavDir> favdirs) {
		String val = ComplexListEditor.createList(favdirs, new FavDirTranslater());
		PI.put(PI.favDirs, val);
	}
	
	public static List<FavDir> getFavDirs() {
		return ComplexListEditor.parseString(PI.get(PI.favDirs), new FavDirTranslater());
	}
	
	
	public static class FavDir {
		
		
		protected String name;
		protected File directory;
		
		public FavDir(String name, File directory){
			this.name=name;
			this.directory=directory;
		}
		
		
		
		/*
		 * 
		 * @param itemnode node corresponding to the name
		 * of the FavDir
		 *
		protected void storeDir(Preferences itemnode) {
			itemnode.put(PI.sharedDirsName, name);
			itemnode.put(PI.sharedDirsdirectory, directory.getPath());
		} */
		
		/*
		public static void soreFavDirs(Collection<FavDir> favDirs){
			Preferences pref=Application.get().node(PI.favDirs);
			try {
				pref.removeNode();
				pref =  Application.get().node(PI.favDirs);
				for (FavDir fd: favDirs) {
					Preferences p = pref.node(fd.getName());
					p.put(PI.sharedDirsdirectory,fd.getDirectory().getAbsolutePath());
				}
				pref.flush();
			
			} catch(BackingStoreException bse){
				bse.printStackTrace();
			}
		} */
		
		/**
		 * @return the directory
		 */
		public File getDirectory() {
			return directory;
		}
		/**
		 * @param directory the directory to set
		 */
		public void setDirectory(File directory) {
			this.directory = directory;
		}
		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}
		/**
		 * @param name the name to set
		 */
		public void setName(String name) {
			this.name = name;
		}
		@Override
		public int hashCode() {
			final int PRIME = 31;
			int result = 1;
			result = PRIME * result + ((directory == null) ? 0 : directory.hashCode());
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
			final FavDir other = (FavDir) obj;
			if (directory == null) {
				if (other.directory != null)
					return false;
			} else if (!directory.equals(other.directory))
				return false;
			return true;
		}
		
		
	}
	
	public static class FavDirTranslater implements IPrefSerializer<FavDir> {
		public String[] serialize(FavDir t) {
			return new String[]{t.getName(),t.getDirectory().getPath()};
		}

		public FavDir unSerialize(String[] all) {
			return new FavDir(all[0], new File(all[1]));
		}
	}
	
	
	public static class SharedDirTranslater implements IPrefSerializer<SharedDir> {

		public String[] serialize(SharedDir t) {
			return new String[] {t.getName(),t.getDirectory().getPath(),""+t.lastShared};
		}

		public SharedDir unSerialize(String[] all) {
			return new SharedDir(all[0], new File(all[1]),Long.parseLong(all[2]));
		}
		
	}
	
	public static class SharedDir extends FavDir {
		
		private long lastShared;
		
		public SharedDir(String name, File directory,long lastshared){
			this(name,directory);
			this.lastShared = lastshared;
		}
		
		public SharedDir(String name, File directory){
			super(name,directory);
			lastShared = 0;
		}

		

		/**
		 * @return the lastShared
		 */
		public long getLastShared() {
			return lastShared;
		}
		/**
		 * @param lastShared the lastShared to set
		 */
		public void setLastShared(long lastShared) {
			this.lastShared = lastShared;
		}
		
		
		private static List<SharedDir> loadSharedDirs() {
			String sd2 = PI.get(PI.sharedDirs2);
			if (!sd2.equals(PI.LegacyMode)) {
				logger.debug("Loading shared dirs in Modern mode.");
				List<SharedDir> ret = ComplexListEditor.parseString(sd2, new SharedDirTranslater());
				return ret;
			} else {
				logger.debug("Loading shared dirs in Legacy mode.");
				Preferences pref = PI.get().node(PI.sharedDirs);
				List<SharedDir> ret = new ArrayList<SharedDir>();
				try{
					for (String child: pref.childrenNames()) {
						Preferences p = pref.node(child);
						SharedDir sharedDir = new SharedDir(child,
								new File(p.get(PI.sharedDirsdirectory, null)),
								p.getLong(PI.SharedDirslastShared, 0));
						ret.add(sharedDir);
					}
				} catch(BackingStoreException bse) {
					bse.printStackTrace();
				}
				return ret;
			}
		}
		


		@Override
		public int hashCode() {
			return super.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return super.equals(obj);
		}
		
		public boolean isOnline() {
			return getDirectory().isDirectory();
		}
		
		
	}

}
