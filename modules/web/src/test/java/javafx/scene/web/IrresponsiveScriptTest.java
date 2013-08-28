/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package javafx.scene.web;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import javafx.event.EventHandler;
import netscape.javascript.JSException;
import org.junit.Test;

public class IrresponsiveScriptTest extends TestBase {

    @Test public void testInfiniteLoopInScript() {
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
        getEngine().setOnAlert(new EventHandler<WebEvent<String>>() {
            public void handle(WebEvent<String> ev) {
                ThreadMXBean bean = ManagementFactory.getThreadMXBean();
                long startCpuTime = bean.getCurrentThreadCpuTime();
                while (bean.getCurrentThreadCpuTime() - startCpuTime
                        < CPU_TIME_TO_RUN)
                {
                    // Do something that consumes CPU time
                    Math.sqrt(Math.random() * 21082013);
                }
            }
        });
        executeScript("alert('Jumbo!');");
    }
}
