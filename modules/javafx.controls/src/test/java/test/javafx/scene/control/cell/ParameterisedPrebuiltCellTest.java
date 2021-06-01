/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.Collection;

import javafx.scene.control.Cell;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.control.cell.CheckBoxTreeTableCell;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class ParameterisedPrebuiltCellTest {

    @Parameters public static Collection implementations() {
        return Arrays.asList(new Object[][] {
            { CheckBoxListCell.class },
            { CheckBoxTableCell.class },
            { CheckBoxTreeCell.class },
            { CheckBoxTreeTableCell.class },
        });
    }

    private Class<? extends Cell> cellClass;
    private Cell cell;

    private int count = 0;

    public ParameterisedPrebuiltCellTest(Class<? extends Cell> cellClass) {
        this.cellClass = cellClass;
    }

    @Before public void setup() throws Exception {
        count = 0;
        cell = cellClass.getDeclaredConstructor().newInstance();
    }


    /**************************************************************************
     *
     * Text
     *
     **************************************************************************/

    @Test public void testSetText() {
        assertNull(cell.getText());
        cell.setText("TEST");
        assertEquals("TEST", cell.getText());
    }

    @Test public void testTextProperty() {
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

    @Test public void testSetGraphic() {
        Rectangle rect = new Rectangle(10, 10, Color.RED);
        cell.setGraphic(rect);
        assertEquals(rect, cell.getGraphic());
    }

    @Test public void testGraphicProperty() {
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
