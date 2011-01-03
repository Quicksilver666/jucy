package uihelpers;

import helpers.StatusObject.ChangeType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;


import org.eclipse.jface.viewers.TreeViewer;

public class DelayedTreeUpdater<V> {


	private final TreeViewer viewer;
	
	
	private final Object synchUpdate = new Object();
	
	private Set<DTEntry<V>> add = new HashSet<DTEntry<V>>();
	private Map<Object,Set<V>> addInverse = new HashMap<Object,Set<V>>();
	
	
	private Set<DTEntry<V>> update = new HashSet<DTEntry<V>>();
	private Set<DTEntry<V>> remove = new HashSet<DTEntry<V>>();
	
	private List<DTEntry<V>> delayed = new ArrayList<DTEntry<V>>();
	private final int delay;
	
	
	public DelayedTreeUpdater(TreeViewer viewer) {
		this(viewer,500);
	}
	
	public DelayedTreeUpdater(TreeViewer viewer,int millisecDelay) {
		this.viewer = viewer;
		this.delay = millisecDelay;
	}
	
	
	public void add(V v,Object parent) {
		DTEntry<V> dt = new DTEntry<V>(parent, v,ChangeType.ADDED);
		synchronized(synchUpdate) {
			if (remove.contains(dt) || update.contains(dt)) {
				delayed.add(dt);
			} else {
				add.add(dt);
				addToMapSet(addInverse, parent, v);
			}
		/*	if (remove.remove(dt)) {
				update.add(dt);
			} else {
				add.add(dt);
				addToMapSet(addInverse, parent, v);
			} */
		}
		ensureUpdate();
	}
	
	public void change(V v,Object parent) {
		DTEntry<V> dt = new DTEntry<V>(parent, v,ChangeType.CHANGED);
		synchronized(synchUpdate) {
			if (add.contains(dt) || remove.contains(dt)) {
				delayed.add(dt);
			} else {
				update.add(dt);
			}
		/*	if (!add.contains(dt)) {
				update.add(dt);
			} */
		}
		ensureUpdate();
	}
	
	public void remove(V v,Object parent) {
		DTEntry<V> dt = new DTEntry<V>(parent, v,ChangeType.REMOVED);
		synchronized(synchUpdate) {
			if (add.contains(dt) || update.contains(dt)) {
				delayed.add(dt);
			} else {
		//	if (!add.remove(dt)) {
				removeMapSet(addInverse, parent, v);
				remove.add(dt);
			}
		}
		ensureUpdate();
	}
	
	public void put(ChangeType ct,V value,Object parent) {
		switch(ct) {
		case ADDED:
			add(value, parent);
			break;
		case CHANGED:
			change(value, parent);
			break;
		case REMOVED:
			remove(value, parent);
			break;

		}
	}
	
	private void ensureUpdate() {

		new SUIJob(viewer.getControl()) { //"bulkupdate"
			public void run() {
				synchronized(synchUpdate) {
					boolean done = false;
					while (!done) {
						if (!add.isEmpty()) {
							for (Entry<Object,Set<V>> e:addInverse.entrySet()) {
								viewer.add(e.getKey(), e.getValue().toArray());
							}
							add.clear();
							addInverse.clear();
						}
						if (!update.isEmpty()) {
							viewer.update(toArray(update), null); //.toArray()
							update.clear();
						}
						if (!remove.isEmpty()) {
							viewer.remove(toArray(remove) ); //.toArray()
							remove.clear();
						}
						done = delayed.isEmpty();
						if (!done) {
							List<DTEntry<V>> delayCopy = delayed;
							delayed = new ArrayList<DTEntry<V>>();
							for (DTEntry<V> dt:delayCopy) {
								put(dt.ct,dt.value,dt.parent);
							}
						}
					}

				}
				//viewer.refresh();

				updateDone();

			}

		}.scheduleIfNotRunning(delay,this);

	}
	
	/*private Map<Object,List<V>> inverseAdd() {
		Map<Object,List<V>> inverseMap = new HashMap<Object,List<V>>();
		for (Entry<V,Object> e: add.entrySet()) {
			addToMapList(inverseMap, e.getValue(), e.getKey());
		}
		return inverseMap;
	} */
	
	private static <V> void addToMapSet(Map<Object,Set<V>> map,Object o, V v) {
		Set<V> list = map.get(o);
		if (list == null) {
			list = new HashSet<V>();
			map.put(o, list);
		}
		list.add(v);
	}
	private static <V> void removeMapSet(Map<Object,Set<V>> map,Object o, V v) {
		Set<V> list = map.get(o);
		if (v != null && list != null) {
			list.remove(v);
		}
	}
	
	/**
	 * discards all running add/update/delete ops
	 */
	public void clear() {
		synchronized(synchUpdate) {
			add.clear();
			update.clear();
			remove.clear();
			delayed.clear();
		}
	}
	
	/**
	 * method for overwriting.. called by UI thread..
	 */
	protected void updateDone() {}
	
	
	private static <V> Object[] toArray(Collection<DTEntry<V>> v) {
		Object[] o = new Object[v.size()];
		Iterator<DTEntry<V>> it = v.iterator();
		for (int i = 0; i < o.length && it.hasNext(); i++) {
			o[i] = it.next().value;
		}
		return o;
	}
	
	private static class DTEntry<V> {
		

		
		private final Object parent;
		private final V value;
		private final ChangeType ct;

		public DTEntry(Object parent, V value,ChangeType ct) {
			super();
			this.parent = parent;
			this.value = value;
			this.ct = ct;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((value == null) ? 0 : value.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			DTEntry<?> other = (DTEntry<?>) obj;
			if (value == null) {
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}
		
		
		
	}
	
}
