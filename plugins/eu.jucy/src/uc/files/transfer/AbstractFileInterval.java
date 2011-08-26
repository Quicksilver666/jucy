package uc.files.transfer;


/**
 * a FileInterval that can be shown in the status field of the gui..
 * @author Quicksilver
 *
 */
public abstract class AbstractFileInterval {
	
	/**
	 * where in the file we started..
	 */
	protected final long startpos;
	
	/**
	 * where we currently are in the file
	 * 
	 * 0 <= startpos <= currentpos <= startpos+length
	 */
	protected volatile long currentpos;
	
	/**
	 * length of transferred
	 */
	protected volatile long length;
	
	/**
	 * length of the transfered file
	 * 0 <= startpos <= currentpos <= startpos+length <= totalLength
	 *  
	 */
	private final long totalLength;
	
	public AbstractFileInterval(long startpos,long length,long totalLength) {
		this.startpos = startpos;
		this.currentpos = startpos;
		this.length = length;
		this.totalLength = totalLength;
	}
	
	
	/**
	 * retrieves the total length of the transferred interval
	 * @return r of bytes in total
	 */
	public long length() {
		return length;
	}
	
	/**
	 * 
	 * @return current position in the file.. 
	 * (this is also the start position before writing to the interval)
	 * so it can be used for creating the ADCGET
	 */
	public long getCurrentPosition() {
		return currentpos;
	}
	
	/**
	 * position in the interval  relative to startposition
	 * 
	 * @return currentposition - startposition
	 */
	public long getRelativeCurrentPos() {
		return currentpos- startpos;
	}


	/**
	 * 
	 * @return startposition in the file 
	 */
	public long getStartpos() {
		return startpos;
	}

	/**
	 * 
	 * @return length of the file being transferred
	 */
	public long getTotalLength() {
		return totalLength;
	}


	@Override
	public String toString() {
		return "AbstractFileInterval [startpos=" + startpos + ", currentpos="
				+ currentpos + ", length=" + length + ", totalLength="
				+ totalLength + "]";
	}
	
	
	
}
