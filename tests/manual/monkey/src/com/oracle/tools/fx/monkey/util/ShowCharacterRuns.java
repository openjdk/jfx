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
package com.oracle.tools.fx.monkey.util;

import java.util.List;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.HitInfo;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * Shows character boundaries using navigation code similar to TextArea.nextCharacterVisually()
 */
public class ShowCharacterRuns extends Group {
    public ShowCharacterRuns() {
        setManaged(false);
    }

    /**
     * Creates ShowCharacterRuns Node for the given Text node.
     * The Text node must be a child of a Group.
     * @param owner the Text node to show character runs for
     */
    public static void createFor(Text owner) {
        Platform.runLater(() -> {
            List<Node> cs = getChildren(owner);
            ShowCharacterRuns r = new ShowCharacterRuns();
            int len = owner.getText().length();
            for (int i = 0; i < len; i++) {
                PathElement[] caret = owner.caretShape(i, true);
                if (caret.length == 4) {
                    caret = new PathElement[] {
                        caret[0],
                        caret[1]
                    };
                }
    
                Bounds caretBounds = new Path(caret).getLayoutBounds();
                double x = caretBounds.getMaxX();
                double y = (caretBounds.getMinY() + caretBounds.getMaxY()) / 2;
                HitInfo hit = owner.hitTest(new Point2D(x, y));
                Path p = new Path(owner.rangeShape(hit.getCharIndex(), hit.getCharIndex() + 1));
                //System.err.println(i + " " + cs); // FIX
                Color c = color(i);
                p.setFill(c);
                p.setStroke(c);
                r.getChildren().add(p);
            }
            cs.add(r);
        });
    }

    /**
     * Creates ShowCharacterRuns Node for the given TextFlow node.
     * The Text node must be a child of a Group.
     * @param owner the Text node to show character runs for
     */
    public static void createFor(TextFlow owner) {
        Platform.runLater(() -> {
            ShowCharacterRuns r = new ShowCharacterRuns();
            int len = FX.getTextLength(owner);
            for (int i = 0; i < len; i++) {
                PathElement[] caret = owner.caretShape(i, true);
                if (caret.length == 4) {
                    caret = new PathElement[] {
                        caret[0],
                        caret[1]
                    };
                }
    
                Bounds caretBounds = new Path(caret).getLayoutBounds();
                double x = caretBounds.getMaxX();
                double y = (caretBounds.getMinY() + caretBounds.getMaxY()) / 2;
                HitInfo hit = owner.hitTest(new Point2D(x, y));
                Path cs = new Path(owner.rangeShape(hit.getCharIndex(), hit.getCharIndex() + 1));
                //System.err.println(i + " " + cs); // FIX
                Color c = color(i);
                cs.setFill(c);
                cs.setStroke(c);
                r.getChildren().add(cs);
            }
            owner.getChildren().add(r);
        });
    }

    public static void remove(Node owner) {
        List<Node> cs = getChildren(owner);
        for (Node ch : cs) {
            if (ch instanceof ShowCharacterRuns r) {
                cs.remove(r);
                return;
            }
        }
    }

    private static List<Node> getChildren(Node n) {
        if (n instanceof TextFlow f) {
            return f.getChildren();
        }
        Parent p = n.getParent();
        if (p instanceof Group g) {
            return g.getChildren();
        }
        return null;
    }

    private static Color color(int i) {
        switch (i % 3) {
        case 0:
            return Color.rgb(255, 0, 0, 0.5);
        case 1:
            return Color.rgb(0, 255, 0, 0.5);
        default:
            return Color.rgb(0, 0, 255, 0.5);
        }
    }
}
