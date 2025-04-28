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

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;
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
    private JTextArea textArea;
    private JTextField textField;

    public EmbeddedJTextAreaWindow() {
        swingNode = new SwingNode();

        CheckBox rtl = new CheckBox("right-to-left (Swing ComponentOrientation)");
        rtl.selectedProperty().addListener((s, p, c) -> {
            EventQueue.invokeLater(() -> {
                ComponentOrientation ori = c ? ComponentOrientation.RIGHT_TO_LEFT : ComponentOrientation.LEFT_TO_RIGHT;
                textArea.setComponentOrientation(ori);
                textField.setComponentOrientation(ori);
            });
        });

        CheckBox rtl2 = new CheckBox("right-to-left (FX Scene.NodeOrientation)");
        rtl2.selectedProperty().addListener((s, p, c) -> {
            // ha ha mirror images the text area, including text!
            NodeOrientation v = (c) ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT;
            getScene().setNodeOrientation(v);
        });

        ToolBar tb = new ToolBar(rtl, rtl2);

        setTop(tb);
        setCenter(swingNode);

        EventQueue.invokeLater(() -> {
            int fontSize = 36;

            textArea = new JTextArea("Arabic: العربية\nHebrew: עברית");
            updateFont(textArea, fontSize);
            textField = new JTextField("Arabic: العربية Hebrew: עברית");
            updateFont(textField, fontSize);

            String[] names = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            JComboBox<String> fonts = new JComboBox(names);
            fonts.addActionListener((ev) -> {
                String name = (String)fonts.getSelectedItem();
                Font f = new Font(name, Font.PLAIN, fontSize);
                textArea.setFont(f);
                textField.setFont(f);
            });

            JPanel p = new JPanel(new BorderLayout());
            JScrollPane sp = new JScrollPane(textArea);
            p.add(fonts, BorderLayout.NORTH);
            p.add(sp, BorderLayout.CENTER);
            p.add(textField, BorderLayout.SOUTH);
            p.setBorder(new EmptyBorder(10, 10, 10, 10));
            swingNode.setContent(p);
        });
    }

    private static void updateFont(JTextComponent c, float size) {
        Font f = c.getFont().deriveFont(size);
        c.setFont(f);
    }
}
