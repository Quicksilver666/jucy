package uihelpers;

import helpers.IObservableCollection;
import helpers.Observable.IObserver;
import helpers.StatusObject;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class ObservableCollectionContentProvider implements
		IStructuredContentProvider {

	private final IObserver<StatusObject> observer;
	
	
	public ObservableCollectionContentProvider(IObserver<StatusObject> observer) {
		super();
		this.observer = observer;
	}

	@Override
	public void dispose() {}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (oldInput instanceof IObservableCollection) {
			((IObservableCollection)oldInput).deleteObserver(observer);
		}
		if (newInput instanceof IObservableCollection) {
			((IObservableCollection)newInput).addObserver(observer);
		}
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof IObservableCollection) {
			return ((IObservableCollection)inputElement).values().toArray();
		}
		return new Object[]{};
	}

}
