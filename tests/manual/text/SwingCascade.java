/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.BorderFactory;
import java.awt.Color;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import javax.swing.BoxLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Map;
import java.awt.Toolkit;

// Draws text cascades using black-on-white, white-on-black, and white-on-blue.
//
// In each block the left column is LCD text, the right is grayscale.
//
// The first parameter (optional) is the name of the font to use. Defaults to System.
// The second parameter (optional) is a description added at the bottom. This is
// useful to ensure descriptive text is included in screenshots.
public class SwingCascade {
    private static JComponent createLabel(String text, boolean lcd) {
        var label = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D custom = (Graphics2D) g.create();
                if (lcd) {
                    custom.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                } else {
                    custom.setRenderingHint(
                        RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON
                    );
                }
                custom.setRenderingHint(
                    RenderingHints.KEY_FRACTIONALMETRICS,
                    RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                super.paintComponent(custom);
                custom.dispose();
            }
        };
        return label;
    }

    private static JComponent createCascade(String fontFamily, Color textColor, Color backgroundColor, boolean lcd) {
        var sampleText = "Documents   \u59cb\u3081\u308b \u26AB";

        JPanel column = new JPanel();
        column.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setBackground(backgroundColor);

        for (int size = 6; size <= 14; size += 1) {
            var font = new Font(fontFamily, Font.PLAIN, size);
            var label = createLabel(sampleText, lcd);
            label.setForeground(textColor);
            label.setFont(font);
            column.add(label);
        }

        return column;
    }

    private static JComponent createSampleBlock(String fontFamily, Color textColor, Color backgroundColor) {
        var block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.X_AXIS));
        block.add(createCascade(fontFamily, textColor, backgroundColor, true));
        block.add(createCascade(fontFamily, textColor, backgroundColor, false));
        return block;
    }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "off");

        JFrame frame = new JFrame("Cascade");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);

        var fontFamily = Font.SANS_SERIF;
        String extraDesc = null;
        if (args.length > 0) {
            fontFamily = args[0];
        }
        if (args.length > 1) {
            extraDesc = args[1];
        }

        var row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.add(createSampleBlock(fontFamily, Color.BLACK, Color.WHITE));
        row.add(createSampleBlock(fontFamily, Color.WHITE, Color.BLACK));
        var blue = new Color(0.0f, 0.59f, 0.79f);
        row.add(createSampleBlock(fontFamily, Color.WHITE, blue));

        frame.add(row, BorderLayout.CENTER);

        var actualFont = new Font(fontFamily, Font.PLAIN, 10);
        var actualFontName = actualFont.getFamily();
        String caption = actualFontName + " 6pt-14pt " + System.getProperty("os.name");
        if (extraDesc != null) {
            caption = caption + " " + extraDesc;
        }
        var label = createLabel(caption, true);
        label.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        var labelFont = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        label.setFont(labelFont);
        frame.add(label, BorderLayout.SOUTH);

        frame.pack();
        frame.setVisible(true);
    }
}
