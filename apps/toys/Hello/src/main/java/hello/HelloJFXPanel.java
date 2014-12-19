/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

package hello;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class HelloJFXPanel extends JFrame {

    public HelloJFXPanel() {
        this.setTitle("Simple JFXPanel Test");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLayout(new BorderLayout());

        JPanel swingPanel = new JPanel();
        swingPanel.setLayout(new FlowLayout());
        this.add(swingPanel, BorderLayout.NORTH);

        JButton swingButton = new JButton("Swing Button: press to close window");
        swingButton.addActionListener(e -> this.dispose());

        swingPanel.add(swingButton);

        // Create JavaFX panel
        final JFXPanel fxPanel = new JFXPanel();
        fxPanel.setPreferredSize(new Dimension(550, 400));
        this.add(fxPanel, BorderLayout.CENTER);

        // create JavaFX scene
        createScene(fxPanel);

        this.pack();
        this.setLocationRelativeTo(null);
    }

    private void createScene(final JFXPanel fxPanel) {
        System.err.println("createScene: calling Platform.runLater");
        Platform.runLater(() -> {
            System.err.println("Platform.runLater :: run");
            Group root = new Group();
            final Scene scene = new Scene(root);
            scene.setFill(Color.LIGHTGREEN);

            Rectangle rect = new Rectangle();
            rect.setX(25);
            rect.setY(40);
            rect.setWidth(100);
            rect.setHeight(50);
            rect.setFill(Color.RED);

            root.getChildren().add(rect);

            final Timeline timeline = new Timeline();
            timeline.setCycleCount(Timeline.INDEFINITE);
            timeline.setAutoReverse(true);
            final KeyValue kv = new KeyValue(rect.xProperty(), 200);
            final KeyFrame kf = new KeyFrame(Duration.millis(500), kv);
            timeline.getKeyFrames().add(kf);
            timeline.play();

            // add scene to panel
            fxPanel.setScene(scene);
        });
        System.err.println("PlatformImpl.runLater returns");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new HelloJFXPanel().setVisible(true);
        });
    }
}
