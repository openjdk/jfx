/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.binding.ListenerManagerBase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public abstract class AbstractListenerManagerTest {
    private final List<String> notifications = new ArrayList<>();

    private SimpleObservableValue<String> ov;
    private ListenerManagerBase<String, SimpleObservableValue<String>> helper;

    protected abstract SimpleObservableValue<String> getTestObservableValue();
    protected abstract ListenerManagerBase<String, SimpleObservableValue<String>> getListenerManager();

    @BeforeEach
    void beforeEach() {
        this.ov = getTestObservableValue();
        this.helper = getListenerManager();
    }

    @Test
    void shouldNotifyChangeListeners() {
        ChangeListener<String> cl1 = (_, o, n) -> notifications.add("CL1: " + o + " -> " + n);
        ChangeListener<String> cl2 = (_, o, n) -> notifications.add("CL2: " + o + " -> " + n);

        ov.setValue("A");
        ov.fireValueChanged();

        assertEquals(List.of(), notifications);  // expect nothing, as there are no listeners

        helper.addListener(ov, cl1);

        assertNotNull(ov.data);

        ov.setValue("B");
        ov.fireValueChanged();

        assertEquals(List.of("CL1: A -> B"), notifications);

        helper.addListener(ov, cl2);

        notifications.clear();

        ov.setValue("C");
        ov.fireValueChanged();

        assertEquals(List.of("CL1: B -> C", "CL2: B -> C"), notifications);

        helper.removeListener(ov, cl1);

        notifications.clear();

        ov.setValue("D");
        ov.fireValueChanged();

        assertEquals(List.of("CL2: C -> D"), notifications);

        notifications.clear();

        ov.setValue("E");
        ov.fireValueChanged();

        assertEquals(List.of("CL2: D -> E"), notifications);

        helper.removeListener(ov, cl2);

        notifications.clear();

        ov.setValue("F");
        ov.fireValueChanged();

        assertEquals(List.of(), notifications);
    }

    @Test
    void shouldNotMixUpDualPurposeListeners() {
        DualPurposeListener listener1 = new DualPurposeListener("1");
        InvalidationListener listener2 = _ -> notifications.add("2: invalidated");
        ChangeListener<String> listener3 = (_, o, n) -> notifications.add("3: " + o + " -> " + n);

        helper.addListener(ov, (ChangeListener<String>)listener1);
        helper.addListener(ov, listener2);
        helper.addListener(ov, listener3);

        helper.removeListener(ov, (InvalidationListener)listener1);  // should have no effect

        notifications.clear();

        ov.setValue("A");
        ov.fireValueChanged();

        assertEquals(List.of("2: invalidated", "1: null -> A", "3: null -> A"), notifications);

        helper.removeListener(ov, (ChangeListener<String>)listener1);  // should have effect

        notifications.clear();

        ov.setValue("B");
        ov.fireValueChanged();

        assertEquals(List.of("2: invalidated", "3: A -> B"), notifications);
    }

    class DualPurposeListener implements InvalidationListener, ChangeListener<String> {
        private final String name;

        public DualPurposeListener(String name) {
            this.name = name;
        }

        @Override
        public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
            notifications.add(name + ": " + oldValue + " -> " + newValue);
        }

        @Override
        public void invalidated(Observable observable) {
            notifications.add(name + ": invalidated");
        }
    }

    /*
     * The shouldKeepPropertyValid tests ensure that if there was a change listener present
     * at the start of a notification that, regardless of the end state, the property will
     * be valid.
     */

    @Test
    void shouldKeepPropertyValidWhenExistingChangeListenerIsRemovedDuringNotification() {
        ObjectProperty<String> p = new SimpleObjectProperty<>("A");
        List<String> changes = new ArrayList<>();
        ChangeListener<String> cl = (_, old, val) -> changes.add("1: " + old + " -> " + val);

        AtomicBoolean replace = new AtomicBoolean(true);

        p.addListener(_ -> {
            changes.add("invalidated");

            if (replace.getAndSet(false)) {
                p.removeListener(cl);
            }
        });

        p.addListener(cl);
        p.set("B");
        p.set("C");

        assertEquals(List.of("invalidated", "invalidated"), changes);
    }

    @Test
    void shouldKeepPropertyValidWhenChangeListenerIsReplacedDuringNotification() {
        ObjectProperty<String> p = new SimpleObjectProperty<>("A");
        List<String> changes = new ArrayList<>();
        ChangeListener<String> cl1 = (_, old, val) -> changes.add("1: " + old + " -> " + val);
        ChangeListener<String> cl2 = (_, old, val) -> changes.add("2: " + old + " -> " + val);

        AtomicBoolean replace = new AtomicBoolean(true);

        p.addListener(_ -> {
            changes.add("invalidated");

            if (replace.getAndSet(false)) {
                p.removeListener(cl1);
                p.addListener(cl2);
            }
        });

        p.addListener(cl1);
        p.set("B");
        p.set("C");

        assertEquals(List.of("invalidated", "invalidated", "2: B -> C"), changes);
    }

    @Test
    void shouldKeepPropertyValidWhenNewChangeListenerIsAddedBeforeExistingOneIsRemovedDuringNotification() {
        ObjectProperty<String> p = new SimpleObjectProperty<>("A");
        List<String> changes = new ArrayList<>();
        ChangeListener<String> cl1 = (_, old, val) -> changes.add("1: " + old + " -> " + val);
        ChangeListener<String> cl2 = (_, old, val) -> changes.add("2: " + old + " -> " + val);

        AtomicBoolean replace = new AtomicBoolean(true);

        p.addListener(_ -> {
            changes.add("invalidated");

            if (replace.getAndSet(false)) {
                p.addListener(cl2);
                p.removeListener(cl1);
            }
        });

        p.addListener(cl1);
        p.set("B");
        p.set("C");

        assertEquals(List.of("invalidated", "invalidated", "2: B -> C"), changes);
    }
}
