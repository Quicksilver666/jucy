package helpers;


import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * 
 * an implementation of the ukkonen algorithm
 * - for searching in a filelist
 * - also for indexing hublists..
 * 
 * @author Quicksilver  
 *
 */
public class SubStringMap<V>  {
	
	public static long edgecounter = 0; 

	private static final char TERMINATOR =(char)3;
	/**
	 * reverse sorts of the strings
	 */
	private static final Comparator<String> stringLengthComp = 
		new Comparator<String>(){
			public int compare(String a, String b){
				int i= b.length()- a.length();
				if ( i != 0) {
					return i;
				} else {
					return a.compareTo(b);
				}
			}
	};
	

	
	private final int minimumSubStringlenght ; 
	/**
	 * if this is set all strings will be transformed to lowercase
	 */
	private final boolean casesensitive;
	/**
	 * root node to all Edges..
	 */
	private Map<Character,Edge<V>> root= new HashMap<Character,Edge<V>>();
	
	public SubStringMap() {
		this(false);
	}
	
	public SubStringMap(boolean casesensitive){
		this(casesensitive,3);
	}
	
	public SubStringMap(boolean casesensitive, int minimumSubStringlenght){
		this.casesensitive=casesensitive;
		if (minimumSubStringlenght < 0) {
			throw new IllegalArgumentException("Argument must be an positive integer");
		}
		this.minimumSubStringlenght=minimumSubStringlenght;
	}
	
	
	public synchronized void put(String text, V toMap){
		if (toMap == null)  {
			throw new IllegalArgumentException("argument toMap is null");
		}
		if (text.length() >= Short.MAX_VALUE ) {
			throw new IllegalArgumentException("too long String");
		}
		
		if (text.contains(""+TERMINATOR)) {
			text = text.replace(TERMINATOR, ' ');
		}
		
		if (!casesensitive) {
			text = text.toLowerCase();
		}
		text += TERMINATOR;

		for (int i = 0,max = text.length() - minimumSubStringlenght; i < max; i++) { 
			Edge<V> newedge = new Edge<V>(text,i,text.length(),toMap);
			Edge<V> oldedge = root.get(newedge.getChar(0));
			if (oldedge == null) {
				root.put(newedge.getChar(0),newedge );
			} else {
				oldedge.merge(newedge);
			}
		}
	}
	
	/**
	 * convenience method to search for a single string
	 * @param search- the substring for which to search in the map
	 * @return all the results..
	 */
	public synchronized Set<V> search(String search) {
		if (!casesensitive) {
			search = search.toLowerCase();
		}
		
		Edge<V> edge= root.get(search.charAt(0));
		if (edge != null) {
			return edge.search(search , null);	
		} else {
			return Collections.<V>emptySet();
		}
	}

	/**
	 * method for searching in the substring map
	 * 
	 * @param searchstrings a set of search strings
	 * @return a set of result values that match all search strings
	 */
	public synchronized Set<V> search(Set<String> searchstrings) {
		searchstrings.remove(""); //no empty strings allowed.
		SortedSet<String> sorter = new TreeSet<String>(stringLengthComp);
		Set<V> result = null;
		sorter.addAll(searchstrings);
		for (String search: sorter) {
			if (!casesensitive) {
				search = search.toLowerCase();
			}
				
			Edge<V> edge= root.get(search.charAt(0));
			if (edge != null) {
				result = edge.search(search , result);
			} 
			
			if (result == null ||result.size() == 0) {
				break;
			}
		}
		if (result == null) {
			result =  Collections.<V>emptySet();
		}

		return result;
	}
	
	
	@SuppressWarnings("unchecked")
	private static class Edge<V> implements Comparable<Edge<V>>{
		
		private static final Comparator<Object> identity = new Comparator<Object>() {
			public int compare(Object o1, Object o2) {
				return Integer.valueOf(System.identityHashCode(o1)).compareTo(System.identityHashCode(o2)); 
			}
			
		};
		
		/**
		 * the string that is used for the edge description
		 */
		private final String edgestring;
		
		/**
		 * the start in the strign above
		 */
		private short start;
		
		/**
		 * the end in the string above
		 * end can change when 1 edge is divided into two 
		 */
		private short end;
		
		/**
		 * a map to the child nodes
		 */
		//private HashMap<Character,Edge<V>> children = new HashMap<Character,Edge<V>>(1,2);
		private Edge<V>[] children = new Edge[0];
		
		
		
		/**
		 * the values on this edge..
		 */
		//private HashSet<V> values = new HashSet<V>(1,2);
		private Object[] values = new Object[0];
		
		/**
		 * 
		 * @param edgestring - the string used to describe the edge
		 * @param start  - start in the string
		 * @param end - end in the string
		 */
		private Edge(String edgestring, int start, int end) {
			this(edgestring,start,end,null);
		}
		
		private Edge(String edgestring, int start, int end, V value){
			this.edgestring = edgestring;
			this.start = (short)start;
			this.end = (short)end;
			edgecounter++;
			if (value != null) {
				addValue(value);
			}
		}
		
		private char getChar(int pos){
			return edgestring.charAt(start + pos);
		}
		
		private char getFirstChar() {
			return edgestring.charAt(start);
		}
		
		private void merge(Edge<V> toMerge ) {
		
			for (int i=0 ; start + i < end   ; i++ ) {
				if (getChar(i) != toMerge.getChar(i)) {
					//--  start dividing at i
					
					//set create a new edge that goes from i To the End and give him our data
					Edge<V> fromIToEnd= new Edge<V>(edgestring,i+start,end);
					fromIToEnd.children = children;
					fromIToEnd.values = values;
					//delete our data
					children=  new Edge[0];//new HashMap<Character,Edge<V>>(1,2); // new map for the children
					values 	= new Object[0];                   // new set for the values..
					end =(short) (start+i);                         // reduce the end to the current value
					//add to the new edge to our end
					putChildren( fromIToEnd);
					
					//--  end deviding at i
					
					//change start of toMerge accordingly
					toMerge.start += i ;
					//now put into our edge
					putChildren(toMerge);
					
					return; //we are done
				}
			}
			
			if ( end-start ==  toMerge.end- toMerge.start ) {
				//if we are here it means all chars were the same.. 
				//so we are the same edge.. so we just add ourself to this edge
				addValue(toMerge.values);
				
			} else {
				toMerge.start += end-start ;
				Edge<V> other = getChildren(toMerge.getFirstChar());
				if (other == null) {
					putChildren(toMerge);
				} else {
					//merge with the leftover chars..
					other.merge(toMerge);
				}
				
			}
		}
		
		private void putChildren(Edge e) {
			Edge[] newchildren = new Edge[children.length+1];
			System.arraycopy(children,0, newchildren, 0, children.length);
			newchildren[children.length] = e;
			Arrays.sort(newchildren);
			children = newchildren;
		}
		
		private Edge getChildren(char c) {
			for (int i =0;i < children.length; i++) {
				char found = children[i].getFirstChar();
				if (found == c) {
					return children[i];
				} 
			}
			return null;
		}
		
		private void addValue(Object... v) {
			Object[] newvalues = new Object[values.length+v.length];
			System.arraycopy(values,0, newvalues, 0, values.length);
			System.arraycopy(v, 0, newvalues, values.length, v.length);
			Arrays.sort(newvalues,identity);
			values = newvalues;
		}
		
		private boolean containsValue(Object o) {
			return Arrays.binarySearch(values, o,identity) >= 0;
		}
		

		public int compareTo(Edge<V> o) {
			return Character.valueOf(getFirstChar()).compareTo(o.getFirstChar());
		}

		/**
		 * recursively searches the substrings
		 * @param substring the left over search string
		 * @return a set of values
		 */
		private Set<V> search(String substring,Set<V> oldRes){
			if (substring == null) {
				throw new IllegalArgumentException("substring is null");
			}
			for (int i=0; i < substring.length(); i++) {
				if (end == i + start) { //means we are at the end of the edge
					Edge<V> next = getChildren(substring.charAt(i));
					if (next != null){
						//not yetfound but 
						return next.search(substring.substring(i),oldRes); 
					} else {
						//not found .. therefore return empty set
						return Collections.<V>emptySet();
					}
						
				} else { //ok we are still in the middel so check if chars are unequal
					if (substring.charAt(i) != getChar(i)) {
						//chars don't equal so return the empty set
						return Collections.<V>emptySet();
					} //if the equal we simply go on
				}
			}
			//add all and the succeeding..

			return cut(oldRes);
		}
		
		/**
		 * 
		 * @param all - where all values should go to.. 
		 * @return
		 */
		private void allValues(HashSet<V> all) {
			for (Object o: values) {
				all.add((V)o);
			}
			for (Edge<V> subs : children) {
				subs.allValues(all);
			}
		}
		
		/**
		 * 
		 * @param oldres search results from another string
		 * @return a cut of the older results with current results
		 */
		private Set<V> cut(Set<V> oldres){
			if (oldres == null) {
				HashSet<V> results = new HashSet<V>();
				allValues(results);
				return results;
			}
			
			HashSet<V> result = new HashSet<V>();
			for (Iterator<V> it = oldres.iterator(); it.hasNext();) {
				V v = it.next();
				if (containsValue(v)) {
					result.add(v); 
					it.remove(); //no longer search for it
				}
			}
			for (Edge<V> subs : children) {
				result.addAll(subs.cut(oldres));
			}
			return result;
		}
		
	}
	
}
