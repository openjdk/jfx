/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jfx.incubator.scene.control.richtext;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import com.sun.jfx.incubator.scene.control.richtext.util.RichUtils;

/**
 * This component gets added to TextFlow to provide various types of highlight:
 * <ol>
 * <li>text highlight</li>
 * <li>wavy underline</li>
 * </ol>
 */
public class HighlightShape extends Path {
    public enum Type {
        HIGHLIGHT,
        SQUIGGLY,
    }

    private final Type type;
    private final int start;
    private final int end;

    public HighlightShape(Type t, int start, int end) {
        this.type = t;
        this.start = start;
        this.end = end;
    }

    private PathElement[] createPath(TextFlow f) {
        switch (type) {
        case HIGHLIGHT:
            return f.rangeShape(start, end);
        case SQUIGGLY:
            PathElement[] pe = f.underlineShape(start, end);
            return generateSquiggly(pe);
        default:
            // never happens
            return new PathElement[0];
        }
    }

    // underlineShape returns a series of rectangular shapes (MLLLL,MLLLL,...)
    // first we convert each rectangle to a line at its vertical midpoint,
    // then generate squiggly line (saw tooth, actually, for now)
    private PathElement[] generateSquiggly(PathElement[] in) {
        ArrayList<PathElement> list = new ArrayList<>(in.length * 8);
        double x0 = Integer.MAX_VALUE;
        double x1 = Integer.MIN_VALUE;
        double y0 = Integer.MAX_VALUE;
        double y1 = Integer.MIN_VALUE;
        int sz = in.length + 1;

        for (int i = 0; i < sz; i++) {
            PathElement p = i < in.length ? in[i] : null;

            if ((p == null) || (p instanceof MoveTo)) {
                if (x0 < x1) {
                    generateSquiggly(list, x0, x1, (y0 + y1) / 2.0, 1.0);
                }

                if (p == null) {
                    break;
                }

                x0 = Integer.MAX_VALUE;
                x1 = Integer.MIN_VALUE;
                y0 = Integer.MAX_VALUE;
                y1 = Integer.MIN_VALUE;
            }

            if (p instanceof MoveTo mt) {
                double x = mt.getX();
                if (x < x0) {
                    x0 = x;
                }
                if (x > x1) {
                    x1 = x;
                }
                double y = mt.getY();
                if (y < y0) {
                    y0 = y;
                }
                if (y > y1) {
                    y1 = y;
                }
            } else if (p instanceof LineTo lt) {
                double x = lt.getX();
                if (x < x0) {
                    x0 = x;
                }
                if (x > x1) {
                    x1 = x;
                }
                double y = lt.getY();
                if (y < y0) {
                    y0 = y;
                }
                if (y > y1) {
                    y1 = y;
                }
            }
        }

        return list.toArray(new PathElement[list.size()]);
    }

    private void generateSquiggly(List<PathElement> list, double xmin, double xmax, double ycenter, double sz) {
        double x = xmin;
        double y = ycenter;
        double y0 = ycenter - sz;
        double y1 = ycenter + sz;
        boolean up = true;
        boolean run = true;

        list.add(new MoveTo(x, y));

        while (run) {
            double delta = up ? (y - y0) : (y1 - y);
            if (x + delta > xmax) {
                delta = xmax - x;
                run = false;
            }

            x += delta;
            if (up) {
                y -= delta;
            } else {
                y += delta;
            }
            up = !up;

            list.add(new LineTo(x, y));
        }
    }

    private void updatePath(TextFlow f) {
        PathElement[] pe = createPath(f);
        getElements().setAll(pe);
    }

    public static void addTo(Region r, Type t, int start, int end, Color c) {
        if (r instanceof TextFlow f) {
            String style = createStyle(t, c);
            addHighlight(f, t, start, end, style, null);
        }
    }

    public static void addTo(Region r, Type t, int start, int end, String... styles) {
        if (r instanceof TextFlow f) {
            addHighlight(f, t, start, end, null, styles);
        }
    }

    private static String createStyle(Type t, Color c) {
        switch (t) {
        case HIGHLIGHT:
            // filled shape
            return "-fx-fill: " + RichUtils.toCssColor(c) + "; -fx-stroke-width:0;";
        default:
            // stroke
            return "-fx-stroke: " + RichUtils.toCssColor(c) + "; -fx-stroke-width:1;";
        }
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

        // highlights must be added before any Text nodes
        List<Node> children = f.getChildren();
        int sz = children.size();
        int ix = -1;
        for (int i = 0; i < sz; i++) {
            if (children.get(i) instanceof Text) {
                ix = i;
                break;
            }
        }
        if (ix < 0) {
            children.add(p);
        } else {
            children.add(ix, p);
        }
    }
}
