/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Robot;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import test.util.Util;

@TestMethodOrder(OrderAnnotation.class)
public class LinuxScreencastHangCrashTest {

    private static Robot robot;
    private static javafx.scene.robot.Robot jfxRobot;

    private static final int DELAY_BEFORE_SESSION_CLOSE = 2000;
    private static final int DELAY_WAIT_FOR_SESSION_TO_CLOSE = DELAY_BEFORE_SESSION_CLOSE + 250;
    private static final int DELAY_KEEP_SESSION = DELAY_BEFORE_SESSION_CLOSE - 1000;

    private static volatile boolean isFxStarted = false;

    @BeforeAll
    public static void init() throws Exception {
        Assumptions.assumeTrue(!Util.isOnWayland()); // JDK-8335470
        Assumptions.assumeTrue(Util.isOnWayland());
        robot = new Robot();
    }


    static void awtPixel() {
        System.out.println("awtPixel on " + Thread.currentThread().getName());
        java.awt.Color pixelColor = robot.getPixelColor(100, 100);
        System.out.println("\tAWT pixelColor: " + pixelColor);
    }

    private static void awtPixelOnFxThread() throws InterruptedException {
        System.out.println("awtPixelOnFxThread");
        initFX();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            awtPixel();
            latch.countDown();
        });
        if (!latch.await(5, TimeUnit.SECONDS)) {
            throw new RuntimeException("Timed out waiting for awt pixel on FX thread");
        }
    }

    private static void fxPixel() throws InterruptedException {
        System.out.println("fxPixel");
        initFX();

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            Color pixelColor = jfxRobot.getPixelColor(100, 100);
            System.out.println("\tFX pixelColor: " + pixelColor);
            latch.countDown();
        });
        if (!latch.await(5, TimeUnit.SECONDS)) {
            throw new RuntimeException("Timed out waiting for FX pixelColor");
        }
    }

    private static void initFX() {
        if (!isFxStarted) {
            System.out.println("Platform.startup");
            Platform.startup(() -> jfxRobot = new javafx.scene.robot.Robot());
            isFxStarted = true;
        }
    }

    @Test
    @Order(1)
    @Timeout(value=30)
    public void testHang() throws Exception {
        awtPixel();
        robot.delay(DELAY_WAIT_FOR_SESSION_TO_CLOSE);

        initFX();
        robot.delay(500);
        awtPixel();
        robot.delay(DELAY_WAIT_FOR_SESSION_TO_CLOSE);

        awtPixelOnFxThread();
        robot.delay(DELAY_WAIT_FOR_SESSION_TO_CLOSE);

        fxPixel();
        robot.delay(DELAY_WAIT_FOR_SESSION_TO_CLOSE);

        awtPixelOnFxThread();
        robot.delay(DELAY_WAIT_FOR_SESSION_TO_CLOSE);

        awtPixel();
    }

    @ParameterizedTest
    @Order(2)
    @Timeout(value=60)
    @ValueSource(ints = {
        DELAY_KEEP_SESSION,
        DELAY_BEFORE_SESSION_CLOSE, // 3 following are just in case
        DELAY_BEFORE_SESSION_CLOSE - 25,
        DELAY_BEFORE_SESSION_CLOSE + 25
    })
    public void testCrash(int delay) throws Exception {
        System.out.println("Testing with delay: " + delay);

        robot.delay(DELAY_WAIT_FOR_SESSION_TO_CLOSE);
        awtPixel();
        robot.delay(delay);
        fxPixel();

        robot.delay(DELAY_WAIT_FOR_SESSION_TO_CLOSE);
        fxPixel();
        robot.delay(delay);
        awtPixelOnFxThread();
    }
}
