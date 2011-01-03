package eu.jucy.gui.texteditor.hub;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;

public class ItemSelectionProvider implements IPostSelectionProvider {

	private IStructuredSelection o;
	
	private final List<ISelectionChangedListener> listeners = 
		new ArrayList<ISelectionChangedListener>();
	
	public ItemSelectionProvider() {}
	
	public ItemSelectionProvider(Object o) {
		this.o = new StructuredSelection(o);
	}
	
	public ItemSelectionProvider(List<Object> o) {
		this.o = new StructuredSelection(o);
	}
	
	
	
	public void addPostSelectionChangedListener(
			ISelectionChangedListener listener) {
		listeners.add(listener);
	}


	public void removePostSelectionChangedListener(
			ISelectionChangedListener listener) {
		listeners.remove(listener);
	}


	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		addPostSelectionChangedListener(listener);
	}


	public IStructuredSelection getSelection() {
		return o;
	}


	public void removeSelectionChangedListener(
			ISelectionChangedListener listener) {
		removePostSelectionChangedListener(listener);
	}


	public void setSelection(ISelection selection) {
		if (selection != null && selection instanceof IStructuredSelection) {
			this.o = (IStructuredSelection)selection;
		}
		for (ISelectionChangedListener scl: listeners) {
			scl.selectionChanged(new SelectionChangedEvent(this,o));
		}
	}
	
	
	public void setSelection(Object o) {
		setSelection(new StructuredSelection(o));
	}

}
