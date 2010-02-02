package eu.jucy.adlsearch;

import helpers.PreferenceChangedAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.preferences.InstanceScope;

import eu.jucy.adlsearch.ui.ADLFieldEditor;

import uc.files.filelist.FileList;
import uc.files.filelist.FileListFile;
import uc.files.filelist.FileListFolder;
import uc.files.filelist.IFilelistProcessor;

public class ADLFileListProcessor implements IFilelistProcessor {

	public static final String PLUGIN_ID = "eu.jucy.adlsearch";
	
	public static final String adlID = "ADLEntrys";
	
	private List<ADLSearchEntry> entrys = Collections.synchronizedList(new ArrayList<ADLSearchEntry>());
	
	
	
	public ADLFileListProcessor() {
		new PreferenceChangedAdapter(new InstanceScope().getNode(PLUGIN_ID),adlID) {
			@Override
			public void preferenceChanged(String preference,String oldValue,String newValue) {
				update();
			}
		};
		update();
	}
	
	private void update() {
		entrys.clear();
		List<ADLSearchEntry> allEntrys = ADLFieldEditor.loadFromString(new InstanceScope().getNode(PLUGIN_ID).get(adlID, null));
		synchronized(entrys) {
			for (ADLSearchEntry entry: allEntrys) {
				if (entry.canBeUsed()) {
					entrys.add(entry);
				}
			}
		}
	}

	/**
	 * searches through the hole FileList and copies matching files
	 * into the folders.. 
	 */
	public void processFilelist(FileList fileList, boolean onDownload) {
		synchronized(entrys) {
			for (FileListFile file :fileList.getRoot()) {
				for (ADLSearchEntry entry:entrys) {
					if (entry.matches(file)) {
						FileListFolder folder = fileList.getRoot().getChildPerName(entry.getTargetFolder());
						if (folder == null) {
							folder = new FileListFolder(fileList,fileList.getRoot(),entry.getTargetFolder());
						}
						new ADLFileListFile(folder,file.getName(),file.getSize(),file.getTTHRoot(),file);
						if (entry.isDownloadMatches() && onDownload) {
							file.download();
						}
					}
				}
			}
			for (ADLSearchEntry entry:entrys) {
				entry.finishedSearch();
			}
		}
	}

}
