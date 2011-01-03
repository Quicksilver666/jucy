package uc.files.downloadqueue;


import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.HashMap;
import java.util.Map;

import logger.LoggerFactory;

import org.apache.log4j.Logger;

import uc.crypto.HashValue;
import uc.crypto.IBlock;
import uc.crypto.IHashEngine.VerifyListener;


/**
 * the smallest unit of verification in a file..
 * 
 * size is determined by the amount of interleave hashes we have..
 * each interleave represents one Block..
 * 
 * @author Quicksilver
 *
 */
public class Block implements IBlock {
	
	
	private static final Logger logger = LoggerFactory.make();
	
	public static class FileChannelManager {
		
		
		
		/**
		 * holds FileChannels for reuse
		 */
		private  Map<File,FileChannel> fcmap = new HashMap<File,FileChannel>();
		
		/**
		 * counts how often a FileChannel is in use..
		 */
		private  Map<File,Integer> fcCounter = new HashMap<File,Integer>();
		
		private synchronized  FileChannel getFC(File f) throws IOException {
			FileChannel fc = fcmap.get(f);
			if (fc == null || !fc.isOpen()) {
				fc = new RandomAccessFile(f,"rw").getChannel();
				fcmap.put(f, fc);
			}
			changeFCCounter(f,1);
					
			return fc;
		}
		
		private int changeFCCounter(File f,int howmuch) {
			Integer count = fcCounter.get(f);
			if (count == null) {
				count = 0;
			}
			count += howmuch;
			fcCounter.put(f, count);
			return count;
		}
		
		private synchronized  void returnFC(File f) throws IOException {
			int count = changeFCCounter(f,-1);
			if (count <= 0) {
				if (count < 0) {
					throw new IllegalStateException("counter can't be smaller than zero");
				}
				
				FileChannel fc = fcmap.remove(f);
				fc.force(true);
				fc.close();
			}
		}
		
	}
	

	
	/**
	 * where this block belongs to
	 */
	private final FileDQE dqe;
	
	/**
	 * the number in the file..
	 * position in the file starting from zero
	 */
	private final int blocknumber;

	/**
	 * how many bytes this Block has.. so it doesn't has to be calculated on each 
	 * request.. 
	 * unknown set from start..
	 */
	private long length = -1;
	
	private volatile BlockState state = BlockState.UNVERIFIED;
	
	private final HashValue hashOfBlock;

	
	public Block(FileDQE dqe,int blocknumber,HashValue hashOfBlock,boolean finished) {
		this.hashOfBlock = hashOfBlock;
		this.blocknumber = blocknumber;
		this.dqe = dqe;	
		if (hashOfBlock == null) {
			logger.warn("Block not correctly loaded: "+dqe.toString()+ "  BN: "+blocknumber,new Throwable());
		}
		//check for the existence and set empty  if not existing.. else hash it

		if (dqe.getTempPath().isFile() && dqe.getTempPath().length() >= getStartPosition() + getLength()) {
			if (finished) {
				state = BlockState.FINISHED;
			} else {
				verify();
			}	
		} else {
			state = BlockState.EMPTY;
		}
		
	}
	
	
	public Block(FileDQE dqe,int blocknumber,HashValue hashOfBlock) {
		this(dqe,blocknumber,hashOfBlock,false);
	}
	
	
	
	public HashValue getHashOfBlock() {
		return hashOfBlock;
	}
	
	
	private long getStartPosition() {
		return blocknumber * dqe.getBlocksize();
	}
	
	public long getLength() {
		if (length == -1 ) {
			if (getStartPosition()+ dqe.getBlocksize() > dqe.getSize()) {
				length =  dqe.getSize() - getStartPosition();
			} else {
				length =  dqe.getBlocksize();
			}
		} 
		return length;
	}
	
	public ReadableByteChannel getReadChannel() throws IOException {
		if (state == BlockState.EMPTY || state == BlockState.WRITEINPROGRESS ) {
			throw new IllegalStateException("the block is not in a state that allows reading from it");
		}
		
	
		
		final FileChannel fc = dqe.getFileChannelManager().getFC(dqe.getTempPath()); 
		
		return new ReadableByteChannel() {
			private long currentpos = getStartPosition();
			private long bytesleft = getLength();
			private boolean open = true;
			
			
			public void close() throws IOException {
				if (open) {
					dqe.getFileChannelManager().returnFC(dqe.getTempPath());
					open = false;
				}
			}

			
			public boolean isOpen() {
				return open;
			}

			
			public int read(ByteBuffer dst) throws IOException {
				if (bytesleft <= 0) {
					if (bytesleft < 0) {
						throw new IllegalStateException("read too much from the block");
					}
					return -1;
					
				}
				if (dst.remaining() > bytesleft) {
					dst.limit((int)(dst.position()+bytesleft));
				}

				int read = fc.read(dst,currentpos);
				if (read == -1) {
				
				//	return read(dst);
				
					return -1;
					
				} else {
					bytesleft  -= read;
					currentpos += read;
					return read;
				}
			}
			
		};
		
	}
	
	public boolean isWritable() {
		return state == BlockState.EMPTY;
	}
	
	public boolean isFinished() {
		return state == BlockState.FINISHED;
	}
	
	private void setState(BlockState bs) {
		state = bs;
		dqe.internal_NotifyBlockChanged(this,bs);
	}
	
	/**
	 * returns a writable channel for this block.. that will write the bytes 
	 * to the specified position in the file..
	 * 
	 * when finished writing 0 will be returned..
	 * from the write operation
	 * 
	 * when ever the channel is closed the Block will be verified..
	 * 
	 * @return a channel 
	 * @throws IOException
	 */
	public WritableByteChannel getWriteChannel() throws IOException {
		if (state != BlockState.EMPTY) {
			throw new IllegalStateException("current state: "+state+" this is not legal");
		}
		final FileChannel fc =  dqe.getFileChannelManager().getFC(dqe.getTempPath());  
		setState(BlockState.WRITEINPROGRESS);
		
		return new WritableByteChannel() {
			private long currentpos = getStartPosition();
			private long bytesleft = getLength();
			boolean open = true;
			
			
			public void close() throws IOException {
				if (open) {
					dqe.getFileChannelManager().returnFC(dqe.getTempPath());	
					open = false;
					if (bytesleft == 0) {
						verify();
					} else {
						setState(BlockState.EMPTY);
					}
				}
			}

			
			public boolean isOpen() {
				return open;
			}

			
			public int write(ByteBuffer src) throws IOException {
				if (bytesleft == 0) {
					return 0;
				}
				
				boolean bufferduplicatedone = false; //signal if the following operation was done
				//this is needed to restrict the possible amount of written bytes to the remaining bytes of the block
				if (src.remaining() > bytesleft) {
					ByteBuffer old = src;
					src = src.duplicate();
					
					//advance position of the old buffer to a value that means all possible byte where written
					old.position((int)(old.position()+bytesleft)); 
					//the copied buffer be set so only the really needed amount of bytes are written
					src.limit((int)(src.position()+bytesleft));
					bufferduplicatedone = true;
				}
			
				//now write all possible bytes
				int written = fc.write(src, currentpos);
				
				if (written == -1) {
					return -1;
				} else {
					currentpos += written;
					bytesleft  -= written;
					//just a check if this buffer copying did work..
					if (bufferduplicatedone && bytesleft != 0) {
						throw new IOException("buffer duplicate operation did not work Block.class is not working properly"); 
					}
					if (bytesleft < 0) {
						throw new IOException("Too many bytes written");
					}
					return written;
				}
			}
		};
		
	}
	
	/**
	 * asynchronous verification with the help of the 
	 * HashEngine..
	 */
	private void verify() { 
		setState(BlockState.UNVERIFIED);
		dqe.getDCC().getHashEngine().checkBlock(this, new VerifyListener() {
			public void checked(boolean verified) {
				setState(verified?BlockState.FINISHED:BlockState.EMPTY );
			}
		});
	}
	
	
	public static enum BlockState {
		EMPTY,WRITEINPROGRESS,UNVERIFIED,FINISHED;
	}


	public int getBlocknumber() {
		return blocknumber;
	}
	

	/**
	 * calculates the maximum size of an interval to write
	 * started by this block
	 * 
	 * @return the IntervalSize from this to the first block that can't be written to
	 */
	public long getIntervalLength() {
		//Block next = dqe.getBlock(blocknumber+1);
		
		int count = 0;
		long total = 0;
		while (true) {
			Block next = dqe.getBlock(blocknumber+count);
			if (next != null && next.isWritable()) {
				total += next.getLength();
				count++;
			} else {
				return total;
			}
		}
		
	}
	
	public int getIntervalLengthInBlocks() {
		//try {
		int count = 1 ;
		while (true) {
			Block next = dqe.getBlock(blocknumber+count);
			if (next != null && next.isWritable()) {
				count++;
			} else {
				return count;
			}
		}
	}


	public BlockState getState() {
		return state;
	}
	
	public String toString() {
		return "BlockNr: "+ blocknumber ;
	}

}
