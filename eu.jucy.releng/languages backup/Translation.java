package xmlhandling;



import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;


import xmlhandling.Plugin.Entry;
import xmlhandling.Plugin.Transl;



public class Translation {

	public static final String[] PLUGIN_IDs = new String[]{
		"eu.jucy.adlsearch",
		"eu.jucy.countries",
		"eu.jucy.gui",
		"eu.jucy.helpers",
		"eu.jucy.language",
		"eu.jucy.notepad",
		"eu.jucy.product1",
		"eu.jucy.ui.searchspy"};
	public static final String[] MESSAGENAMES = new String[]{"adl","countries","gui","helpers",
		"language","notepad","product","searchspy"};
	
	
	private final SortedSet<Plugin> allPlugins = new TreeSet<Plugin>();

	public Translation() {}
	
	public void addPlugin(Plugin p) {
		allPlugins.add(p);
	}
	
	public void writeProperties(File basepath) throws Exception {
		for (Plugin p:allPlugins) {
			 p.writeProperties(basepath);
		}
	}
	
	public void readProperties(File basepath) throws Exception {
		for (int i = 0; i < PLUGIN_IDs.length; i++) {
			allPlugins.add(new Plugin(PLUGIN_IDs[i],MESSAGENAMES[i]));
		}
		for (Plugin p: allPlugins) {
			p.readProperties(basepath);
		}
	}

	public void writeXML(OutputStream out) throws Exception {
		StreamResult streamResult = new StreamResult(out);
		SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
		// SAX2.0 ContentHandler.
		TransformerHandler hd = tf.newTransformerHandler();
		Transformer serializer = hd.getTransformer();
		serializer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
		serializer.setOutputProperty(OutputKeys.INDENT, "yes");
		serializer.setOutputProperty(OutputKeys.VERSION, "1.0");
		serializer.setOutputProperty(OutputKeys.STANDALONE, "yes");
		hd.setResult(streamResult);
		hd.startDocument();
		AttributesImpl atts = new AttributesImpl();

		hd.startElement("", "", "Translation", atts);

		for (Plugin p: allPlugins) {
			p.writeToXML(hd, atts);
		}
		hd.endElement("", "", "Translation");
		hd.endDocument();
	}
	
	public void readXML(InputStream in) throws Exception {
		SAXParserFactory saxFactory = SAXParserFactory.newInstance();
		SAXParser saxParser = saxFactory.newSAXParser();
		saxParser.parse(new InputSource(in), new TranslationParser());
	}
	
	private class TranslationParser extends DefaultHandler {

		private Plugin current;
		private Entry currentE;
		private Transl currentT;
		
		private final int outside= 0;
		private final int plugin= 1;
		private final int entry= 2;
		private final int transl= 3;
		
		private int state = outside ; //in what state we are.. 0 outside, 1 
		
		
		
		@Override
		public void characters(char[] arg0, int start, int length) throws SAXException {
			String s = new String(arg0,start,length);
			if (state == transl) {
				currentT.addString(s);
			}
		}



		@Override
		public void startElement(String arg0, String arg1, String qName,Attributes attributes) throws SAXException {
			switch(state) {
			case outside:
				if (Plugin.qNamePlugin.equals(qName)) {
					String id = attributes.getValue("id" );
					String name = attributes.getValue("name" );
					current = new Plugin(id, name);
					state = plugin;
				} 
			break;
			case plugin:
				currentE = new Entry(qName);
				state = entry;
				break;
			case entry:
				currentT = new Transl(qName);
				state = transl;
				break;
			case transl:
				throw new IllegalStateException();
			}
			
		}
		@Override
		public void endElement(String arg0, String arg1, String arg2)throws SAXException {
			switch(state) {
			case outside: break;
			case plugin: 
				addPlugin(current);
				current = null;
				state = outside;
				break;
			case entry:
				current.addEntry(currentE);
				state = plugin;
				currentE = null;
				break;
			case transl:
				currentE.addTransl(currentT);
				state = entry;
				currentT = null;
				break;
			}
		}
	}
	
}
