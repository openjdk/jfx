/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import com.sun.javafx.event.EventUtil;
import com.sun.javafx.scene.control.ListenerHelper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.concurrent.Task;
import javafx.event.EventTarget;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TreeItem;
import javafx.scene.control.skin.LabelSkin;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import test.com.sun.javafx.scene.control.infrastructure.MouseEventGenerator;
import test.util.memory.JMemoryBuddy;

/**
 * Tests ListenerHelper utility class.
 */
public class TestListenerHelper {
    @Test
    public void testCheckAlive() {
        Object owner = new Object();
        WeakReference<Object> ref = new WeakReference<>(owner);
        SimpleStringProperty p = new SimpleStringProperty();
        AtomicInteger ct = new AtomicInteger();
        AtomicInteger disconnected = new AtomicInteger();

        ListenerHelper h = new ListenerHelper(owner);

        h.addChangeListener(p, (v) -> ct.incrementAndGet());
        h.addDisconnectable(() -> disconnected.incrementAndGet());

        // check that the listener is working
        p.set("1");
        assertEquals(1, ct.get());

        // collect
        owner = null;
        JMemoryBuddy.assertCollectable(ref);

        // fire an event that should be ignored
        p.set("2");
        assertEquals(1, ct.get());

        // check that helper has disconnected all its items
        assertEquals(1, disconnected.get());
    }

    // change listeners

    @Test
    public void testChangeListener_MultipleProperties() {
        ListenerHelper h = new ListenerHelper();
        SimpleStringProperty p1 = new SimpleStringProperty();
        SimpleStringProperty p2 = new SimpleStringProperty();
        AtomicInteger ct = new AtomicInteger();

        h.addChangeListener(() -> ct.incrementAndGet(), p1, p2);

        p1.set("1");
        p2.set("2");
        assertEquals(2, ct.get());

        h.disconnect();

        p1.set("3");
        p2.set("4");
        assertEquals(2, ct.get());
    }

    @Test
    public void testChangeListener_MultipleProperties_FireImmediately() {
        ListenerHelper h = new ListenerHelper();
        SimpleStringProperty p1 = new SimpleStringProperty();
        SimpleStringProperty p2 = new SimpleStringProperty();
        AtomicInteger ct = new AtomicInteger();

        h.addChangeListener(() -> ct.incrementAndGet(), true, p1, p2);

        p1.set("1");
        p2.set("2");
        assertEquals(3, ct.get());

        h.disconnect();

        p1.set("3");
        p2.set("4");
        assertEquals(3, ct.get());
    }

    @Test
    public void testChangeListener_Plain() {
        ListenerHelper h = new ListenerHelper();
        SimpleStringProperty p = new SimpleStringProperty();
        AtomicInteger ct = new AtomicInteger();

        h.addChangeListener(p, (s, old, cur) -> ct.incrementAndGet());

        p.set("1");
        assertEquals(1, ct.get());

        h.disconnect();

        p.set("2");
        assertEquals(1, ct.get());
    }

    @Test
    public void testChangeListener_Plain_FireImmediately() {
        ListenerHelper h = new ListenerHelper();
        SimpleStringProperty p = new SimpleStringProperty();
        AtomicInteger ct = new AtomicInteger();

        h.addChangeListener(p, true, (s, old, cur) -> ct.incrementAndGet());

        p.set("1");
        assertEquals(2, ct.get());

        h.disconnect();

        p.set("2");
        assertEquals(2, ct.get());
    }

    @Test
    public void testChangeListener_Callback() {
        ListenerHelper h = new ListenerHelper();
        SimpleStringProperty p = new SimpleStringProperty();
        AtomicInteger ct = new AtomicInteger();

        h.addChangeListener(p, (cur) -> ct.incrementAndGet());

        p.set("1");
        assertEquals(1, ct.get());

        h.disconnect();

        p.set("2");
        assertEquals(1, ct.get());
    }

    @Test
    public void testChangeListener_Callback_FireImmediately() {
        ListenerHelper h = new ListenerHelper();
        SimpleStringProperty p = new SimpleStringProperty();
        AtomicInteger ct = new AtomicInteger();

        h.addChangeListener(p, true, (cur) -> ct.incrementAndGet());

        p.set("1");
        assertEquals(2, ct.get());

        h.disconnect();

        p.set("2");
        assertEquals(2, ct.get());
    }

    // invalidation listeners

    @Test
    public void testInvalidationListener_MultipleProperties() {
        ListenerHelper h = new ListenerHelper();
        SimpleStringProperty p1 = new SimpleStringProperty();
        SimpleStringProperty p2 = new SimpleStringProperty();
        AtomicInteger ct = new AtomicInteger();

        h.addInvalidationListener(() -> ct.incrementAndGet(), p1, p2);

        p1.set("1");
        p2.set("2");
        assertEquals(2, ct.get());

        h.disconnect();

        p1.set("3");
        p2.set("4");
        assertEquals(2, ct.get());
    }

    @Test
    public void testInvalidationListener_MultipleProperties_FireImmediately() {
        ListenerHelper h = new ListenerHelper();
        SimpleStringProperty p1 = new SimpleStringProperty();
        SimpleStringProperty p2 = new SimpleStringProperty();
        AtomicInteger ct = new AtomicInteger();

        h.addInvalidationListener(() -> ct.incrementAndGet(), true, p1, p2);

        p1.set("1");
        p2.set("2");
        assertEquals(3, ct.get());

        h.disconnect();

        p1.set("3");
        p2.set("4");
        assertEquals(3, ct.get());
    }

    @Test
    public void testInvalidationListener_Plain() {
        ListenerHelper h = new ListenerHelper();
        SimpleStringProperty p = new SimpleStringProperty();
        AtomicInteger ct = new AtomicInteger();

        h.addInvalidationListener(p, (x) -> ct.incrementAndGet());

        p.set("1");
        assertEquals(1, ct.get());

        h.disconnect();

        p.set("2");
        assertEquals(1, ct.get());
    }

    @Test
    public void testInvalidationListener_Plain_FireImmediately() {
        ListenerHelper h = new ListenerHelper();
        SimpleStringProperty p = new SimpleStringProperty();
        AtomicInteger ct = new AtomicInteger();

        h.addInvalidationListener(p, true, (x) -> ct.incrementAndGet());

        p.set("1");
        assertEquals(2, ct.get());

        h.disconnect();

        p.set("2");
        assertEquals(2, ct.get());
    }

    @Test
    public void testInvalidationListener_Callback() {
        ListenerHelper h = new ListenerHelper();
        SimpleStringProperty p = new SimpleStringProperty();
        AtomicInteger ct = new AtomicInteger();

        h.addInvalidationListener(() -> ct.incrementAndGet(), p);

        p.set("1");
        assertEquals(1, ct.get());

        h.disconnect();

        p.set("2");
        assertEquals(1, ct.get());
    }

    @Test
    public void testInvalidationListener_Callback_FireImmediately() {
        ListenerHelper h = new ListenerHelper();
        SimpleStringProperty p = new SimpleStringProperty();
        AtomicInteger ct = new AtomicInteger();

        h.addInvalidationListener(() -> ct.incrementAndGet(), true, p);

        p.set("1");
        assertEquals(2, ct.get());

        h.disconnect();

        p.set("2");
        assertEquals(2, ct.get());
    }

    // list change listeners

    @Test
    public void testListChangeListener() {
        ListenerHelper h = new ListenerHelper();
        ObservableList<String> list = FXCollections.observableArrayList();
        AtomicInteger ct = new AtomicInteger();
        ListChangeListener<String> li = (ch) -> ct.incrementAndGet();

        h.addListChangeListener(list, li);

        list.add("1");
        assertEquals(1, ct.get());

        h.disconnect();

        list.add("2");
        assertEquals(1, ct.get());
    }

    // set change listeners

    @Test
    public void testSetChangeListener() {
        ListenerHelper h = new ListenerHelper();
        ObservableSet<String> list = FXCollections.observableSet();
        AtomicInteger ct = new AtomicInteger();
        SetChangeListener<String> li = (ch) -> ct.incrementAndGet();

        h.addSetChangeListener(list, li);

        list.add("1");
        assertEquals(1, ct.get());

        h.disconnect();

        list.add("2");
        assertEquals(1, ct.get());
    }

    // map change listeners

    @Test
    public void testMapChangeListener() {
        ListenerHelper h = new ListenerHelper();
        ObservableMap<String, String> m = FXCollections.observableHashMap();
        AtomicInteger ct = new AtomicInteger();
        MapChangeListener<String, String> li = (ch) -> ct.incrementAndGet();

        h.addMapChangeListener(m, li);

        m.put("1", "a");
        assertEquals(1, ct.get());

        h.disconnect();

        m.put("2", "b");
        assertEquals(1, ct.get());
    }

    // event handlers

    @Test
    public void testEventHandler() {
        EventTarget[] items = eventHandlerTargets();

        for (EventTarget item : items) {
            ListenerHelper h = new ListenerHelper();
            AtomicInteger ct = new AtomicInteger();

            h.addEventHandler(item, MouseEvent.ANY, (ev) -> ct.incrementAndGet());

            MouseEvent ev = MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0);
            EventUtil.fireEvent(ev, item);

            assertEquals(1, ct.get());

            h.disconnect();

            EventUtil.fireEvent(ev, item);
            assertEquals(1, ct.get());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEventHandlerCheck() {
        ListenerHelper h = new ListenerHelper();
        h.addEventHandler(new Object(), MouseEvent.ANY, (ev) -> { throw new Error(); });
    }

    // event filters

    @Test
    public void testEventFilter() {
        EventTarget[] items = eventHandlerFilters();

        for (EventTarget item : items) {
            ListenerHelper h = new ListenerHelper();
            AtomicInteger ct = new AtomicInteger();

            h.addEventFilter(item, MouseEvent.ANY, (ev) -> ct.incrementAndGet());

            MouseEvent ev = MouseEventGenerator.generateMouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0);
            EventUtil.fireEvent(ev, item);

            assertEquals(1, ct.get());

            h.disconnect();

            EventUtil.fireEvent(ev, item);
            assertEquals(1, ct.get());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEventFilterCheck() {
        ListenerHelper h = new ListenerHelper();
        h.addEventFilter(new Object(), MouseEvent.ANY, (ev) -> { throw new Error(); });
    }

    //

    protected EventTarget[] eventHandlerTargets() {
        return new EventTarget[] {
            new Region(),
            new Stage(),
            new Scene(new Group()),
            new MenuItem(),
            new TreeItem(),
            new TableColumn(),
            new Scale(),
            new Task() {
                @Override
                protected Object call() throws Exception {
                    return null;
                }
            }
        };
    }

    protected EventTarget[] eventHandlerFilters() {
        return new EventTarget[] {
            new Region(),
            new Stage(),
            new Scene(new Group()),
            new Scale(),
            new Task() {
                @Override
                protected Object call() throws Exception {
                    return null;
                }
            }
        };
    }
}
