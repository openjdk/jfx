/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
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
 * This TestWatcher writes a base-64 encoded PNG screenshot of the desktop to {@code stderr}
 * when a test fails.
 * <p>
 * To use, simply add the following annotation to your class:
 * <pre>{@code
 * @ExtendWith(ScreenCaptureTestWatcher.class)
 * }</pre>
 * <p>
 * WARNING: using this utility may significantly increase the size of your logs!
 * Make sure there is plenty of free disk space.
 */
public class ScreenCaptureTestWatcher implements TestWatcher {
    @Override
    public void testFailed(ExtensionContext extensionContext, Throwable err) {
        err.printStackTrace();
        System.err.println(generateScreenshot("Screenshot:{", "}"));
    }

    private String generateScreenshot(String prefix, String postfix) {
        AtomicReference<String> ref = new AtomicReference<>();
        Util.runAndWait(() -> {
            String s = generateScreenshotFX(prefix, postfix);
            ref.set(s);
        });
        return ref.get();
    }

    private String generateScreenshotFX(String prefix, String postfix) {
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
            String s = Base64.getEncoder().encodeToString(b);
            if ((prefix == null) && (postfix == null)) {
                return s;
            }
            return
                (prefix == null ? "" : prefix) +
                s +
                (postfix == null ? "" : postfix);
        } catch (IOException e) {
            return "error generating screenshot: " + e;
        }
    }
}
