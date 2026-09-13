/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
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

import javafx.scene.layout.Region;
import com.oracle.tools.fx.monkey.options.BooleanOption;
import com.oracle.tools.fx.monkey.options.BorderOption;
import com.oracle.tools.fx.monkey.options.InsetsOption;
import com.oracle.tools.fx.monkey.util.OptionPane;

/**
 * Region Property Sheet.
 */
public class RegionPropertySheet {
    public static void appendTo(OptionPane op, Region r) {
        op.section("Region");
        op.option("Background:", Options.background(r, "background", r.backgroundProperty()));
        op.option("Border:", new BorderOption("border", r.borderProperty()));
        op.option(new BooleanOption("cacheShape", "cache shape", r.cacheShapeProperty()));
        op.option(new BooleanOption("centerShape", "center shape", r.centerShapeProperty()));
        op.option("Max Height:", Options.forRegion("maxHeight", r.maxHeightProperty()));
        op.option("Max Width:", Options.forRegion("maxWidth", r.maxWidthProperty()));
        op.option("Min Height:", Options.forRegion("minHeight", r.minHeightProperty()));
        op.option("Min Width:", Options.forRegion("minWidth", r.minWidthProperty()));
        op.option("Opaque Insets:", new InsetsOption("opaqueInsets", true, r.opaqueInsetsProperty()));
        op.option("Padding:", new InsetsOption("padding", false, r.paddingProperty()));
        op.option("Pref Height:", Options.forRegion("prefHeight", r.prefHeightProperty()));
        op.option("Pref Width:", Options.forRegion("prefWidth", r.prefWidthProperty()));
        op.option(new BooleanOption("scaleShape", "scale shape", r.scaleShapeProperty()));
        op.option("Shape:", Options.shape("shape", r, r.shapeProperty()));
        op.option(new BooleanOption("snapToPixel", "snap to pixel", r.snapToPixelProperty()));

        NodePropertySheet.appendTo(op, r);
    }
}
