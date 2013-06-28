/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package javafx.scene.web;

import java.util.concurrent.atomic.AtomicBoolean;
import javafx.event.EventHandler;
import netscape.javascript.JSException;
import org.junit.Test;
import static org.junit.Assert.*;


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
    
    @Test public void testInfiniteLoopInHandler() {
        // This test verifies that user code is not subject to Webkit timeout.
        // It installs an alert handler that takes TIMEOUT seconds to run,
        // and checks that it is not interrupted.
        final int TIMEOUT = 24;    // seconds
        final AtomicBoolean passed = new AtomicBoolean(false);
        getEngine().setOnAlert(new EventHandler<WebEvent<String>>() {
            public void handle(WebEvent<String> ev) {
                try {
                    synchronized (this) {
                        wait(TIMEOUT * 1000);
                    }
                    passed.set(true);
                } catch (InterruptedException e) {
                }
            }
        });
        executeScript("alert('Jumbo!');");
    }
}
