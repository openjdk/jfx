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
package test.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.WritableImage;
import javafx.scene.robot.Robot;
import javafx.stage.Screen;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

/**
 * Standard Test Watcher for Headful Tests (JUnit 5 Only).
 * <p>
 * This facility takes a screenshot of any failed test, then logs the base64-encoded screenshot
 * to {@code stderr}.
 * <p>
 * To use, simply add the following annotation to your class:
 * <pre>{@code
 * @ExtendWith(ScreenCaptureTestWatcher.class)
 * }</pre>
 * <p>
 * For Eclipse users, override dependencies with the following entries:
 * <pre>{@code
 * --add-modules=javafx.base,javafx.graphics,javafx.controls,javafx.swing
 * --add-opens javafx.controls/test.com.sun.javafx.scene.control.test=javafx.base
 * --add-exports javafx.base/com.sun.javafx=ALL-UNNAMED
 * -Djava.library.path="../../../../build/sdk/lib"
 * -ea
 * }</pre>
 * <p>
 * WARNING: using this utility may significantly increase the size of your logs!
 * Make sure there is plenty of free disk space.
 */
// TODO investigate having a hard-coded or programmable via property limit on the number of
// captured screenshots.
public class ScreenCaptureTestWatcher implements TestWatcher {
    @Override
    public void testAborted(ExtensionContext extensionContext, Throwable err) {
        err.printStackTrace();
    }

    @Override
    public void testDisabled(ExtensionContext cx, Optional<String> optional) {
        System.out.println("Test Disabled: " + optional.get());
    }

    @Override
    public void testFailed(ExtensionContext extensionContext, Throwable err) {
        err.printStackTrace();
        // TODO perhaps this should be atomic, and output a valid JSON object so any
        // JSON-compatible log viewer could extract the image.
        System.err.println("Screenshot:{");
        System.err.println(generateScreenshot());
        System.err.println("}");
    }

    @Override
    public void testSuccessful(ExtensionContext cx) {
        //System.out.println("Test Successful: " + cx.getDisplayName());
    }

    private String generateScreenshot() {
        AtomicReference<String> ref = new AtomicReference<>();
        Util.runAndWait(() -> {
            String s = generateScreenshotFX();
            ref.set(s);
        });
        return ref.get();
    }

    private String generateScreenshotFX() {
        try {
            // there should be a JavaFX way to create images without requiring ImageIO and Swing!
            ImageIO.setUseCache(false);

            Rectangle2D bounds = Screen.getPrimary().getBounds();
            Robot r = new Robot();
            // do not scale to fit, capture all pixels
            WritableImage im = r.getScreenCapture(null, bounds, false);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            BufferedImage im2 = SwingFXUtils.fromFXImage(im, null);
            ImageIO.write(im2, "PNG", os);
            byte[] b = os.toByteArray();
            return Base64.getEncoder().encodeToString(b);
        } catch (IOException e) {
            return "error generating screenshot: " + e;
        }
    }
}
