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
				return SizeEnum.getReadableSize(GH.isEmpty(value) ? 0 : Long.parseLong(value));
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
				return 	(GH.isEmpty(o1) ? Long.valueOf(0) :Long.valueOf(o1)).compareTo(
						(GH.isEmpty(o2) ? Long.valueOf(0) :Long.valueOf(o2)));
			case PERCENT:
			case INT:
				return (GH.isEmpty(o1) ? Integer.valueOf(0): Integer.valueOf(o1)).compareTo(
						GH.isEmpty(o2) ? Integer.valueOf(0): Integer.valueOf(o2));
			case STRING:
				return o1.compareTo(o2);
			}
		} catch (NumberFormatException nfe) {
			return STRING.compare(o1, o2);
		}
		return 0;
	}
	

	
	
	
}