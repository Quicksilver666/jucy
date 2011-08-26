package helpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * helper functions for handling preferences..
 * 
 * @author Quicksilver
 *
 */
public final class PrefConverter {

	
    
    /**
     * helper methods as its sometimes easier to store it like that
     * will escape the breaking characters..
     * @param property
     * @return
     */
    public static String[] asArray(String property) {
    	List<String> list = new ArrayList<String>();
    	if (!property.endsWith("\n") && !GH.isEmpty(property)) {
    		property+="\n";
    	}
    	
    	int i;
    	while (-1 != (i = property.indexOf('\n'))) {
    		list.add(GH.revReplace(property.substring(0, i))) ;
    		property = property.substring(i+1);
    	}
    		
    	return list.toArray(new String[]{});
    }
    
    public static String asString(String[] ar) {
    	StringBuilder finished = new StringBuilder();
    	for (String s:ar) {
    		finished.append(GH.replaces(s)).append('\n'); 
    	}
    	return finished.toString();
    }
    
    
    public static String asString(Map<String,String> kvMap) {
    	String[] str = new String[kvMap.size() *2];
    	int i =0;
    	for (Entry<String,String> e:kvMap.entrySet()) {
    		str[i] = e.getKey();
    		str[i+1]= e.getValue();
    		i+=2;
    	}
    	return asString(str);
    }
    
    public static Map<String,String> asMap(String property) {
    	String[] str = asArray(property);
    	Map<String,String> map = new HashMap<String,String>();
    	for (int i=0; i < str.length; i+=2) {
    		map.put(str[i], str[i+1]);
    	}
    	return map;
    }

	public static <V> String createList(List<V> items,IPrefSerializer<V> translater) {
		StringBuilder s = new StringBuilder();
		for (V v: items) {
			String item = asString(translater.serialize(v));
			s.append( GH.replaces(item)).append('\n');
		}
		return s.toString();
	}

	/**
	 * Splits the given string into a list of strings.
	 * This method is the converse of <code>createList</code>. 
	 * <p>
	 * Subclasses must implement this method.
	 * </p>
	 *
	 * @param stringList the string
	 * @return an array of <code>String</code>
	 * @see createList
	 */
	public static <V> List<V> parseString(String stringList,IPrefSerializer<V> translater) {
		List<V> list = new ArrayList<V>();
		for (String s :  loadList(stringList)) {
			list.add(translater.unSerialize(asArray(s)));
		}
		return list;
	}

	/**
	 * just loads a List of item strings from the given string..
	 * allows decoding 
	 * 
	 * @param stringList - the preference value that was stored before via complex List editor
	 * @return
	 */
	public static List<String> loadList(String stringList) {
		if (stringList == null) {
			stringList = "";
		}
		if (!stringList.endsWith("\n")&& !GH.isEmpty(stringList)) { //workaround for old ComplexListEditor so data can still be loaded..
			stringList+= "\n";
		}
		
		List<String> list = new ArrayList<String>();
		int i = 0;
		while ((i = stringList.indexOf('\n')) != -1) {
			list.add(GH.revReplace(stringList.substring(0, i)));
			stringList = stringList.substring(i+1);
		}
		return list;
	}
    
	
	
}
