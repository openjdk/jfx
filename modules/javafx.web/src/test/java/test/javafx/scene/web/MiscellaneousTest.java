/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.junit.Test;
import org.w3c.dom.Document;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javafx.scene.web.WebEngineShim;
import com.sun.webkit.WebPage;
import com.sun.webkit.WebPageShim;
import com.sun.webkit.graphics.WCGraphicsContext;

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
                new Double[] {141.4803314, 0.001});
        svgPaths.put("'M 778.4191616766467 375.19086364081954 C 781.239563 " +
                        "375.1908569 786.8525244750526 346.60170830052556 786.8802395209582 346.87991373394766'",
                new Double[] {29.86020, 0.1});
        svgPaths.put("'M 0 0 C 0.00001 0.00001 0.00002 0.00001 0.00003 0'",
                new Double[] {0.0000344338, 0.0001});

        loadContent(svgStub);

        svgPaths.forEach((pathData, expected) -> {
            executeScript("document.getElementById('pathId').setAttribute('d' , " + pathData + ");");
            // Get svg path's total length
            Double totalLength = ((Number) executeScript("document.getElementById('pathId').getTotalLength();")).doubleValue();
            final String msg = String.format(
                    "svg.path.getTotalLength() for %s",
                    pathData);
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

    /**
     * @test
     * @bug 8180825
     * Checks CSS FontFace supports ArrayBuffer and ArrayBufferView arguments.
     * This test is derived based on a DRT testcase written as part of
     * WebKit changeset https://trac.webkit.org/changeset/200921/webkit
    */
    public class FontFaceTestHelper {
        private final CountDownLatch latch = new CountDownLatch(1);

        public final byte[] ttfFileContent;

        FontFaceTestHelper(String ttfPath) throws Exception {
            final File ttfFile = new File(ttfPath);
            assertNotNull(ttfFile);
            assertTrue(ttfFile.canRead());
            assertTrue(ttfFile.length() > 0);
            final int length = (int) ttfFile.length();
            ttfFileContent = new byte[length];
            // Read ttf file contents
            int offset = 0;
            final FileInputStream ttfFileStream = new FileInputStream(ttfFile);
            assertNotNull(ttfFileContent);
            while (offset < length) {
                final int available = ttfFileStream.available();
                ttfFileStream.read(ttfFileContent, (int)offset, available);
                offset += available;
            }
            assertEquals("Offset must equal to file length", length, offset);
        }

        // Will be called from JS to indicate test complete
        public void finish() {
            latch.countDown();
        }

        private String failureMsg;
        // Will be called from JS to pass failure message
        public void failed(String msg) {
            failureMsg = msg;
        }

        void waitForCompletion() {
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new AssertionError(e);
            }

            if (failureMsg != null) {
                fail(failureMsg);
            }
        }
    }

    @Test public void testFontFace() throws Exception {
        final FontFaceTestHelper fontFaceHelper = new FontFaceTestHelper("src/main/native/Tools/TestWebKitAPI/Tests/mac/Ahem.ttf");
        loadContent(
                "<body>\n" +
                "<span id='probe1' style='font-size: 100px;'>l</span>\n" +
                "<span id='probe2' style='font-size: 100px;'>l</span>\n" +
                "</body>\n"
        );
        submit(() -> {
            final JSObject window = (JSObject) getEngine().executeScript("window");
            assertNotNull(window);
            assertEquals("undefined", window.getMember("fontFaceHelper"));
            window.setMember("fontFaceHelper", fontFaceHelper);
            assertTrue(window.getMember("fontFaceHelper") instanceof FontFaceTestHelper);
            // Create font-face object from byte[]
            getEngine().executeScript(
                "var byteArray = new Uint8Array(fontFaceHelper.ttfFileContent);\n" +
                "var arrayBuffer = byteArray.buffer;\n" +
                "window.fontFace1 = new FontFace('WebFont1', arrayBuffer, {});\n" +
                "window.fontFace2 = new FontFace('WebFont2', byteArray, {});\n"
            );
            assertEquals("loaded", getEngine().executeScript("fontFace1.status"));
            assertEquals("loaded", getEngine().executeScript("fontFace2.status"));
            // Add font-face to Document, so that it could be usable on styles
            getEngine().executeScript(
                "document.fonts.add(fontFace1);\n" +
                "document.fonts.add(fontFace2);\n" +
                "document.getElementById('probe1').style.fontFamily = 'WebFont1';\n" +
                "document.getElementById('probe2').style.fontFamily = 'WebFont2';\n"
            );

            // Ensure web font is applied by checking width property of bounding rect.
            assertEquals(100, getEngine().executeScript("document.getElementById('probe1').getBoundingClientRect().width"));
            assertEquals(100, getEngine().executeScript("document.getElementById('probe2').getBoundingClientRect().width"));
            getEngine().executeScript(
                "fontFace1.loaded.then(function() {\n" +
                "   return fontFace2.loaded;\n" +
                "}, function() {\n" +
                "   fontFaceHelper.failed(\"fontFace1's promise should be successful\");\n" +
                "   fontFaceHelper.finish();\n" +
                "}).then(function() {\n" +
                "   fontFaceHelper.finish();\n" +
                "}, function() {\n" +
                "   fontFaceHelper.failed(\"fontFace2's promise should be successful\");\n" +
                "   fontFaceHelper.finish();\n" +
                "});\n"
            );
        });
        fontFaceHelper.waitForCompletion();
    }

    /**
     * @test
     * @bug 8178360
     * Check for ICU word wrap. Compare element height which has single word vs multiline text which doesn't have
     * breakable text.
     */
    @Test public void testICUTextWrap() {
        loadContent(
        "<p id='idword'>Lorem ipsum</p>" +
        "<p id='idwrap'>Lorem​Ipsum​Dolor​Sit​Amet​Consectetur​Adipiscing​Elit​Sed​Do​Eiusmod​Tempor​Incididunt​Ut​" +
        "Labore​Et​Dolore​Magna​Aliqua​Ut​Enim​Ad​Minim​Veniam​Quis​Nostrud​Exercitation​Ullamco​Laboris​Nisi​Ut​Aliqu" +
        "ip​Ex​Ea​Commodo​Consequat​Duis​Aute​Irure​Dolor​In​Reprehenderit​In​Voluptate​Velit​Esse​Cillum​Dolore​Eu​Fug" +
        "iat​Nulla​Pariatur​Excepteur​Sint​Occaecat​Cupidatat​Non​Proident​Sunt​In​Culpa​Qui​Officia​Deserunt​Mollit" +
        "​Anim​Id​Est​Laborum</p>"
        );

        submit(()->{
            assertFalse("ICU text wrap failed ",
                (Boolean) getEngine().executeScript(
                "document.getElementById('idwrap').clientHeight == document.getElementById('idword').clientHeight"));
        });
    }

    /**
     * @test
     * @bug 8185132
     * Check window.requestAnimationFrame functionality
     */
    @Test public void testRequestAnimationFrame() {
        final CountDownLatch latch = new CountDownLatch(1);
        loadContent("hello");
        submit(() -> {
            final JSObject window =
                (JSObject) getEngine().executeScript("window");
            assertNotNull(window);
            assertNotNull(window.getMember("requestAnimationFrame"));
            window.setMember("latch", latch);
            getEngine().executeScript(
                    "window.requestAnimationFrame(function() {\n" +
                    "latch.countDown(); });");
        });

        try {
            assertTrue("No callback received from window.requestAnimationFrame",
                    latch.await(10, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * @test
     * @bug 8193207
     * Check UserAgentString for javafx runtime version and webkit version
     */
    @Test public void testUserAgentString() {
        submit(() -> {
            final String userAgentString = getEngine().getUserAgent();
            final String fxVersion = System.getProperty("javafx.runtime.version");
            final String numericStr = fxVersion.split("[^0-9]")[0];
            final String fxVersionString = "JavaFX/" + numericStr;
            assertTrue("UserAgentString does not contain " + fxVersionString, userAgentString.contains(fxVersionString));

            File webkitLicense = new File("src/main/legal/webkit.md");
            assertTrue("File does not exist: " + webkitLicense, webkitLicense.exists());

            try (final BufferedReader licenseText = new BufferedReader(new FileReader(webkitLicense))) {
                final String firstLine = licenseText.readLine().trim();
                final String webkitVersion = firstLine.substring(firstLine.lastIndexOf(" ") + 2);
                assertTrue("webkitVersion should not be empty", webkitVersion.length() > 0);
                assertTrue("UserAgentString does not contain: " + webkitVersion, userAgentString.contains(webkitVersion));
            } catch (IOException ex){
                throw new AssertionError(ex);
            }
        });
    }

    @Test public void testSVGRenderingWithGradient() {
        loadContent("<html>\n" +
                    "<body style='margin: 0px 0px;'>\n" +
                    "<svg width='400' height='150'>\n" +
                    "<defs>\n" +
                    "<linearGradient id='grad1' x1='0%' y1='0%' x2='100%' y2='100%'>\n" +
                    "<stop offset='0%' style='stop-color:red' />\n" +
                    "<stop offset='100%' style='stop-color:yellow' />\n" +
                    "</linearGradient>\n" +
                    "</defs>\n" +
                    "<rect width='400' height='150' fill='url(#grad1)' />\n" +
                    "</svg>\n" +
                    "</body>\n" +
                    "</html>");
        submit(() -> {
            final WebPage webPage = WebEngineShim.getPage(getEngine());
            assertNotNull(webPage);
            final BufferedImage img = WebPageShim.paint(webPage, 0, 0, 800, 600);
            assertNotNull(img);

            final Color pixelAt0x0 = new Color(img.getRGB(0, 0), true);
            assertTrue("Color should be opaque red:" + pixelAt0x0, isColorsSimilar(Color.RED, pixelAt0x0, 1));

            final Color pixelAt100x36 = new Color(img.getRGB(100, 36), true);
            assertTrue("Color should be almost red:" + pixelAt100x36, isColorsSimilar(Color.RED, pixelAt100x36, 40));
            assertFalse("Color shouldn't be yellow:" + pixelAt100x36, isColorsSimilar(Color.YELLOW, pixelAt100x36, 10));

            final Color pixelAt200x75 = new Color(img.getRGB(200, 75), true);
            assertFalse("Color shouldn't be red:" + pixelAt200x75, isColorsSimilar(Color.RED, pixelAt200x75, 10));
            assertTrue("Color should look like yellow:" + pixelAt200x75, isColorsSimilar(Color.YELLOW, pixelAt200x75, 40));

            final Color pixelAt399x145 = new Color(img.getRGB(399, 149), true);
            assertTrue("Color should be opaque yellow:" + pixelAt399x145, isColorsSimilar(Color.YELLOW, pixelAt399x145, 1));
        });
    }

    @Test public void testShadowDOMWithLoadContent() {
        loadContent("<html>\n" +
                    "  <body>\n" +
                    "    <template id='element-details-template'>\n" +
                    "      <style>\n" +
                    "        p { font-weight: bold; }\n" +
                    "      </style>\n" +
                    "    </template>\n" +
                    "    <element-details>\n" +
                    "    </element-details>\n" +
                    "    <script>\n" +
                    "    customElements.define('element-details',\n" +
                    "      class extends HTMLElement {\n" +
                    "        constructor() {\n" +
                    "          super();\n" +
                    "          const template = document\n" +
                    "            .getElementById('element-details-template')\n" +
                    "            .content;\n" +
                    "          const shadowRoot = this.attachShadow({mode: 'open'})\n" +
                    "            .appendChild(template.cloneNode(true));\n" +
                    "        }\n" +
                    "      })\n" +
                    "    </script>\n" +
                    "  </body>\n" +
                    "</html>");
    }

    @Test public void testWindows1251EncodingWithXML() {
        loadContent(
            "<script>\n" +
            "const text = '<?xml version=\"1.0\" encoding=\"windows-1251\"?><test/>';\n" +
            "const parser = new DOMParser();\n" +
            "window.xmlDoc = parser.parseFromString(text, 'text/xml');\n" +
            "</script>"
        );
        submit(() -> {
            // WebKit injects error message into body incase of encoding error, otherwise
            // body should be null.
            assertNull(getEngine().executeScript("window.xmlDoc.body"));
        });
    }
}
