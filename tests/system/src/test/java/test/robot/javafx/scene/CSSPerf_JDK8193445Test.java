/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import org.junit.Test;
import test.robot.testharness.VisualTestBase;
import static org.junit.Assert.assertTrue;

/**
 * This test is based on the test case reported in JDK-8209830
 *
 * Redundant CSS Re-application was avoided in JDK-8193445.
 * It results in faster application of CSS on Controls (Nodes). In turn,
 * resulting in improved Node creation/addition time to a Scene.
 *
 * The goal of this test is *NOT* to measure absolute performance, but to show
 * creating and adding 300 Nodes to a scene does not take more than a
 * particular threshold of time.
 *
 * The selected thresold is larger than actual observed time.
 * It is not a benchmark value. It is good enough to catch the regression
 * in performance, if any.
 */

public class CSSPerf_JDK8193445Test extends VisualTestBase {

    private Stage testStage;
    private Scene testScene;
    private BorderPane pane = new BorderPane();
    private long mSec = 0;

    @Test(timeout = 15000)
    public void testTimeForAdding300NodesToScene() {
        runAndWait(() -> {
            testStage = getStage();
            testScene = new Scene(pane);
            testStage.setScene(testScene);
            testStage.show();
        });

        waitFirstFrame();

        // Measure time to create and add 300 Nodes to Scene
        runAndWait(() -> {
            long startTime = System.currentTimeMillis();

            HBox hbox = new HBox();
            for (int i = 0; i < 300; i++) {
                hbox = new HBox(new Text("y"), hbox);
                final HBox h = hbox;
                h.setPadding(new Insets(1));
            }
            pane.setCenter(hbox);

            long endTime = System.currentTimeMillis();

            mSec = endTime - startTime;
        });

        System.out.println("Time to create and add 300 nodes to a Scene = " + mSec + " mSec");

        // NOTE : 400 mSec is not a benchmark value
        // It is good enough to catch the regression in performance, if any
        assertTrue("Time to add 300 Nodes is more than 400 mSec", mSec < 400);
    }
}
