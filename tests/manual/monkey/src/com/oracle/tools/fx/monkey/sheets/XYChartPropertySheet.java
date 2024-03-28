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

import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import com.oracle.tools.fx.monkey.options.BooleanOption;
import com.oracle.tools.fx.monkey.pages.XYChartPageBase;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.Utils;

/**
 * XYChart Property Sheet.
 */
public class XYChartPropertySheet {
    public static void appendTo(XYChartPageBase owner, OptionPane op, XYChart<?,Number> chart) {
        op.section("XYChart");
        op.option(new BooleanOption("alternativeColumnFillVisible", "alternative column fill visible", chart.alternativeColumnFillVisibleProperty()));
        op.option(new BooleanOption("alternativeRowFillVisible", "alternative row fill visible", chart.alternativeRowFillVisibleProperty()));
        op.option("Data:", Utils.buttons(
            FX.button("Add Series", "Adds random series to the chart", owner::addSeries),
            FX.button("Remove", "Removes first series from the chart", owner::removeSeries),
            FX.button("Add/Remove", "Removes the first series then immediately adds that series back", owner::addRemoveSeries)
        ));
        op.option(Utils.buttons(
            FX.button("Add Point", "Randomly adds a point random point to the first series", owner::addPoint),
            FX.button("Remove", "Randomly removes a point from the first series", owner::removePoint),
            FX.button("ΔX", "Changes the X value of a point", owner::changePointX),
            FX.button("ΔY", "Changes the Y value of a point", owner::changePointY),
            FX.button("Clear", "Removes all the points in the first series", owner::clearPoints)
        ));
        // TODO context menu?
        op.option(new BooleanOption("horizontalGridLinesVisible", "horizontal grid lines visible", chart.horizontalGridLinesVisibleProperty()));
        op.option(new BooleanOption("horizontalZeroLineVisible", "horizontal zero line visible", chart.horizontalZeroLineVisibleProperty()));
        op.option(new BooleanOption("verticalGridLinesVisible", "vertical grid lines visible", chart.verticalGridLinesVisibleProperty()));
        op.option(new BooleanOption("verticalZeroLineVisible", "vertical zero line visible", chart.verticalZeroLineVisibleProperty()));

        ChartPropertySheet.appendTo(op, chart);
    }
}
