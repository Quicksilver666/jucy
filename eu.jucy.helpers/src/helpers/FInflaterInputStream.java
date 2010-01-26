package helpers;

import java.io.IOException;
import java.io.InputStream;

import com.jcraft.jzlib.ZInputStream;

public class FInflaterInputStream extends ZInputStream {

	public FInflaterInputStream(InputStream in) {
		super(in);
	}

	@Override
	public void close() throws IOException {
		try {
			super.close();
		} catch (RuntimeException re) {
			throw new IOException();
		}
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		try {
			return super.read(b, off, len);
		} catch (RuntimeException re) {
			throw new IOException();
		}
	}

	
	
}
