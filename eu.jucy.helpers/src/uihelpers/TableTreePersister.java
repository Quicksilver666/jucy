package uihelpers;

import helpers.PreferenceChangedAdapter;

import java.util.regex.Pattern;

import logger.LoggerFactory;

import org.apache.log4j.Logger;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.osgi.service.prefs.BackingStoreException;




/**
 * persists the Column with of the columns
 * of a table, additionally the order of the 
 * columns
 * 
 * Whole class became a bit ugly since it also works for trees...
 * should have rather split it into 3 classes..
 * 
 * 
 * @author Quicksilver
 *
 */
public class TableTreePersister {

	private static final String PLUGIN_ID = "eu.jucy.helpers";
	
	private static final Logger logger = LoggerFactory.make();
	
	private Table table;
	private Tree tree;
	private final Composite tableOrTree;
	private final String columnSizeID;
	private final String columnOrderID;
	
	private final int[] defaultWidths;
	
	private PreferenceChangedAdapter pca;

	private TableTreePersister(Composite tableOrTreex, String persistenceID, int[] defaultWidths) {
		this.tableOrTree = tableOrTreex;
		this.defaultWidths = defaultWidths;
		columnSizeID = persistenceID+".columnsize";
		columnOrderID = persistenceID+".columnorder";
		
		//listener that will update the table if some changes occurred
		pca = new PreferenceChangedAdapter(new InstanceScope().getNode(PLUGIN_ID),columnSizeID,columnOrderID){
			@Override
			public void preferenceChanged(String preference, String oldValue,String newValue) {
				new SUIJob() {
					@Override
					public void run() {
						if (!tableOrTree.isDisposed()) {
							load();
						}
					}
				}.scheduleIfNotRunning(10000,this);
			}
		};
	}
	
	
	public TableTreePersister(Tree treex, String persistenceID, int[] defaultWidths) {
		this((Composite)treex,persistenceID,defaultWidths);
		this.tree = treex;
	}
	
	
	

	public TableTreePersister(Table tablex, String persistenceID, int[] defaultWidths) {
		this((Composite)tablex,persistenceID,defaultWidths);
		this.table = tablex;
	}
	
	/**
	 * may only be called after all TableColumsn have been created..
	 * will set widths to the columns and rearrange them
	 * 
	 */
	public void load() {
		int[] colwidths = getWidths();
		for (int i=0; i <colwidths.length; i++) {
			int width = Math.max(colwidths[i], 10);
			if (table == null) {
				tree.getColumn(i).setWidth(width);
			} else {
				table.getColumn(i).setWidth(width);
			}
		}
		
		 
		if (table == null) {
			tree.setColumnOrder(getColumnOrder());
		} else {
			table.setColumnOrder(getColumnOrder());
		}
		
		
		ControlAdapter ca = new ControlAdapter() {
			public void controlResized(final ControlEvent e) {
				new SUIJob() {
					@Override
					public void run() {
						store();
					}
						
				}.scheduleIfNotRunning(1000,TableTreePersister.this);
			}
		};
		
		
		
		if (table == null) {
			for (TreeColumn ti : tree.getColumns()) {
				ti.addControlListener(ca);
			}
		} else {
			for (TableColumn ti : table.getColumns()) {
				
				ti.addControlListener(ca);
			}
		}
	}
	
	/**
	 * stores all persistence information..
	 * must be called before columns are disposed..
	 */
	private void store() {
		if (table != null) {
			int[] colwidths = new int[table.getColumns().length];
			for (int i=0; i <colwidths.length; i++) {
				colwidths[i] = table.getColumn(i).getWidth();
			}
			store(columnSizeID, colwidths);
			store(columnOrderID,table.getColumnOrder());
		} else {
			int[] colwidths = new int[tree.getColumns().length];
			for (int i=0; i <colwidths.length; i++) {
				colwidths[i] = tree.getColumn(i).getWidth();
			}
			store(columnSizeID, colwidths);
			store(columnOrderID,tree.getColumnOrder());
		}
		
	}
	
	private int[] getWidths() {
		return load(columnSizeID,defaultWidths);
	}
	
	private int[] getColumnOrder() {
		int[] def = new int[defaultWidths.length];
		for (int i=0;i < def.length; i++) {
			def[i]=i;
		}
		
		return load(columnOrderID,def);
	}
	
	/**
	 * @return the persistent values (if they are correct)
	 */
	private int[] load(String key, int[] def) {
		IEclipsePreferences stored = new InstanceScope().getNode(PLUGIN_ID);
		
		int[] ret = fromString( stored.get(key, toString(def)) );
		if (ret.length != def.length) { 
			return def;
		} else {
			return ret ;
		}
	}
	
	private void store(String key, int[] what) {
		IEclipsePreferences stored = new InstanceScope().getNode(PLUGIN_ID);
		
		stored.put(key, toString(what));
		try {
			stored.flush();
		} catch(BackingStoreException bse) {
			logger.warn(bse,bse);
		}
	}
	
	private static int[] fromString(String s) {
		String[] splits= s.split(Pattern.quote(";"));
		int[] vals = new int[splits.length];
		for (int i=0; i < vals.length; i++) {
			vals[i] = Integer.parseInt(splits[i]);
		}
		return vals;
	}
	
	private static String toString(int[] a) {
		String ret = ""+a[0];
		for (int i =1 ; i < a.length; i++) {
			ret+= ";"+a[i];
		}
		return ret;
	}
	
	/**
	 * dispose of the persister..
	 */
	public void dispose() {
		pca.dispose();
	}
	
	
}
