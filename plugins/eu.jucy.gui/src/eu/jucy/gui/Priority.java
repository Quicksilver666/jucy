package eu.jucy.gui;





/**
 * 
 * simple enum to handle the priority of files
 * 
 * @author Quicksilver
 *
 */
public enum Priority {
	
	
	
	PAUSED(0,Lang.Paused),
	LOWEST(255/4,Lang.Lowest), 
	LOW((255/2)-1,Lang.Low ), 
	NORMAL(255/2,Lang.Normal), 
	HIGH((255*3)/4,Lang.High), 
	HIGHEST(255,Lang.Highest);
	
	
	
	private final int top;
	private final String lang;
	
	Priority(int topValue,String lang) {
		this.top = topValue;
		this.lang = lang;
	}
	
	public static Priority getPriority(int value) {
		for (Priority p:values()) {
			if (value <= p.top) {
				return p;
			}
		}
		throw new IllegalStateException("bad priority: "+value);
	}
	
	public String toString() {
		return lang;
	}

	public int getDefaultValue() {
		return top;
	}

}
