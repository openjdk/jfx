/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.appmanager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.CountDownLatch;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public final class BootstrapApplication extends Application {
    private static final CountDownLatch appStartedLatch =
            new CountDownLatch(1);

    @Override
    public void start(final Stage primaryStage) {
//        Platform.setImplicitExit(false);
        final BorderPane root = new BorderPane();
        root.setCenter(new Label("Mobile Center"));
        root.setBottom(createOutputArea());

        primaryStage.setTitle("Mobile Center");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();

        appStartedLatch.countDown();
    }

    public static void waitForStart() throws InterruptedException {
        appStartedLatch.await();
    }

    private static Node createOutputArea() {
        final TextArea textArea = new TextArea();
        textArea.setPrefColumnCount(80);
        textArea.setPrefRowCount(25);
        textArea.setEditable(false);
        textArea.setFont(Font.font("Monospaced"));
        textArea.setWrapText(true);

        final OutputStream textAreaStream = new TextAreaStream(textArea);
        System.setOut(new PrintStream(textAreaStream, true));
        System.setErr(new PrintStream(textAreaStream, true));

        return textArea;
    }

    private static final class TextAreaStream extends OutputStream
                                              implements Runnable {
        private final TextArea textArea;
        private final ByteArrayOutputStream byteBuffer;
        private final StringBuilder textToAppend;

        public TextAreaStream(final TextArea textArea) {
            this.textArea = textArea;
            this.byteBuffer = new ByteArrayOutputStream();
            this.textToAppend = new StringBuilder();
        }

        @Override
        public void write(final int value) {
            synchronized (byteBuffer) {
                byteBuffer.write(value);
            }
        }

        @Override
        public void write(final byte buffer[], final int offset,
                          final int length) {
            synchronized (byteBuffer) {
                byteBuffer.write(buffer, offset, length);
            }
        }

        @Override
        public synchronized void flush() throws IOException {
            final byte[] bytes;
            synchronized (byteBuffer) {
                byteBuffer.flush();
                if (byteBuffer.size() == 0) {
                    return;
                }

                bytes = byteBuffer.toByteArray();
                byteBuffer.reset();
            }

            synchronized (textToAppend) {
                if (textToAppend.length() == 0) {
                    // no flush has been schedulet yet
                    Platform.runLater(this);
                }

                textToAppend.append(new String(bytes));
            }
        }

        @Override
        public void run() {
            final String newText;
            synchronized (textToAppend) {
                newText = textToAppend.toString();
                textToAppend.setLength(0);
            }

            textArea.appendText(newText);
        }
    }
}
