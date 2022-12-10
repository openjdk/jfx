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

package test.memoryleak;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import junit.framework.AssertionFailedError;
import netscape.javascript.JSObject;
import test.util.Util;

public class JSCallbackMemoryTest {

    private static final String html = "<!DOCTYPE html>" +
        "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-16\">" +
        "<html>" +
        "<button id=\"mybtn1\" type=\"button\" onClick=\"callback1.jscallback1()\">Hello</button>" +
        "<button id=\"mybtn2\" type=\"button\" \">PrimitiveArray</button>" +
        "<button id=\"mybtn3\" type=\"button\" onClick=\"callback1.releaseObj()\">Release</button>" +
        "<button id=\"mybtn4\" type=\"button\" onClick=\"callback4.jsobjcallback()\">Object</button>" +
        "</html>";

    // Sleep time showing/hiding window in milliseconds
    private static final int SLEEP_TIME = 1000;

    // Sleep time before GC starts again
    private static final int GCWAIT_TIME = 100;

    // Used to launch the application before running any test
    private static final CountDownLatch launchLatch = new CountDownLatch(1);

    private static final int NUM_STAGES = 5;

    private static final int OFFSET = 30;

    private static final boolean VERBOSE = false;

    private static Stage primarystage;

    private boolean unexpectedCallback = false;

    private MyObject myObj = new MyObject();

    private Stage[] stages = new Stage[NUM_STAGES];

    private final Set<WeakReference<Object>> refs = new HashSet<>();

    private final boolean[] callbackStatus = new boolean[NUM_STAGES];

    private final WebView[] webviewArray = new WebView[NUM_STAGES];

    private final int[] primitiveArray = { 1, 2, 3, 4, 5 };

    private final Object[] objectArray = { new Object(), new Object(), new Object(), new Object() };

    private Throwable encounteredException = null;

    public final class MyObject {

        // called from JavaScript
        public void jsobjcallback() {
            if (VERBOSE) {
                System.err.println("Object...Callback from JavaScript!");
            }
            unexpectedCallback = true;
        }
    }

    public final class TestStage extends Stage {

        private int stageIndex = 0;

        TestStage(int index) {
            this.stageIndex = index;
        }

        // called from JavaScript
        public void jscallback1() {
            if (VERBOSE) {
                System.err.println("Callback from JavaScript!");
            }

            callbackStatus[stageIndex] = true;
        }

        // called from JavaScript
        public void jscallback2(int[] pArray) {
            if (VERBOSE) {
                System.err.println("Strong Ref PrimitiveArray ...Callback from JavaScript!");
            }

            assertEquals(primitiveArray.length, pArray.length);
            for (int i = 0; i < pArray.length; i++) {
                assertEquals(primitiveArray[i], pArray[i]);
            }
            callbackStatus[stageIndex] = true;
        }

        // called from JavaScript
        public void jscallback3(int[] pArray) {
            if (VERBOSE) {
                System.err.println("LocalPrimitiveArray with GC ...Callback from JavaScript!");
            }

            if (pArray != null) {
                unexpectedCallback = true;
            }
        }

        // called from JavaScript
        public void jscallback4(Object[] objArray) {
            if (VERBOSE) {
                System.err.println("Strong Ref ObjectArray...Callback from JavaScript!");
            }

            assertEquals(objectArray.length, objArray.length);
            for (int i = 0; i < objArray.length; i++) {
                assertSame(objectArray[i], objArray[i]);
            }
            callbackStatus[stageIndex] = true;
        }

        // called from JavaScript
        public void jscallback5(Object[] objArray) {
            if (VERBOSE) {
                System.err.println("LocalObjectArray with GC...Callback from JavaScript!");
            }

            if (objArray != null) {
                unexpectedCallback = true;
            }
        }

        // called from JavaScript
        public void releaseObj() {
            if (VERBOSE) {
                System.err.println("Remove myObj reference");
            }

            myObj = null;
            System.gc();
        }
    }

    @BeforeClass
    public static void doSetupOnce() throws Exception {

        Platform.setImplicitExit(false);
        Util.startup(launchLatch, () -> {
            launchLatch.countDown();
        });

        Util.runAndWait(() -> {
            primarystage = new Stage();
            primarystage.setTitle("Primary Stage");
            WebView webview = new WebView();
            Scene scene = new Scene(webview);
            scene.setFill(Color.LIGHTYELLOW);
            primarystage.setX(20);
            primarystage.setY(20);
            primarystage.setWidth(100);
            primarystage.setHeight(100);
            primarystage.setScene(scene);
            primarystage.show();
        });
    }

    @AfterClass
    public static void doTeardownOnce() {
        Util.shutdown();
    }

    @After
    public void doTeardown() {
        Util.runAndWait(() -> {
            if (stages != null) {
                for (int i = 0; i < NUM_STAGES; i++) {
                    if (stages[i].isShowing()) {
                        stages[i].hide();
                    }
                }
                stages = null;
            }
        });
    }

    private boolean isAllStagesNull() {
        for (WeakReference<Object> ref : refs) {
            if (ref.get() != null) {
                return false;
            }
        }
        return true;
    }

    private boolean isAllCallbackStatusTrue() {
        for (int i = 0; i < NUM_STAGES; i++) {
            if (callbackStatus[i] == false) {
                return false;
            }
        }
        return true;
    }

    private void checkEncounteredException() {
        if (encounteredException != null) {
            if (encounteredException instanceof Error) {
                throw (Error) encounteredException;
            } else if (encounteredException instanceof RuntimeException) {
                throw (RuntimeException) encounteredException;
            } else {
                AssertionFailedError err = new AssertionFailedError("Unknown execution exception");
                err.initCause(encounteredException.getCause());
                throw err;
            }
        }
    }

    // ========================== TEST CASES ==========================

    @Test(timeout = 20000) public void testJsCallbackLeak() throws Exception {
        final CountDownLatch latch = new CountDownLatch(NUM_STAGES);

        Util.runAndWait(() -> {

            int stagePosition = 40;
            for (int i = 0; i < NUM_STAGES; i++) {
                final Stage stage = new TestStage(i);
                stages[i] = stage;
                stage.setTitle("Stage " + i);
                WebView webview = new WebView();
                Scene scene = new Scene(webview);
                scene.setFill(Color.LIGHTYELLOW);
                stage.setX(stagePosition);
                stage.setY(stagePosition);
                stagePosition += OFFSET;
                stage.setWidth(210);
                stage.setHeight(180);
                stage.setScene(scene);

                webview.getEngine().getLoadWorker().stateProperty().addListener((ov, o, n) -> {
                    if (n == Worker.State.SUCCEEDED) {
                        try {
                            final JSObject window = (JSObject) webview.getEngine().executeScript("window");
                            assertNotNull(window);
                            window.setMember("callback1", stage);
                        } catch (Throwable ex) {
                            encounteredException = ex;
                        } finally {
                            latch.countDown();
                        }
                    }
                });

                webview.getEngine().loadContent(html);
                stage.show();
                refs.add( new WeakReference<Object>(stage));
            }
        });

        try {
            latch.await();
        } catch (InterruptedException ex) {
            throw new AssertionFailedError("Unexpected exception: " + ex);
        }

        checkEncounteredException();

        Util.runAndWait(() -> {

            for (int i = 0; i < NUM_STAGES; i++) {
                stages[i].hide();
            }
            stages = null;
        });


        for (int j = 0; j < 5; j++) {
            System.gc();

            if (isAllStagesNull()) {
                break;
            }

            Util.sleep(SLEEP_TIME);
        }

        assertTrue("All Stages are null", isAllStagesNull());
    }

    @Test(timeout = 20000) public void testJsCallbackFunction() throws Exception {
        final CountDownLatch latch = new CountDownLatch(NUM_STAGES);

        Util.runAndWait(() -> {

            int stagePosition = 40;
            for (int i = 0; i < NUM_STAGES; i++) {
                final Stage stage = new TestStage(i);
                stages[i] = stage;
                stage.setTitle("Stage " + i);
                WebView webview = new WebView();
                Scene scene = new Scene(webview);
                scene.setFill(Color.LIGHTYELLOW);
                stage.setX(stagePosition);
                stage.setY(stagePosition);
                stagePosition += OFFSET;
                stage.setWidth(210);
                stage.setHeight(180);
                stage.setScene(scene);

                webview.getEngine().getLoadWorker().stateProperty().addListener((ov, o, n) -> {
                    if (n == Worker.State.SUCCEEDED) {
                        try {
                            final JSObject window = (JSObject) webview.getEngine().executeScript("window");
                            assertNotNull(window);
                            window.setMember("callback1", stage);
                            webview.getEngine().executeScript("document.getElementById(\"mybtn1\").click()");
                        } catch (Throwable ex) {
                            encounteredException = ex;
                        } finally {
                            latch.countDown();
                        }
                    }
                });

                webview.getEngine().loadContent(html);
                stage.show();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException ex) {
            throw new AssertionFailedError("Unexpected exception: " + ex);
        }

        checkEncounteredException();

        for (int j = 0; j < 5; j++) {
            System.gc();

            if (isAllCallbackStatusTrue()) {
                break;
            }

            Util.sleep(SLEEP_TIME);
        }

        assertTrue("All Button Callback return true", isAllCallbackStatusTrue());
    }

    @Test(timeout = 20000) public void testJsCallbackReleaseFunction() throws Exception {
        final CountDownLatch latch = new CountDownLatch(NUM_STAGES);

        Util.runAndWait(() -> {

            int stagePosition = 40;
            for (int i = 0; i < NUM_STAGES; i++) {
                final Stage stage = new TestStage(i);
                stages[i] = stage;
                stage.setTitle("Stage " + i);
                WebView webview = new WebView();
                Scene scene = new Scene(webview);
                scene.setFill(Color.LIGHTYELLOW);
                stage.setX(stagePosition);
                stage.setY(stagePosition);
                stagePosition += OFFSET;
                stage.setWidth(210);
                stage.setHeight(180);
                stage.setScene(scene);

                webview.getEngine().getLoadWorker().stateProperty().addListener((ov, o, n) -> {
                    if (n == Worker.State.SUCCEEDED) {
                        try {
                            final JSObject window = (JSObject) webview.getEngine().executeScript("window");
                            assertNotNull(window);
                            window.setMember("callback1", stage);
                            window.setMember("callback4", myObj);

                            webview.getEngine().executeScript("document.getElementById(\"mybtn1\").click()");

                            // Below executeScript call will make myObj=null and GC'ed
                            webview.getEngine().executeScript("document.getElementById(\"mybtn3\").click()");

                            // Below executeScript call should not execute the JS callback (jsobjcallback) and should not cause crash as above executeScript just made myObj=null;
                            webview.getEngine().executeScript("document.getElementById(\"mybtn4\").click()");
                        } catch (Throwable ex) {
                            encounteredException = ex;
                        } finally {
                            latch.countDown();
                        }
                    }
                });

                webview.getEngine().loadContent(html);
                stage.show();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException ex) {
            throw new AssertionFailedError("Unexpected exception: " + ex);
        }

        checkEncounteredException();

        for (int j = 0; j < 5; j++) {
            System.gc();

            if (unexpectedCallback) {
                break;
            }

            Util.sleep(GCWAIT_TIME);
        }

        assertFalse(unexpectedCallback);
    }

    @Test(timeout = 20000) public void testJsCallbackConsoleFunction() throws Exception {
        final CountDownLatch latch = new CountDownLatch(NUM_STAGES);

        Util.runAndWait(() -> {

            int stagePosition = 40;
            for (int i = 0; i < NUM_STAGES; i++) {
                final Stage stage = new TestStage(i);
                stages[i] = stage;
                stage.setTitle("Stage " + i);
                WebView webview = new WebView();
                Scene scene = new Scene(webview);
                scene.setFill(Color.LIGHTYELLOW);
                stage.setX(stagePosition);
                stage.setY(stagePosition);
                stagePosition += OFFSET;
                stage.setWidth(210);
                stage.setHeight(180);
                stage.setScene(scene);

                webview.getEngine().getLoadWorker().stateProperty().addListener((ov, o, n) -> {
                    if (n == Worker.State.SUCCEEDED) {
                        try {
                            final JSObject window = (JSObject) webview.getEngine().executeScript("window");
                            assertNotNull(window);

                            window.setMember("console", new Object());
                            System.gc(); System.gc();
                            webview.getEngine().executeScript("window.console.debug = function() {}");
                        } catch (Throwable ex) {
                            encounteredException = ex;
                        } finally {
                            latch.countDown();
                        }
                    }
                });

                webview.getEngine().loadContent(html);
                stage.show();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException ex) {
            throw new AssertionFailedError("Unexpected exception: " + ex);
        }

        checkEncounteredException();

        System.gc();
    }

    @Test(timeout = 20000) public void testJsCallbackStrongRefPrimitiveArrayFunction() throws Exception {
        final CountDownLatch latch = new CountDownLatch(NUM_STAGES);

        Util.runAndWait(() -> {

            int stagePosition = 40;
            for (int i = 0; i < NUM_STAGES; i++) {
                final Stage stage = new TestStage(i);
                stages[i] = stage;
                stage.setTitle("Stage " + i);
                WebView webview = new WebView();
                Scene scene = new Scene(webview);
                scene.setFill(Color.LIGHTYELLOW);
                stage.setX(stagePosition);
                stage.setY(stagePosition);
                stagePosition += OFFSET;
                stage.setWidth(210);
                stage.setHeight(180);
                stage.setScene(scene);

                webview.getEngine().getLoadWorker().stateProperty().addListener((ov, o, n) -> {
                    if (n == Worker.State.SUCCEEDED) {
                        try {
                            final JSObject window = (JSObject) webview.getEngine().executeScript("window");
                            assertNotNull(window);
                            window.setMember("callback2", stage);
                            window.setMember("primitiveArray", primitiveArray);
                            webview.getEngine().executeScript("document.getElementById(\"mybtn2\").onclick = function() {callback2.jscallback2(primitiveArray);}");
                            webview.getEngine().executeScript("document.getElementById(\"mybtn2\").click()");
                        } catch (Throwable ex) {
                            encounteredException = ex;
                        } finally {
                            latch.countDown();
                        }
                    }
                });

                webview.getEngine().loadContent(html);
                stage.show();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException ex) {
            throw new AssertionFailedError("Unexpected exception: " + ex);
        }

        checkEncounteredException();

        for (int j = 0; j < 5; j++) {
            System.gc();

            if (isAllCallbackStatusTrue()) {
                break;
            }

            Util.sleep(GCWAIT_TIME);
        }

        assertTrue("All Button Callback return true", isAllCallbackStatusTrue());
    }

    @Test(timeout = 20000) public void testJsCallbackLocalPrimitiveArrayFunctionWithGC() throws Exception {
        final CountDownLatch latch1 = new CountDownLatch(NUM_STAGES);
        final CountDownLatch latch2 = new CountDownLatch(1);

        Util.runAndWait(() -> {

            int[] localPrimitiveArray = {1, 2, 3, 4, 5};
            int stagePosition = 40;
            for (int i = 0; i < NUM_STAGES; i++) {
                final Stage stage = new TestStage(i);
                stages[i] = stage;
                stage.setTitle("Stage " + i);
                WebView webview = new WebView();
                webviewArray[i] = webview;
                Scene scene = new Scene(webview);
                scene.setFill(Color.LIGHTYELLOW);
                stage.setX(stagePosition);
                stage.setY(stagePosition);
                stagePosition += OFFSET;
                stage.setWidth(210);
                stage.setHeight(180);
                stage.setScene(scene);

                webview.getEngine().getLoadWorker().stateProperty().addListener((ov, o, n) -> {
                    if (n == Worker.State.SUCCEEDED) {
                        try {
                            final JSObject window = (JSObject) webview.getEngine().executeScript("window");
                            assertNotNull(window);
                            window.setMember("callback2", stage);
                            window.setMember("localPrimitiveArray", new int[] { 1, 2, 3, 4, 5 });
                            System.gc(); System.gc();
                        } catch (Throwable ex) {
                            encounteredException = ex;
                        } finally {
                            latch1.countDown();
                        }
                    }
                });

                webview.getEngine().loadContent(html);
                stage.show();
            }
        });

        try {
            latch1.await();
        } catch (InterruptedException ex) {
            throw new AssertionFailedError("Unexpected exception: " + ex);
        }

        checkEncounteredException();

        Util.sleep(SLEEP_TIME);

        Util.runAndWait(() -> {

            for (int i = 0; i < NUM_STAGES; i++) {
                System.gc();
                webviewArray[i].getEngine().executeScript("document.getElementById(\"mybtn2\").onclick = function() {callback2.jscallback3(localPrimitiveArray);}");
                webviewArray[i].getEngine().executeScript("document.getElementById(\"mybtn2\").click()");
            }
            latch2.countDown();
        });

        try {
            latch2.await();
        } catch (InterruptedException ex) {
            throw new AssertionFailedError("Unexpected exception: " + ex);
        }

        assertFalse(unexpectedCallback);
    }

    @Test(timeout = 20000) public void testJsCallbackStrongRefObjectArrayFunction() throws Exception {
        final CountDownLatch latch = new CountDownLatch(NUM_STAGES);

        Util.runAndWait(() -> {

            int stagePosition = 40;
            for (int i = 0; i < NUM_STAGES; i++) {
                final Stage stage = new TestStage(i);
                stages[i] = stage;
                stage.setTitle("Stage " + i);
                WebView webview = new WebView();
                Scene scene = new Scene(webview);
                scene.setFill(Color.LIGHTYELLOW);
                stage.setX(stagePosition);
                stage.setY(stagePosition);
                stagePosition += OFFSET;
                stage.setWidth(210);
                stage.setHeight(180);
                stage.setScene(scene);

                webview.getEngine().getLoadWorker().stateProperty().addListener((ov, o, n) -> {
                    if (n == Worker.State.SUCCEEDED) {
                        try {
                            final JSObject window = (JSObject) webview.getEngine().executeScript("window");
                            assertNotNull(window);
                            window.setMember("callback2", stage);
                            window.setMember("objectArray", objectArray);
                            webview.getEngine().executeScript("document.getElementById(\"mybtn2\").onclick = function() {callback2.jscallback4(objectArray);}");
                            webview.getEngine().executeScript("document.getElementById(\"mybtn2\").click()");
                        } catch (Throwable ex) {
                            encounteredException = ex;
                        } finally {
                            latch.countDown();
                        }
                    }
                });

                webview.getEngine().loadContent(html);
                stage.show();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException ex) {
            throw new AssertionFailedError("Unexpected exception: " + ex);
        }

        checkEncounteredException();

        for (int j = 0; j < 5; j++) {
            System.gc();

            if (isAllCallbackStatusTrue()) {
                break;
            }

            Util.sleep(GCWAIT_TIME);
        }

        assertTrue("All Button Callback return true", isAllCallbackStatusTrue());

    }

    @Test(timeout = 20000) public void testJsCallbackLocalObjectArrayFunctionWithGC() throws Exception {
        final CountDownLatch latch1 = new CountDownLatch(NUM_STAGES);
        final CountDownLatch latch2 = new CountDownLatch(1);

        Util.runAndWait(() -> {

            int stagePosition = 40;
            for (int i = 0; i < NUM_STAGES; i++) {
                final Stage stage = new TestStage(i);
                stages[i] = stage;
                stage.setTitle("Stage " + i);
                WebView webview = new WebView();
                webviewArray[i] = webview;
                Scene scene = new Scene(webview);
                scene.setFill(Color.LIGHTYELLOW);
                stage.setX(stagePosition);
                stage.setY(stagePosition);
                stagePosition += OFFSET;
                stage.setWidth(210);
                stage.setHeight(180);
                stage.setScene(scene);

                webview.getEngine().getLoadWorker().stateProperty().addListener((ov, o, n) -> {
                    if (n == Worker.State.SUCCEEDED) {
                        try {
                            final JSObject window = (JSObject) webview.getEngine().executeScript("window");
                            assertNotNull(window);
                            window.setMember("callback2", stage);
                            window.setMember("localObjectArray", new Object[] { new Object(), new Object(), new Object(), new Object() });
                            System.gc(); System.gc();
                        } catch (Throwable ex) {
                            encounteredException = ex;
                        } finally {
                            latch1.countDown();
                        }
                    }
                });

                webview.getEngine().loadContent(html);
                stage.show();
            }
        });

        try {
            latch1.await();
        } catch (InterruptedException ex) {
            throw new AssertionFailedError("Unexpected exception: " + ex);
        }

        checkEncounteredException();

        Util.sleep(SLEEP_TIME);

        Util.runAndWait(() -> {

            for (int i = 0; i < NUM_STAGES; i++) {
                System.gc();
                webviewArray[i].getEngine().executeScript("document.getElementById(\"mybtn2\").onclick = function() {callback2.jscallback5(localObjectArray);}");
                webviewArray[i].getEngine().executeScript("document.getElementById(\"mybtn2\").click()");
            }
            latch2.countDown();
        });

        try {
            latch2.await();
        } catch (InterruptedException ex) {
            throw new AssertionFailedError("Unexpected exception: " + ex);
        }

        assertFalse(unexpectedCallback);
    }
}
