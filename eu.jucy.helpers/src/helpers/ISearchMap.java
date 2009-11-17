package helpers;

import java.util.Set;

public interface ISearchMap<V> {

	void put(V toMap);

	/**
	 * simple search for a single term
	 * 
	 * @param s
	 * @return
	 */
	Set<V> search(String s);

	/**
	 * searches for given strings without filter
	 * @param searchStrings
	 * @return
	 */
	Set<V> search(Set<String> searchStrings);

	/**
	 * searches for given strings and filters the output immediately..
	 * (boolean AND query)   (excludes are NOT AND)
	 * 
	 * @param searchStrings
	 * @param filter
	 * @return
	 */
	Set<V> search(Set<String> searchStrings, Set<String> excludes,
			IFilter<V> filter);

}