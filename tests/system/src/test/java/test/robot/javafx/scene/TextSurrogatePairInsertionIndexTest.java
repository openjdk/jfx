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

package test.robot.javafx.scene;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.scene.text.HitInfo;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import test.util.Util;

/*
 * Verifies insertion index computation in Text in the presence of surrogate pairs.
 */
public class TextSurrogatePairInsertionIndexTest {
    private static final int FONT_SIZE = 36;
    private static final int WIDTH = 200;
    private static final int HEIGHT = FONT_SIZE * 7;

    private static CountDownLatch startupLatch = new CountDownLatch(1);
    private static Random random;
    private static Text control;
    private static Stage stage;
    private static Scene scene;
    private static BorderPane content;

    private void create(String text) {
        control = new Text(text);
        control.setFont(new Font(FONT_SIZE));
        content.setTop(control);
        control.wrappingWidthProperty().bind(content.widthProperty());
    }

    /**
     * Test tests HitInfo invariants randomly picking a point within the Text instance.
     */
    @Test
    public void testHitInfo() {
        Util.runAndWait(() -> {
            create("[This is text 😀😃😄\n\n😁😆 💙🦋🏁🔥]");
        });
        Util.waitForIdle(scene);

        Util.runAndWait(() -> {
            Bounds b = control.getLayoutBounds();
            Assert.assertTrue(b.getWidth() > 10);
            Assert.assertTrue(b.getHeight() > 10);

            int max = 1_000_000;
            for (int i = 0; i < max; i++) {
                double x = random.nextDouble() * b.getWidth();
                double y = random.nextDouble() * b.getHeight();
                HitInfo h = control.hitTest(new Point2D(x, y));

                Assert.assertTrue(h.getInsertionIndex() >= 0);
                String s = h.toString();
                Assert.assertTrue(s != null);
            }
        });
    }

    @BeforeClass
    public static void initFX() {
        long seed = new Random().nextLong();
        // if any test fails, we can use the seed found in the log to reproduce exact sequence of events
        System.out.println("seed=" + seed);
        random = new Random(seed);

        Util.launch(startupLatch, TestApp.class);
    }

    @AfterClass
    public static void exit() {
        Util.shutdown();
    }

    public static class TestApp extends Application {
        @Override
        public void start(Stage primaryStage) {
            stage = primaryStage;

            content = new BorderPane();
            scene = new Scene(content, WIDTH, HEIGHT);
            stage.setScene(scene);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setOnShown(event -> Platform.runLater(startupLatch::countDown));
            stage.setAlwaysOnTop(true);
            stage.show();
        }
    }
}
