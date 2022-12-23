/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates. All rights reserved.
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

import org.eclipse.swt.widgets.Display;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A {@code MethodRule} to execute test methods synchronously on the SWT UI thread. The execution will wait for any
 * asynchronous runnables scheduled by the test method on the SWT UI thread during its execution.
 */
public class SwtRule implements MethodRule {

    private void rethrow(final AtomicReference<Throwable> throwableRef) throws Throwable {
        Throwable thrown = throwableRef.get();
        if (thrown != null) {
            throw thrown;
        }
    }

    @Override
    public Statement apply(final Statement base, final FrameworkMethod testMethod, final Object target) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                Display display = Display.getDefault();

                // keep track of exceptions thrown in UI thread
                final AtomicReference<Throwable> throwableRef = new AtomicReference<>();

                final CountDownLatch latch = new CountDownLatch(1);
                display.asyncExec(() -> {
                    try {
                        // ensure test method is synchronously executed (without spawning a new thread)
                        testMethod.invokeExplosively(target);
                    } catch (Throwable throwable) {
                        throwableRef.set(throwable);
                    } finally {
                        display.asyncExec(() -> {
                            // wait for any runnables scheduled (asynchronously)
                            // by test method on the UI thread
                            latch.countDown();
                        });
                    }
                });

                while (latch.getCount() > 0) {
                    // run SWT event loop
                    if (!display.readAndDispatch()) {
                        display.sleep();
                    }
                }

                rethrow(throwableRef);
            }
        };
    }
}
