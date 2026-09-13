/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.skin.LabeledSkinBase;
import javafx.scene.control.skin.LabeledSkinBaseShim;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.sun.javafx.tk.Toolkit;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;
import test.com.sun.javafx.scene.control.infrastructure.VirtualFlowTestUtils;

/**
 * Tests for the indentation of the {@link javafx.scene.control.TreeTableView}.
 */
public class TreeTableViewIndentationTest {

    private TreeTableView<String> treeTableView;
    private TreeTableColumn<String, String> column;
    private StageLoader stageLoader;

    @BeforeEach
    public void setup() {
        treeTableView = new TreeTableView<>();

        column = new TreeTableColumn<>("Column");
        treeTableView.getColumns().add(column);

        TreeItem<String> root = new TreeItem<>("Root");
        root.getChildren().addAll(List.of(new TreeItem<>("TreeItem 1"), new TreeItem<>("TreeItem 2")));
        treeTableView.setRoot(root);

        stageLoader = new StageLoader(treeTableView);
    }

    @AfterEach
    public void cleanup() {
        stageLoader.dispose();
    }

    @Test
    public void testIndentationOfCell() {
        column.setCellFactory(col -> new TreeTableCell<>());

        testXCoordinateIsAfterDisclosureNode();
    }

    @Test
    public void testIndentationOfCellWithGraphic() {
        column.setCellFactory(col -> new TreeTableCell<>() {

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(new Label("Graphic"));
                }
            }
        });

        testXCoordinateIsAfterDisclosureNode();
    }

    @Test
    public void testIndentationOfCellWithText() {
        column.setCellFactory(col -> new TreeTableCell<>() {

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setText(null);
                } else {
                    setText("Text");
                }
            }
        });

        testXCoordinateIsAfterDisclosureNode();
    }

    @Test
    public void testIndentationOfCellWithGraphicAndText() {
        column.setCellFactory(col -> new TreeTableCell<>() {

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                    setText(null);
                } else {
                    setGraphic(new Label("Graphic"));
                    setText("Text");
                }
            }
        });

        testXCoordinateIsAfterDisclosureNode();
    }

    private void testXCoordinateIsAfterDisclosureNode() {
        Toolkit.getToolkit().firePulse();

        TreeTableRow<String> row = (TreeTableRow<String>) VirtualFlowTestUtils.getCell(treeTableView, 0);
        TreeTableCell<String, String> cell = (TreeTableCell<String, String>) row.getChildrenUnmodifiable().get(0);

        Node graphic = cell.getGraphic();
        double x;
        if (graphic != null) {
            x = graphic.getLayoutX();
        } else {
            x = LabeledSkinBaseShim.get_text((LabeledSkinBase<TreeTableCell>) cell.getSkin()).getLayoutX();
        }

        double leftInset = cell.snappedLeftInset();
        double disclosureNodeWidth = row.getDisclosureNode().prefWidth(-1);
        double expectedX = leftInset + disclosureNodeWidth;

        assertEquals(expectedX, x, 0);
    }

}
