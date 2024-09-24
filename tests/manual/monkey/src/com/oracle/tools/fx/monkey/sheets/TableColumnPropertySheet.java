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

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.layout.BorderPane;
import com.oracle.tools.fx.monkey.options.BooleanOption;
import com.oracle.tools.fx.monkey.options.EnumOption;
import com.oracle.tools.fx.monkey.options.GraphicOption;
import com.oracle.tools.fx.monkey.options.TextOption;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.OptionWindow;

/**
 * TreeTableView/TableView (Selected) Column Property Sheet
 */
public class TableColumnPropertySheet extends BorderPane {
    protected TableColumnPropertySheet(TableColumnBase<?,?> c) {
        OptionPane op = new OptionPane();
        if(c instanceof TableColumn tc) {
            tableColumnOptions(op, tc);
        } else if(c instanceof TreeTableColumn tc) {
            treeTableColumnOptions(op, tc);
        }
        tableColumnBaseOptions(op, c);

        StyleablePropertySheet.appendTo(op, c);
        setCenter(op);
    }

    public static void open(Object parent, TableColumnBase<?, ?> c) {
        String name = c.getText();
        if (name == null) {
            name = "<null>";
        } else {
            name = " [" + name + "]";
        }

        TableColumnPropertySheet p = new TableColumnPropertySheet(c);
        OptionWindow.open(parent, "Table Column Properties" + name, 500, 800, p);
    }

    private void tableColumnOptions(OptionPane op, TableColumn<?, ?> c) {
        op.section("TableColumn");
        op.option("Cell Factory: TODO", null); // TODO
        op.option("Cell Value Factory: TODO", null); // TODO
        op.option("Sort Type:", new EnumOption(null, TableColumn.SortType.class, c.sortTypeProperty()));
    }

    private void treeTableColumnOptions(OptionPane op, TreeTableColumn<?, ?> c) {
        op.section("TreeTableColumn");
        op.option("Cell Factory: TODO", null); // TODO
        op.option("Cell Value Factory: TODO", null); // TODO
        op.option("Sort Type:", new EnumOption(null, TreeTableColumn.SortType.class, c.sortTypeProperty()));
    }

    private void tableColumnBaseOptions(OptionPane op, TableColumnBase<?, ?> c) {
        op.section("TableColumnBase");
        op.option("Comparator: TODO", null); // TODO
        op.option("Context Menu: TODO", null); // TODO
        op.option(new BooleanOption(null, "editable", c.editableProperty()));
        op.option("Graphic:", new GraphicOption("graphic", c.graphicProperty()));
        op.option("Id:", new TextOption("id", c.idProperty()));
        op.option("Max Width:", Options.forColumnWidth("maxWidth", 5000.0, c.maxWidthProperty()));
        op.option("Min Width:", Options.forColumnWidth("minWidth", 10.0, c.minWidthProperty()));
        op.option("Pref Width:", Options.forColumnWidth("prefWidth", 80.0, c.prefWidthProperty()));
        op.option(new BooleanOption(null, "reorderable", c.reorderableProperty()));
        op.option(new BooleanOption(null, "resizeable", c.resizableProperty()));
        op.option(new BooleanOption(null, "sortable", c.sortableProperty()));
        op.option("Sort Node: TODO", null); // TODO
        op.option("Style:", new TextOption("style", c.styleProperty()));
        op.option("Text:", new TextOption("text", c.textProperty()));
        op.option("User Data: TODO", null); // TODO
        op.option(new BooleanOption(null, "visible", c.visibleProperty()));
    }
}
