package helpers;

import java.io.IOException;
import java.io.InputStream;

/**
 * Filters lower characters from an InputStream by replacing them with a space character..
 * Helps reading of the often invalid XML found in hublists..
 * 
 * @author Quicksilver
 *
 */
public class FilterLowerBytes extends InputStream {

	private final InputStream in;
	
	private int filteredChars = 0;

	public FilterLowerBytes(InputStream in) {
		this.in = in;
	}
	
	/**
	 * replace invalid characters with a space..
	 */
	@Override
	public int read() throws IOException {
		int read = in.read();
		if (read >= 0x20  || read == -1) {  
			return read;
		} else {
			filteredChars++;
			return ' '; 
		}
	}

	@Override
	public void close() throws IOException {
		super.close();
		in.close();
	}
	
	public int getFilteredChars() {
		return filteredChars;
	}
	
}
