/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.scene.control.behavior;

import java.lang.ref.WeakReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.javafx.scene.control.behavior.BehaviorBase;
import com.sun.javafx.scene.control.behavior.ListCellBehavior;

import static javafx.collections.FXCollections.*;
import static org.junit.Assert.*;
import static test.com.sun.javafx.scene.control.infrastructure.ControlSkinFactory.*;

import javafx.scene.control.ListView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

/**
 * Test for misbehavior of individual implementations that turned
 * up in binch testing.
 *
 */
public class BehaviorCleanupTest {

//----------- TreeView

    /**
     * Test cleanup of selection listeners in TreeViewBehavior.
     */
    @Test
    public void testTreeViewBehaviorDisposeSelect() {
        TreeView<String> treeView = new TreeView<>(createRoot());
        WeakReference<BehaviorBase<?>> weakRef = new WeakReference<>(createBehavior(treeView));
        treeView.getSelectionModel().select(1);
        weakRef.get().dispose();
        treeView.getSelectionModel().select(0);
        assertNull("anchor must remain cleared on selecting when disposed",
                treeView.getProperties().get("anchor"));
    }

    @Test
    public void testTreeViewBehaviorSelect() {
        TreeView<String> treeView = new TreeView<>(createRoot());
        createBehavior(treeView);
        int last = 1;
        treeView.getSelectionModel().select(last);
        assertEquals("anchor must be set", last, treeView.getProperties().get("anchor"));
    }

    @Test
    public void testTreeViewBehaviorDispose() {
        TreeView<String> treeView = new TreeView<>(createRoot());
        WeakReference<BehaviorBase<?>> weakRef = new WeakReference<>(createBehavior(treeView));
        treeView.getSelectionModel().select(1);
        weakRef.get().dispose();
        assertNull("anchor must be cleared after dispose", treeView.getProperties().get("anchor"));
    }

    /**
     * Creates and returns an expanded treeItem with two children.
     */
    private TreeItem<String> createRoot() {
        TreeItem<String> root = new TreeItem<>("root");
        root.setExpanded(true);
        root.getChildren().addAll(new TreeItem<>("child one"), new TreeItem<>("child two"));
        return root;
    }


// ---------- ListView

    /**
     * Test cleanup of listener to itemsProperty.
     */
    @Test
    public void testListViewBehaviorDisposeSetItems() {
        ListView<String> listView = new ListView<>(observableArrayList("one", "two"));
        WeakReference<BehaviorBase<?>> weakRef = new WeakReference<>(createBehavior(listView));
        weakRef.get().dispose();
        int last = 1;
        ListCellBehavior.setAnchor(listView, last, false);
        listView.setItems(observableArrayList("other", "again"));
        assertEquals("sanity: anchor unchanged", last, listView.getProperties().get("anchor"));
        listView.getItems().remove(0);
        assertEquals("anchor must not be updated on items modification when disposed",
                last, listView.getProperties().get("anchor"));
    }

    @Test
    public void testListViewBehaviorSetItems() {
        ListView<String> listView = new ListView<>(observableArrayList("one", "two"));
        createBehavior(listView);
        int last = 1;
        ListCellBehavior.setAnchor(listView, last, false);
        listView.setItems(observableArrayList("other", "again"));
        assertEquals("sanity: anchor unchanged", last, listView.getProperties().get("anchor"));
        listView.getItems().remove(0);
        assertEquals("anchor must be updated on items modification",
                last -1, listView.getProperties().get("anchor"));
   }

    /**
     * Test cleanup of itemsList listener in ListViewBehavior.
     */
    @Test
    public void testListViewBehaviorDisposeRemoveItem() {
        ListView<String> listView = new ListView<>(observableArrayList("one", "two"));
        WeakReference<BehaviorBase<?>> weakRef = new WeakReference<>(createBehavior(listView));
        weakRef.get().dispose();
        int last = 1;
        ListCellBehavior.setAnchor(listView, last, false);
        listView.getItems().remove(0);
        assertEquals("anchor must not be updated on items modification when disposed",
                last,
                listView.getProperties().get("anchor"));
    }

    @Test
    public void testListViewBehaviorRemoveItem() {
        ListView<String> listView = new ListView<>(observableArrayList("one", "two"));
        createBehavior(listView);
        int last = 1;
        ListCellBehavior.setAnchor(listView, last, false);
        assertEquals("behavior must set anchor on select", last, listView.getProperties().get("anchor"));
        listView.getItems().remove(0);
        assertEquals("anchor must be updated on items modification",
                last -1, listView.getProperties().get("anchor"));
    }

    /**
     * Test cleanup of selection listeners in ListViewBehavior.
     */
    @Test
    public void testListViewBehaviorDisposeSelect() {
        ListView<String> listView = new ListView<>(observableArrayList("one", "two"));
        WeakReference<BehaviorBase<?>> weakRef = new WeakReference<>(createBehavior(listView));
        listView.getSelectionModel().select(1);
        weakRef.get().dispose();
        listView.getSelectionModel().select(0);
        assertNull("anchor must remain cleared on selecting when disposed",
                listView.getProperties().get("anchor"));
    }

    @Test
    public void testListViewBehaviorSelect() {
        ListView<String> listView = new ListView<>(observableArrayList("one", "two"));
        createBehavior(listView);
        int last = 1;
        listView.getSelectionModel().select(last);
        assertEquals("anchor must be set", last, listView.getProperties().get("anchor"));
    }

    @Test
    public void testListViewBehaviorDispose() {
        ListView<String> listView = new ListView<>(observableArrayList("one", "two"));
        WeakReference<BehaviorBase<?>> weakRef = new WeakReference<>(createBehavior(listView));
        listView.getSelectionModel().select(1);
        weakRef.get().dispose();
        assertNull("anchor must be cleared after dispose", listView.getProperties().get("anchor"));
    }

  //------------------ setup/cleanup

    @After
    public void cleanup() {
        Thread.currentThread().setUncaughtExceptionHandler(null);
    }

    @Before
    public void setup() {
        Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException)throwable;
            } else {
                Thread.currentThread().getThreadGroup().uncaughtException(thread, throwable);
            }
        });
    }

}
