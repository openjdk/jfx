/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.collections.ListListenerHelper;
import com.sun.javafx.collections.MapListenerHelper;
import com.sun.javafx.collections.SetListenerHelper;
import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;

import javafx.beans.InvalidationListener;

import com.sun.javafx.collections.ObservableListWrapper;
import com.sun.javafx.collections.ObservableMapWrapper;
import com.sun.javafx.collections.ObservableSetWrapper;
import com.sun.javafx.collections.MapAdapterChange;
import com.sun.javafx.collections.ObservableFloatArrayImpl;
import com.sun.javafx.collections.ObservableIntegerArrayImpl;
import com.sun.javafx.collections.ObservableSequentialListWrapper;
import com.sun.javafx.collections.SetAdapterChange;
import com.sun.javafx.collections.SortableList;
import com.sun.javafx.collections.SourceAdapterChange;
import java.util.RandomAccess;
import javafx.beans.Observable;
import javafx.collections.ListChangeListener.Change;
import javafx.util.Callback;

/**
 * Utility class that consists of static methods that are 1:1 copies of java.util.Collections methods.
 * <br><br>
 * The wrapper methods (like synchronizedObservableList or emptyObservableList) has exactly the same
 * functionality as the methods in Collections, with exception that they return ObservableList and are
 * therefore suitable for methods that require ObservableList on input.
 * <br><br>
 * The utility methods are here mainly for performance reasons. All methods are optimized in a way that
 * they yield only limited number of notifications. On the other hand, java.util.Collections methods
 * might call "modification methods" on an ObservableList multiple times, resulting in a number of notifications.
 *
 * @since JavaFX 2.0
 */
public class FXCollections {
    /** Not to be instantiated. */
    private FXCollections() { }

    /**
     * Constructs an ObservableList that is backed by the specified list.
     * Mutation operations on the ObservableList instance will be reported
     * to observers that have registered on that instance.<br>
     * Note that mutation operations made directly to the underlying list are
     * <em>not</em> reported to observers of any ObservableList that
     * wraps it.
     *
     * @param <E> The type of List to be wrapped
     * @param list a concrete List that backs this ObservableList
     * @return a newly created ObservableList
     */
    public static <E> ObservableList<E> observableList(List<E> list) {
        if (list == null) {
            throw new NullPointerException();
        }
        return list instanceof RandomAccess ? new ObservableListWrapper<E>(list) :
                new ObservableSequentialListWrapper<E>(list);
    }

    /**
     * Constructs an ObservableList that is backed by the specified list.
     * Mutation operations on the ObservableList instance will be reported
     * to observers that have registered on that instance.<br>
     * Note that mutation operations made directly to the underlying list are
     * <em>not</em> reported to observers of any ObservableList that
     * wraps it.
     * <br>
     * This list also reports mutations of the elements in it by using <code>extractor</code>.
     * Observable objects returned by extractor (applied to each list element) are listened for changes
     * and transformed into "update" change of ListChangeListener.
     *
     * @param <E> The type of List to be wrapped
     * @param list a concrete List that backs this ObservableList
     * @param extractor element to Observable[] convertor
     * @since JavaFX 2.1
     * @return a newly created ObservableList
     */
    public static <E> ObservableList<E> observableList(List<E> list, Callback<E, Observable[]> extractor) {
        if (list == null || extractor == null) {
            throw new NullPointerException();
        }
        return list instanceof RandomAccess ? new ObservableListWrapper<E>(list, extractor) :
            new ObservableSequentialListWrapper<E>(list, extractor);
    }

    /**
     * Constructs an ObservableMap that is backed by the specified map.
     * Mutation operations on the ObservableMap instance will be reported
     * to observers that have registered on that instance.<br>
     * Note that mutation operations made directly to the underlying map are <em>not</em>
     * reported to observers of any ObservableMap that wraps it.
     * @param <K> the type of the wrapped key
     * @param <V> the type of the wrapped value
     * @param map a Map that backs this ObservableMap
     * @return a newly created ObservableMap
     */
    public static <K, V> ObservableMap<K, V> observableMap(Map<K, V> map) {
        if (map == null) {
            throw new NullPointerException();
        }
        return new ObservableMapWrapper<K, V>(map);
    }

    /**
     * Constructs an ObservableSet that is backed by the specified set.
     * Mutation operations on the ObservableSet instance will be reported
     * to observers that have registered on that instance.<br>
     * Note that mutation operations made directly to the underlying set are <em>not</em>
     * reported to observers of any ObservableSet that wraps it.
     * @param <E> The type of List to be wrapped
     * @param set a Set that backs this ObservableSet
     * @return a newly created ObservableSet
     * @since JavaFX 2.1
     */
    public static <E> ObservableSet<E> observableSet(Set<E> set) {
        if (set == null) {
            throw new NullPointerException();
        }
        return new ObservableSetWrapper<E>(set);
    }

    /**
     * Constructs an ObservableSet backed by a HashSet
     * that contains all the specified elements.
     * @param <E> The type of List to be wrapped
     * @param elements elements that will be added into returned ObservableSet
     * @return a newly created ObservableSet
     * @since JavaFX 2.1
     */
    public static <E> ObservableSet<E> observableSet(E... elements) {
        if (elements == null) {
            throw new NullPointerException();
        }
        Set<E> set = new HashSet<E>(elements.length);
        Collections.addAll(set, elements);
        return new ObservableSetWrapper<E>(set);
    }

    /**
     * Constructs a read-only interface to the specified ObservableMap. Only
     * mutation operations made to the underlying ObservableMap will be reported
     * to observers that have registered on the unmodifiable instance. This allows
     * clients to track changes in a Map but disallows the ability to modify it.
     * @param <K> the type of the wrapped key
     * @param <V> the type of the wrapped value
     * @param map an ObservableMap that is to be monitored by this interface
     * @return a newly created UnmodifiableObservableMap
     */
    public static <K, V> ObservableMap<K, V> unmodifiableObservableMap(ObservableMap<K, V> map) {
        if (map == null) {
            throw new NullPointerException();
        }
        return new com.sun.javafx.collections.UnmodifiableObservableMap<K, V>(map);
    }

    /**
     * Creates and returns a typesafe wrapper on top of provided observable map.
     * @param <K> the type of the wrapped key
     * @param <V> the type of the wrapped value
     * @param map an Observable map to be wrapped
     * @param keyType the type of key that {@code map} is permitted to hold
     * @param valueType the type of value that {@code map} is permitted to hold
     * @return a dynamically typesafe view of the specified map
     * @see Collections#checkedMap(java.util.Map, java.lang.Class, java.lang.Class)
     * @since JavaFX 8.0
     */
    public static <K, V> ObservableMap<K, V> checkedObservableMap(ObservableMap<K, V> map, Class<K> keyType, Class<V> valueType) {
        if (map == null || keyType == null || valueType == null) {
            throw new NullPointerException();
        }
        return new CheckedObservableMap<K, V>(map, keyType, valueType);
    }

    /**
     * Creates and returns a synchronized wrapper on top of provided observable map.
     * @param <K> the type of the wrapped key
     * @param <V> the type of the wrapped value
     * @param  map the map to be "wrapped" in a synchronized map.
     * @return A synchronized version of the observable map
     * @see Collections#synchronizedMap(java.util.Map)
     * @since JavaFX 8.0
     */
    public static <K, V> ObservableMap<K, V> synchronizedObservableMap(ObservableMap<K, V> map) {
        if (map == null) {
            throw new NullPointerException();
        }
        return new SynchronizedObservableMap<K, V>(map);
    }

    private static ObservableMap EMPTY_OBSERVABLE_MAP = new EmptyObservableMap();

    /**
     * Creates an empty unmodifiable observable map.
     * @param <K> the type of the wrapped key
     * @param <V> the type of the wrapped value
     * @return An empty unmodifiable observable map
     * @see Collections#emptyMap()
     * @since JavaFX 8.0
     */
    @SuppressWarnings("unchecked")
    public static <K, V> ObservableMap<K, V> emptyObservableMap() {
        return EMPTY_OBSERVABLE_MAP;
    }

    /**
     * Creates a new empty observable integer array.
     * @return a newly created ObservableIntegerArray
     * @since JavaFX 8.0
     */
    public static ObservableIntegerArray observableIntegerArray() {
        return new ObservableIntegerArrayImpl();
    }

    /**
     * Creates a new observable integer array with {@code values} set to it.
     * @param values the values that will be in the new observable integer array
     * @return a newly created ObservableIntegerArray
     * @since JavaFX 8.0
     */
    public static ObservableIntegerArray observableIntegerArray(int... values) {
        return new ObservableIntegerArrayImpl(values);
    }

    /**
     * Creates a new observable integer array with copy of elements in given
     * {@code array}.
     * @param array observable integer array to copy
     * @return a newly created ObservableIntegerArray
     * @since JavaFX 8.0
     */
    public static ObservableIntegerArray observableIntegerArray(ObservableIntegerArray array) {
        return new ObservableIntegerArrayImpl(array);
    }

    /**
     * Creates a new empty observable float array.
     * @return a newly created ObservableFloatArray
     * @since JavaFX 8.0
     */
    public static ObservableFloatArray observableFloatArray() {
        return new ObservableFloatArrayImpl();
    }

    /**
     * Creates a new observable float array with {@code values} set to it.
     * @param values the values that will be in the new observable float array
     * @return a newly created ObservableFloatArray
     * @since JavaFX 8.0
     */
    public static ObservableFloatArray observableFloatArray(float... values) {
        return new ObservableFloatArrayImpl(values);
    }

    /**
     * Creates a new observable float array with copy of elements in given
     * {@code array}.
     * @param array observable float array to copy
     * @return a newly created ObservableFloatArray
     * @since JavaFX 8.0
     */
    public static ObservableFloatArray observableFloatArray(ObservableFloatArray array) {
        return new ObservableFloatArrayImpl(array);
    }

    /**
     * Creates a new empty observable list that is backed by an arraylist.
     * @see #observableList(java.util.List)
     * @param <E> The type of List to be wrapped
     * @return a newly created ObservableList
     */
    @SuppressWarnings("unchecked")
    public static <E> ObservableList<E> observableArrayList() {
        return observableList(new ArrayList());
    }

    /**
     * Creates a new empty observable list backed by an arraylist.
     *
     * This list reports element updates.
     * @param <E> The type of List to be wrapped
     * @param extractor element to Observable[] convertor. Observable objects are listened for changes on the element.
     * @see #observableList(java.util.List, javafx.util.Callback)
     * @since JavaFX 2.1
     * @return a newly created ObservableList
     */
    public static <E> ObservableList<E> observableArrayList(Callback<E, Observable[]> extractor) {
        return observableList(new ArrayList(), extractor);
    }

    /**
     * Creates a new observable array list with {@code items} added to it.
     * @param <E> The type of List to be wrapped
     * @param items the items that will be in the new observable ArrayList
     * @return a newly created observableArrayList
     * @see #observableArrayList()
     */
    public static <E> ObservableList<E> observableArrayList(E... items) {
        ObservableList<E> list = observableArrayList();
        list.addAll(items);
        return list;
    }

    /**
     * Creates a new observable array list and adds a content of collection {@code col}
     * to it.
     * @param <E> The type of List to be wrapped
     * @param col a collection which content should be added to the observableArrayList
     * @return a newly created observableArrayList
     */
    public static <E> ObservableList<E> observableArrayList(Collection<? extends E> col) {
        ObservableList<E> list = observableArrayList();
        list.addAll(col);
        return list;
    }

    /**
     * Creates a new empty observable map that is backed by a HashMap.
     * @param <K> the type of the wrapped key
     * @param <V> the type of the wrapped value
     * @return a newly created observable HashMap
     */
    public static <K,V> ObservableMap<K,V> observableHashMap() {
        return observableMap(new HashMap<K, V>());
    }

    /**
     * Concatenates more observable lists into one. The resulting list
     * would be backed by an arraylist.
     * @param <E> The type of List to be wrapped
     * @param lists lists to concatenate
     * @return new observable array list concatenated from the arguments
     */
    public static <E> ObservableList<E> concat(ObservableList<E>... lists) {
        if (lists.length == 0 ) {
            return observableArrayList();
        }
        if (lists.length == 1) {
            return observableArrayList(lists[0]);
        }
        ArrayList<E> backingList = new ArrayList<E>();
        for (ObservableList<E> s : lists) {
            backingList.addAll(s);
        }

        return observableList(backingList);
    }

    /**
     * Creates and returns unmodifiable wrapper list on top of provided observable list.
     * @param list  an ObservableList that is to be wrapped
     * @param <E> The type of List to be wrapped
     * @return an ObserableList wrapper that is unmodifiable
     * @see Collections#unmodifiableList(java.util.List)
     */
    public static<E> ObservableList<E> unmodifiableObservableList(ObservableList<E> list) {
        if (list == null) {
            throw new NullPointerException();
        }
        return new UnmodifiableObservableListImpl<E>(list);
    }

    /**
     * Creates and returns a typesafe wrapper on top of provided observable list.
     * @param <E> The type of List to be wrapped
     * @param list  an Observable list to be wrapped
     * @param type   the type of element that {@code list} is permitted to hold
     * @return a dynamically typesafe view of the specified list
     * @see Collections#checkedList(java.util.List, java.lang.Class)
     */
    public static<E> ObservableList<E> checkedObservableList(ObservableList<E> list, Class<E> type) {
        if (list == null) {
            throw new NullPointerException();
        }
        return new CheckedObservableList<E>(list, type);
    }

    /**
     * Creates and returns a synchronized wrapper on top of provided observable list.
     * @param <E> The type of List to be wrapped
     * @param  list the list to be "wrapped" in a synchronized list.
     * @return A synchronized version of the observable list
     * @see Collections#synchronizedList(java.util.List)
     */
    public static<E> ObservableList<E> synchronizedObservableList(ObservableList<E> list) {
        if (list == null) {
            throw new NullPointerException();
        }
        return new SynchronizedObservableList<E>(list);
    }

    private static ObservableList EMPTY_OBSERVABLE_LIST = new EmptyObservableList();


    /**
     * Creates an empty unmodifiable observable list.
     * @param <E> The type of List to be wrapped
     * @return An empty unmodifiable observable list
     * @see Collections#emptyList()
     */
    @SuppressWarnings("unchecked")
    public static<E> ObservableList<E> emptyObservableList() {
        return EMPTY_OBSERVABLE_LIST;
    }

    /**
     * Creates an unmodifiable observable list with single element.
     * @param <E> The type of List to be wrapped
     * @param e the only elements that will be contained in this singleton observable list
     * @return a singleton observable list
     * @see Collections#singletonList(java.lang.Object)
     */
    public static<E> ObservableList<E> singletonObservableList(E e) {
        return new SingletonObservableList<E>(e);
    }

    /**
     * Creates and returns unmodifiable wrapper on top of provided observable set.
     * @param <E> The type of List to be wrapped
     * @param set an ObservableSet that is to be wrapped
     * @return an ObserableSet wrapper that is unmodifiable
     * @see Collections#unmodifiableSet(java.util.Set)
     * @since JavaFX 8.0
     */
    public static<E> ObservableSet<E> unmodifiableObservableSet(ObservableSet<E> set) {
        if (set == null) {
            throw new NullPointerException();
        }
        return new UnmodifiableObservableSet<E>(set);
    }

    /**
     * Creates and returns a typesafe wrapper on top of provided observable set.
     * @param <E> The type of List to be wrapped
     * @param set an Observable set to be wrapped
     * @param type  the type of element that {@code set} is permitted to hold
     * @return a dynamically typesafe view of the specified set
     * @see Collections#checkedSet(java.util.Set, java.lang.Class)
     * @since JavaFX 8.0
     */
    public static<E> ObservableSet<E> checkedObservableSet(ObservableSet<E> set, Class<E> type) {
        if (set == null) {
            throw new NullPointerException();
        }
        return new CheckedObservableSet<E>(set, type);
    }

    /**
     * Creates and returns a synchronized wrapper on top of provided observable set.
     * @param <E> The type of List to be wrapped
     * @param  set the set to be "wrapped" in a synchronized set.
     * @return A synchronized version of the observable set
     * @see Collections#synchronizedSet(java.util.Set)
     * @since JavaFX 8.0
     */
    public static<E> ObservableSet<E> synchronizedObservableSet(ObservableSet<E> set) {
        if (set == null) {
            throw new NullPointerException();
        }
        return new SynchronizedObservableSet<E>(set);
    }

    private static ObservableSet EMPTY_OBSERVABLE_SET = new EmptyObservableSet();

    /**
     * Creates an empty unmodifiable observable set.
     * @param <E> The type of List to be wrapped
     * @return An empty unmodifiable observable set
     * @see Collections#emptySet()
     * @since JavaFX 8.0
     */
    @SuppressWarnings("unchecked")
    public static<E> ObservableSet<E> emptyObservableSet() {
        return EMPTY_OBSERVABLE_SET;
    }

    /**
     * Copies elements from src to dest. Fires only <b>one</b> change notification on dest.
     * @param <T> The type of List to be wrapped
     * @param dest the destination observable list
     * @param src the source list
     * @see Collections#copy(java.util.List, java.util.List)
     */
    @SuppressWarnings("unchecked")
    public static <T> void copy(ObservableList<? super T> dest, List<? extends T> src) {
        final int srcSize = src.size();
        if (srcSize > dest.size()) {
            throw new IndexOutOfBoundsException("Source does not fit in dest");
        }
        T[] destArray = (T[]) dest.toArray();
        System.arraycopy(src.toArray(), 0, destArray, 0, srcSize);
        dest.setAll(destArray);
    }

    /**
     * Fills the provided list with obj. Fires only <b>one</b> change notification on the list.
     * @param <T> The type of List to be wrapped
     * @param list the list to fill
     * @param obj the object to fill the list with
     * @see Collections#fill(java.util.List, java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    public static <T> void fill(ObservableList<? super T> list, T obj) {
        T[] newContent = (T[]) new Object[list.size()];
        Arrays.fill(newContent, obj);
        list.setAll(newContent);
    }

    /**
     * Replace all oldVal elements in the list with newVal element.
     * Fires only <b>one</b> change notification on the list.
     * @param <T> The type of List to be wrapped
     * @param list the list which will have it's elements replaced
     * @param oldVal the element that is going to be replace
     * @param newVal the replacement
     * @return true if the list was modified
     * @see Collections#replaceAll(java.util.List, java.lang.Object, java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    public static <T> boolean replaceAll(ObservableList<T> list, T oldVal, T newVal) {
        T[] newContent = (T[]) list.toArray();
        boolean modified = false;
        for (int i = 0 ; i < newContent.length; ++i) {
            if (newContent[i].equals(oldVal)) {
                newContent[i] = newVal;
                modified = true;
            }
        }
        if (modified) {
            list.setAll(newContent);
        }
        return modified;
    }

    /**
     * Reverse the order in the list
     * Fires only <b>one</b> change notification on the list.
     * @param list the list to be reversed
     * @see Collections#reverse(java.util.List)
     */
    @SuppressWarnings("unchecked")
    public static void reverse(ObservableList list) {
        Object[] newContent = list.toArray();
        for (int i = 0; i < newContent.length / 2; ++i) {
            Object tmp = newContent[i];
            newContent[i] = newContent[newContent.length - i - 1];
            newContent[newContent.length -i - 1] = tmp;
        }
        list.setAll(newContent);
    }

    /**
     * Rotates the list by distance.
     * Fires only <b>one</b> change notification on the list.
     * @param list the list to be rotated
     * @param distance the distance of rotation
     * @see Collections#rotate(java.util.List, int)
     */
    @SuppressWarnings("unchecked")
    public static void rotate(ObservableList list, int distance) {
        Object[] newContent = list.toArray();

        int size = list.size();
        distance = distance % size;
        if (distance < 0)
            distance += size;
        if (distance == 0)
            return;

        for (int cycleStart = 0, nMoved = 0; nMoved != size; cycleStart++) {
            Object displaced = newContent[cycleStart];
            Object tmp;
            int i = cycleStart;
            do {
                i += distance;
                if (i >= size)
                    i -= size;
                tmp = newContent[i];
                newContent[i] = displaced;
                displaced = tmp;
                nMoved ++;
            } while(i != cycleStart);
        }
        list.setAll(newContent);
    }

    /**
     * Shuffles all elements in the observable list.
     * Fires only <b>one</b> change notification on the list.
     * @param list the list to shuffle
     * @see Collections#shuffle(java.util.List)
     */
    public static void shuffle(ObservableList<?> list) {
        if (r == null) {
            r = new Random();
        }
        shuffle(list, r);
    }
    private static Random r;

    /**
     * Shuffles all elements in the observable list.
     * Fires only <b>one</b> change notification on the list.
     * @param list the list to be shuffled
     * @param rnd the random generator used for shuffling
     * @see Collections#shuffle(java.util.List, java.util.Random)
     */
    @SuppressWarnings("unchecked")
    public static void shuffle(ObservableList list, Random rnd) {
        Object newContent[] = list.toArray();

        for (int i = list.size(); i > 1; i--) {
            swap(newContent, i - 1, rnd.nextInt(i));
        }

        list.setAll(newContent);
    }

    private static void swap(Object[] arr, int i, int j) {
        Object tmp = arr[i];
        arr[i] = arr[j];
        arr[j] = tmp;
    }

    /**
     * Sorts the provided observable list.
     * Fires only <b>one</b> change notification on the list.
     * @param <T> The type of List to be wrapped
     * @param list the list to be sorted
     * @see Collections#sort(java.util.List)
     */
    @SuppressWarnings("unchecked")
    public static <T extends Comparable<? super T>> void sort(ObservableList<T> list) {
        if (list instanceof SortableList) {
            ((SortableList<? extends T>)list).sort();
        } else {
            List<T> newContent = new ArrayList<T>(list);
            Collections.sort(newContent);
            list.setAll((Collection<T>)newContent);
        }
    }

    /**
     * Sorts the provided observable list using the c comparator.
     * Fires only <b>one</b> change notification on the list.
     * @param <T> The type of List to be wrapped
     * @param list the list to sort
     * @param c comparator used for sorting. Null if natural ordering is required.
     * @see Collections#sort(java.util.List, java.util.Comparator)
     */
    @SuppressWarnings("unchecked")
    public static <T> void sort(ObservableList<T> list, Comparator<? super T> c) {
        if (list instanceof SortableList) {
            ((SortableList<? extends T>)list).sort(c);
        } else {
            List<T> newContent = new ArrayList<T>(list);
            Collections.sort(newContent, c);
            list.setAll((Collection<T>)newContent);
        }
    }

    private static class EmptyObservableList<E> extends AbstractList<E> implements ObservableList<E> {

        private static final ListIterator iterator = new ListIterator() {

            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public Object next() {
                throw new NoSuchElementException();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean hasPrevious() {
                return false;
            }

            @Override
            public Object previous() {
                throw new NoSuchElementException();
            }

            @Override
            public int nextIndex() {
                return 0;
            }

            @Override
            public int previousIndex() {
                return -1;
            }

            @Override
            public void set(Object e) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void add(Object e) {
                throw new UnsupportedOperationException();
            }
        };

        public EmptyObservableList() {
        }

        @Override
        public final void addListener(InvalidationListener listener) {
        }

        @Override
        public final void removeListener(InvalidationListener listener) {
        }


        @Override
        public void addListener(ListChangeListener<? super E> o) {
        }

        @Override
        public void removeListener(ListChangeListener<? super E> o) {
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean contains(Object o) {
            return false;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Iterator<E> iterator() {
            return iterator;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return c.isEmpty();
        }

        @Override
        public E get(int index) {
            throw new IndexOutOfBoundsException();
        }

        @Override
        public int indexOf(Object o) {
            return -1;
        }

        @Override
        public int lastIndexOf(Object o) {
            return -1;
        }

        @Override
        @SuppressWarnings("unchecked")
        public ListIterator<E> listIterator() {
            return iterator;
        }

        @Override
        @SuppressWarnings("unchecked")
        public ListIterator<E> listIterator(int index) {
            if (index != 0) {
                throw new IndexOutOfBoundsException();
            }
            return iterator;
        }

        @Override
        public List<E> subList(int fromIndex, int toIndex) {
            if (fromIndex != 0 || toIndex != 0) {
                throw new IndexOutOfBoundsException();
            }
            return this;
        }

        @Override
        public boolean addAll(E... elements) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean setAll(E... elements) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean setAll(Collection<? extends E> col) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(E... elements) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(E... elements) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void remove(int from, int to) {
            throw new UnsupportedOperationException();
        }
    }

    private static class SingletonObservableList<E> extends AbstractList<E> implements ObservableList<E> {

        private final E element;

        public SingletonObservableList(E element) {
            if (element == null) {
                throw new NullPointerException();
            }
            this.element = element;
        }

        @Override
        public boolean addAll(E... elements) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean setAll(E... elements) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean setAll(Collection<? extends E> col) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(E... elements) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(E... elements) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void remove(int from, int to) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addListener(InvalidationListener listener) {
        }

        @Override
        public void removeListener(InvalidationListener listener) {
        }

        @Override
        public void addListener(ListChangeListener<? super E> o) {
        }

        @Override
        public void removeListener(ListChangeListener<? super E> o) {
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean contains(Object o) {
            return element.equals(o);
        }

        @Override
        public E get(int index) {
            if (index != 0) {
                throw new IndexOutOfBoundsException();
            }
            return element;
        }

    }

    private static class UnmodifiableObservableListImpl<T> extends ObservableListBase<T> implements ObservableList<T> {

        private final ObservableList<T> backingList;
        private final ListChangeListener<T> listener;

        public UnmodifiableObservableListImpl(ObservableList<T> backingList) {
            this.backingList = backingList;
            listener = c -> {
                fireChange(new SourceAdapterChange<T>(UnmodifiableObservableListImpl.this, c));
            };
            this.backingList.addListener(new WeakListChangeListener<T>(listener));
        }

        @Override
        public T get(int index) {
            return backingList.get(index);
        }

        @Override
        public int size() {
            return backingList.size();
        }

        @Override
        public boolean addAll(T... elements) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean setAll(T... elements) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean setAll(Collection<? extends T> col) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(T... elements) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(T... elements) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void remove(int from, int to) {
            throw new UnsupportedOperationException();
        }

    }

    private static class SynchronizedList<T> implements List<T> {
        final Object mutex;
        private final List<T> backingList;

        SynchronizedList(List<T> list, Object mutex) {
            this.backingList = list;
            this.mutex = mutex;
        }

        @Override
        public int size() {
            synchronized(mutex) {
                return backingList.size();
            }
        }

        @Override
        public boolean isEmpty() {
            synchronized(mutex) {
                return backingList.isEmpty();
            }
        }

        @Override
        public boolean contains(Object o) {
            synchronized(mutex) {
                return backingList.contains(o);
            }
        }

        @Override
        public Iterator<T> iterator() {
            return backingList.iterator();
        }

        @Override
        public Object[] toArray() {
            synchronized(mutex)  {
                return backingList.toArray();
            }
        }

        @Override
        public <T> T[] toArray(T[] a) {
            synchronized(mutex) {
                return backingList.toArray(a);
            }
        }

        @Override
        public boolean add(T e) {
            synchronized(mutex) {
                return backingList.add(e);
            }
        }

        @Override
        public boolean remove(Object o) {
            synchronized(mutex) {
                return backingList.remove(o);
            }
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            synchronized(mutex) {
                return backingList.containsAll(c);
            }
        }

        @Override
        public boolean addAll(Collection<? extends T> c) {
            synchronized(mutex) {
                return backingList.addAll(c);
            }
        }

        @Override
        public boolean addAll(int index, Collection<? extends T> c) {
            synchronized(mutex) {
                return backingList.addAll(index, c);

            }
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            synchronized(mutex) {
                return backingList.removeAll(c);
            }
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            synchronized(mutex) {
                return backingList.retainAll(c);
            }
        }

        @Override
        public void clear() {
            synchronized(mutex) {
                backingList.clear();
            }
        }

        @Override
        public T get(int index) {
            synchronized(mutex) {
                return backingList.get(index);
            }
        }

        @Override
        public T set(int index, T element) {
            synchronized(mutex) {
                return backingList.set(index, element);
            }
        }

        @Override
        public void add(int index, T element) {
            synchronized(mutex) {
                backingList.add(index, element);
            }
        }

        @Override
        public T remove(int index) {
            synchronized(mutex) {
                return backingList.remove(index);
            }
        }

        @Override
        public int indexOf(Object o) {
            synchronized(mutex) {
                return backingList.indexOf(o);
            }
        }

        @Override
        public int lastIndexOf(Object o) {
            synchronized(mutex) {
                return backingList.lastIndexOf(o);
            }
        }

        @Override
        public ListIterator<T> listIterator() {
            return backingList.listIterator();
        }

        @Override
        public ListIterator<T> listIterator(int index) {
            synchronized(mutex) {
                return backingList.listIterator(index);
            }
        }

        @Override
        public List<T> subList(int fromIndex, int toIndex) {
            synchronized(mutex) {
                return new SynchronizedList<T>(backingList.subList(fromIndex, toIndex),
                        mutex);
            }
        }

        @Override
        public String toString() {
            synchronized(mutex) {
                return backingList.toString();
            }
        }

        @Override
        public int hashCode() {
            synchronized(mutex) {
                return backingList.hashCode();
            }
        }

        @Override
        public boolean equals(Object o) {
            synchronized(mutex) {
                return backingList.equals(o);
            }
        }

    }

    private static class SynchronizedObservableList<T> extends SynchronizedList<T> implements ObservableList<T> {

        private ListListenerHelper helper;

        private final ObservableList<T> backingList;
        private final ListChangeListener<T> listener;

        SynchronizedObservableList(ObservableList<T> seq, Object mutex) {
            super(seq, mutex);
            this.backingList = seq;
            listener = c -> {
                ListListenerHelper.fireValueChangedEvent(helper, new SourceAdapterChange<T>(SynchronizedObservableList.this, c));
            };
            backingList.addListener(new WeakListChangeListener<T>(listener));
        }

        SynchronizedObservableList(ObservableList<T> seq) {
            this(seq, new Object());
        }

        @Override
        public boolean addAll(T... elements) {
            synchronized(mutex) {
                return backingList.addAll(elements);
            }
        }

        @Override
        public boolean setAll(T... elements) {
            synchronized(mutex) {
                return backingList.setAll(elements);
            }
        }

        @Override
        public boolean removeAll(T... elements) {
            synchronized(mutex) {
                return backingList.removeAll(elements);
            }
        }

        @Override
        public boolean retainAll(T... elements) {
            synchronized(mutex) {
                return backingList.retainAll(elements);
            }
        }

        @Override
        public void remove(int from, int to) {
            synchronized(mutex) {
                backingList.remove(from, to);
            }
        }

        @Override
        public boolean setAll(Collection<? extends T> col) {
            synchronized(mutex) {
                return backingList.setAll(col);
            }
        }

        @Override
        public final void addListener(InvalidationListener listener) {
            synchronized (mutex) {
                helper = ListListenerHelper.addListener(helper, listener);
            }
        }

        @Override
        public final void removeListener(InvalidationListener listener) {
            synchronized (mutex) {
                helper = ListListenerHelper.removeListener(helper, listener);
            }
        }

        @Override
        public void addListener(ListChangeListener<? super T> listener) {
            synchronized (mutex) {
                helper = ListListenerHelper.addListener(helper, listener);
            }
        }

        @Override
        public void removeListener(ListChangeListener<? super T> listener) {
            synchronized (mutex) {
                helper = ListListenerHelper.removeListener(helper, listener);
            }
        }


    }

    private static class CheckedObservableList<T> extends ObservableListBase<T> implements ObservableList<T> {

        private final ObservableList<T> list;
        private final Class<T> type;
        private final ListChangeListener<T> listener;

        CheckedObservableList(ObservableList<T> list, Class<T> type) {
            if (list == null || type == null) {
                throw new NullPointerException();
            }
            this.list = list;
            this.type = type;
            listener = c -> {
                fireChange(new SourceAdapterChange<T>(CheckedObservableList.this, c));
            };
            list.addListener(new WeakListChangeListener<T>(listener));
        }

        void typeCheck(Object o) {
            if (o != null && !type.isInstance(o)) {
                throw new ClassCastException("Attempt to insert "
                        + o.getClass() + " element into collection with element type "
                        + type);
            }
        }

        @Override
        public int size() {
            return list.size();
        }

        @Override
        public boolean isEmpty() {
            return list.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return list.contains(o);
        }

        @Override
        public Object[] toArray() {
            return list.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return list.toArray(a);
        }

        @Override
        public String toString() {
            return list.toString();
        }

        @Override
        public boolean remove(Object o) {
            return list.remove(o);
        }

        @Override
        public boolean containsAll(Collection<?> coll) {
            return list.containsAll(coll);
        }

        @Override
        public boolean removeAll(Collection<?> coll) {
            return list.removeAll(coll);
        }

        @Override
        public boolean retainAll(Collection<?> coll) {
            return list.retainAll(coll);
        }

        @Override
        public boolean removeAll(T... elements) {
            return list.removeAll(elements);
        }

        @Override
        public boolean retainAll(T... elements) {
            return list.retainAll(elements);
        }

        @Override
        public void remove(int from, int to) {
            list.remove(from, to);
        }

        @Override
        public void clear() {
            list.clear();
        }

        @Override
        public boolean equals(Object o) {
            return o == this || list.equals(o);
        }

        @Override
        public int hashCode() {
            return list.hashCode();
        }

        @Override
        public T get(int index) {
            return list.get(index);
        }

        @Override
        public T remove(int index) {
            return list.remove(index);
        }

        @Override
        public int indexOf(Object o) {
            return list.indexOf(o);
        }

        @Override
        public int lastIndexOf(Object o) {
            return list.lastIndexOf(o);
        }

        @Override
        public T set(int index, T element) {
            typeCheck(element);
            return list.set(index, element);
        }

        @Override
        public void add(int index, T element) {
            typeCheck(element);
            list.add(index, element);
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean addAll(int index, Collection<? extends T> c) {
            T[] a = null;
            try {
                a = c.toArray((T[]) Array.newInstance(type, 0));
            } catch (ArrayStoreException e) {
                throw new ClassCastException();
            }

            return this.list.addAll(index, Arrays.asList(a));
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean addAll(Collection<? extends T> coll) {
            T[] a = null;
            try {
                a = coll.toArray((T[]) Array.newInstance(type, 0));
            } catch (ArrayStoreException e) {
                throw new ClassCastException();
            }

            return this.list.addAll(Arrays.asList(a));
        }

        @Override
        public ListIterator<T> listIterator() {
            return listIterator(0);
        }

        @Override
        public ListIterator<T> listIterator(final int index) {
            return new ListIterator<T>() {

                ListIterator<T> i = list.listIterator(index);

                @Override
                public boolean hasNext() {
                    return i.hasNext();
                }

                @Override
                public T next() {
                    return i.next();
                }

                @Override
                public boolean hasPrevious() {
                    return i.hasPrevious();
                }

                @Override
                public T previous() {
                    return i.previous();
                }

                @Override
                public int nextIndex() {
                    return i.nextIndex();
                }

                @Override
                public int previousIndex() {
                    return i.previousIndex();
                }

                @Override
                public void remove() {
                    i.remove();
                }

                @Override
                public void set(T e) {
                    typeCheck(e);
                    i.set(e);
                }

                @Override
                public void add(T e) {
                    typeCheck(e);
                    i.add(e);
                }
            };
        }

        @Override
        public Iterator<T> iterator() {
            return new Iterator<T>() {

                private final Iterator<T> it = list.iterator();

                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public T next() {
                    return it.next();
                }

                @Override
                public void remove() {
                    it.remove();
                }
            };
        }

        @Override
        public boolean add(T e) {
            typeCheck(e);
            return list.add(e);
        }

        @Override
        public List<T> subList(int fromIndex, int toIndex) {
            return Collections.checkedList(list.subList(fromIndex, toIndex), type);
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean addAll(T... elements) {
            try {
                T[] array = (T[]) Array.newInstance(type, elements.length);
                System.arraycopy(elements, 0, array, 0, elements.length);
                return list.addAll(array);
            } catch (ArrayStoreException e) {
                throw new ClassCastException();
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean setAll(T... elements) {
            try {
                T[] array = (T[]) Array.newInstance(type, elements.length);
                System.arraycopy(elements, 0, array, 0, elements.length);
                return list.setAll(array);
            } catch (ArrayStoreException e) {
                throw new ClassCastException();
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean setAll(Collection<? extends T> col) {
            T[] a = null;
            try {
                a = col.toArray((T[]) Array.newInstance(type, 0));
            } catch (ArrayStoreException e) {
                throw new ClassCastException();
            }

            return list.setAll(Arrays.asList(a));
        }
    }

    private static class EmptyObservableSet<E> extends AbstractSet<E> implements ObservableSet<E> {

        public EmptyObservableSet() {
        }

        @Override
        public void addListener(InvalidationListener listener) {
        }

        @Override
        public void removeListener(InvalidationListener listener) {
        }

        @Override
        public void addListener(SetChangeListener<? super E> listener) {
        }

        @Override
        public void removeListener(SetChangeListener<? super E> listener) {
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean contains(Object obj) {
            return false;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return c.isEmpty();
        }

        @Override
        public Object[] toArray() {
            return new Object[0];
        }

        @Override
        public <E> E[] toArray(E[] a) {
            if (a.length > 0)
                a[0] = null;
            return a;
        }

        @Override
        public Iterator<E> iterator() {
            return new Iterator() {

                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public Object next() {
                    throw new NoSuchElementException();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

    }

    private static class UnmodifiableObservableSet<E> extends AbstractSet<E> implements ObservableSet<E> {

        private final ObservableSet<E> backingSet;
        private SetListenerHelper<E> listenerHelper;
        private SetChangeListener<E> listener;

        public UnmodifiableObservableSet(ObservableSet<E> backingSet) {
            this.backingSet = backingSet;
            this.listener = null;
        }

        private void initListener() {
            if (listener == null) {
                listener = c -> {
                    callObservers(new SetAdapterChange<E>(UnmodifiableObservableSet.this, c));
                };
                this.backingSet.addListener(new WeakSetChangeListener<E>(listener));
            }
        }

        private void callObservers(SetChangeListener.Change<? extends E> change) {
            SetListenerHelper.fireValueChangedEvent(listenerHelper, change);
        }

        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {
                private final Iterator<? extends E> i = backingSet.iterator();

                @Override
                public boolean hasNext() {
                    return i.hasNext();
                }

                @Override
                public E next() {
                    return i.next();
                }
            };
        }

        @Override
        public int size() {
            return backingSet.size();
        }

        @Override
        public boolean isEmpty() {
            return backingSet.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return backingSet.contains(o);
        }

        @Override
        public void addListener(InvalidationListener listener) {
            initListener();
            listenerHelper = SetListenerHelper.addListener(listenerHelper, listener);
        }

        @Override
        public void removeListener(InvalidationListener listener) {
            listenerHelper = SetListenerHelper.removeListener(listenerHelper, listener);
        }

        @Override
        public void addListener(SetChangeListener<? super E> listener) {
            initListener();
            listenerHelper = SetListenerHelper.addListener(listenerHelper, listener);
        }

        @Override
        public void removeListener(SetChangeListener<? super E> listener) {
            listenerHelper = SetListenerHelper.removeListener(listenerHelper, listener);
        }

        @Override
        public boolean add(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }
    }

    private static class SynchronizedSet<E> implements Set<E> {
        final Object mutex;
        private final Set<E> backingSet;

        SynchronizedSet(Set<E> set, Object mutex) {
            this.backingSet = set;
            this.mutex = mutex;
        }

        SynchronizedSet(Set<E> set) {
            this(set, new Object());
        }

        @Override
        public int size() {
            synchronized(mutex) {
                return backingSet.size();
            }
        }

        @Override
        public boolean isEmpty() {
            synchronized(mutex) {
                return backingSet.isEmpty();
            }
        }

        @Override
        public boolean contains(Object o) {
            synchronized(mutex) {
                return backingSet.contains(o);
            }
        }

        @Override
        public Iterator<E> iterator() {
            return backingSet.iterator();
        }

        @Override
        public Object[] toArray() {
            synchronized(mutex) {
                return backingSet.toArray();
            }
        }

        @Override
        public <E> E[] toArray(E[] a) {
            synchronized(mutex) {
                return backingSet.toArray(a);
            }
        }

        @Override
        public boolean add(E e) {
            synchronized(mutex) {
                return backingSet.add(e);
            }
        }

        @Override
        public boolean remove(Object o) {
            synchronized(mutex) {
                return backingSet.remove(o);
            }
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            synchronized(mutex) {
                return backingSet.containsAll(c);
            }
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            synchronized(mutex) {
                return backingSet.addAll(c);
            }
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            synchronized(mutex) {
                return backingSet.retainAll(c);
            }
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            synchronized(mutex) {
                return backingSet.removeAll(c);
            }
        }

        @Override
        public void clear() {
            synchronized(mutex) {
                backingSet.clear();
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            synchronized(mutex) {
                return backingSet.equals(o);
            }
        }

        @Override
        public int hashCode() {
            synchronized (mutex) {
                return backingSet.hashCode();
            }
        }
    }

    private static class SynchronizedObservableSet<E> extends SynchronizedSet<E> implements ObservableSet<E> {

        private final ObservableSet<E> backingSet;
        private SetListenerHelper listenerHelper;
        private final SetChangeListener<E> listener;

        SynchronizedObservableSet(ObservableSet<E> set, Object mutex) {
            super(set, mutex);
            backingSet = set;
            listener = c -> {
                SetListenerHelper.fireValueChangedEvent(listenerHelper, new SetAdapterChange<E>(SynchronizedObservableSet.this, c));
            };
            backingSet.addListener(new WeakSetChangeListener<E>(listener));
        }

        SynchronizedObservableSet(ObservableSet<E> set) {
            this(set, new Object());
        }

        @Override
        public void addListener(InvalidationListener listener) {
            synchronized (mutex) {
                listenerHelper = SetListenerHelper.addListener(listenerHelper, listener);
            }
        }

        @Override
        public void removeListener(InvalidationListener listener) {
            synchronized (mutex) {
                listenerHelper = SetListenerHelper.removeListener(listenerHelper, listener);
            }
        }
        @Override
        public void addListener(SetChangeListener<? super E> listener) {
            synchronized (mutex) {
                listenerHelper = SetListenerHelper.addListener(listenerHelper, listener);
            }
        }

        @Override
        public void removeListener(SetChangeListener<? super E> listener) {
            synchronized (mutex) {
                listenerHelper = SetListenerHelper.removeListener(listenerHelper, listener);
            }
        }
    }

    private static class CheckedObservableSet<E> extends AbstractSet<E> implements ObservableSet<E> {

        private final ObservableSet<E> backingSet;
        private final Class<E> type;
        private SetListenerHelper listenerHelper;
        private final SetChangeListener<E> listener;

        CheckedObservableSet(ObservableSet<E> set, Class<E> type) {
            if (set == null || type == null) {
                throw new NullPointerException();
            }
            backingSet = set;
            this.type = type;
            listener = c -> {
                callObservers(new SetAdapterChange<E>(CheckedObservableSet.this, c));
            };
            backingSet.addListener(new WeakSetChangeListener<E>(listener));
        }

        private void callObservers(SetChangeListener.Change<? extends E> c) {
            SetListenerHelper.fireValueChangedEvent(listenerHelper, c);
        }

        void typeCheck(Object o) {
            if (o != null && !type.isInstance(o)) {
                throw new ClassCastException("Attempt to insert "
                        + o.getClass() + " element into collection with element type "
                        + type);
            }
        }

        @Override
        public void addListener(InvalidationListener listener) {
            listenerHelper = SetListenerHelper.addListener(listenerHelper, listener);
        }

        @Override
        public void removeListener(InvalidationListener listener) {
            listenerHelper = SetListenerHelper.removeListener(listenerHelper, listener);
        }

        @Override
        public void addListener(SetChangeListener<? super E> listener) {
            listenerHelper = SetListenerHelper.addListener(listenerHelper, listener);
        }

        @Override
        public void removeListener(SetChangeListener<? super E> listener) {
            listenerHelper = SetListenerHelper.removeListener(listenerHelper, listener);
        }

        @Override
        public int size() {
            return backingSet.size();
        }

        @Override
        public boolean isEmpty() {
            return backingSet.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return backingSet.contains(o);
        }

        @Override
        public Object[] toArray() {
            return backingSet.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return backingSet.toArray(a);
        }

        @Override
        public boolean add(E e) {
            typeCheck(e);
            return backingSet.add(e);
        }

        @Override
        public boolean remove(Object o) {
            return backingSet.remove(o);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return backingSet.containsAll(c);
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean addAll(Collection<? extends E> c) {
            E[] a = null;
            try {
                a = c.toArray((E[]) Array.newInstance(type, 0));
            } catch (ArrayStoreException e) {
                throw new ClassCastException();
            }

            return backingSet.addAll(Arrays.asList(a));
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return backingSet.retainAll(c);
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return backingSet.removeAll(c);
        }

        @Override
        public void clear() {
            backingSet.clear();
        }

        @Override
        public boolean equals(Object o) {
            return o == this || backingSet.equals(o);
        }

        @Override
        public int hashCode() {
            return backingSet.hashCode();
        }

        @Override
        public Iterator<E> iterator() {
            final Iterator<E> it = backingSet.iterator();

            return new Iterator<E>() {
                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public E next() {
                    return it.next();
                }

                @Override
                public void remove() {
                    it.remove();
                }
            };
        }

    }

    private static class EmptyObservableMap<K, V> extends AbstractMap<K, V> implements ObservableMap<K, V> {

        public EmptyObservableMap() {
        }

        @Override
        public void addListener(InvalidationListener listener) {
        }

        @Override
        public void removeListener(InvalidationListener listener) {
        }

        @Override
        public void addListener(MapChangeListener<? super K, ? super V> listener) {
        }

        @Override
        public void removeListener(MapChangeListener<? super K, ? super V> listener) {
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean containsKey(Object key) {
            return false;
        }

        @Override
        public boolean containsValue(Object value) {
            return false;
        }

        @Override
        public V get(Object key) {
            return null;
        }

        @Override
        public Set<K> keySet() {
            return emptyObservableSet();
        }

        @Override
        public Collection<V> values() {
            return emptyObservableSet();
        }

        @Override
        public Set<Map.Entry<K,V>> entrySet() {
            return emptyObservableSet();
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof Map) && ((Map<?,?>)o).isEmpty();
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }

    private static class CheckedObservableMap<K, V> extends AbstractMap<K, V> implements ObservableMap<K, V> {

        private final ObservableMap<K, V> backingMap;
        private final Class<K> keyType;
        private final Class<V> valueType;
        private MapListenerHelper listenerHelper;
        private final MapChangeListener<K, V> listener;

        CheckedObservableMap(ObservableMap<K, V> map, Class<K> keyType, Class<V> valueType) {
            backingMap = map;
            this.keyType = keyType;
            this.valueType = valueType;
            listener = c -> {
                callObservers(new MapAdapterChange<K, V>(CheckedObservableMap.this, c));
            };
            backingMap.addListener(new WeakMapChangeListener<K, V>(listener));
        }

        private void callObservers(MapChangeListener.Change<? extends K, ? extends V> c) {
            MapListenerHelper.fireValueChangedEvent(listenerHelper, c);
        }

        void typeCheck(Object key, Object value) {
            if (key != null && !keyType.isInstance(key)) {
                throw new ClassCastException("Attempt to insert "
                        + key.getClass() + " key into map with key type "
                        + keyType);
            }

            if (value != null && !valueType.isInstance(value)) {
                throw new ClassCastException("Attempt to insert "
                        + value.getClass() + " value into map with value type "
                        + valueType);
            }
        }

        @Override
        public void addListener(InvalidationListener listener) {
            listenerHelper = MapListenerHelper.addListener(listenerHelper, listener);
        }

        @Override
        public void removeListener(InvalidationListener listener) {
            listenerHelper = MapListenerHelper.removeListener(listenerHelper, listener);
        }

        @Override
        public void addListener(MapChangeListener<? super K, ? super V> listener) {
            listenerHelper = MapListenerHelper.addListener(listenerHelper, listener);
        }

        @Override
        public void removeListener(MapChangeListener<? super K, ? super V> listener) {
            listenerHelper = MapListenerHelper.removeListener(listenerHelper, listener);
        }

        @Override
        public int size() {
            return backingMap.size();
        }

        @Override
        public boolean isEmpty() {
            return backingMap.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return backingMap.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return backingMap.containsValue(value);
        }

        @Override
        public V get(Object key) {
            return backingMap.get(key);
        }

        @Override
        public V put(K key, V value) {
            typeCheck(key, value);
            return backingMap.put(key, value);
        }

        @Override
        public V remove(Object key) {
            return backingMap.remove(key);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void putAll(Map t) {
            // Satisfy the following goals:
            // - good diagnostics in case of type mismatch
            // - all-or-nothing semantics
            // - protection from malicious t
            // - correct behavior if t is a concurrent map
            Object[] entries = t.entrySet().toArray();
            List<Map.Entry<K,V>> checked =
                new ArrayList<Map.Entry<K,V>>(entries.length);
            for (Object o : entries) {
                Map.Entry<?,?> e = (Map.Entry<?,?>) o;
                Object k = e.getKey();
                Object v = e.getValue();
                typeCheck(k, v);
                checked.add(
                    new AbstractMap.SimpleImmutableEntry<K,V>((K) k, (V) v));
            }
            for (Map.Entry<K,V> e : checked)
                backingMap.put(e.getKey(), e.getValue());
        }

        @Override
        public void clear() {
            backingMap.clear();
        }

        @Override
        public Set<K> keySet() {
            return backingMap.keySet();
        }

        @Override
        public Collection<V> values() {
            return backingMap.values();
        }

        private transient Set<Map.Entry<K,V>> entrySet = null;

        @Override
        public Set entrySet() {
            if (entrySet==null)
                entrySet = new CheckedEntrySet<K,V>(backingMap.entrySet(), valueType);
            return entrySet;
        }

        @Override
        public boolean equals(Object o) {
            return o == this || backingMap.equals(o);
        }

        @Override
        public int hashCode() {
            return backingMap.hashCode();
        }

        static class CheckedEntrySet<K,V> implements Set<Map.Entry<K,V>> {
            private final Set<Map.Entry<K,V>> s;
            private final Class<V> valueType;

            CheckedEntrySet(Set<Map.Entry<K, V>> s, Class<V> valueType) {
                this.s = s;
                this.valueType = valueType;
            }

            @Override
            public int size() {
                return s.size();
            }

            @Override
            public boolean isEmpty() {
                return s.isEmpty();
            }

            @Override
            public String toString() {
                return s.toString();
            }

            @Override
            public int hashCode() {
                return s.hashCode();
            }

            @Override
            public void clear() {
                s.clear();
            }

            @Override
            public boolean add(Map.Entry<K, V> e) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean addAll(Collection<? extends Map.Entry<K, V>> coll) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Iterator<Map.Entry<K,V>> iterator() {
                final Iterator<Map.Entry<K, V>> i = s.iterator();
                final Class<V> valueType = this.valueType;

                return new Iterator<Map.Entry<K,V>>() {
                    @Override
                    public boolean hasNext() {
                        return i.hasNext();
                    }

                    @Override
                    public void remove() {
                        i.remove();
                    }

                    @Override
                    public Map.Entry<K,V> next() {
                        return checkedEntry(i.next(), valueType);
                    }
                };
            }

            @Override
            @SuppressWarnings("unchecked")
            public Object[] toArray() {
                Object[] source = s.toArray();

                /*
                 * Ensure that we don't get an ArrayStoreException even if
                 * s.toArray returns an array of something other than Object
                 */
                Object[] dest = (CheckedEntry.class.isInstance(
                    source.getClass().getComponentType()) ? source :
                                 new Object[source.length]);

                for (int i = 0; i < source.length; i++)
                    dest[i] = checkedEntry((Map.Entry<K,V>)source[i],
                                           valueType);
                return dest;
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T> T[] toArray(T[] a) {
                // We don't pass a to s.toArray, to avoid window of
                // vulnerability wherein an unscrupulous multithreaded client
                // could get his hands on raw (unwrapped) Entries from s.
                T[] arr = s.toArray(a.length==0 ? a : Arrays.copyOf(a, 0));

                for (int i=0; i<arr.length; i++)
                    arr[i] = (T) checkedEntry((Map.Entry<K,V>)arr[i],
                                              valueType);
                if (arr.length > a.length)
                    return arr;

                System.arraycopy(arr, 0, a, 0, arr.length);
                if (a.length > arr.length)
                    a[arr.length] = null;
                return a;
            }

            /**
             * This method is overridden to protect the backing set against
             * an object with a nefarious equals function that senses
             * that the equality-candidate is Map.Entry and calls its
             * setValue method.
             */
            @Override
            public boolean contains(Object o) {
                if (!(o instanceof Map.Entry))
                    return false;
                Map.Entry<?,?> e = (Map.Entry<?,?>) o;
                return s.contains(
                    (e instanceof CheckedEntry) ? e : checkedEntry(e, valueType));
            }

            /**
             * The bulk collection methods are overridden to protect
             * against an unscrupulous collection whose contains(Object o)
             * method senses when o is a Map.Entry, and calls o.setValue.
             */
            @Override
            public boolean containsAll(Collection<?> c) {
                for (Object o : c)
                    if (!contains(o)) // Invokes safe contains() above
                        return false;
                return true;
            }

            @Override
            public boolean remove(Object o) {
                if (!(o instanceof Map.Entry))
                    return false;
                return s.remove(new AbstractMap.SimpleImmutableEntry
                                <Object, Object>((Map.Entry<?,?>)o));
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                return batchRemove(c, false);
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                return batchRemove(c, true);
            }

            private boolean batchRemove(Collection<?> c, boolean complement) {
                boolean modified = false;
                Iterator<Map.Entry<K,V>> it = iterator();
                while (it.hasNext()) {
                    if (c.contains(it.next()) != complement) {
                        it.remove();
                        modified = true;
                    }
                }
                return modified;
            }

            @Override
            public boolean equals(Object o) {
                if (o == this)
                    return true;
                if (!(o instanceof Set))
                    return false;
                Set<?> that = (Set<?>) o;
                return that.size() == s.size()
                    && containsAll(that); // Invokes safe containsAll() above
            }

            static <K,V,T> CheckedEntry<K,V,T> checkedEntry(Map.Entry<K,V> e,
                                                            Class<T> valueType) {
                return new CheckedEntry<K,V,T>(e, valueType);
            }

            /**
             * This "wrapper class" serves two purposes: it prevents
             * the client from modifying the backing Map, by short-circuiting
             * the setValue method, and it protects the backing Map against
             * an ill-behaved Map.Entry that attempts to modify another
             * Map.Entry when asked to perform an equality check.
             */
            private static class CheckedEntry<K,V,T> implements Map.Entry<K,V> {
                private final Map.Entry<K, V> e;
                private final Class<T> valueType;

                CheckedEntry(Map.Entry<K, V> e, Class<T> valueType) {
                    this.e = e;
                    this.valueType = valueType;
                }

                @Override
                public K getKey() {
                    return e.getKey();
                }

                @Override
                public V getValue() {
                    return e.getValue();
                }

                @Override
                public int hashCode() {
                    return e.hashCode();
                }

                @Override
                public String toString() {
                    return e.toString();
                }

                @Override
                public V setValue(V value) {
                    if (value != null && !valueType.isInstance(value))
                        throw new ClassCastException(badValueMsg(value));
                    return e.setValue(value);
                }

                private String badValueMsg(Object value) {
                    return "Attempt to insert " + value.getClass() +
                        " value into map with value type " + valueType;
                }

                @Override
                public boolean equals(Object o) {
                    if (o == this)
                        return true;
                    if (!(o instanceof Map.Entry))
                        return false;
                    return e.equals(new AbstractMap.SimpleImmutableEntry
                                    <Object, Object>((Map.Entry<?,?>)o));
                }
            }
        }

    }

    private static class SynchronizedMap<K, V> implements Map<K, V> {
        final Object mutex;
        private final Map<K, V> backingMap;

        SynchronizedMap(Map<K, V> map, Object mutex) {
            backingMap = map;
            this.mutex = mutex;
        }

        SynchronizedMap(Map<K, V> map) {
            this(map, new Object());
        }

        @Override
        public int size() {
            synchronized (mutex) {
                return backingMap.size();
            }
        }

        @Override
        public boolean isEmpty() {
            synchronized (mutex) {
                return backingMap.isEmpty();
            }
        }

        @Override
        public boolean containsKey(Object key) {
            synchronized (mutex) {
                return backingMap.containsKey(key);
            }
        }

        @Override
        public boolean containsValue(Object value) {
            synchronized (mutex) {
                return backingMap.containsValue(value);
            }
        }

        @Override
        public V get(Object key) {
            synchronized (mutex) {
                return backingMap.get(key);
            }
        }

        @Override
        public V put(K key, V value) {
            synchronized (mutex) {
                return backingMap.put(key, value);
            }
        }

        @Override
        public V remove(Object key) {
            synchronized (mutex) {
                return backingMap.remove(key);
            }
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            synchronized (mutex) {
                backingMap.putAll(m);
            }
        }

        @Override
        public void clear() {
            synchronized (mutex) {
                backingMap.clear();
            }
        }

        private transient Set<K> keySet = null;
        private transient Set<Map.Entry<K,V>> entrySet = null;
        private transient Collection<V> values = null;

        @Override
        public Set<K> keySet() {
            synchronized(mutex) {
                if (keySet==null)
                    keySet = new SynchronizedSet<K>(backingMap.keySet(), mutex);
                return keySet;
            }
        }

        @Override
        public Collection<V> values() {
            synchronized(mutex) {
                if (values==null)
                    values = new SynchronizedCollection<V>(backingMap.values(), mutex);
                return values;
            }
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            synchronized(mutex) {
                if (entrySet==null)
                    entrySet = new SynchronizedSet<Map.Entry<K,V>>(backingMap.entrySet(), mutex);
                return entrySet;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            synchronized(mutex) {
                return backingMap.equals(o);
            }
        }

        @Override
        public int hashCode() {
            synchronized(mutex) {
                return backingMap.hashCode();
            }
        }

    }

    private static class SynchronizedCollection<E> implements Collection<E> {

        private final Collection<E> backingCollection;
        final Object mutex;

        SynchronizedCollection(Collection<E> c, Object mutex) {
            backingCollection = c;
            this.mutex = mutex;
        }

        SynchronizedCollection(Collection<E> c) {
            this(c, new Object());
        }

        @Override
        public int size() {
            synchronized (mutex) {
                return backingCollection.size();
            }
        }

        @Override
        public boolean isEmpty() {
            synchronized (mutex) {
                return backingCollection.isEmpty();
            }
        }

        @Override
        public boolean contains(Object o) {
            synchronized (mutex) {
                return backingCollection.contains(o);
            }
        }

        @Override
        public Iterator<E> iterator() {
            return backingCollection.iterator();
        }

        @Override
        public Object[] toArray() {
            synchronized (mutex) {
                return backingCollection.toArray();
            }
        }

        @Override
        public <T> T[] toArray(T[] a) {
            synchronized (mutex) {
                return backingCollection.toArray(a);
            }
        }

        @Override
        public boolean add(E e) {
            synchronized (mutex) {
                return backingCollection.add(e);
            }
        }

        @Override
        public boolean remove(Object o) {
            synchronized (mutex) {
                return backingCollection.remove(o);
            }
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            synchronized (mutex) {
                return backingCollection.containsAll(c);
            }
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            synchronized (mutex) {
                return backingCollection.addAll(c);
            }
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            synchronized (mutex) {
                return backingCollection.removeAll(c);
            }
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            synchronized (mutex) {
                return backingCollection.retainAll(c);
            }
        }

        @Override
        public void clear() {
            synchronized (mutex) {
                backingCollection.clear();
            }
        }
    }

    private static class SynchronizedObservableMap<K, V> extends SynchronizedMap<K, V> implements ObservableMap<K, V> {

        private final ObservableMap<K, V> backingMap;
        private MapListenerHelper listenerHelper;
        private final MapChangeListener<K, V> listener;

        SynchronizedObservableMap(ObservableMap<K, V> map, Object mutex) {
            super(map, mutex);
            backingMap = map;
            listener = c -> {
                MapListenerHelper.fireValueChangedEvent(listenerHelper, new MapAdapterChange<K, V>(SynchronizedObservableMap.this, c));
            };
            backingMap.addListener(new WeakMapChangeListener<K, V>(listener));
        }

        SynchronizedObservableMap(ObservableMap<K, V> map) {
            this(map, new Object());
        }

        @Override
        public void addListener(InvalidationListener listener) {
            synchronized (mutex) {
                listenerHelper = MapListenerHelper.addListener(listenerHelper, listener);
            }
        }

        @Override
        public void removeListener(InvalidationListener listener) {
            synchronized (mutex) {
                listenerHelper = MapListenerHelper.removeListener(listenerHelper, listener);
            }
        }

        @Override
        public void addListener(MapChangeListener<? super K, ? super V> listener) {
            synchronized (mutex) {
                listenerHelper = MapListenerHelper.addListener(listenerHelper, listener);
            }
        }

        @Override
        public void removeListener(MapChangeListener<? super K, ? super V> listener) {
            synchronized (mutex) {
                listenerHelper = MapListenerHelper.removeListener(listenerHelper, listener);
            }
        }

    }

}
