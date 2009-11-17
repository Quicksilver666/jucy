package uc.files.filelist;


import helpers.FilterLowerBytes;
import helpers.GH;

import java.io.FileInputStream;
import java.io.InputStream;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;


import logger.LoggerFactory;

import org.apache.log4j.Logger;
import org.apache.tools.bzip2.CBZip2InputStream;
import org.apache.tools.bzip2.CBZip2OutputStream;


import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import uc.DCClient;
import uc.PI;
import uc.User;
import uc.crypto.HashValue;
import uc.crypto.TigerHashValue;



public class FileList implements Iterable<IFileListItem> {

	private static final Logger logger= LoggerFactory.make();
		
	protected String cid 		= "NONE";
	protected String generator	= "NONE";
	
	
	protected final User usr; //the owner
	
	protected volatile boolean completed = false;

	private volatile long sharedSize;
	protected final FileListFolder root = new FileListFolder(this,null, "");

	private final Map<HashValue,FileListFile> contents = new HashMap<HashValue,FileListFile>();

	/**
	 * holds a Link to full FileLists in upload preventing 
	 * too much ram need if FileList is uploaded multiple times simultaneously..
	 */
	private WeakReference<byte[]> fullFileListinUpload = new WeakReference<byte[]>(null);
	
	/**
	 * creates an empty FileList for the specified user..
	 * @param usr - the user that owns the FileList
	 */
	public FileList(User usr){
		this.usr = usr;
	}

	public boolean isCompleted(){
		return completed;
	}

	/**
	 * @return the root
	 */
	public FileListFolder getRoot() {
		return root;
	}
	
	
	
	/**
	 * provides an iterator over all FileList Items
	 * will first iterate over all Files then iterate over all
	 * Folders
	 */
	public Iterator<IFileListItem> iterator() {
		
		return new Iterator<IFileListItem>() {
			private final Iterator<FileListFile> itFile = root.iterator();
			private final Iterator<FileListFolder> itFolder = root.iterator2();
			private Iterator<? extends IFileListItem> current= itFile;
			
			public boolean hasNext() {
				if (!current.hasNext()) {
					current = itFolder;
				}
				return current.hasNext();
			}

			public IFileListItem next() {
				return current.next();
			}

			public void remove() {
				current.remove();
			}
			
		};
	}

	/**
	 * @return the numberOfFiles
	 */
	public int getNumberOfFiles() {
		return root.getContainedFiles();
	}
	
	void calcSharesizeAndBuildTTHMap() {
		contents.clear();
		long totalSize = 0;
		for (FileListFile f:root) {
			if (contents.put(f.getTTHRoot(),f) == null) {
				totalSize+= f.getSize();
			}
		}
		sharedSize = totalSize; 
		
	}

	/**
	 * @return the shared size
	 */
	public long getSharesize() {
		return  sharedSize; //root.getContainedSize();
	}
	
	/**
	 * reads in a filelist..
	 * @param path - the path where the filelist resides on the disc..
	 * @return true if successful .. false implies problem reading..
	 */
	public boolean readFilelist(File path){
		InputStream in = null;
		try {
			in = new FileInputStream(path);
			if (!path.toString().endsWith(".xml")) {
				in = new PushbackInputStream(in);
				int b = in.read();
				if (b == 'B') {
					in.read(); // //Z
					in = new CBZip2InputStream(in);
				} else {
					((PushbackInputStream)in).unread(b);
				}
			}
			in = new FilterLowerBytes(in); //needed to capture bad FileLists..
			
			readFilelist(in);
			
		} catch(Exception e){
			logger.debug(e,e);
			return false;
		} finally {
			GH.close(in);
		}
		return true;
	}
	

	public void readFilelist(InputStream  in)throws  IOException ,IllegalArgumentException {
		InputStreamReader isr = null;
		try {

			setCompleted(false);
			//little workaround.. ignore bad input if Stream is directly used for parsing inserted it wouldn't
			isr = new InputStreamReader(in,"utf-8"); 

			SAXParserFactory saxFactory = SAXParserFactory.newInstance();
			
			
	/*		SchemaFactory schFactory =  SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		
			schFactory.setFeature("http://apache.org/xml/features/validation/schema-full-checking", false);
			Bundle bundle = Platform.getBundle(Application.PLUGIN_ID);
			Path path = new Path("XMLSchema/FilelistSchema.xml"); 

			URL url = FileLocator.find(bundle, path, Collections.EMPTY_MAP);

			Schema schema = schFactory.newSchema(url);
			saxFactory.setSchema(schema); */

			SAXParser saxParser = saxFactory.newSAXParser();



			saxParser.parse(new InputSource(isr), new FileListParser(this));

		} catch(ParserConfigurationException pce){
			logger.error(pce,pce);
		} catch(SAXException saxe){
			logger.error(saxe,saxe);
		} 
		calcSharesizeAndBuildTTHMap();
	}
	
	/**
	 * 
	 * @param out
	 * @param path - "/" usually  otherwise the path of the base from where Serialization should start..
	 * @param recursive
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 * @throws IllegalArgumentException - if the path does no exist
	 */
	private void writeFilelist(OutputStream out,String path,boolean recursive) throws UnsupportedEncodingException , IOException , IllegalArgumentException {
		try {
			StreamResult streamResult = new StreamResult(out);
			SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory
				.newInstance();
			// SAX2.0 ContentHandler.
			TransformerHandler hd = tf.newTransformerHandler();
			Transformer serializer = hd.getTransformer();
			serializer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
			//serializer.setOutputProperty(OutputKeys.INDENT, "yes");
			serializer.setOutputProperty(OutputKeys.VERSION, "1.0");
			serializer.setOutputProperty(OutputKeys.STANDALONE, "yes");
			hd.setResult(streamResult);
			hd.startDocument();
			AttributesImpl atts = new AttributesImpl();
			// USERS tag.
			atts.addAttribute("", "", "Version", "CDATA", "1");
			atts.addAttribute("", "", "CID", "CDATA", cid);
			atts.addAttribute("", "", "Base", "CDATA", path);
			atts.addAttribute("", "", "Generator", "CDATA", uc.DCClient.LONGVERSION);
		
			hd.startElement("", "", "FileListing", atts);
			
			
			FileListFolder parentFolder = root.getByPath(path.replace('/', File.separatorChar));
			
			if (parentFolder != null) {
				parentFolder.writeToXML(hd, atts,recursive,true);	
			} else {
				logger.debug("Path requested. Though not found: "+path);
			}

			hd.endElement("", "", "FileListing");
			hd.endDocument();
		
			out.flush();
		} catch(SAXException sax) {
			throw new missing16api.IOException(sax);
		} catch (TransformerConfigurationException pce) {
			throw new missing16api.IOException(pce);
		}
	}
	
	
	
	/*
	 * writes the FileList to the provided stream..
	 * 
	 * @param out - target where the FileList should be written to
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
//	private void writeFilelist(OutputStream out) throws UnsupportedEncodingException , IOException {
	//	writeFilelist(out, "/", true);
		/*
		try {
			StreamResult streamResult = new StreamResult(out);
			SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory
				.newInstance();
			// SAX2.0 ContentHandler.
			TransformerHandler hd = tf.newTransformerHandler();
			Transformer serializer = hd.getTransformer();
			serializer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
			//serializer.setOutputProperty(OutputKeys.INDENT, "yes");
			serializer.setOutputProperty(OutputKeys.VERSION, "1.0");
			serializer.setOutputProperty(OutputKeys.STANDALONE, "yes");
			hd.setResult(streamResult);
			hd.startDocument();
			AttributesImpl atts = new AttributesImpl();
			// USERS tag.
			atts.addAttribute("", "", "Version", "CDATA", "1");
			atts.addAttribute("", "", "CID", "CDATA", cid);
			atts.addAttribute("", "", "Base", "CDATA", "/");
			atts.addAttribute("", "", "Generator", "CDATA", uc.DCClient.VERSION);
		
			hd.startElement("", "", "FileListing", atts);
		//	root.writeToXML(hd, atts);
			for (FileListFolder f:root.getSubfolders().values()) {
				f.writeToXML(hd, atts,true,true);
			}

			hd.endElement("", "", "FileListing");
			hd.endDocument();
		
			out.flush();
		} catch(SAXException sax) {
			throw new IOException(sax);
		} catch (TransformerConfigurationException pce) {
			throw new IOException(pce);
		} */
	//}
	
	/**
	 * writes filelist to a byte[] array
	 * @return
	 */
	public byte[] writeFileList(String path, boolean recursive)  {
		if (path == null) {
			path = "/";
		}
		if (path.equals("/")) { //try load filelist from reference... if filelist is currently in upload..
			synchronized (this) {
				byte[] present = fullFileListinUpload.get();
				if (present != null) {
					return present;
				}
			}
		}
		ByteArrayOutputStream baos = null;
		OutputStream zipstream = null;
		try {
			baos = new  ByteArrayOutputStream();
			baos.write('B');
			baos.write('Z');
			zipstream 	= new CBZip2OutputStream(baos);
			writeFilelist(zipstream,path,recursive);
			zipstream.flush();
		} catch(IOException ioe) {
			logger.warn(ioe,ioe); //this should never happen... everything is done in memory..
		} finally {
			GH.close(zipstream);
		}
		byte[] fileList = baos.toByteArray();
		
		
		if (path.equals("/")) { //store fileList in ref ..
			synchronized (this) {
				fullFileListinUpload = new WeakReference<byte[]>(fileList);
			}
		}
		
		return fileList;
	}
	/**
	 * 
	 * @param file - the target where the filelist should be written to..
	 * @throws IOException - if some error occurs..
	 * 
	 */
	public void writeFilelist(File file) throws IOException {
		FileOutputStream fos = null;
		try {
			fos	= new FileOutputStream(file);
			fos.write(writeFileList("/", true));
			fos.flush();
		} finally {
			GH.close(fos);
		}
	}
	
	/**
	 * deletes all FileLists in the FileList directory
	 */
	public static void deleteFilelists() {
		File f = PI.getFileListPath();
		if (f.isDirectory()) {
			for (File filelist:f.listFiles()) {
				if (filelist.isFile() && filelist.getName().endsWith(".xml.bz2")) {
					if (!filelist.delete()) {
						filelist.deleteOnExit();
					}
				}
			}
		}
	}
	
	/**
	 * adds the owner of this FileList to all files
	 * that are in DownloadQueue as well as in this FileList..
	 */
	public void match() {
		DCClient.get().getDownloadQueue().match(this);
	}
	
	/**
	 * searches for a file by its hashvalue
	 * 
	 * @param value - TTH of the searched file
	 * @return null if not found otherwise the found file
	 */
	public FileListFile search(HashValue value) {
		return contents.get(value);
	}
	
	/**
	 * 
	 * @param onSearch - a regexp used to search in names of files and folders
	 * @return the results containing of a list with folders and files
	 */
	public List<IFileListItem> search(Pattern onSearch) {
		List<IFileListItem> results = new ArrayList<IFileListItem>();
		root.search(onSearch, results);
		return results;
	}
	
	/**
	 * 
	 * @param onSearch - a substring to match
	 * @return all files and folders which names match the provided substring
	 */
	public List<IFileListItem> search(String onSearch) {
		List<IFileListItem> found = search(Pattern.compile(Pattern.quote(onSearch)));
		if (onSearch.matches(TigerHashValue.TTHREGEX)) {
			FileListFile f = search(HashValue.createHash(onSearch));
			if (f != null) {
				found.add(f);
			}
		}
		return found; 
	}
	
	

	/**
	 * @return the usr
	 */
	public User getUsr() {
		return usr;
	}

	/**
	 * @return the cID
	 */
	public String getCID() {
		return cid;
	}

	/**
	 * @param cid the cID to set
	 */
	public void setCID(String cid) {
		this.cid = cid;
	}

	/**
	 * @return the generator
	 */
	public String getGenerator() {
		return generator;
	}

	/**
	 * @param generator the generator to set
	 */
	public void setGenerator(String generator) {
		this.generator = generator;
	}

	/**
	 * @param completed the completed to set
	 */
	public void setCompleted(boolean completed) {
		this.completed = completed;
	}
	
	public boolean deepEquals(FileList f) {
		return usr.equals(f.usr) && root.deepEquals(f.root);
	}
	
	
	

}