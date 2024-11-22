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
package test.robot.javafx.scene.control.behavior;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.WritableImage;
import javafx.scene.robot.Robot;
import javafx.stage.Screen;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import test.util.Util;

/**
 * Takes a screenshot of any failed test, then outputs it to {@code stderr}
 * in a base-64-encoded data URL.  This string can be copied to a browser address bar
 * to see the image (the image might be truncated by the browser, so try different browsers
 * or use a dedicated program).
 * <p>
 * To use, simply add the following annotation to your class:
 * <pre>{@code
 * @ExtendWith(ScreenshotFailedTestWatcher.class)
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
public class ScreenshotFailedTestWatcher implements TestWatcher {
    @Override
    public void testFailed(ExtensionContext cx, Throwable e) {
        System.err.println(generateScreenshot("Screenshot:\ndata:image/png;base64,", "\n"));
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
            String b64 = Base64.getEncoder().encodeToString(b);

            int len0 = (prefix == null) ? 0 : prefix.length();
            int len1 = (postfix == null) ? 0 : postfix.length();

            if ((len0 + len1) == 0) {
                return b64;
            }

            int sz = b64.length() + len0 + len1;
            StringBuilder sb = new StringBuilder(sz);
            if (prefix != null) {
                sb.append(prefix);
            }
            sb.append(b64);
            if (postfix != null) {
                sb.append(postfix);
            }
            return sb.toString();
        } catch (IOException e) {
            return "error generating screenshot: " + e;
        }
    }
}
