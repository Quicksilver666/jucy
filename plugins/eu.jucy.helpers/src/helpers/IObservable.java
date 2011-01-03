package helpers;

import helpers.Observable.IObserver;

public interface IObservable<X> {

	void addObserver(IObserver<X> o);

	void deleteObserver(IObserver<X> o);

}