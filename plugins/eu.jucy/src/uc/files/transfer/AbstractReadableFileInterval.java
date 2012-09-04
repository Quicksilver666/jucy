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
import uc.files.downloadqueue.Block;
import uc.files.downloadqueue.FileDQE;

public abstract class AbstractReadableFileInterval extends AbstractFileInterval {

	private static final Logger logger = LoggerFactory.make();

	private static final class FCHolder {
		public FCHolder(FileChannel fc) {
			super();
			this.fc = fc;
		}
		private final FileChannel fc;
		private final Set<AbstractReadableFileInterval> readingFrom = new HashSet<AbstractReadableFileInterval>();
		private Future<?> closer;
	}
	private static final Map<File,FCHolder> cachedFiles = new HashMap<File,FCHolder>();
	
	private static FileChannel openFC(File source,AbstractReadableFileInterval rfi) throws IOException {
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
	
	private static boolean isOpenFC(File source,AbstractReadableFileInterval rfi) {
		logger.debug("checking file: "+source);
		synchronized (cachedFiles) {
			FCHolder fch = cachedFiles.get(source);
			if (fch == null) {
				return false;
			}
			return fch.readingFrom.contains(rfi) && fch.fc.isOpen();
		}
	}
	
	private static void closeFC(final File source,AbstractReadableFileInterval rfi) {
	//	logger.debug("closeing file: "+source);
		synchronized (cachedFiles) {
			final FCHolder fch = cachedFiles.get(source);
			if (fch != null) {
				fch.readingFrom.remove(rfi);
				if (fch.closer != null) {
					fch.closer.cancel(false);
				}
				if (fch.readingFrom.isEmpty()) { //Delayed close -> better caching for os if lots of stopping reads..
					fch.closer = DCClient.getScheduler().schedule(new Runnable() {
						public void run() {
							synchronized(cachedFiles) {
								if (fch.readingFrom.isEmpty()) {
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

	public static class FileReadInterval extends AbstractReadableFileInterval {
		/**
		 * possible source for upload
		 */
		private final File source;
		
		/**
		 * 
		 * @param f - the whole file will be transferred..
		 */
		public FileReadInterval(File wholeFile) {
			this(wholeFile,0);
		}
		public FileReadInterval(File wholeFile, long startpos) {
			this(wholeFile,startpos,wholeFile.length()-startpos);
		}
		
		public FileReadInterval(File wholeFile, long startpos , long length ) {
			super(startpos,length,wholeFile.length());
			if (!wholeFile.isFile()) {
				throw new IllegalArgumentException("did not provide a valid file "+wholeFile);
			}
			
			source = wholeFile;
			currentpos = startpos;
			
			this.length = length+startpos; 
		
		}
		@Override
		public ReadableByteChannel getReadableChannel() throws IOException {
			return new ReadableByteChannel() {
				FileChannel fc = openFC(source,FileReadInterval.this); 
				
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
					closeFC(source, FileReadInterval.this);
				}
			
				public boolean isOpen() {
					return isOpenFC(source, FileReadInterval.this); // fc.isOpen();
				}
			};
		}
		
		
		
	}
	
	
	public static class MemoryReadInterval extends AbstractReadableFileInterval {
		/**
		 * other possible source for an upload..
		 */
		
		private final byte[] directBytes;
		
		/**
		 * 
		 * @param bytes makes some bytes readable for a transfer..
		 */
		public MemoryReadInterval(byte[] bytes) {
			super(0,bytes.length,bytes.length);
			directBytes = bytes;
		}

		@Override
		public ReadableByteChannel getReadableChannel() throws IOException {
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
		}
	}

	public static class FileDQEReadInterval extends AbstractReadableFileInterval {

		private final int startBlock;
		private volatile int currentBlock;
		private long discardBytes;
		private final FileDQE dqe;
		public FileDQEReadInterval(FileDQE fdqe,long startpos, long length) {
			super(startpos, length, fdqe.getSize());
			this.dqe = fdqe;
			long blocksize = dqe.getBlock(0).getLength();
			this.startBlock = blocksize == 0 ? 0: (int) (startpos / blocksize);
			if (blocksize != 0) { //discard bytes if startpos should not match a block
				discardBytes = startpos % blocksize;
			}
			this.currentBlock = startBlock;
		}

		@Override
		public ReadableByteChannel getReadableChannel() throws IOException {
			return new ReadableByteChannel() {
				private Block current = dqe.getBlock(currentBlock);
				private ReadableByteChannel currentBlockChannel = null;
				
				private int recursiveness = 0;
				@Override
				public int read(ByteBuffer dst) throws IOException {
					//set a working current Write channel if not already set
					if (currentBlockChannel == null) {
						if (current == null) {
							throw new IOException();
						}
						synchronized(current) {
							if (current.isFinished()) { 
								currentBlockChannel = current.getReadChannel();
								while (discardBytes > 0) {
									ByteBuffer bb = ByteBuffer.allocate((int)Math.min(1024, discardBytes));
									int r = currentBlockChannel.read(bb);
									if (r > 0) {
										discardBytes-=r;
									} else {
										throw new IOException("Discard failure");
									}
								}
								
							} else {
								return -1;
							}
						}
					}
					
					int read = currentBlockChannel.read(dst);
					
					if (read == 0 | read == -1) { //no more bytes could be written..
						//so advance by one block
						currentBlockChannel.close();
						currentBlockChannel = null;
						currentBlock++;
						current = dqe.getBlock(currentBlock);
						if (++recursiveness > 1) {
							throw new IOException("read failed");
						}
						return read(dst);
						
					} else {
						recursiveness = 0;
						currentpos += read; 
						return read;
					}
				}
				
				public void close() throws IOException {
					//hook start.. this hook will jump in for 0 Byte files . Otherwise they can't be downloaded 
					long blocksize = dqe.getBlock(0).getLength();
					if (blocksize == 0) {
						dqe.getBlock(0).getWriteChannel().close();
					}
					//hook end
					
					if (currentBlockChannel != null) {
						currentBlockChannel.close();
					}

				}

				
				public boolean isOpen() {
					if (currentBlockChannel != null) {
						return currentBlockChannel.isOpen();
					} else {
						return dqe.getBlock(currentBlock+1) != null;
					}
				}
				
			};
		}
		
		
		
	}
	
	


	protected AbstractReadableFileInterval(long startpos, long length, long totalLength) {
		super(startpos, length, totalLength);
	}

	/**
	 * creates a readable file interval for upload of interleave hashes
	 * @param interleaves - the interleave hashes that should be uploaded
	 * this is a conve
	 */
	public static AbstractReadableFileInterval create(InterleaveHashes interleaves) {
		byte[] directBytes = new byte[(int)interleaves.byteSize()];
		
		int current = 0;
		for (HashValue hash :interleaves.getInterleaves()) {
			byte[] hashbytes= hash.getRaw();
			System.arraycopy(hashbytes, 0, directBytes, current, hashbytes.length);
			current += hashbytes.length;
		}
		return new MemoryReadInterval(directBytes);
	}

	/**
	 * 
	 * @return a channel to read date from the file..
	 * 
	 */
	public abstract ReadableByteChannel getReadableChannel() throws IOException;
	

	

	@Override
	public long getCurrentPosition() {
		return currentpos;
	}

	@Override
	public long length() {
		return length;
	}
	
//	/**
//	 * 
//	 * @return a channel to read date from the file..
//	 * 
//	 */
//	public ReadableByteChannel getReadableChannel() throws IOException {
//		if (directBytes != null) {
//			return Channels.newChannel( new ByteArrayInputStream(directBytes) {
//
//				@Override
//				public synchronized int read(byte[] b, int off, int len) {
//					int read = super.read(b, off, len); 
//					if (read != -1) {
//						currentpos += read;
//					}
//					return read;
//				}
//			});
//		} else {
//			return new ReadableByteChannel() {
//				FileChannel fc = openFC(source,ReadableFileInterval.this); 
//				
//				public int read(ByteBuffer dst) throws IOException {
//					if (currentpos == length) {
//						return -1;
//					}
//					
//					if (dst.remaining() > length - currentpos) {
//						dst.limit((int)(dst.position()+ length - currentpos));
//					}
//					
//					int read = fc.read(dst, currentpos);
//					if (read != -1) {
//						currentpos += read;
//					}
//					return read;
//				}
//
//		
//				public void close() throws IOException {
//					closeFC(source, ReadableFileInterval.this);
//				}
//			
//				public boolean isOpen() {
//					return isOpenFC(source, ReadableFileInterval.this); // fc.isOpen();
//				}
//			};
//			
//		}
//	}

}
