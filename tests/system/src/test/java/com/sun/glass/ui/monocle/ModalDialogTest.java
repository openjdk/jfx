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

package com.sun.glass.ui.monocle;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.sun.glass.ui.Robot;
import junit.framework.AssertionFailedError;
import org.junit.*;
import org.junit.rules.TestName;
import com.sun.glass.ui.monocle.input.TestLog;
import com.sun.glass.ui.monocle.input.TestApplication;

import java.util.concurrent.CountDownLatch;

public class ModalDialogTest {

    @Rule
    public TestName name = new TestName();

    @Before
    public void setUpScreen() throws Exception {
        TestLog.reset();
        TestLog.log(name.getMethodName());
        TestApplication.showFullScreenScene();
    }

    @Test
    public void test1() throws Exception {
        Stage rootStage = TestApplication.getStage();
        rootStage.getScene().setOnMouseClicked(
                (e) -> TestLog.format("Clicked at %.0f, %.0f",
                        e.getScreenX(), e.getScreenY()));
        Platform.runLater(() -> {
            final Stage p = new Stage();
            p.initOwner(rootStage);
            p.initModality(Modality.APPLICATION_MODAL);
            p.setX(0);
            p.setY(0);
            p.setWidth(200);
            p.setHeight(200);
            p.setScene(new Scene(new Group()));
            p.getScene().setOnMouseClicked(
                    (e) -> TestLog.format("Clicked at %.0f, %.0f",
                            e.getScreenX(), e.getScreenY()));
            p.show();
        });
        TestLog.clear();
        Platform.runLater(() -> {
            Robot robot = com.sun.glass.ui.Application.GetApplication().createRobot();
            robot.mouseMove(300, 400);
            robot.mousePress(Robot.MOUSE_LEFT_BTN);
            robot.mouseRelease(Robot.MOUSE_LEFT_BTN);
            robot.mouseMove(100, 100);
            robot.mousePress(Robot.MOUSE_LEFT_BTN);
            robot.mouseRelease(Robot.MOUSE_LEFT_BTN);
        });
        TestLog.waitForLog("Clicked at 100, 100");
        if (TestLog.countLog("Clicked at 300, 400") != 0) {
            throw new AssertionFailedError("Disabled window should not receive mouse events!");
        }
    }
}

