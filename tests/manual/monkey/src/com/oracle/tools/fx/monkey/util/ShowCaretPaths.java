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
package com.oracle.tools.fx.monkey.util;

import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.TextFlow;

/**
 * Shows caret paths for each text position.
 */
public class ShowCaretPaths extends Path {
    public ShowCaretPaths() {
        setStrokeWidth(1);
        setStroke(Color.RED);
        setManaged(false);
    }

    /**
     * Creates ShowCaretPaths Node for the given TextFlow node.
     * The Text node must be a child of a Group.
     * @param owner the Text node to show character runs for
     */
    public static void createFor(TextFlow owner) {
        ShowCaretPaths p = new ShowCaretPaths();
        int len = FX.getTextLength(owner);
        for (int i = 0; i < len; i++) {
            PathElement[] es = owner.caretShape(i, true);
            p.getElements().addAll(es);
        }
        owner.getChildren().add(p);
    }

    public static void remove(Pane p) {
        for (Node ch : p.getChildren()) {
            if (ch instanceof ShowCaretPaths) {
                p.getChildren().remove(ch);
                return;
            }
        }
    }
}
