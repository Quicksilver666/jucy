package eu.jucy.hublist;

import helpers.GH;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * parses hublists in config format
 * 
 * ex.:
 * theukhub|theukhub.no-ip.org:1209|Anything goes RCv10.023.px.|17||||||
 * Jamtland�@�TropiCo.Se - Bad Boys on DC !!!!|norrlandshubben.no-ip.org:2222|[[ DC-SPIRIT ]] [[ 5000+ USERSHUB ]]|980||||||
 * 
 * $HubINFO <hub name>$<hub address:port>$<hub description>$<max users>$<min share in bytes>$<min slots>$<max hubs>$<hub type>$<hubowner login>|
 * @author Quicksilver
 *
 */
public class OldStyleHublistParser {
	
	private List<Column> columns = Arrays.asList(Column.HUBNAME, Column.ADDRESS, Column.DESCRIPTION, Column.USERS);
	
	private List<HublistHub> hubs = new ArrayList<HublistHub>();
	
	private final InputStream in;
	
	private static final String text = "([^|]*)",
								sep  = "\\|",
								number = "(\\d+)"; 
	private static final Pattern hubpattern = Pattern.compile(text+sep+text+sep+text+sep+number+".*");
	
	public OldStyleHublistParser(InputStream in) {
		this.in = in;
	}
	
	/**
	 * reads in the complete hublist..
	 * @throws IOException
	 */
	public void parse() throws IOException {
		BufferedReader bufr = null;
		try {
			bufr = new BufferedReader(new InputStreamReader(in, "windows-1252"));
			String read = null;
			while (null != (read = bufr.readLine())) {
				parseHub(read);
			}
			
		} finally {
			GH.close(bufr);
		}
	}
	
	private void parseHub(String hubstr) {
		Matcher m = hubpattern.matcher(hubstr);
		
		if (m.matches()) {
			HublistHub hub = new HublistHub();
			
			hub.setAttribute(Column.HUBNAME, m.group(1));
			hub.setAttribute(Column.ADDRESS, m.group(2));
			hub.setAttribute(Column.DESCRIPTION, m.group(3));
			hub.setAttribute(Column.USERS, m.group(4));
			
			hubs.add(hub);
		}
	}

	public List<Column> getColumns() {
		return columns;
	}

	public List<HublistHub> getHubs() {
		return hubs;
	}
	
	

	
}
