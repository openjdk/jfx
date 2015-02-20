/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.web;

import com.sun.javafx.PlatformUtil;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import javafx.event.EventHandler;
import netscape.javascript.JSException;
import org.junit.Assume;
import org.junit.Test;

public class IrresponsiveScriptTest extends TestBase {

    @Test public void testInfiniteLoopInScript() {
        // Ignore test on Linux until RT-40077 is fixed
        Assume.assumeTrue(!PlatformUtil.isLinux());

        try {
            // This infinite loop should get interrupted by Webkit in about 10s.
            // If it doesn't, test times out.
            executeScript("while (true) {}");
        } catch (AssertionError e) {
            Throwable cause = e.getCause();
            if (!(cause instanceof JSException)) {
                // we expect a JSException("JavaScript execution exceeded timeout")
                // to be thrown here. Otherwise the test should fail.
                throw new AssertionError(cause);
            }
        }
    }

    @Test public void testLongWaitInHandler() {
        // This test verifies that user code is not subject to Webkit timeout.
        // It installs an alert handler that takes TIMEOUT seconds to run,
        // and checks that it is not interrupted.
        final int TIMEOUT = 24;    // seconds
        getEngine().setOnAlert(new EventHandler<WebEvent<String>>() {
            public void handle(WebEvent<String> ev) {
                try {
                    synchronized (this) {
                        wait(TIMEOUT * 1000);
                    }
                } catch (InterruptedException e) {
                }
            }
        });
        executeScript("alert('Jumbo!');");
    }

    @Test public void testLongLoopInHandler() {
        // This test verifies that user code is not subject to Webkit timeout.
        // The test installs an alert handler that takes a sufficiently large
        // amount of CPU time to run, and checks that the handler is not
        // interrupted.
        final long CPU_TIME_TO_RUN = 24L * 1000 * 1000 * 1000;
        getEngine().setOnAlert(ev -> {
            ThreadMXBean bean = ManagementFactory.getThreadMXBean();
            long startCpuTime = bean.getCurrentThreadCpuTime();
            while (bean.getCurrentThreadCpuTime() - startCpuTime
                    < CPU_TIME_TO_RUN)
            {
                // Do something that consumes CPU time
                Math.sqrt(Math.random() * 21082013);
            }
        });
        executeScript("alert('Jumbo!');");
    }
}
