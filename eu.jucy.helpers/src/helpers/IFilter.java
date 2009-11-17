package helpers;

import java.util.Set;

public interface IFilter<Item> {
	
	
	public static final IFilter<Object> emptyFilter = new IFilter<Object>() {
		public boolean filter(Object item) {
			return true;
		}
		
		public Set<Object> mapItems(Set<Object> nodeItems) {
			return nodeItems;
		}
		
	};
	
	/**
	 * 
	 * @param item an item to be filtered additionally
	 * @return true - if this item matches the filter..
	 *  false if it can be dropped..
	 */
	boolean filter(Item item);
	
	
	
	/**
	 * in a directory structure node Items might be
	 * mapped from directories to files.
	 * 
	 * if this is not a 
	 * 
	 * @param nodeItems - all items found 
	 * @return the nodeItems list usually but leaf nodes if nodeItems present a 
	 * TreeStructure
	 */
	Set<Item> mapItems(Set<Item> nodeItems);

}
