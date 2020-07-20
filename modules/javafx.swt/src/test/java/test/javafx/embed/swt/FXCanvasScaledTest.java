/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.embed.swt;

import static org.junit.Assert.fail;

import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Test;

import javafx.embed.swt.FXCanvas;
import javafx.scene.Scene;
import javafx.scene.layout.Region;

public class FXCanvasScaledTest {

    private int cnt;

    static Shell shell;

    static Display display;

    /* Base size, so that with a scaling of 125% there are different results for Math.round and Math.ceil */
    final static int TARGET_BASE_SIZE = 101;

    @Test(timeout = 10000)
    public void testScale() throws Throwable {
        System.setProperty("sun.java2d.uiScale.enabled", "true");
        System.setProperty("sun.java2d.uiScale", "125%");
        System.setProperty("glass.win.uiScale", "125%");
        System.setProperty("glass.win.renderScale", "125%");
        System.setProperty("glass.gtk.uiScale", "1.25");
        System.setProperty("swt.autoScale", "125");

        // Start the Application
        display = new Display();
        shell = new Shell(display);
        shell.setLayout(new FillLayout());
        final FXCanvas canvas = new FXCanvas(shell, SWT.NONE);
        initFX(canvas);

        Timer t = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                switch (cnt) {
                    case 0:
                        display.asyncExec(() -> canvas.setBounds(0, 0, 201, 201));
                        break;
                    case 1:
                        display.asyncExec(() -> canvas.setBounds(0, 0, TARGET_BASE_SIZE, TARGET_BASE_SIZE));
                        break;
                    case 2:
                        t.cancel();
                        display.asyncExec(() -> {
                            // Capture painted component. Bounds are in pt, so size is 101 and not 127
                            GC gc = new GC(canvas);
                            final Image image = new Image(display, canvas.getBounds());
                            gc.copyArea(image, canvas.getBounds().x, canvas.getBounds().y);
                            gc.dispose();
                            PaletteData palette = image.getImageData().palette;
                            int referenceWhitePixel = image.getImageData().getPixel(0, 0);
                            RGB referenceRGB = palette.getRGB(referenceWhitePixel);
                            // check if there is a diagonal, which should be the right border
                            for (int x = 10; x < 30; x++) {
                                for (int y = 80; y < 100; y++) {
                                    int pixel = image.getImageData().getPixel(x, y);
                                    RGB rgb = palette.getRGB(pixel);
                                    if (!referenceRGB.equals(rgb)) {
                                        fail("image is skewed");
                                    }
                                }
                            }
                            shell.close();
                        });
                        break;
                }
                cnt++;
            }
        };
        t.schedule(task, 500, 500);

        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }
        display.dispose();
    }

    private static void initFX(FXCanvas canvas) {
        Region region = new Region();
        region.setStyle("-fx-background-color: #FFFFFF;" + "-fx-border-color: #000000;" + "-fx-border-width: 0 5px 0 0;"
                + "-fx-border-style: solid");
        Scene scene = new Scene(region);
        canvas.setScene(scene);
        canvas.setBounds(0, 0, 100, 100);
    }
}
