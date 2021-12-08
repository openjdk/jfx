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

package test.javafx.scene.control.skin;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.javafx.tk.Toolkit;

import static javafx.scene.control.skin.VirtualFlowShim.*;
import static org.junit.Assert.*;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTablePosition;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.control.skin.VirtualContainerBase;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.converter.DefaultStringConverter;
import test.com.sun.javafx.scene.control.infrastructure.MouseEventFirer;

/**
 * Test scrolling while editing, mainly fix for JDK-8272118 - do not cancel edit on mouse click.
 *
 * The basic test strategy is:
 *
 * - for each virtualized control and for each scroll direction,
 * - create and show the control such it is large enough in be scrollable
 *
 * Test the fix:
 *
 * - start editing a cell in the viewport
 * - click on the scrollBar to scroll the cell off the viewport and verify that
 *   control is still editing
 *
 * Test that clicking is still requesting focus:
 *
 * - add a focused control to the scene
 * - click on the scrollBar and verify that the control is focusOwner
 *
 */
public class EditAndScrollTest {

    private Scene scene;
    private Stage stage;
    private Pane root;

    int rows;
    int editingRow;

//----------------- TreeTableView

    @Test
    public void testTreeTableViewEditingAfterMouseOnVerticalScrollBar() {
        TreeTableView<?> control = createAndShowTreeTableView(true);
        TreeTablePosition<?, ?> editingItem = new TreeTablePosition(control, editingRow, control.getColumns().get(0));
        fireMouseOnVerticalTrack(control);
        assertEquals(editingItem, control.getEditingCell());
    }

    @Test
    public void testTreeTableViewEditingAfterMouseOnHorizontalScrollBar() {
        TreeTableView<?> control = createAndShowTreeTableView(true);
        TreeTablePosition<?, ?> editingItem = new TreeTablePosition(control, editingRow, control.getColumns().get(0));
        fireMouseOnHorizontalTrack(control);
        assertEquals(editingItem, control.getEditingCell());
    }

    @Test
    public void testTreeTableViewFocusedAfterMouseOnVerticalScrollBar() {
        TreeTableView<?> control = createAndShowTreeTableView(false);
        assertFocusedAfterMouseOnScrollBar(control, Orientation.VERTICAL);
    }

    @Test
    public void testTreeTableViewFocusedAfterMouseOnHorizontalScrollBar() {
        TreeTableView<?> control = createAndShowTreeTableView(false);
        assertFocusedAfterMouseOnScrollBar(control, Orientation.HORIZONTAL);
    }

    /**
     * Test the test setup for TreeTableView.
     */
    @Test
    public void testTreeTableViewEditing() {
        TreeTableView<?> control = createAndShowTreeTableView(true);
        assertEquals(rows + 1, control.getExpandedItemCount());
        assertEquals(100, scene.getWidth(), 1);
        assertEquals(330, scene.getHeight(), 1);
        assertTrue("sanity: vertical scrollbar visible for list " ,
                getHorizontalScrollBar(control).isVisible());
        assertTrue("sanity: vertical scrollbar visible for list " ,
                getVerticalScrollBar(control).isVisible());
        TreeTablePosition<?, ?> editingItem = new TreeTablePosition(control, editingRow, control.getColumns().get(0));
        assertEquals("control must be editing at", editingItem, control.getEditingCell());
    }

    /**
     * Creates and shows a TableView configured to be
     * - not/editing depending on the startEdit
     * - scrollable both horizontally and vertically
     */
    private TreeTableView<?> createAndShowTreeTableView(boolean startEdit) {
        TreeItem<MenuItem> root = new TreeItem<>(new MenuItem("root"));
        root.setExpanded(true);
        ObservableList<String> baseData = createData(rows, false);
        baseData.forEach(s -> root.getChildren().add(new TreeItem<>(new MenuItem(s))));
        TreeTableView<MenuItem> control = new TreeTableView<>(root);
        control.setEditable(true);
        TreeTableColumn<MenuItem, String> first = createTreeTableColumn();
        control.getColumns().addAll(first);
        for (int i = 0; i < 10; i++) {
            control.getColumns().add(createTreeTableColumn());
        }
        showControl(control, true, 100, 330);
        if (startEdit) {
            control.edit(editingRow, first);
        }
        return control;
    }

    private TreeTableColumn<MenuItem, String> createTreeTableColumn() {
        TreeTableColumn<MenuItem, String> first = new TreeTableColumn<>("Text");
        first.setCellFactory(TextFieldTreeTableCell.forTreeTableColumn());
        first.setCellValueFactory(new TreeItemPropertyValueFactory<>("text"));
        return first;
    }


//----------------- TableView

    @Test
    public void testTableViewEditingAfterMouseOnVerticalScrollBar() {
        TableView<?> control = createAndShowTableView(true);
        TablePosition<?, ?> editingItem = new TablePosition(control, editingRow, control.getColumns().get(0));
        fireMouseOnVerticalTrack(control);
        assertEquals(editingItem, control.getEditingCell());
    }

    @Test
    public void testTableViewEditingAfterMouseOnHorizontalScrollBar() {
        TableView<?> control = createAndShowTableView(true);
        TablePosition<?, ?> editingItem = new TablePosition(control, editingRow, control.getColumns().get(0));
        fireMouseOnHorizontalTrack(control);
        assertEquals(editingItem, control.getEditingCell());
    }

    @Test
    public void testTableViewFocusedAfterMouseOnVerticalScrollBar() {
        TableView<?> control = createAndShowTableView(false);
        assertFocusedAfterMouseOnScrollBar(control, Orientation.VERTICAL);
    }

    @Test
    public void testTableViewFocusedAfterMouseOnHorizontalScrollBar() {
        TableView<?> control = createAndShowTableView(false);
        assertFocusedAfterMouseOnScrollBar(control, Orientation.HORIZONTAL);
    }

    /**
     * Test the test setup for TreeView.
     */
    @Test
    public void testTableViewEditing() {
        TableView<?> control = createAndShowTableView(true);
        assertEquals(rows, control.getItems().size());
        assertEquals(100, scene.getWidth(), 1);
        assertEquals(330, scene.getHeight(), 1);
        assertTrue("sanity: vertical scrollbar visible for list " ,
                getHorizontalScrollBar(control).isVisible());
        assertTrue("sanity: vertical scrollbar visible for list " ,
                getVerticalScrollBar(control).isVisible());
        TablePosition<?, ?> editingItem = new TablePosition(control, editingRow, control.getColumns().get(0));
        assertEquals("control must be editing at", editingItem, control.getEditingCell());
    }

    /**
     * Creates and shows a TableView configured to be
     * - not/editing depending on the startEdit
     * - scrollable both horizontally and vertically
     */
    private TableView<?> createAndShowTableView(boolean startEdit) {
        TableView<MenuItem> control = new TableView<>();
        createData(60, false).forEach(s -> control.getItems().add(new MenuItem(s)));
        control.setEditable(true);
        TableColumn<MenuItem, String> first = createColumn();
        control.getColumns().addAll(first);
        for (int i = 0; i < 10; i++) {
            control.getColumns().add(createColumn());
        }
        showControl(control, true, 100, 330);
        if (startEdit) {
            control.edit(editingRow, first);
        }
        return control;
    }

    private TableColumn<MenuItem, String> createColumn() {
        TableColumn<MenuItem, String> first = new TableColumn<>("Text");
        first.setCellFactory(TextFieldTableCell.forTableColumn());
        first.setCellValueFactory(new PropertyValueFactory<>("text"));
        return first;
    }


//----------------- TreeView

    @Test
    public void testTreeViewEditingAfterMouseOnVerticalScrollBar() {
        TreeView<?> control = createAndShowTreeView(true);
        TreeItem<?> editingItem = control.getTreeItem(editingRow);
        fireMouseOnVerticalTrack(control);
        assertEquals(editingItem, control.getEditingItem());
    }

    @Test
    public void testTreeViewEditingAfterMouseOnHorizontalScrollBar() {
        TreeView<?> control = createAndShowTreeView(true);
        TreeItem<?> editingItem = control.getTreeItem(editingRow);
        fireMouseOnHorizontalTrack(control);
        assertEquals(editingItem, control.getEditingItem());
    }

    @Test
    public void testTreeViewFocusedAfterMouseOnVerticalScrollBar() {
        TreeView<?> control = createAndShowTreeView(false);
        assertFocusedAfterMouseOnScrollBar(control, Orientation.VERTICAL);
    }

    @Test
    public void testTreeViewFocusedAfterMouseOnHorizontalScrollBar() {
        TreeView<?> control = createAndShowTreeView(false);
        assertFocusedAfterMouseOnScrollBar(control, Orientation.HORIZONTAL);
    }

    /**
     * Test the test setup for TreeView.
     */
    @Test
    public void testTreeViewEditing() {
        TreeView<?> control = createAndShowTreeView(true);
        assertEquals(rows + 1, control.getExpandedItemCount());
        assertEquals(100, scene.getWidth(), 1);
        assertEquals(330, scene.getHeight(), 1);
        assertTrue("sanity: vertical scrollbar visible for list " ,
                getHorizontalScrollBar(control).isVisible());
        assertTrue("sanity: vertical scrollbar visible for list " ,
                getVerticalScrollBar(control).isVisible());
        TreeItem<?> editingItem = control.getTreeItem(editingRow);
        assertEquals("control must be editing at", editingItem, control.getEditingItem());
    }

    /**
     * Creates and shows a TreeView configured to be
     * - not/editing depending on the startEdit
     * - scrollable both horizontally and vertically
     */
    private TreeView<?> createAndShowTreeView(boolean startEdit) {
        TreeItem<String> root = new TreeItem<>("root");
        root.setExpanded(true);
        ObservableList<String> baseData = createData(rows, true);
        baseData.forEach(s -> root.getChildren().add(new TreeItem<>(s)));
        TreeView<String> control = new TreeView<>(root);
        control.setEditable(true);
        control.setCellFactory(TextFieldTreeCell.forTreeView(new DefaultStringConverter()));
        showControl(control, true, 100, 330);
        if (startEdit) {
            TreeItem<String> editingItem = control.getTreeItem(editingRow);
            control.edit(editingItem);
        }
        return control;
    }


//----------------- ListView

    @Test
    public void testListViewEditingAfterMouseOnVerticalScrollBar() {
        ListView<?> control = createAndShowListView(true);
        fireMouseOnVerticalTrack(control);
        assertEquals(editingRow, control.getEditingIndex());
    }

    @Test
    public void testListViewEditingAfterMouseOnHorizontalScrollBar() {
        ListView<?> control = createAndShowListView(true);
        fireMouseOnHorizontalTrack(control);
        assertEquals(editingRow, control.getEditingIndex());
    }

    @Test
    public void testListViewFocusedAfterMouseOnVerticalScrollBar() {
        ListView<?> control = createAndShowListView(false);
        assertFocusedAfterMouseOnScrollBar(control, Orientation.VERTICAL);
    }

    @Test
    public void testListViewFocusedAfterMouseOnHorizontalScrollBar() {
        ListView<?> control = createAndShowListView(false);
        assertFocusedAfterMouseOnScrollBar(control, Orientation.HORIZONTAL);
    }

    /**
     * Test the test setup for ListView.
     */
    @Test
    public void testListViewEditing() {
        ListView<?> control = createAndShowListView(true);
        assertEquals(rows, control.getItems().size());
        assertEquals(100, scene.getWidth(), 1);
        assertEquals(330, scene.getHeight(), 1);
        assertTrue("sanity: vertical scrollbar visible for list " ,
                getHorizontalScrollBar(control).isVisible());
        assertTrue("sanity: vertical scrollbar visible for list " ,
                getVerticalScrollBar(control).isVisible());
        assertEquals("control must be editing at", editingRow, control.getEditingIndex());
    }

    /**
     * Creates and shows a ListView configured to be
     * - not/editing depending on the startEdit
     * - scrollable both horizontally and vertically
     */
    private ListView<?> createAndShowListView(boolean startEdit) {
        ObservableList<String> baseData = createData(rows, true);
        ListView<String> control = new ListView<>(baseData);
        control.setEditable(true);
        control.setCellFactory(TextFieldListCell.forListView(new DefaultStringConverter()));
        showControl(control, true, 100, 330);
        if (startEdit) {
            control.edit(editingRow);
        }
        return control;
    }


//--------------- test helpers

    /**
     * Tests that clicking on the scrollbar in the given direction requests
     * focus back to the given control.
     *
     * Note: the control must be showing in the default scene of this test and
     * have a skin of type VirtualContainerBase.
     *
     */
    private void assertFocusedAfterMouseOnScrollBar(Control control, Orientation dir) {
        // add and focus additional control
        Button focusableControl = new Button("dummy");
        showControl(focusableControl, true);
        if (dir == Orientation.HORIZONTAL) {
           fireMouseOnHorizontalTrack(control);
        } else {
            fireMouseOnVerticalTrack(control);
        }
        assertEquals("virtualized control must be focusOwner after mouse on scrollbar",
                control, scene.getFocusOwner());
    }


//----------------- Utility methods (TODO: move into infrastructure)

    /**
     * Fires a mouse event onto the middle of the vertical scrollbar's track.
     * @throws IllegalStateException if control's skin is not VirtualContainerBase
     */
    public static void fireMouseOnVerticalTrack(Control control) {
        ScrollBar scrollBar = getVerticalScrollBar(control);
        Region track = (Region) scrollBar.lookup(".track");
        MouseEventFirer firer = new MouseEventFirer(track, true);
        firer.fireMousePressAndRelease();
        Toolkit.getToolkit().firePulse();
    }

    /**
     * Fires a mouse event onto the middle of the horizontal scrollbar's track.
     * @throws IllegalStateException if control's skin is not VirtualContainerBase
     */
    public static void fireMouseOnHorizontalTrack(Control control) {
        ScrollBar scrollBar = getHorizontalScrollBar(control);
        Region track = (Region) scrollBar.lookup(".track");
        MouseEventFirer firer = new MouseEventFirer(track, true);
        firer.fireMousePressAndRelease();
        Toolkit.getToolkit().firePulse();
    }

    /**
     * Returns a vertical ScrollBar of the control.
     * @throws IllegalStateException if control's skin is not VirtualContainerBase
     */
    public static ScrollBar getVerticalScrollBar(Control control) {
        if (control.getSkin() instanceof VirtualContainerBase) {
            VirtualFlow<?> flow = getVirtualFlow((VirtualContainerBase<?, ?>) control.getSkin());
            return getVBar(flow);
        }
        throw new IllegalStateException("control's skin must be of type VirtualContainerBase but was: " + control.getSkin());
    }

    /**
     * Returns a vertical ScrollBar of the control.
     * @throws IllegalStateException if control's skin is not VirtualContainerBase
     */
    public static ScrollBar getHorizontalScrollBar(Control control) {
        if (control.getSkin() instanceof VirtualContainerBase) {
            VirtualFlow<?> flow = getVirtualFlow((VirtualContainerBase<?, ?>) control.getSkin());
            return getHBar(flow);
        }
        throw new IllegalStateException("control's skin must be of type VirtualContainerBase but was: " + control.getSkin());
    }

//----------------- setup

    /**
     * Creates and returns a list of long/short (depending on wide parameter) Strings.
     */
    private ObservableList<String> createData(int size, boolean wide) {
        ObservableList<String> data = FXCollections.observableArrayList();
        String item = wide ? "something that really really guarantees a horizontal scrollbar is visible  " : "item";
        for (int i = 0; i < size; i++) {
            data.add(item + i);
        }
        return data;
    }

    /**
     * Ensures the control is shown in an active scenegraph. Requests
     * focus on the control if focused == true.
     *
     * @param control the control to show
     * @param focused if true, requests focus on the added control
     */
    protected void showControl(Control control, boolean focused) {
        showControl(control, focused, -1, -1);
    }

    /**
     * Ensures the control is shown in an active scenegraph. Requests
     * focus on the control if focused == true.
     * On first call, sizes the scene to width/height if width > 0
     *
     * @param control the control to show
     * @param focused if true, requests focus on the added control
     * @param width the width of the scene or -1 for auto-sizing
     * @param height the height of the scene or -1 for auto-sizing
     */
    protected void showControl(Control control, boolean focused, double width, double height) {
        if (root == null) {
            root = new VBox();
            if (width > 0) {
                scene = new Scene(root, width, height);
            } else {
                scene = new Scene(root);
            }
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

    @Before public void setup() {
        Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException)throwable;
            } else {
                Thread.currentThread().getThreadGroup().uncaughtException(thread, throwable);
            }
        });
        editingRow = 1;
        rows = 60;
    }

    @After public void cleanup() {
        if (stage != null) stage.hide();
        Thread.currentThread().setUncaughtExceptionHandler(null);
    }

}
