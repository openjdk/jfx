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

import javafx.beans.WeakListener;
import javafx.collections.*;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class ContentBinding implements WeakListener {

    private static void checkParameters(Object property1, Object property2) {
        if ((property1 == null) || (property2 == null)) {
            throw new NullPointerException("Both parameters must be specified.");
        }
        if (property1 == property2) {
            throw new IllegalArgumentException("Cannot bind object to itself");
        }
    }

    public static <E> Object bind(List<E> list1, ObservableList<? extends E> list2) {
        unbind(list1, list2);
        final ListContentBinding<E> contentBinding = new ListContentBinding<>(list1, list2);
        if (list1 instanceof ObservableList) {
            ((ObservableList<E>)list1).setAll(list2);
        } else {
            list1.clear();
            list1.addAll(list2);
        }
        list2.removeListener(contentBinding);
        list2.addListener(contentBinding);
        if (list1 instanceof ObservableList<?>) {
            ((ObservableList<E>)list1).addListener(contentBinding);
        }
        return contentBinding;
    }

    public static <E> Object bind(Set<E> set1, ObservableSet<? extends E> set2) {
        unbind(set1, set2);
        final SetContentBinding<E> contentBinding = new SetContentBinding<>(set1, set2);
        set1.clear();
        set1.addAll(set2);
        set2.removeListener(contentBinding);
        set2.addListener(contentBinding);
        if (set1 instanceof ObservableSet<?>) {
            ((ObservableSet<E>)set1).addListener(contentBinding);
        }
        return contentBinding;
    }

    public static <K, V> Object bind(Map<K, V> map1, ObservableMap<? extends K, ? extends V> map2) {
        unbind(map1, map2);
        final MapContentBinding<K, V> contentBinding = new MapContentBinding<>(map1, map2);
        map1.clear();
        map1.putAll(map2);
        map2.removeListener(contentBinding);
        map2.addListener(contentBinding);
        if (map1 instanceof ObservableMap<?, ?>) {
            ((ObservableMap<? extends K, ? extends V>)map1).addListener(contentBinding);
        }
        return contentBinding;
    }

    public static <E> void unbind(List<E> obj1, ObservableList<? extends E> obj2) {
        checkParameters(obj1, obj2);
        var binding = new ListContentBinding<>(obj1, obj2);
        obj2.removeListener(binding);

        if (obj1 instanceof ObservableList<?>) {
            ((ObservableList<? extends E>)obj1).removeListener(binding);
        }
    }

    public static <E> void unbind(Set<E> obj1, ObservableSet<? extends E> obj2) {
        checkParameters(obj1, obj2);
        var binding = new SetContentBinding<>(obj1, obj2);
        obj2.removeListener(binding);

        if (obj1 instanceof ObservableSet<?>) {
            ((ObservableSet<? extends E>)obj1).removeListener(binding);
        }
    }

    public static <K, V> void unbind(Map<K, V> obj1, ObservableMap<? extends K, ? extends V> obj2) {
        checkParameters(obj1, obj2);
        var binding = new MapContentBinding<>(obj1, obj2);
        obj2.removeListener(binding);

        if (obj1 instanceof ObservableMap<?, ?>) {
            ((ObservableMap<? extends K, ? extends V>)obj1).removeListener(binding);
        }
    }

    public abstract void dispose();

    private static class ListContentBinding<E> extends ContentBinding implements ListChangeListener<E> {
        private final WeakReference<List<E>> list1;
        private final WeakReference<ObservableList<? extends E>> list2;
        private boolean updating;

        public ListContentBinding(List<E> list1, ObservableList<? extends E> list2) {
            this.list1 = new WeakReference<>(list1);
            this.list2 = new WeakReference<>(list2);
        }

        @Override
        public void onChanged(Change<? extends E> change) {
            if (updating) {
                return;
            }

            final List<E> list1 = this.list1.get();

            // If the change is originating from list1 (which is the bound list), the change is an
            // illegal list modification. Since the content binding is now in an undefined state, the
            // only thing we can do is to error out and remove the binding.
            if (change.getList() == list1) {
                ((ObservableList<E>)list1).removeListener(this);

                ObservableList<? extends E> list2 = this.list2.get();
                if (list2 != null) {
                    list2.removeListener(this);
                }

                throw new RuntimeException(
                    "Illegal list modification: Content binding was removed because the lists are out-of-sync.");
            }

            if (list1 == null) {
                change.getList().removeListener(this);
            } else {
                try {
                    updating = true;

                    while (change.next()) {
                        if (change.wasPermutated()) {
                            list1.subList(change.getFrom(), change.getTo()).clear();
                            list1.addAll(change.getFrom(), change.getList().subList(change.getFrom(), change.getTo()));
                        } else {
                            if (change.wasRemoved()) {
                                list1.subList(change.getFrom(), change.getFrom() + change.getRemovedSize()).clear();
                            }
                            if (change.wasAdded()) {
                                list1.addAll(change.getFrom(), change.getAddedSubList());
                            }
                        }
                    }
                } finally {
                    updating = false;
                }
            }
        }

        @Override
        public void dispose() {
            List<E> list1 = this.list1.get();
            ObservableList<? extends E> list2 = this.list2.get();

            if (list1 instanceof ObservableList<?>) {
                ((ObservableList<E>)list1).removeListener(this);
            }

            if (list2 != null) {
                list2.removeListener(this);
            }
        }

        @Override
        public boolean wasGarbageCollected() {
            return list1.get() == null || list2.get() == null;
        }

        @Override
        public int hashCode() {
            final List<E> list1 = this.list1.get();
            final ObservableList<? extends E> list2 = this.list2.get();
            final int hc1 = (list1 == null) ? 0 : list1.hashCode();
            final int hc2 = (list2 == null) ? 0 : list2.hashCode();
            return hc1 * hc2;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            final Object listA1 = list1.get();
            final Object listA2 = list2.get();
            if ((listA1 == null) || (listA2 == null)) {
                return false;
            }

            if (obj instanceof ListContentBinding) {
                final ListContentBinding<?> otherBinding = (ListContentBinding<?>)obj;
                final Object listB1 = otherBinding.list1.get();
                final Object listB2 = otherBinding.list2.get();
                if ((listB1 == null) || (listB2 == null)) {
                    return false;
                }

                if ((listA1 == listB1) && (listA2 == listB2)) {
                    return true;
                }

                if ((listA1 == listB2) && (listA2 == listB1)) {
                    return true;
                }
            }

            return false;
        }
    }

    private static class SetContentBinding<E> extends ContentBinding implements SetChangeListener<E> {
        private final WeakReference<Set<E>> set1;
        private final WeakReference<ObservableSet<? extends E>> set2;
        private boolean updating;

        public SetContentBinding(Set<E> set1, ObservableSet<? extends E> set2) {
            this.set1 = new WeakReference<>(set1);
            this.set2 = new WeakReference<>(set2);
        }

        @Override
        public void onChanged(Change<? extends E> change) {
            if (updating) {
                return;
            }
            
            final Set<E> set1 = this.set1.get();

            // If the change is originating from set1 (which is the bound set), the change is an
            // illegal set modification. Since the content binding is now in an undefined state, the
            // only thing we can do is to error out and remove the binding.
            if (change.getSet() == set1) {
                ((ObservableSet<E>)set1).removeListener(this);

                ObservableSet<? extends E> set2 = this.set2.get();
                if (set2 != null) {
                    set2.removeListener(this);
                }

                throw new RuntimeException(
                    "Illegal set modification: Content binding was removed because the sets are out-of-sync.");
            }
            
            if (set1 == null) {
                change.getSet().removeListener(this);
            } else {
                try {
                    updating = true;

                    if (change.wasRemoved()) {
                        set1.remove(change.getElementRemoved());
                    } else {
                        set1.add(change.getElementAdded());
                    }
                } finally {
                    updating = false;
                }
            }
        }

        @Override
        public void dispose() {
            Set<E> set1 = this.set1.get();
            ObservableSet<? extends E> set2 = this.set2.get();

            if (set1 instanceof ObservableSet<?>) {
                ((ObservableSet<E>)set1).removeListener(this);
            }

            if (set2 != null) {
                set2.removeListener(this);
            }
        }

        @Override
        public boolean wasGarbageCollected() {
            return set1.get() == null || set2.get() == null;
        }

        @Override
        public int hashCode() {
            final Set<E> set1 = this.set1.get();
            final ObservableSet<? extends E> set2 = this.set2.get();
            final int hc1 = (set1 == null) ? 0 : set1.hashCode();
            final int hc2 = (set2 == null) ? 0 : set2.hashCode();
            return hc1 * hc2;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            final Object setA1 = set1.get();
            final Object setA2 = set2.get();
            if ((setA1 == null) || (setA2 == null)) {
                return false;
            }

            if (obj instanceof SetContentBinding) {
                final SetContentBinding<?> otherBinding = (SetContentBinding<?>)obj;
                final Object setB1 = otherBinding.set1.get();
                final Object setB2 = otherBinding.set2.get();
                if ((setB1 == null) || (setB2 == null)) {
                    return false;
                }

                if ((setA1 == setB1) && (setA2 == setB2)) {
                    return true;
                }

                if ((setA1 == setB2) && (setA2 == setB1)) {
                    return true;
                }
            }

            return false;
        }
    }

    private static class MapContentBinding<K, V> extends ContentBinding implements MapChangeListener<K, V> {
        private final WeakReference<Map<K, V>> map1;
        private final WeakReference<ObservableMap<? extends K, ? extends V>> map2;
        private boolean updating;

        public MapContentBinding(Map<K, V> map1, ObservableMap<? extends K, ? extends V> map2) {
            this.map1 = new WeakReference<>(map1);
            this.map2 = new WeakReference<>(map2);
        }

        @Override
        public void onChanged(Change<? extends K, ? extends V> change) {
            if (updating) {
                return;
            }

            final Map<K, V> map1 = this.map1.get();

            // If the change is originating from map1 (which is the bound map), the change is an
            // illegal map modification. Since the content binding is now in an undefined state, the
            // only thing we can do is to error out and remove the binding.
            if (change.getMap() == map1) {
                ((ObservableMap<? extends K, ? extends V>)map1).removeListener(this);

                ObservableMap<? extends K, ? extends V> map2 = this.map2.get();
                if (map2 != null) {
                    map2.removeListener(this);
                }

                throw new RuntimeException(
                    "Illegal map modification: Content binding was removed because the maps are out-of-sync.");
            }
            
            if (map1 == null) {
                change.getMap().removeListener(this);
            } else {
                try {
                    updating = true;

                    if (change.wasRemoved()) {
                        map1.remove(change.getKey());
                    }
                    if (change.wasAdded()) {
                        map1.put(change.getKey(), change.getValueAdded());
                    }
                } finally {
                    updating = false;
                }
            }
        }

        @Override
        public void dispose() {
            Map<K, V> map1 = this.map1.get();
            ObservableMap<? extends K, ? extends V> map2 = this.map2.get();

            if (map1 instanceof ObservableMap<?, ?>) {
                ((ObservableMap<? extends K, ? extends V>)map1).removeListener(this);
            }

            if (map2 != null) {
                map2.removeListener(this);
            }
        }

        @Override
        public boolean wasGarbageCollected() {
            return map1.get() == null || map2.get() == null;
        }

        @Override
        public int hashCode() {
            final Map<K, V> map1 = this.map1.get();
            final ObservableMap<? extends K, ? extends V> map2 = this.map2.get();
            final int hc1 = (map1 == null) ? 0 : map1.hashCode();
            final int hc2 = (map2 == null) ? 0 : map2.hashCode();
            return hc1 * hc2;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            final Object mapA1 = map1.get();
            final Object mapA2 = map2.get();
            if ((mapA1 == null) || (mapA2 == null)) {
                return false;
            }

            if (obj instanceof MapContentBinding) {
                final MapContentBinding<?, ?> otherBinding = (MapContentBinding<?, ?>)obj;
                final Object mapB1 = otherBinding.map1.get();
                final Object mapB2 = otherBinding.map2.get();
                if ((mapB1 == null) || (mapB2 == null)) {
                    return false;
                }

                if ((mapA1 == mapB1) && (mapA2 == mapB2)) {
                    return true;
                }

                if ((mapA1 == mapB2) && (mapA2 == mapB1)) {
                    return true;
                }
            }

            return false;
        }
    }
}
