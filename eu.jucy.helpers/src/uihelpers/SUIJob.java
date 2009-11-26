package uihelpers;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.swt.widgets.Control;
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

	private static Set<Object> synchInProgress = Collections.synchronizedSet(
			new HashSet<Object>());
	
	private volatile boolean cancel = false;
	
	
	private final Runnable exec;
	
	public SUIJob() {
		exec = new Runnable() {
			public void run() {
				if (!cancel) {
					SUIJob.this.run();
				}
			}
		};
	}
	
	public SUIJob(final Control testDisposed) {
		exec = new Runnable() {
			public void run() {
				if (!cancel && !testDisposed.isDisposed()) {
					SUIJob.this.run();
				}
			}
		};
	}
	
	public abstract void run();
	

	public void schedule() {
		Display d = Display.getDefault();
		if (!d.isDisposed()) {
			d.asyncExec(exec);
		}
	}
	
	/**
	 * schedules the job only if not the an instance of the same class is already running..
	 * @param delayMillisecs
	 */
	public void scheduleIfNotRunning(final int delayMillisecs,final Object synch) {
		if (synchInProgress.add(synch)) {
			Display d = Display.getDefault();
			if (!d.isDisposed()) {
				d.asyncExec(new Runnable() {
					public void run() {
						Display d = Display.getDefault();
						d.timerExec(delayMillisecs, new Runnable() {
							public void run() {
								synchInProgress.remove(synch);
								exec.run();
							}
						});
					}
				});
			}
		}
		
	}
	
	/**
	 * 
	 * @param delayMillisecs - delay scheduled in milliseconds.
	 */
	public void schedule(final int delayMillisecs) {
		final Display d = Display.getDefault();
		if (!d.isDisposed()) {
			d.asyncExec(new Runnable() {
				public void run() {
					d.timerExec(delayMillisecs, exec);
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
			d.syncExec(exec);
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
