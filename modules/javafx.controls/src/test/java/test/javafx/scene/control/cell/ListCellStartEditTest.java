/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.control.cell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.control.cell.ChoiceBoxListCell;
import javafx.scene.control.cell.ComboBoxListCell;
import javafx.scene.control.cell.TextFieldListCell;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Parameterized tests for the {@link ListCell#startEdit()} method of {@link ListCell} and all sub implementations.
 * The {@link CheckBoxListCell} is special as in there the checkbox will be disabled based of the editability.
 */
public class ListCellStartEditTest {

    private static final boolean[] EDITABLE_STATES = { true, false };

    private ListView<String> listView;
    private ListCell<String> listCell;

    private static Collection<Supplier<ListCell<String>>> parameters() {
        return List.of(
            ListCell::new,
            ComboBoxListCell::new,
            TextFieldListCell::new,
            ChoiceBoxListCell::new,
            () -> new CheckBoxListCell<>(obj -> new SimpleBooleanProperty())
        );
    }

    // @BeforeEach
    // junit5 does not support parameterized class-level tests yet
    public void setup(Supplier<ListCell<String>> listCellSupplier) {
        ObservableList<String> items = FXCollections.observableArrayList("1", "2", "3");
        listView = new ListView<>(items);

        listCell = listCellSupplier.get();
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testStartEditMustNotThrowNPE(Supplier<ListCell<String>> listCellSupplier) {
        setup(listCellSupplier);
        // A list cell without anything attached should not throw a NPE.
        listCell.startEdit();
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testStartEditRespectsEditable(Supplier<ListCell<String>> listCellSupplier) {
        setup(listCellSupplier);
        listCell.updateIndex(0);
        listCell.updateListView(listView);

        for (boolean isListViewEditable : EDITABLE_STATES) {
            for (boolean isCellEditable : EDITABLE_STATES) {
                testStartEditImpl(isListViewEditable, isCellEditable);
            }
        }
    }

    /**
     * A {@link ListCell} (or sub implementation) should be editable (thus, can be in editing state), if the
     * corresponding list view and cell is editable.
     *
     * @param isListViewEditable true, when the list view should be editable, false otherwise
     * @param isCellEditable true, when the cell should be editable, false otherwise
     */
    private void testStartEditImpl(boolean isListViewEditable, boolean isCellEditable) {
        assertFalse(listCell.isEditing());

        listView.setEditable(isListViewEditable);
        listCell.setEditable(isCellEditable);

        listCell.startEdit();

        boolean expectedEditingState = isListViewEditable && isCellEditable;
        assertEquals(expectedEditingState, listCell.isEditing());

        if (listCell instanceof CheckBoxListCell) {
            assertNotNull(listCell.getGraphic());
            // Ignored until https://bugs.openjdk.org/browse/JDK-8270042 is resolved.
            // Check if the checkbox is disabled when not editable.
            // assertEquals(expectedEditingState, !listCell.getGraphic().isDisabled());
        } else if (!listCell.getClass().equals(ListCell.class)) {
            // All other sub implementation should show a graphic when editable.
            assertEquals(expectedEditingState, listCell.getGraphic() != null);
        }

        // Restore the editing state.
        listCell.cancelEdit();
    }
}
