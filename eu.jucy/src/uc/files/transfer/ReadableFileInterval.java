package uc.files.transfer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

import uc.crypto.HashValue;
import uc.crypto.InterleaveHashes;

public class ReadableFileInterval extends AbstractFileInterval {




	
	/**
	 * possible source for upload
	 */
	private final File source;
	/**
	 * other possible source for an upload..
	 */
	//private final InterleaveHashes interleaves;
	
	private final byte[] directBytes;
	/**
	 * 
	 * @param f - the whole file will be transferred..
	 */
	public ReadableFileInterval(File wholeFile) {
		this(wholeFile,0);
	}
	public ReadableFileInterval(File wholeFile, long startpos) {
		this(wholeFile,startpos,wholeFile.length()-startpos);
	}
	
	public ReadableFileInterval(File wholeFile, long startpos , long length ) {
		super(startpos,length,wholeFile.length());
		if (!wholeFile.isFile()) {
			throw new IllegalArgumentException("did not provide a valid file "+wholeFile);
		}
		
		source = wholeFile;
		currentpos = startpos;
		
		this.length = length+startpos; 
		directBytes = null;
	}
	
//	@Override
//	public long getShownLength() {
//		return getTotalLength();
//	}
//	
//	@Override
//	public long getShownRelativePos() {
//		return currentpos;
//	}
	
/*
	public ReadableFileInterval(InterleaveHashes interleaves) {
		source = null;
		currentpos = 0;
		length = interleaves.byteSize();
		//this.interleaves = interleaves;
		directBytes = new byte[(int)length];
		
		int current = 0;
		for (HashValue hash :interleaves.getInterleaves()) {
			byte[] hashbytes= hash.getRaw();
			System.arraycopy(hashbytes, 0, directBytes, current, hashbytes.length);
			current += hashbytes.length;
		}
	} */
	

	/**
	 * creates a readable file interval for upload of interleave hashes
	 * @param interleaves - the interleave hashes that should be uploaded
	 * this is a conve
	 */
	public static ReadableFileInterval create(InterleaveHashes interleaves) {
		byte[] directBytes = new byte[(int)interleaves.byteSize()];
		
		int current = 0;
		for (HashValue hash :interleaves.getInterleaves()) {
			byte[] hashbytes= hash.getRaw();
			System.arraycopy(hashbytes, 0, directBytes, current, hashbytes.length);
			current += hashbytes.length;
		}
		return new ReadableFileInterval(directBytes);
	}

	/**
	 * 
	 * @param bytes makes some bytes readable for a transfer..
	 */
	public ReadableFileInterval(byte[] bytes) {
		super(0,bytes.length,bytes.length);
		source = null;
	//	currentpos = 0;
	//	length = bytes.length;
	//	interleaves = null;
		directBytes = bytes;
	}
	

	
	

	@Override
	public long getCurrentPosition() {
		return currentpos;
	}

	@Override
	public long length() {
		return length;
	}
	
	/**
	 * 
	 * @return a channel to read date from the file..
	 * 
	 */
	public ReadableByteChannel getReadableChannel() throws IOException {
		if (directBytes != null) {
			return Channels.newChannel( new ByteArrayInputStream(directBytes) {

				@Override
				public synchronized int read(byte[] b, int off, int len) {
					int read = super.read(b, off, len); 
					if (read != -1) {
						currentpos += read;
					}
					return read;
				}
				
			});
		} else {
			return new ReadableByteChannel() {

				FileChannel fc = new FileInputStream(source).getChannel();
				
		
				public int read(ByteBuffer dst) throws IOException {
					if (currentpos == length) {
						return -1;
					}
					
					if (dst.remaining() > length - currentpos) {
						dst.limit((int)(dst.position()+ length - currentpos));
					}
					
					int read = fc.read(dst, currentpos);
					if (read != -1) {
						currentpos += read;
					}
					return read;
				}

		
				public void close() throws IOException {
					fc.close();
				}

			
				public boolean isOpen() {
					return fc.isOpen();
				}
			};
			
		}
	}
	
	/*
	 * creates a channel for the interleave hashes..
	 *
	 *
	private ReadableByteChannel getInterleaveChannel() {
		return new ReadableByteChannel() {
			private boolean open = true;
			
			@Override
			public int read(ByteBuffer dst) throws IOException {
				if (currentpos == length) {
					return -1;
				}
				
				int read = 0;
				while (dst.hasRemaining() && currentpos != length) {
					int currentHashValue = (int) (currentpos / HashValue.digestlength);
					int positionInArray = (int) (currentpos % HashValue.digestlength);
					
					int bytesSentToBuffer = Math.min(dst.remaining(),  HashValue.digestlength - positionInArray);
					
					dst.put( interleaves.getHashValue(currentHashValue).getRaw() ,positionInArray , bytesSentToBuffer);
					currentpos   += bytesSentToBuffer;
					read += bytesSentToBuffer;
				}
				
				return read;
			}

			@Override
			public void close() throws IOException {
				open = false;
			}

			@Override
			public boolean isOpen() {
				return open;
			}
			
		};
	} */
	

}
