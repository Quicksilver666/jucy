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

	/**
	 * checks if exactly that hub (not a hub with same address is already favhub)
	 */
	boolean contains(FavHub hub);
	
	
	void changeOrder(FavHub favHub, boolean up);
	
	
	void addToFavorites(FavHub hub);
	 
	void removeFromFavorites(FavHub hub);
	
	void exchange(FavHub old,FavHub newHub);
	
	
	/**
	 * retrieves internal presentation of given  FavHub
	 * i.e. if a hub with the given husb address exists that hub is returned..
	 * 
	 * @param fh
	 * @return
	 */
	public FavHub internal(FavHub fh);
	

}