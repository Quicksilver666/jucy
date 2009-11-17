package uc.files.transfer;


import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;


import uc.crypto.InterleaveHashes;
import uc.files.downloadqueue.FileDQE;
import uc.files.downloadqueue.FileListDQE;
import uc.files.downloadqueue.TTHLDQE;


/**
 * the abstract WriteableFileInterval
 * represents a starting point to make downloads independent 
 * of what is downloaded .. TTHL files or FileList just 
 * extend this abstract class to fit their needs.
 * 
 * @author quicksilver
 *
 */
public abstract class AbstractWritableFileInterval extends AbstractFileInterval {
	
	public abstract WritableByteChannel getWriteChannel() throws IOException;

	
	
	
	
	public AbstractWritableFileInterval(long startpos, long length,long totalLength) {
		super(startpos, length, totalLength);
	}


//	@Override
//	public long getShownLength() {
//		return length();
//	}
//	
//	@Override
//	public long getShownRelativePos() {
//		return currentpos-startpos;
//	}




	public static class FileListWriteInterval extends AbstractWritableFileInterval {
		
		private final FileListDQE fdqe;
		
		public FileListWriteInterval(FileListDQE fdqe, long length) {
			super(0L,length,length);
			this.fdqe = fdqe;
		}
		
		@Override
		public WritableByteChannel getWriteChannel() throws IOException {
			return new WritableByteChannel() {
				private final FileChannel fc = 
					new FileOutputStream(fdqe.getTempPath()).getChannel();
				
				public void close() throws IOException {
					boolean wasOpen = fc.isOpen();
					fc.close();
					if (currentpos == length && wasOpen) {
						fdqe.downloadedFilelist();
					}
				}

				
				public boolean isOpen() {
					return fc.isOpen();
				}

				
				public int write(ByteBuffer src) throws IOException {
					int written = fc.write(src);
					if (written >= 0) {
						currentpos += written;
					}
					return written;
				}
				
			};
		}
	}
	
	public static class TTHLWriteInterval extends AbstractWritableFileInterval {

		private final TTHLDQE dqe;
		
		public TTHLWriteInterval(TTHLDQE dqe,long length) {
			super(0L,length,length);
			this.dqe = dqe;
		}
		
		public WritableByteChannel getWriteChannel() throws IOException {
			final ByteBuffer bbuf = ByteBuffer.allocate((int)length);
			bbuf.clear();
			
			return new WritableByteChannel() {

				private boolean open = true;
				
				public int write(ByteBuffer src) throws IOException {
					int remaining = src.remaining();
					if (remaining <= bbuf.remaining()) {
						bbuf.put(src);
						currentpos += remaining;
						return remaining;
					} else {
						return 0;
					}
				}

				/**
				 * when the WritableByteChannel for interleaves is closed
				 * the Interleave hashes are generated from the read bytes..
				 * afterwards the Interleave hashes are set to the DQE if they are valid
				 */
				
				public void close() throws IOException {
					if (open && !bbuf.hasRemaining()) {
						bbuf.flip();
						InterleaveHashes ih = new InterleaveHashes(bbuf);
						if (ih.verify(dqe.getTTHRoot())) {
							dqe.onDownloadOfInterleaves(ih);
						} else {
							throw new IOException("Interleaves do not match TTH root");
						}
					}
					open = false;
					
				}

				
				public boolean isOpen() {
					return open;
				}
				
			};
		}
		
	}
	
	public static class FileWriteInterval extends AbstractWritableFileInterval {

		private final FileDQE dqe;
		private final int startBlock;
		private volatile int currentBlock;
		//private volatile long bytesWrittenInCurrentBlock; 
		
		public FileWriteInterval(FileDQE dqe, long startpos , long length) {
			super(startpos,length,dqe.getSize());
			this.dqe = dqe;
			long blocksize = dqe.getBlock(0).getLength();
			this.startBlock = blocksize==0? 0: (int) (startpos / blocksize);
			if (blocksize != 0 && startpos % blocksize != 0 ) {
				throw new IllegalArgumentException("can't start download in the middle of a block sp:"+startpos+" blocksize: "+blocksize+"  "+dqe.getBlock(startBlock).getState());
			}
			
			this.currentBlock = startBlock;
			updateLength();
			//this.currentpos = 0; //startpos;
		}
		
		public WritableByteChannel getWriteChannel() throws IOException {
			return new WritableByteChannel() {

				private Block current = dqe.getBlock(startBlock);
				private WritableByteChannel currentBlockChannel = null;
				private int recursiveness = 0;
				
				
				public int write(ByteBuffer src) throws IOException {
					//set a working current Write channel if not already set
					if (currentBlockChannel == null) {
						if (current == null) {
							throw new IOException();
						}
						synchronized(current) {
							if (current.isWritable()) { //current != null &&  removed null check... would be dereferenced before
								currentBlockChannel = current.getWriteChannel();
							} else {
								return -1;
							}
						}
					}
					
					int written = currentBlockChannel.write(src);
					
					if (written == 0) { //no more bytes could be written..
						//so advance by one block
						currentBlockChannel.close();
						currentBlockChannel = null;
						
						currentBlock++;
						current = dqe.getBlock(currentBlock);
						
					//	bytesWrittenInCurrentBlock = 0;
						
						
						updateLength();
						
						if (++recursiveness > 20) {
							throw new IOException("write failed");
						}
						return write(src);
						
					} else {
						recursiveness = 0;
						currentpos += written; 
					//	bytesWrittenInCurrentBlock += written; 
						return written;
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
		
		/**
		 * updates the length value.. 
		 * called only on each new Block.. 
		 * from the writing stream
		 */
		private void updateLength() {
			
			long total = 0;
			Block b = dqe.getBlock(startBlock);
			long blocksize = b.getLength();
			total += blocksize * (currentBlock-startBlock);
			
			int current = currentBlock;
			while ((b = dqe.getBlock(current)) != null) {
				if (b.isWritable()) {
					total += b.getLength();
					current++;
				} else {
					break;
				}
			}
			length = total;
		}
		
		
	}
}
