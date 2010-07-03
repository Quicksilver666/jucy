package helpers;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;



public class SchedulableTask {

	private final Runnable task;
	private final ScheduledExecutorService scheduler;
	
	private final Object synch = new Object();
	private ScheduledFuture<?> runningTask; 
	
	public SchedulableTask(Runnable task,ScheduledExecutorService executor) {
		this.task = task;
		this.scheduler = executor;
	}
	
	/**
	 * 
	 * @param cancel task if running then schedule task if time is positive.. 
	 */
	public void reschedule(int time,TimeUnit unit) {
		synchronized (synch) {
			cancelScheduled();
			
			if (time >= 0) {
				runningTask = scheduler.schedule(task, time, unit);
			}
		}
	}
	
	/**
	 * stop the task if it was scheduled from starting execution
	 */
	public void cancelScheduled() {
		synchronized (synch) {
			if (runningTask != null) {
				runningTask.cancel(false);
				runningTask = null;
			}
		}
	}
	
	/**
	 * 
	 * @return true if this task will be executed eventually
	 */
	public boolean isScheduled() {
		synchronized (synch) {
			return runningTask != null;
		}	
	}
	
	/**
	 * 
	 * @return the Time until the task will execute..
	 * -1 if no task present..
	 */
	public long getDelay(TimeUnit unit) {
		synchronized (synch) {
			if (runningTask == null) {
				return -1;
			} else {
				return runningTask.getDelay(unit);
			}
		}
	}
	
}
