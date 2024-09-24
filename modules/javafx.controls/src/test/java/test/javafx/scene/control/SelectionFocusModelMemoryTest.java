/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ChoiceBoxShim;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ComboBoxShim;
import javafx.scene.control.Control;
import javafx.scene.control.FocusModel;
import javafx.scene.control.ListView;
import javafx.scene.control.ListViewShim;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPaneShim;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewFocusModel;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.control.TableViewShim;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeTableView.TreeTableViewFocusModel;
import javafx.scene.control.TreeTableView.TreeTableViewSelectionModel;
import javafx.scene.control.TreeTableViewShim;
import javafx.scene.control.TreeView;
import javafx.scene.control.TreeViewShim;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Testing for potential memory leaks in xxSelectionModel and xxFocusModel (
 * https://bugs.openjdk.org/browse/JDK-8241455).
 * Might happen, when the concrete selection/focusModel registers strong listeners on any of the
 * control's properties.
 * <p>
 * Parameterized in not/showing the control before replacing the model (aka:
 * no/skin that might have a reference to any property of the old model as well).
 * Note that failing/passing tests with skin reveal the mis/behavior on part on
 * the skin - added here for convenience (and because it is simple).
 *
 */
public class SelectionFocusModelMemoryTest {
    private Scene scene;
    private Stage stage;
    private Pane root;

//---------- focusModel

    @ParameterizedTest
    @MethodSource("parameters")
    public void testTreeViewFocusModel(boolean showBeforeReplaceSM) {
        TreeItem<String> root = new TreeItem<>("root");
        ObservableList<String> data = FXCollections.observableArrayList("Apple", "Orange", "Banana");
        data.forEach(text -> root.getChildren().add(new TreeItem<>(text)));
        TreeView<String> control = new TreeView<>(root);
        WeakReference<FocusModel<?>> weakRef = new WeakReference<>(control.getFocusModel());
        FocusModel<TreeItem<String>> replacingSm = TreeViewShim.get_TreeViewFocusModel(control);
        maybeShowControl(control, showBeforeReplaceSM);
        control.setFocusModel(replacingSm);
        attemptGC(weakRef, 10);
        assertNull(weakRef.get(), "focusModel must be gc'ed");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testTreeTableViewFocusModel(boolean showBeforeReplaceSM) {
        TreeItem<String> root = new TreeItem<>("root");
        ObservableList<String> data = FXCollections.observableArrayList("Apple", "Orange", "Banana");
        data.forEach(text -> root.getChildren().add(new TreeItem<>(text)));
        TreeTableView<String> control = new TreeTableView<>(root);
        WeakReference<FocusModel<?>> weakRef = new WeakReference<>(control.getFocusModel());
        TreeTableViewFocusModel<String> replacingSm = new TreeTableViewFocusModel<>(control);
        maybeShowControl(control, showBeforeReplaceSM);
        control.setFocusModel(replacingSm);
        attemptGC(weakRef, 10);
        assertNull(weakRef.get(), "focusModel must be gc'ed");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testTableViewFocusModel(boolean showBeforeReplaceSM) {
        TableView<String> control = new TableView<>(FXCollections.observableArrayList("Apple", "Orange", "Banana"));
        WeakReference<FocusModel<?>> weakRef = new WeakReference<>(control.getFocusModel());
        TableViewFocusModel<String> replacingSm = new TableViewFocusModel<>(control);
        maybeShowControl(control, showBeforeReplaceSM);
        control.setFocusModel(replacingSm);
        attemptGC(weakRef, 10);
        assertNull(weakRef.get(), "focusModel must be gc'ed");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testListViewFocusModel(boolean showBeforeReplaceSM) {
        ListView<String> control = new ListView<>(FXCollections.observableArrayList("Apple", "Orange", "Banana"));
        WeakReference<FocusModel<?>> weakRef = new WeakReference<>(control.getFocusModel());
        FocusModel<String> replacingSm = ListViewShim.getListViewFocusModel(control);
        maybeShowControl(control, showBeforeReplaceSM);
        control.setFocusModel(replacingSm);
        attemptGC(weakRef, 10);
        assertNull(weakRef.get(), "focusModel must be gc'ed");
    }

//------------------------ selectionModel

    @ParameterizedTest
    @MethodSource("parameters")
    public void testTreeViewSelectionModel(boolean showBeforeReplaceSM) {
        TreeItem<String> root = new TreeItem<>("root");
        ObservableList<String> data = FXCollections.observableArrayList("Apple", "Orange", "Banana");
        data.forEach(text -> root.getChildren().add(new TreeItem<>(text)));
        TreeView<String> control = new TreeView<>(root);
        WeakReference<SelectionModel<?>> weakRef = new WeakReference<>(control.getSelectionModel());
        MultipleSelectionModel<TreeItem<String>> replacingSm = TreeViewShim.get_TreeViewBitSetSelectionModel(control);
        maybeShowControl(control, showBeforeReplaceSM);
        control.setSelectionModel(replacingSm);
        attemptGC(weakRef, 10);
        assertNull(weakRef.get(), "selectionModel must be gc'ed");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testTreeTableViewSelectionModel(boolean showBeforeReplaceSM) {
        TreeItem<String> root = new TreeItem<>("root");
        ObservableList<String> data = FXCollections.observableArrayList("Apple", "Orange", "Banana");
        data.forEach(text -> root.getChildren().add(new TreeItem<>(text)));
        TreeTableView<String> control = new TreeTableView<>(root);
        WeakReference<SelectionModel<?>> weakRef = new WeakReference<>(control.getSelectionModel());
        TreeTableViewSelectionModel<String> replacingSm = (TreeTableViewSelectionModel<String>) TreeTableViewShim.get_TreeTableViewArrayListSelectionModel(control);
        maybeShowControl(control, showBeforeReplaceSM);
        control.setSelectionModel(replacingSm);
        attemptGC(weakRef, 10);
        assertNull(weakRef.get(), "selectionModel must be gc'ed");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testTableViewSelectionModel(boolean showBeforeReplaceSM) {
        TableView<String> control = new TableView<>(FXCollections.observableArrayList("Apple", "Orange", "Banana"));
        WeakReference<SelectionModel<?>> weakRef = new WeakReference<>(control.getSelectionModel());
        TableViewSelectionModel<String> replacingSm = TableViewShim.get_TableViewArrayListSelectionModel(control);
        maybeShowControl(control, showBeforeReplaceSM);
        control.setSelectionModel(replacingSm);
        attemptGC(weakRef, 10);
        assertNull(weakRef.get(), "selectionModel must be gc'ed");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testListViewSelectionModel(boolean showBeforeReplaceSM) {
        ListView<String> control = new ListView<>(FXCollections.observableArrayList("Apple", "Orange", "Banana"));
        WeakReference<SelectionModel<?>> weakRef = new WeakReference<>(control.getSelectionModel());
        MultipleSelectionModel<String> replacingSm = ListViewShim.getListViewBitSetSelectionModel(control);
        maybeShowControl(control, showBeforeReplaceSM);
        control.setSelectionModel(replacingSm);
        attemptGC(weakRef, 10);
        assertNull(weakRef.get(), "selectionModel must be gc'ed");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testTabPaneSelectionModel(boolean showBeforeReplaceSM) {
        TabPane control = new TabPane();
        ObservableList<String> data = FXCollections.observableArrayList("Apple", "Orange", "Banana");
        data.forEach(text -> control.getTabs().add(new Tab(text)));
        WeakReference<SelectionModel<?>> weakRef = new WeakReference<>(control.getSelectionModel());
        SingleSelectionModel<Tab> replacingSm = TabPaneShim.getTabPaneSelectionModel(control);
        maybeShowControl(control, showBeforeReplaceSM);
        control.setSelectionModel(replacingSm);
        attemptGC(weakRef, 10);
        assertNull(weakRef.get(), "selectionModel must be gc'ed");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testComboBoxSelectionModel(boolean showBeforeReplaceSM) {
        ComboBox<String> control = new ComboBox<>(FXCollections.observableArrayList("Apple", "Orange", "Banana"));
        WeakReference<SelectionModel<?>> weakRef = new WeakReference<>(control.getSelectionModel());
        SingleSelectionModel<String> replacingSm = ComboBoxShim.get_ComboBoxSelectionModel(control);
        maybeShowControl(control, showBeforeReplaceSM);
        control.setSelectionModel(replacingSm);
        attemptGC(weakRef, 10);
        assertNull(weakRef.get(), "selectionModel must be gc'ed");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testChoiceBoxSelectionModel(boolean showBeforeReplaceSM) {
        ChoiceBox<String> control = new ChoiceBox<>(FXCollections.observableArrayList("Apple", "Orange", "Banana"));
        WeakReference<SelectionModel<?>> weakRef = new WeakReference<>(control.getSelectionModel());
        SingleSelectionModel<String> replacingSm = ChoiceBoxShim.get_ChoiceBoxSelectionModel(control);
        maybeShowControl(control, showBeforeReplaceSM);
        control.setSelectionModel(replacingSm);
        attemptGC(weakRef, 10);
        assertNull(weakRef.get(), "selectionModel must be gc'ed");
    }

    private void attemptGC(WeakReference<?> weakRef, int n) {
        // Attempt gc n times
        for (int i = 0; i < n; i++) {
            System.gc();

            if (weakRef.get() == null) {
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                fail(e);
            }
        }
    }

    protected void maybeShowControl(Control control, boolean showBeforeReplaceSM) {
        if (!showBeforeReplaceSM) return;
        show(control);
    }

// ------------- parameterized

    private static Collection<Boolean> parameters() {
        // show the control before replacing the selectionModel
        return List.of(
            false,
            true
        );
    }

//------------------ setup

    private void show(Control node) {
        if (root == null) {
            root =  new VBox();
            scene = new Scene(root);
            stage = new Stage();
            stage.setScene(scene);
        }
        root.getChildren().add(node);
        if (!stage.isShowing()) {
            stage.show();
        }
    }

    @AfterEach
    public void cleanup() {
        if (stage != null) {
            stage.hide();
        }
    }
}
