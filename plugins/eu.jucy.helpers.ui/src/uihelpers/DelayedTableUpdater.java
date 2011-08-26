package uihelpers;




import helpers.IObservable;
import helpers.Observable.IObserver;
import helpers.StatusObject;
import helpers.StatusObject.ChangeType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;



import org.eclipse.jface.viewers.TableViewer;




public class DelayedTableUpdater<V> implements IObserver<StatusObject>{
	
	private final TableViewer viewer;
	
	private final Object synchUser = new Object();
	
	private final Set<V> add = new HashSet<V>();
	private final Set<V> update = new HashSet<V>();
	private final Set<V> remove = new HashSet<V>();
	
	private final int delay;
	
	private List<DUEntry<V>> delayed = new ArrayList<DUEntry<V>>();
	
	public DelayedTableUpdater(TableViewer viewer) {
		this(viewer,500);
	}
	
	public DelayedTableUpdater(TableViewer viewer,int millisecDelay) {
		this.viewer = viewer;
		this.delay = millisecDelay;
	}
	

	
	
	
	
	private boolean isPresent(V v) {
		return add.contains(v)|| update.contains(v)|| remove.contains(v);
	}
	
	public void put(ChangeType ct,V v) {
		switch(ct) {
		case ADDED: 	add(v); 		break;
		case CHANGED:	change(v); 	break;
		case REMOVED:	remove(v); 	break;
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void update(IObservable<StatusObject> o, StatusObject arg) {
		put(arg.getType(),(V)arg.getValue());
	}

	public void add(V v) {
		synchronized(synchUser) {
			if (isPresent(v)) {
				delayed.add(new DUEntry<V>(v, ChangeType.ADDED));
			} else {
				add.add(v);
			}
		}
		ensureUpdate();
	}
	
	public void change(V v) {
		synchronized(synchUser) {
			if (add.contains(v) || remove.contains(v)) {
				delayed.add(new DUEntry<V>(v, ChangeType.CHANGED));
			} else {
				update.add(v);
			}
		}
		ensureUpdate();
	}
	
	public void remove(V v) {
		synchronized(synchUser) {
			if (isPresent(v)) {
				delayed.add(new DUEntry<V>(v, ChangeType.REMOVED));
			} else {
				remove.add(v);
			}
		}
		ensureUpdate();
	}
	
	private void ensureUpdate() {
		new SUIJob(viewer.getControl()) { //"bulkupdate"
			public void run() {
				synchronized(synchUser) {
					while (!add.isEmpty() || !update.isEmpty() || !remove.isEmpty()) {

						if (!add.isEmpty()) {
							viewer.add(add.toArray());
							add.clear();
						}
						if (!update.isEmpty()) {
							Object[] updateArray = update.toArray();
						//	viewer.update(update.toArray(), null);
							viewer.remove(updateArray);
							viewer.add(updateArray);
							update.clear();
						}
						if (!remove.isEmpty()) {
							viewer.remove(remove.toArray());
							remove.clear();
						}
						if (!delayed.isEmpty()) {
							List<DUEntry<V>> dlaLocal = delayed;
							delayed = new ArrayList<DUEntry<V>>();
							for (DUEntry<V> de: dlaLocal) {
								put(de.ct,de.v);
							}
						}
					}
				}
				updateDone();

			}

		}.scheduleIfNotRunning(delay,this);

	}
	
	/**
	 * discards all running add/update/delete ops
	 */
	public void clear() {
		synchronized(synchUser) {
			add.clear();
			update.clear();
			remove.clear();
		}
	}
	
	/**
	 * method for overwriting.. called by UI thread..
	 */
	protected void updateDone() {}
		
		
	private static class DUEntry<V> {
		private final V v;
		private final ChangeType ct;
		
		public DUEntry(V v, ChangeType ct) {
			super();
			this.v = v;
			this.ct = ct;
		}
		
		
	}
	
	
}
