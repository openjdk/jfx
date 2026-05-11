/*
 * Copyright (c) 2025, 2026, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.fx.monkey.pages;

import javafx.beans.Observable;
import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import com.oracle.tools.fx.monkey.util.Formats;
import com.oracle.tools.fx.monkey.util.TestPaneBase;
import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.RichTextModel;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import jfx.incubator.scene.control.richtext.model.StyledInput;

/**
 * Screen Page.
 */
public class ScreenPage extends TestPaneBase {

    private static final StyleAttributeMap HEAD = mkTitle();
    private static final StyleAttributeMap LABL = mkLabel();
    private static final StyleAttributeMap TEXT = mkText();
    private final RichTextArea rta;

    public ScreenPage() {
        super("ScreenPage");

        ScreenModel m = new ScreenModel();
        rta = new RichTextArea(m);
        rta.setEditable(false);
        setContent(rta);
    }

    private static StyleAttributeMap mkTitle() {
        return StyleAttributeMap.builder().
            setFontSize(18).
            setUnderline(true).
            build();
    }

    private static StyleAttributeMap mkLabel() {
        return StyleAttributeMap.builder().
            setTextColor(Color.DARKGRAY).
            setFontFamily("Monospace").
            build();
    }

    private static StyleAttributeMap mkText() {
        return StyleAttributeMap.builder().
            setFontFamily("Monospace").
            build();
    }

    private static class ScreenModel extends RichTextModel {
        public ScreenModel() {
            Screen.getScreens().addListener((Observable _) -> {
                update();
            });
            update();
        }

        private void update() {
            replace(null, TextPos.ZERO, getDocumentEnd(), "");
            int ix = 0;
            for(Screen s: Screen.getScreens()) {
                boolean primary = Screen.getPrimary().equals(s);
                Rectangle2D r = s.getBounds();
                Rectangle2D v = s.getVisualBounds();
                String bounds =
                    Formats.formatDouble(v.getWidth()) + " x " + Formats.formatDouble(v.getHeight()) + " at " +
                    Formats.formatDouble(v.getMinX()) + ", " + Formats.formatDouble(v.getMinY());

                append(HEAD, "Screen " + ix + (primary ? " - Primary" : ""));
                nl();
                a("          Size: ", Formats.formatDouble(r.getWidth()) + " x " + Formats.formatDouble(r.getHeight()));
                a("      Position: ", Formats.formatDouble(r.getMinX()) + ", " + Formats.formatDouble(r.getMinY()));
                a("           DPI: ", Formats.formatDouble(s.getDpi()));
                a("Output Scale X: ", Formats.formatDouble(s.getOutputScaleX()));
                a("Output Scale Y: ", Formats.formatDouble(s.getOutputScaleY()));
                a(" Visual Bounds: ", bounds);
                nl();
                ix++;
            }
        }

        private void a(String label, String text) {
            append(LABL, label);
            append(TEXT, text);
            nl();
        }

        // TODO maybe create a method
        private void append(StyleAttributeMap a, String text) {
            TextPos end = getDocumentEnd();
            replace(null, end, end, StyledInput.of(text, a));
        }

        private void nl() {
            append(TEXT, "\n");
        }
    }
}
