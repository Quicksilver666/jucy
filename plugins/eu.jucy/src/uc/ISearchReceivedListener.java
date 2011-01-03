package uc;


import java.util.Set;

public interface ISearchReceivedListener {

	/**
	 * 
	 * @param searchStrings  - for what was searched .. in case of TTH size will be 1
	 * @param source - who sent the search  a user object if passive .. InetSocketAddress if active
	 * @param NrOfFoundResults - how many results to this search we had..
	 */
	public void searchReceived(Set<String> searchStrings,Object source, int nrOfFoundResults);
}
