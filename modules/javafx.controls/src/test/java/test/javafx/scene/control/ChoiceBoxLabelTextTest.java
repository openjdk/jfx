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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.stream.Stream;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Contains tests around the text shown in the box's label, mainly covering
 * fix for https://bugs.openjdk.org/browse/JDK-8087555.
 *
 * <p>
 * It is parameterized in the converter
 * used by ChoiceBox.
 */
public class ChoiceBoxLabelTextTest {

    private Scene scene;
    private Stage stage;
    private Pane root;
    private ChoiceBox<String> box;
    private String uncontained;

// -------------- test change uncontained -> different selected

    @ParameterizedTest
    @MethodSource("parameters")
    public void testChangeUncontainedSelectIndex(StringConverter<String> converter) {
        setup(converter);
        showChoiceBox();
        box.setValue(uncontained);
        box.getSelectionModel().select(1);
        assertEquals(getValueText(), getLabelText(), "label updated after select index ");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testChangeUncontainedSelectItem(StringConverter<String> converter) {
        setup(converter);
        showChoiceBox();
        box.setValue(uncontained);
        box.getSelectionModel().select(box.getItems().get(1));
        assertEquals(getValueText(), getLabelText(), "label updated after select item ");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testChangeUncontainedSelectItemOtherUncontained(StringConverter<String> converter) {
        setup(converter);
        showChoiceBox();
        box.setValue(uncontained);
        box.getSelectionModel().select(uncontained + "xx");
        assertEquals(getValueText(), getLabelText(), "label updated after select item ");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testChangeUncontainedSetValue(StringConverter<String> converter) {
        setup(converter);
        showChoiceBox();
        box.setValue(uncontained);
        box.setValue(box.getItems().get(1));
        assertEquals(getValueText(), getLabelText(), "label updated after select item ");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testChangeContainedSetValueOtherUncontained(StringConverter<String> converter) {
        setup(converter);
        showChoiceBox();
        box.setValue(uncontained);
        box.setValue(uncontained + "xx");
        assertEquals(getValueText(), getLabelText(), "label updated after select item ");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testChangeUncontainedClear(StringConverter<String> converter) {
        setup(converter);
        showChoiceBox();
        box.setValue(uncontained);
        box.getSelectionModel().clearSelection();
        assertEquals(getValueText(), getLabelText(), "label updated after select item ");
    }


// ------------- test change selected contained -> different selected

    @ParameterizedTest
    @MethodSource("parameters")
    public void testChangeContainedSelectIndex(StringConverter<String> converter) {
        setup(converter);
        showChoiceBox();
        int index = 1;
        box.setValue(box.getItems().get(index));
        box.getSelectionModel().select(index -1);
        assertEquals(getValueText(), getLabelText(), "label updated after select index ");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testChangeContainedSelectItem(StringConverter<String> converter) {
        setup(converter);
        showChoiceBox();
        int index = 1;
        box.setValue(box.getItems().get(index));
        box.getSelectionModel().select(box.getItems().get(index -1));
        assertEquals(getValueText(), getLabelText(), "label updated after select item ");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testChangeContainedSelectItemUncontained(StringConverter<String> converter) {
        setup(converter);
        showChoiceBox();
        int index = 1;
        box.setValue(box.getItems().get(index));
        box.getSelectionModel().select(uncontained);
        assertEquals(getValueText(), getLabelText(), "label updated after select item ");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testChangeContainedSetValue(StringConverter<String> converter) {
        setup(converter);
        showChoiceBox();
        int index = 1;
        box.setValue(box.getItems().get(index));
        box.setValue(box.getItems().get(index -1));
        assertEquals(getValueText(), getLabelText(), "label updated after set value ");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testChangeContainedSetValueUncontained(StringConverter<String> converter) {
        setup(converter);
        showChoiceBox();
        int index = 1;
        box.setValue(box.getItems().get(index));
        box.setValue(uncontained);
        assertEquals(getValueText(), getLabelText(), "label updated after set value ");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testChangeContainedClear(StringConverter<String> converter) {
        setup(converter);
        showChoiceBox();
        int index = 1;
        box.setValue(box.getItems().get(index));
        box.getSelectionModel().clearSelection();
        assertEquals(getValueText(), getLabelText(), "label updated after clear selection ");
    }

// ------------- test empty -> selected

    @ParameterizedTest
    @MethodSource("parameters")
    public void testChangeEmptySelectIndex(StringConverter<String> converter) {
        setup(converter);
        showChoiceBox();
        box.getSelectionModel().select(1);
        assertEquals(getValueText(), getLabelText(), "label updated after select index ");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testChangeEmptySelectItem(StringConverter<String> converter) {
        setup(converter);
        showChoiceBox();
        box.getSelectionModel().select(box.getItems().get(1));
        assertEquals(getValueText(), getLabelText(), "label updated after select item ");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testChangeEmptySelectItemUncontained(StringConverter<String> converter) {
        setup(converter);
        showChoiceBox();
        box.getSelectionModel().select(uncontained);
        assertEquals(getValueText(), getLabelText(), "label updated after select item ");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testChangeEmptySetValue(StringConverter<String> converter) {
        setup(converter);
        showChoiceBox();
        box.setValue(box.getItems().get(1));
        assertEquals(getValueText(), getLabelText(), "label updated after set value ");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testChangeEmptySetValueUncontained(StringConverter<String> converter) {
        setup(converter);
        showChoiceBox();
        box.setValue(uncontained);
        assertEquals(getValueText(), getLabelText(), "label updated after set value ");
    }


//------------- test initial label text

    @ParameterizedTest
    @MethodSource("parameters")
    public void testInitialEmpty(StringConverter<String> converter) {
        setup(converter);
        showChoiceBox();
        assertEquals(getValueText(), getLabelText(), "label has empty value ");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testInitialUncontained(StringConverter<String> converter) {
        setup(converter);
        box.setValue(uncontained);
        showChoiceBox();
        assertEquals(getValueText(), getLabelText(), "label has uncontainedValue ");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testInitialUncontained1999(StringConverter<String> converter) {
        setup(converter);
        box.getSelectionModel().select(1);
        box.setValue(uncontained);
        showChoiceBox();
        assertEquals(getValueText(), getLabelText(), "label has uncontainedValue ");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testInitialContained(StringConverter<String> converter) {
        setup(converter);
        int index = 1;
        box.setValue(box.getItems().get(index));
        showChoiceBox();
        assertEquals(getValueText(), getLabelText(), "label has contained value");
    }

//------------- test label text sync after modifications of items

    @ParameterizedTest
    @MethodSource("parameters")
    public void testModifyItemsSetEqualList(StringConverter<String> converter) {
        setup(converter);
        showChoiceBox();
        box.setValue(uncontained);
        box.setItems(FXCollections.observableArrayList(box.getItems()));
        assertEquals(getValueText(), getLabelText(), "label has uncontainedValue ");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testModifyItemsSetItems(StringConverter<String> converter) {
        setup(converter);
        showChoiceBox();
        box.setValue(uncontained);
        box.setItems(FXCollections.observableArrayList("one", "two", "three"));
        assertEquals(getValueText(), getLabelText(), "label has uncontainedValue ");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testModifyItemsSetAll(StringConverter<String> converter) {
        setup(converter);
        showChoiceBox();
        box.setValue(uncontained);
        box.getItems().setAll(FXCollections.observableArrayList("one", "two", "three"));
        assertEquals(getValueText(), getLabelText(), "label has uncontainedValue ");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testModifyItemsRemoveItem(StringConverter<String> converter) {
        setup(converter);
        showChoiceBox();
        box.setValue(uncontained);
        box.getItems().remove(0);
        assertEquals(uncontained, box.getValue(), "sanity: is still set to uncontained");
        assertEquals(getValueText(), getLabelText(), "label has uncontainedValue ");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testModifyItemsReplaceItem(StringConverter<String> converter) {
        setup(converter);
        showChoiceBox();
        box.setValue(uncontained);
        box.getItems().set(0, "replaced");
        assertEquals(getValueText(), getLabelText(), "label has uncontainedValue ");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testModifyItemsAddItem(StringConverter<String> converter) {
        setup(converter);
        showChoiceBox();
        box.setValue(uncontained);
        box.getItems().add(0, "added");
        assertEquals(getValueText(), getLabelText(), "label has uncontainedValue ");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testToggleText(StringConverter<String> converter) {
        setup(converter);
        showChoiceBox();
        ContextMenu popup = ChoiceBoxSkinNodesShim.getChoiceBoxPopup((ChoiceBoxSkin) box.getSkin());
        for (int i = 0; i < popup.getItems().size(); i++) {
            MenuItem item = popup.getItems().get(i);
            assertEquals(getItemText(box.getItems().get(i)), item.getText(), "menuItem text at " + i);
        }
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testToggleConverter(StringConverter<String> converter) {
        setup(converter);
        showChoiceBox();
        box.setValue(uncontained);
        // before fix failing here: initial state incorrect
        assertEquals(getValueText(), getLabelText(), "sanity: label has uncontainedValue ");
        if (box.getConverter() == null) {
            box.setConverter(createStringConverter());
        } else {
            box.setConverter(null);
        }
        assertEquals(uncontained, box.getValue(), "after change converter - sanity: value is set to uncontained");
        assertEquals(getValueText(), getLabelText(), "after change converter - label has uncontainedValue ");
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

    private static Stream<StringConverter<String>> parameters() {
        return Stream.of(
            null,
            createStringConverter()
        );
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

//------------------ setup/cleanup and initial state

    @ParameterizedTest
    @MethodSource("parameters")
    public void testSetupState(StringConverter<String> converter) {
        setup(converter);
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

    @AfterEach
    public void cleanup() {
        stage.hide();
    }

    // @Before
    // junit5 does not support parameterized class-level tests yet
    public void setup(StringConverter<String> converter) {
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
