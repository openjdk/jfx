/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.fx.monkey.tools;

import java.awt.EventQueue;
import javafx.embed.swing.SwingNode;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;

/**
 * JTextArea tool for comparison with FX.
 */
public class EmbeddedJTextAreaWindow extends BorderPane {
    private final SwingNode swingNode;
    private JTextPanel panel;

    public EmbeddedJTextAreaWindow() {
        swingNode = new SwingNode();

        CheckBox rtl = new CheckBox("right-to-left (FX Scene.NodeOrientation)");
        rtl.selectedProperty().addListener((s, p, c) -> {
            // why does it mirror images the text area, including text??
            // https://bugs.openjdk.org/browse/JDK-8317835
            NodeOrientation v = (c) ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT;
            getScene().setNodeOrientation(v);
        });

        ToolBar tb = new ToolBar(rtl);

        setTop(tb);
        setCenter(swingNode);

        EventQueue.invokeLater(() -> {
            panel = new JTextPanel();
            swingNode.setContent(panel);
        });
    }
}
