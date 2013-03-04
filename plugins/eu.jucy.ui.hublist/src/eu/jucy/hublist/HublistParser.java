package eu.jucy.hublist;

import helpers.GH;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;



import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * parser for XML hublists..
 * 
 * @author Quicksilver
 *
 */
public class HublistParser extends DefaultHandler {

	private List<Column> columns = new ArrayList<Column>();
	
	private List<HublistHub> hubs = new ArrayList<HublistHub>();
	
	public HublistParser() {
		
	}
	
	@Override
	public void endDocument() throws SAXException {
		//remove unused columns
		for (Iterator<Column> it = columns.iterator(); it.hasNext() ;) {
			Column c = it.next();
			if (!isUsed(c)) {
				it.remove();
			}
		}
		
	}
	
	private boolean isUsed(Column c) {
		for (HublistHub hub: hubs) {
			if (!GH.isEmpty( hub.getAttribute(c))) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void endElement(String uri, String localName, String name)
			throws SAXException {

	}

	@Override
	public void startElement(String uri, String localName, String name,
			Attributes attributes) throws SAXException {

		if ("Hub".equals(name)) {
			HublistHub h = new HublistHub();
			
			for (Column c: columns) {	
				String attrib = attributes.getValue(c.getName());
				h.setAttribute(c, GH.coalesce(attrib,""));
			}
			
			hubs.add(h);
		
		} else if ("Column".equals(name)) {
			String colName = attributes.getValue("Name");
			String colType = attributes.getValue("Type"); 
			ColumnType colt = ColumnType.forName(colType);
			if (colt != null && colName != null && !GH.isEmpty(colName)) {
				Column c = new Column(colName, colt);
				if (!columns.contains(c)) {
					columns.add(c);
				}
			}
		} 
	}

	
	
	public List<Column> getColumns() {
		return columns;
	}

	public List<HublistHub> getHubs() {
		return hubs;
	}
	
}
