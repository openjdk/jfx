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

package test.robot.javafx.scene;

import java.util.concurrent.CountDownLatch;
import javafx.application.Application;
import javafx.application.ColorScheme;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import test.util.Util;

import static org.junit.jupiter.api.Assertions.*;

public class SceneFillTest {

    private static CountDownLatch startupLatch = new CountDownLatch(1);
    private static Stage stage;
    private static Scene scene;
    private static Robot robot;

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) throws Exception {
            stage = primaryStage;
            robot = new Robot();
            scene = new Scene(new VBox(), 300, 300);
            stage.setScene(scene);
            stage.setAlwaysOnTop(true);
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, _ -> Platform.runLater(startupLatch::countDown));
            stage.initStyle(StageStyle.DECORATED);
            stage.show();
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

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
        null | WHITE | BLACK
        TRANSPARENT | WHITE | BLACK
        rgba(0, 0, 0, 0.75) | rgb(63, 63, 63) | BLACK
        rgba(127, 127, 127, 0.5) | rgb(191, 191, 191) | rgb(63, 63, 63)
        rgba(255, 0, 0, 0.2) | rgb(255, 204, 204) | rgb(51, 0, 0)
        rgba(0, 100, 0, 0.8) | rgb(50, 131, 50) | rgb(0, 80, 0)
    """)
    public void testSceneFill(String fill, String expectLight, String expectDark) {
        Util.runAndWait(() -> {
            scene.getPreferences().setColorScheme(ColorScheme.LIGHT);
            scene.setFill("null".equals(fill) ? null : Color.web(fill));
        });

        Util.sleep(200);

        Util.runAndWait(() -> {
            assertClose(
                Color.web(expectLight),
                robot.getPixelColor(stage.getX() + 100, stage.getY() + 100));

            scene.getPreferences().setColorScheme(ColorScheme.DARK);
        });

        Util.sleep(200);

        Util.runAndWait(() -> {
            assertClose(
                Color.web(expectDark),
                robot.getPixelColor(stage.getX() + 100, stage.getY() + 100));
        });
    }

    private static void assertClose(Color expected, Color actual) {
        assertEquals(expected.getRed(), actual.getRed(), 0.07, "red");
        assertEquals(expected.getGreen(), actual.getGreen(), 0.07, "green");
        assertEquals(expected.getBlue(), actual.getBlue(), 0.07, "blue");
        assertEquals(expected.getOpacity(), actual.getOpacity(), 0.07, "opacity");
    }
}
