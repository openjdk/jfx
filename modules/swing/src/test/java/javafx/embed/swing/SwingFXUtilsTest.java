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

package javafx.embed.swing;

import java.awt.image.BufferedImage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.stage.Stage;
import junit.framework.AssertionFailedError;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

public class SwingFXUtilsTest {
    static final boolean verbose = false;

    // Used to launch the application before running any test
    private static final CountDownLatch launchLatch = new CountDownLatch(1);

    // Application class. An instance is created and initialized before running
    // the first test, and it lives through the execution of all tests.
    public static class MyApp extends Application {
        @Override
        public void start(Stage primaryStage) throws Exception {
            Platform.setImplicitExit(false);
            assertTrue(Platform.isFxApplicationThread());
            assertNotNull(primaryStage);

            launchLatch.countDown();
        }
    }

    @BeforeClass
    public static void doSetupOnce() {
        // Start the Application
        new Thread(() -> Application.launch(MyApp.class, (String[]) null)).start();

        try {
            if (!launchLatch.await(5000, TimeUnit.MILLISECONDS)) {
                throw new AssertionFailedError("Timeout waiting for Application to launch");
            }
        } catch (InterruptedException ex) {
            AssertionFailedError err = new AssertionFailedError("Unexpected exception");
            err.initCause(ex);
            throw err;
        }

        assertEquals(0, launchLatch.getCount());
    }

    @AfterClass
    public static void doTeardownOnce() {
        Platform.exit();
    }

    @Test
    public void testFromFXImg() {
        testFromFXImg("alpha.png");
        testFromFXImg("opaque.gif");
        testFromFXImg("opaque.jpg");
        testFromFXImg("opaque.png");
        testFromFXImg("trans.gif");
    }

    static void testFromFXImg(String imgfilename) {
        Image img = new Image("javafx/embed/swing/"+imgfilename);
        boolean rgbrequired = (img.getPixelReader().getPixelFormat().getType() == PixelFormat.Type.BYTE_RGB);
        BufferedImage bimg = SwingFXUtils.fromFXImage(img, null);
        checkBimg(img, bimg);
        boolean reusesitself = reusesBimg(img, bimg, true);
        boolean reusesxrgb = reusesBimg(img, BufferedImage.TYPE_INT_RGB, rgbrequired);
        boolean reusesargb = reusesBimg(img, BufferedImage.TYPE_INT_ARGB, true);
        boolean reusesargbpre = reusesBimg(img, BufferedImage.TYPE_INT_ARGB_PRE, true);
        if (verbose) {
            System.out.println(imgfilename+" type = "+img.getPixelReader().getPixelFormat());
            System.out.println(imgfilename+" bimg type = "+bimg.getType());
            System.out.println(imgfilename+" reuses own bimg = "+reusesitself);
            System.out.println(imgfilename+" reuses rgb bimg = "+reusesxrgb);
            System.out.println(imgfilename+" reuses argb bimg = "+reusesargb);
            System.out.println(imgfilename+" reuses argb pre bimg = "+reusesargbpre);
            System.out.println();
        }
    }

    static boolean reusesBimg(Image img, int type, boolean required) {
        int iw = (int) img.getWidth();
        int ih = (int) img.getHeight();
        BufferedImage bimg = new BufferedImage(iw, ih, type);
        return reusesBimg(img, bimg, required);
    }

    static boolean reusesBimg(Image img, BufferedImage bimg, boolean required) {
        BufferedImage ret = SwingFXUtils.fromFXImage(img, bimg);
        checkBimg(img, ret);
        if (required) {
            assertTrue(bimg == ret);
        }
        return (bimg == ret);
    }

    static void checkBimg(Image img, BufferedImage bimg) {
        PixelReader pr = img.getPixelReader();
        int iw = (int) img.getWidth();
        int ih = (int) img.getHeight();
        for (int y = 0; y < ih; y++) {
            for (int x = 0; x < iw; x++) {
                int imgargb = pr.getArgb(x, y);
                int bimgargb = bimg.getRGB(x, y);
                if (imgargb != bimgargb) {
                    System.err.println(">>>> wrong color in bimg: "+hex(bimgargb)+
                                       " at "+x+", "+y+
                                       " should be: "+hex(imgargb));
                    assertEquals(imgargb, bimgargb);
                }
            }
        }
    }

    static String hex(int i) {
        return String.format("0x%08x", i);
    }
}
