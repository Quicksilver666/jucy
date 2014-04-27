package uc.protocols;

import helpers.FDeflaterOutputStream;
import helpers.FInflaterInputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Map;


import org.apache.tools.bzip2.CBZip2InputStream;
import org.apache.tools.bzip2.CBZip2OutputStream;







/**
 * wrapper of all possible compressions supported.
 * 
 * 
 * @author Quicksilver
 *
 */
public enum Compression {

	
	NONE(""),
	BZ2(" BZ2"),
	ZLIB_FAST(" ZL1"),
	ZLIB_BEST(" ZL1");
	

	private final String nmdcString;
	
	Compression(String nmdcidentifier) {
		nmdcString = nmdcidentifier;
	}
	
	/**
	 * wraps a decompressing channel around rbc
	 */
	public CompressionWrapper wrapIncoming(ReadableByteChannel rbc) throws IOException {
		
		switch(this){
		case NONE:
			return new CompressionWrapper(rbc);
		case ZLIB_FAST:	
		case ZLIB_BEST:
			
			FInflaterInputStream zis =  new FInflaterInputStream(
					new BufferedInputStream( Channels.newInputStream(rbc)));
			return new CompressionWrapper(Channels.newChannel(zis),zis);
		case BZ2:
			return new CompressionWrapper(Channels.newChannel(new CBZip2InputStream( 
					new BufferedInputStream( Channels.newInputStream(rbc)) )));  
		}
		throw new IllegalStateException();
	}
	
	/**
	 * wraps a decompressing stream around in
	 * 
	 */
	public InputStream wrapIncoming(InputStream in) throws IOException {
		switch(this){
		case NONE:
			return in;
		case ZLIB_FAST:	
		case ZLIB_BEST:
			return new FInflaterInputStream(in);
		case BZ2:
			return new CBZip2InputStream(in);  
		}
		throw new IllegalStateException();
	}
	
	
	
	/**
	 * wrap a stream so  
	 * 
	 * @param wbc - writable bytechannel that should be wrapped usually a socketchannel
	 * @return a writable bytechannel that writes compressed info to the provided one
	 * @throws IOException - if an error happens on creation
	 */
	public FinishWrapper wrapOutgoing(WritableByteChannel wbc) throws IOException {
		FDeflaterOutputStream out = null;
	//	Deflater def;
		switch(this){
		case NONE:
			return new FinishWrapper(wbc);
		case ZLIB_FAST:
			OutputStream os = new BufferedOutputStream(Channels.newOutputStream(wbc));
			//out = new DeflaterOutputStream(os);
		//	def = new Deflater(Deflater.BEST_SPEED);
			out =  new FDeflaterOutputStream(os,true); //   new ZOutputStream( os ,JZlib.Z_BEST_SPEED) ;  //new Deflater(Deflater.BEST_SPEED) );
			//return Channels.newChannel(new GZIPOutputStream( new BufferedOutputStream(Channels.newOutputStream(wbc))));
			return  new FinishWrapper(Channels.newChannel(out),out);
		case ZLIB_BEST:
	//		def = new Deflater(Deflater.BEST_COMPRESSION);
			out = new FDeflaterOutputStream( new BufferedOutputStream(Channels.newOutputStream(wbc)),false);  //new Deflater(Deflater.BEST_COMPRESSION) );
			return  new FinishWrapper(Channels.newChannel(out),out);
		case BZ2:
			return  new FinishWrapper(Channels.newChannel(new CBZip2OutputStream( new BufferedOutputStream(Channels.newOutputStream(wbc)))));  
		}
		
		throw new IllegalStateException();
	}

	
	/**
	 * 
	 * @param toCompress what should be compressed..
	 * @return bytes in compressed form..
	 */
	public byte[] compress(byte[]  toCompress) throws IOException {
		if (this == NONE) {
			return toCompress;
		} else {
			ByteBuffer bytes = ByteBuffer.wrap(toCompress);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			FinishWrapper fw = wrapOutgoing(Channels.newChannel(baos));
			WritableByteChannel wbc = fw.getChan();
			while (bytes.hasRemaining())  {
				wbc.write(bytes);
			}
			fw.finnish();
			byte[] compressed = baos.toByteArray();
			return compressed;
		}
	}
	
	/**
	 * 
	 * @param nmdc - a string either empty or  "ZL1"  or  "ZL2"
	 * @return the matching compression... will return NONE as defaultvalue
	 */
	public static Compression parseProtocolString(String nmdc) {
		nmdc = nmdc.trim();
		if ( "ZL1".equals(nmdc)) {
			return ZLIB_FAST;
		} else if ("BZ2".equals(nmdc)) {
			return BZ2;
		} else {
			return NONE;
		}
	}
	
	public static Compression parseAttributeMap(Map<String,String> attributes) {
		if ("1".equals(attributes.get("ZL"))) {
			return Compression.ZLIB_FAST;
		} else if ("2".equals("BZ")) {
			return BZ2;
		} else {
			return NONE;
		}
	}
	
	public String toTransferViewString() {
		switch(this) {
		case NONE: 
			return "";
		case ZLIB_FAST:
		case ZLIB_BEST:
			return "[Z]";
		case BZ2:
			return "[BZ2]";
		}
		return null;
	}
	
	
	public String toString() {
		return nmdcString;
	}
	
	public static class FinishWrapper implements ICompIO {
		
		private final WritableByteChannel chan;

		private final FDeflaterOutputStream flush;
	//	private final Deflater def;
		public FinishWrapper(WritableByteChannel chan) {
			this(chan,null);
		}
		public FinishWrapper(WritableByteChannel chan, FDeflaterOutputStream flush /*,Deflater def*/) {
			this.chan = chan;
			this.flush = flush;
		}
		
		public WritableByteChannel getChan() {
			return chan;
		}
		
		public void finnish() throws IOException {
			if (flush != null) {
				flush.finish();
			}
			
		}
		public long getCompIO() {
			return flush== null? 1L : flush.getTotalOut(); //  .getBytesWritten();
		}
		public long getIO() {
			return flush == null? 1L : flush.getTotalIn();  //def.getBytesRead();
		}
		
		
		
	}
	
	public static class CompressionWrapper implements ICompIO {
		
		private final FInflaterInputStream inf;
		
		private final ReadableByteChannel rbc;
		
		public CompressionWrapper(ReadableByteChannel rbc) {
			this(rbc,null);
		}
		/**
		 * 
		 * @param rbc
		 * @param inf
		 * 
		 */
		public CompressionWrapper(ReadableByteChannel rbc, FInflaterInputStream inf) {
			this.inf = inf;
			this.rbc = rbc;
		}

		
		public long getCompIO() {
			return inf != null? inf.getTotalIn(): 1L ;
		}

		public long getIO() {
			return inf != null? inf.getTotalOut(): 1L ; 
		}
		
		public ReadableByteChannel getRbc() {
			return rbc;
		}

	}
	
	public static interface ICompIO {
		/**
		 * 
		 * @return IO data read ... always 1 if no compression
		 */
		long getIO();
		
		/**
		 * 
		 * @return compressed IOData read... always 1 if no compression..
		 */
		long getCompIO();
	}
	
	
}
