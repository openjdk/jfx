/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import static java.lang.String.format;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.w3c.dom.Document;

public class MiscellaneousTest extends TestBase {

    @Test public void testNoEffectOnFollowRedirects() {
        assertEquals("Unexpected HttpURLConnection.getFollowRedirects() result",
                true, HttpURLConnection.getFollowRedirects());
        load("test/html/ipsum.html");
        assertEquals("Unexpected HttpURLConnection.getFollowRedirects() result",
                true, HttpURLConnection.getFollowRedirects());
    }

    @Test public void testRT22458() throws Exception {
        final WebEngine webEngine = createWebEngine();
        Platform.runLater(() -> {
            webEngine.load(format("file://%d.ajax.googleapis.com/ajax",
                                  new Random().nextInt()));
        });
        Thread.sleep(200);
        long startTime = System.currentTimeMillis();
        DummyClass.dummyField++;
        long time = System.currentTimeMillis() - startTime;
        if (time > 2000) {
            fail(format("DummyClass took %f seconds to load", time / 1000.));
        }
    }

    private static final class DummyClass {
        private static int dummyField;
    }

    @org.junit.Ignore
    @Test public void testRT30835() throws Exception {
        class Record {
            private final Document document;
            private final String location;
            public Record(Document document, String location) {
                this.document = document;
                this.location = location;
            }
        }
        final ArrayList<Record> records = new ArrayList<Record>();
        ChangeListener<State> listener = (ov, oldValue, newValue) -> {
            if (newValue == State.SUCCEEDED) {
                records.add(new Record(
                        getEngine().getDocument(),
                        getEngine().getLocation()));
            }
        };
        submit(() -> {
            getEngine().getLoadWorker().stateProperty().addListener(listener);
        });
        String location = new File("src/test/resources/test/html/RT30835.html")
                .toURI().toASCIIString().replaceAll("^file:/", "file:///");
        load(location);
        assertEquals(1, records.size());
        assertNotNull(records.get(0).document);
        assertEquals(location, records.get(0).location);
    }

    @Test public void testRT26306() {
        loadContent(
                "<script language='javascript'>\n" +
                "var s = '0123456789abcdef';\n" +
                "while (true) {\n" +
                "    alert(s.length);\n" +
                "    s = s + s;\n" +
                "}\n" +
                "</script>");
    }

    @Test public void testWebViewWithoutSceneGraph() {
        submit(() -> {
             WebEngine engine = new WebView().getEngine();
             engine.getLoadWorker().stateProperty().addListener(
                    (observable, oldValue, newValue) -> {
                        if (State.SUCCEEDED == newValue) {
                            engine.executeScript(
                                "window.scrollTo" +
                                "(0, document.documentElement.scrollHeight)");
                        }
                    });
             engine.loadContent("<body> <a href=#>hello</a></body>");
        });
    }

    // JDK-8133775
    @Test(expected = IllegalStateException.class) public void testDOMObjectThreadOwnership() {
          class IllegalStateExceptionChecker {
              public Object resultObject;
              public void start() {
                 WebEngine engine = new WebEngine();
                 // Get DOM object from JavaFX Application Thread.
                 resultObject = engine.executeScript("document.createElement('span')");
              }
           }
           IllegalStateExceptionChecker obj = new IllegalStateExceptionChecker();
           submit(obj::start);
           // Try accessing the resultObject created in JavaFX Application Thread
           // from someother thread. It should throw an exception.
           obj.resultObject.toString();
     }

    // JDK-8162715
    public class TimerCallback {
        private static final int INTERVAL_COUNT = 20;
        private final CountDownLatch latch = new CountDownLatch(INTERVAL_COUNT);
        private class Stat {
            private long firedTime;
            private long createdTime;
            private long interval;
        }
        private Stat[] stats = new Stat[INTERVAL_COUNT];

        public void call(long createdTime, long interval, int index) {
            Stat stat = new Stat();
            stat.firedTime = System.currentTimeMillis();
            stat.createdTime = createdTime;
            stat.interval = interval;
            stats[index] = stat;
            latch.countDown();
        }
    }

    @Test(timeout = 30000) public void testDOMTimer() {
        final TimerCallback timer = new TimerCallback();
        final WebEngine webEngine = createWebEngine();
        submit(() -> {
            final JSObject window = (JSObject) webEngine.executeScript("window");
            assertNotNull(window);
            window.setMember("timer", timer);
            // Try various intervals
            for (int i = 0; i < timer.INTERVAL_COUNT; i++) {
                int timeout = i * (1000 / timer.INTERVAL_COUNT);
                webEngine.executeScript("window.setTimeout("
                                      + "timer.call.bind(timer, Date.now(),"
                                      // pass 'i' to call to test time
                                      + timeout +"," + i + "),"
                                      // set 'i' as a timeout interval
                                      + timeout + ")");
            }

        });

        try {
            timer.latch.await();
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
        for (TimerCallback.Stat stat : timer.stats) {
            assertNotNull(stat);
            final String msg = String.format(
                    "expected delta:%d, actual delta:%d",
                    stat.interval,
                    stat.firedTime - stat.createdTime);
            // Timer should not fire too early. Added 20 ms offset to compensate
            // the floating point approximation issues while dealing with timer.
            assertTrue(msg,
                    ((stat.firedTime + 20) - stat.createdTime) >= stat.interval);
            // Timer should not be too late. Since it is not a real time system,
            // we can't expect the timer to be fire at exactly on the requested
            // time, give a 1000 ms extra time.
            assertTrue(msg,
                    (stat.firedTime - stat.createdTime) <= (stat.interval + 1000));
        }
    }

    /**
     * @test
     * @bug 8163582
     * summary svg.path.getTotalLength
     * Load a simple SVG, Replace its path and get its path's totalLength using pat.getTotalLength
     */
    @Test(timeout = 30000) public void testSvgGetTotalLength() throws Exception {
        final String svgStub = "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 512 512'>" +
                " <path id='pathId' d='M150 0 L75 200 L225 200 Z' /> <svg>";

        // <Path, [Expected, Error Tolerance]>
        final HashMap<String, Double[]> svgPaths = new HashMap<>();
        svgPaths.put("'M 0 0 L 100 0 L 100 100 L 0 100 Z'",
                new Double[] {400.0, 0.000001});
        svgPaths.put("'M 0 0 l 100 0 l 0 100 l -100 0 Z'",
                new Double[] {400.0, 0.000001});
        svgPaths.put("'M 0 0 t 0 100'",
                new Double[] {100.0, 0.1});
        svgPaths.put("'M 0 0 Q 55 50 100 100'",
                new Double[] {141.4803314, 0.000001});
        svgPaths.put("'M 778.4191616766467 375.19086364081954 C 781.239563 " +
                        "375.1908569 786.8525244750526 346.60170830052556 786.8802395209582 346.87991373394766'",
                new Double[] {29.86020, 0.0001});
        svgPaths.put("'M 0 0 C 0.00001 0.00001 0.00002 0.00001 0.00003 0'",
                new Double[] {0.0000344338, 0.000000001});

        loadContent(svgStub);

        svgPaths.forEach((pathData, expected) -> {
            executeScript("document.getElementById('pathId').setAttribute('d' , " + pathData + ");");
            // Get svg path's total length
            Double totalLength = ((Number) executeScript("document.getElementById('pathId').getTotalLength();")).doubleValue();
            final String msg = String.format(
                    "svg.path.getTotalLength() for %s, expected : %f, actual : %f",
                    pathData, expected[0], totalLength);
            assertEquals(msg,
                    expected[0], totalLength, expected[1]);
        });
    }

    // This test case will be removed once we implement Websql feature.
    @Test public void testWebSQLUndefined() {
        final WebEngine webEngine = createWebEngine();
        submit(() -> {
            assertEquals("undefined", webEngine.executeScript("window.openDatabase"));
        });
    }

    private WebEngine createWebEngine() {
        return submit(() -> new WebEngine());
    }
}
