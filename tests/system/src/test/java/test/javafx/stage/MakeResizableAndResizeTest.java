/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.stage;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import test.util.Util;

public class MakeResizableAndResizeTest {
    static CountDownLatch startupLatch = new CountDownLatch(1);
    static Timer timer;
    static Runnable runNext;
    static volatile Alert alert;

    public static void main(String[] args) {
        initFX();
        try {
            new MakeResizableAndResizeTest().testSize();
        } finally {
            shutdown();
        }
    }

    @BeforeClass
    public static void initFX() {
        Util.launch(startupLatch, TestApp.class);
    }

    @AfterClass
    public static void shutdown() {
        Util.shutdown();
    }

    @Test
    public void testSize() {
        Assert.assertTrue("Wrong window width", alert.getWidth() >= alert.getDialogPane().getWidth());
        Assert.assertTrue("Wrong window height", alert.getHeight() >= alert.getDialogPane().getHeight());
    }

    public static class TestApp extends Application implements ChangeListener {


        @Override
        public void start(Stage stage) throws Exception {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Exception Dialog");
            alert.setHeaderText("ERROR");
            alert.setContentText("Exception: ...");

            Exception ex = new FileNotFoundException("Could not find file ...");

            // Create expandable Exception.
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            String exceptionText = sw.toString();

            Label label = new Label("The exception stacktrace was:");

            TextArea textArea = new TextArea(exceptionText);
            textArea.setEditable(false);
            textArea.setWrapText(true);

            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            GridPane.setVgrow(textArea, Priority.ALWAYS);
            GridPane.setHgrow(textArea, Priority.ALWAYS);

            GridPane expContent = new GridPane();
            expContent.setMaxWidth(Double.MAX_VALUE);
            expContent.add(label, 0, 0);
            expContent.add(textArea, 0, 1);

            // Set expandable Exception into the dialog pane.
            alert.getDialogPane().setExpandableContent(expContent);
            alert.xProperty().addListener(this);
            alert.yProperty().addListener(this);
            alert.widthProperty().addListener(this);
            alert.heightProperty().addListener(this);

            MakeResizableAndResizeTest.alert = alert;
            runNext = () -> {
                System.out.println("Alert window created.");
                runNext = () -> {};
                Platform.runLater(this::resize);
            };
            alert.showAndWait();
        }

        @Override
        public void changed(ObservableValue observable, Object oldValue, Object newValue) {
            if (timer != null) {
                timer.cancel();
            }
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    runNext.run();
                }
            }, 1500);
        }

        void resize() {
            alert.getDialogPane().setExpanded(true);
            System.out.println("Details expanded.");
            runNext = startupLatch::countDown;
        }
    }

}
