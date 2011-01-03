/**
 * 
 */
package eu.jucy.hublist;

import helpers.GH;
import helpers.SizeEnum;

import java.util.Comparator;

enum ColumnType implements Comparator<String> {
	STRING,INT,BYTES,PERCENT;
	
	public static ColumnType forName(String name) {
		return  ColumnType.valueOf(name.toUpperCase());
	}
	
	public String getPresentation(String value) {
		try {
			switch(this) {
			case BYTES:
				long val = GH.isEmpty(value) ? 0L : (long)Double.parseDouble(value);
				return SizeEnum.getReadableSize(val);
			case PERCENT:
				return (GH.isEmpty(value) ? "" :value+" %");
			}
		} catch (NumberFormatException nfe) { }
		
		return value;
	}

	public int compare(String o1, String o2) {
		try {
			switch(this) {
			case BYTES:
			case PERCENT:
			case INT:
				long lone = GH.isEmpty(o1) ?0:Long.parseLong(o1);
				long ltwo = GH.isEmpty(o2) ?0:Long.parseLong(o2);
				return GH.compareTo(lone, ltwo);
			case STRING:
				return o1.compareTo(o2);
			}
		} catch (NumberFormatException nfe) {
			return STRING.compare(o1, o2);
		}
		return 0;
	}
	

	
	
	
}