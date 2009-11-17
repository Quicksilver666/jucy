package uc.crypto;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

/**
 * Represents a smallest verifiable unit of a file
 * 
 * @author Quicksilver
 *
 */
public interface IBlock {

	/**
	 * 
	 * @return a channel for reading of the block
	 * @throws IOException if the underlying file can't be opened..
	 */
	ReadableByteChannel getReadChannel() throws IOException;
	
	/**
	 * nr of bytes in this block
	 * @return
	 */
	long getLength();
	
	/**
	 * 
	 * @return the TTH interleave of this Block..
	 */
	HashValue getHashOfBlock();
}
