package uihelpers;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Control;

/**
 * IPostSelectionProvider implementation that delegates to another
 * ISelectionProvider or IPostSelectionProvider. The selection provider used
 * for delegation can be exchanged dynamically. Registered listeners are
 * adjusted accordingly. This utility class may be used in workbench parts with
 * multiple viewers.
 * 
 * @author Marc R. Hoffmann
 */
public class SelectionProviderIntermediate implements IPostSelectionProvider {

	private final ListenerList selectionListeners = new ListenerList();

	private final ListenerList postSelectionListeners = new ListenerList();

	private ISelectionProvider delegate;

	private ISelectionChangedListener selectionListener = new ISelectionChangedListener() {
		public void selectionChanged(SelectionChangedEvent event) {
			if (event.getSelectionProvider() == delegate) {
				fireSelectionChanged(event.getSelection());
			}
		}
	};

	private ISelectionChangedListener postSelectionListener = new ISelectionChangedListener() {
		public void selectionChanged(SelectionChangedEvent event) {
			if (event.getSelectionProvider() == delegate) {
				firePostSelectionChanged(event.getSelection());
			}
		}
	};

	/**
	 * Sets a new selection provider to delegate to. Selection listeners
	 * registered with the previous delegate are removed before. 
	 * 
	 * @param newDelegate new selection provider
	 */
	public void setSelectionProviderDelegate(ISelectionProvider newDelegate) {
		if (delegate == newDelegate) {
			return;
		}
		if (delegate != null) {
			delegate.removeSelectionChangedListener(selectionListener);
			if (delegate instanceof IPostSelectionProvider) {
				((IPostSelectionProvider)delegate).removePostSelectionChangedListener(postSelectionListener);
			}
		}
		delegate = newDelegate;
		if (newDelegate != null) {
			newDelegate.addSelectionChangedListener(selectionListener);
			if (newDelegate instanceof IPostSelectionProvider) {
				((IPostSelectionProvider)newDelegate).addPostSelectionChangedListener(postSelectionListener);
			}
			fireSelectionChanged(newDelegate.getSelection());
			firePostSelectionChanged(newDelegate.getSelection());
		}
	}

	protected void fireSelectionChanged(ISelection selection) {
		fireSelectionChanged(selectionListeners, selection);
	}

	protected void firePostSelectionChanged(ISelection selection) {
		fireSelectionChanged(postSelectionListeners, selection);
	}

	private void fireSelectionChanged(ListenerList list, ISelection selection) {
		if (selection == null) {
			return;
		}
		SelectionChangedEvent event = new SelectionChangedEvent(delegate, selection);
		Object[] listeners = list.getListeners();
		for (int i = 0; i < listeners.length; i++) {
			ISelectionChangedListener listener = (ISelectionChangedListener) listeners[i];
			listener.selectionChanged(event);
		}
	}

	// IPostSelectionProvider Implementation

	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		selectionListeners.add(listener);
	}

	public void removeSelectionChangedListener(
			ISelectionChangedListener listener) {
		selectionListeners.remove(listener);
	}

	public void addPostSelectionChangedListener(
			ISelectionChangedListener listener) {
		postSelectionListeners.add(listener);
	}

	public void removePostSelectionChangedListener(
			ISelectionChangedListener listener) {
		postSelectionListeners.remove(listener);
	}

	public ISelection getSelection() {
		return delegate == null ? null : delegate.getSelection();
	}

	public void setSelection(ISelection selection) {
		if (delegate != null) {
			delegate.setSelection(selection);
		}
	}

	/**
	 * adds a viewer to the intermediate which's control will be used to detect
	 * if it has focus/the provider should be used now, 
	 * @param viewer
	 */
	public void addViewer(Viewer viewer) {
		addSelectionProvider(viewer.getControl(), viewer);
	}
	
	public void addSelectionProvider(Control control,final ISelectionProvider providerDelegate) {
		control.addFocusListener(new FocusListener() {

			public void focusGained(FocusEvent e) {
				setSelectionProviderDelegate(providerDelegate);
			}
			
			public void focusLost(FocusEvent e) {}
			
		});
	}
	
	
}
