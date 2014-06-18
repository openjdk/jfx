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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.util.Callback;

import org.junit.Ignore;
import org.junit.Test;


public class CallbackTest extends TestBase {
    final static String JS_ALERT = "alert('MESSAGE');";
    final static String JS_CONFIRM = "confirm('MESSAGE');";
    final static String JS_PROMPT = "prompt('MESSAGE', 'DEFAULT');";
    final static String JS_OPEN =
            "window.open('', '', 'menubar=0, status=1, toolbar=0, resizable=1');";
    final static String JS_OPEN_DEFAULT = "window.open('');";
    final static String JS_CLOSE = "window.close();";
    final static String JS_STATUS = "window.status = 'STATUS'";

    final static String HTML_ONLOAD =
            "<html><body onload=\"ONLOAD\"></body></html>";

    final static String ALERT = "onAlert";
    final static String RESIZED = "onResized";
    final static String STATUS_CHANGED = "onStatusChanged";
    final static String VISIBILITY_CHANGED = "onVisibilityChanged";
    final static String CONFIRM = "confirmHandler";
    final static String CREATE_POPUP = "createPopupHandler";
    final static String PROMPT = "promptHandler";

    TestUI mainUi = new TestUI();
    TestUI popupUi = new TestUI();

    {
        WebEngine w = getEngine();

        w.setOnAlert(mainUi.onAlert);
        w.setOnStatusChanged(mainUi.onStatusChanged);
        w.setOnResized(mainUi.onResized);
        w.setOnVisibilityChanged(mainUi.onVisibilityChanged);

        w.setCreatePopupHandler(mainUi.createPopup);
        w.setConfirmHandler(mainUi.confirm);
        w.setPromptHandler(mainUi.prompt);
    }

    private void clear() {
        mainUi.clear();
        popupUi.clear();
    }

    @Ignore("RT-34508")
    @Test public void testDefaultPopup() {
        clear();
        executeScript(JS_OPEN_DEFAULT);
        checkDefaultPopup();

        clear();
        String html = HTML_ONLOAD.replaceAll("ONLOAD", JS_OPEN_DEFAULT);
        loadContent(html);
        checkDefaultPopup();
    }

    private void checkDefaultPopup() {
        mainUi.checkCalled(CREATE_POPUP, true, true, true, true);
        popupUi.checkCalled(RESIZED);
        popupUi.checkCalled(VISIBILITY_CHANGED, true);
    }

    @Ignore("RT-34508")
    @Test public void testCustomPopup() {
        clear();
        executeScript(JS_OPEN);
        checkCustomPopup();

        clear();
        String html = HTML_ONLOAD.replaceAll("ONLOAD", JS_OPEN);
        loadContent(html);
        checkCustomPopup();
    }

    private void checkCustomPopup() {
        mainUi.checkCalled(CREATE_POPUP, false, true, false, true);
        popupUi.checkCalled(RESIZED);
        popupUi.checkCalled(VISIBILITY_CHANGED, true);
    }
    
    // Tests that no exceptions occur when createPopupHandler is null (RT-15512).
    // Exceptions are cleared in native code and we cannot catch them at Java
    // level. So we check stderr output to detect them.
    @Test public void testNullPopupHandler() {
        PrintStream err = System.err;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        System.setErr(new PrintStream(bytes));
        
        getEngine().setCreatePopupHandler(null);
        executeScript(JS_OPEN_DEFAULT);

        System.setErr(err);
        checkErrorOutput(bytes);
    }
    
    // Tests that no exceptions occur when createPopupHandler returns null (RT-15512).
    // See comment to testNullPopupHandler().
    @Test public void testBlockingPopupHandler() {
        PrintStream err = System.err;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        System.setErr(new PrintStream(bytes));
        
        getEngine().setCreatePopupHandler(features -> null);
        executeScript(JS_OPEN_DEFAULT);

        System.setErr(err);
        checkErrorOutput(bytes);
    }
    
    private void checkErrorOutput(ByteArrayOutputStream bytes) {
        String s = bytes.toString();
        if (s.contains("Exception") || s.contains("Error")) {
            System.err.println(s);
            throw new AssertionError("Test failed, see error output");
        }
    }

    @Test public void testCloseWindow() {
        clear();
        executeScript(JS_CLOSE);
        mainUi.checkCalled(VISIBILITY_CHANGED, false);

        clear();
        String html = HTML_ONLOAD.replaceAll("ONLOAD", JS_CLOSE);
        loadContent(html);
        mainUi.checkCalled(VISIBILITY_CHANGED, false);
    }

    @Test public void testDialogs() {
        final String message = "Favorite color?";
        final String defaultValue = "0x33babe";

        clear();
        String script = JS_ALERT.replaceAll("MESSAGE", message);
        executeScript(script);
        mainUi.checkCalled(ALERT, message);

        clear();
        String html = HTML_ONLOAD.replaceAll("ONLOAD", script);
        loadContent(html);
        mainUi.checkCalled(ALERT, message);

        clear();
        script = JS_CONFIRM.replaceAll("MESSAGE", message);
        executeScript(script);
        mainUi.checkCalled(CONFIRM, message);

        clear();
        html = HTML_ONLOAD.replaceAll("ONLOAD", script);
        loadContent(html);
        mainUi.checkCalled(CONFIRM, message);

        clear();
        script = JS_PROMPT.replaceAll("MESSAGE", message)
                       .replaceAll("DEFAULT", defaultValue);
        executeScript(script);
        mainUi.checkCalled(PROMPT, message, defaultValue);

        clear();
        html = HTML_ONLOAD.replaceAll("ONLOAD", script);
        loadContent(html);
        mainUi.checkCalled(PROMPT, message, defaultValue);
    }

    @Test public void testStatus() {
        final String status = "Ready";

        clear();
        String script = JS_STATUS.replaceAll("STATUS", status);
        executeScript(script);
        mainUi.checkCalled(STATUS_CHANGED, status);

        clear();
        String html = HTML_ONLOAD.replaceAll("ONLOAD", script);
        loadContent(html);
        mainUi.checkCalled(STATUS_CHANGED, status);
    }
    
    class TestUI {
        private List<List<Object>> calls = new LinkedList<List<Object>>();

        public void clear() {
            calls.clear();
        }

        private void called(String methodName, Object... args) {
//            System.out.println("CALLED " + methodName);
//            for (Object a: args) {
//                System.out.print("" + a + ", ");
//            }
//            System.out.println();
            List<Object> call = new ArrayList<Object>(args.length + 1);
            call.add(methodName);
            Collections.addAll(call, args);
            calls.add(call);
        }

        public void checkCalled(String methodName) {
            for (List<Object> call: calls) {
                if (call.get(0).equals(methodName)) {
                    return;
                }
            }
            fail("Method " + methodName + " was not called");
        }

        public void checkCalled(String methodName, Object... args) {
            List<Object> e = new ArrayList<Object>(args.length + 1);
            e.add(methodName);
            Collections.addAll(e, args);
            for (List<Object> call: calls) {
                if (call.equals(e)) {
                    return;
                }
            }
            StringBuilder argsList = new StringBuilder();
            for (Object arg: args) {
                argsList.append(arg).append(',');
            }
            fail("Method " + methodName + " was not called with args: " + argsList);
        }

        public final EventHandler<WebEvent<String>> onAlert =
                ev -> {
                    called(ALERT, ev.getData());
                };

        public final EventHandler<WebEvent<String>> onStatusChanged =
                ev -> {
                    called(STATUS_CHANGED, ev.getData());
                };

        public final EventHandler<WebEvent<Rectangle2D>> onResized =
                ev -> {
                    Rectangle2D r = ev.getData();
                    called(RESIZED, r.getMinX(), r.getMinY(),
                                    r.getWidth(), r.getHeight());
                };

        public final EventHandler<WebEvent<Boolean>> onVisibilityChanged =
                ev -> {
                    called(VISIBILITY_CHANGED, ev.getData());
                };

        public final Callback<PopupFeatures, WebEngine> createPopup =
                f -> {
                    called(CREATE_POPUP, f.hasMenu(), f.hasStatus(),
                            f.hasToolbar(), f.isResizable());
                    WebEngine w2 = new WebEngine();
                    w2.setOnResized(popupUi.onResized);
                    w2.setOnVisibilityChanged(popupUi.onVisibilityChanged);
                    return w2;
                };

        public final Callback<String, Boolean> confirm =
                message -> {
                    called(CONFIRM, message);
                    return false;
                };

        public final Callback<PromptData, String> prompt =
                data -> {
                    called(PROMPT, data.getMessage(), data.getDefaultValue());
                    return data.getDefaultValue();
                };
    }
}

