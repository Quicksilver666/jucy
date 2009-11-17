package eu.jucy.hublist;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;

import uc.DCClient;
import uc.FavHub;

public class HublistHub implements Iterable<String> {
	
	private final Map<Column,String> attribs = new HashMap<Column,String>();
	
	public HublistHub() {}
	
	public void setAttribute(Column c, String value) {
		attribs.put(c, value);
	}
	
	public String getAttribute(Column c) {
		String s = attribs.get(c);
		return s == null? "":s ;
	}
	
	public String getPresentableForColumn(Column c) {
		String data =  getAttribute(c);
		String presentation = c.getType().getPresentation(data);
		return presentation;
	}
	
	public void connect() {
		FavHub fh = getFavHub();
		DCClient.get().getHub(fh,true);
	}
	

	
	public void addToFavorites() {
		getFavHub().addToFavHubs(ApplicationWorkbenchWindowAdvisor.get().getFavHubs());		
		
	}
	
	private FavHub getFavHub() {

		FavHub fh = new FavHub(getAttribute(Column.ADDRESS));
		String val = null;
		if (null != (val= getAttribute(Column.HUBNAME))) {
			fh.setHubname(val);
		}
		
		if (null != (val = getAttribute(Column.DESCRIPTION))) {
			fh.setDescription(val);
		}
		
		return fh;
	}
	
	public String toString() {
		String s="";
		for (String val:attribs.values()) {
			s+= val + ";";
		}
		return s;
	}


	public Iterator<String> iterator() {
		return attribs.values().iterator();
	}
	
	
	
}
