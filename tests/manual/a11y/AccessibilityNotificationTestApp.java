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

import java.util.concurrent.CountDownLatch;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class AccessibilityNotificationTestApp extends Application {
    static TextGenerator tg;
    static CountDownLatch latch = new CountDownLatch(1);

    @Override
    public void start(Stage stage) throws Exception {
        stage.setTitle("Accessibility Test Children");
        tg = new TextGenerator();
        BorderPane root = new BorderPane();
        TextArea textArea = new TextArea();
        textArea.textProperty().bind(tg.getValue());
        root.setCenter(textArea);
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.sizeToScene();
        stage.setOnShown(we -> {
            if (we.getEventType() == WindowEvent.WINDOW_SHOWN) {
                Platform.runLater(this::performTest);
            }
        });
        stage.show();
    }

    public void performTest() {
        new Thread(() -> {
            try {
                latch.await();
            } catch (InterruptedException ignore) {
            } finally {
                Platform.exit();
            }
        }).start();
        yieldFor(1000);
        new Thread(() -> {
            try {
                for (int i = 0; i < 1000; i++) {
                    tg.addValue(String.valueOf(i));
                    yieldFor(10);
                }
            } catch (Throwable ignore) {
            } finally {
                latch.countDown();
            }
        }).start();
    }

    public void yieldFor(long ms) {
        long completiontime = System.currentTimeMillis() + ms;
        do {
            try {
                Thread.sleep(1);
            } catch (InterruptedException ignore) {}
        } while (System.currentTimeMillis() < completiontime);
    }

    public class TextGenerator{
        private final StringProperty value;

        public TextGenerator() {
            value = new SimpleStringProperty("");
        }

        public StringProperty getValue() {
            return value;
        }

        public void addValue(String s) {
            try {
                this.value.set(this.value.getValue() + s);
            } catch (Exception ignore) {}
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Application.launch(args);
        System.out.println("Test completed");
    }
}
