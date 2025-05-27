/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.robot.javafx.scene;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.robot.Robot;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import test.util.Util;

@Timeout(value=120000, unit=TimeUnit.MILLISECONDS)
public class MouseLocationOnScreenTest {
    static CountDownLatch startupLatch = new CountDownLatch(1);
    static Robot robot;
    private static int DELAY_TIME = 1;
    private static int VALIDATE_COUNT = 5;

    public static class TestApp extends Application {

        @Override
        public void start(Stage primaryStage) throws Exception {
            robot = new Robot();
            startupLatch.countDown();
        }
    }

    @BeforeAll
    public static void initFX() {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterAll
    public static void teardown() {
        Util.shutdown();
    }

    @Test
    public void testMouseLocation() throws Exception {

        Screen screen = Screen.getPrimary();
        // using visual bounds prevents hitting the camera notch area on newer Macs
        Rectangle2D bounds = screen.getVisualBounds();
        int x1 = (int) bounds.getMinX();
        int x2 = (int) (x1 + bounds.getWidth() - 1);
        int y1 = (int) bounds.getMinY();
        int y2 = (int) (y1 + bounds.getHeight() - 1);

        // Check all edge (two pixels in a width)
        Util.runAndWait(() -> {
            edge(robot, x1, y1, x2, y1);         // top
        });
        Util.runAndWait(() -> {
            edge(robot, x1, y1 + 1, x2, y1 + 1); // top
        });

        Util.runAndWait(() -> {
            edge(robot, x2, y1, x2, y2);         // right
        });
        Util.runAndWait(() -> {
            edge(robot, x2 - 1, y1, x2 - 1, y2); // right
        });

        Util.runAndWait(() -> {
            edge(robot, x1, y1, x1, y2);         // left
        });
        Util.runAndWait(() -> {
            edge(robot, x1 + 1, y1, x1 + 1, y2); // left
        });

        Util.runAndWait(() -> {
            edge(robot, x1, y2, x2, y2);         // bottom
        });
        Util.runAndWait(() -> {
            edge(robot, x1, y2 - 1, x2, y2 - 1); // bottom
        });

        // Check crossing of diagonals
        Util.runAndWait(() -> {
            cross(robot, x1, y1, x2, y2); // cross left-bottom
        });
        Util.runAndWait(() -> {
            cross(robot, x1, y2, x2, y1); // cross left-top
        });
    }

    /**
     * This method checks the coordinates which were passed to robot and
     * returned by robot are same
     */
    static void validate(Robot robot, int x, int y) {
        boolean equalValue = false;
        for (int i = 0; i < VALIDATE_COUNT; i++) {
            // Making delay and check at 1 ms and every 2 ms gap onwards
            Util.sleep(i == 0 ? DELAY_TIME : DELAY_TIME + 1);
            if (x == (int)robot.getMouseX() &&
                y == (int)robot.getMouseY()) {
                equalValue = true;
                break;
            }
        }

        if (!equalValue) {
            // Delay for 500ms more
            Util.sleep(500);
        }
        Assertions.assertEquals(x, (int) robot.getMouseX());
        Assertions.assertEquals(y, (int) robot.getMouseY());
    }

    private static void edge(Robot robot, int x1, int y1, int x2, int y2) {
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                robot.mouseMove(x, y);
                validate(robot, x, y);
            }
        }
    }

    private static void cross(Robot robot, int x0, int y0, int x1, int y1) {
        double dmax = (double) Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        double dx = (x1 - x0) / dmax;
        double dy = (y1 - y0) / dmax;

        robot.mouseMove(x0, y0);
        validate(robot, x0, y0);

        for (int i = 1; i <= dmax; i++) {
            int x = (int) (x0 + dx * i);
            int y = (int) (y0 + dy * i);
            robot.mouseMove(x, y);
            validate(robot, x, y);
        }
    }
}
