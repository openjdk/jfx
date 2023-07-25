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

package com.sun.javafx.scene.control.rich;

import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.TextFlow;

/**
 * This component gets added to TextFlow to provide various types of highlight:
 * <ol>
 * <li>text highlight</li>
 * <li>squiggly line</li>
 * <li>underline</li>
 * </ol>
 */
public class HighlightShape extends Path {
    public enum Type {
        HIGHLIGHT,
        SQUIGGLY,
        UNDERLINE
    }
    
    private final Type type;
    private final int start;
    private final int end;
    
    public HighlightShape(Type t, int start, int end) {
        this.type = t;
        this.start = start;
        this.end = end;
    }

    // TODO underlineShape returns rectangulars shapes!
    // we need to convert to single lines + squiggly lines
    private PathElement[] createPath(TextFlow f) {
        switch(type) {
        case HIGHLIGHT:
            return f.rangeShape(start, end);
        case SQUIGGLY:
            // FIX
            return f.underlineShape(start, end);
        case UNDERLINE:
        default:
            return f.underlineShape(start, end);
        }
    }

    protected void updatePath(TextFlow f) {
        PathElement[] pe = createPath(f);
        getElements().setAll(pe);
    }
    
    public static void addTo(Region r, Type t, int start, int end, Color c) {
        if (r instanceof TextFlow f) {
            String style = createStyle(t, c);
            //
            addHighlight(f, t, start, end, style, null);
        }
    }

    public static void addTo(Region r, Type t, int start, int end, String ... styles) {
        if (r instanceof TextFlow f) {
            addHighlight(f, t, start, end, null, styles);
        }
    }

    private static String createStyle(Type t, Color c) {
//        switch(t) {
//        case HIGHLIGHT:
            // filled shape
            return "-fx-fill: " + RichUtils.toCssColor(c) + "; -fx-stroke-width:0;";
//        default:
//            // stroke
//            return "-fx-stroke: " + RichUtils.toCssColor(c) + "; -fx-stroke-width:1;";
//        }
    }

    private static void addHighlight(TextFlow f, Type t, int start, int end, String directStyle, String[] styles) {
        HighlightShape p = new HighlightShape(t, start, end);
        p.setStyle(directStyle);
        if (styles != null) {
            p.getStyleClass().addAll(styles);
        }

        f.widthProperty().addListener((x) -> p.updatePath(f));
        p.updatePath(f);
        p.setManaged(false);
        // FIX must add before the first TextNode!
        f.getChildren().add(p);
    }
}
