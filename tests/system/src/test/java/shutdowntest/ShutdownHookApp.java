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

package shutdowntest;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;
import util.Util;

import static shutdowntest.Constants.*;

/**
 * Test application launched by ShutdownHookTest. The FX application adds a
 * shutdown hook that calls Platform.runLater. It should be a no-op.
 */
public class ShutdownHookApp extends Application {

    // Socket for communicating with ShutdownHookTest
    private static Socket socket;
    private static OutputStream out;
    private static boolean statusWritten = false;

    private static void initSocket(String[] args) throws Exception {
        int port = Integer.parseInt(args[0]);
        socket = new Socket((String)null, port);
        out = socket.getOutputStream();
        out.write(SOCKET_HANDSHAKE);
        out.flush();
    }

    private synchronized static void writeStatus(int status) {
        if (!statusWritten) {
            statusWritten = true;
            try {
                out.write(status);
                out.flush();
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            }
        }
    }

    @Override
    public void start(Stage stage) throws Exception {

        // ShutdownHook that will be executed upon calling System.exit
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override public void run() {
                AtomicInteger err = new AtomicInteger(STATUS_OK);
                try {
                    Platform.runLater(() -> {
                        err.set(STATUS_RUNNABLE_EXECUTED);
                    });
                    // Allow time for runnable to execute (it should be ignored)
                    Util.sleep(500);
                } catch (IllegalStateException ex) {
                    err.set(STATUS_ILLEGAL_STATE);
                } catch (Throwable t) {
                    t.printStackTrace(System.err);
                    err.set(STATUS_UNEXPECTED_EXCEPTION);
                }
                writeStatus(err.get());
            }
        });

        Scene scene = new Scene(new Group(), 300, 200);
        stage.setScene(scene);
        stage.show();

        KeyFrame keyFrame = new KeyFrame(Duration.millis(500), e -> {
            System.exit(ERROR_NONE);
        });
        Timeline timeline = new Timeline(keyFrame);
        timeline.play();
    }

    public static void main(String[] args) {
        try {
            initSocket(args);
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            System.exit(Constants.ERROR_SOCKET);
        }
        Application.launch(args);
    }

}
