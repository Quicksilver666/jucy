package helpers;

import java.io.IOException;
import java.io.OutputStream;

import com.jcraft.jzlib.JZlib;
import com.jcraft.jzlib.ZOutputStream;


public class FDeflaterOutputStream extends ZOutputStream {

	public FDeflaterOutputStream(OutputStream out,boolean fastEncryption) {
		super(out, fastEncryption? JZlib.Z_BEST_SPEED: JZlib.Z_BEST_COMPRESSION);
	}

	

	@Override
	public void finish() throws IOException {
		try {
			super.finish();
		} catch (RuntimeException re) {
			throw new IOException();
		}
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
	public void write(byte[] b, int off, int len) throws IOException {
		try {
			super.write(b, off, len);
		} catch (RuntimeException re) {
			throw new IOException();
		}
	}

}
