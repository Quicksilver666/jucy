package eu.jucy.ui.searchspy;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;


import eu.jucy.ui.searchspy.SearchSpyEditor.SearchInfo;
import uihelpers.TableViewerAdministrator.ColumnDescriptor;

public abstract class SearchSpyColumns extends ColumnDescriptor<SearchInfo> {

	public SearchSpyColumns(int defaultColumnSize, String columnName, int style) {
		super(defaultColumnSize, columnName, style);
	}

	
	public Image getImage(SearchInfo x) {
		return null;
	}


	
	public static class SearchStringColumn extends SearchSpyColumns {

		public SearchStringColumn() {
			super(350, Lang.SearchString, SWT.LEAD);
		}
		
		
		public String getText(SearchInfo x) {
			return x.getSearchString();
		}
	}
	
	public static class Count extends SearchSpyColumns {

		public Count() {
			super(80, Lang.Count, SWT.TRAIL);
		}

		
		public Comparator<SearchInfo> getComparator() {
			return new Comparator<SearchInfo>() {
				
				public int compare(SearchInfo o1, SearchInfo o2) {
					return Integer.valueOf(o1.getCount()).compareTo(o2.getCount());
				}
			};
		}

		
		public String getText(SearchInfo x) {
			return ""+x.getCount();
		}
	}
	
	public static class Time extends SearchSpyColumns {

		private final SimpleDateFormat format;
		public Time() {
			super(100, Lang.Time, SWT.LEAD);
			format = new SimpleDateFormat("HH:mm:ss");
		}

		
		public Comparator<SearchInfo> getComparator() {
			return new Comparator<SearchInfo>() {

				
				public int compare(SearchInfo o1, SearchInfo o2) {
					Long.valueOf(o1.getDate()).compareTo(o2.getDate());
					return 0;
				}
				
			};
		}

		
		public String getText(SearchInfo x) {
			return format.format(new Date(x.getDate()));
		}
	}
	
	public static class Hits extends SearchSpyColumns {
		
		public Hits() {
			super(60,Lang.Hits,SWT.TRAIL);
		}

		
		public Comparator<SearchInfo> getComparator() {
			return new Comparator<SearchInfo>() {
				
				public int compare(SearchInfo o1, SearchInfo o2) {
					return Integer.valueOf(o1.getResults()).compareTo(o2.getResults());
				}
			};
		}

		
		public String getText(SearchInfo x) {
			return Integer.toString(x.getResults());
		}
	}
	
	
}
