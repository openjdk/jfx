/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.glass.ui;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.util.Util;
import com.sun.javafx.PlatformUtil;
import javafx.application.Application;
import javafx.geometry.Dimension2D;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

public class InitialWindowSizeTest {

    private static final CountDownLatch startupLatch = new CountDownLatch(1);
    private static volatile Dimension2D showingSize, shownSize;

    public static class TestApp extends Application {
        @Override
        public void start(Stage stage) {
            stage.setMinWidth(300);
            stage.setMinHeight(200);
            stage.setScene(new Scene(new StackPane(new Rectangle(20, 20))));
            stage.setOnShowing(event -> showingSize = new Dimension2D(stage.getWidth(), stage.getHeight()));
            stage.setOnShown(event -> {
                shownSize = new Dimension2D(stage.getWidth(), stage.getHeight());
                startupLatch.countDown();
            });
            stage.show();
        }
    }

    @BeforeAll
    public static void setup() throws Exception {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterAll
    public static void shutdown() {
        Util.shutdown();
    }

    @Test
    public void testInitialWindowSize() {
        // JDK-8310845
        assumeFalse(PlatformUtil.isLinux());

        Util.waitForLatch(startupLatch, 10, "startupLatch");

        assertTrue(Double.isNaN(showingSize.getWidth()), "width = " + showingSize.getWidth() + ", expected = NaN");
        assertTrue(Double.isNaN(showingSize.getHeight()), "height = " + showingSize.getHeight() + ", expected = NaN");
        assertEquals(300.0, shownSize.getWidth(), 0.001);
        assertEquals(200.0, shownSize.getHeight(), 0.001);
    }
}
