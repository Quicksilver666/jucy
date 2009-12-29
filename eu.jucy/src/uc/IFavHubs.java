package uc;

import helpers.IObservable;

import java.io.IOException;
import java.util.List;

public interface IFavHubs extends IObservable<FavHub> {

	/**
	 * stores all hubs to disc.
	 * @throws IOException if storing didn't work.
	 */
	void store();

	List<FavHub> getFavHubs();

	void openAutoStartHubs();

	/**
	 * checks if a hub exists that has the provided 
	 * address
	 * @return true if the hub exists..
	 */
	boolean contains(String hubaddress);

	boolean contains(FavHub hub);
	
	
	void changeOrder(FavHub favHub, boolean up);
	
	
	void addToFavorites(FavHub hub);
	 
	void removeFromFavorites(FavHub hub);

}