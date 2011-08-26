package helpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import java.util.Set;

import helpers.StatusObject.ChangeType;

/**
 * 
 * Wraps around some map.. access to map should be done through methods of the ObservableMap
 *
 * @param <K>
 * @param <V>
 */
public class ObservableMap<K,V> extends Observable<StatusObject> implements IObservableCollection, Map<K,V> {

	private final Map<K,V> backingMap;
	
	public ObservableMap(Map<K, V> backingMap) {
		super();
		this.backingMap = backingMap;
	}

	public void notifyChanged(V v) {
		notifyObservers(new StatusObject(v, ChangeType.CHANGED));
	}
	
	public int size() {
		return backingMap.size();
	}


	public boolean isEmpty() {
		return backingMap.isEmpty();
	}


	public boolean containsKey(Object key) {
		return backingMap.containsKey(key);
	}


	public boolean containsValue(Object value) {
		return backingMap.containsValue(value);
	}


	public V get(Object key) {
		return backingMap.get(key);
	}


	public V put(K key, V value) {
		V removed = backingMap.put(key, value);
		if (removed != null) {
			notifyObservers(new StatusObject(removed, ChangeType.REMOVED));
		}
		notifyObservers(new StatusObject(value, ChangeType.ADDED));
		return removed;
	}


	public V remove(Object key) {
		V removed = backingMap.remove(key);
		if (removed != null) {
			notifyObservers(new StatusObject(removed, ChangeType.REMOVED));
		}
		return removed;
	}


	public void putAll(Map<? extends K, ? extends V> m) {
		for (Entry<? extends K,? extends V> e:m.entrySet()) {
			put(e.getKey(),e.getValue());
		}
	}


	public void clear() {
		for (K k:new ArrayList<K>(backingMap.keySet())) {
			remove(k);
		}
	}


	public Set<K> keySet() {
		return Collections.unmodifiableSet(backingMap.keySet());
	}


	public Collection<V> values() {
		return Collections.unmodifiableCollection(backingMap.values());
	}


	public Set<Entry<K, V>> entrySet() {
		return Collections.unmodifiableSet(backingMap.entrySet());
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((backingMap == null) ? 0 : backingMap.hashCode());
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
		@SuppressWarnings("rawtypes")
		ObservableMap other = (ObservableMap) obj;
		if (backingMap == null) {
			if (other.backingMap != null)
				return false;
		} else if (!backingMap.equals(other.backingMap))
			return false;
		return true;
	}



	
	

}
