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

package test.javafx.scene.control;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.control.cell.ChoiceBoxTreeCell;
import javafx.scene.control.cell.ComboBoxTreeCell;
import javafx.scene.control.cell.TextFieldTreeCell;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Parameterized tests for the {@link TreeCell#startEdit()} method of {@link TreeCell} and all sub implementations.
 * The {@link CheckBoxTreeCell} is special as in there the checkbox will be disabled
 * based of the editability.
 */
@RunWith(Parameterized.class)
public class TreeCellStartEditTest {

    private static final boolean[] EDITABLE_STATES = { true, false };

    private TreeView<String> treeView;
    private final TreeCell<String> treeCell;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return wrapAsObjectArray(List.of(new TreeCell<>(), new ComboBoxTreeCell<>(), new TextFieldTreeCell<>(),
                new ChoiceBoxTreeCell<>(), new CheckBoxTreeCell<>(obj -> new SimpleBooleanProperty())));
    }

    private static Collection<Object[]> wrapAsObjectArray(List<TreeCell<String>> treeCells) {
        return treeCells.stream().map(tc -> new Object[] { tc }).collect(toList());
    }

    public TreeCellStartEditTest(TreeCell<String> treeCell) {
        this.treeCell = treeCell;
    }

    @Before
    public void setup() {
        TreeItem<String> root = new TreeItem<>("1");
        root.getChildren().addAll(List.of(new TreeItem<>("2"), new TreeItem<>("3")));
        treeView = new TreeView<>(root);
    }

    @Test
    public void testStartEdit() {
        // First test startEdit() without anything set yet.
        treeCell.startEdit();

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

        // Ignored until https://bugs.openjdk.java.net/browse/JDK-8270042 is resolved.
        // Special check for CheckBoxTreeCell.
//        if (treeCell instanceof CheckBoxTreeCell) {
//            assertEquals(expectedEditingState, !treeCell.getGraphic().isDisabled());
//        }

        // Restore the editing state.
        treeCell.cancelEdit();
    }

}
