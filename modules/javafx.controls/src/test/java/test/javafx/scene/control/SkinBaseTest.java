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

package test.javafx.scene.control;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import javafx.beans.Observable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.scene.control.Control;
import javafx.scene.control.SkinBaseShim;

public class SkinBaseTest {
    private ControlStub c;
    private SkinBaseStub<ControlStub> s;

    @Before public void setup() {
        c = new ControlStub();
        s = new SkinBaseStub<ControlStub>(c);
    }

    @Test public void skinNotAssignedToControlShouldStillHaveReferenceToControl() {
        assertSame(c, s.getSkinnable());
    }

    @Test public void skinAddedToControlShouldReferToControl() {
        c.setSkin(s);
        assertSame(c, s.getSkinnable());
    }

    @Test public void skinRemovedFromControlShouldHaveNullReferenceToControl() {
        c.setSkin(s);
        c.setSkin(null);
        assertNull(s.getSkinnable());
    }

//-------------- testing listener registration
// Note: the behavior is fully tested in the handler test, here we only verify that the
// expected methods are actually used

    @Test
    public void testChangeSupport() {
        IntegerProperty p = new SimpleIntegerProperty();
        int[] count = new int[] {0};
        Consumer<ObservableValue<?>> consumer = c -> count[0]++;
        s.addChangeListener(p, consumer);
        p.set(200);
        assertEquals("change listener must be notified", 1, count[0]);
        Consumer<ObservableValue<?>> removed = s.removeChangeListeners(p);
        p.set(100);
        assertEquals("changeListener must not be notified", 1, count[0]);
        assertSame(consumer, removed);
    }

    @Test
    public void testInvalidationSupport() {
        IntegerProperty p = new SimpleIntegerProperty();
        int[] count = new int[] {0};
        Consumer<Observable> consumer = c -> count[0]++;
        s.addInvalidationListener(p, consumer);
        p.set(200);
        assertEquals("invalidation listener must be notified", 1, count[0]);
        Consumer<Observable> removed = s.removeInvalidationListeners(p);
        p.set(100);
        assertEquals("invalidation listener must not be notified", 1, count[0]);
        assertSame(consumer, removed);
    }

    @Test
    public void testListChangeSupport() {
        ObservableList<String> list = FXCollections.observableArrayList("one");
        List<Change<?>> changes = new ArrayList<>();
        Consumer<Change<?>> consumer = c -> changes.add(c);
        s.addListChangeListener(list, consumer);
        list.add("added");
        assertEquals(1, changes.size());
        Consumer<Change<?>> removed = s.removeListChangeListeners(list);
        list.add("another");
        assertEquals(1, changes.size());
        assertSame(consumer, removed);
    }

    @Test
    public void testRegisterNull() {
        s.addChangeListener(null, null);
        s.addInvalidationListener(null, null);
        s.addListChangeListener(null, null);
    }

    @Test
    public void testUnregistersNull() {
        assertNull(s.removeChangeListeners(null));
        assertNull(s.removeInvalidationListeners(null));
        assertNull(s.removeListChangeListeners(null));
    }

    public static class SkinBaseStub<C extends Control> extends SkinBaseShim<C> {
        public SkinBaseStub(C control) {
            super(control);
        }

        // FIXME: un/registerXXListener are final protected
        // - how to access without adding a wrapper (with potential of introducing bugs)?
        void addChangeListener(ObservableValue<?> p, Consumer<ObservableValue<?>> consumer) {
            registerChangeListener(p, consumer);
        }

        Consumer<ObservableValue<?>> removeChangeListeners(ObservableValue<?> p) {
            return unregisterChangeListeners(p);
        }

        void addInvalidationListener(Observable p, Consumer<Observable> consumer) {
            registerInvalidationListener(p, consumer);
        }

        Consumer<Observable> removeInvalidationListeners(Observable p) {
            return unregisterInvalidationListeners(p);
        }

        void addListChangeListener(ObservableList<?> list, Consumer<Change<?>> consumer) {
            registerListChangeListener(list, consumer);
        }

        Consumer<Change<?>> removeListChangeListeners(ObservableList<?> list) {
            return unregisterListChangeListeners(list);
        }

    }

}
