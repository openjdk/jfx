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

package test.javafx.scene.control.skin;

import java.lang.ref.WeakReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static javafx.collections.FXCollections.*;
import static javafx.scene.control.ControlShim.*;
import static org.junit.Assert.*;
import static test.com.sun.javafx.scene.control.infrastructure.ControlSkinFactory.*;

import javafx.scene.Scene;
import javafx.scene.control.skin.TabPaneSkin;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Control;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToolBar;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

/**
 * Tests around the cleanup task JDK-8241364.
 */
public class SkinCleanupTest {

    private Scene scene;
    private Stage stage;
    private Pane root;

  //---------------- TreeView

    /**
     * Sanity: replacing the root has no side-effect, listener to rootProperty
     * is registered with skin api
     */
    @Test
    public void testTreeViewSetRoot() {
        TreeView<String> treeView = new TreeView<>(createRoot());
        installDefaultSkin(treeView);
        replaceSkin(treeView);
        treeView.setRoot(createRoot());
    }

    /**
     * NPE from event handler to treeModification of root.
     */
    @Test
    public void testTreeViewAddRootChild() {
        TreeView<String> treeView = new TreeView<>(createRoot());
        installDefaultSkin(treeView);
        replaceSkin(treeView);
        treeView.getRoot().getChildren().add(createRoot());
    }

    /**
     * NPE from event handler to treeModification of root.
     */
    @Test
    public void testTreeViewReplaceRootChildren() {
        TreeView<String> treeView = new TreeView<>(createRoot());
        installDefaultSkin(treeView);
        replaceSkin(treeView);
        treeView.getRoot().getChildren().setAll(createRoot().getChildren());
    }

    /**
     * NPE due to properties listener not removed
     */
    @Test
    public void testTreeViewRefresh() {
        TreeView<String> treeView = new TreeView<>();
        installDefaultSkin(treeView);
        replaceSkin(treeView);
        treeView.refresh();
    }

    /**
     * Sanity: guard against potential memory leak from root property listener.
     */
    @Test
    public void testMemoryLeakAlternativeSkinWithRoot() {
        TreeView<String> treeView = new TreeView<>(createRoot());
        installDefaultSkin(treeView);
        WeakReference<?> weakRef = new WeakReference<>(replaceSkin(treeView));
        assertNotNull(weakRef.get());
        attemptGC(weakRef);
        assertEquals("Skin must be gc'ed", null, weakRef.get());
    }

    /**
     * Creates and returns an expanded treeItem with two children
     */
    private TreeItem<String> createRoot() {
        TreeItem<String> root = new TreeItem<>("root");
        root.setExpanded(true);
        root.getChildren().addAll(new TreeItem<>("child one"), new TreeItem<>("child two"));
        return root;
    }


// ------------------ TreeCell

    @Test
    public void testTreeCellReplaceTreeViewWithNull() {
        TreeCell<Object> cell =  new TreeCell<>();
        TreeView<Object> treeView = new TreeView<>();
        cell.updateTreeView(treeView);
        installDefaultSkin(cell);
        cell.updateTreeView(null);
        // 8253634: updating the old treeView must not throw NPE in skin
        treeView.setFixedCellSize(100);
    }

    @Test
    public void testTreeCellPrefHeightOnReplaceTreeView() {
        TreeCell<Object> cell =  new TreeCell<>();
        cell.updateTreeView(new TreeView<>());
        installDefaultSkin(cell);
        TreeView<Object> treeView = new TreeView<>();
        treeView.setFixedCellSize(100);
        cell.updateTreeView(treeView);
        assertEquals("fixed cell set to value of new treeView",
                cell.getTreeView().getFixedCellSize(),
                cell.prefHeight(-1), 1);
    }

// ------------------ ListCell

    @Test
    public void testListCellReplaceListViewWithNull() {
        ListCell<Object> cell =  new ListCell<>();
        ListView<Object> listView = new ListView<>();
        cell.updateListView(listView);
        installDefaultSkin(cell);
        cell.updateListView(null);
        // 8246745: updating the old listView must not throw NPE in skin
        listView.setFixedCellSize(100);
    }

   @Test
   public void testListCellPrefHeightOnReplaceListView() {
       ListCell<Object> cell =  new ListCell<>();
       cell.updateListView(new ListView<>());
       installDefaultSkin(cell);
       ListView<Object> listView = new ListView<>();
       listView.setFixedCellSize(100);
       cell.updateListView(listView);
       assertEquals("fixed cell set to value of new listView",
               cell.getListView().getFixedCellSize(),
               cell.prefHeight(-1), 1);
   }

  //-------------- listView

    @Test
    public void testListViewAddItems() {
        ListView<String> listView = new ListView<>();
        installDefaultSkin(listView);
        replaceSkin(listView);
        listView.getItems().add("addded");
    }

    @Test
    public void testListViewRefresh() {
        ListView<String> listView = new ListView<>();
        installDefaultSkin(listView);
        replaceSkin(listView);
        listView.refresh();
    }

    @Test
    public void testListViewSetItems() {
        ListView<String> listView = new ListView<>();
        installDefaultSkin(listView);
        replaceSkin(listView);
        listView.setItems(observableArrayList());
    }

//-------- choiceBox, toolBar

    /**
     * NPE on sequence setItems -> modify items after skin is replaced.
     */
    @Test
    public void testChoiceBoxSetItems() {
        ChoiceBox<String> box = new ChoiceBox<>();
        installDefaultSkin(box);
        replaceSkin(box);
        box.setItems(observableArrayList("one"));
        box.getItems().add("added");
    }

    /**
     * NPE when adding items after skin is replaced
     */
    @Test
    public void testChoiceBoxAddItems() {
        ChoiceBox<String> box = new ChoiceBox<>();
        installDefaultSkin(box);
        replaceSkin(box);
        box.getItems().add("added");
    }

    @Test
    public void testToolBarAddItems() {
        ToolBar bar = new ToolBar();
        installDefaultSkin(bar);
        replaceSkin(bar);
        bar.getItems().add(new Rectangle());
    }

    /**
     * Sanity test - fix changed listening to focusProperty, ensure
     * that it's still working as before.
     */
    @Test
    public void testToolBarFocus() {
        ToolBar bar = new ToolBar();
        bar.getItems().addAll(new Button("dummy"), new Button("other"));
        showControl(bar, false);
        Button outside = new Button("outside");
        showControl(outside, true);
        bar.requestFocus();
        assertEquals("first item in toolbar must be focused", bar.getItems().get(0), scene.getFocusOwner());
    }

//-------- TabPane
    @Test
    public void testChildrenCountAfterSkinIsReplaced() {
        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(new Tab("0"), new Tab("1"));
        installDefaultSkin(tabPane);
        int childrenCount = tabPane.getChildrenUnmodifiable().size();
        replaceSkin(tabPane);
        assertEquals(childrenCount, tabPane.getChildrenUnmodifiable().size());
    }

    @Test
    public void testChildrenCountAfterSkinIsRemoved() {
        TabPane tabPane = new TabPane();
        assertEquals(0, tabPane.getChildrenUnmodifiable().size());
        tabPane.getTabs().addAll(new Tab("0"), new Tab("1"));
        installDefaultSkin(tabPane);
        assertEquals(3, tabPane.getChildrenUnmodifiable().size());
        tabPane.setSkin(null);
        assertNull(tabPane.getSkin());
        assertEquals(0, tabPane.getChildrenUnmodifiable().size());
    }

    @Test
    public void testNPEWhenTabsAddedAfterSkinIsReplaced() {
        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(new Tab("0"), new Tab("1"));
        installDefaultSkin(tabPane);
        replaceSkin(tabPane);
        tabPane.getTabs().addAll(new Tab("2"), new Tab("3"));
    }

    @Test
    public void testNPEWhenTabRemovedAfterSkinIsReplaced() {
        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(new Tab("0"), new Tab("1"));
        installDefaultSkin(tabPane);
        replaceSkin(tabPane);
        tabPane.getTabs().remove(0);
    }

    @Test
    public void testAddRemoveTabsAfterSkinIsReplaced() {
        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(new Tab("0"), new Tab("1"));
        installDefaultSkin(tabPane);
        assertEquals(2, tabPane.getTabs().size());
        assertEquals(3, tabPane.getChildrenUnmodifiable().size());
        replaceSkin(tabPane);
        tabPane.getTabs().addAll(new Tab("2"), new Tab("3"));
        assertEquals(4, tabPane.getTabs().size());
        assertEquals(5, tabPane.getChildrenUnmodifiable().size());
        tabPane.getTabs().clear();
        assertEquals(0, tabPane.getTabs().size());
        assertEquals(1, tabPane.getChildrenUnmodifiable().size());
    }

//---------------- setup and initial

    /**
     * Ensures the control is shown in an active scenegraph. Requests
     * focus on the control if focused == true.
     *
     * @param control the control to show
     * @param focused if true, requests focus on the added control
     */
    protected void showControl(Control control, boolean focused) {
        if (root == null) {
            root = new VBox();
            scene = new Scene(root);
            stage = new Stage();
            stage.setScene(scene);
        }
        if (!root.getChildren().contains(control)) {
            root.getChildren().add(control);
        }
        stage.show();
        if (focused) {
            stage.requestFocus();
            control.requestFocus();
            assertTrue(control.isFocused());
            assertSame(control, scene.getFocusOwner());
        }
    }

    @After
    public void cleanup() {
        if (stage != null) stage.hide();
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


