/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.web;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javafx.scene.web.WebEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A page callback whose Java handler throws, against the real library. The failure is logged and
 * contained, and it must not be left behind for an unrelated caller. The JNI build that commit
 * 939aa61ead replaced cleared the exception straight after the call, in
 * {@code ChromeClientJava::runJavaScriptAlert} and at every other client call site. Some C++ sites
 * branch on whether the last upcall failed, and {@code ImageBufferJavaBackend::create} is one of
 * them: a failure left pending takes a canvas's backing store away.
 */
public class ClientCallbackFailureTest extends TestBase {

    /*
     * Paints a small canvas red and reads back the red and alpha channels of one pixel. The answer
     * is "255,255" when the canvas got a backing store and "0,0" when ImageBufferJavaBackend::create
     * took its failure branch.
     */
    private static final String CANVAS_PROBE = "(function() {"
            + " var canvas = document.createElement('canvas'); canvas.width = 4; canvas.height = 4;"
            + " var g = canvas.getContext('2d'); g.fillStyle = 'rgb(255, 0, 0)';"
            + " g.fillRect(0, 0, 4, 4);"
            + " var pixel = g.getImageData(0, 0, 1, 1).data; return pixel[0] + ',' + pixel[3];"
            + " })()";

    private static final String ALERT_FAILURE =
            "javafx.web upcall page callback alert failed and was contained";
    private static final String STATUS_FAILURE =
            "javafx.web upcall page callback set_statusbar_text failed and was contained";

    private final List<String> severe = new CopyOnWriteArrayList<>();

    private final Handler handler = new Handler() {
        @Override
        public void publish(LogRecord record) {
            if (record.getLevel() == Level.SEVERE) {
                severe.add(record.getMessage());
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    };

    private final Logger logger = Logger.getLogger("com.sun.webkit.WebKitNative");

    @BeforeEach
    public void captureContainedFailures() {
        logger.addHandler(handler);
    }

    @AfterEach
    public void stopCapturing() {
        logger.removeHandler(handler);
        submit(() -> {
            getEngine().setOnAlert(null);
            getEngine().setOnStatusChanged(null);
        });
    }

    /*
     * Something between the alert and the probe already asks check_and_clear_exception on this
     * path, so the canvas survives with or without the clearing, and this case cannot catch a flag
     * left set; the two status cases below can. It stays because onAlert is the handler an
     * application most often sets: it pins the log line, and that the page still runs script and
     * paints a canvas after the handler threw.
     */
    @Test
    public void aThrowingAlertHandlerIsLoggedAndContained() {
        submit(() -> {
            WebEngine web = getEngine();
            assertEquals("255,255", web.executeScript(CANVAS_PROBE), "the probe itself");
            web.setOnAlert(event -> {
                throw new IllegalStateException("deliberate onAlert failure");
            });
            web.executeScript("alert('contained')");
            assertEquals("255,255", web.executeScript(CANVAS_PROBE),
                    "the page no longer painted a canvas after its alert callback threw");
        });
        assertTrue(severe.contains(ALERT_FAILURE), "the contained failure was not logged: " + severe);
    }

    /*
     * window.status reaches ChromeClientJava::setStatusbarText and nothing else on its way, so no
     * later upcall can clear what the failed status callback left behind before the probe runs.
     */
    @Test
    public void aThrowingStatusHandlerLeavesNothingBehindForTheNextScript() {
        submit(() -> {
            WebEngine web = getEngine();
            assertEquals("255,255", web.executeScript(CANVAS_PROBE), "the probe itself");
            web.setOnStatusChanged(event -> {
                throw new IllegalStateException("deliberate onStatusChanged failure");
            });
            web.executeScript("window.status = 'contained'");
            assertEquals("255,255", web.executeScript(CANVAS_PROBE),
                    "the failed status callback left the upcall-failure flag set");
        });
        assertTrue(severe.contains(STATUS_FAILURE),
                "the contained failure was not logged: " + severe);
    }

    @Test
    public void aThrowingStatusHandlerLeavesNothingBehindForTheRestOfItsScript() {
        submit(() -> {
            WebEngine web = getEngine();
            assertEquals("255,255", web.executeScript(CANVAS_PROBE), "the probe itself");
            web.setOnStatusChanged(event -> {
                throw new IllegalStateException("deliberate onStatusChanged failure");
            });
            assertEquals("255,255",
                    web.executeScript("window.status = 'contained'; " + CANVAS_PROBE),
                    "the failed status callback left the upcall-failure flag set");
        });
        assertTrue(severe.contains(STATUS_FAILURE),
                "the contained failure was not logged: " + severe);
    }
}
