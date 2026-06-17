/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;

/**
 * JTextField/JTextArea panel with the font selector.
 */
public class JTextPanel extends JPanel {
    private final JComboBox<String> fonts;
    private final JTextArea textArea;
    private final JTextField textField;
    private final JCheckBox rtl;

    public JTextPanel() {
        super(new BorderLayout());

        int fontSize = 36;

        textArea = new JTextArea("Arabic: العربية\nHebrew: עברית");
        updateFont(textArea, fontSize);
        textField = new JTextField("Arabic: العربية Hebrew: עברית");
        updateFont(textField, fontSize);

        String[] names = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();

        fonts = new JComboBox(names);
        fonts.addActionListener((ev) -> {
            String name = (String)fonts.getSelectedItem();
            Font f = new Font(name, Font.PLAIN, fontSize);
            textArea.setFont(f);
            textField.setFont(f);
        });

        rtl = new JCheckBox("right-to-left (Swing ComponentOrientation)");
        rtl.addActionListener((ev) -> {
            boolean isRtl = rtl.isSelected();
            ComponentOrientation ori = isRtl ? ComponentOrientation.RIGHT_TO_LEFT : ComponentOrientation.LEFT_TO_RIGHT;
            setOrientation(ori);
        });

        JPanel p = new JPanel(new BorderLayout());
        p.add(fonts, BorderLayout.NORTH);
        p.add(rtl, BorderLayout.SOUTH);

        JScrollPane sp = new JScrollPane(textArea);
        add(p, BorderLayout.NORTH);
        add(sp, BorderLayout.CENTER);
        add(textField, BorderLayout.SOUTH);
        setBorder(new EmptyBorder(10, 10, 10, 10));
    }

    public static void openSwing() {
        EventQueue.invokeLater(() -> {
            JFrame f = new JFrame();
            f.setTitle("JTextArea/JTextField in Swing JFrame");
            f.setSize(500, 400);
            f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            f.add(new JTextPanel());
            f.setVisible(true);
        });
    }

    private void setOrientation(ComponentOrientation ori) {
        fonts.setComponentOrientation(ori);
        rtl.setComponentOrientation(ori);
        textArea.setComponentOrientation(ori);
        textField.setComponentOrientation(ori);
    }

    private static void updateFont(JTextComponent c, float size) {
        Font f = c.getFont().deriveFont(size);
        c.setFont(f);
    }
}
