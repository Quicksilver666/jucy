package xmlhandling;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class Plugin implements Comparable<Plugin> {

	public static final String qNamePlugin= "Plugin";
				
	public static final String DISABLED = "DISABLED";
	private String id;
	private String propertiesname;
	private String langFileName;
	
	private final SortedSet<Entry> entries = new TreeSet<Entry>();
	
	public Plugin(String id,String propertiesname,String langFileName) {
		this.id = id;
		this.propertiesname = propertiesname;
		this.langFileName = langFileName;
	}
	
	public void writeLangFile(File basepath) throws IOException {
		if (!DISABLED.equals(langFileName)) {
			File pluginPath = new File(basepath,id);
			List<String> s = new ArrayList<String>();
			for (Entry e:entries) {
				s.add(e.name);
			}
			CreateLangFiles.createLangFile(pluginPath, langFileName, s, propertiesname);
		}
	}
	
	public void addEntry(Entry e) {
		entries.add(e);
	}
	
	public void writeProperties(File basepath) throws Exception {
		File pluginPath = new File(basepath,id);
		
		Map<String,Properties> langToProp = new TreeMap<String,Properties>();
		for (Entry e:entries) {
			e.add(langToProp);
		}
		for (Map.Entry<String , Properties> e:langToProp.entrySet()) {
			String langAppend = e.getKey().equals("en") ? "" : "_"+e.getKey();
			File prop = new File(pluginPath,"nl/"+propertiesname+langAppend+".properties");
			if (!prop.getParentFile().isDirectory()) {
				prop.getParentFile().mkdirs();
			}
			if (!prop.isFile() || !e.getValue().equals(readProp(prop))) {
				FileOutputStream fos = new FileOutputStream(prop); 
				fos.getChannel().truncate(0); //delete file first.. if exists..
				e.getValue().store(fos, null);
				fos.close();
				System.out.println("Written file: "+prop.getCanonicalPath());
			} else {
			//	System.out.println("Unchanged file: "+prop.getAbsolutePath());
			}
			
			
		}
	}
	
	private static Properties readProp(File f) throws IOException {
		Properties p = new Properties();
		FileInputStream fis = new FileInputStream(f);
		p.load(fis);
		fis.close();
		return p;
	}
	
	public void readProperties(File basepath) throws Exception {
		File pluginPath = new File(basepath,id);
		
		File[] files = pluginPath.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith(propertiesname) && name.endsWith(".properties");
			}
		});
		Map<String,Properties> langToProp = new TreeMap<String,Properties>();
		for (File f: files) {
			String fname = f.getName();
			int start = fname.indexOf('_');
			int end = fname.indexOf('.');
			String lang = "en";
			if (start > 0) {
				lang = fname.substring(start+1, end);
			}
			Properties p = readProp(f);
			langToProp.put(lang, p);
		}
		Properties en = langToProp.get("en");
		for (Object o : en.keySet()) {
			String key = o.toString();
			Entry e = new Entry(key);
			for (Map.Entry<String,Properties> props:langToProp.entrySet()) {
				String trans = props.getValue().getProperty(key);
				if (trans != null) {
					Transl t= new Transl(props.getKey(),trans);
					e.addTransl(t);
				}
			}
			entries.add(e);
		}
	}
	
	
	
	private static Properties get(Map<String,Properties> langToProp, String lang) {
		Properties p = langToProp.get(lang);
		if (p == null) {
			p = new Properties();
			langToProp.put(lang, p);
		}
		return p;
	}
	
	
	public void writeToXML(TransformerHandler hd,AttributesImpl atts) throws SAXException {
		atts.clear();

		atts.addAttribute("", "", "id", "CDATA", id);
		atts.addAttribute("", "", "name", "CDATA", propertiesname);
		
		hd.startElement("", "", qNamePlugin, atts);
		
		for (Entry e:entries) {
			e.writeToXML(hd, atts);
		}
		
		hd.endElement("", "",qNamePlugin);
		
	}
	
	public void writeToXML2(TransformerHandler hd,AttributesImpl atts) throws SAXException {
		atts.clear();

		atts.addAttribute("", "", "id", "CDATA", id);
		atts.addAttribute("", "", "name", "CDATA", propertiesname);
		
		hd.startElement("", "", qNamePlugin, atts);
		
		for (Entry e:entries) {
			e.writeToXML2(hd, atts);
		}
		
		hd.endElement("", "",qNamePlugin);
		
	}
	
	
	@Override
	public int compareTo(Plugin o) {
		return id.compareTo(o.id);
	}



	public static class Entry implements Comparable<Entry> {
		private final String name;
		private final List<Transl> translations = new ArrayList<Transl>();
	
		public Entry(String name) {
			this.name = name;
		}

		public void addTransl(Transl t) {
			translations.add(t);
		}
		
		public void add(Map<String,Properties> langToProp) {
			for (Transl t:translations) {
				Properties p = get(langToProp,t.langID);
				p.put(name, t.translation);
			}
		}

		public void writeToXML(TransformerHandler hd,AttributesImpl atts) throws SAXException {
			atts.clear();
			hd.startElement("", "", name, atts);
			for (Transl t:translations) {
				t.writeToXML(hd, atts);
			}
			
			hd.endElement("", "", name);
		}
		
		public void writeToXML2(TransformerHandler hd,AttributesImpl atts) throws SAXException {
			atts.clear();
			atts.addAttribute("", "", "id", "CDATA", name);
			hd.startElement("", "", "Trl", atts);
			for (Transl t:translations) {
				t.writeToXML(hd, atts);
			}
			
			hd.endElement("", "", "Trl");
		}

		@Override
		public int compareTo(Entry o) {
			return name.compareTo(o.name);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
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
			Entry other = (Entry) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}
		
		
	}
	
	public static class Transl {
		private final String langID;
		private String translation;
		
		public Transl(String langID) {
			this(langID,"");
		}
		public Transl(String langID,String translation) {
			this.langID = langID;
			this.translation = translation;
		}
		
		public void addString(String s) {
			translation+=s;
		}
		
		public void writeToXML(TransformerHandler hd,AttributesImpl atts) throws SAXException {
			atts.clear();
			hd.startElement("", "", langID, atts);
			hd.characters(translation.toCharArray(), 0, translation.length());
			hd.endElement("", "", langID);
		}
		
		
	}
}
