/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.embed.swing;

import java.awt.AlphaComposite;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.SecondaryLoop;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.IntBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;
import com.sun.javafx.application.PlatformImpl;
import com.sun.javafx.tk.Toolkit;
import sun.awt.AWTAccessor;
import sun.awt.FwDispatcher;
import sun.awt.image.IntegerComponentRaster;

/**
 * This class provides utility methods for converting data types between
 * Swing/AWT and JavaFX formats.
 * @since JavaFX 2.2
 */
public class SwingFXUtils {
    private SwingFXUtils() {} // no instances

    /**
     * Snapshots the specified {@link BufferedImage} and stores a copy of
     * its pixels into a JavaFX {@link Image} object, creating a new
     * object if needed.
     * The returned {@code Image} will be a static snapshot of the state
     * of the pixels in the {@code BufferedImage} at the time the method
     * completes.  Further changes to the {@code BufferedImage} will not
     * be reflected in the {@code Image}.
     * <p>
     * The optional JavaFX {@link WritableImage} parameter may be reused
     * to store the copy of the pixels.
     * A new {@code Image} will be created if the supplied object is null,
     * is too small or of a type which the image pixels cannot be easily
     * converted into.
     * 
     * @param bimg the {@code BufferedImage} object to be converted
     * @param wimg an optional {@code WritableImage} object that can be
     *        used to store the returned pixel data
     * @return an {@code Image} object representing a snapshot of the
     *         current pixels in the {@code BufferedImage}.
     * @since JavaFX 2.2
     */
    public static WritableImage toFXImage(BufferedImage bimg, WritableImage wimg) {
        int bw = bimg.getWidth();
        int bh = bimg.getHeight();
        switch (bimg.getType()) {
            case BufferedImage.TYPE_INT_ARGB:
            case BufferedImage.TYPE_INT_ARGB_PRE:
                break;
            default:
                BufferedImage converted =
                    new BufferedImage(bw, bh, BufferedImage.TYPE_INT_ARGB_PRE);
                Graphics2D g2d = converted.createGraphics();
                g2d.drawImage(bimg, 0, 0, null);
                g2d.dispose();
                bimg = converted;
                break;
        }
        // assert(bimg.getType == TYPE_INT_ARGB[_PRE]);
        if (wimg != null) {
            int iw = (int) wimg.getWidth();
            int ih = (int) wimg.getHeight();
            if (iw < bw || ih < bh) {
                wimg = null;
            } else if (bw < iw || bh < ih) {
                int empty[] = new int[iw];
                PixelWriter pw = wimg.getPixelWriter();
                PixelFormat pf = PixelFormat.getIntArgbPreInstance();
                if (bw < iw) {
                    pw.setPixels(bw, 0, iw-bw, bh, pf, empty, 0, 0);
                }
                if (bh < ih) {
                    pw.setPixels(0, bh, iw, ih-bh, pf, empty, 0, 0);
                }
            }
        }
        if (wimg == null) {
            wimg = new WritableImage(bw, bh);
        }
        PixelWriter pw = wimg.getPixelWriter();
        IntegerComponentRaster icr = (IntegerComponentRaster) bimg.getRaster();
        int data[] = icr.getDataStorage();
        int offset = icr.getDataOffset(0);
        int scan = icr.getScanlineStride();
        PixelFormat<IntBuffer> pf = (bimg.isAlphaPremultiplied() ?
                                     PixelFormat.getIntArgbPreInstance() :
                                     PixelFormat.getIntArgbInstance());
        pw.setPixels(0, 0, bw, bh, pf, data, offset, scan);
        return wimg;
    }

    /**
     * Snapshots the specified JavaFX {@link Image} object and stores a
     * copy of its pixels into a {@link BufferedImage} object, creating
     * a new object if needed.
     * The method will only convert a JavaFX {@code Image} that is readable
     * as per the conditions on the
     * {@link Image#getPixelReader() Image.getPixelReader()}
     * method.
     * If the {@code Image} is not readable, as determined by its
     * {@code getPixelReader()} method, then this method will return null.
     * If the {@code Image} is a writable, or other dynamic image, then
     * the {@code BufferedImage} will only be set to the current state of
     * the pixels in the image as determined by its {@link PixelReader}.
     * Further changes to the pixels of the {@code Image} will not be
     * reflected in the returned {@code BufferedImage}.
     * <p>
     * The optional {@code BufferedImage} parameter may be reused to store
     * the copy of the pixels.
     * A new {@code BufferedImage} will be created if the supplied object
     * is null, is too small or of a type which the image pixels cannot
     * be easily converted into.
     * 
     * @param img the JavaFX {@code Image} to be converted
     * @param bimg an optional {@code BufferedImage} object that may be
     *        used to store the returned pixel data
     * @return a {@code BufferedImage} containing a snapshot of the JavaFX
     *         {@code Image}, or null if the {@code Image} is not readable.
     * @since JavaFX 2.2
     */
    public static BufferedImage fromFXImage(Image img, BufferedImage bimg) {
        PixelReader pr = img.getPixelReader();
        if (pr == null) {
            return null;
        }
        int iw = (int) img.getWidth();
        int ih = (int) img.getHeight();
        if (bimg != null) {
            int type = bimg.getType();
            int bw = bimg.getWidth();
            int bh = bimg.getHeight();
            if (bw < iw || bh < ih ||
                (type != BufferedImage.TYPE_INT_ARGB &&
                 type != BufferedImage.TYPE_INT_ARGB_PRE))
            {
                bimg = null;
            } else if (iw < bw || ih < bh) {
                Graphics2D g2d = bimg.createGraphics();
                g2d.setComposite(AlphaComposite.Clear);
                g2d.fillRect(0, 0, bw, bh);
                g2d.dispose();
            }
        }
        if (bimg == null) {
            bimg = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB_PRE);
        }
        IntegerComponentRaster icr = (IntegerComponentRaster) bimg.getRaster();
        int offset = icr.getDataOffset(0);
        int scan = icr.getScanlineStride();
        int data[] = icr.getDataStorage();
        WritablePixelFormat<IntBuffer> pf = (bimg.isAlphaPremultiplied() ?
                                             PixelFormat.getIntArgbPreInstance() :
                                             PixelFormat.getIntArgbInstance());
        pr.getPixels(0, 0, iw, ih, pf, data, offset, scan);
        return bimg;
    }

    /**
     * If called from the FX Application Thread
     * invokes a runnable directly blocking the calling code
     * Otherwise
     * uses Platform.runLater without blocking
     */
    static void runOnFxThread(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }

    private static class FwSecondaryLoop implements SecondaryLoop {

        private final AtomicBoolean isRunning = new AtomicBoolean(false);

        @Override public boolean enter() {
            if (isRunning.compareAndSet(false, true)) {
                PlatformImpl.runAndWait(new Runnable() {
                    @Override public void run() {
                        Toolkit.getToolkit().enterNestedEventLoop(FwSecondaryLoop.this);
                    }
                });
                return true;
            }
            return false;
        }

        @Override public boolean exit() {
            if (isRunning.compareAndSet(true, false)) {
                PlatformImpl.runAndWait(new Runnable() {
                    @Override public void run() {
                        Toolkit.getToolkit().exitNestedEventLoop(FwSecondaryLoop.this, null);
                    }
                });
                return true;
            }
            return false;
        }
    }

    private static class FXDispatcher implements FwDispatcher {
        @Override public boolean isDispatchThread() {
            return Platform.isFxApplicationThread();
        }

        @Override public void scheduleDispatch(Runnable runnable) {
            Platform.runLater(runnable);
        }

        @Override public SecondaryLoop createSecondaryLoop() {
            return new FwSecondaryLoop();
        }
    }

    //Called with reflection from PlatformImpl to avoid dependency
    public static void installFwEventQueue() {
        EventQueue eq = AccessController.doPrivileged(
                new PrivilegedAction<EventQueue>() {
                    @Override public EventQueue run() {
                        return java.awt.Toolkit.getDefaultToolkit().getSystemEventQueue();
                    }
                });
        AWTAccessor.getEventQueueAccessor().setFwDispatcher(eq, new FXDispatcher());
    }
}
