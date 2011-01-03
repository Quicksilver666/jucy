package uc.protocols;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;



import logger.LoggerFactory;

import org.apache.log4j.Logger;



public class VarByteBuffer {

	private static Logger logger = LoggerFactory.make(); 
	

	
	
	private byte[] current = new byte[1024];
	private int readpos;
	private int writepos;
	private ByteBuffer wrapper = ByteBuffer.wrap(current);
	private final int maxSize;
	
	private InputStream decompression;
	
	
	



	/**
	 * 
	 * @param wantedMaxSize recommended size buffer 
	 * will shrink if it gets larger..
	 * 
	 */
	public VarByteBuffer(int wantedMaxSize) {
		this.maxSize = wantedMaxSize;
	}
	

	
	public int writeToChannel(WritableByteChannel wchan) throws IOException {
		wrapper.position(readpos);
		wrapper.limit(writepos);
		int written = wchan.write(wrapper);
		readpos = wrapper.position();
		
		if (current.length > maxSize && writepos-readpos < current.length/4) {
			shrinkBuffer();
		}
	
		return written;
	}
	
	/*
	 * @param addBytes - bytes that should be encrypted..
	 * @param engine - used for encryption..
	 *
	public SSLEngineResult encrypt(ByteBuffer addBytes, SSLEngine engine) throws SSLException {
		ensureWriteSize(Math.max(addBytes.remaining(), engine.getSession().getPacketBufferSize()));
		wrapper.limit(current.length);
		wrapper.position(writepos);
		
		
		SSLEngineResult ssler = engine.wrap(addBytes, wrapper);
		writepos = wrapper.position();
		
		return ssler;
	} */
	/*
	public SSLEngineResult decrypt(ByteBuffer encrypted, SSLEngine engine) throws SSLException {
		ByteBuffer helpdec = ByteBuffer.allocate(engine.getSession().getApplicationBufferSize());
		SSLEngineResult ssler = engine.unwrap(encrypted, helpdec);
		
		helpdec.flip();
		
		putBytes(helpdec);
		
	//	ensureWriteSize(encrypted.remaining()+1000);
	//	wrapper.limit(current.length);
	//	wrapper.position(writepos);
		
		
	//	SSLEngineResult ssler= engine.unwrap(encrypted, wrapper);
	//	writepos = wrapper.position();
		
		return ssler;
		
	} */
	
	public boolean hasRemaining() {
		return readpos < writepos;
	}
	
	public int remaining() {
		return writepos - readpos;
	}
	
	public void clear() {
		current = new byte[1024];
		wrapper = ByteBuffer.wrap(current);
		readpos = 0 ;
		writepos = 0;
		decompression = null;
	}
	
	public void putBytes(ByteBuffer bytes) {
	
		ensureWriteSize(bytes.remaining());
		wrapper.limit(current.length);
		wrapper.position(writepos);
		
	
		wrapper.put(bytes);

		writepos = wrapper.position();
	}
	
	/**
	 * 
	 * @param bytes - where the bytes shall go to
	 * @return number of bytes trnasferred
	 */
	public int getBytes(ByteBuffer bytes) {
		wrapper.position(readpos);
		
		int remaining = bytes.remaining();
		int limit =  Math.min( readpos+remaining , writepos);
		
		wrapper.limit( limit);
		
		bytes.put(wrapper);
		
		readpos = wrapper.position();

		return remaining - bytes.remaining();
		
		
	}
	
	private void ensureWriteSize(int size) {
		if (size >= current.length - writepos) {
			if (readpos + current.length - writepos > size) { //just move if total size allows it
				System.arraycopy(current, readpos, current, 0, writepos - readpos);
			} else { //create new
				byte[] newArray = new byte[Math.max(current.length*2,current.length+size+1)];
				System.arraycopy(current, readpos, newArray, 0, writepos - readpos);
				current = newArray;
				wrapper = ByteBuffer.wrap(current);
			}
			writepos -= readpos;
			readpos = 0;
			
		} 
	}
	
	private void shrinkBuffer() {
		byte[] newArray = new byte[Math.max(writepos - readpos,1024)];
		System.arraycopy(current, readpos, newArray, 0, writepos-readpos);
		current = newArray;
		writepos-=readpos;
		readpos = 0;
		wrapper = ByteBuffer.wrap(current);
	}
	
	/**
	 * 
	 * @param stopper - read until this char is reached ..
	 * or everything available.. if not found..
	 * 
	 * @param decoder - used for decoding
	 * @return the String read..
	 */
	public byte[] readUntil(byte stopper) {
		if (decompression == null) {
			int limit = writepos;
			for (int i = readpos; i < writepos ; i++) {
				if (current[i] == stopper) {
					limit = i+1;
					break;
				}
			}
			wrapper.position(readpos);
			wrapper.limit(limit);
			byte[] read= new byte[limit-readpos];
			wrapper.get(read);
//			CharBuffer cb = CharBuffer.allocate(limit-readpos);
//			decoder.decode(wrapper, cb, true);
//			cb.flip();
			readpos = wrapper.position();
			return read;
		} else {
			int read;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
			try {
				while ( -1 != (read = decompression.read())) {
					//System.out.print((char)read);
					baos.write(read);
					if (read == stopper) {
						break;
					}
				}
				if (read == -1) {
					decompression = null;
					
				//	logger.warn("disabling ZPipe");
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
			
//			ByteBuffer buf = ByteBuffer.wrap(baos.toByteArray());
//			CharBuffer cb = CharBuffer.allocate(buf.remaining());
//			decoder.decode(buf, cb, true);
//			cb.flip();
//			if (decompression == null) {
//		
//				StringBuilder sb = new StringBuilder(); 
//				for (int i = readpos ; i < writepos; i++) {
//					sb.append((char)(current[i] & 0xff));
//				}
//				
//				logger.debug("last string read: "+cb.toString()+" nextin Buffer: "+sb );
//				
//			}
//			if (cb.toString().equals("IZON")) {
//				logger.debug("used ZON for eof");
//				decompression = null;
//			}
//			logger.debug("read compressed: "+cb);
			
			return baos.toByteArray();
		}
	}
	
	public void setDecompression(Compression comp,final Lock lock,final Condition bytesChanged) throws IOException {
		if (comp == Compression.NONE) {
			decompression = null;
		} else {
			InputStream is = new InputStream() {
				@Override
				public int read() throws IOException {
					int i = 0;
					
					lock.lock();
					try {
						while (!hasRemaining()) {
							try {
								i++;
								bytesChanged.await(100, TimeUnit.MILLISECONDS);
								logger.debug("wait" + i);
							} catch (InterruptedException ie) {
							}
							if (i > 120) { // 2 minutes timeout...
								logger.warn("timeout");
								return -1;
							}
						}
						return current[readpos++] & 0xff;
					} finally {
						lock.unlock();
					}
					
				}
			};
			decompression =  comp.wrapIncoming(is);
		}
	}
	

	
	

	/**
	 * debug output of current contents
	 */
	public String toString() {
		return new String(current,readpos, writepos-readpos);
	}

	
}
