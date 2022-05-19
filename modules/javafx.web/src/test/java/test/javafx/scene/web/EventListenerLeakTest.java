/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.application.PlatformImpl;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import static org.junit.Assert.*;
import org.junit.Before;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

// NOTE: We cannot use TestBase since we need multiple WebView instances, and
// greater control over the lifecycle.
public class EventListenerLeakTest {

    // List of WeakReferences to EventListener objects to count which are active
    // NOTE: this must be reset for each test
    static List<WeakReference<?>> listenerRefs;

    // Save WeakReferences to WebView objects to later check that it is released
    // NOTE: this must be reset for each test
    static List<WeakReference<?>> webViewRefs;

    // WebView instances for testing
    WebView webView1;
    WebView webView2;

    // List of DOM nodes for testing
    List<EventTarget> domNodes1;
    List<EventTarget> domNodes2;

    static class MyListener implements EventListener {

        private final AtomicInteger clickCount = new AtomicInteger(0);

        private MyListener() {
        }

        int getClickCount() {
            return clickCount.get();
        }

        static MyListener create() {
            MyListener listener = new MyListener();
            listenerRefs.add(new WeakReference<>(listener));
            return listener;
        }

        @Override
        public void handleEvent(Event evt) {
            clickCount.incrementAndGet();
        }
    }

    @BeforeClass
    public static void setupOnce() throws Exception {
        final CountDownLatch startupLatch = new CountDownLatch(1);

        PlatformImpl.startup(() -> {
            startupLatch.countDown();
        });

        assertTrue("Timeout waiting for FX runtime to start",
                startupLatch.await(15, TimeUnit.SECONDS));
    }

    @AfterClass
    public static void cleanupOnce() {
        Platform.exit();
    }

    /**
     * Executes a job on FX app thread, and waits until it is complete.
     *
     * Must be called on the test thread.
     */
    void submit(Runnable job) {
        final FutureTask<Void> future = new FutureTask<>(job, null);
        Platform.runLater(future);
        try {
            // block until job is complete
            future.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            // rethrow any assertion errors as is
            if (cause instanceof AssertionError) {
                throw (AssertionError) e.getCause();
            } else if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            // any other exception should be considered a test error
            throw new AssertionError(cause);
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Loads HTML content from a String. This method does not return until
     * loading is finished.
     *
     * Must be called on the test thread.
     */
    protected void loadContent(final WebView webView, final String content) {
        final CountDownLatch loadLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            final AtomicReference<ChangeListener<Worker.State>> stateListener
                    = new AtomicReference<>();
            stateListener.set((obs, oldState, newState) -> {
                WebEngine engine = webView.getEngine();
                if (newState == Worker.State.SUCCEEDED) {
                    // Remove ChangeListener so we don't hold reference to the EventListener
                    engine.getLoadWorker().stateProperty()
                            .removeListener(stateListener.get());
                    stateListener.set(null);
                    loadLatch.countDown();
                }
            });

            webView.getEngine().getLoadWorker().stateProperty()
                    .addListener(stateListener.get());
            webView.getEngine().loadContent(content, "text/html");
        });

        try {
            assertTrue("Timeout waiting for content to load",
                    loadLatch.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException ex) {
            throw new RuntimeException("Unexpected exception", ex);
        }
    }

    /**
     * Gets the list of DOM anchor nodes.
     *
     * Must be called on the FX app thread
     */
    private List<EventTarget> getDomNodes(WebView webView) {
        final List<EventTarget> nodes = new ArrayList<>();
        Document doc = webView.getEngine().getDocument();
        assertNotNull("Document", doc);

        NodeList nodeList = doc.getElementsByTagName("a");
        assertNotNull("DOM nodes", nodeList);
        for (int i = 0; i < nodeList.getLength(); i++) {
            EventTarget node = (EventTarget) nodeList.item(i);
            nodes.add(node);
        }
        return nodes;
    }

    // Must be called on the event thread
    void click(WebView webView, int link) {
        webView.getEngine().executeScript("document.getElementById(\"link"
                + link + "\").click()");
    }

    void assertNumActive(String msg, List<WeakReference<?>> refs, int exCount)
            throws InterruptedException {

        int count = -1;

        for (int i = 0; i < 10; i++) {
            System.gc();

            count = (int) refs.stream()
                    .filter(e -> e.get() != null)
                    .count();

            if (exCount == 0 && count == 0) {
                break;
            }

            Thread.sleep(250);
        }

        assertEquals("Active references (" + msg + ")", exCount, count);
    }

    @Before
    public void initEach() {
        listenerRefs = new ArrayList<>();
        webViewRefs = new ArrayList<>();

        submit(() -> {
            webView1 = new WebView();
            webViewRefs.add(new WeakReference<>(webView1));
            webView2 = new WebView();
            webViewRefs.add(new WeakReference<>(webView2));
        });
    }

// ---------------------------------------------------------------
    private static final String HTML =
            "<body><html>" +
            "Link: <a id=\"link0\" href=click>click me 0</a><br>" +
            "Link: <a id=\"link1\" href=click>click me 1</a><br>" +
            "Link: <a id=\"link2\" href=click>click me 2</a><br>" +
            "Link: <a id=\"link3\" href=click>click me 3</a><br>" +
            "</html></body>";

    private static final String HTML2 =
            "<body><html>" +
            "Link: <a id=\"link0\" href=click>click me 0</a><br>" +
             "</html></body>";

    private static final int NUM_DOM_NODES = 4;

    /**
     * Test that the listener remains active without a strong reference to
     * either the listener or the DOM node when the WebView is active.
     */
    @Test
    public void oneWebViewSingleListenerNoRelease() throws Exception {
        webView2 = null; // unused

        // Load HTML content and get list of DOM nodes
        loadContent(webView1, HTML);

        final List<MyListener> listeners = new ArrayList<>();
        submit(() -> {
            domNodes1 = getDomNodes(webView1);
            assertEquals(NUM_DOM_NODES, domNodes1.size());

            // Create listener and attach to DOM node 0
            listeners.add(MyListener.create());
            domNodes1.get(0).addEventListener("click", listeners.get(0), false);

            // Send click event
            click(webView1, 0);
        });

        Thread.sleep(100);

        // Verify that the event is delivered to the listener
        assertEquals("Click count", 1, listeners.get(0).getClickCount());

        // Clear strong reference to listener and the DOM nodes
        listeners.clear();
        domNodes1.clear();

        // Verify that listener is still strongly held since we didn't release it
        assertNumActive("MyListener", listenerRefs, 1);
    }

    /**
     * Test that there is no leak when a listener is explicitly released.
     */
    @Test
    public void oneWebViewSingleListenerExplicitRelease() throws Exception {
        webView2 = null; // unused

        // Load HTML content and get list of DOM nodes
        loadContent(webView1, HTML);

        final List<WeakReference<MyListener>> listeners = new ArrayList<>();
        submit(() -> {
            domNodes1 = getDomNodes(webView1);
            assertEquals(NUM_DOM_NODES, domNodes1.size());

            // Create listener and attach to DOM node 0
            MyListener myListener = MyListener.create();
            listeners.add(new WeakReference<>(myListener));
            domNodes1.get(0).addEventListener("click", listeners.get(0).get(), false);

            // Send clilck event
            click(webView1, 0);
        });

        // Verify that listener has not been released
        assertNumActive("MyListener", listenerRefs, 1);

        Thread.sleep(100);

        // Verify that the event is delivered to the listener
        assertNotNull(listeners.get(0).get());
        assertEquals("Click count", 1, listeners.get(0).get().getClickCount());

        submit(() -> {
            // Remove event listener
            assertNotNull(listeners.get(0).get());
            domNodes1.get(0).removeEventListener("click", listeners.get(0).get(), false);
        });

        // Release strong reference to DOM nodes
//        listeners.clear();
        domNodes1.clear();

        // Verify that listener has been released
        assertNumActive("MyListener", listenerRefs, 0);
    }

    /**
     * Test that there is no leak when a listener is explicitly released.
     */
    @Test
    public void oneWebViewMultipleListenersExplicitRelease() throws Exception {
        webView2 = null; // unused

        // Load HTML content and get list of DOM nodes
        loadContent(webView1, HTML);

        final List<WeakReference<MyListener>> listeners = new ArrayList<>();
        submit(() -> {
            domNodes1 = getDomNodes(webView1);
            assertEquals(NUM_DOM_NODES, domNodes1.size());

            // Create listeners and attach to DOM node 0
            MyListener listenerA = MyListener.create();
            MyListener listenerB = MyListener.create();

            listeners.add(new WeakReference<>(listenerA));
            listeners.add(new WeakReference<>(listenerB));
            listeners.add(new WeakReference<>(listenerA));

            for (int i = 0; i < 3; i++) {
                domNodes1.get(i).addEventListener("click", listeners.get(i).get(), false);
            }
        });

        // Confirm that listeners(0) == listeners(2)
        assertSame(listeners.get(0).get(), listeners.get(2).get());

        // Verify that neither listener has been released
        assertNumActive("MyListener", listenerRefs, 2);
        assertNotNull(listeners.get(0).get());
        assertNotNull(listeners.get(1).get());
        assertNotNull(listeners.get(2).get());

        submit(() -> {
            // Send clilck events
            click(webView1, 0);
            click(webView1, 1);
            click(webView1, 2);
        });

        // Verify that the events are delivered to the listeners (0 and 2 are same)
        Thread.sleep(100);
        assertEquals("Click count", 2, listeners.get(0).get().getClickCount());
        assertEquals("Click count", 1, listeners.get(1).get().getClickCount());
        assertEquals("Click count", 2, listeners.get(2).get().getClickCount());

        submit(() -> {
            // Remove shared event listener from dom node 0
            assertNotNull(listeners.get(0).get());
            domNodes1.get(0).removeEventListener("click", listeners.get(0).get(), false);
            domNodes1.set(0, null);
        });

        // Verify that neither listener has been released
        assertNumActive("MyListener", listenerRefs, 2);
        assertNotNull(listeners.get(0).get());
        assertNotNull(listeners.get(1).get());
        assertNotNull(listeners.get(2).get());

        submit(() -> {
            // Send clilck events again
            click(webView1, 0);
            click(webView1, 1);
            click(webView1, 2);
        });

        // Verify that one more event is delivered to each listener (0 and 2 are same)
        Thread.sleep(100);
        assertEquals("Click count", 3, listeners.get(0).get().getClickCount());
        assertEquals("Click count", 2, listeners.get(1).get().getClickCount());
        assertEquals("Click count", 3, listeners.get(2).get().getClickCount());

        submit(() -> {
            // Remove event listener from dom node 1
            assertNotNull(listeners.get(1).get());
            domNodes1.get(1).removeEventListener("click", listeners.get(1).get(), false);
            domNodes1.set(1, null);
        });

        // Verify that only listener 1 has been released
        assertNumActive("MyListener", listenerRefs, 1);
        assertNotNull(listeners.get(0).get());
        assertNull(listeners.get(1).get());
        assertNotNull(listeners.get(2).get());

        submit(() -> {
            // Send clilck events again
            click(webView1, 0);
            click(webView1, 1);
            click(webView1, 2);
        });

        // Verify that one more event is delivered to active listener (0 and 2 are same)
        Thread.sleep(100);
        assertEquals("Click count", 4, listeners.get(0).get().getClickCount());
        assertEquals("Click count", 4, listeners.get(2).get().getClickCount());


        submit(() -> {
            // Remove event listener from dom node 2
            assertNotNull(listeners.get(2).get());
            domNodes1.get(2).removeEventListener("click", listeners.get(2).get(), false);
            domNodes1.set(2, null);
        });

        // Verify that all listners have been released
        assertNumActive("MyListener", listenerRefs, 0);
        assertNull(listeners.get(0).get());
        assertNull(listeners.get(1).get());
        assertNull(listeners.get(2).get());

        submit(() -> {
            // Send clilck events again
            click(webView1, 0);
            click(webView1, 1);
            click(webView1, 2);
        });

        // One last test of ref count after sending the clicks
        assertNumActive("MyListener", listenerRefs, 0);
    }

    /**
     * Test that a listener is implicitly released when the WebView is.
     */
    @Test
    public void oneWebViewSingleListenerImplicitRelease() throws Exception {
        webView2 = null; // unused

        // Load HTML content and get list of DOM nodes
        loadContent(webView1, HTML);

        final List<MyListener> listeners = new ArrayList<>();
        submit(() -> {
            domNodes1 = getDomNodes(webView1);
            assertEquals(NUM_DOM_NODES, domNodes1.size());

            // Create listener and attach to DOM node 0
            listeners.add(MyListener.create());
            domNodes1.get(0).addEventListener("click", listeners.get(0), false);
        });

        // Save for later
        WeakReference<MyListener> ref = new WeakReference<>(listeners.get(0));

        // Clear strong reference to listener and the DOM nodes
        listeners.clear();
        domNodes1.clear();

        // Verify that listener is still strongly held
        assertNumActive("listeners", listenerRefs, 1);

        submit(() -> {
            // Send click event
            click(webView1, 0);
        });

        Thread.sleep(100);

        // Retrieve the listener from the weak ref and check that the event
        // was delivered even though we held no reference to the event or
        // the DOM node.
        listeners.add(ref.get());
        assertNotNull(listeners.get(0));

        // Verify that the event is delivered to the listener
        assertEquals("Click count", 1, listeners.get(0).getClickCount());

        // Clear strong reference to listener and WebView
        listeners.clear();
        webView1 = null;

        // Verify that there is no strong reference to the WebView
        assertNumActive("WebView", webViewRefs, 0);

        // Verify that no listeners are strongly held
        assertNumActive("MyListener", listenerRefs, 0);
    }

    /**
     * Test that there is no leak when a listener is explicitly released in
     * one WebView, and that the listener attached to the other WebView is
     * still active.
     */
    @Test
    public void twoWebViewSingleListenerExplicitRelease() throws Exception {
        // Load HTML content and get list of DOM nodes
        loadContent(webView1, HTML);
        loadContent(webView2, HTML);

        final List<MyListener> listeners1 = new ArrayList<>();
        final List<MyListener> listeners2 = new ArrayList<>();
        submit(() -> {
            domNodes1 = getDomNodes(webView1);
            assertEquals(NUM_DOM_NODES, domNodes1.size());
            domNodes2 = getDomNodes(webView2);
            assertEquals(NUM_DOM_NODES, domNodes2.size());

            // Create listener for each WebView and attach to DOM node 0
            listeners1.add(MyListener.create());
            domNodes1.get(0).addEventListener("click", listeners1.get(0), false);

            listeners2.add(MyListener.create());
            domNodes2.get(0).addEventListener("click", listeners2.get(0), false);

            // Send clilck event to node 0 in webview1
            click(webView1, 0);
        });

        // Verify that the event is delivered to the right listener
        Thread.sleep(100);
        assertEquals("Click count", 1, listeners1.get(0).getClickCount());
        assertEquals("Click count", 0, listeners2.get(0).getClickCount());

        submit(() -> {
            // Now click the other WebView's node
            click(webView2, 0);
        });

        // Verify that the event is delivered to the right listener
        Thread.sleep(100);
        assertEquals("Click count", 1, listeners1.get(0).getClickCount());
        assertEquals("Click count", 1, listeners2.get(0).getClickCount());

        submit(() -> {
            // Remove event listener from first WebView
            domNodes1.get(0).removeEventListener("click", listeners1.get(0), false);
        });

        submit(() -> {
            // Now click both WebView's node
            click(webView1, 0);
            click(webView2, 0);
        });

        // Verify that the event is delivered to the right listener
        assertEquals("Click count", 1, listeners1.get(0).getClickCount());
        assertEquals("Click count", 2, listeners2.get(0).getClickCount());

        // Release strong reference to listener and the DOM nodes
        listeners1.clear();
        domNodes1.clear();

        // Verify that only one listener has been released
        assertNumActive("MyListener", listenerRefs, 1);

        submit(() -> {
            // Remove event listener from second WebView
            domNodes2.get(0).removeEventListener("click", listeners2.get(0), false);
        });

        submit(() -> {
            // Now click the second WebView's node again
            click(webView2, 0);
        });

        // Verify that no more events are delivered
        Thread.sleep(100);
        assertEquals("Click count", 2, listeners2.get(0).getClickCount());

        // Release strong reference to listener and the DOM nodes
        listeners2.clear();
        domNodes2.clear();

        // Verify that no listeners are strongly held
        assertNumActive("MyListener", listenerRefs, 0);
    }

    /**
     * Test checks listener is not released in
     * WebView , in case WebView load new content.
     */
    @Test
    public void TestStrongRefNewContentLoad() throws Exception {
        // Load HTML content and get list of DOM nodes
        loadContent(webView1, HTML);

        final List<MyListener> listeners1 = new ArrayList<>();
        submit(() -> {
            domNodes1 = getDomNodes(webView1);

            listeners1.add(MyListener.create());
            domNodes1.get(0).addEventListener("click", listeners1.get(0), false);

            // Send clilck event to node 0 in webview1
            click(webView1, 0);
        });

        // Verify that the event is delivered to the right listener
        Thread.sleep(100);
        assertEquals("Click count", 1, listeners1.get(0).getClickCount());

        // load new content
        loadContent(webView1, HTML2);

        // Verify that all listeners have not been released
        Thread.sleep(100);
        submit(() -> {
            // Send click event
            click(webView1, 0);
        });

        assertEquals("Click count", 1, listeners1.get(0).getClickCount());
    }
}
