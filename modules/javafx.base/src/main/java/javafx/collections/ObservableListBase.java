/*
 * Copyright (c) 2010, 2023, Oracle and/or its affiliates. All rights reserved.
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
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javafx.beans.InvalidationListener;

/**
 * Abstract class that serves as a base class for {@link ObservableList} implementations.
 * The base class provides two functionalities for the implementing classes.
 * <ul>
 * <li> Listener handling by implementing {@code addListener} and {@code removeListener} methods.
 *      {@link #fireChange(javafx.collections.ListChangeListener.Change)  } method is provided
 *      for notifying the listeners with a {@code Change} object.
 * <li> Methods for building up a {@link ListChangeListener.Change} object. There are various methods called
 *      {@code next*}, like {@link #nextAdd(int, int) } for new items in the lists or {@link #nextRemove(int, java.lang.Object) } for
 *      an item being removed from the list.
 *      <p><strong>These methods must be always enclosed in {@link #beginChange() } and {@link #endChange() } block.</strong>
 *      <p>See the example below.
 * </ul>
 *
 * The following example shows how the Change build-up works:
 * <pre>
 *  <strong>public void</strong> removeOddIndexes() {
 *      beginChange();
 *      try {
 *          for (<strong>int</strong> i = 1; i &lt; size(); ++i) {
 *              remove(i);
 *          }
 *      } finally {
 *          endChange();
 *      }
 *  }
 *
 *  <strong>public void</strong> remove(<strong>int</strong> i) {
 *      beginChange();
 *      try {
 *          <strong>E</strong> removed = ... //do some stuff that will actually remove the element at index i
 *          nextRemove(i, removed);
 *      } finally {
 *          endChange();
 *      }
 *  }
 *
 * </pre>
 *
 * The {@code try}/{@code finally} blocks in the example are needed only if there's a possibility for an exception to occur
 * inside a {@code beginChange()} / {@code endChange()} block
 *
 * <p>
 * Note: If you want to create modifiable {@link ObservableList} implementation, consider
 * using {@link ModifiableObservableListBase} as a superclass.
 * <p>
 * Note: In order to create list with sequential access, you should override {@link #listIterator()},
 * {@link #iterator() } methods and use them in {@link #get}, {@link #size()} and other methods accordingly.
 *
 * @param <E> the type of the elements contained in the List
 * @see ObservableList
 * @see ListChangeListener.Change
 * @see ModifiableObservableListBase
 * @since JavaFX 8.0
 */
public abstract class ObservableListBase<E> extends AbstractList<E>  implements ObservableList<E> {

    private ListListenerHelper<E> listenerHelper;
    private ListChangeBuilder<E> changeBuilder;

    /**
     * Creates a default {@code ObservableListBase}.
     */
    public ObservableListBase() {
    }

    /**
     * Adds a new update operation to the change.
     * <p><strong>Note</strong>: needs to be called inside {@code beginChange()} / {@code endChange()} block.
     * <p><strong>Note</strong>: needs to reflect the <em>current</em> state of the list.
     * @param pos the position in the list where the updated element resides.
     */
    protected final void nextUpdate(int pos) {
        getListChangeBuilder().nextUpdate(pos);
    }

    /**
     * Adds a new set operation to the change.
     * Equivalent to {@code nextRemove(idx); nextAdd(idx, idx + 1); }.
     * <p><strong>Note</strong>: needs to be called inside {@code beginChange()} / {@code endChange()} block.
     * <p><strong>Note</strong>: needs to reflect the <em>current</em> state of the list.
     * @param idx the index of the item that was set
     * @param old the old value at the {@code idx} position.
     */
    protected final void nextSet(int idx, E old) {
        getListChangeBuilder().nextSet(idx, old);
    }

    /**
     * Adds a new replace operation to the change.
     * Equivalent to {@code nextRemove(from, removed); nextAdd(from, to); }
     * <p><strong>Note</strong>: needs to be called inside {@code beginChange()} / {@code endChange()} block.
     * <p><strong>Note</strong>: needs to reflect the <em>current</em> state of the list.
     * @param from the index where the items were replaced
     * @param to the end index (exclusive) of the range where the new items reside
     * @param removed the list of items that were removed
     */
    protected final void nextReplace(int from, int to, List<? extends E> removed) {
        getListChangeBuilder().nextReplace(from, to, removed);
    }

    /**
     * Adds a new remove operation to the change with multiple items removed.
     * <p><strong>Note</strong>: needs to be called inside {@code beginChange()} / {@code endChange()} block.
     * <p><strong>Note</strong>: needs to reflect the <em>current</em> state of the list.
     * @param idx the index where the items were removed
     * @param removed the list of items that were removed
     */
    protected final void nextRemove(int idx, List<? extends E> removed) {
        getListChangeBuilder().nextRemove(idx, removed);
    }

    /**
     * Adds a new remove operation to the change with single item removed.
     * <p><strong>Note</strong>: needs to be called inside {@code beginChange()} / {@code endChange()} block.
     * <p><strong>Note</strong>: needs to reflect the <em>current</em> state of the list.
     * @param idx the index where the item was removed
     * @param removed the item that was removed
     */
    protected final void nextRemove(int idx, E removed) {
        getListChangeBuilder().nextRemove(idx, removed);
    }

    /**
     * Adds a new permutation operation to the change.
     * The permutation on index {@code "i"} contains the index, where the item from the index {@code "i"} was moved.
     * <p>It's not necessary to provide the smallest permutation possible. It's correct to always call this method
     * with {@code nextPermutation(0, size(), permutation); }
     * <p><strong>Note</strong>: needs to be called inside {@code beginChange()} / {@code endChange()} block.
     * <p><strong>Note</strong>: needs to reflect the <em>current</em> state of the list.
     * @param from marks the beginning (inclusive) of the range that was permutated
     * @param to marks the end (exclusive) of the range that was permutated
     * @param perm the permutation in that range. Even if {@code from != 0}, the array should
     * contain the indexes of the list. Therefore, such permutation would not contain indexes of range {@code (0, from)}
     */
    protected final void nextPermutation(int from, int to, int[] perm) {
        getListChangeBuilder().nextPermutation(from, to, perm);
    }

    /**
     * Adds a new add operation to the change.
     * There's no need to provide the list of added items as they can be found directly in the list
     * under the specified indexes.
     * <p><strong>Note</strong>: needs to be called inside {@code beginChange()} / {@code endChange()} block.
     * <p><strong>Note</strong>: needs to reflect the <em>current</em> state of the list.
     * @param from marks the beginning (inclusive) of the range that was added
     * @param to marks the end (exclusive) of the range that was added
     */
    protected final void nextAdd(int from, int to) {
        getListChangeBuilder().nextAdd(from, to);
    }

    /**
     * Begins a change block.
     *
     * Must be called before any of the {@code next*} methods is called.
     * For every {@code beginChange()}, there must be a corresponding {@link #endChange() } call.
     * <p>{@code beginChange()} calls can be nested in a {@code beginChange()}/{@code endChange()} block.
     *
     * @see #endChange()
     */
    protected final void beginChange() {
        getListChangeBuilder().beginChange();
    }

    /**
     * Ends the change block.
     *
     * If the block is the outer-most block for the {@code ObservableList}, the
     * {@code Change} is constructed and all listeners are notified.
     * <p> Ending a nested block doesn't fire a notification.
     *
     * @see #beginChange()
     */
    protected final void endChange() {
        getListChangeBuilder().endChange();
    }

    private ListChangeBuilder<E> getListChangeBuilder() {
        if (changeBuilder == null) {
            changeBuilder = new ListChangeBuilder<>(this);
        }

        return changeBuilder;
    }

    @Override
    public final void addListener(InvalidationListener listener) {
        listenerHelper = ListListenerHelper.addListener(listenerHelper, listener);
    }

    @Override
    public final void removeListener(InvalidationListener listener) {
        listenerHelper = ListListenerHelper.removeListener(listenerHelper, listener);
    }

    @Override
    public final void addListener(ListChangeListener<? super E> listener) {
        listenerHelper = ListListenerHelper.addListener(listenerHelper, listener);
    }

    @Override
    public final void removeListener(ListChangeListener<? super E> listener) {
        listenerHelper = ListListenerHelper.removeListener(listenerHelper, listener);
    }

    /**
     * Notifies all listeners of a change
     * @param change an object representing the change that was done
     */
    protected final void fireChange(ListChangeListener.Change<? extends E> change) {
        ListListenerHelper.fireValueChangedEvent(listenerHelper, change);
    }

    /**
     * Returns true if there are some listeners registered for this list.
     * @return true if there is a listener for this list
     */
    protected final boolean hasListeners() {
        return ListListenerHelper.hasListeners(listenerHelper);
    }

    @Override
    public boolean addAll(E... elements) {
        return addAll(Arrays.asList(elements));
    }

    @Override
    public boolean setAll(E... elements) {
        return setAll(Arrays.asList(elements));
    }

    @Override
    public boolean setAll(Collection<? extends E> col) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(E... elements) {
        return removeAll(Arrays.asList(elements));
    }

    @Override
    public boolean retainAll(E... elements) {
        return retainAll(Arrays.asList(elements));
    }

    @Override
    public void remove(int from, int to) {
        removeRange(from, to);
    }
}
