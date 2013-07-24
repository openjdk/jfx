/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package javafx.collections;

/**
 * Interface that receives notifications of changes to an ObservableMap.
 * @param <K> the key element type
 * @param <V> the value element type
 * @since JavaFX 2.0
 */
@FunctionalInterface
public interface MapChangeListener<K, V> {

    /**
     * An elementary change done to an ObservableMap.
     * Change contains information about a put or remove operation.
     * Note that put operation might remove an element if there was
     * already a value associated with the same key. In this case
     * wasAdded() and wasRemoved() will both return true.
     *
     * @param <K> key type
     * @param <V> value type
     * @since JavaFX 2.0
     */
    public static abstract class Change<K, V> {

        private final ObservableMap<K,V> map;

        /**
         * Constructs a change associated with a map.
         * @param map the source of the change
         */
        public Change(ObservableMap<K, V> map) {
            this.map = map;
        }

        /**
         * An observable map that is associated with the change.
         * @return the source map
         */
        public ObservableMap<K, V> getMap() {
            return map;
        }

        /**
         * If this change is a result of add operation.
         * @return true if a new value (or key-value) entry was added to the map
         */
        public abstract boolean wasAdded();

        /**
         * If this change is a result of removal operation.
         * Note that an element might be removed even as a result of put operation.
         * @return true if an old value (or key-value) entry was removed from the map
         */
        public abstract boolean wasRemoved();

        /**
         * A key associated with the change.
         * If the change is a remove change, the key no longer exist in a map.
         * Otherwise, the key got set to a new value.
         * @return the key that changed
         */
        public abstract K getKey();

        /**
         * Get the new value of the key. Return null if this is a removal.
         * @return the value that is now associated with the key
         */
        public abstract V getValueAdded();

        /**
         * Get the old value of the key. This is null if and only if the value was
         * added to the key that was not previously in the map.
         * @return the value previously associated with the key
         */
        public abstract V getValueRemoved();

    }

    /**
     * Called after a change has been made to an ObservableMap.
     * This method is called on every elementary change (put/remove) once.
     * This means, complex changes like keySet().removeAll(Collection) or clear()
     * may result in more than one call of onChanged method.
     *
     * @param change the change that was made
     */
    void onChanged(Change<? extends K,? extends V> change);
}
