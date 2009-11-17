package uc.files.filelist;



import logger.LoggerFactory;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import uc.crypto.HashValue;

public class FileListParser extends DefaultHandler {

	private static Logger logger = LoggerFactory.make();


	private FileListFolder current;
	private FileList fileList;

	public FileListParser(FileList fileList){
		this.fileList = fileList;
		current = fileList.getRoot();
	}
	
	
	public void startElement(String uri,String localName, String qName,Attributes attributes) {
		
		if ("File".equals(qName)) {
			if (current!= null) {
				
			String hashValue = attributes.getValue("TTH" );
			String name = attributes.getValue("Name");
			String size = attributes.getValue("Size");
			
			if (hashValue != null && name != null && size != null) {
				try {
					HashValue h =  HashValue.createHash(hashValue.toUpperCase());
					new FileListFile(current,
							name, 
							Long.valueOf(size),
							h);
				} catch(RuntimeException re) {
					logger.debug(re);
				}
			}
				
			}
		} else if("Directory".equals(qName)) {
			if (current != null) {
				current = new FileListFolder(fileList,current,attributes.getValue("Name"));	
			}
		} else if("FileListing".equals(qName)) {
			fileList.setCID(attributes.getValue("CID"));
			fileList.setGenerator(attributes.getValue("Generator"));
		}
	}
	
	public void endElement(String uri, String localName, String qName){
		if ("Directory".equals(qName) && current != null) {
			current = current.getParent();
		}
	}
	
	public void endDocument (){
		fileList.setCompleted(true);
	}
	
}
