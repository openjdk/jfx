/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.fx.monkey.sheets;

import javafx.beans.property.ObjectProperty;
import javafx.scene.control.TableColumn;
import com.oracle.tools.fx.monkey.util.OptionPane;

/**
 * TreeTableView/TableView (Selected) Column Property Sheet
 */
// TODO this is not used
public class TableColumnPropertySheet {
//    public TableColumnOptions(String name, ObjectProperty<TableColumn> p) {
//        super(name, (v) -> {
//            var col = p.get();
//            if(col != null) {
//                col.set
//            }
//            updateValue(v);
//        });
//        disableProperty().bind(Bindings.createBooleanBinding(() -> {
//            return p.get() == null;
//        }, p));
//
//        this.currentColumn = p;
//    }

    public static void appendTo(OptionPane op, ObjectProperty<TableColumn> currentColumn) {
        op.section("Current Column");
        op.option("Cell Factory: TODO", null); // TODO
        op.option("Cell Value Factory: TODO", null); // TODO
        op.option("Comparator: TODO", null); // TODO
        op.option("Context Menu: TODO", null); // TODO
        //op.option(new BooleanOption("editable", "editable", null)); // FIX how to set properties dynamically? Optional?
        op.option("Graphic: TODO", null); // TODO
        op.option("Id: TODO", null); // TODO
        op.option("Max Width: TODO", null); // TODO
        op.option("Min Width: TODO", null); // TODO
        op.option("Pref Width: TODO", null); // TODO
        op.option("reorderable: TODO", null); // TODO
        op.option("resizeable: TODO", null); // TODO
        op.option("Sort Type: TODO", null); // TODO
        op.option("sortable: TODO", null); // TODO
        op.option("Sort Node: TODO", null); // TODO
        op.option("Style: TODO", null); // TODO
        op.option("Text: TODO", null); // TODO
        op.option("User Data: TODO", null); // TODO
        op.option("visible: TODO", null); // TODO
        op.option(": TODO", null); // TODO
    }
}
