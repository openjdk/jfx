/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.scene.control;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;

import com.sun.javafx.scene.control.LambdaMultiplePropertyChangeListenerHandler;

import static org.junit.Assert.*;
import static test.com.sun.javafx.scene.control.infrastructure.ControlSkinFactory.*;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;

/**
 * Test support of listChange listeners.
 */
public class LambdaMultipleListHandlerTest {

    private LambdaMultiplePropertyChangeListenerHandler handler;
    private ObservableList<String> items;

//------------ unregister

    /**
     * Single consumer for multiple lists: test that
     * removing from one list doesn't effect listening to other list
     */
    @Test
    public void testUnregistersSingleConsumerMultipleLists() {
        List<Change<?>> changes = new ArrayList<>();
        Consumer<Change<?>> consumer = change -> changes.add(change);
        ObservableList<String> otherList = FXCollections.observableArrayList("other");
        handler.registerListChangeListener(items, consumer);
        handler.registerListChangeListener(otherList, consumer);
        handler.unregisterListChangeListeners(otherList);
        items.add("added");
        otherList.add("added other");
        assertEquals(1, changes.size());
        assertEquals(items, changes.get(0).getList());
    }

    /**
     * Test that all consumers for a single list are removed
     * and manually adding the removed consumer chain as listener
     * has the same effect as when invoked via handler.
     */
    @Test
    public void testUnregistersMultipleConsumers() {
        List<Change<?>> changes = new ArrayList<>();
        Consumer<Change<?>> consumer = change -> changes.add(change);
        List<Change<?>> secondChanges = new ArrayList<>();
        Consumer<Change<?>> secondConsumer = change -> secondChanges.addAll(changes);
        handler.registerListChangeListener(items, consumer);
        handler.registerListChangeListener(items, secondConsumer);
        // remove listener chain
        Consumer<Change<?>> removedChain = handler.unregisterListChangeListeners(items);
        items.add("added after removed");
        assertEquals("none of the removed listeners must be notified",
                0, changes.size() + secondChanges.size());
       // manually add the chained listener
        items.addListener((ListChangeListener)(c -> removedChain.accept(c)));
        items.add("added");
        assertEquals(1, changes.size());
        assertEquals(changes, secondChanges);
    }

    @Test
    public void testUnregistersSingleConsumer() {
        List<Change<?>> changes = new ArrayList<>();
        Consumer<Change<?>> consumer = change -> changes.add(change);
        ObservableList<String> otherList = FXCollections.observableArrayList("other");
        handler.registerListChangeListener(items, consumer);
        Consumer<Change<?>> removed = handler.unregisterListChangeListeners(items);
        items.add("added");
        assertEquals(0, changes.size());
        assertSame(consumer, removed);
    }

    /**
     * Test unregisters not registered list.
     */
    @Test
    public void testUnregistersNotRegistered() {
        assertNull(handler.unregisterListChangeListeners(items));
    }

    @Test
    public void testUnregistersNull() {
        assertNull(handler.unregisterListChangeListeners(null));
    }


//------------- register

    @Test
    public void testRegisterConsumerToMultipleLists() {
        List<Change<?>> changes = new ArrayList<>();
        Consumer<Change<?>> consumer = change -> changes.add(change);
        ObservableList<String> otherList = FXCollections.observableArrayList("other");
        handler.registerListChangeListener(items, consumer);
        handler.registerListChangeListener(otherList, consumer);
        items.add("added");
        otherList.add("added other");
        assertEquals(2, changes.size());
        assertEquals(items, changes.get(0).getList());
        assertEquals(otherList, changes.get(1).getList());
    }

    /**
     * Test that multiple consumers to same observable are invoked in order
     * of registration.
     */
    @Test
    public void testRegisterMultipleConsumerToSingleList() {
        List<Change<?>> changes = new ArrayList<>();
        Consumer<Change<?>> consumer = change -> changes.add(change);
        List<Change<?>> secondChanges = new ArrayList<>();
        Consumer<Change<?>> secondConsumer = change -> secondChanges.addAll(changes);
        handler.registerListChangeListener(items, consumer);
        handler.registerListChangeListener(items, secondConsumer);
        items.add("added");
        assertEquals(1, changes.size());
        assertEquals(changes, secondChanges);
    }

    @Test
    public void testRegister() {
        List<Change<?>> changes = new ArrayList<>();
        Consumer<Change<?>> consumer = change -> changes.add(change);
        handler.registerListChangeListener(items, consumer);
        String added = "added";
        items.add(added);
        assertEquals(1, changes.size());
        Change<?> change = changes.get(0);
        change.next();
        assertTrue(change.wasAdded());
        assertTrue(change.getAddedSubList().contains(added));
    }

    @Test
    public void testRegisterNullConsumer() {
        handler.registerListChangeListener(items, null);
    }

    @Test
    public void testRegisterNullList() {
        handler.registerListChangeListener(null, c -> {});
    }

//--------- dispose

    @Test
    public void testDispose() {
        List<Change<?>> changes = new ArrayList<>();
        Consumer<Change<?>> consumer = change -> changes.add(change);
        handler.registerListChangeListener(items, consumer);
        handler.dispose();
        items.add("added");
        assertEquals("listener must not be invoked after dispose", 0, changes.size());
        handler.registerListChangeListener(items, consumer);
        items.add("added");
        assertEquals("listener must be invoked when re-registered after dispose", 1, changes.size());
    }


//--------- test weak registration

    /**
     * Test that handler is gc'ed and listener no longer notified.
     */
    @Test
    public void testRegisterMemoryLeak() {
        List<Change<?>> changes = new ArrayList<>();
        Consumer<Change<?>> consumer = change -> changes.add(change);
        LambdaMultiplePropertyChangeListenerHandler handler = new LambdaMultiplePropertyChangeListenerHandler();
        WeakReference<LambdaMultiplePropertyChangeListenerHandler> ref = new WeakReference<>(handler);
        handler.registerListChangeListener(items, consumer);
        items.add("added");
        assertEquals(1, changes.size());
        handler = null;
        attemptGC(ref);
        assertNull("handler must be gc'ed", ref.get());
        items.add("another");
        assertEquals("listener must not be invoked after gc", 1, changes.size());
    }


//----------- setup and intial

    @Before
    public void setup() {
        handler = new LambdaMultiplePropertyChangeListenerHandler();
        items = FXCollections.observableArrayList("one", "two", "four");
    }

    /**
     * Demonstrating why we need an invalidation listener for list-valued observables.
     */
    @Test
    public void testInvalidationOfListValuedObservable() {
        String[] data = {"one", "two", "other"};
        ObservableList<String> first = FXCollections.observableArrayList(data);
        ObjectProperty<ObservableList<String>> itemsProperty = new SimpleObjectProperty<>(first);
        assertSame(first, itemsProperty.get());
        int[] invalidations = new int[] {0};
        int[] changes = new int[] {0};
        itemsProperty.addListener(obs -> invalidations[0]++);
        itemsProperty.addListener((obs, ov, nv) -> changes[0]++);
        itemsProperty.set(FXCollections.observableArrayList(data));
        // notifications when newList.equals(oldList)
        assertEquals("changeListener not notified", 0, changes[0]);
        assertEquals("invalidationListener notified", 1, invalidations[0]);
        itemsProperty.get().add("added");
        // sanity: no notification on modifications to the list
        assertEquals(0, changes[0]);
        assertEquals(1, invalidations[0]);
        // sanity: notification from both when !newList.equals(oldList)
        itemsProperty.set(first);
        assertEquals(1, changes[0]);
        assertEquals(2, invalidations[0]);
    }
}
