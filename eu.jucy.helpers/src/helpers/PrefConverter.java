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
    	String finished = "";
    	for (String s:ar) {
    		finished += GH.replaces(s) +"\n";
    	}
    	return finished;
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
    
	
	
}
