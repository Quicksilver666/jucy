package uc;

public class ChangedAttribute {
	private final String attribute,oldValue,newValue;

	public ChangedAttribute(String attribute, String oldValue,
			String newValue) {
		super();
		this.attribute = attribute;
		this.oldValue = oldValue;
		this.newValue = newValue;
	}

	public String getAttribute() {
		return attribute;
	}

	public String getOldValue() {
		return oldValue;
	}

	public String getNewValue() {
		return newValue;
	}
	
}