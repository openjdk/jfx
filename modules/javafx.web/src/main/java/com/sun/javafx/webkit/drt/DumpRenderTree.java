/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.webkit.drt;

import com.sun.javafx.application.PlatformImpl;
import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.logging.PlatformLogger.Level;
import com.sun.webkit.*;
import com.sun.webkit.graphics.*;

import static com.sun.webkit.network.URLs.newURL;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Map;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import javafx.scene.web.WebEngine;

public final class DumpRenderTree {
    private final static PlatformLogger log = PlatformLogger.getLogger("DumpRenderTree");
    private final static long PID = (new Date()).getTime() & 0xFFFF;
    private final static String fileSep = System.getProperty("file.separator");
    private static boolean forceDumpAsText = false;

    final static PrintWriter out;
    static {
        try {
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                    System.out, "UTF-8")), true);
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }
    static volatile DumpRenderTree drt;

    private final WebPage webPage;
    private final UIClientImpl uiClient;
    private EventSender eventSender;

    private CountDownLatch latch;
    private Timer timer;
    private String testPath;
    private boolean loaded;
    private boolean waiting;
    private boolean complete;

    static class RenderUpdateHelper extends TimerTask {
        private WebPage webPage;

        public RenderUpdateHelper(WebPage webPage) {
            this.webPage = webPage;
        }

        @Override
        public void run() {
            Invoker.getInvoker().invokeOnEventThread(() -> {
                    webPage.forceRepaint();
            });
        }
    }

    static class ThemeClientImplStub extends ThemeClient {
        @Override
        protected RenderTheme createRenderTheme() {
            return new RenderThemeStub();
        }

        @Override
        protected ScrollBarTheme createScrollBarTheme() {
            return new ScrollBarThemeStub();
        }
    }

    static class RenderThemeStub extends RenderTheme {
        @Override
        protected Ref createWidget(long id, int widgetIndex, int state, int w, int h, int bgColor, ByteBuffer extParams) {
            return null;
        }

        @Override
        public void drawWidget(WCGraphicsContext g, Ref widget, int x, int y) {
        }

        @Override
        protected int getRadioButtonSize() {
            return 0;
        }

        @Override
        protected int getSelectionColor(int index) {
            return 0;
        }

        @Override
        public WCSize getWidgetSize(Ref widget) {
            return new WCSize(0, 0);
        }
    }

    static class ScrollBarThemeStub extends ScrollBarTheme {
        @Override
        protected Ref createWidget(long id, int w, int h, int orientation, int value, int visibleSize, int totalSize) {
            return null;
        }

        @Override
        protected void getScrollBarPartRect(long id, int part, int rect[]) {}

        @Override
        public void paint(WCGraphicsContext g, Ref sbRef, int x, int y, int pressedPart, int hoveredPart) {
        }

        @Override
        public WCSize getWidgetSize(Ref widget) {
            return new WCSize(0, 0);
        }
    }

    // called on FX thread
    private DumpRenderTree() {
        uiClient = new UIClientImpl();
        webPage = new WebPage(new WebPageClientImpl(), uiClient, null, null,
                              new ThemeClientImplStub(), false);
        uiClient.setWebPage(webPage);

        webPage.setBounds(0, 0, 800, 600);
        webPage.setDeveloperExtrasEnabled(true);
        webPage.addLoadListenerClient(new DRTLoadListener());

    }

    private String getTestPath(String testString) {
        int t = testString.indexOf("'");
        String pixelsHash = "";
        if ((t > 0) && (t < testString.length() - 1)) {
            pixelsHash = testString.substring(t + 1);
            testString = testString.substring(0, t);
        }
        this.testPath = testString;
        initTest(testString, pixelsHash);
        return testString;
    }

    protected String getTestURL() {
        return testPath;
    }

/*
    private static boolean isDebug()
    {
        return log.isLoggable(Level.FINE);
    }
*/

    private static void mlog(String msg) {
        if (log.isLoggable(Level.FINE)) {
            log.fine("PID:" + Long.toHexString(PID)
                    + " TID:" + Thread.currentThread().getId()
                        + "(" + Thread.currentThread().getName() + ") "
                    + msg);
        }
    }

    private static void initPlatform() throws Exception {
        // initialize default toolkit
        final CountDownLatch latch = new CountDownLatch(1);
        PlatformImpl.startup(() -> {
            // initialize Webkit classes
            try {
                Class.forName(WebEngine.class.getName());
                Class.forName(WebPage.class.getName());
            } catch (Exception e) {}

            System.loadLibrary("DumpRenderTreeJava");
            initDRT();
            new WebEngine();
            drt = new DumpRenderTree();
            PageCache.setCapacity(1);
            latch.countDown();
        });
        // wait for libraries to load
        latch.await();
    }

    boolean complete() { return this.complete; }

    private void resetToConsistentStateBeforeTesting(final TestOptions options) {
        // Reset native objects associated with WebPage
        webPage.resetToConsistentStateBeforeTesting();

        // Assign default values for all supported TestOptions
        webPage.overridePreference("experimental:CSSCustomPropertiesAndValuesEnabled", "false");
        webPage.overridePreference("enableColorFilter", "false");
        webPage.overridePreference("enableIntersectionObserver", "false");
        // Enable features based on TestOption
        for (Map.Entry<String, String> option : options.getOptions().entrySet()) {
            webPage.overridePreference(option.getKey(), option.getValue());
        }
    }

    private void reset(final TestOptions options) {
        mlog("reset");
        // newly create EventSender for each test
        eventSender = new EventSender(webPage);
        resetToConsistentStateBeforeTesting(options);
        // Clear frame name
        webPage.reset(webPage.getMainFrame());
        // Reset zoom factors
        webPage.setZoomFactor(1.0f, true);
        webPage.setZoomFactor(1.0f, false);
        // Reset DRT internal states
        complete = false;
        loaded = false;
        waiting = false;
    }

    // called on FX thread
    private void run(final String testString, final CountDownLatch latch) {
        this.latch = latch;
        String file = getTestPath(testString);
        mlog("{runTest: " + file);
        long mainFrame = webPage.getMainFrame();
        try {
            new URL(file);
        } catch (MalformedURLException ex) {
            file = "file:///" + file;
        }
        // parse test options from the html test header
        final TestOptions options = new TestOptions(file);
        reset(options);
        webPage.open(mainFrame, file);
        mlog("}runTest");
    }

    private void runTest(final String testString) throws Exception {
        final CountDownLatch l = new CountDownLatch(1);
        Invoker.getInvoker().invokeOnEventThread(() -> {
            run(testString, l);
        });

        timer = new Timer();
        TimerTask task = new RenderUpdateHelper(webPage);
        timer.schedule(task, 1000/60, 1000/60);
        // wait until test is finished
        l.await();
        task.cancel();
        timer.cancel();
        final CountDownLatch latchForEvents = new CountDownLatch(1);
        Invoker.getInvoker().invokeOnEventThread(() -> {
            mlog("dispose");
            webPage.stop();
            dispose();
            latchForEvents.countDown();
        });
        latchForEvents.await();
    }

    // called from native
    private static void waitUntilDone() {
        mlog("waitUntilDone");
        drt.setWaiting(true); // TODO: handle timeout
    }

    // called from native
    private static void notifyDone() {
        mlog("notifyDone");
        drt.setWaiting(false);
    }

    private static void overridePreference(String key, String value) {
        mlog("overridePreference");
        drt.webPage.overridePreference(key, value);
    }

    private synchronized void setLoaded(boolean loaded) {
        this.loaded = loaded;
        done();
    }

    private synchronized void setWaiting(boolean waiting) {
        this.waiting = waiting;
        done();
    }

    private synchronized StringBuilder dumpFramesAsText(long frame) {
        StringBuilder str = new StringBuilder();
        String innerText = webPage.getInnerText(frame);
        if (frame == webPage.getMainFrame()) {
            if (innerText != null) {
                // don't use println() here as it varies from platform
                // to platform, but DRT expects it always to be 0x0A
                str.append(innerText + '\n');
            }
        } else {
            str.append("\n--------\nFrame: '");
            str.append(webPage.getName(frame));
            str.append("'\n--------\n");
            str.append(innerText + "\n");
        }
        if (dumpChildFramesAsText()) {
            List<Long> children = webPage.getChildFrames(frame);
            if (children != null) {
                for (long child : children) {
                    str.append(dumpFramesAsText(child));
                }
            }
        }

        // To keep things tidy, strip all trailing spaces: they are not a meaningful part of dumpAsText test output.
        int spacePosition;
        while ((spacePosition = str.indexOf(" \n")) != -1)
            str.deleteCharAt(spacePosition);
        while (str.length() != 0 && str.charAt(str.length() - 1) == ' ')
            str.deleteCharAt(str.length() - 1);

        return str;
    }

    private synchronized void dump(long frame) {
        boolean dumpAsText = dumpAsText() || forceDumpAsText;
        mlog("dumpAsText = " + dumpAsText);
        if (dumpAsText) {
            out.print(dumpFramesAsText(frame));
            if (dumpBackForwardList() && frame == webPage.getMainFrame()) {
                drt.dumpBfl();
            }
        } else {
            String renderTree = webPage.getRenderTree(frame);
            out.print(renderTree);
        }
    }

    private synchronized void done() {
        if (waiting || !loaded || complete) {
            return;
        }
        mlog("dump");
        dump(webPage.getMainFrame());

        mlog("done");
        out.print("#EOF" + '\n');
        // TODO: dump pixels here
        out.print("#EOF" + '\n');
        out.flush();

        System.err.print("#EOF" + '\n');
        System.err.flush();

        complete = true;
        // notify main thread that test is finished
        this.latch.countDown();
    }

    private static native void initDRT();
    private static native void initTest(String testPath, String pixelsHash);
    private static native void didClearWindowObject(long pContext,
            long pWindowObject, EventSender eventSender);
    private static native void dispose();

    private static native boolean dumpAsText();
    private static native boolean dumpChildFramesAsText();
    private static native boolean dumpBackForwardList();
    protected static native boolean shouldStayOnPageAfterHandlingBeforeUnload();
    protected static native String[] openPanelFiles();

    private final class DRTLoadListener implements LoadListenerClient {
        @Override
        public void dispatchLoadEvent(long frame, int state,
                                      String url, String contentType,
                                      double progress, int errorCode)
        {
            mlog("dispatchLoadEvent: ENTER");
            if (frame == webPage.getMainFrame()) {
                mlog("dispatchLoadEvent: STATE = " + state);
                switch (state) {
                    case PAGE_STARTED:
                        mlog("PAGE_STARTED");
                        setLoaded(false);
                        break;
                    case PAGE_FINISHED:
                        mlog("PAGE_FINISHED");
                        if (didFinishLoad()) {
                            setLoaded(true);
                        }
                        break;
                    case DOCUMENT_AVAILABLE:
                        dumpUnloadListeners(webPage, frame);
                        break;
                    case LOAD_FAILED:
                        mlog("LOAD_FAILED");
                        // safety net: if load fails, e.g. command line
                        // parameters were bad, let's not hang forever
                        setLoaded(true);
                        break;
                }
            }
            mlog("dispatchLoadEvent: EXIT");
        }
        @Override
        public void dispatchResourceLoadEvent(long frame, int state,
                                              String url, String contentType,
                                              double progress, int errorCode)
        {
        }
    }


    public static void main(final String[] args) throws Exception {
/*
        if ( isDebug() ) {
            // 'log' here is from java.util.logging
            log.setLevel(Level.FINEST);
            FileHandler handler = new FileHandler("drt.log", true);
            handler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    return formatMessage(record) + "\n";
                }
            });
            log.addHandler(handler);
        }
*/
        mlog("{main");
        initPlatform();
        assert drt != null;
        for (String arg: args) {
            if ("--dump-as-text".equals(arg)) {
                forceDumpAsText = true;
            } else if ("-".equals(arg)) {
                // read from stdin
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(System.in));
                String testPath;
                while ((testPath = in.readLine()) != null) {
                    drt.runTest(testPath);
                }
                in.close();
            } else {
                drt.runTest(arg);
            }
        }
        PlatformImpl.exit();
        mlog("}main");
        System.exit(0); // workaround to kill media threads
    }

    // called from native
    private static int getWorkerThreadCount() {
        return WebPage.getWorkerThreadCount();
    }

    // called from native
    private static String resolveURL(String relativeURL) {
        String testDir = new File(drt.testPath).getParentFile().getPath();
        File f = new File(testDir, relativeURL);
        String url = "file:///" + f.toString().replace(fileSep, "/");
        mlog("resolveURL: " + url);
        return url;
    }

    // called from native
    private static void loadURL(String url) {
        drt.webPage.open(drt.webPage.getMainFrame(), url);
    }

    // called from native
    private static void goBackForward(int dist) {
        // TODO: honor the dist
        if (dist > 0) {
            drt.webPage.goForward();
        } else {
            drt.webPage.goBack();
        }
    }

    // called from native
    private static int getBackForwardItemCount() {
        return drt.getBackForwardList().size();
    }

    // called from native
    private static void clearBackForwardList() {
        drt.getBackForwardList().clearBackForwardListForDRT();
    }

    private static final String TEST_DIR_NAME = "LayoutTests";
    private static final int TEST_DIR_LEN = TEST_DIR_NAME.length();
    private static final String CUR_ITEM_STR = "curr->";
    private static final int CUR_ITEM_STR_LEN = CUR_ITEM_STR.length();
    private static final String INDENT = "    ";

    private BackForwardList bfl;
    private BackForwardList getBackForwardList() {
        if (bfl == null) {
            bfl = webPage.createBackForwardList();
        }
        return bfl;
    }

    private void dumpBfl() {
        out.print("\n============== Back Forward List ==============\n");
        getBackForwardList();
        BackForwardList.Entry curItem = bfl.getCurrentEntry();
        for (BackForwardList.Entry e: bfl.toArray()) {
            dumpBflItem(e, 2, e == curItem);
        }
        out.print("===============================================\n");
    }

    private void dumpBflItem(BackForwardList.Entry item, int indent, boolean isCurrent) {
        StringBuilder str = new StringBuilder();
        for (int i = indent; i > 0; i--) str.append(INDENT);

        if (isCurrent) str.replace(0, CUR_ITEM_STR_LEN, CUR_ITEM_STR);

        String url = item.getURL().toString();
        if (url.contains("file:/")) {
            String subUrl = url.substring(url.indexOf(TEST_DIR_NAME) + TEST_DIR_LEN + 1);
            str.append("(file test):" + subUrl);
        } else {
            str.append(url);
        }
        if (item.getTarget() != null) {
            str.append(" (in frame \"" + item.getTarget() + "\")");
        }
        if (item.isTargetItem()) {
            str.append("  **nav target**\n");
        } else {
            str.append("\n");
        }
        out.print(str);
        if (item.getChildren() != null)
            for (BackForwardList.Entry child: item.getChildren())
                dumpBflItem(child, indent + 1, false);
    }

    void dumpUnloadListeners(WebPage page, long frame) {
        if (waiting == true && dumpAsText()) {
            String dump = getUnloadListenersDescription(page, frame);
            if (dump != null) {
                out.print(dump + '\n');
            }
        }
    }

    private static String getUnloadListenersDescription(WebPage page, long frame) {
        int count = page.getUnloadEventListenersCount(frame);
        if (count > 0) {
            return getFrameDescription(page, frame) +
                   " - has " + count + " onunload handler(s)";
        }
        return null;
    }

    private static String getFrameDescription(WebPage page, long frame) {
        String name = page.getName(frame);
        if (frame == page.getMainFrame()) {
            return name == null ? "main frame" : "main frame " + name;
        }
        return name == null ? "frame (anonymous)" : "frame " + name;
    }

    private native static boolean didFinishLoad();

    private final class WebPageClientImpl implements WebPageClient<Void> {

        @Override
        public void setCursor(long cursorID) {
        }

        @Override
        public void setFocus(boolean focus) {
        }

        @Override
        public void transferFocus(boolean forward) {
        }

        @Override
        public void setTooltip(String tooltip) {
        }

        @Override
        public WCRectangle getScreenBounds(boolean available) {
            return new WCRectangle(0, 0, 800, 600);
        }

        @Override
        public int getScreenDepth() {
            return 24;
        }

        @Override
        public Void getContainer() {
            return null;
        }

        @Override
        public WCPoint screenToWindow(WCPoint ptScreen) {
            return ptScreen;
        }

        @Override
        public WCPoint windowToScreen(WCPoint ptWindow) {
            return ptWindow;
        }

        @Override
        public WCPageBackBuffer createBackBuffer() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isBackBufferSupported() {
            return false;
        }

        @Override
        public void addMessageToConsole(String message, int lineNumber,
                                        String sourceId)
        {
            if (complete) {
                return;
            }
            if (!message.isEmpty()) {
                int pos = message.indexOf("file://");
                if (pos != -1) {
                    String s1 = message.substring(0, pos);
                    String s2 = message.substring(pos);
                    try {
                        // Extract the last path component aka file name
                        s2 = new File(newURL(s2).getPath()).getName();
                    } catch (MalformedURLException ignore) {}
                    message = s1 + s2;
                }
            }
            out.printf("CONSOLE MESSAGE:%s\n",
                (message.isEmpty() || message.startsWith("\n")) ? message : " " + message);
        }

        @Override
        public void didClearWindowObject(long context, long windowObject) {
            mlog("didClearWindowObject");
            if (eventSender != null) {
                DumpRenderTree.didClearWindowObject(context, windowObject,
                                                    eventSender);
            }
        }
    }
}
