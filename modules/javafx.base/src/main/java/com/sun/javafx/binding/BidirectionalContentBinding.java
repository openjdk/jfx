/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.binding;

import com.sun.javafx.beans.BeanErrors;
import javafx.beans.WeakListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyMapProperty;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.ReadOnlySetProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Set;

import static com.sun.javafx.beans.BeanErrors.*;

public abstract class BidirectionalContentBinding implements WeakListener {

    /**
     * Exceptions thrown here will be surfaced by {@link Bindings#bindContentBidirectional(ObservableList, ObservableList)}
     * and by {@link ReadOnlyListProperty#bindContentBidirectional(ObservableList)} (set/map similarly).
     * In the latter case, the 'target' argument is the 'this' pointer of the ReadOnlyListProperty class.
     * If 'this' is content-bound, we throw an IllegalStateException instead of an IllegalArgumentException,
     * because from the perspective of a user of the ReadOnlyListProperty class, 'target' was not specified
     * as an argument.
     *
     * However, when using the Bindings class to set up a bidirectional content binding, we catch the
     * IllegalStateException and re-throw it as an IllegalArgumentException. From the perspective of a user of
     * the Bindings class, 'target' was specified as an argument.
     */
    private static void checkParameters(Object target, Object source, Class<?> collectionType) {
        if (target == null) {
            throw new NullPointerException(BINDING_TARGET_NULL.getMessage());
        }

        if (source == null) {
            throw new NullPointerException(BINDING_SOURCE_NULL.getMessage(target));
        }

        if (isContentBound(target)) {
            throw new IllegalStateException(CONTENT_BIND_CONFLICT_BIDIRECTIONAL.getMessage(target));
        }

        if (isContentBound(source)) {
            throw new IllegalArgumentException(CONTENT_BIND_CONFLICT_BIDIRECTIONAL.getMessage(source));
        }

        Object c1 = unwrapObservableCollection(target, collectionType);
        Object c2 = unwrapObservableCollection(source, collectionType);

        if (c1 != null && c1 == c2) {
            throw new IllegalArgumentException(CANNOT_BIND_COLLECTION_TO_ITSELF.getMessage(c1));
        }
    }

    private static boolean isContentBound(Object c) {
        return c instanceof ReadOnlyListProperty<?> && ((ReadOnlyListProperty<?>)c).isContentBound()
            || c instanceof ReadOnlySetProperty<?> && ((ReadOnlySetProperty<?>)c).isContentBound()
            || c instanceof ReadOnlyMapProperty<?, ?> && ((ReadOnlyMapProperty<?, ?>)c).isContentBound();
    }

    private static Object unwrapObservableCollection(Object property, Class<?> collectionType) {
        while (property != null) {
            if (property instanceof ReadOnlyProperty<?>) {
                property = ((ReadOnlyProperty<?>)property).getValue();
            } else {
                return collectionType.isInstance(property) ? property : null;
            }
        }

        return null;
    }

    public static <E> Object bind(ObservableList<E> target, ObservableList<E> source) {
        checkParameters(target, source, ObservableList.class);
        final ListContentBinding<E> binding = new ListContentBinding<>(target, source);
        target.removeListener(binding);
        source.removeListener(binding);
        target.setAll(source);
        target.addListener(binding);
        source.addListener(binding);
        return binding;
    }

    public static <E> Object bind(ObservableSet<E> target, ObservableSet<E> source) {
        checkParameters(target, source, ObservableSet.class);
        final SetContentBinding<E> binding = new SetContentBinding<>(target, source);
        target.removeListener(binding);
        source.removeListener(binding);
        target.clear();
        target.addAll(source);
        target.addListener(binding);
        source.addListener(binding);
        return binding;
    }

    public static <K, V> Object bind(ObservableMap<K, V> target, ObservableMap<K, V> source) {
        checkParameters(target, source, ObservableMap.class);
        final MapContentBinding<K, V> binding = new MapContentBinding<>(target, source);
        target.removeListener(binding);
        source.removeListener(binding);
        target.clear();
        target.putAll(source);
        target.addListener(binding);
        source.addListener(binding);
        return binding;
    }

    public static <E> void unbind(ObservableList<E> target, ObservableList<E> source) {
        checkParameters(target, source, ObservableList.class);
        final ListContentBinding<E> binding = new ListContentBinding<>(target, source);
        target.removeListener(binding);
        source.removeListener(binding);
    }

    public static <E> void unbind(ObservableSet<E> target, ObservableSet<E> source) {
        checkParameters(target, source, ObservableSet.class);
        final SetContentBinding<E> binding = new SetContentBinding<>(target, source);
        target.removeListener(binding);
        source.removeListener(binding);
    }

    public static <K, V> void unbind(ObservableMap<K, V> target, ObservableMap<K, V> source) {
        checkParameters(target, source, ObservableMap.class);
        final MapContentBinding<K, V> binding = new MapContentBinding<>(target, source);
        target.removeListener(binding);
        source.removeListener(binding);
    }

    private static class ListContentBinding<E>
            extends BidirectionalContentBinding implements ListChangeListener<E> {

        private final WeakReference<ObservableList<E>> propertyRef1;
        private final WeakReference<ObservableList<E>> propertyRef2;

        private boolean updating = false;


        public ListContentBinding(ObservableList<E> list1, ObservableList<E> list2) {
            propertyRef1 = new WeakReference<ObservableList<E>>(list1);
            propertyRef2 = new WeakReference<ObservableList<E>>(list2);
        }

        @Override
        public void onChanged(Change<? extends E> change) {
            if (!updating) {
                final ObservableList<E> list1 = propertyRef1.get();
                final ObservableList<E> list2 = propertyRef2.get();
                if ((list1 == null) || (list2 == null)) {
                    if (list1 != null) {
                        list1.removeListener(this);
                    }
                    if (list2 != null) {
                        list2.removeListener(this);
                    }
                } else {
                    try {
                        updating = true;
                        final ObservableList<E> dest = (list1 ==  change.getList())? list2 : list1;
                        while (change.next()) {
                            if (change.wasPermutated()) {
                                dest.remove(change.getFrom(), change.getTo());
                                dest.addAll(change.getFrom(), change.getList().subList(change.getFrom(), change.getTo()));
                            } else {
                                if (change.wasRemoved()) {
                                    dest.remove(change.getFrom(), change.getFrom() + change.getRemovedSize());
                                }
                                if (change.wasAdded()) {
                                    dest.addAll(change.getFrom(), change.getAddedSubList());
                                }
                            }
                        }
                    } finally {
                        updating = false;
                    }
                }
            }
        }

        @Override
        public boolean wasGarbageCollected() {
            return (propertyRef1.get() == null) || (propertyRef2.get() == null);
        }

        @Override
        public int hashCode() {
            final ObservableList<E> list1 = propertyRef1.get();
            final ObservableList<E> list2 = propertyRef2.get();
            final int hc1 = (list1 == null)? 0 : list1.hashCode();
            final int hc2 = (list2 == null)? 0 : list2.hashCode();
            return hc1 * hc2;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            final Object propertyA1 = propertyRef1.get();
            final Object propertyA2 = propertyRef2.get();
            if ((propertyA1 == null) || (propertyA2 == null)) {
                return false;
            }

            if (obj instanceof ListContentBinding) {
                final ListContentBinding otherBinding = (ListContentBinding) obj;
                final Object propertyB1 = otherBinding.propertyRef1.get();
                final Object propertyB2 = otherBinding.propertyRef2.get();
                if ((propertyB1 == null) || (propertyB2 == null)) {
                    return false;
                }

                if ((propertyA1 == propertyB1) && (propertyA2 == propertyB2)) {
                    return true;
                }
                if ((propertyA1 == propertyB2) && (propertyA2 == propertyB1)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static class SetContentBinding<E>
            extends BidirectionalContentBinding implements SetChangeListener<E> {

        private final WeakReference<ObservableSet<E>> propertyRef1;
        private final WeakReference<ObservableSet<E>> propertyRef2;

        private boolean updating = false;


        public SetContentBinding(ObservableSet<E> list1, ObservableSet<E> list2) {
            propertyRef1 = new WeakReference<ObservableSet<E>>(list1);
            propertyRef2 = new WeakReference<ObservableSet<E>>(list2);
        }

        @Override
        public void onChanged(Change<? extends E> change) {
            if (!updating) {
                final ObservableSet<E> set1 = propertyRef1.get();
                final ObservableSet<E> set2 = propertyRef2.get();
                if ((set1 == null) || (set2 == null)) {
                    if (set1 != null) {
                        set1.removeListener(this);
                    }
                    if (set2 != null) {
                        set2.removeListener(this);
                    }
                } else {
                    try {
                        updating = true;
                        final Set<E> dest = (set1 == change.getSet())? set2 : set1;
                        if (change.wasRemoved()) {
                            dest.remove(change.getElementRemoved());
                        } else {
                            dest.add(change.getElementAdded());
                        }
                    } finally {
                        updating = false;
                    }
                }
            }
        }

        @Override
        public boolean wasGarbageCollected() {
            return (propertyRef1.get() == null) || (propertyRef2.get() == null);
        }

        @Override
        public int hashCode() {
            final ObservableSet<E> set1 = propertyRef1.get();
            final ObservableSet<E> set2 = propertyRef2.get();
            final int hc1 = (set1 == null)? 0 : set1.hashCode();
            final int hc2 = (set2 == null)? 0 : set2.hashCode();
            return hc1 * hc2;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            final Object propertyA1 = propertyRef1.get();
            final Object propertyA2 = propertyRef2.get();
            if ((propertyA1 == null) || (propertyA2 == null)) {
                return false;
            }

            if (obj instanceof SetContentBinding) {
                final SetContentBinding otherBinding = (SetContentBinding) obj;
                final Object propertyB1 = otherBinding.propertyRef1.get();
                final Object propertyB2 = otherBinding.propertyRef2.get();
                if ((propertyB1 == null) || (propertyB2 == null)) {
                    return false;
                }

                if ((propertyA1 == propertyB1) && (propertyA2 == propertyB2)) {
                    return true;
                }
                if ((propertyA1 == propertyB2) && (propertyA2 == propertyB1)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static class MapContentBinding<K, V>
            extends BidirectionalContentBinding implements MapChangeListener<K, V> {

        private final WeakReference<ObservableMap<K, V>> propertyRef1;
        private final WeakReference<ObservableMap<K, V>> propertyRef2;

        private boolean updating = false;


        public MapContentBinding(ObservableMap<K, V> list1, ObservableMap<K, V> list2) {
            propertyRef1 = new WeakReference<ObservableMap<K, V>>(list1);
            propertyRef2 = new WeakReference<ObservableMap<K, V>>(list2);
        }

        @Override
        public void onChanged(Change<? extends K, ? extends V> change) {
            if (!updating) {
                final ObservableMap<K, V> map1 = propertyRef1.get();
                final ObservableMap<K, V> map2 = propertyRef2.get();
                if ((map1 == null) || (map2 == null)) {
                    if (map1 != null) {
                        map1.removeListener(this);
                    }
                    if (map2 != null) {
                        map2.removeListener(this);
                    }
                } else {
                    try {
                        updating = true;
                        final Map<K, V> dest = (map1 == change.getMap())? map2 : map1;
                        if (change.wasRemoved()) {
                            dest.remove(change.getKey());
                        }
                        if (change.wasAdded()) {
                            dest.put(change.getKey(), change.getValueAdded());
                        }
                    } finally {
                        updating = false;
                    }
                }
            }
        }

        @Override
        public boolean wasGarbageCollected() {
            return (propertyRef1.get() == null) || (propertyRef2.get() == null);
        }

        @Override
        public int hashCode() {
            final ObservableMap<K, V> map1 = propertyRef1.get();
            final ObservableMap<K, V> map2 = propertyRef2.get();
            final int hc1 = (map1 == null)? 0 : map1.hashCode();
            final int hc2 = (map2 == null)? 0 : map2.hashCode();
            return hc1 * hc2;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            final Object propertyA1 = propertyRef1.get();
            final Object propertyA2 = propertyRef2.get();
            if ((propertyA1 == null) || (propertyA2 == null)) {
                return false;
            }

            if (obj instanceof MapContentBinding) {
                final MapContentBinding otherBinding = (MapContentBinding) obj;
                final Object propertyB1 = otherBinding.propertyRef1.get();
                final Object propertyB2 = otherBinding.propertyRef2.get();
                if ((propertyB1 == null) || (propertyB2 == null)) {
                    return false;
                }

                if ((propertyA1 == propertyB1) && (propertyA2 == propertyB2)) {
                    return true;
                }
                if ((propertyA1 == propertyB2) && (propertyA2 == propertyB1)) {
                    return true;
                }
            }
            return false;
        }
    }

}
