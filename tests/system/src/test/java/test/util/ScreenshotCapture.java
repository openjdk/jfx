/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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
import java.io.PrintStream;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.WritableImage;
import javafx.scene.robot.Robot;
import javafx.stage.Screen;

/**
 * A utility to capture a screenshot via JavaFX {@link Robot}.
 * <p>
 * Example:
 * <pre>
 * // write a base-64 encoded screenshot to stderr
 * ScreenshotCapture.writeScreenshot();
 * </pre>
 */
public class ScreenshotCapture {
    /**
     * Captures a screenshot using JavaFX {@link Robot} in PNG format,
     * returning it as a byte array.
     * <p>
     * This method can be called from any thread.  If called from a thread other than
     * the JavaFX Application Thread, the current thread will be paused until the screenshot is taken.
     *
     * @return the byte array containing the screenshot in PNG format
     * @throws IOException when an I/O error occurs
     */
    public static byte[] takeScreenshot() throws IOException {
        if (Platform.isFxApplicationThread()) {
            return screenshotFX();
        } else {
            AtomicReference<Object> ref = new AtomicReference<>();
            Util.runAndWait(() -> {
                try {
                    Object s = screenshotFX();
                    ref.set(s);
                } catch (IOException e) {
                    ref.set(e);
                }
            });
            Object result = ref.get();
            if (result instanceof IOException e) {
                throw e;
            }
            return (byte[])result;
        }
    }

    /**
     * Captures a screenshot using JavaFX {@link Robot} in PNG format,
     * then writes it in a Base-64 encoding to {@code System.err}.
     * <p>
     * Example:
     * <pre>
     * Screenshot:
     * data:image/png;base64,iVBORw0KGgoA...</pre>
     */
    public static void writeScreenshot() {
        System.err.println(ScreenshotCapture.takeScreenshotBase64("Screenshot:\ndata:image/png;base64,", null));
    }

    /**
     * Captures a screenshot using JavaFX {@link Robot} in PNG format,
     * in the form of a Base-64 encoded {@code String}.
     * <p>
     * This method can be called from any thread.  If called from a thread other than
     * the JavaFX Application Thread, the current thread will be paused until the screenshot is taken.
     *
     * @param prefix the string to append before the base-64 representation, or null
     * @param postfix the string to append after the base-64 representation, or null
     * @return the screenshot in Base-64 encoded PNG, or an error message
     */
    public static String takeScreenshotBase64(String prefix, String postfix) {
        Object result;
        if (Platform.isFxApplicationThread()) {
            try {
                result = screenshotFX();
            } catch (IOException e) {
                result = e;
            }
        } else {
            AtomicReference<Object> ref = new AtomicReference<>();
            Util.runAndWait(() -> {
                try {
                    Object s = screenshotFX();
                    ref.set(s);
                } catch (IOException e) {
                    ref.set(e);
                }
            });
            result = ref.get();
        }

        if (result instanceof IOException e) {
            return "error generating screenshot: " + e;
        }

        byte[] b = (byte[])result;
        String s = Base64.getEncoder().encodeToString(b);
        if ((prefix == null) && (postfix == null)) {
            return s;
        }
        return
            (prefix == null ? "" : prefix) +
            s +
            (postfix == null ? "" : postfix);
    }

    private static byte[] screenshotFX() throws IOException {
        // there should be a JavaFX way to create images without requiring ImageIO and Swing!
        ImageIO.setUseCache(false);

        Rectangle2D bounds = Screen.getPrimary().getBounds();
        Robot r = new Robot();
        // do not scale to fit, capture all pixels
        WritableImage im = r.getScreenCapture(null, bounds, false);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        BufferedImage im2 = SwingFXUtils.fromFXImage(im, null);
        ImageIO.write(im2, "PNG", os);
        return os.toByteArray();
    }
}
