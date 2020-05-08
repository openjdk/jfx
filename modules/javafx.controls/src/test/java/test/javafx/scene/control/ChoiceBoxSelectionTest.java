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

package test.javafx.scene.control;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ChoiceBoxShim;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.skin.ChoiceBoxSkin;
import javafx.scene.control.skin.ChoiceBoxSkinNodesShim;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Collection of tests around state related to selection.
 * <p>
 *
 * Tests that toggles are un/selected as required (JDK-8242489).
 *
 * Note that the selection should be correct even if the
 * popup has not yet been shown.
 *
 * <p>
 * Need to test (testBaseToggle):
 * a) initial sync of selection state: selected toggle must be that of selectedIndex or none
 * b) change selection state after skin: selected toggle must follow
 *
 * <p>
 *
 * Tests around selectedIndex and uncontained value (testSynced).
 *
 */
public class ChoiceBoxSelectionTest {
    private Scene scene;
    private Stage stage;
    private Pane root;

    private ChoiceBox<String> box;

    private String uncontained;

    /**
     * selected index taken by toggle when popup open
     */
    @Test
    public void testBaseToggleInitialSelectOpenPopup() {
        SingleSelectionModel<String> sm = box.getSelectionModel();
        int selectedIndex = box.getItems().size() - 1;
        sm.select(selectedIndex);
        showChoiceBox();
        box.show();
        assertToggleSelected(selectedIndex);
    }

    /**
     * selected index taken by toggle
     */
    @Test
    public void testBaseToggleInitialSelect() {
        SingleSelectionModel<String> sm = box.getSelectionModel();
        int selectedIndex = box.getItems().size() - 1;
        sm.select(selectedIndex);
        showChoiceBox();
        assertToggleSelected(selectedIndex);
    }

    /**
     * Toggle must be unselected if separator is selected
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testBaseToggleSeparator() {
        ChoiceBox box = new ChoiceBox(FXCollections.observableArrayList(
                "Apple", "Banana", new Separator(), "Orange"));
        int separatorIndex = 2;
        showControl(box);
        SingleSelectionModel<?> sm = box.getSelectionModel();
        int selectedIndex = 1;
        sm.select(selectedIndex);
        sm.select(separatorIndex);
        // implementation detail of current sm (openjfx14): it allows a Separator
        // to be selected - skin must unselect its toggles
        assertToggleSelected(box, -1);
    }

    /**
     * Not quite https://bugs.openjdk.java.net/browse/JDK-8089398
     * (the issue there is setting value while selectionModel == null)
     *
     * This here throws NPE if selectionModel is null when the skin is attached.
     * Base reason is JDK-8242489.
     *
     * @see ChoiceBoxTest#selectionModelCanBeNull()
     */
    @Test
    public void testNullSelectionModel() {
        box.setSelectionModel(null);
        showChoiceBox();
    }


//------------ toggle follows selection change: select -> show -> empty

    /**
     * select -> show -> clear
     */
    @Test
    public void testBaseToggleClearSelection() {
        SingleSelectionModel<String> sm = box.getSelectionModel();
        sm.select(2);
        showChoiceBox();
        sm.clearSelection();
        assertToggleSelected(-1);
    }

    /**
     * select -> show -> select(-1)
     */
    @Test
    public void testBaseToggleMinusIndex() {
        SingleSelectionModel<String> sm = box.getSelectionModel();
        sm.select(2);
        showChoiceBox();
        sm.select(-1);
        assertToggleSelected(-1);
    }

    /**
     * select -> show -> select(null)
     */
    @Test
    public void testBaseToggleNullItem() {
        SingleSelectionModel<String> sm = box.getSelectionModel();
        sm.select(2);
        showChoiceBox();
        sm.select(null);
        assertToggleSelected(-1);
    }

    /**
     * select -> show -> null value
     */
    @Test
    public void testBaseToggleNullValue() {
        SingleSelectionModel<String> sm = box.getSelectionModel();
        sm.select(2);
        showChoiceBox();
        box.setValue(null);
        assertToggleSelected(-1);
    }

    //------------ toggle follows selection change: select -> show -> other selection

    @Test
    public void testBaseToggleChangeIndex() {
        SingleSelectionModel<String> sm = box.getSelectionModel();
        sm.select(2);
        showChoiceBox();
        int other = 1;
        sm.select(other);
        assertToggleSelected(other);
    }

    @Test
    public void testBaseToggleChangeItem() {
        SingleSelectionModel<String> sm = box.getSelectionModel();
        sm.select(2);
        showChoiceBox();
        int other = 1;
        String otherItem = box.getItems().get(other);
        sm.select(otherItem);
        assertToggleSelected(other);
    }

    @Test
    public void testBaseToggleChangeValue() {
        SingleSelectionModel<String> sm = box.getSelectionModel();
        sm.select(2);
        showChoiceBox();
        int other = 1;
        String otherItem = box.getItems().get(other);
        box.setValue(otherItem);
        assertToggleSelected(other);
    }

//------------ toggle follows selection change: empty -> selected

    @Test
    public void testBaseToggleSetValue() {
        showChoiceBox();
        int selectedIndex = box.getItems().size() - 1;
        box.setValue(box.getItems().get(selectedIndex));
        assertToggleSelected(selectedIndex);
    }

    @Test
    public void testBaseToggleSelectItem() {
        showChoiceBox();
        SingleSelectionModel<String> sm = box.getSelectionModel();
        int selectedIndex = box.getItems().size() - 1;
        sm.select(box.getItems().get(selectedIndex));
        assertToggleSelected(selectedIndex);
    }

    @Test
    public void testBaseToggleSelectIndex() {
        showChoiceBox();
        SingleSelectionModel<String> sm = box.getSelectionModel();
        int selectedIndex = box.getItems().size() - 1;
        sm.select(selectedIndex);
        assertToggleSelected(selectedIndex);
    }

    //------------- assertion helper

    protected void assertToggleSelected(ChoiceBox<?> box, int selectedIndex) {
        boolean isSelected = selectedIndex >= 0;
        ContextMenu popup = ChoiceBoxSkinNodesShim.getChoiceBoxPopup((ChoiceBoxSkin<?>) box.getSkin());
        for (int i = 0; i < popup.getItems().size(); i++) {
            boolean shouldBeSelected = isSelected ? selectedIndex == i : false;
            MenuItem item = popup.getItems().get(i);
            if (item instanceof RadioMenuItem) {
                RadioMenuItem selectedToggle = (RadioMenuItem) popup.getItems().get(i);
                assertEquals("toggle " + selectedToggle.getText() + " at index: " + i + " must be selected: " + shouldBeSelected,
                        shouldBeSelected,
                        selectedToggle.isSelected());
            }
        }
    }

    protected void assertToggleSelected(int selectedIndex) {
        assertToggleSelected(box, selectedIndex);
    }

    //------------- tests for JDK-8241999

    @Test
    public void testSyncedToggleUncontainedValue() {
        SingleSelectionModel<String> sm = box.getSelectionModel();
        sm.select(2);
        showChoiceBox();
        box.setValue(uncontained);
        assertToggleSelected(-1);
    }

    /**
     * Base reason for "8241999": selected index not sync'ed.
     */
    @Test
    public void testSyncedSelectedIndexUncontained() {
        box.setValue(box.getItems().get(1));
        box.setValue(uncontained);
        assertEquals("selectedIndex for uncontained value ", -1, box.getSelectionModel().getSelectedIndex());
    }

    /**
     * From review of JDK-8087555:
     * select contained -> select uncontained -> clearselection -> nulls value
     */
    @Test
    public void testSyncedSelectedOnPreselectedThenUncontained() {
        box.setValue(box.getItems().get(1));
        box.setValue(uncontained);
        box.getSelectionModel().clearSelection();
        assertEquals("uncontained value must be unchanged after clearSelection", uncontained, box.getValue());
    }

    /**
     * From review of JDK-8087555:
     * select uncontained -> clearselection -> nulls value
     */
    @Test
    public void testSyncedClearSelectionUncontained() {
        box.setValue(uncontained);
        box.getSelectionModel().clearSelection();
        assertEquals(uncontained, box.getValue());
    }

    //------------- tests for JDK-8242001

    /**
     * Testing JDK-8242001: box value not updated on replacing selection model.
     *
     * Happens if replacing.selectedItem == null
     *
     */
    @Test
    public void testSyncedContainedValueReplaceSMEmpty() {
        box.setValue(box.getItems().get(1));
        SingleSelectionModel<String> replaceSM = ChoiceBoxShim.get_ChoiceBoxSelectionModel(box);
        assertNull(replaceSM.getSelectedItem());
        box.setSelectionModel(replaceSM);
        assertEquals(replaceSM.getSelectedItem(), box.getValue());
    }

    @Test
    public void testSyncedUncontainedValueReplaceSMEmpty() {
        box.setValue(uncontained);
        SingleSelectionModel<String> replaceSM = ChoiceBoxShim.get_ChoiceBoxSelectionModel(box);
        assertNull(replaceSM.getSelectedItem());
        box.setSelectionModel(replaceSM);
        assertEquals(replaceSM.getSelectedItem(), box.getValue());
    }

    @Test
    public void testSyncedBoundValueReplaceSMEmpty() {
        StringProperty valueSource = new SimpleStringProperty("stickyValue");
        box.valueProperty().bind(valueSource);
        SingleSelectionModel<String> replaceSM = ChoiceBoxShim.get_ChoiceBoxSelectionModel(box);
        assertNull(replaceSM.getSelectedItem());
        box.setSelectionModel(replaceSM);
        assertEquals(valueSource.get(), box.getValue());
    }

    //----------- setup and sanity test for initial state

    @Test
    public void testSetupState() {
        assertNotNull(box);
        showChoiceBox();
        List<Node> expected = List.of(box);
        assertEquals(expected, root.getChildren());
    }

    protected void showChoiceBox() {
        showControl(box);
    }

    protected void showControl(Control box) {
        if (!root.getChildren().contains(box)) {
            root.getChildren().add(box);
        }
        stage.show();
        stage.requestFocus();
        box.requestFocus();
        assertTrue(box.isFocused());
        assertSame(box, scene.getFocusOwner());
    }

    @After
    public void cleanup() {
        stage.hide();
    }

    @Before
    public void setup() {
        uncontained = "uncontained";
        root = new VBox();
        scene = new Scene(root);
        stage = new Stage();
        stage.setScene(scene);
        box = new ChoiceBox<>(FXCollections.observableArrayList("Apple", "Banana", "Orange"));
        root.getChildren().addAll(box);
    }

}
