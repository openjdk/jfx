/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.css.imagecacheleaktest;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.lang.ref.WeakReference;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Application;

import static test.javafx.css.imagecacheleaktest.Constants.*;

/*
 * Test application launched by ImageCacheLeakTest with -Xmx16m.
 * Test steps:
 * 1. Create scene & add css stylesheet.
 * 2. Apply css to image view.
 * 3. Cause OOM
 * 4. Verify that,
 * 4.1 Unused image gets GCed.
 * 4.2 Image being used does not get GCed.
 */
public class ImageCacheLeakApp extends Application {

    // Socket for communicating with ImageCacheLeakTest
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
        WeakReference<Image> img1Ref;
        WeakReference<Image> img2Ref;
        AtomicInteger err = new AtomicInteger(STATUS_OK);

        // 1. Create scene & add css stylesheet.
        ImageView imageView = new ImageView();
        Group root = new Group();
        root.getChildren().add(imageView);
        Scene scene = new Scene(root);
        stage.setScene(scene);
        scene.getStylesheets().add(ImageCacheLeakApp.class.getResource("css.css").toExternalForm());
        stage.show();

        // 2. Apply css to image view.
        imageView.applyCss();
        imageView.getStyleClass().add("image1");
        imageView.applyCss();
        img1Ref = new WeakReference<Image>(imageView.getImage());
        imageView.getStyleClass().remove("image1");
        imageView.applyCss();

        imageView.getStyleClass().add("image2");
        imageView.applyCss();
        img2Ref = new WeakReference<Image>(imageView.getImage());

        if (img1Ref.get() == null || img2Ref.get() == null) {
            stage.hide();
            System.exit(ERROR_IMAGE_VIEW);
        }

        try {
            // 3. Cause OOM
            byte[] buf = new byte[1024 * 1024 * 20]; // 20mb
        } catch (Exception e) {
        } finally {
            // 4.1 Verify that unused image gets GCed.
            if (img1Ref.get() != null) {
                err.set(STATUS_LEAK);
            }
            // 4.2 Verify that image being used does not get GCed.
            if (img2Ref.get() == null) {
                err.set(STATUS_INCORRECT_GC);
            }
            writeStatus(err.get());
            stage.hide();
            System.exit(ERROR_NONE);
        }
    }

    public static void main(String[] args) {
        try {
            initSocket(args);
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            System.exit(ERROR_SOCKET);
        }
        Application.launch(args);
    }
}
