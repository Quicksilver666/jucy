package helpers;

import java.util.Collection;

public interface IObservableCollection extends IObservable<StatusObject> {
	
	/**
	 * 
	 * @return a view of the current state of the collection
	 */
	Collection<? extends Object> values();
	
}