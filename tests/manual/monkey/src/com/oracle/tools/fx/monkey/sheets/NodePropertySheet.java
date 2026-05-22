/*
 * Copyright (c) 2024, 2026, Oracle and/or its affiliates. All rights reserved.
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
import javafx.geometry.NodeOrientation;
import javafx.scene.AccessibleRole;
import javafx.scene.CacheHint;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.Bloom;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.ColorInput;
import javafx.scene.effect.DisplacementMap;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.effect.FloatMap;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.effect.Glow;
import javafx.scene.effect.ImageInput;
import javafx.scene.effect.InnerShadow;
import javafx.scene.effect.Light;
import javafx.scene.effect.Lighting;
import javafx.scene.effect.MotionBlur;
import javafx.scene.effect.PerspectiveTransform;
import javafx.scene.effect.Reflection;
import javafx.scene.effect.SepiaTone;
import javafx.scene.effect.Shadow;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import com.oracle.tools.fx.monkey.options.BooleanOption;
import com.oracle.tools.fx.monkey.options.DoubleOption;
import com.oracle.tools.fx.monkey.options.DoubleSpinner;
import com.oracle.tools.fx.monkey.options.EnumOption;
import com.oracle.tools.fx.monkey.options.ObjectOption;
import com.oracle.tools.fx.monkey.options.StyleClassOption;
import com.oracle.tools.fx.monkey.options.TextOption;
import com.oracle.tools.fx.monkey.util.ImageTools;
import com.oracle.tools.fx.monkey.util.OptionPane;

/**
 * Node Property Sheet.
 */
public class NodePropertySheet {
    public static void appendTo(OptionPane op, Node n) {
        op.section("Node");
        op.option("Acc. Help:", new TextOption("accessibleHelp", n.accessibleHelpProperty()));
        op.option("Acc. Role:", new EnumOption<>("accessibleRole", AccessibleRole.class, n.accessibleRoleProperty()));
        op.option("Acc. Role Descr.:", new TextOption("accessibleRoleDescription", n.accessibleRoleDescriptionProperty()));
        op.option("Acc. Text:", new TextOption("accessibleText", n.accessibleTextProperty()));
        op.option("Blend Mode:", new EnumOption<>("blendMode", BlendMode.class, n.blendModeProperty()));
        op.option(new BooleanOption("cache", "cache", n.cacheProperty()));
        op.option("Cache Hint:", new EnumOption<>("cacheHint", CacheHint.class, n.cacheHintProperty()));
        op.option("Clip:", Options.clip("clip", n, n.clipProperty()));
        op.option("Cursor:", cursorOptions("cursor", n, n.cursorProperty()));
        op.option("Depth Test:", new EnumOption<>("depthText", CacheHint.class, n.cacheHintProperty()));
        op.option(new BooleanOption("disable", "disable", n.disableProperty()));
        op.option("Effect:", effectOptions("effect", n, n.effectProperty()));
        op.option(new BooleanOption("focusTraversable", "focus traversable", n.focusTraversableProperty()));
        op.option("Id:", new TextOption("id", n.idProperty()));
        //op.option("Input Method Requests: TODO", null); // TODO
        op.option("Layout X:", new DoubleOption("layoutX", n.layoutXProperty()));
        op.option("Layout Y:", new DoubleOption("layoutY", n.layoutYProperty()));
        op.option(new BooleanOption("managed", "managed", n.managedProperty()));
        op.option(new BooleanOption("mouseTransparent", "mouse transparent", n.mouseTransparentProperty()));
        op.option("Node Orientation:", new EnumOption<>("nodeOrientation", NodeOrientation.class, n.nodeOrientationProperty()));
        op.option("Opacity:", new DoubleSpinner("opacity", -0.1, 1.1, 0.1, n.opacityProperty()));
        op.option(new BooleanOption("pickOnBounds", "pick on bounds", n.pickOnBoundsProperty()));
        op.option("Rotate:", new DoubleOption("rotate", n.rotateProperty()));
        //op.option("Rotation Axis: TODO", null); // TODO
        op.option("Scale X:", new DoubleOption("scaleX", n.scaleXProperty()));
        op.option("Scale Y:", new DoubleOption("scaleY", n.scaleYProperty()));
        op.option("Scale Z:", new DoubleOption("scaleZ", n.scaleZProperty()));
        op.option("Style:", new TextOption("style", n.styleProperty()));
        op.option("Style Class:", new StyleClassOption("styleClass", n.getStyleClass()));
        op.option("Translate X:", new DoubleOption("translateX", n.translateXProperty()));
        op.option("Translate Y:", new DoubleOption("translateY", n.translateYProperty()));
        op.option("Translate Z:", new DoubleOption("translateZ", n.translateZProperty()));
        //op.option("User Data: TODO", null); // TODO
        op.option("View Order:", new DoubleOption("viewOrder", n.viewOrderProperty()));
        op.option(new BooleanOption("visible", "visible", n.visibleProperty()));

        StyleablePropertySheet.appendTo(op, n);
    }

    private static Node cursorOptions(String name, Node n, ObjectProperty<Cursor> p) {
        ObjectOption<Cursor> op = new ObjectOption<>(name, p);
        op.addChoice("<null>", null);
        op.addChoice("CLOSED_HAND", Cursor.CLOSED_HAND);
        op.addChoice("CROSSHAIR", Cursor.CROSSHAIR);
        op.addChoice("DEFAULT", Cursor.DEFAULT);
        op.addChoice("DISAPPEAR", Cursor.DISAPPEAR);
        op.addChoice("E_RESIZE", Cursor.E_RESIZE);
        op.addChoice("H_RESIZE", Cursor.H_RESIZE);
        op.addChoice("HAND", Cursor.HAND);
        op.addChoice("MOVE", Cursor.MOVE);
        op.addChoice("N_RESIZE", Cursor.N_RESIZE);
        op.addChoice("NE_RESIZE", Cursor.NE_RESIZE);
        op.addChoice("NONE", Cursor.NONE);
        op.addChoice("NW_RESIZE", Cursor.NW_RESIZE);
        op.addChoice("OPEN_HAND", Cursor.OPEN_HAND);
        op.addChoice("S_RESIZE", Cursor.S_RESIZE);
        op.addChoice("SE_RESIZE", Cursor.SE_RESIZE);
        op.addChoice("SW_RESIZE", Cursor.SW_RESIZE);
        op.addChoice("TEXT", Cursor.TEXT);
        op.addChoice("V_RESIZE", Cursor.V_RESIZE);
        op.addChoice("W_RESIZE", Cursor.W_RESIZE);
        op.addChoice("W_RESIZE", Cursor.W_RESIZE);
        return op;
    }

    private static ObjectOption<Effect> effectOptions(String name, Node n, ObjectProperty<Effect> p) {
        ObjectOption<Effect> op = new ObjectOption<>(name, p);
        op.addChoice("<null>", null);
        op.addChoiceSupplier("Blend", () -> {
            ColorInput c = new ColorInput();
            c.setPaint(Color.STEELBLUE);
            c.setX(10);
            c.setY(10);
            c.setWidth(100);
            c.setHeight(180);
            Blend b = new Blend();
            b.setMode(BlendMode.COLOR_BURN);
            b.setTopInput(c);
            return b;
        });
        op.addChoiceSupplier("Bloom", () -> {
            return new Bloom(0.1);
        });
        op.addChoiceSupplier("BoxBlur", () -> {
            BoxBlur b = new BoxBlur();
            b.setWidth(10);
            b.setHeight(3);
            b.setIterations(3);
            return b;
        });
        op.addChoiceSupplier("ColorAdjust", () -> {
            ColorAdjust a = new ColorAdjust();
            a.setContrast(0.5);
            a.setHue(0.5);
            a.setBrightness(0.5);
            a.setSaturation(0.2);
            return a;
        });
        op.addChoiceSupplier("ColorInput", () -> {
            return new ColorInput(0, 0, 100, 10, Color.RED);
        });
        op.addChoiceSupplier("DisplacementMap", () -> {
            int w = 220;
            int h = 100;
            FloatMap f = new FloatMap();
            f.setWidth(w);
            f.setHeight(h);
            for (int i = 0; i < w; i++) {
                double v = (Math.sin(i / 20.0 * Math.PI) - 0.5) / 40.0;
                for (int j = 0; j < h; j++) {
                    f.setSamples(i, j, 0.0f, (float) v);
                }
            }
            return new DisplacementMap(f);
        });
        op.addChoiceSupplier("Drop Shadow", () -> {
            DropShadow d = new DropShadow();
            d.setRadius(5.0);
            d.setOffsetX(3.0);
            d.setOffsetY(3.0);
            d.setColor(Color.color(0.4, 0.5, 0.5));
            return d;
        });
        op.addChoiceSupplier("GaussianBlur", () -> {
            return new GaussianBlur(5.0);
        });
        op.addChoiceSupplier("Glow", () -> {
            return new Glow(2.0);
        });
        op.addChoiceSupplier("ImageInput", () -> {
            Image im = ImageTools.createImage(50, 50);
            return new ImageInput(im);
        });
        op.addChoiceSupplier("InnerShadow", () -> {
            InnerShadow s = new InnerShadow();
            s.setOffsetX(4);
            s.setOffsetY(4);
            s.setColor(Color.web("0x3b596d"));
            return s;
        });
        op.addChoiceSupplier("Lighting", () -> {
            Light.Distant li = new Light.Distant();
            li.setAzimuth(-135.0);
            Lighting t = new Lighting();
            t.setLight(li);
            t.setSurfaceScale(5.0);
            return t;
        });
        op.addChoiceSupplier("MotionBlur", () -> {
            MotionBlur t = new MotionBlur();
            t.setRadius(30);
            t.setAngle(-15.0);
            return t;
        });
        op.addChoiceSupplier("PerspectiveTransform", () -> {
            PerspectiveTransform t = new PerspectiveTransform();
            t.setUlx(10.0);
            t.setUly(10.0);
            t.setUrx(310.0);
            t.setUry(40.0);
            t.setLrx(310.0);
            t.setLry(60.0);
            t.setLlx(10.0);
            t.setLly(90.0);
            return t;
        });
        op.addChoiceSupplier("Reflection", () -> {
            Reflection r = new Reflection();
            r.setFraction(0.7);
            return r;
        });
        op.addChoiceSupplier("SepiaTone", () -> {
            SepiaTone s = new SepiaTone();
            s.setLevel(0.7);
            return s;
        });
        op.addChoiceSupplier("Shadow", () -> {
            return new Shadow();
        });
        return op;
    }
}
