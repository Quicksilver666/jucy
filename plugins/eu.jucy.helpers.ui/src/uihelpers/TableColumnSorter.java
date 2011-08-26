package uihelpers;




import java.util.Comparator;

import logger.LoggerFactory;

import org.apache.log4j.Logger;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

import uihelpers.TableViewerAdministrator.IColumnDescriptor;


public class TableColumnSorter<T> implements SelectionListener {
	
	private static final Logger logger = LoggerFactory.make();
	
	private final StructuredViewer viewer;
	private Table table;
	private Tree tree;
	
	private final Item column;

	
	private final ViewerComparator sorterUp;
	private final ViewerComparator sorterDown;

	
	private int sortDirection = SWT.UP;
	
	
	
	private TableColumnSorter(StructuredViewer structuredViewer,Item column, 
			ViewerComparator sorterUp,ViewerComparator sorterDown ) {
		this.viewer = structuredViewer;
		this.column = column;
		this.sorterUp = sorterUp;
		this.sorterDown = sorterDown;
	} 
	
	public TableColumnSorter(TableViewer tableViewer,TableColumn tableColumn, 
			ViewerComparator sorterUp,ViewerComparator sorterDown ) {
		this((StructuredViewer)tableViewer,tableColumn,sorterUp,sorterDown);
		
		this.table = tableViewer.getTable();
		tableColumn.addSelectionListener(this);
	}
	
	public TableColumnSorter(TableViewer tableViewer,TableColumn tableColumn,IColumnDescriptor<T> col ) {
		this(tableViewer,tableColumn, col.getComparator(false), col.getComparator(true));
	}
	
	public TableColumnSorter(TableViewer tableViewer,TableColumn tableColumn,Comparator<T> c ) {
		this(tableViewer,tableColumn, Comp(c,true),Comp(c,false));
	}
	
	public TableColumnSorter(TreeViewer treeViewer,TreeColumn treeColumn, 
			ViewerComparator sorterUp,ViewerComparator sorterDown ) {
		this((StructuredViewer)treeViewer,treeColumn,sorterUp,sorterDown);
		
		this.tree = treeViewer.getTree();
		treeColumn.addSelectionListener(this);
	}
	
	public TableColumnSorter(TreeViewer treeViewer,TreeColumn treeColumn,IColumnDescriptor<T> col ) {
		this(treeViewer,treeColumn, col.getComparator(false), col.getComparator(true));
	}
	
	public TableColumnSorter(TreeViewer treeViewer,TreeColumn treeColumn,Comparator<T> c ) {
		this(treeViewer,treeColumn, Comp(c,true),Comp(c,false));
	}

	


 
	private void setSorter() {
		if (table == null) {
			tree.setSortDirection(sortDirection);
		} else {
			table.setSortDirection(sortDirection);
		}
		
		viewer.getControl().setRedraw(false);

		
		switch(sortDirection) {
		case SWT.NONE:
			viewer.setComparator(null); 
			break;
		case SWT.UP: 
			viewer.setComparator(sorterUp); 
			break;
		case SWT.DOWN: 
			viewer.setComparator(sorterDown); 
			break;
		}
		viewer.getControl().setRedraw(true);
	}
	

	public void widgetDefaultSelected(SelectionEvent e) {
		widgetSelected(e);
	}


	public void widgetSelected(SelectionEvent e) {
		if (getSortColumn() == column) {
			changeSortDirection();
			setSorter();
		} else {
			logger.debug("column changed");
			if (sorterUp != null) {
				setSortColumn(column); //choose this column for sorting
				setSorter();
			} else {
				setSortColumn(null);
			}
		}		
	}
	
	private Item getSortColumn() {
		if (table == null) {
			return tree.getSortColumn();
		} else {
			return table.getSortColumn();
		}
	}
	
	private void setSortColumn(Item i) {
		if (table == null) {
			tree.setSortColumn((TreeColumn)i);
		} else {
			table.setSortColumn((TableColumn)i);
		}
	}

	private void changeSortDirection() {
		switch(sortDirection) {
		case SWT.NONE:
			sortDirection = SWT.UP;
			break;
		case SWT.UP:
			sortDirection = SWT.DOWN;
			break;
		case SWT.DOWN:
			sortDirection = SWT.NONE;
			break;
		}
	}
	
	
	private static <T> ViewerComparator Comp(final Comparator<T> comp,final boolean inverted) {
		return new ViewerComparator() {
			@SuppressWarnings("unchecked")
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				return (inverted? -1: 1)* comp.compare((T)e1, (T)e2);
			}
		};
	}

}
