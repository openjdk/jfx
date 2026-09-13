/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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

package helloworld;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * @author kcr
 */
public class HelloJFXPanel2 {

    public HelloJFXPanel2() {
        final JFrame frame = new JFrame("Hello JFXPanel");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JPanel swingPanel = new JPanel();
        swingPanel.setLayout(new FlowLayout());
        frame.getContentPane().add(swingPanel, BorderLayout.NORTH);

        JButton swingButton = new JButton("A Swing Button (press to close the window)");
        swingButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                frame.dispose();
            }
        });

        swingPanel.add(swingButton);

        // Create javafx panel
        final JFXPanel fxPanel = new JFXPanel();
        fxPanel.setPreferredSize(new Dimension(550,400));
        frame.getContentPane().add(fxPanel, BorderLayout.CENTER);

        // create JavaFX scene
        createScene(fxPanel);

        // show frame
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void createScene(final JFXPanel fxPanel) {
        System.err.println("createScene: calling Platform.runLater");
        Platform.runLater(() -> {
            System.err.println("Platform.runLater :: run");
            VBox root = new VBox(10);
            final Scene scene = new Scene(root);

            Button button = new Button("Click me");
            button.setTooltip(new Tooltip("button tooltip"));

            ComboBox<String> comboBox = new ComboBox<String>();
            comboBox.getItems().add("Item 1");
            comboBox.getItems().add("Item 2");
            comboBox.getItems().add("Item 3");
            comboBox.setPromptText("Choose One");
            comboBox.setTooltip(new Tooltip("combobox tooltip"));

            root.getChildren().addAll(button, comboBox);

            // add scene to panel
            fxPanel.setScene(scene);
        });
        System.err.println("PlatformImpl.runLater returns");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new HelloJFXPanel2();
        });
    }
}
