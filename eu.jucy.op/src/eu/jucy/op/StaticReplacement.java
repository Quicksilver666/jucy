package eu.jucy.op;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.jucy.op.ui.ReplacementsEditor.RepSerializer;

import uc.FavHub;
import uihelpers.ComplexListEditor;

public class StaticReplacement {

	



	private String name = "";
	private String replacement = "";
	
	public StaticReplacement() {
	}
	
	public StaticReplacement(String name, String replacement) {
		super();
		this.name = name;
		this.replacement = replacement;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getReplacement() {
		return replacement;
	}

	public void setReplacement(String replacement) {
		this.replacement = replacement;
	}
	
	/**
	 * loads all replacements that exist in a Map
	 * @return all replacements 
	 */
	public static Map<String,String> loadReplacements() {
		List<StaticReplacement> replacements = 
			ComplexListEditor.parseString(OPI.get(OPI.staticReplacements), new RepSerializer());
		Map<String,String> staticReplacement = new HashMap<String,String>();
		for (StaticReplacement sr:replacements) {
			staticReplacement.put(sr.getName(), sr.getReplacement());
		}
		return staticReplacement;
	}
	
	public static List<StaticReplacement> loadReplacement(FavHub fh) {
		return ComplexListEditor.parseString(fh.get(OPI.fh_replacements), new RepSerializer());
	}
	public static Map<String,String> loadReplacements(FavHub fh) {
		Map<String,String> staticReplacement = new HashMap<String,String>();
		for (StaticReplacement sr : loadReplacement(fh)) {
			staticReplacement.put(sr.getName(), sr.getReplacement());
		}
		return staticReplacement;
	}
	
	public static void storeReplacements(FavHub fh, List<StaticReplacement> replacements) {
		String s = ComplexListEditor.createList(replacements, new RepSerializer());
		fh.put(OPI.fh_replacements, s);
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
		StaticReplacement other = (StaticReplacement) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
	
}
