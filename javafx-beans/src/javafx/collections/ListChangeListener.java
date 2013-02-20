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

import java.util.Collections;
import java.util.List;

/**
 * Interface that receives notifications of changes to an ObservableList.
 *
 * @param <E> the list element type
 * @see Change
 */
public interface ListChangeListener<E> {

    /**
     * Represents a report of a change done to an Observablelist.<br>
     * <br>
     * The {@code getRemoved()} method returns a list of elements that have been
     * replaced or removed from the list.<br>
     *<br>
     * The range {@code [getFrom(), getTo())} is the range of elements
     * in the list that contain new elements. Note that this is a half-open
     * interval, so if the range is empty, addedFrom will be equal to
     * addedTo. This will occur, for example, if the operation was a
     * deletion and will mark the index where the deletion happened<br>
     * It is possible to get a list of added elements by calling getAddedSubList().<br>
     *<br>
     * The permutation and updated flags are exclusive with all other states.
     * If a list was permutated/updated, no content was removed or added at the same time and the indexes marks the range
     * of permutated items or the position of updated element<br>
     * Typical usage is to observe changes on an ObservableList in order
     * to hook or unhook (or add or remove a listener) or in order to maintain
     * some invariant on every element in that ObservableList. A common code
     * pattern for doing this looks something like the following:<br>
     *<br>
     *<br>
     * <pre>{@code
     * ObservableList<Item> theList = ...;
     * {
     *     theList.addListener(new ListChangeListener<Item>() {
     *         public void onChanged(Change<Item> c) {
     *             while (c.next()) {
     *                 if (c.wasPermutated()) {
     *                     for (int i = c.getFrom(); i < c.getTo(); ++i) {
     *                          //permutate
     *                     }
     *                 } else if (c.wasUpdated()) {
     *                          //update item
     *                 } else {
     *                     for (Item remitem : c.getRemoved()) {
     *                         remitem.remove(Outer.this);
     *                     }
     *                     for (Item additem : c.getAddedSubList()) {
     *                         additem.add(Outer.this);
     *                     }
     *                 }
     *             }
     *         }
     *     });
     * }
     * }</pre>
     * 
     * <b>Warning:</b> This class directly accesses the source list to acquire information about the changes.
     * <br> This effectively makes the Change object invalid when another change occurs on the list.
     * <br> For this reason it is <b>not</b> safe to use this class on a different thread.
     * 
     * @param <E> the list element type
     */
    public abstract static class Change<E> {
        private final ObservableList<E> list;
        
        /**
         * Go to the next change.
         * In initial state is invalid a require a call to next() before
         * calling other methods. The first next() call will make this object
         * represent the first change.
         * @return true if switched to the next change, false if this is the last change.
         */
        public abstract boolean next();

        /**
         * Reset to the initial stage. After this call, the next() must be called
         * before working with the first change.
         */
        public abstract void reset();
        
        /**
         * Constructs a new change done to a list.
         * @param list that was changed
         */
        public Change(ObservableList<E> list) {
            this.list = list;
        }

        /**
         * The source list of the change.
         * @return a list that was changed
         */
        public ObservableList<E> getList() {
            return list;
        }

        /**
         * If wasAdded is true, the interval contains all the values that were added.
         * If wasPermutated is true, the interval marks the values that were permutated.
         * If wasRemoved is true and wasAdded is false, getFrom() and getTo() should
         * return the same number - the place where the removed elements were positioned in the list.
         * @return a beginning (inclusive) of an interval related to the change
         * @throws IllegalStateException if this Change is in initial state
         */
        public abstract int getFrom();
        /**
         * The end of the change interval.
         * @return a end (exclusive) of an interval related to the change.
         * @throws IllegalStateException if this Change is in initial state
         * @see #getFrom() 
         */
        public abstract int getTo();
        /**
         * An immutable list of removed/replaced elements. If no elements
         * were removed from the list, an empty list is returned.
         * @return a list with all the removed elements
         * @throws IllegalStateException if this Change is in initial state
         */
        public abstract List<E> getRemoved();
        /**
         * Indicates if the change was only a permutation.
         * @return true if the change was just a permutation.
         * @throws IllegalStateException if this Change is in initial state
         */
        public boolean wasPermutated() {
            return getPermutation().length != 0;
        }

        /**
         * Indicates if elements were added during this change
         * @return true if something was added to the list
         * @throws IllegalStateException if this Change is in initial state
         */
        public boolean wasAdded() {
            return !wasPermutated() && !wasUpdated() && getFrom() < getTo();
        }

        /**
         * Indicates if elements were removed during this change.
         * Note that using set will also produce a change with wasRemoved() returning
         * true. See {@link #wasReplaced()}.
         * @return true if something was removed from the list
         * @throws IllegalStateException if this Change is in initial state
         */
        public boolean wasRemoved() {
            return !getRemoved().isEmpty();
        }

        /**
         * Indicates if elements were replaced during this change.
         * This is usually true when set is called on the list.
         * Set operation will act like remove and add operation at the same time.
         * <p>
         * Usually, it's not necessary to use this method directly.
         * Handling remove operation and then add operation, as in the example {@link ListChangeListener$Change above},
         * will effectively handle also set operation.
         *
         * @return same <code> as wasAdded() && wasRemoved() </code>
         * @throws IllegalStateException if this Change is in initial state
         */
        public boolean wasReplaced() {
            return wasAdded() && wasRemoved();
        }
        
        /**
         * Indicates that the elements between getFrom() (inclusive)
         * to getTo() exclusive has changed.
         * This is the only optional event type and may not be
         * fired by all ObservableLists.
         * @return true if the current change is an update change.
         * @since 2.1
         */
        public boolean wasUpdated() {
            return false;
        }
        
        /**
         * To get a subList view of the list that contains only the elements
         * added, use getAddedSubList() method.
         * This is actually a shortcut to <code>c.getList().subList(c.getFrom(), c.getTo());</code><br>
         *
         * <pre><code>
         * for (Node n : change.getAddedSubList()) {
         *       // do something
         * }
         * </code></pre>
         * @return the newly created sublist view that contains all the added elements.
         * @throws IllegalStateException if this Change is in initial state
         */
        public List<E> getAddedSubList() {
            return wasAdded()? getList().subList(getFrom(), getTo()) : Collections.<E>emptyList();
        }

        /**
         * Size of getRemoved() list.
         * @return the number of removed items
         * @throws IllegalStateException if this Change is in initial state
         */
        public int getRemovedSize() {
            return getRemoved().size();
        }

        /**
         * Size of the interval that was added.
         * @return the number of added items
         * @throws IllegalStateException if this Change is in initial state
         */
        public int getAddedSize() {
            return wasAdded() ? getTo() - getFrom() : 0;
        }
        
        /**
         * If this change is an permutation, it returns an integer array
         * that describes the permutation.
         * This array maps directly from the previous indexes to the new ones.
         * This method is not publicly accessible and therefore can return an array safely.
         * The method is used by {@link #wasPermutated() } and {@link #getPermutation(int)} methods.
         * @return empty array if this is not permutation or an integer array containing the permutation 
         * @throws IllegalStateException if this Change is in initial state
         */
        protected abstract int[] getPermutation();
        
        /**
         * By calling these method, you can observe the permutation that happened.
         * In order to get the new position of an element, you must call:
         * <pre>
         *    change.getPermutation()(oldIndex);
         * </pre>
         *  
         * Note: default implementation of this method takes the information
         * from {@link #getPermutation()} method. You don't have to override this method.
         * @param i the old index that contained the element prior to this change
         * @return the new index of the same element
         */
        public int getPermutation(int i) {
            return getPermutation()[i - getFrom()];
        }

    }
    /**
     * Called after a change has been made to an ObservableList.
     *
     * @param c an object representing the change that was done
     * @see Change
     */
    public void onChanged(Change<? extends E> c);
}
