package helpers;

/**
 * 
 * a status Object that can be passed to an Observer
 * 
 * 
 * @author Quicksilver
 *
 */
public class StatusObject {

	public static enum ChangeType {
		ADDED,CHANGED,REMOVED;
	}
	
	private final Object value;
	
	private final ChangeType type;
	
	/**
	 * detail information that might be useful..
	 * i.e. if what was changed should be done more precisely..
	 */
	private final int detail;
	
	/**
	 * object with more information ..
	 * could be parent object for a tree..
	 */
	private final Object detailObject;
	
	public StatusObject(Object o,ChangeType ct, int detail,Object detailObject) {
		this.value = o;
		this.type = ct;
		this.detail = detail;
		this.detailObject = detailObject;
	}

	
	public StatusObject(Object o,ChangeType ct) {
		this(o,ct,0,null);
	}

	public Object getValue() {
		return value;
	}

	public ChangeType getType() {
		return type;
	}
	



	@Override
	public String toString() {
		return "StatusObject [value=" + value + ", type=" + type + ", detail="
				+ detail + ", detailObject=" + detailObject + "]";
	}


	public int getDetail() {
		return detail;
	}


	public Object getDetailObject() {
		return detailObject;
	}
	
	
	
	
}
