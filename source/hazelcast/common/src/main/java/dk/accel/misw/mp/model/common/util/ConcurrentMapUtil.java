package dk.accel.misw.mp.model.common.util;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;

public class ConcurrentMapUtil {

	public static interface ItemFactory<K, V> {
		V newInstance(K key);
	}
	
	public static <K, V> V getItemCreateIfAbsent(K key, ConcurrentMap<K, V> map, ItemFactory<K, V> factory) {
		V result = map.get(key);
		if (result == null) {
			result = factory.newInstance(key);
			V raced = map.putIfAbsent(key, result);
			if (raced != null) {
				result = raced;
			}
		}
		return result;
	}
	
	public static <K, V> V getItemCreateIfAbsentWeakMap(K key, ConcurrentMap<K, WeakReference<V>> map, ItemFactory<K, V> factory) {
		WeakReference<V> ref = map.get(key);
		if (ref != null) {
			V result = ref.get();
			if (result != null) {
				return result;
			}
		}
		
		// either ref or the item it references are null
		V result = factory.newInstance(key);
		WeakReference<V> newRef = new WeakReference<V>(result);
		if (ref == null) {
			if (map.putIfAbsent(key, newRef) != null) {
				// somebody raced us - take another round
				return getItemCreateIfAbsentWeakMap(key, map, factory);
			}
		} else {
			if (!map.replace(key, ref, newRef)) {
				// somebody raced us - take another round
				return getItemCreateIfAbsentWeakMap(key, map, factory);
			}
		}
		// if we get down here the result is placed correctly in a ref in the map.
		return result;
	}
	
	/**
	 * Creates a runnable that cleans all dead mappings from a map with
	 * {@link WeakReference} values. Should be scheduled for periodically
	 * running using a {@link ScheduledExecutorService}.
	 */
	public static <K, V> Runnable newWeakMapCleanerTask(final ConcurrentMap<K, WeakReference<V>> map) {
		return ExecutorsUtil.wrap(new Runnable() {
			
			/**
			 * Implementation note: We do this in two steps as the 
			 * Map.EntrySet#iterator()'s remove method does only check for
			 * the same key (i.e. not value) for at least the ConcurrentHashMap implementation.
			 */
			@Override
			public void run() {
				// important that we use a map type that allows null
				Map<K, WeakReference<?>> deadKeyCandidates = new HashMap<K, WeakReference<?>>();

				// find dead candidates
				for (Map.Entry<K, WeakReference<V>> entry : map.entrySet()) {
					WeakReference<?> ref = entry.getValue();
					if ((ref == null) || (ref.get() == null)) {
						deadKeyCandidates.put(entry.getKey(), ref);
					}
				}
				
				// remove dead candidates if still dead (i.e. still mapped to same weakref mapping)
				for (Map.Entry<K, WeakReference<?>> entry : deadKeyCandidates.entrySet()) {
					map.remove(entry.getKey(), entry.getValue());
				}
			}
		});
	}
	
	/**
	 * No instances.
	 */
	private ConcurrentMapUtil() {
		throw new AssertionError();
	}
}
