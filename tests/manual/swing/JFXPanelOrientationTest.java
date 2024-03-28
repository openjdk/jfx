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

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.EventQueue;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.WindowConstants;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * https://bugs.openjdk.org/browse/JDK-8317836
 */
public class JFXPanelOrientationTest extends Application {
    private static JFXPanel jfxPanel;
    private static TextArea textArea;
    private static JFrame frame;

    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage stage) {
        Button passButton = new Button("Pass");
        Button failButton = new Button("Fail");
        passButton.setOnAction(e -> this.quit());
        failButton.setOnAction(e -> {
            this.quit();
            throw new AssertionError("Orientation change is not working");
        });

        BorderPane pane = new BorderPane();
        VBox rootNode = new VBox(6,
                new Label("1. This is a test for JFXPanel orientation."),
                new Label("2. A Left-To-Right text will be shown along with a checkbox"),
                new Label("3. If the text's orientation is changed on clicking on checkbox, click on Pass or else click on Fail"),
                new Label(""),
                new HBox(10, passButton, failButton), pane);

        stage.setScene(new Scene(rootNode, 500, 150));
        stage.show();
        EventQueue.invokeLater(JFXPanelOrientationTest::initSwing);
    }

    private void quit() {
        frame.dispose();
        Platform.exit();
    }

    private static void initSwing() {
        frame = new JFrame();

        jfxPanel = new JFXPanel();

        Platform.runLater(JFXPanelOrientationTest::initFX);

        JCheckBox rtl = new JCheckBox("RTL (JFrame.componentOrientation)");
        rtl.addActionListener((ev) -> {
            ComponentOrientation ori = rtl.isSelected() ? ComponentOrientation.RIGHT_TO_LEFT : ComponentOrientation.LEFT_TO_RIGHT;
            frame.applyComponentOrientation(ori);
            frame.validate();
            frame.repaint();
        });

        JToolBar tb = new JToolBar();
        tb.add(rtl);

        JPanel p = new JPanel(new BorderLayout());
        p.add(jfxPanel, BorderLayout.CENTER);
        p.add(tb, BorderLayout.NORTH);

        frame.setContentPane(p);
        frame.setSize(400, 200);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setTitle("FX TextArea embedded in JFXPanel");
        frame.setVisible(true);
    }

    private static void initFX() {
        textArea = new TextArea("Hebrew: עברית");
        jfxPanel.setScene(new Scene(textArea));
    }
}
