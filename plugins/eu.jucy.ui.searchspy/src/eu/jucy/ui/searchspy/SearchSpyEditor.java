package eu.jucy.ui.searchspy;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;


import uc.ISearchReceivedListener;
import uc.crypto.HashValue;
import uihelpers.SUIJob;
import uihelpers.TableViewerAdministrator;

import eu.jucy.gui.ApplicationWorkbenchWindowAdvisor;
import eu.jucy.gui.UCEditor;
import eu.jucy.ui.searchspy.SearchSpyColumns.Count;
import eu.jucy.ui.searchspy.SearchSpyColumns.Hits;
import eu.jucy.ui.searchspy.SearchSpyColumns.SearchStringColumn;
import eu.jucy.ui.searchspy.SearchSpyColumns.Time;



public class SearchSpyEditor extends UCEditor implements ISearchReceivedListener {

	public static final String ID = "eu.jucy.ui.searchspy.SearchSpyEditor" ;
	
	private Label hitRatioLabel;
	private Label hitsLabel;
	private Label averageLabel;
	private Label totalLabel;
	private Table table;
	private TableViewer tableViewer;
	
	private TableViewerAdministrator<SearchInfo> tva;
	
	private final Map<String,SearchInfo> info = new HashMap<String,SearchInfo>();
	
	/**
	 * 
	 */
	private final List<SearchInfo> lastSearchInfos = new LinkedList<SearchInfo>();
	
	private long totalSearches = 0;
	private long hits = 0;
	

	
	public SearchSpyEditor() {
	}
	
	
	
	
	
	public void createPartControl(Composite parent) {
		parent.setLayout(new GridLayout());

		tableViewer = new TableViewer(parent, SWT.FULL_SELECTION | SWT.BORDER);
		tableViewer.setContentProvider(new SearchSpyContentProvider());
		table = tableViewer.getTable();
		table.setHeaderVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		final Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		final GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 5;
		composite.setLayout(gridLayout);

		final Button ignoreTthSearchesButton = new Button(composite, SWT.CHECK);
		ignoreTthSearchesButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (ignoreTthSearchesButton.getSelection()) {
					tableViewer.addFilter(new ViewerFilter() {
						
						public boolean select(Viewer viewer,
								Object parentElement, Object element) {
							return  !((SearchInfo)element).tthSearch;
						}
					});
				} else {
					tableViewer.setFilters(new ViewerFilter[]{});
				}
			}
		});
		ignoreTthSearchesButton.setSelection(false);
		ignoreTthSearchesButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		ignoreTthSearchesButton.setText(Lang.SPYHideTTHSearches);

		totalLabel = new Label(composite, SWT.BORDER);
		totalLabel.setLayoutData(new GridData(100, SWT.DEFAULT));
	

		averageLabel = new Label(composite, SWT.BORDER);
		averageLabel.setLayoutData(new GridData(100, SWT.DEFAULT));


		hitsLabel = new Label(composite, SWT.BORDER);
		hitsLabel.setLayoutData(new GridData(100, SWT.DEFAULT));
	

		hitRatioLabel = new Label(composite, SWT.BORDER);
		hitRatioLabel.setLayoutData(new GridData(100, SWT.DEFAULT));
	
		tva = new TableViewerAdministrator<SearchInfo>(tableViewer,
				Arrays.asList(new SearchStringColumn(),new Count(),new Time(),new Hits()),
				ID,TableViewerAdministrator.NoSorting);
		tva.apply();
		
		
		tableViewer.setInput(this);
		ApplicationWorkbenchWindowAdvisor.get().registerSRL(this);
		setControlsForFontAndColour(tableViewer.getTable());
	}



	
	public void searchReceived(Set<String> searchStrings, Object source,
			final int nrOfFoundResults) {
		final String shown =concat(searchStrings);
		
		new SUIJob(table) {
			public void run() {
				hits += nrOfFoundResults;
				totalSearches++;
				SearchInfo found = info.get(shown);
				if (found == null) {
					found = new SearchInfo(shown,nrOfFoundResults);
					info.put(shown, found);
					tableViewer.add(found);
				}
				found.update(nrOfFoundResults);
				tableViewer.update(found, null);

				lastSearchInfos.add(found);
				if (lastSearchInfos.size() > 5 ) {
					lastSearchInfos.remove(0);
				}
				updateLabels();
			}
		}.schedule();
	}
	
	private void updateLabels() {
		totalLabel.setText(String.format(Lang.SPYTotal,totalSearches)); 
		averageLabel.setText(String.format(Lang.SPYAverage,Float.valueOf(getAverage())));
		hitsLabel.setText(String.format(Lang.SPYHitsFormatted,hits));
		hitRatioLabel.setText(String.format(Lang.SPYHitRatio,(float)hits/(float)totalSearches));
	}
	
	private float getAverage() {
		if (lastSearchInfos.isEmpty() ) {
			return 0f;
		}
		long timedif = lastSearchInfos.get(lastSearchInfos.size()-1 ).date-lastSearchInfos.get(0).date;
		if (timedif == 0) {
			return 0f;
		}
		return (1000f*(float)lastSearchInfos.size())/(float)timedif  ;
		
		
	}
	
	private static String concat(Set<String> searchStrings) {
		if (searchStrings.size() == 1) {
			return ((String)searchStrings.toArray()[0]).intern();
		} else {
			String s = null;
			for (String part: searchStrings) {
				if (s == null) {
					s = part;
				} else {
					s += "  "+part;
				}
			}
			if (s == null) {
				return "";
			} else {
				return s.intern();
			}
		}
	}
	
	

	
	public void setFocus() {
		table.setFocus();
	}


	public void dispose() {;
		ApplicationWorkbenchWindowAdvisor.get().unregisterSRL(this);
		super.dispose();
	}
	
	public static class SearchInfo {
		private final String searchString;
		private final boolean tthSearch;
		private int results;
		private int count = 0;
		private long date;
		
		public SearchInfo(String searchString, int results) {
			this.searchString = searchString;
			this.results = results;
			tthSearch = HashValue.isHash(searchString);
		}
		
		void update(int searchresults) {
			results = searchresults;
			count++;
			date = System.currentTimeMillis();
		}

		public String getSearchString() {
			return searchString;
		}

		public int getResults() {
			return results;
		}

		public int getCount() {
			return count;
		}

		public long getDate() {
			return date;
		}
		
	}

	class SearchSpyContentProvider implements IStructuredContentProvider {

		
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof SearchSpyEditor) {
				return info.values().toArray();
			}
			return new Object[]{};
		}

		
		public void dispose() {}

		
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
		
	}
	
}
