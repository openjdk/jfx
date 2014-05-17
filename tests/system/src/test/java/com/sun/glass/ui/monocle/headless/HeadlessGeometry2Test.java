/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.monocle.headless;

import com.sun.glass.ui.Screen;
import javafx.application.Application;
import javafx.stage.Stage;
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class HeadlessGeometry2Test {

    private static CountDownLatch startupLatch = new CountDownLatch(1);

    private static int width;
    private static int height;
    private static int depth;

    public static class TestApp extends Application {
        @Override
        public void start(Stage t) {
            width = Screen.getMainScreen().getWidth();
            height = Screen.getMainScreen().getHeight();
            depth = Screen.getMainScreen().getDepth();
            startupLatch.countDown();
        }
    }

    @BeforeClass
    public static void setup() throws Exception {
        System.setProperty("glass.platform", "Monocle");
        System.setProperty("monocle.platform", "Headless");
        System.setProperty("prism.order", "sw");
        System.setProperty("headless.geometry", "150x250-16");
        new Thread(() -> Application.launch(TestApp.class)).start();
        startupLatch.await(5, TimeUnit.SECONDS);
        Assert.assertEquals(0, startupLatch.getCount());
    }

    @Test
    public void setScreenBounds() throws Exception {
        Assert.assertEquals(150, width);
        Assert.assertEquals(250, height);
        Assert.assertEquals(16, depth);
    }

}
