/*
 * Copyright (c) 2013, 2024, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.Collection;
import java.util.List;
import javafx.scene.control.Cell;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.control.cell.CheckBoxTreeTableCell;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class ParameterisedPrebuiltCellTest {

    private static Collection<Class> parameters() {
        return List.of(
            CheckBoxListCell.class,
            CheckBoxTableCell.class,
            CheckBoxTreeCell.class,
            CheckBoxTreeTableCell.class
        );
    }

    private Cell cell;
    private int count = 0;

    // @BeforeEach
    // junit5 does not support parameterized class-level tests yet
    private void setup(Class<? extends Cell> cellClass) {
        count = 0;
        try {
            cell = cellClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            fail(e);
        }
    }


    /**************************************************************************
     *
     * Text
     *
     **************************************************************************/

    @ParameterizedTest
    @MethodSource("parameters")
    public void testSetText(Class<? extends Cell> cellClass) {
        setup(cellClass);
        assertNull(cell.getText());
        cell.setText("TEST");
        assertEquals("TEST", cell.getText());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testTextProperty(Class<? extends Cell> cellClass) {
        setup(cellClass);
        assertEquals(0, count);
        cell.textProperty().addListener((observable, oldValue, newValue) -> {
            count++;
        });

        cell.setText("TEST");
        assertEquals(1, count);

        cell.setText("TEST");
        assertEquals(1, count);

        cell.setText("TEST 2");
        assertEquals(2, count);

        cell.textProperty().set("TEST");
        assertEquals(3, count);
    }


    /**************************************************************************
     *
     * Graphics
     *
     **************************************************************************/

    @ParameterizedTest
    @MethodSource("parameters")
    public void testSetGraphic(Class<? extends Cell> cellClass) {
        setup(cellClass);
        Rectangle rect = new Rectangle(10, 10, Color.RED);
        cell.setGraphic(rect);
        assertEquals(rect, cell.getGraphic());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testGraphicProperty(Class<? extends Cell> cellClass) {
        setup(cellClass);
        assertEquals(0, count);
        cell.graphicProperty().addListener((observable, oldValue, newValue) -> {
            count++;
        });

        Rectangle rect1 = new Rectangle(10, 10, Color.RED);
        Rectangle rect2 = new Rectangle(10, 10, Color.GREEN);

        cell.setGraphic(rect1);
        assertEquals(1, count);

        cell.setGraphic(rect1);
        assertEquals(1, count);

        cell.setGraphic(rect2);
        assertEquals(2, count);

        cell.graphicProperty().set(rect1);
        assertEquals(3, count);
    }
}
