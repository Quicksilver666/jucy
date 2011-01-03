package eu.jucy.hashengine;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

import org.eclipse.core.runtime.IProgressMonitor;

import uc.crypto.HashValue;
import uc.crypto.InterleaveHashes;

public interface IHasher {

	
	/**
	 * 
	 * @param interleaves
	 * @param monitor - one job should equal to 64KiB of hashed data..
	 * @return
	 */
	HashValue hash(InterleaveHashes interleaves);
	
	InterleaveHashes hash(ReadableByteChannel chan,long size, IProgressMonitor monitor) throws IOException;
}
