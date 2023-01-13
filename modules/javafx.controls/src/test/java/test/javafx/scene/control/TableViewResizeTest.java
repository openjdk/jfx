/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.ConstrainedColumnResizeBase;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.util.Callback;
import org.junit.Test;
import com.sun.javafx.tk.Toolkit;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;

/**
 * Tests TableView constrained resize policies.
 */
public class TableViewResizeTest extends ResizeHelperTestBase {

    protected void checkInvariants(TableView<String> t) {
        List<TableColumn<String,?>> cols = t.getColumns();
        checkInvariants(cols);
    }

    protected static TableView<String> createTable(Object[] spec) {
        TableView<String> table = new TableView();
        table.getSelectionModel().setCellSelectionEnabled(true);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        TableColumn<String, String> lastColumn = null;
        int id = 1;

        for (int i = 0; i < spec.length;) {
            Object x = spec[i++];
            if (x instanceof Cmd cmd) {
                switch (cmd) {
                case COL:
                    TableColumn<String, String> c = new TableColumn<>();
                    table.getColumns().add(c);
                    c.setText("C" + table.getColumns().size());
                    c.setCellValueFactory((f) -> new SimpleStringProperty(" "));
                    lastColumn = c;
                    break;
                case MAX:
                    {
                        int w = (int)(spec[i++]);
                        lastColumn.setMaxWidth(w);
                    }
                    break;
                case MIN:
                    {
                        int w = (int)(spec[i++]);
                        lastColumn.setMinWidth(w);
                    }
                    break;
                case PREF:
                    {
                        int w = (int)(spec[i++]);
                        lastColumn.setPrefWidth(w);
                    }
                    break;
                case ROWS:
                    int n = (int)(spec[i++]);
                    for (int j = 0; j < n; j++) {
                        table.getItems().add(String.valueOf(n));
                    }
                    break;
                case COMBINE:
                    int ix = (int)(spec[i++]);
                    int ct = (int)(spec[i++]);
                    combineColumns(table, ix, ct, id++);
                    break;
                default:
                    throw new Error("?" + cmd);
                }
            } else {
                throw new Error("?" + x);
            }
        }

        return table;
    }

    protected static void combineColumns(TableView<String> t, int ix, int count, int name) {
        TableColumn<String,String> tc = new TableColumn<>();
        tc.setText("N" + name);

        for (int i = 0; i < count; i++) {
            TableColumn<String, String> c = (TableColumn<String, String>)t.getColumns().remove(ix);
            tc.getColumns().add(c);
        }
        t.getColumns().add(ix, tc);
    }

    /** verify that a custom constrained resize policy can indeed be implemented using public APIs */
    @Test
    public void testCanImplementCustomResizePolicy() {
        double WIDTH = 15.0;

        // constrained resize policy that simply sets all column widths to WIDTH
        class UserPolicy
            extends ConstrainedColumnResizeBase
            implements Callback<TableView.ResizeFeatures, Boolean> {

            @Override
            public Boolean call(TableView.ResizeFeatures rf) {
                List<? extends TableColumnBase<?, ?>> columns = rf.getTable().getVisibleLeafColumns();
                int sz = columns.size();
                // new public method getContentWidth() is visible
                double w = rf.getContentWidth();
                for (TableColumnBase<?, ?> c: columns) {
                    // using added public method setColumnWidth()
                    rf.setColumnWidth(c, WIDTH);
                }
                return false;
            }
        }

        Object[] spec = {
            Cmd.ROWS, 3,
            Cmd.COL,
            Cmd.COL,
            Cmd.COL,
            Cmd.COL
        };
        TableView<String> table = createTable(spec);

        UserPolicy policy = new UserPolicy();
        table.setColumnResizePolicy(policy);
        table.setPrefWidth(10);

        // verify the policy is in effect
        stageLoader = new StageLoader(new BorderPane(table));
        Toolkit.getToolkit().firePulse();

        for (TableColumn<?, ?> c: table.getColumns()) {
            assertEquals(WIDTH, c.getWidth(), EPSILON);
        }

        // resize and check again
        table.setPrefWidth(10_000);
        Toolkit.getToolkit().firePulse();

        for (TableColumn<?, ?> c: table.getColumns()) {
            assertEquals(WIDTH, c.getWidth(), EPSILON);
        }
    }

    /**
     * Exhausive behavioral test.
     *
     * Goes through all the policies, all valid combinations of constraints,
     * and widths increasing to MAX_WIDTH and back,
     * checkint that the initial resize does not violate (min,max) constraints.
     */
    //@Test // this test takes too much time!
    public void testWidthChange() {
        int[] COLUMNS = {
            0, 1, 2, 5
        };
        long start = System.currentTimeMillis();
        for (int numCols: COLUMNS) {
            SpecGen gen = new SpecGen(numCols);
            while (gen.hasNext()) {
                Object[] spec = gen.next();
                TableView<String> table = createTable(spec);
                stageLoader = new StageLoader(new BorderPane(table));
                try {
                    for (int ip = 0; ip < POLICIES.length; ip++) {
                        Callback<TableView.ResizeFeatures, Boolean> policy = createPolicy(ip);
                        table.setColumnResizePolicy(policy);
                        for (int width: gen.WIDTHS) {
                            table.setPrefWidth(width);
                            Toolkit.getToolkit().firePulse();
                            checkInvariants(table);
                        }
                    }
                } finally {
                    stageLoader.dispose();
                }
            }
        }

        System.out.println("elapsed time = " + (System.currentTimeMillis() - start) / 60_000 + " minutes.");
    }

    protected static final Object[] POLICIES = {
        TableView.CONSTRAINED_RESIZE_POLICY_FLEX_NEXT_COLUMN,
        TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN,
        TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS,
        TableView.CONSTRAINED_RESIZE_POLICY_LAST_COLUMN,
        TableView.CONSTRAINED_RESIZE_POLICY_NEXT_COLUMN,
        TableView.CONSTRAINED_RESIZE_POLICY_SUBSEQUENT_COLUMNS
    };

    protected static Callback<TableView.ResizeFeatures, Boolean> createPolicy(int ix) {
        return (Callback<TableView.ResizeFeatures, Boolean>)POLICIES[ix];
    }
}
