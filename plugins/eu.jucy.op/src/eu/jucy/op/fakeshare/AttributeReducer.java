package eu.jucy.op.fakeshare;

import java.util.List;

import uc.files.filelist.FileList;

public abstract class AttributeReducer {

	public abstract List<FileListAttribute> getAttributes(FileList fl);
	
	
	
	/**
	 * reduces file list by looking at distribution of
	 * file sizes and how well these fit to a zipf distribution..
	 * 
	 * @author Quicksilver
	 *
	 */
	public static class ZipfFileSizeReducer {
		
	}
	
	
	
	public static class FileListAttribute {
		private String name;
		private double value;
	}
	
}
