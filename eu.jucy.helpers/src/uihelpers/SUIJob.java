package uihelpers;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;


/**
 * a simple UIJob replacement
 * so UIJobs can be replaced by this 
 * benefit will be that this is no shown in the 
 * job view..
 * 
 * @author Quicksilver
 *
 */
public abstract class SUIJob implements Runnable {

	private static Set<Class<? extends SUIJob>> jobsInProgress = Collections.synchronizedSet(
			new HashSet<Class<? extends SUIJob>>());
	
	private volatile boolean cancel = false;
	
	
	
	public abstract void run();
	

	public void schedule() {
		Display d = Display.getDefault();
		if (!d.isDisposed()) {
			d.asyncExec(this);
		}
	}
	
	/**
	 * schedules the job only if not the an instance of the same class is already running..
	 * @param delayMillisecs
	 */
	public void scheduleIfNotRunning(final int delayMillisecs) {
		synchronized(jobsInProgress) {
			if (!jobsInProgress.contains(getClass())) {
				jobsInProgress.add(getClass());
				final Display d = Display.getDefault();
				if (!d.isDisposed()) {
					d.asyncExec(new Runnable() {
						public void run() {
							d.timerExec(delayMillisecs, new Runnable() {
								public void run() {
									jobsInProgress.remove(SUIJob.this.getClass());
									if (!cancel) {
										SUIJob.this.run();
									}
								}
							});
						}
					});
				}
			}
		}
	}
	
	/**
	 * 
	 * @param delayMillisecs - delay scheduled in miliseconds.
	 */
	public void schedule(final int delayMillisecs) {
		final Display d = Display.getDefault();
		if (!d.isDisposed()) {
			d.asyncExec(new Runnable() {
				public void run() {
					d.timerExec(delayMillisecs, new Runnable() {
						public void run() {
							if (!cancel) {
								SUIJob.this.run();
							}
						}
					});
				}
			});
		}
	}
	
	/**
	 * schedules the job and waits until it is finished.
	 */
	public void scheduleAndJoin() {
		Display d = Display.getDefault();
		if (!d.isDisposed()) {
			d.syncExec(this);
		}
	}
	
	public void cancel() {
		cancel = true;
	}
	

	
	/**
	 * special schedule and join 
	 * that determines if the caller thread is the UI
	 * thread.. based on this
	 * the caller runs itself or waits until ui thread has run it..
	 */
	public void executeNow() {
		if (PlatformUI.getWorkbench().getActiveWorkbenchWindow() == null) {
			scheduleAndJoin();
		} else {
			run();
		}
	}
	
	protected IWorkbenchWindow getWindow() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	}


}
