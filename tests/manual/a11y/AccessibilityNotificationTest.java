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

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class AccessibilityNotificationTest extends Application implements ChangeListener<Boolean> {
    volatile static int exitCode;
    static final String INSTRUCTIONS =
            "This test is suitable for macOS and Windows only.\n" +
            "Please carefully read instructions before start testing!\n" +
            "1) Enable accessibility subsystem (Narrator on Windows, VoiceOver on macOS);\n" +
            "2) Click \"Start test\" button. The 5 seconds countdown will start;\n" +
            "3) Wait for the test to complete;\n" +
            "4) The test passes if it doesn't crash; " +
            "exceptions logged to the file \"error.log\" are expected.\n";
    static CountDownLatch latch = new CountDownLatch(1);
    static Button button = new Button("Start test");
    static TextField notificationArea = new TextField();

    public static void main(String[] args) throws InterruptedException {
        Application.launch(args);
    }

    @Override
    public void start(Stage stage) throws InterruptedException {
        Platform.accessibilityActiveProperty().addListener(this);
        stage.setTitle("Accessibility Notification Test");
        GridPane root = new GridPane();
        TextArea instructions = new TextArea();
        instructions.setText(INSTRUCTIONS);
        root.add(instructions, 0, 0);
        button = new Button("Start test");
        button.setOnAction(e -> {
            button.setDisable(true);
            new Thread(this::performTest).start();
        });
        root.add(button, 0, 1);
        root.add(notificationArea, 0, 2);
        if (Platform.accessibilityActiveProperty().get()) {
            setNotificationArea("Ready", "palegreen");
            button.setDisable(false);
        } else {
            setNotificationArea("Please enable accessibility", "red");
            button.setDisable(true);
        }
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.sizeToScene();
        stage.show();
    }

    public void yieldFor(long ms) {
        long completiontime = System.currentTimeMillis() + ms;
        do {
            try {
                Thread.sleep(1);
            } catch (InterruptedException ignore) {}
        } while (System.currentTimeMillis() < completiontime);
    }

    public void setNotificationArea(String notification, String color) {
        Platform.runLater(() -> {
            notificationArea.setText(notification);
            notificationArea.setStyle("-fx-background-color: " + color);
        });
    }

    public void performTest() {
        final String logFileName = "error.log";
        final String appName = "AccessibilityNotificationTestApp";

        new Thread(() -> {
            try {
                latch.await();
            } catch (InterruptedException ignore) {
            } finally {
                if (exitCode == 0) {
                    setNotificationArea("Complete", "green");
                }
            }
        }).start();
        for (int c = 5; c > 0 ; c--) {
            setNotificationArea("Start in " + c, "yellow");
            yieldFor(1000);
        }
        setNotificationArea("Running...", "lightyellow");
        new Thread(() -> {
            try {
                File logFile = new File(logFileName);
                String runArgs = "@../../../build/run.args";
                ProcessBuilder pb = new ProcessBuilder("java", runArgs, appName);
                pb.redirectErrorStream(true);
                pb.redirectOutput(logFile);
                Process process = pb.start();
                if (!process.waitFor(30, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    System.err.println("FAIL: Timeout waiting for test to complete");
                    System.err.println("See " + logFileName + " for more information");
                    throw new AssertionError("Error");
                }
                exitCode = process.exitValue();
                if (exitCode != 0) {
                    System.out.println("FAIL: Test exited abnormally; exitCode = " + exitCode);
                    System.out.println("See " + logFileName + " for more information");
                    setNotificationArea("TEST FAILED", "red");
                    throw new AssertionError("Error");
                }
            } catch (Throwable ignore) {
            } finally {
                latch.countDown();
            }
        }).start();
    }

    @Override
    public void changed(ObservableValue<? extends Boolean> observableValue,
                        Boolean oldValue, Boolean newValue) {
        if (newValue) {
            setNotificationArea("Ready", "palegreen");
            button.setDisable(false);
        } else {
            setNotificationArea("Please enable accessibility", "red");
            button.setDisable(true);
        }}
}
