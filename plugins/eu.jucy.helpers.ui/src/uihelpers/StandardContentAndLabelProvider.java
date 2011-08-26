package uihelpers;


import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;

public abstract class StandardContentAndLabelProvider<T,I> extends LabelProvider implements
		IStructuredContentProvider, ITableLabelProvider {

	public StandardContentAndLabelProvider() {}
	
	@SuppressWarnings("unchecked")
	public Object[] getElements(Object inputElement) {
		return getElementss((I)inputElement);
	}
	
	public abstract T[] getElementss(I inputElement);


	@SuppressWarnings("unchecked")	
	public final void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		inputChangeds(viewer,(I)oldInput,(I)newInput);
	}
	
	public void inputChangeds(Viewer viewer, I oldInput, I newInput) {}

	
	@SuppressWarnings("unchecked")
	
	public Image getColumnImage(Object element, int columnIndex) {
		return getColumnImage(columnIndex,(T)element);
	}

	protected abstract Image getColumnImage(int columnIndex,T element);
	
	@SuppressWarnings("unchecked")
	
	public String getColumnText(Object element, int columnIndex) {
		return getColumnText(columnIndex,(T) element);
	}
	
	protected abstract String getColumnText(int columnIndex,T element);


}
