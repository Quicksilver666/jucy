package missing16api;

public class IOException extends java.io.IOException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public IOException(Throwable  e) {
		this.initCause(e);
	}
	
	public IOException(String message,Throwable e) {
		super(message);
		this.initCause(e);
	}

}
