/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

    WeakReference<Image> img1Ref;
    WeakReference<Image> img2Ref;
    int err = ERROR_NONE;
    ImageView imageView;
    Group root;
    Scene scene;

    @Override
    public void start(Stage stage) throws Exception {

        // 1. Create scene & add css stylesheet.
        imageView = new ImageView();
        root = new Group();
        root.getChildren().add(imageView);
        scene = new Scene(root);
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
                err = ERROR_LEAK;
            }
            // 4.2 Verify that image being used does not get GCed.
            if (img2Ref.get() == null) {
                err = ERROR_INCORRECT_GC;
            }
            stage.hide();
            System.exit(err);
        }
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
