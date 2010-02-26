package eu.jucy.testfragment;

import java.io.File;

import uc.crypto.HashValue;
import uc.crypto.IBlock;
import uc.crypto.IHashEngine;
import uc.crypto.InterleaveHashes;

public class FakeHashEngine implements IHashEngine {

	public void checkBlock(IBlock block, VerifyListener checkListener) {
		throw new IllegalStateException("Called method in FakeHashEngine");
	}

	public void clearFileJobs() {
		throw new IllegalStateException("Called method in FakeHashEngine");
	}

	public void hashFile(File f, IHashedFileListener listener) {
		throw new IllegalStateException("Called method in FakeHashEngine");
	}

	public void init() {
		throw new IllegalStateException("Called method in FakeHashEngine");
	}

	
	public void stop() {
		throw new IllegalStateException("Called method in FakeHashEngine");
	}

	public void registerHashedListener(IHashedListener listener) {
		throw new IllegalStateException("Called method in FakeHashEngine");
	}

	public void unregisterHashedListener(IHashedListener listener) {
		throw new IllegalStateException("Called method in FakeHashEngine");
	}

	public boolean verifyInterleaves(InterleaveHashes hashes, HashValue root) {
		throw new IllegalStateException("Called method in FakeHashEngine");
	}

}
