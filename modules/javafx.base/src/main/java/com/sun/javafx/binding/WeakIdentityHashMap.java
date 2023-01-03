package com.sun.javafx.binding;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

public class WeakIdentityHashMap<K, V> {
    private final Map<WeakReference<K>, V> map = new HashMap<>();
    private final ReferenceQueue<K> referenceQueue = new ReferenceQueue<>();

    public V get(Object key) {
        cleanQueued();

        return map.get(new IdentityWeakReference<>(key));
    }

    public V put(K key, V value) {
        cleanQueued();

        return map.put(new IdentityWeakReference<>(key, referenceQueue), value);
    }

    public V remove(K key) {
        cleanQueued();

        return map.remove(new IdentityWeakReference<>(key));
    }

    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction);
        V value = get(key);

        if (value != null) {
            return value;
        }

        V newValue = mappingFunction.apply(key);

        if (newValue != null) {
            put(key, newValue);
        }

        return newValue;
    }

    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);

        V oldValue = get(key);

        if (oldValue == null) {
            return null;
        }

        V newValue = remappingFunction.apply(key, oldValue);

        if (newValue != null) {
            put(key, newValue);
        }
        else {
            remove(key);
        }

        return newValue;
    }

    private void cleanQueued() {
        Reference<? extends K> reference;

        while ((reference = referenceQueue.poll()) != null) {
            map.remove(reference);
        }
    }

    private static class IdentityWeakReference<T> extends WeakReference<T> {
        private final int hashCode;

        IdentityWeakReference(T obj) {
            this(obj, null);
        }

        IdentityWeakReference(T obj, ReferenceQueue<T> queue) {
            super(Objects.requireNonNull(obj, "obj"), queue);

            this.hashCode = System.identityHashCode(obj);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof IdentityWeakReference<?>)) {
                return false;
            }

            @SuppressWarnings("unchecked")
            IdentityWeakReference<T> other = (IdentityWeakReference<T>) obj;
            T referent = get();

            return referent != null && other.refersTo(referent);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }
}
