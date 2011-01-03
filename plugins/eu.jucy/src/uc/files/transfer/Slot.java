package uc.files.transfer;

/**
 * A slot shoudl represent a unit of uploads..
 * just so we have an object we may later use for better traffic control with semaphores..
 * like in TooFree..
 * 
 * @author Quicksilver
 *
 */
public class Slot {
	
	private final boolean regular;
	/**
	 * 
	 * @param regular - if this is a regular slot
	 */
	public Slot(boolean regular) {
		this.regular = regular;
	}
	
	
	public boolean isRegular() {
		return regular;
	}
	
	
	
}
