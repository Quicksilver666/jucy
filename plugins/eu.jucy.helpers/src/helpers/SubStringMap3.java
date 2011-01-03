package helpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;



/**
 * New mapping tried to implement 
 * a simple Suffix Array.
 * Allows users to make substring searches.
 * i.e.  if a mapping has the key "bananach"
 * search on for example  anac  will result in a hit.  
 * 
 * 
 * 
 * @author Quicksilver
 *
 */
public class SubStringMap3<V> implements ISearchMap<V> {

	private final int minimumlength;
	
	private final ISubstringMapping2<V> mapping;
	
	private boolean sorted = true;
	
	private final ArrayList<Node> all = new ArrayList<Node>();
	
	/**
	 * creates mapping that maps items in the way specified..
	 * by the provided mapping
	 */
	public SubStringMap3(ISubstringMapping2<V> mapping) {
		this.minimumlength = 3;
		this.mapping = mapping;
	}
	
	/**
	 * creates substring map that simply maps items to their toString result
	 * 
	 */
	public SubStringMap3() {
		this(new ISubstringMapping2<V>() {
			public String getMappingString(V item) {
				return item.toString();
			}
		});
	}
	
	/* (non-Javadoc)
	 * @see helpers.ISearchMap#put(V)
	 */
	public synchronized void put(V toMap) {
		if (toMap == null)  {
			throw new IllegalArgumentException("argument toMap is null");
		}
		
		String text = mapping.getMappingString(toMap);
		if (text.length() >= Short.MAX_VALUE ) {
			throw new IllegalArgumentException("too long String");
		}
		sorted = false;
		
		for (short i = 0,max =(short) (text.length() - minimumlength+1); i < max; i++) { 
			all.add(new Node(i,toMap));
		}
	}
	
	/* (non-Javadoc)
	 * @see helpers.ISearchMap#search(java.lang.String)
	 */
	public Set<V> search(String s) {
		return search(Collections.singleton(s));
		//return search(Collections.singleton(s),Collections.<String>emptySet(),null);
	}
	
	private void prepareForSearch() {
		Collections.sort(all);
		List<Node> newList = new ArrayList<Node>();
		List<Node> currentList = new ArrayList<Node>();
		
		String current = "";

		for (Node n : all) {
			String nString = n.toString();
			if (current.equals(nString)) {
				currentList.add(n);
			} else {
				if (currentList.size() > 1 ) {
					newList.add(createFatNode(currentList));
				} else if (!currentList.isEmpty()) {
					newList.add(currentList.get(0));
				}
				currentList.clear();
				currentList.add(n);
				current = nString;
			}
		}
		all.clear();
		all.addAll(newList);
		all.trimToSize();
		
	}
	
	/* (non-Javadoc)
	 * @see helpers.ISearchMap#search(java.util.Set)
	 */
	public Set<V> search(Set<String> searchStrings) {
		return search(searchStrings,Collections.<String>emptySet(),new IFilter<V>() { //empty filter..
			public boolean filter(V item) {
				return true;
			}
			
			public Set<V> mapItems(Set<V> nodeItems) {
				return nodeItems;
			}});
	}
	
	/* (non-Javadoc)
	 * @see helpers.ISearchMap#search(java.util.Set, java.util.Set, helpers.IFilter)
	 */
	public Set<V> search(Set<String> searchStrings,Set<String> excludes,IFilter<V> filter) {
		synchronized(this) {
			if (!sorted) {
				prepareForSearch();
				sorted = true;
			}
		}
		List<String> searches = new ArrayList<String>();
		//normalise the strings and remove doublets
		for (String s:searchStrings) {
			String normS = normalize(s).trim();
			if (!GH.isEmpty(normS) && !searches.contains(normS)) {
				searches.add(normS);
			}
		}
		
		Set<V> current = null;
		//do the searches..
		for (String s:searches) {
			Set<V> found = getMatching(s);
			found = filter.mapItems(found);
			if (current != null) {
				found.retainAll(current);
			} else {
				//remove filtered items
				for (Iterator<V> it = found.iterator(); it.hasNext();) {
					if (!filter.filter(it.next())) {
						it.remove();
					}
				}
				
			}
			current = found;
			if (current.isEmpty()) {
				break;
			}
		}

		if (current == null) {
			current = Collections.<V>emptySet();
		}
		//remove all excludes ...
		for (String exclude : excludes) {
			Set<V> found = getMatching(exclude);
			found = filter.mapItems(found);
			current.removeAll(found);
		}
		
		return current;
	}
	
	/**
	 * normalises a string so it can be used for searching
	 * @param unnormalized
	 * @return
	 */
	public static String normalize(String unnormalized) {
		return unnormalized.toLowerCase();
	}
	
	private String getMapping(V v) {
		return normalize(mapping.getMappingString(v));
	}
	
	/**
	 * 
	 * @param s
	 * @return
	 */
	private Set<V> getMatching(String s) {
		int found =  binarySearch(all,s);
		if (found < 0) {
			return Collections.<V>emptySet();
		} else {
			Set<V> matching = new HashSet<V>();
			all.get(found).addAll(matching);
			for (int up = found+1; up < all.size() ; up++) {
				if (all.get(up).matches(s)) {
					all.get(up).addAll(matching);
				} else {
					break;
				}
			}
			for (int down = found-1; down >= 0 ; down--) {
				if (all.get(down).matches(s)) {
					all.get(down).addAll(matching);
				} else {
					break;
				}
			}
			return matching;
		}
	}

	/**
	 * 
	 * copied from java Arrays.. does a binary Search on a List
	 */
	private int binarySearch(List<Node> a, String key) {
		return binarySearch0(a, 0, a.size(), key);
	}

	//Like public version, but without range checks.
	private int binarySearch0(List<Node> a, int fromIndex, int toIndex,
			String key) {
		int low = fromIndex;
		int high = toIndex - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
		Node midVal = (Node)a.get(mid); // a[mid];
		int cmp = midVal.compareTo(key);

		if (cmp < 0)
			low = mid + 1;
		else if (cmp > 0)
			high = mid - 1;
		else
			return mid; // key found
		}
		return -(low + 1);  // key not found.
	}
	
	
	private class Node implements Comparable<Node> {
		
		private final short beginIndex;
		private final V mapped;
		
		public Node(short beginIndex, V mapped) {
			this.beginIndex = beginIndex;
			this.mapped = mapped;
		}


		public int compareTo(Node o) {
			String own = toString();
			String other = o.toString();
			return own.compareTo(other);
		}
	
		
		public int compareTo(String o) {
			String own = toString();
			if (own.length() > o.length()) {
				own = own.substring(0, o.length());
			}
			
			return own.compareTo(o);
		}

		public boolean matches(String s) {
			return toString().startsWith(s);
		}

		public void addAll(Set<V> where) {
			where.add(mapped);
		}
		
		public String toString() {
			return getMapping(mapped).substring(beginIndex);
		}
	}
	
	public FatNode createFatNode(List<Node> nodes) {
		if (nodes.size() < 1) {
			throw new IllegalArgumentException("bad number of nodes");
		}
		Node first= nodes.get(0);
		Set<V> items = new HashSet<V>();
		for (Node n: nodes) {
			n.addAll(items);
		}
		items.remove(first.mapped);
		
		return new FatNode(first.beginIndex,first.mapped, items);
	}
	
	private class FatNode extends Node {

		private final Object[] moreItems;

		public FatNode(short beginIndex, V mapped,Collection<V> other) {
			super(beginIndex, mapped);
			moreItems = other.toArray();
		}

		@SuppressWarnings("unchecked")
		@Override
		public void addAll(Set<V> where) {
			super.addAll(where);
			for (Object o:moreItems) {
				where.add((V)o);
			}
		}
		
		
		
	}
	
	
	
}
