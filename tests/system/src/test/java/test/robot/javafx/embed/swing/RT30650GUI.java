/*
 * Copyright (c) 2013, 2022, Oracle and/or its affiliates. All rights reserved.
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

package test.robot.javafx.embed.swing;

import static com.sun.javafx.application.PlatformImpl.runAndWait;
import com.sun.javafx.tk.TKPulseListener;

import test.util.Util;

import java.awt.Color;
import javafx.animation.AnimationTimer;
import javafx.scene.Scene;

import javax.swing.SwingUtilities;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import javax.swing.JPanel;

/**
 * RT-30650: SwingNode is not Resizable
 *
 * The scenario: SwingNode contains JPanel. It should be initialized with the same layout bounds.
 * SwingNode is added to StackPane. The JPanel's max size is unbounded, so SwingNode is expected
 * to fill the whole space.
 */
public class RT30650GUI extends Application {
    SwingNode swingNode;

    private final static int SIZE = 400;

    // 100 pulses is not enough for the interop case, so we wait for 400 instead
    private static int pulseCount = 400;
    private static volatile boolean passed;

    public static boolean test() {
        launch(new String[]{});
        return passed;
    }

    private AnimationTimer animationTimer;
    private TKPulseListener pulseListener;
    private Robot robot;

    @Override
    public void start(final Stage stage) {

        // start constant pulse activity
        animationTimer = new AnimationTimer() {
            @Override public void handle(long l) {}
        };
        animationTimer.start();

        swingNode = new SwingNode();

        Pane pane = new StackPane();
        pane.setBackground(new Background(new BackgroundFill(javafx.scene.paint.Color.GREEN, null, null)));
        pane.getChildren().add(swingNode);
        Scene scene = new Scene(pane, SIZE, SIZE);

        stage.setScene(scene);
        stage.setTitle("RT-30650");
        stage.show();

        robot = new Robot();
        Util.parkCursor(robot);

        SwingUtilities.invokeLater(() -> {
            JPanel panel = new JPanel();
            panel.setBackground(Color.RED);
            swingNode.setContent(panel);

            Platform.runLater(() -> {
                pulseListener = () -> {
                    if (--pulseCount == 0) {
                        SwingUtilities.invokeLater(() -> {
                            passed = testColor(stage);
                            Platform.runLater(stage::close);
                        });
                    }
                };
                com.sun.javafx.tk.Toolkit.getToolkit().addSceneTkPulseListener(pulseListener);
            });
        });
    }

    public boolean testColor(Stage stage) {
        int x = (int)stage.getX();
        int y = (int)stage.getY();

        final javafx.scene.paint.Color rgb[] = new javafx.scene.paint.Color[1];
        runAndWait(() -> {
            rgb[0] = robot.getPixelColor(x + SIZE/2, y + SIZE/2);
        });

        System.out.println("detected color: " + rgb[0].toString());

        // On MacOSX the robot returns the color affected by the color profile.
        // And so the resulting color may differ from the requested one.
        // Here we have to check that the color is closer to red rather than to green.
        return rgb[0].getRed() * 255d > 200 && rgb[0].getGreen() * 255d < 100;
    }
}
