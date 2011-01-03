/**
 * 
 */
package eu.jucy.hublist;

import java.util.Comparator;


public class Column {
	
	public static final Column 	ADDRESS = new Column("Address",ColumnType.STRING),
								HUBNAME	= new Column("Name",ColumnType.STRING),
								DESCRIPTION = new Column("Description",ColumnType.STRING),
								PORT = new Column("Port", ColumnType.STRING),
								USERS = new Column("Users",ColumnType.INT);
	
	private final String name;
	private final ColumnType type;
	
	public Column(String name,ColumnType type) {
		this.name = name;
		this.type = type;
	}
	
	
	public int getDefaultWidth() {
		if (this.equals(HUBNAME)) {
			return 200;
		} else if (this.equals(DESCRIPTION)) {
			return 300;
		} else {
			switch (type) {
			case BYTES:
				return 70;
			case INT:
				return 50;
			case PERCENT:
				return 40;
			case STRING:
				return 100;
			}
			
		}
		throw new IllegalStateException("unreachable");
	}
	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Column other = (Column) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	public String getName() {
		return name;
	}

	public ColumnType getType() {
		return type;
	}
	
	public Comparator<HublistHub> getComparator() {
		return new Comparator<HublistHub>() {
			public int compare(HublistHub o1, HublistHub o2) {
				return type.compare(o1.getAttribute(Column.this), o2.getAttribute(Column.this));
			}
			
		};
	}


	
	
}