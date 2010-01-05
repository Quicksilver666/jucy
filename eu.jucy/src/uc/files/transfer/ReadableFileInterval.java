package uc.files.transfer;

import helpers.GH;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import logger.LoggerFactory;


import org.apache.log4j.Logger;

import uc.DCClient;
import uc.crypto.HashValue;
import uc.crypto.InterleaveHashes;

public class ReadableFileInterval extends AbstractFileInterval {

	private static final Logger logger = LoggerFactory.make();

	private static final class FCHolder {
		public FCHolder(FileChannel fc) {
			super();
			this.fc = fc;
		}
		private final FileChannel fc;
		private final Set<ReadableFileInterval> readingFrom = new HashSet<ReadableFileInterval>();
		private Future<?> closer;
	}
	private static final Map<File,FCHolder> cachedFiles = new HashMap<File,FCHolder>();
	
	private static FileChannel openFC(File source,ReadableFileInterval rfi) throws IOException {
	//	logger.debug("opening file: "+source);
		synchronized (cachedFiles) {
			FCHolder fch = cachedFiles.get(source);
			if (fch == null) {
				fch = new FCHolder(new FileInputStream(source).getChannel());
				cachedFiles.put(source, fch);
			}
			if (fch.closer != null) {
				fch.closer.cancel(false);
			}
			fch.readingFrom.add(rfi);
			return fch.fc;
		}
	}
	
	private static boolean isOpenFC(File source,ReadableFileInterval rfi) {
		logger.debug("checking file: "+source);
		synchronized (cachedFiles) {
			FCHolder fch = cachedFiles.get(source);
			if (fch == null) {
				return false;
			}
			return fch.readingFrom.contains(rfi) && fch.fc.isOpen();
		}
	}
	
	private static void closeFC(final File source,ReadableFileInterval rfi) {
	//	logger.debug("closeing file: "+source);
		synchronized (cachedFiles) {
			final FCHolder fch = cachedFiles.get(source);
			if (fch != null) {
				fch.readingFrom.remove(rfi);
				if (fch.closer != null) {
					fch.closer.cancel(false);
				}
				if (fch.readingFrom.isEmpty()) {
				//	logger.debug("scheduleing closer: "+source);
					fch.closer = DCClient.getScheduler().schedule(new Runnable() {
						public void run() {
							synchronized(cachedFiles) {
								if (fch.readingFrom.isEmpty()) {
								//	logger.debug("final closeing file: "+source);
									GH.close(fch.fc);
									cachedFiles.remove(source);
								}
							}
						}
					}, 15, TimeUnit.SECONDS);
				}
			}
		}
	}

	
	
	/**
	 * possible source for upload
	 */
	private final File source;
	/**
	 * other possible source for an upload..
	 */
	
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
				FileChannel fc = openFC(source,ReadableFileInterval.this); 
				
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
					closeFC(source, ReadableFileInterval.this);
				}
			
				public boolean isOpen() {
					return isOpenFC(source, ReadableFileInterval.this); // fc.isOpen();
				}
			};
			
		}
	}

}
