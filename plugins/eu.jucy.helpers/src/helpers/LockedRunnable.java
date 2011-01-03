package helpers;

import java.util.concurrent.locks.Lock;

public abstract class LockedRunnable implements Runnable {

	private final Lock lock;
	

	public LockedRunnable(Lock lock) {
		super();
		this.lock = lock;
	}

	
	@Override
	public final void run() {
		lock.lock();
		try {
			lockedRun();
		} finally {
			lock.unlock();
		}

	}

	protected abstract void lockedRun();
	
}
