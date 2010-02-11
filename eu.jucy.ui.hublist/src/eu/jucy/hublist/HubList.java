package eu.jucy.hublist;

import helpers.FilterLowerBytes;
import helpers.GH;




import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;


import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;


import org.apache.tools.bzip2.CBZip2InputStream;
import org.xml.sax.SAXException;

import eu.jucy.ui.hublist.HublistPI;


public class HubList {

//	private static Logger logger = LoggerFactory.make();

	private final URL url;
	
	private final List<Column> columns = new ArrayList<Column>();
	private final List<HublistHub> hubs = new ArrayList<HublistHub>();

	private FilterLowerBytes filter;

	private long users;

	public HubList(URL url) {
		this.url = url;
	}
	
	/**
	 * loads the HubList from the provided URL
	 * 
	 * @throws IOException
	 */
	public void load(boolean tryIgnoreErrors) throws IOException , SAXException {
		columns.clear();
		hubs.clear();
		
		InputStream in = null;
		URLConnection urlc = null;
		try {
			urlc = url.openConnection();
			urlc.setConnectTimeout(10000);
			urlc.setReadTimeout(10000);
			in = urlc.getInputStream();
			in = new BufferedInputStream( in );
			if (url.toString().endsWith(".bz2")){
				in.read();//read B
				in.read();//read Z
				try {
					in = new CBZip2InputStream(in);
				} catch (NullPointerException npe) {
					throw new missing16api.IOException("No Hublist found",npe);
				}
				//logger.debug("loading with compression");
			}
			
			if (url.toString().contains(".xml")) {
				if (tryIgnoreErrors) {
					in = (filter = new FilterLowerBytes(in));
				}
				readInXMLHublist(in);
			} else {
				readInHublist(in);
			}
			
		} finally {
			GH.close(in);
		}
	}
	
	
	private void readInXMLHublist(InputStream in) throws IOException, SAXException {
		HublistParser hp = new HublistParser();
		try {
			SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
				
			saxParser.parse(in,hp);
		

		} catch(ParserConfigurationException e) {
			throw new missing16api.IOException("Parser config Exception: "+e,e);
		}
		
		columns.addAll(hp.getColumns());
		hubs.addAll(hp.getHubs());
		if (!hubs.isEmpty() && (!columns.contains(Column.USERS) || !columns.contains(Column.ADDRESS)||!columns.contains(Column.HUBNAME) )) {
			throw new IOException("Missing columns in Hublist");
		}
		calcUserStats();
	}
	
	private void readInHublist(InputStream in) throws IOException {
		OldStyleHublistParser hp = new OldStyleHublistParser(in);
		
		hp.parse();
		columns.addAll(hp.getColumns());
		hubs.addAll(hp.getHubs());
		calcUserStats();
	}

	private void calcUserStats() {
		long count = 0;
		for (HublistHub h:hubs) {
			count += Long.parseLong(h.getAttribute(Column.USERS));
		}
		users = count;
	}

	/**
	 * @return how many hubs are in the HubList
	 */
	public int getNrOfHubs() {
		return hubs.size();
	}
	
	/**
	 * 
	 * @return the number of users total in the hublist..
	 */
	public long getUserCount() {
		return users;
	}
	
	public List<Column> getColumns() {
		return columns;
	}


	public List<HublistHub> getHubs() {
		return hubs;
	}
	
	public int getNumberOfErrors() {
		return filter != null? filter.getFilteredChars():0;
	}

	
	/**
	 * test an URL
	 */
	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			System.out.println("Please provide an URL to a hublist as parameter. Otherwise jucy's default hublists will be used..");
			args = HublistPI.defaultHublists.split(Pattern.quote(";"));
		}
		
		for (String url : args) {
			System.out.println("Checking: "+url);
			try {
				HubList hu = new HubList(new URL(url));
				hu.load(false);
				System.out.println("Success");
				if (args.length == 1) {
					System.exit(0);
				}
			} catch(org.xml.sax.SAXParseException e) {
				System.out.println("Failed: "+ e.toString()+"  line:"+e.getLineNumber()+"  col:"+e.getColumnNumber());
				if (args.length == 1) {
					System.exit(-1);
				}
			} catch (Exception e) {
				System.out.println("Failed: "+ e.toString());
				if (args.length == 1) {
					System.exit(-1);
				}
			}
		}
		
	}
	
}
