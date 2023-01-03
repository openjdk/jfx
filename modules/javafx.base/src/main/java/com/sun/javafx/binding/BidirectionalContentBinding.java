/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

import javafx.beans.WeakListener;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;

/**
 */
public class BidirectionalContentBinding {

    private static void checkParameters(Object property1, Object property2) {
        if ((property1 == null) || (property2 == null)) {
            throw new NullPointerException("Both parameters must be specified.");
        }
        if (property1 == property2) {
            throw new IllegalArgumentException("Cannot bind object to itself");
        }
    }

    // TODO this really shouldn't be returning anything (unless it is a Subscription); only used for testing
    public static <E> Object bind(ObservableList<E> list1, ObservableList<E> list2) {
        return ListContentBinding.of(list1, list2);
    }

    public static <E> Object bind(ObservableSet<E> set1, ObservableSet<E> set2) {
        checkParameters(set1, set2);
        final SetContentBinding<E> binding = new SetContentBinding<>(set1, set2);
        set1.clear();
        set1.addAll(set2);
        set1.addListener(binding);
        set2.addListener(binding);
        return binding;
    }

    public static <K, V> Object bind(ObservableMap<K, V> map1, ObservableMap<K, V> map2) {
        checkParameters(map1, map2);
        final MapContentBinding<K, V> binding = new MapContentBinding<>(map1, map2);
        map1.clear();
        map1.putAll(map2);
        map1.addListener(binding);
        map2.addListener(binding);
        return binding;
    }

    public static void unbind(Object obj1, Object obj2) {
        checkParameters(obj1, obj2);
        if ((obj1 instanceof ObservableList<?> list1) && (obj2 instanceof ObservableList<?> list2)) {
            ListContentBinding.unbindIfPresent(list1, list2);
        } else if ((obj1 instanceof ObservableSet<?> set1) && (obj2 instanceof ObservableSet<?> set2)) {
            @SuppressWarnings("unchecked")
            final SetContentBinding<Object> binding = new SetContentBinding<>((ObservableSet<Object>) set1, (ObservableSet<Object>) set2);
            set1.removeListener(binding);
            set2.removeListener(binding);
        } else if ((obj1 instanceof ObservableMap) && (obj2 instanceof ObservableMap)) {
            final ObservableMap map1 = (ObservableMap)obj1;
            final ObservableMap map2 = (ObservableMap)obj2;
            final MapContentBinding binding = new MapContentBinding(map1, map2);
            map1.removeListener(binding);
            map2.removeListener(binding);
        }
    }

    private static class ListContentBinding implements ListChangeListener<Object>, WeakListener {
        private static final Map<ObservableList<?>, Set<ListContentBinding>> BINDINGS = new WeakHashMap<>();

        private final WeakReference<ObservableList<?>> propertyRef1;
        private final WeakReference<ObservableList<?>> propertyRef2;

        private boolean updating = false;

        /**
         * Creates a bidirectional content binding between the given lists.
         *
         * @param <E> the element type of the lists
         * @param list1 the first list, cannot be {@code null}
         * @param list2 the second list, cannot be {@code null}
         * @throws NullPointerException when either list is {@code null}
         * @throws IllegalArgumentException when the lists are the same, the lists are already bound to each other, or
         *   the binding would result in a circular binding between 3 or more lists
         */
        public static <E> ListContentBinding of(ObservableList<E> list1, ObservableList<E> list2) {
            checkParameters(list1, list2);
            ensureNoCyclicDependencies(list1, list2, obj -> Stream.ofNullable(BINDINGS.get(obj))
                .flatMap(Set::stream)
                .map(binding -> binding.propertyRef2.get())
                .filter(Objects::nonNull)  // skip if reference is gone
                .filter(observable -> observable != obj)  // skip if "target" is same same as "source" vertex in a DAG
                .toList()
            );

            ListContentBinding binding = new ListContentBinding(list1, list2);

            if (!BINDINGS.computeIfAbsent(list1, k -> new HashSet<>()).add(binding)) {
                throw new IllegalArgumentException("Already bound to each other");
            }

            BINDINGS.computeIfAbsent(list2, k -> new HashSet<>()).add(binding);

            list1.setAll(list2);
            list1.addListener(binding);
            list2.addListener(binding);

            return binding;  // TODO this is currently only returned for testing the hashcode/equals stuff, but IMHO should not be exposed at all!
        }

        public static void unbindIfPresent(ObservableList<?> list1, ObservableList<?> list2) {
            Set<ListContentBinding> set1 = BINDINGS.get(list1);
            Set<ListContentBinding> set2 = BINDINGS.get(list2);

            if(set1 != null && set2 != null) {
                set1.stream().filter(set2::contains).findFirst().ifPresent(binding -> {
                    BINDINGS.computeIfPresent(list1, (k, v) -> v.remove(binding) && v.isEmpty() ? null : v);
                    BINDINGS.computeIfPresent(list2, (k, v) -> v.remove(binding) && v.isEmpty() ? null : v);

                    list1.removeListener(binding);
                    list2.removeListener(binding);
                });
            }
        }

        private <E> ListContentBinding(ObservableList<E> list1, ObservableList<E> list2) {
            propertyRef1 = new WeakReference<>(list1);
            propertyRef2 = new WeakReference<>(list2);
        }

        @Override
        public void onChanged(Change<?> change) {
            if (!updating) {
                @SuppressWarnings("unchecked")  // Safe cast, content of list is irrelevant
                final ObservableList<Object> list1 = (ObservableList<Object>) propertyRef1.get();

                @SuppressWarnings("unchecked")  // Safe cast, content of list is irrelevant
                final ObservableList<Object> list2 = (ObservableList<Object>) propertyRef2.get();

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
                        final ObservableList<Object> dest = (list1 ==  change.getList())? list2 : list1;
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
            final ObservableList<?> list1 = propertyRef1.get();
            final ObservableList<?> list2 = propertyRef2.get();
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

            if (obj instanceof ListContentBinding otherBinding) {
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

    private static class SetContentBinding<E> implements SetChangeListener<E>, WeakListener {

        private final WeakReference<ObservableSet<E>> propertyRef1;
        private final WeakReference<ObservableSet<E>> propertyRef2;

        private boolean updating = false;


        public SetContentBinding(ObservableSet<E> list1, ObservableSet<E> list2) {
            propertyRef1 = new WeakReference<>(list1);
            propertyRef2 = new WeakReference<>(list2);
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

    private static class MapContentBinding<K, V> implements MapChangeListener<K, V>, WeakListener {

        private final WeakReference<ObservableMap<K, V>> propertyRef1;
        private final WeakReference<ObservableMap<K, V>> propertyRef2;

        private boolean updating = false;


        public MapContentBinding(ObservableMap<K, V> list1, ObservableMap<K, V> list2) {
            propertyRef1 = new WeakReference<>(list1);
            propertyRef2 = new WeakReference<>(list2);
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

    private static void ensureNoCyclicDependencies(Object obj1, Object obj2, Function<Object, List<?>> dependencyProvider) {
        Set<Object> input = Collections.newSetFromMap(new IdentityHashMap<>());

        input.add(obj1);
        input.add(obj2);

        class CycleDetector {
            Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
            Set<Object> visiting = Collections.newSetFromMap(new IdentityHashMap<>());

            Set<Object> getCycle() {
                for(Object node : input) {
                    if(!visited.contains(node) && hasCycle(node)) {
                        return visiting;
                    }
                }

                return visiting;
            }

            boolean hasCycle(Object node) {
                visiting.add(node);

                for(Object dependency : dependencyProvider.apply(node)) {
                    if(visiting.contains(dependency)) {
                        return true;
                    }
                    else if(!visited.contains(dependency) && hasCycle(dependency)) {
                        return true;
                    }
                }

                visiting.remove(node);
                visited.add(node);

                return false;
            }
        }

        Set<Object> cycle = new CycleDetector().getCycle();

        if(!cycle.isEmpty()) {
            throw new IllegalArgumentException("Cycle in: " + cycle);
        }
    }
}
