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

package test.javafx.embed.swt;

import javafx.application.Platform;
import org.eclipse.swt.widgets.Display;
import org.junit.jupiter.api.BeforeAll;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public abstract class SWTTest {

    private static Display display;

    @BeforeAll
    static void beforeAll() {
        Platform.setImplicitExit(false);

        display = Display.getDefault();
    }

    protected final void runOnSwtThread(Runnable runnable) throws Throwable {
        final AtomicReference<Throwable> throwableRef = new AtomicReference<>();

        final CountDownLatch latch = new CountDownLatch(1);
        display.asyncExec(() -> {
            try {
                runnable.run();
            } catch (Throwable e) {
                throwableRef.set(e);
            } finally {
                display.asyncExec(latch::countDown);
            }
        });

        while (latch.getCount() > 0) {
            // run SWT event loop
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }

        Throwable thrown = throwableRef.get();
        if (thrown != null) {
            throw thrown;
        }
    }
}
