/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.sun.javafx.binding.ListenerList;
import com.sun.javafx.binding.ListenerListBase;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

public class ListenerListBaseTest {
    private final InvalidationListener il1 = obs -> {};
    private final ChangeListener<?> cl1 = (obs, o, n) -> {};
    private final InvalidationListener il2 = obs -> {};
    private final ChangeListener<?> cl2 = (obs, o, n) -> {};

    private static class AccessibleListenerListBase extends ListenerListBase {
        AccessibleListenerListBase(Object listener1, Object listener2) {
            super(listener1, listener2);
        }

        public void accessibleLock() {
            lock();
        }

        public boolean accessibleUnlock() {
            return unlock();
        }

        public Object get(int index) {
            return index < invalidationListenersSize() ? getInvalidationListener(index) : getChangeListener(index - invalidationListenersSize());
        }
    }

    @Test
    void shouldConstructListWithInvalidationBeforeChangeListeners() {
        AccessibleListenerListBase list = new AccessibleListenerListBase(cl1, il1);

        assertEquals(il1, list.get(0));
        assertEquals(cl1, list.get(1));

        list = new AccessibleListenerListBase(il1, cl1);

        assertEquals(il1, list.get(0));
        assertEquals(cl1, list.get(1));
    }

    @Test
    void shouldRejectAddNullListeners() {
        AccessibleListenerListBase list = new AccessibleListenerListBase(cl1, il1);

        assertThrows(NullPointerException.class, () -> list.add(null));
    }

    @Test
    void shouldRejectUnlockedWhenNotLocked() {
        AccessibleListenerListBase list = new AccessibleListenerListBase(cl1, il1);

        assertThrows(AssertionError.class, () -> list.accessibleUnlock());

        list.accessibleLock();
        list.accessibleUnlock();

        assertThrows(AssertionError.class, () -> list.accessibleUnlock());
    }

    @Test
    void shouldRejectLockWhenLocked() {
        AccessibleListenerListBase list = new AccessibleListenerListBase(cl1, il1);

        list.accessibleLock();

        assertThrows(AssertionError.class, () -> list.accessibleLock());

        list.accessibleUnlock();
        list.accessibleLock();
    }

    @Test
    void shouldAllowRemovingAllListeners() {
        ListenerList<?> list = new ListenerList<>(cl1, il1);

        assertEquals(1, list.invalidationListenersSize());
        assertEquals(1, list.changeListenersSize());
        assertEquals(2, list.totalListeners());
        assertTrue(list.hasChangeListeners());

        list.remove(cl1);

        assertEquals(1, list.invalidationListenersSize());
        assertEquals(0, list.changeListenersSize());
        assertEquals(1, list.totalListeners());
        assertFalse(list.hasChangeListeners());

        list.remove(il1);

        assertEquals(0, list.invalidationListenersSize());
        assertEquals(0, list.changeListenersSize());
        assertEquals(0, list.totalListeners());
        assertFalse(list.hasChangeListeners());

        list.remove(cl1);
        list.remove(il1);
    }

    @Test
    void shouldUpdateTotalListenersWhileLocked() {
        AccessibleListenerListBase list = new AccessibleListenerListBase(cl1, il1);

        list.accessibleLock();

        assertEquals(1, list.invalidationListenersSize());
        assertEquals(1, list.changeListenersSize());
        assertEquals(2, list.totalListeners());
        assertTrue(list.hasChangeListeners());

        list.remove(cl1);

        assertEquals(1, list.invalidationListenersSize());
        assertEquals(1, list.changeListenersSize());
        assertEquals(1, list.totalListeners());
        assertTrue(list.hasChangeListeners());

        list.remove(il1);

        assertEquals(1, list.invalidationListenersSize());
        assertEquals(1, list.changeListenersSize());
        assertEquals(0, list.totalListeners());
        assertTrue(list.hasChangeListeners());

        list.accessibleUnlock();

        assertEquals(0, list.invalidationListenersSize());
        assertEquals(0, list.changeListenersSize());
        assertEquals(0, list.totalListeners());
        assertFalse(list.hasChangeListeners());
    }

    @Test
    void shouldNeverSilentlyRemoveWeakListeners() {

        /*
         * Weak listeners should only ever be removed as part of a
         * add or remove listener call (although more listeners can be removed
         * than just the given one during such a call). It should never
         * happen during the unlock operation as the listener count then
         * changes unexpectedly. This is also the behavior of the old
         * ExpressionHelper.
         *
         * WeakListeners that remove themselves during notification are
         * not a problem as this always goes through a removeListener call.
         */

        AccessibleListenerListBase list = new AccessibleListenerListBase(cl1, il1);
        WeakInvalidationListener weakListener = new WeakInvalidationListener("");

        list.add(weakListener);  // add the listener that should NOT be removed during cleanup

        list.accessibleLock();

        // make a change to the list so it will trigger a cleanup during unlock
        list.remove(cl1);  // nulls out cl1, forcing a cleanup during unlock

        weakListener.setGarbageCollected(true);

        list.accessibleUnlock();

        // Ensure the unlock did not remove the weak listener even though it is no longer needed
        assertEquals(2, list.invalidationListenersSize());
        assertEquals(0, list.changeListenersSize());
        assertEquals(2, list.totalListeners());
        assertListeners(list, List.of(il1, weakListener));

        for(int i = 0; i < 100; i++) {
            list.add(il2);
        }

        assertEquals(101, list.totalListeners());  // Not 102 as weak listener was removed during a compaction step
    }

    @Test
    void hasChangeListenersShouldReturnCorrectState() {
        ListenerList<?> list = new ListenerList<>(cl1, il1);

        assertTrue(list.hasChangeListeners());

        list.remove(cl1);

        assertFalse(list.hasChangeListeners());

        ListenerList<?> list2 = new ListenerList<>(il1, il2);

        assertFalse(list2.hasChangeListeners());
    }

    @Test
    void hasChangeListenersShouldReturnCorrectStateWhileLocked_2() {
        AccessibleListenerListBase list = new AccessibleListenerListBase(il1, il2);

        list.accessibleLock();

        assertFalse(list.hasChangeListeners());  // TODO is this what we want???? This is mainly tracked so we know when to update/clear old value...

        list.add(cl1);

        assertFalse(list.hasChangeListeners());
    }

    @Test
    void shouldKeepInvalidationAndChangeListenersSeparated() {
        AccessibleListenerListBase list = new AccessibleListenerListBase(cl1, il1);
        Random rnd = new Random(1);

        for(int i = 0; i < 10000; i++) {
            list.add(rnd.nextBoolean() ? il1 : cl1);
        }

        assertEquals(10002, list.invalidationListenersSize() + list.changeListenersSize());
        assertListenerSeparation(list);
    }

    @Test
    void shouldKeepInvalidationAndChangeListenersSeparatedAndInOrderWhenSomeAreRemovedOrExpired() {
        Random rnd = new Random(1);
        List<Object> addedListeners = new ArrayList<>();
        AccessibleListenerListBase list = new AccessibleListenerListBase(new WeakInvalidationListener("0"), new WeakChangeListener("1"));

        addedListeners.add(list.get(0));
        addedListeners.add(list.get(1));

        for(int i = 2; i < 10000; i++) {
            Object listener = rnd.nextBoolean() ? new WeakInvalidationListener("" + i) : new WeakChangeListener("" + i);

            addedListeners.add(listener);
            list.add(listener);

            double nextDouble = rnd.nextDouble();

            if(nextDouble < 0.05) {
                list.remove(addedListeners.remove(rnd.nextInt(addedListeners.size())));
            }
            else if(nextDouble < 0.15) {
                if(list.get(rnd.nextInt(list.invalidationListenersSize() + list.changeListenersSize())) instanceof SettableWeakListener swl) {
                    swl.setGarbageCollected(true);
                }
            }
        }

        // the exact size is not known, as it would depend on how many listeners have expired and
        // when the last resize was done to clean expired elements

        assertListenerSeparation(list);

        List<Object> separatedListeners =
            Stream.concat(
                addedListeners.stream().filter(InvalidationListener.class::isInstance),
                addedListeners.stream().filter(ChangeListener.class::isInstance)
            )
            .toList();

        for(int i = 0; i < list.invalidationListenersSize() + list.changeListenersSize(); i++) {
            Object listener = list.get(i);

            assertTrue(separatedListeners.contains(listener));

            // assert that all indices are smaller than the ones in the separated added listeners, as some
            // may have been garbage collected
            assertTrue(i <= separatedListeners.indexOf(listener), i + " should be <= " + separatedListeners.indexOf(listener));
        }
    }

    @Test
    void shouldKeepInvalidationAndChangeListenersSeparatedAndInOrderWhileLockedAndWhenSomeAreRemoved() {
        Random rnd = new Random(1);
        List<Object> addedListeners = new ArrayList<>();
        AccessibleListenerListBase list = new AccessibleListenerListBase(new WeakInvalidationListener("0"), new WeakChangeListener("1"));

        addedListeners.add(list.get(0));
        addedListeners.add(list.get(1));

        // Add some listeners first before the lock:
        for(int i = 2; i < 1000; i++) {
            Object listener = rnd.nextBoolean() ? new WeakInvalidationListener("" + i) : new WeakChangeListener("" + i);

            addedListeners.add(listener);
            list.add(listener);
        }

        list.accessibleLock();

        for(int i = 1000; i < 10000; i++) {
            Object listener = rnd.nextBoolean() ? new WeakInvalidationListener("" + i) : new WeakChangeListener("" + i);

            addedListeners.add(listener);
            list.add(listener);

            double nextDouble = rnd.nextDouble();

            if(nextDouble < 0.05) {
                list.remove(addedListeners.remove(rnd.nextInt(addedListeners.size())));
            }
            else if(nextDouble < 0.15) {
                if(addedListeners.get(rnd.nextInt(addedListeners.size())) instanceof SettableWeakListener swl) {
                    swl.setGarbageCollected(true);
                }
            }
        }

        list.accessibleUnlock();

        // the exact size is not known, as it would depend on how many listeners have expired and
        // when the last resize was done to clean expired elements

        assertListenerSeparation(list);

        List<Object> separatedListeners =
            Stream.concat(
                addedListeners.stream().filter(InvalidationListener.class::isInstance),
                addedListeners.stream().filter(ChangeListener.class::isInstance)
            )
            .toList();

        int skips = 0;

        for(int i = 0; i < list.invalidationListenersSize() + list.changeListenersSize(); i++) {
            Object listener = list.get(i);

            if (listener == null) {
                skips++;
                continue;
            }

            assertTrue(separatedListeners.contains(listener), "expected " + listener + " to be have been added before");

            // assert that all indices are smaller than the ones in the separated added listeners, as some
            // may have been garbage collected; don't count nulls
            assertTrue(i - skips <= separatedListeners.indexOf(listener), i + " - " + skips + " should be <= " + separatedListeners.indexOf(listener));
        }

        // Add some more listeners after unlock to trigger compaction:
        for(int i = 10000; i < 20000; i++) {
            Object listener = rnd.nextBoolean() ? new WeakInvalidationListener("" + i) : new WeakChangeListener("" + i);

            addedListeners.add(listener);
            list.add(listener);
        }

        assertListenerSeparation(list);

        separatedListeners =
            Stream.concat(
                addedListeners.stream().filter(InvalidationListener.class::isInstance),
                addedListeners.stream().filter(ChangeListener.class::isInstance)
            )
            .filter(l -> l instanceof WeakListener wl && !wl.wasGarbageCollected())
            .collect(Collectors.toCollection(ArrayList::new));  // going to modify it in a bit

        // after compaction, can now assert exact order and contents:
        assertListeners(list, separatedListeners);

        // remove many listeners to trigger a shrink:
        for(int i = 0; i < 10000; i++) {
            list.remove(separatedListeners.remove(rnd.nextInt(separatedListeners.size())));
        }

        // verify again:
        assertListeners(list, separatedListeners);
    }

    @Test
    void additionsShouldNotShowUpWhileLocked() {
        AccessibleListenerListBase list = new AccessibleListenerListBase(cl1, il1);

        assertEquals(1, list.invalidationListenersSize());
        assertEquals(1, list.changeListenersSize());
        assertEquals(2, list.totalListeners());

        list.accessibleLock();

        list.add(il2);
        list.add(cl2);

        assertEquals(1, list.invalidationListenersSize());
        assertEquals(1, list.changeListenersSize());
        assertEquals(4, list.totalListeners());
        assertEquals(il1, list.get(0));
        assertEquals(cl1, list.get(1));
        assertThrows(AssertionError.class, () -> list.get(2));  // reject attempts to bypass lock

        list.accessibleUnlock();

        assertEquals(2, list.invalidationListenersSize());
        assertEquals(2, list.changeListenersSize());
        assertEquals(4, list.totalListeners());

        assertListeners(list, List.of(il1, il2, cl1, cl2));
    }

    @Test
    void removalsShouldBecomeNullsWhileLocked() {
        AccessibleListenerListBase list = new AccessibleListenerListBase(cl1, il1);

        assertEquals(1, list.invalidationListenersSize());
        assertEquals(1, list.changeListenersSize());
        assertEquals(2, list.totalListeners());

        list.add(il2);
        list.add(cl2);

        assertEquals(2, list.invalidationListenersSize());
        assertEquals(2, list.changeListenersSize());
        assertEquals(4, list.totalListeners());
        assertListeners(list, List.of(il1, il2, cl1, cl2));

        list.accessibleLock();

        assertEquals(2, list.invalidationListenersSize());
        assertEquals(2, list.changeListenersSize());
        assertEquals(4, list.totalListeners());
        assertListeners(list, List.of(il1, il2, cl1, cl2));

        list.remove(il1);

        assertEquals(2, list.invalidationListenersSize());
        assertEquals(2, list.changeListenersSize());
        assertEquals(3, list.totalListeners());
        assertListeners(list, List.of(il2, cl1, cl2));

        list.remove(cl1);

        assertEquals(2, list.invalidationListenersSize());
        assertEquals(2, list.changeListenersSize());
        assertEquals(2, list.totalListeners());
        assertListeners(list, List.of(il2, cl2));

        assertEquals(null, list.get(0));
        assertEquals(il2, list.get(1));
        assertEquals(null, list.get(2));
        assertEquals(cl2, list.get(3));

        list.accessibleUnlock();

        assertEquals(1, list.invalidationListenersSize());
        assertEquals(1, list.changeListenersSize());
        assertEquals(2, list.totalListeners());
        assertListeners(list, List.of(il2, cl2));
    }

    private void assertListenerSeparation(AccessibleListenerListBase list) {
        boolean foundChange = false;

        for(int i = 0; i < list.invalidationListenersSize() + list.changeListenersSize(); i++) {
            if(list.get(i) instanceof ChangeListener) {
                foundChange = true;
            }
            else if(foundChange && list.get(i) != null) {
                fail("Found an invalidation listener at index " + i + " after a change listener was seen");
            }
        }
    }

    private void assertListeners(AccessibleListenerListBase list, List<Object> expectedListeners) {
        assertListenerSeparation(list);

        int j = 0;
        int x = 0;

        for(int i = 0; i < list.invalidationListenersSize() + list.changeListenersSize(); i++) {
            Object listener = list.get(i);

            if(listener == null) {
                x++;
                continue;
            }

            if(j >= expectedListeners.size()) {
                fail("Listener at index " + i + " (with " + x + " nulls skipped) is listener " + (i - x) + ", but only " + expectedListeners.size() + " were expected: " + listener);
            }

            assertEquals(expectedListeners.get(j), listener, "Listener at index " + i + " (with " + x + " nulls skipped) did not match listener at index " + j);
            j++;
        }

        if(j != expectedListeners.size()) {
            fail("Only " + j + " listeners were found, but " + expectedListeners.size() + " were expected");
        }
    }

    interface SettableWeakListener extends WeakListener {
        void setGarbageCollected(boolean gc);
    }

    static class WeakInvalidationListener implements InvalidationListener, SettableWeakListener {
        private final String name;

        private boolean wasGarbageCollected;

        public WeakInvalidationListener(String name) {
            this.name = name;
        }

        @Override
        public void setGarbageCollected(boolean gc) {
            this.wasGarbageCollected = gc;
        }

        @Override
        public boolean wasGarbageCollected() {
            return wasGarbageCollected;
        }

        @Override
        public void invalidated(Observable observable) {
        }

        @Override
        public String toString() {
            return "IL[" + name + "]";
        }
    }

    static class WeakChangeListener implements ChangeListener<Object>, SettableWeakListener {
        private final String name;

        private boolean wasGarbageCollected;

        public WeakChangeListener(String name) {
            this.name = name;
        }

        @Override
        public void setGarbageCollected(boolean gc) {
            this.wasGarbageCollected = gc;
        }

        @Override
        public boolean wasGarbageCollected() {
            return wasGarbageCollected;
        }

        @Override
        public void changed(ObservableValue<? extends Object> observable, Object oldValue, Object newValue) {
        }

        @Override
        public String toString() {
            return "CL[" + name + "]";
        }
    }
}
