/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import javafx.embed.swt.FXCanvas;
import javafx.scene.Group;
import javafx.scene.ImageCursor;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertNotNull;

public class SWTCursorsTest {

    @Test(timeout=10000)
    public void testImageCursor() throws Throwable {
        // create display on first thread (required when using SWT on Mac OS X)
        final Display display = new Display();
        final Shell shell = new Shell(display);
        final FXCanvas canvas = new FXCanvas(shell, SWT.NONE);
        shell.open();

        // keep track of exceptions thrown in UI thread
        final AtomicReference<Throwable> throwableRef = new AtomicReference<>();

        final CountDownLatch latch = new CountDownLatch(2);
        display.asyncExec(() -> {
            try {
                // create and hook scene
                Scene scene = new Scene(new Group());
                canvas.setScene(scene);

                // set image cursor to scene
                Image cursorImage = new Image("test/javafx/embed/swt/cursor.png");
                scene.setCursor(new ImageCursor(cursorImage));

            } catch (Throwable throwable) {
                throwableRef.set(throwable);
            } finally {
                latch.countDown();
            }
        });

        while (latch.getCount() > 1) {
            // run SWT event loop
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }

        rethrow(throwableRef);

        // ensure at least one pulse has passed before asserting the cursor is set,
        // as the scene property synchronization is triggered by the pulse listener
        display.asyncExec(() -> {
            try {
                assertNotNull(canvas.getCursor());
            } catch (Throwable throwable) {
                throwableRef.set(throwable);
            } finally {
                latch.countDown();
            }
        });

        while (latch.getCount() > 0) {
            // run SWT event loop (again)
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }

        shell.close();
        shell.dispose();
        display.dispose();

        rethrow(throwableRef);
    }

    private void rethrow(AtomicReference<Throwable> throwableRef) throws Throwable {
        Throwable thrown = throwableRef.get();
        if (thrown != null) {
            throw thrown;
        }
    }
}
