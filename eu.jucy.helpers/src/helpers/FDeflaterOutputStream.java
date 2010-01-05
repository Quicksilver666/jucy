package helpers;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public class FDeflaterOutputStream extends DeflaterOutputStream {

	public FDeflaterOutputStream(OutputStream out) {
		super(out);
	}

	public FDeflaterOutputStream(OutputStream out, Deflater def) {
		super(out, def);
	}

	public FDeflaterOutputStream(OutputStream out, Deflater def, int size) {
		super(out, def, size);
	}

	@Override
	public void finish() throws IOException {
		if (!def.finished()) {
		    def.finish();
		    int i = 0;
		    while (!def.finished()) {
		    	deflate();
		    	if (++i > 1000) {
		    		throw new IOException(); 
		    	}
		    }
		}
	}
	
	
	

}
