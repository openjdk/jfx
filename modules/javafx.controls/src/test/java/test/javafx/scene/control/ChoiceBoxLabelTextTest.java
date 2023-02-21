/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.*;

import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.skin.ChoiceBoxSkin;
import javafx.scene.control.skin.ChoiceBoxSkinNodesShim;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

/**
 * Contains tests around the text shown in the box's label, mainly covering
 * fix for https://bugs.openjdk.org/browse/JDK-8087555.
 *
 * <p>
 * It is parameterized in the converter
 * used by ChoiceBox.
 */
@RunWith(Parameterized.class)
public class ChoiceBoxLabelTextTest {

    private Scene scene;
    private Stage stage;
    private Pane root;

    private ChoiceBox<String> box;

    private StringConverter<String> converter;
    private String uncontained;

// -------------- test change uncontained -> different selected

    @Test
    public void testChangeUncontainedSelectIndex() {
        showChoiceBox();
        box.setValue(uncontained);
        box.getSelectionModel().select(1);
        assertEquals("label updated after select index ", getValueText(), getLabelText());
    }

    @Test
    public void testChangeUncontainedSelectItem() {
        showChoiceBox();
        box.setValue(uncontained);
        box.getSelectionModel().select(box.getItems().get(1));
        assertEquals("label updated after select item ", getValueText(), getLabelText());
    }

    @Test
    public void testChangeUncontainedSelectItemOtherUncontained() {
        showChoiceBox();
        box.setValue(uncontained);
        box.getSelectionModel().select(uncontained + "xx");
        assertEquals("label updated after select item ", getValueText(), getLabelText());
    }

    @Test
    public void testChangeUncontainedSetValue() {
        showChoiceBox();
        box.setValue(uncontained);
        box.setValue(box.getItems().get(1));
        assertEquals("label updated after select item ", getValueText(), getLabelText());
    }

    @Test
    public void testChangeContainedSetValueOtherUncontained() {
        showChoiceBox();
        box.setValue(uncontained);
        box.setValue(uncontained + "xx");
        assertEquals("label updated after select item ", getValueText(), getLabelText());
    }

    @Test
    public void testChangeUncontainedClear() {
        showChoiceBox();
        box.setValue(uncontained);
        box.getSelectionModel().clearSelection();
        assertEquals("label updated after select item ", getValueText(), getLabelText());
    }


// ------------- test change selected contained -> different selected

    @Test
    public void testChangeContainedSelectIndex() {
        showChoiceBox();
        int index = 1;
        box.setValue(box.getItems().get(index));
        box.getSelectionModel().select(index -1);
        assertEquals("label updated after select index ", getValueText(), getLabelText());
    }

    @Test
    public void testChangeContainedSelectItem() {
        showChoiceBox();
        int index = 1;
        box.setValue(box.getItems().get(index));
        box.getSelectionModel().select(box.getItems().get(index -1));
        assertEquals("label updated after select item ", getValueText(), getLabelText());
    }

    @Test
    public void testChangeContainedSelectItemUncontained() {
        showChoiceBox();
        int index = 1;
        box.setValue(box.getItems().get(index));
        box.getSelectionModel().select(uncontained);
        assertEquals("label updated after select item ", getValueText(), getLabelText());
    }

    @Test
    public void testChangeContainedSetValue() {
        showChoiceBox();
        int index = 1;
        box.setValue(box.getItems().get(index));
        box.setValue(box.getItems().get(index -1));
        assertEquals("label updated after set value ", getValueText(), getLabelText());
    }

    @Test
    public void testChangeContainedSetValueUncontained() {
        showChoiceBox();
        int index = 1;
        box.setValue(box.getItems().get(index));
        box.setValue(uncontained);
        assertEquals("label updated after set value ", getValueText(), getLabelText());
    }

    @Test
    public void testChangeContainedClear() {
        showChoiceBox();
        int index = 1;
        box.setValue(box.getItems().get(index));
        box.getSelectionModel().clearSelection();
        assertEquals("label updated after clear selection ", getValueText(), getLabelText());
    }

// ------------- test empty -> selected

    @Test
    public void testChangeEmptySelectIndex() {
        showChoiceBox();
        box.getSelectionModel().select(1);
        assertEquals("label updated after select index ", getValueText(), getLabelText());
    }

    @Test
    public void testChangeEmptySelectItem() {
        showChoiceBox();
        box.getSelectionModel().select(box.getItems().get(1));
        assertEquals("label updated after select item ", getValueText(), getLabelText());
    }

    @Test
    public void testChangeEmptySelectItemUncontained() {
        showChoiceBox();
        box.getSelectionModel().select(uncontained);
        assertEquals("label updated after select item ", getValueText(), getLabelText());
    }

    @Test
    public void testChangeEmptySetValue() {
        showChoiceBox();
        box.setValue(box.getItems().get(1));
        assertEquals("label updated after set value ", getValueText(), getLabelText());
    }

    @Test
    public void testChangeEmptySetValueUncontained() {
        showChoiceBox();
        box.setValue(uncontained);
        assertEquals("label updated after set value ", getValueText(), getLabelText());
    }


//------------- test initial label text

    @Test
    public void testInitialEmpty() {
        showChoiceBox();
        assertEquals("label has empty value ", getValueText(), getLabelText());
    }

    @Test
    public void testInitialUncontained() {
        box.setValue(uncontained);
        showChoiceBox();
        assertEquals("label has uncontainedValue ", getValueText(), getLabelText());
    }

    @Test
    public void testInitialUncontained1999() {
        box.getSelectionModel().select(1);
        box.setValue(uncontained);
        showChoiceBox();
        assertEquals("label has uncontainedValue ", getValueText(), getLabelText());
    }

    @Test
    public void testInitialContained() {
        int index = 1;
        box.setValue(box.getItems().get(index));
        showChoiceBox();
        assertEquals("label has contained value", getValueText(), getLabelText());
    }

//------------- test label text sync after modifications of items

    @Test
    public void testModifyItemsSetEqualList() {
        showChoiceBox();
        box.setValue(uncontained);
        box.setItems(FXCollections.observableArrayList(box.getItems()));
        assertEquals("label has uncontainedValue ", getValueText(), getLabelText());
    }

    @Test
    public void testModifyItemsSetItems() {
        showChoiceBox();
        box.setValue(uncontained);
        box.setItems(FXCollections.observableArrayList("one", "two", "three"));
        assertEquals("label has uncontainedValue ", getValueText(), getLabelText());
    }

    @Test
    public void testModifyItemsSetAll() {
        showChoiceBox();
        box.setValue(uncontained);
        box.getItems().setAll(FXCollections.observableArrayList("one", "two", "three"));
        assertEquals("label has uncontainedValue ", getValueText(), getLabelText());
    }

    @Test
    public void testModifyItemsRemoveItem() {
        showChoiceBox();
        box.setValue(uncontained);
        box.getItems().remove(0);
        assertEquals("sanity: is still set to uncontained", uncontained, box.getValue());
        assertEquals("label has uncontainedValue ", getValueText(), getLabelText());
    }

    @Test
    public void testModifyItemsReplaceItem() {
        showChoiceBox();
        box.setValue(uncontained);
        box.getItems().set(0, "replaced");
        assertEquals("label has uncontainedValue ", getValueText(), getLabelText());
    }

    @Test
    public void testModifyItemsAddItem() {
        showChoiceBox();
        box.setValue(uncontained);
        box.getItems().add(0, "added");
        assertEquals("label has uncontainedValue ", getValueText(), getLabelText());
    }

    @Test
    public void testToggleText() {
        showChoiceBox();
        ContextMenu popup = ChoiceBoxSkinNodesShim.getChoiceBoxPopup((ChoiceBoxSkin) box.getSkin());
        for (int i = 0; i < popup.getItems().size(); i++) {
            MenuItem item = popup.getItems().get(i);
            assertEquals("menuItem text at " + i, getItemText(box.getItems().get(i)), item.getText());
        }
    }

    @Test
    public void testToggleConverter() {
        showChoiceBox();
        box.setValue(uncontained);
        // before fix failing here: initial state incorrect
        assertEquals("sanity: label has uncontainedValue ", getValueText(), getLabelText());
        if (box.getConverter() == null) {
            box.setConverter(createStringConverter());
        } else {
            box.setConverter(null);
        }
        assertEquals("after change converter - sanity: value is set to uncontained", uncontained, box.getValue());
        assertEquals("after change converter - label has uncontainedValue ", getValueText(), getLabelText());
    }

// -------- helper methods

    /**
     * Returns the text of the choiceBox' label.
     * @return
     */
    protected String getLabelText() {
        return ChoiceBoxSkinNodesShim
                .getChoiceBoxSelectedText((ChoiceBoxSkin<?>) box.getSkin());
    }

    /**
     * Returns choiceBox value as string.
     *
     *
     * @return
     */
    protected String getValueText() {
        return getItemText(box.getValue());
    }

    /**
     * Returns the value as string.
     *
     * If a converter is
     * available, returns the result of converter.toString(box.value).
     * If not, returns the value if not null or empty string if value is null.
     * <p>
     * Note: the non-converter scenario depends on implementation details
     * of the skin.
     * @param value
     * @return
     */
    protected String getItemText(String value) {
        if (box.getConverter() != null) {
            return box.getConverter().toString(value);
        }
        return value != null ? value : "";
    }


//------------------ parameterized
    // Note: name property not supported before junit 4.11
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        // converter
        Object[][] data = new Object[][] {
            {null},
            {createStringConverter()},
        };
        return Arrays.asList(data);
    }

    protected static StringConverter<String> createStringConverter() {
        return new StringConverter<>() {

            @Override
            public String toString(String object) {
                return "converted: " + object;
            }

            @Override
            public String fromString(String string) {
                throw new UnsupportedOperationException(
                        "conversion to value not supported");
            }

        };
    }

    public ChoiceBoxLabelTextTest(StringConverter<String> converter) {
        this.converter = converter;
    }

//------------------ setup/cleanup and initial state

    @Test
    public void testSetupState() {
        assertNotNull(box);
        showChoiceBox();
        List<Node> expected = List.of(box);
        assertEquals(expected, root.getChildren());
        assertEquals(converter, box.getConverter());
    }

    protected void showChoiceBox() {
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
        box = new ChoiceBox<>(FXCollections.observableArrayList("Apple", "Orange", "Banana"));
        box.setConverter(converter);
        root.getChildren().addAll(box);
    }


}
