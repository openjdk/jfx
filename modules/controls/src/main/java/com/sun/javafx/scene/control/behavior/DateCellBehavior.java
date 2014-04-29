/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control.behavior;

import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.control.DateCell;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import com.sun.javafx.scene.control.skin.DatePickerContent;
import com.sun.javafx.scene.traversal.Direction;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MONTHS;
import static java.time.temporal.ChronoUnit.WEEKS;
import static javafx.scene.input.KeyCode.DOWN;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.LEFT;
import static javafx.scene.input.KeyCode.PAGE_DOWN;
import static javafx.scene.input.KeyCode.PAGE_UP;
import static javafx.scene.input.KeyCode.RIGHT;
import static javafx.scene.input.KeyCode.SPACE;
import static javafx.scene.input.KeyCode.UP;
import static javafx.scene.input.KeyEvent.*;

/**
 * Behaviors for LocalDate based cells types. Simply defines methods
 * that subclasses implement so that CellSkinBase has API to call.
 *
 */
public class DateCellBehavior extends BehaviorBase<DateCell> {

    /**************************************************************************
     *                          Setup KeyBindings                             *
     *************************************************************************/
    protected static final List<KeyBinding> DATE_CELL_BINDINGS = new ArrayList<KeyBinding>();
    static {
        DATE_CELL_BINDINGS.add(new KeyBinding(UP, "TraverseUp"));
        DATE_CELL_BINDINGS.add(new KeyBinding(DOWN, "TraverseDown"));
        DATE_CELL_BINDINGS.add(new KeyBinding(LEFT, "TraverseLeft"));
        DATE_CELL_BINDINGS.add(new KeyBinding(RIGHT, "TraverseRight"));
        DATE_CELL_BINDINGS.add(new KeyBinding(ENTER, KEY_RELEASED, "SelectDate"));
        DATE_CELL_BINDINGS.add(new KeyBinding(SPACE, KEY_RELEASED, "SelectDate"));
    }


    public DateCellBehavior(DateCell dateCell) {
        super(dateCell, DATE_CELL_BINDINGS);
    }

    @Override public void callAction(String name) {
        DateCell cell = getControl();
        DatePickerContent dpc = findDatePickerContent(cell);

        if (dpc != null) {
            switch (name) {
              case "SelectDate":    dpc.selectDayCell(cell); break;
              default: super.callAction(name);
            }
            return;
        }
        super.callAction(name);
    }

    @Override public void traverse(final Node node, final Direction dir) {
        boolean rtl = (node.getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT);

        switch (dir) {
          case UP:
          case DOWN:
          case LEFT:
          case RIGHT:
              if (node instanceof DateCell) {
                  DatePickerContent dpc = findDatePickerContent(node);
                  if (dpc != null) {
                      DateCell cell = (DateCell)node;
                      switch (dir) {
                        case UP:    dpc.goToDayCell(cell, -1, WEEKS, true); break;
                        case DOWN:  dpc.goToDayCell(cell, +1, WEEKS, true); break;
                        case LEFT:  dpc.goToDayCell(cell, rtl ? +1 : -1, DAYS,  true); break;
                        case RIGHT: dpc.goToDayCell(cell, rtl ? -1 : +1, DAYS,  true); break;
                      }
                      return;
                  }
              }
        }
        super.traverse(node, dir);
    }

    protected DatePickerContent findDatePickerContent(Node node) {
        Node parent = node;
        while ((parent = parent.getParent()) != null && !(parent instanceof DatePickerContent));
        return (DatePickerContent)parent;
    }
}
