package eu.jucy.gui.settings;

import java.io.File;


import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;

import eu.jucy.gui.Lang;


import uc.PI;


public class DownloadsPreferencePage extends UCPrefpage {
	

	public DownloadsPreferencePage(){
		super(PI.PLUGIN_ID);
	}
	@Override
	protected void createFieldEditors() {
		
		
		//Directories
		checkExistance(new File(PI.get(PI.downloadDirectory)));
		DirectoryFieldEditor defdownloaddir = new DirectoryFieldEditor(PI.downloadDirectory,
				Lang.DefaultDownloadDirectory,
				getFieldEditorParent());
		addField(defdownloaddir);
		
		checkExistance(new File(PI.get(PI.tempDownloadDirectory)));
		DirectoryFieldEditor tempdownloaddir = new DirectoryFieldEditor(PI.tempDownloadDirectory,
				Lang.UnfinishedDownloadsDirectory,
				getFieldEditorParent());
		addField(tempdownloaddir);
		
		//Limits
		
		IntegerFieldEditor maxsimDownloads = new IntegerFieldEditor(PI.maxSimDownloads,
				Lang.MaximumSimultaneousDownloads,
				getFieldEditorParent());
		maxsimDownloads.setValidRange(0, Integer.MAX_VALUE);
		addField( maxsimDownloads );

		IntegerFieldEditor downlimit = new IntegerFieldEditor(PI.downloadLimit,
				Lang.DownloadLimit,
				getFieldEditorParent());
		downlimit.setValidRange(0, Integer.MAX_VALUE/1024);
		addField( downlimit );	
	}
	
	private static void checkExistance(File dir) {
		if (!dir.isDirectory()) {
			dir.mkdirs();
		}
	}


}
