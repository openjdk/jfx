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

import javafx.geometry.NodeOrientation;
import javafx.scene.AccessibleRole;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.effect.BlendMode;
import com.oracle.tools.fx.monkey.options.BooleanOption;
import com.oracle.tools.fx.monkey.options.DoubleOption;
import com.oracle.tools.fx.monkey.options.DoubleSpinner;
import com.oracle.tools.fx.monkey.options.EnumOption;
import com.oracle.tools.fx.monkey.options.StyleClassOption;
import com.oracle.tools.fx.monkey.options.TextOption;
import com.oracle.tools.fx.monkey.util.OptionPane;

/**
 * Node Property Sheet.
 */
public class NodePropertySheet {
    public static void appendTo(OptionPane op, Node n) {
        op.section("Node");
        op.option("Accessible Help:", new TextOption("accessibleHelp", n.accessibleHelpProperty()));
        op.option("Accessible Role:", new EnumOption<>("accessibleRole", AccessibleRole.class, n.accessibleRoleProperty()));
        op.option("Accessible Role Description:", new TextOption("accessibleRoleDescription", n.accessibleRoleDescriptionProperty()));
        op.option("Accessible Text:", new TextOption("accessibleText", n.accessibleTextProperty()));
        op.option("Blend Mode:", new EnumOption<>("blendMode", BlendMode.class, n.blendModeProperty()));
        op.option(new BooleanOption("cache", "cache", n.cacheProperty()));
        op.option("Cache Hint:", new EnumOption<>("cacheHint", CacheHint.class, n.cacheHintProperty()));
        op.option("Clip:", Options.clip("clip", n, n.clipProperty()));
        op.option("Cursor: TODO", null); // TODO
        op.option("Depth Test:", new EnumOption<>("depthText", CacheHint.class, n.cacheHintProperty()));
        op.option(new BooleanOption("disable", "disable", n.disableProperty()));
        op.option("Effect: TODO", null); // TODO
        op.option(new BooleanOption("focusTraversable", "focus traversable", n.focusTraversableProperty()));
        op.option("Id:", new TextOption("id", n.idProperty()));
        op.option("Input Method Requests: TODO", null); // TODO
        op.option("Layout X:", new DoubleOption("layoutX", n.layoutXProperty()));
        op.option("Layout Y:", new DoubleOption("layoutY", n.layoutYProperty()));
        op.option(new BooleanOption("managed", "managed", n.managedProperty()));
        op.option(new BooleanOption("mouseTransparent", "mouse transparent", n.mouseTransparentProperty()));
        op.option("Node Orientation:", new EnumOption<>("nodeOrientation", NodeOrientation.class, n.nodeOrientationProperty()));
        op.option("Opacity:", new DoubleSpinner("opacity", -0.1, 1.1, 0.1, n.opacityProperty()));
        op.option(new BooleanOption("pickOnBounds", "pick on bounds", n.pickOnBoundsProperty()));
        op.option("Rotate:", new DoubleOption("rotate", n.rotateProperty()));
        op.option("Rotation Axis: TODO", null); // TODO
        op.option("Scale X:", new DoubleOption("scaleX", n.scaleXProperty()));
        op.option("Scale Y:", new DoubleOption("scaleY", n.scaleYProperty()));
        op.option("Scale Z:", new DoubleOption("scaleZ", n.scaleZProperty()));
        op.option("Style:", new TextOption("style", n.styleProperty()));
        op.option("Style Class:", new StyleClassOption("styleClass", n.getStyleClass()));
        op.option("Translate X:", new DoubleOption("translateX", n.translateXProperty()));
        op.option("Translate Y:", new DoubleOption("translateY", n.translateYProperty()));
        op.option("Translate Z:", new DoubleOption("translateZ", n.translateZProperty()));
        op.option("User Data: TODO", null); // TODO
        op.option("View Order:", new DoubleOption("viewOrder", n.viewOrderProperty()));
        op.option(new BooleanOption("visible", "visible", n.visibleProperty()));

        StyleablePropertySheet.appendTo(op, n);
    }
}
