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
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.control.cell.ChoiceBoxTreeCell;
import javafx.scene.control.cell.ComboBoxTreeCell;
import javafx.scene.control.cell.TextFieldTreeCell;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;


/**
 * Parameterized tests for the {@link TreeCell#startEdit()} method of {@link TreeCell} and all sub implementations.
 * The {@link CheckBoxTreeCell} is special as in there the checkbox will be disabled based of the editability.
 */
public class TreeCellStartEditTest {

    private static final boolean[] EDITABLE_STATES = { true, false };

    private TreeView<String> treeView;
    private TreeCell<String> treeCell;

    private static Collection<Supplier<TreeCell<String>>> parameters() {
        return List.of(
            TreeCell::new,
            ComboBoxTreeCell::new,
            TextFieldTreeCell::new,
            ChoiceBoxTreeCell::new,
            () -> new CheckBoxTreeCell<>(obj -> new SimpleBooleanProperty())
        );
    }

    // @BeforeEach
    // junit5 does not support parameterized class-level tests yet
    public void setup(Supplier<TreeCell<String>> treeCellSupplier) {
        TreeItem<String> root = new TreeItem<>("1");
        root.getChildren().addAll(List.of(new TreeItem<>("2"), new TreeItem<>("3")));
        treeView = new TreeView<>(root);

        treeCell = treeCellSupplier.get();
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testStartEditMustNotThrowNPE(Supplier<TreeCell<String>> treeCellSupplier) {
        setup(treeCellSupplier);
        // A tree cell without anything attached should not throw a NPE.
        treeCell.startEdit();
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testStartEditRespectsEditable(Supplier<TreeCell<String>> treeCellSupplier) {
        setup(treeCellSupplier);
        treeCell.updateIndex(0);

        treeCell.updateTreeView(treeView);

        for (boolean isTreeViewEditable : EDITABLE_STATES) {
            for (boolean isCellEditable : EDITABLE_STATES) {
                testStartEditImpl(isTreeViewEditable, isCellEditable);
            }
        }
    }

    /**
     * A {@link TreeCell} (or sub implementation) should be editable (thus, can be in editing state), if the
     * corresponding tree view and cell is editable.
     *
     * @param isTreeViewEditable true, when the tree view should be editable, false otherwise
     * @param isCellEditable true, when the cell should be editable, false otherwise
     */
    private void testStartEditImpl(boolean isTreeViewEditable, boolean isCellEditable) {
        assertFalse(treeCell.isEditing());

        treeView.setEditable(isTreeViewEditable);
        treeCell.setEditable(isCellEditable);

        treeCell.startEdit();

        boolean expectedEditingState = isTreeViewEditable && isCellEditable;
        assertEquals(expectedEditingState, treeCell.isEditing());

        if (treeCell instanceof CheckBoxTreeCell) {
            assertNotNull(treeCell.getGraphic());
            // Ignored until https://bugs.openjdk.org/browse/JDK-8270042 is resolved.
            // Check if the checkbox is disabled when not editable.
            // assertEquals(expectedEditingState, !treeCell.getGraphic().isDisabled());
        } else if (!treeCell.getClass().equals(TreeCell.class)) {
            // All other sub implementation should show a graphic when editable.
            assertEquals(expectedEditingState, treeCell.getGraphic() != null);
        }

        // Restore the editing state.
        treeCell.cancelEdit();
    }

}
