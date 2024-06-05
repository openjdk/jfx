/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.TableColumnBase;
import org.junit.After;
import test.com.sun.javafx.scene.control.infrastructure.StageLoader;

/**
 * Base class for tests the new column resize policies.
 * The two descendants are TableViewResizeTest and TreeTableViewResizeTest.
 */
public abstract class ResizeHelperTestBase {

    public enum Cmd {
        ROWS,
        COL,
        MIN,
        PREF,
        MAX,
        COMBINE
    }

    protected static final double EPSILON = 0.000001;
    protected StageLoader stageLoader;

    @After
    public void after() {
        if (stageLoader != null) {
            stageLoader.dispose();
        }
    }

    protected void checkInvariants(List<? extends TableColumnBase<?,?>> cols) {
        for (TableColumnBase<?,?> c: cols) {
            assertTrue("violated min constraint: w=" + c.getWidth() + " min=" + c.getMinWidth(),
                       c.getWidth() >= c.getMinWidth());
            assertTrue("violated max constraint: w=" + c.getWidth() + " max=" + c.getMaxWidth(),
                       c.getWidth() <= c.getMaxWidth());
        }
    }

    protected static double sumColumnWidths(List<? extends TableColumnBase<?,?>> cols) {
        double w = 0.0;
        for (TableColumnBase<?,?> c: cols) {
            w += c.getWidth();
        }
        return w;
    }

    protected static class SpecGen {
        public static final int[] WIDTHS = {
            0, 10, 100, 10_000, 200, 50
        };
        private static final int LAST = 8; // 2^3 min,pref,max + 1 fixed
        private final int[] phase;

        public SpecGen(int numcols) {
            this.phase = new int[numcols];
        }

        public boolean hasNext() {
            int terminal = LAST;
            for (int n: phase) {
                if (n != terminal) {
                    return true;
                }
            }
            return false;
        }

        public Object[] next() {
            ArrayList<Object> rv = new ArrayList<>(phase.length);
            for (int i = 0; i < phase.length; i++) {
                rv.add(Cmd.COL);

                int n = phase[i];
                if (n < 8) {
                    if ((n & 0x01) != 0) {
                        rv.add(Cmd.MIN);
                        rv.add(100);
                    }

                    if ((n & 0x02) != 0) {
                        rv.add(Cmd.PREF);
                        rv.add(200 + 50 * i);
                    }

                    if ((n & 0x04) != 0) {
                        rv.add(Cmd.MAX);
                        rv.add(200 + 100 * i);
                    }
                } else if (n == LAST) {
                    rv.add(Cmd.MIN);
                    rv.add(50);
                    rv.add(Cmd.MAX);
                    rv.add(50);
                }
            }
            return rv.toArray();
        }
    }
}
