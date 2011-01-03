
package uc.files.filelist;

/**
 * Used by ADL search and alike plug-ins to be notified on 
 * a downloaded FileList. This is guaranteed to be executed before the list is shown to the user..
 * 
 * @author Quicksilver
 *
 */
public interface IFilelistProcessor {

	public static final String  ExtensionpointID	= "eu.jucy.files.filelist.processor" ;
	
	/**
	 * requests the plugin to process the Filelist and change the FileList
	 * how ever the plug-in sees it fit
	 * 
	 * @param fileList - the filelist that should be processed.
	 * @param onDownload  true if this is called right ater download
	 * false if this is called because the user reopens the FileList
	 */
	void processFilelist(FileList fileList,boolean onDownload);
	
}
