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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.sun.javafx.binding.ListenerList;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

public class ListenerListTest {
    private final InvalidationListener il1 = obs -> {};
    private final ChangeListener<?> cl1 = (obs, o, n) -> {};

    private final ListenerList list = new ListenerList(cl1, il1);

    @Test
    void shouldConstructListWithInvalidationBeforeChangeListeners() {
        ListenerList list = new ListenerList(cl1, il1);

        assertEquals(il1, list.get(0));
        assertEquals(cl1, list.get(1));

        list = new ListenerList(il1, cl1);

        assertEquals(il1, list.get(0));
        assertEquals(cl1, list.get(1));
    }

    @Test
    void shouldRejectAddNullListeners() {
        assertThrows(NullPointerException.class, () -> list.add(null));
    }

    @Test
    void shouldKeepInvalidationAndChangeListenersSeparated() {
        Random rnd = new Random(1);

        for(int i = 0; i < 10000; i++) {
            list.add(rnd.nextBoolean() ? il1 : cl1);
        }

        assertEquals(10002, list.size());

        boolean foundChange = false;

        for(int i = 0; i < list.size(); i++) {
            if(list.get(i) instanceof ChangeListener) {
                foundChange = true;
            }
            else if(foundChange) {
                fail("Found an invalidation listener at index " + i + " after a change listener was seen");
            }
        }
    }

    @Test
    void shouldKeepInvalidationAndChangeListenersSeparatedWhenSomeAreRemovedOrExpired() {
        Random rnd = new Random(1);
        List<Object> addedListeners = new ArrayList<>();

        addedListeners.add(il1);
        addedListeners.add(cl1);

        for(int i = 0; i < 10000; i++) {
            Object listener = rnd.nextBoolean() ? new WeakInvalidationListener("" + i) : new WeakChangeListener("" + i);

            addedListeners.add(listener);
            list.add(listener);

            double nextDouble = rnd.nextDouble();

            if(nextDouble < 0.05) {
                list.remove(addedListeners.remove(rnd.nextInt(addedListeners.size())));
            }
            else if(nextDouble < 0.15) {
                if(list.get(rnd.nextInt(list.size())) instanceof SettableWeakListener swl) {
                    swl.setGarbageCollected(true);
                }
            }
        }

        // the exact size is not known, as it would depend on how many listeners have expired and
        // when the last resize was done to clean expired elements

        boolean foundChange = false;

        for(int i = 0; i < list.size(); i++) {
            Object listener = list.get(i);

            if(listener instanceof ChangeListener) {
                foundChange = true;
            }
            else if(foundChange) {
                fail("Found " + listener + " at index " + i + " after a change listener was seen");
            }

            assertTrue(addedListeners.contains(listener));
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
