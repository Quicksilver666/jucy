package uihelpers;

import helpers.PreferenceChangedAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;






/**
 * helper class that will take most of the work of managing a TableViewer/TreeViewer
 * 
 * it will handle :
 * - creating columns /make them movable
 * - persisting order and size of the columns
 * - handle sorting
 * 
 * it is no:
 * - ContentProvider! must be set separately
 * 
 * @author Quicksilver
 *
 */
public class TableViewerAdministrator<T> {

	public static final String ExtensionpointID = "eu.jucy.helpers.tablevieweradministrator";
	
	public static final int NoSorting = Integer.MIN_VALUE;
	
	private final StructuredViewer viewer;
	private Table table;
	private Tree tree;
	
	
	private final List<IColumnDescriptor<T>> originalDescriptors = new ArrayList<IColumnDescriptor<T>>();
	private final List<IColumnDescriptor<T>> descriptors = new ArrayList<IColumnDescriptor<T>>();
	
	private PreferenceChangedAdapter changedColsWatcher;
	
	private final String tableID ;
	
	private final int defaultSortCol;
	private TableTreePersister persister;
	
	private final boolean sortAllowed;
	
	
	private TableViewerAdministrator(StructuredViewer viewer, List<? extends IColumnDescriptor<T>> columns, String tableID,int defaultSortCol,boolean allowSorting) {
		this.viewer = viewer;
		this.originalDescriptors.addAll(columns);
		this.tableID = tableID;
		this.sortAllowed = allowSorting;
		this.defaultSortCol = defaultSortCol;
		loadDescriptors();
		
		viewer.getControl().addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				descriptors.clear();
				persister.dispose();
				changedColsWatcher.dispose();
			}
			
		});
	}

	/**
	 * 
	 * @param tableviewer - the Viewer on which this manager will work
	 * @param columns - the Columns that should be added to the viewer
	 * @param tableID - used for storing values and for contributing new columns to the table 
	 * @param allowSorting - allow to use the sorting function provided by the columns?
	 *  default is true
	 * @param defaultSortCol for which column should be sorted by default.. Integer.MinValue for none
	 *        -columnNumber -1 for inverted..
	 */
	public TableViewerAdministrator(TableViewer tableviewer, List<? extends IColumnDescriptor<T>> columns, String tableID,int defaultSortCol,boolean allowSorting) {
		this((StructuredViewer)tableviewer,columns,tableID,defaultSortCol,allowSorting);
		this.table = tableviewer.getTable();
	}
	public TableViewerAdministrator(TableViewer tableviewer, List<? extends IColumnDescriptor<T>> columns, String tableID,int defaultSortCol) {
		this(tableviewer,columns,tableID,defaultSortCol,true );
	}
	
	public TableViewerAdministrator(TreeViewer treeviewer, List<? extends IColumnDescriptor<T>> columns, String tableID,int defaultSortCol,boolean allowSorting) {
		this((StructuredViewer)treeviewer,columns,tableID,defaultSortCol,allowSorting);
		this.tree = treeviewer.getTree();
	}
	public TableViewerAdministrator(TreeViewer treeviewer, List<? extends IColumnDescriptor<T>> columns, String tableID,int defaultSortCol) {
		this(treeviewer,columns,tableID,defaultSortCol,true );
	}
	
	
	private void refresh() {
		//first delete everything
		for (Item tc: (table== null?tree.getColumns():table.getColumns())) {
			tc.dispose();
		}
		descriptors.clear();
		persister.dispose();
		changedColsWatcher.dispose();
		
		loadDescriptors();
		apply();
	}
	
	@SuppressWarnings("unchecked")
	private void loadDescriptors() {
		IExtensionRegistry reg = Platform.getExtensionRegistry();
    	
		IConfigurationElement[] configElements = reg
		.getConfigurationElementsFor(ExtensionpointID);
		
		List<String> allIDs = new ArrayList<String>();

		descriptors.addAll(originalDescriptors);
		
		for (IConfigurationElement element : configElements) {
			try {
				if ("table".equals(element.getName()) && tableID.equals(element.getAttribute("id")) ) {
					for (IConfigurationElement newColumns : element.getChildren("table_column")) {
						String fullID =  TVAPI.IDForTableColumn(tableID, newColumns.getAttribute("id"), false);
						allIDs.add(fullID);
						if (TVAPI.get(fullID)) {
							IColumnDescriptor<T>  col = (IColumnDescriptor<T>)newColumns.createExecutableExtension("class");
							descriptors.add(col);
						}
					}
					
					List<IColumnDescriptor<T>> descriptorsCopy = new ArrayList<IColumnDescriptor<T>>(descriptors);
					
					for (IConfigurationElement decorators : element.getChildren("table_column_decorator")) {
						String fullID =  TVAPI.IDForTableColumn(tableID,decorators.getAttribute("id"), true);
						allIDs.add(fullID);
						if (TVAPI.get(fullID)) {
							TableColumnDecorator<T> tcd = (TableColumnDecorator<T>)decorators.createExecutableExtension("class");
							String classname = decorators.getAttribute("columnToDecorate");
							for (int i =0; i < descriptors.size(); i++) {
								String foundclassanme = descriptorsCopy.get(i).getClass().getName();
								if (foundclassanme.equals(classname)) {
									tcd.init(descriptors.get(i));
									descriptors.set(i, tcd);
								}
							}
						}
					}
				}
			} catch (CoreException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		changedColsWatcher = new PreferenceChangedAdapter(new InstanceScope().getNode(TVAPI.PLUGIN_ID),allIDs.toArray(new String[0])) {
			@Override
			public void preferenceChanged(String preference, String oldValue,String newValue) {
				new SUIJob() {
					@Override
					public void run() {
						refresh();			
					}
				}.scheduleIfNotRunning(1000,this);
				
			}
			
		};
	}
	
	
	
	public void apply() {
	
		if (table == null) {
			for (IColumnDescriptor<T> col: descriptors ) {
				final TreeColumn treeColumn = new TreeColumn(tree, col.getStyle());
				treeColumn.setText(col.getColumnName());
				treeColumn.setWidth(col.getDefaultColumnSize());
				if (sortAllowed) {
					new TableColumnSorter<T>((TreeViewer)viewer,treeColumn, col);
				}
				treeColumn.setMoveable(true);
			}
			persister = new TableTreePersister(tree, tableID, extractColWidths());
		} else {
			for (IColumnDescriptor<T> col: descriptors ) {
				final TableColumn tableColumn = new TableColumn(table, col.getStyle());
				tableColumn.setText(col.getColumnName());
				tableColumn.setWidth(col.getDefaultColumnSize());
				if (sortAllowed) {
					new TableColumnSorter<T>((TableViewer)viewer,tableColumn, col);
				}
				tableColumn.setMoveable(true);
			}
			
			persister = new TableTreePersister(table, tableID, extractColWidths());
		}
		
		persister.load();
		
		viewer.setLabelProvider(new AdminLabelProvider());
		if (defaultSortCol != NoSorting) {
			boolean inverted = defaultSortCol < 0 ;
			int col = inverted? (-defaultSortCol)+1: defaultSortCol;
			viewer.setComparator(descriptors.get(col).getComparator(inverted));
		}
	}
	
	
	private int[] extractColWidths() {
		int[] widths = new int[descriptors.size()];
		
		for (int i=0; i < widths.length; i++) {
			widths[i] = descriptors.get(i).getDefaultColumnSize();
		}
		return widths;
	}
	
	
	/**
	 * content provider utilising the ColumnDescriptors to provide content 
	 * @author Quicksilver
	 *
	 */
	@SuppressWarnings("unchecked")
	class AdminLabelProvider extends LabelProvider  implements ITableLabelProvider,ITableColorProvider,ITableFontProvider  {

		public Image getColumnImage(Object element, int columnIndex) {
			return  descriptors.get(columnIndex).getImage((T)element);
		}

		public String getColumnText(Object element, int columnIndex) {
			return descriptors.get(columnIndex).getText((T)element);
		}


		public Color getBackground(Object element, int columnIndex) {
			return descriptors.get(columnIndex).getBackground((T)element);
		}

		public Color getForeground(Object element, int columnIndex) {
			return descriptors.get(columnIndex).getForeground((T)element);
		}

		public Font getFont(Object element, int columnIndex) {
			return descriptors.get(columnIndex).getFont((T)element);
		}
		
		
		
	}
	
	
	public abstract static class ColumnDescriptor<X> implements IColumnDescriptor<X> {
		
		private final int defaultColumnSize;
		private final String columnName;
		private final int style;
		
		public ColumnDescriptor(int defaultColumnSize, String columnName, int style) {
			this.defaultColumnSize 	= defaultColumnSize;
			this.columnName 		= columnName;
			this.style 				= style;
		}
		
		public ColumnDescriptor(int defaultColumnSize, String columnName) {
			this(defaultColumnSize, columnName , SWT.LEAD);
		}
		
		/**
		 * A text from the object
		 * @param x - the object that is presented
		 * @return a String to be shown in the Column
		 */
		public abstract String getText(X x);
		
		
		/**
		 * an image for the provided object 
		 * @param x 
		 * @return null if no image should be shown..
		 */
		public Image getImage(X x) {
			return null;
		}
		
		
		public Color getForeground(X x) {
			return null;
		}
		
		
		public Color getBackground(X x) {
			return null;
		}
		
		public Font getFont(X x) {
			return null;
		}

		/**
		 * @return a simple comparator that compares
		 * lexicographically by using getText()
		 * subclasses should override if this is not enough
		 */
		public Comparator<X> getComparator() {
			return new Comparator<X>() {
				
				public int compare(X o1, X o2) {
					String t1 = null,t2;
					if (o1 == null || o2 == null || (t1=getText(o1))==null || (t2=getText(o2))==null ) {
						if (o1 != null && t1 == null && Platform.inDevelopmentMode()) {
							System.err.println(o1);
						}
						return 0;
					}
					return t1.compareTo(t2);
				}
				
			};
		}
		
		/**
		 * returns the normal reverse comparator..
		 */
		public Comparator<X> getReverseComparator() {
			if (getComparator() == null) {
				return null;
			}
			return Collections.reverseOrder(getComparator());
		}
		
		/**
		 * 
		 * @return an instance of viewer Comparator for this column
		 * based on getComparator()
		 */
		@SuppressWarnings("unchecked")
		public final ViewerComparator getComparator(boolean inverted) {
			final Comparator<X> comparator = inverted ? getReverseComparator():getComparator();
			if (comparator == null) {
				return null;
			}
			return new ViewerComparator() {
				public int compare(Viewer viewer, Object e1, Object e2) {
					return comparator.compare((X) e1, (X)e2);
				}
			};
			
		}
		
		
		public final int getDefaultColumnSize() {
			return defaultColumnSize;
		}

		public final String getColumnName() {
			return columnName;
		}

		public final int getStyle() {
			return style;
		}
		
	}
	
	
	public static interface IColumnDescriptor<X> {
		
		/**
		 *
		 * @return a text to be presented in the line
		 */
		String getText(X x);
		
		
		/**
		 * 
		 * @return an icon to be presented in the line
		 */
		Image getImage(X x);
		
		
		/**
		 * Colour for Foreground
		 */
		Color getForeground(X x);
		
		/**
		 * Colour for the Background
		 */
		Color getBackground(X x);
		
		/**
		 * Font for the table Column
		 * 
		 * @param x - the item
		 * @return which font
		 */
		Font getFont(X x);
		
		/**
		 * @return comparator used to sort the line
		 */
		Comparator<X> getComparator();
		
		/**
		 * 
		 * @return the reverse comparator.. that might defer from 
		 * just the normal comparators inversion
		 */
		Comparator<X> getReverseComparator();
		
		/**
		 * 
		 * @return the default width for columns
		 * 
		 */
		int getDefaultColumnSize();
		
		/**
		 * @return how the column is called
		 * 
		 */
		String getColumnName();
		
		/**
		 * 
		 * @return what SWT style the column should use
		 */
		int getStyle();
		
		
		/**
		 * 
		 * @param inverted - if the comparator should be inverted..
		 * @return a Comparator for this column..
		 *  (this is automatically implemented by ColumnDscriptor based on getComparator())
		 */
		ViewerComparator getComparator(boolean inverted);
		
	}
	
	public static abstract class TableColumnDecorator<T> implements IColumnDescriptor<T> {


		public TableColumnDecorator() {}
		
		private IColumnDescriptor<T> parent;
		
		protected void init(IColumnDescriptor<T> parent) {
			this.parent = parent;
		}
		
		
		public final Color getBackground(T x) {
			return getBackground(x,parent.getBackground(x));
		}
		
		public Color getBackground(T t,Color parentcolor) {
			return parentcolor;
		}
		

		public final Color getForeground(T x) {
			return getForeground(x,parent.getForeground(x));
		}
		
		public Color getForeground(T t,Color parentcolor) {
			return parentcolor;
		}
		
		public final Font getFont(T x) {
			return getFont(x,parent.getFont(x));
		}
		
		public Font getFont(T t,Font parentfont) {
			return parentfont;
		}
		

		public final Image getImage(T x) {
			return getImage(x,parent.getImage(x));
		}
		
		public Image getImage(T t,Image parent) {
			return parent;
		}
		
		public final String getText(T x) {
			return getText(x,parent.getText(x));
		}
		
		public String getText(T t, String parent) {
			return parent;
		}
		
		
		public String getColumnName() {
			return parent.getColumnName();
		}

		public  Comparator<T> getComparator() {
			return parent.getComparator();
		}

		public ViewerComparator getComparator(boolean inverted) {
			return parent.getComparator(inverted);
		}

		public int getDefaultColumnSize() {
			return parent.getDefaultColumnSize();
		}

		public Comparator<T> getReverseComparator() {
			return parent.getReverseComparator();
		}

		public int getStyle() {
			return parent.getStyle();
		}
	}
	
}
