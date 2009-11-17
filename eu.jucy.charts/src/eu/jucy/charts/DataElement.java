package eu.jucy.charts;

public class DataElement {

	private final long time;
	
	private final float value;

	public DataElement(long time, float value) {
		super();
		this.time = time;
		this.value = value;
	}

	public long getTime() {
		return time;
	}

	public float getValue() {
		return value;
	}
	
	public String toString() {
		return String.format("%f", value);
	}
	
}
