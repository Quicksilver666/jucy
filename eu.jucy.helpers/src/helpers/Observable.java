package helpers;



import java.util.concurrent.CopyOnWriteArrayList;



public class Observable<X> implements IObservable<X> {
	
	private final CopyOnWriteArrayList<IObserver<X>> obs = new CopyOnWriteArrayList<IObserver<X>>();
	
	
	/* (non-Javadoc)
	 * @see helpers.IObserver#addObserver(helpers.Observable.Observer)
	 */
	public void addObserver(IObserver<X> o) {
		obs.addIfAbsent(o);
	}
	
	/* (non-Javadoc)
	 * @see helpers.IObserver#deleteObserver(helpers.Observable.Observer)
	 */
	public void deleteObserver(IObserver<X> o) {
		obs.remove(o);
	}
	
	/**
	 * notifies all Observers that they have changed
	 * and provided the given argument to the,,
	 * 
	 * @param arg
	 */
	public void notifyObservers(X arg) {
		for (IObserver<X> ob:obs) {
			ob.update(this, arg);
		}
	}
	
	 public void notifyObservers() {
		 notifyObservers(null);
	 }
	 
	 /**
	  * Clears the observer list so that this object no longer has any observers.
	  */
	 public void deleteObservers() {
		 obs.clear();
	 }
	
	
	
	public static interface IObserver<X> {
		
	    /**
	     * This method is called whenever the observed object is changed. An
	     * application calls an <tt>Observable</tt> object's
	     * <code>notifyObservers</code> method to have all the object's
	     * observers notified of the change.
	     *
	     * @param   o     the observable object.
	     * @param   arg   an argument passed to the <code>notifyObservers</code>
	     *                 method.
	     */
	    void update(IObservable<X> o, X arg);
	}

}
