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

import com.sun.javafx.application.PlatformImpl;
import com.sun.webkit.Disposer;
import com.sun.webkit.WebKitNativeShim;
import com.sun.webkit.WebPageShim;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
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
import javafx.scene.web.WebEngineShim;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@code wkj_ref} registry against the real jfxwebkit library: every id a page takes while it
 * lives is released once the page is disposed. The registry is where the FFM port keeps the Java
 * objects native code names, and unlike a JNI local reference nothing reclaims an id on return
 * from a call, so one missed release in C++ or in a facade pins its object for the life of the
 * process. {@code WebKitRegistryTest} proves the registry's own arithmetic against the wkjstub
 * library; this test proves that the library and the facades use it without leaking.
 * <p>
 * <b>A cycle</b> creates a {@link WebView}, loads a page from a file, and uses it the way an
 * application does: DOM wrappers read and change the document, two Java {@link EventListener}s
 * are added to a button, one removed again and the other left for page teardown, a Java object is
 * bound into the page and called from script with strings, numbers, a returned Java object, a Java
 * array, a throwing method and a script function, a canvas is drawn and the whole page, iframe
 * included, is painted. It then navigates to a second page, reads the back-forward list, goes
 * back, navigates away to {@code about:blank} and disposes the engine.
 * <p>
 * <b>Settling.</b> After each cycle a JVM collection lets the {@code Disposer} release the Java
 * wrappers the cycle dropped, and a full, synchronous JavaScriptCore collection on the FX thread
 * finalizes the script wrappers of Java objects, which is what releases their ids. A round does not
 * guess how long reference processing takes: it waits until the {@code Disposer} has run a record
 * the round registered itself. The registry size is then read, and the rounds repeat until three
 * reads in a row agree, that is until {@value #AGREEING_READS} reads in a row have each repeated
 * the one before. A registry that has not settled after {@value #MAX_SETTLE_ROUNDS} rounds fails
 * the test with every read it gave, rather than being measured at whatever it last read.
 * <p>
 * <b>Three intervals</b>, measured in one JVM the same way:
 * <ul>
 * <li>a burn-in of {@value #BURN_IN_CYCLES} cycles, not asserted, which absorbs the ids the first
 * pages take once for the process, such as the {@code JSObject.UNDEFINED} id that
 * {@code JNIUtility.cpp} caches on first use, and anything earlier tests in the same JVM left for
 * the {@code Disposer};
 * <li>a clean interval of {@value #MEASURED_CYCLES} cycles, asserted two-sided: the settled size
 * must end within {@value #TOLERANCE} of where it started, so that a registry still falling fails
 * as unsettled instead of hiding a leak;
 * <li>an injected interval of {@value #MEASURED_CYCLES} cycles that also registers one object per
 * cycle and never releases it, the smallest leak there is, asserted to grow past the tolerance.
 * A clean pass only counts because the same measurement demonstrably catches a leak.
 * </ul>
 * <b>The tolerance.</b> A leak of one id per page lifecycle grows the registry by
 * {@value #MEASURED_CYCLES} over the clean interval, three times the tolerance of
 * {@value #TOLERANCE}. The observed noise on Windows x64 is zero; the allowance exists because
 * JavaScriptCore scans the FX thread's stack conservatively, so a script wrapper can survive one
 * collection, and because other tests in the same JVM leave pages behind whose ids the
 * {@code Disposer} releases on its own schedule. There is no allowance for entries the library
 * keeps for the life of the process: those are taken during the burn-in and are part of the
 * baseline.
 * <p>
 * <b>Worker threads.</b> {@link #imageBitmapsClosedOnAWorkerGiveTheirIdsBack} uses the same
 * settling for ids that are released on a Web Worker thread rather than the FX thread.
 */
public class WebKitRegistryLeakTest {

    private static final int BURN_IN_CYCLES = 3;
    private static final int MEASURED_CYCLES = 6;
    private static final int TOLERANCE = 2;
    private static final int MAX_SETTLE_ROUNDS = 12;

    /** Reads in a row that must each equal the one before, so a settled size is read three times. */
    private static final int AGREEING_READS = 2;

    /**
     * The least number of ids a cycle must hold at its peak for the test to mean anything: the page,
     * its host window and frames, the bound Java object and what it returns, the listeners and the
     * history entries take about two dozen. Fewer than ten would mean the cycle no longer reaches the
     * library, and a registry that never grows trivially returns to its baseline.
     */
    private static final int MIN_IDS_IN_USE = 10;

    private static final long TIMEOUT_SECONDS = 30;
    private static final long DISPOSER_WAIT_MILLIS = 500;
    private static final long STATE_POLL_MILLIS = 20;

    /** How many bitmaps the worker test transfers, and the ids each holds while it lives. */
    private static final int WORKER_BITMAPS = 30;
    private static final int IDS_PER_BITMAP = 2;

    @TempDir
    static Path pages;

    private static String firstPage;
    private static String secondPage;
    private static String workerPage;

    @BeforeAll
    static void startPlatformAndWritePages() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        PlatformImpl.startup(started::countDown);
        assertTrue(started.await(15, TimeUnit.SECONDS), "the FX runtime did not start");
        // Starts WebKit, and with it the Invoker through which the Disposer runs its records on the
        // FX thread, before the first settle round depends on both.
        submit(() -> {
            WebEngineShim.dispose(new WebView().getEngine());
            return null;
        });
        firstPage = write("first.html", FIRST_PAGE);
        secondPage = write("second.html", SECOND_PAGE);
        write("frame.html", FRAME_PAGE);
        workerPage = write("bitmaps.html", WORKER_PAGE);
        write("bitmaps.js", WORKER_SCRIPT);
    }

    private static String write(String name, String content) throws IOException {
        Path file = pages.resolve(name);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file.toUri().toASCIIString();
    }

    @Test
    public void everyPageLifecycleReturnsTheRegistryToItsBaseline() throws Exception {
        Interval burnIn = measure(BURN_IN_CYCLES, () -> { });
        Interval clean = measure(MEASURED_CYCLES, () -> { });
        List<Long> leaked = new ArrayList<>();
        Interval injected;
        try {
            injected = measure(MEASURED_CYCLES, () -> leaked.add(WebKitNativeShim.register(new Object())));
        } finally {
            leaked.forEach(WebKitNativeShim::releaseRef);
        }
        String series = "burn-in " + burnIn + ", clean " + clean + ", injected " + injected;
        System.out.println("WebKitRegistryLeakTest: " + series);

        assertTrue(clean.peak() - clean.start() >= MIN_IDS_IN_USE,
                "a cycle held only " + (clean.peak() - clean.start()) + " registry ids at its peak,"
                        + " so it no longer exercises the library: " + series);
        assertTrue(Math.abs(clean.growth()) <= TOLERANCE,
                "the registry did not return to its baseline after " + MEASURED_CYCLES
                        + " page lifecycles: " + series);
        assertTrue(injected.growth() > TOLERANCE,
                "one unreleased id per cycle did not move the registry past the tolerance, so this"
                        + " test could not have seen a leak: " + series);
    }

    /**
     * {@code ImageBitmap}s made on the main thread, transferred to a Web Worker and closed there
     * give their ids back on the worker: {@code rq_dispose_graphics} and {@code core.release} run
     * on {@code WebCore: Worker}. The JNI build skipped both on that thread, which it had detached
     * once {@code WorkerThread::createGlobalScope} returned, and leaked each bitmap's
     * {@code RTImage} and {@code WCRenderQueueImpl}; FFM-ABI-CONTRACT.md section 13.3 records the
     * difference. The worker holds every bitmap until told to close them, so the ids they took are
     * measured before they are given back, which shows the path was taken.
     */
    @Test
    public void imageBitmapsClosedOnAWorkerGiveTheirIdsBack() throws Exception {
        AtomicReference<WebView> view = new AtomicReference<>();
        WebEngine engine = submit(() -> {
            view.set(new WebView());
            return view.get().getEngine();
        });
        try {
            navigate(engine, () -> engine.load(workerPage));
            int start = settle();
            submit(() -> engine.executeScript("transfer(" + WORKER_BITMAPS + ")"));
            awaitState(engine, "held " + WORKER_BITMAPS);
            int held = settle();
            submit(() -> engine.executeScript("closeAll()"));
            awaitState(engine, "closed " + WORKER_BITMAPS);
            int closed = settle();
            String series = start + " -> held " + held + " -> closed " + closed;
            System.out.println("WebKitRegistryLeakTest worker bitmaps: " + series);

            assertTrue(held - start >= IDS_PER_BITMAP * WORKER_BITMAPS,
                    "the bitmaps the worker holds took fewer ids than expected: " + series);
            assertTrue(Math.abs(closed - start) <= TOLERANCE,
                    "bitmaps closed on the worker did not give their ids back: " + series);
        } finally {
            navigate(engine, () -> engine.load("about:blank"));
            submit(() -> {
                WebEngineShim.dispose(engine);
                view.set(null);
                return null;
            });
        }
    }

    /* Polls the page until the worker's last report is `expected`. */
    private static void awaitState(WebEngine engine, String expected) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
        Object state = null;
        while (System.nanoTime() < deadline) {
            state = submit(() -> engine.executeScript("window.state"));
            if (expected.equals(state)) {
                return;
            }
            Thread.sleep(STATE_POLL_MILLIS);
        }
        throw new AssertionError("the worker page reported " + state + ", not " + expected);
    }

    /**
     * The settled registry size before the first cycle of an interval and after each cycle, and the
     * largest size read while a page was alive.
     */
    private record Interval(int start, List<Integer> settled, int peak) {
        int growth() {
            return settled.getLast() - start;
        }

        @Override
        public String toString() {
            return start + " -> " + settled + " (peak " + peak + ")";
        }
    }

    private Interval measure(int cycles, Runnable leak) throws Exception {
        int start = settle();
        List<Integer> settled = new ArrayList<>();
        AtomicInteger peak = new AtomicInteger(start);
        for (int i = 0; i < cycles; i++) {
            runCycle(peak);
            leak.run();
            settled.add(settle());
        }
        return new Interval(start, List.copyOf(settled), peak.get());
    }

    private void runCycle(AtomicInteger peak) throws Exception {
        AtomicReference<WebView> view = new AtomicReference<>();
        WebEngine engine = submit(() -> {
            view.set(new WebView());
            return view.get().getEngine();
        });
        navigate(engine, () -> engine.load(firstPage));
        submit(() -> {
            exerciseDom(engine);
            exerciseLiveConnect(engine);
            WebPageShim.paint(WebEngineShim.getPage(engine), 0, 0, 400, 300);
            peak.accumulateAndGet(WebKitNativeShim.registrySize(), Math::max);
            return null;
        });
        navigate(engine, () -> engine.load(secondPage));
        navigate(engine, () -> {
            WebHistory history = engine.getHistory();
            assertEquals(2, history.getEntries().size(), "the back-forward list");
            history.go(-1);
        });
        navigate(engine, () -> engine.load("about:blank"));
        submit(() -> {
            WebEngineShim.dispose(engine);
            view.set(null);
            return null;
        });
    }

    private static void exerciseDom(WebEngine engine) {
        Document document = engine.getDocument();
        NodeList items = document.getElementsByTagName("li");
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < items.getLength(); i++) {
            text.append(items.item(i).getTextContent());
        }
        assertEquals("onetwothree", text.toString());
        Element added = document.createElement("p");
        added.setAttribute("id", "added");
        added.setTextContent("added");
        document.getElementById("list").appendChild(added);
        assertEquals("added", document.getElementById("added").getTextContent());

        CountingListener removed = new CountingListener();
        CountingListener kept = new CountingListener();
        EventTarget button = (EventTarget) document.getElementById("button");
        button.addEventListener("click", removed, false);
        button.addEventListener("click", kept, false);
        engine.executeScript("document.getElementById('button').click()");
        button.removeEventListener("click", removed, false);
        engine.executeScript("document.getElementById('button').click()");
        assertEquals(1, removed.count.get(), "the listener that was removed after one click");
        assertEquals(2, kept.count.get(), "the listener left for page teardown");
    }

    private static void exerciseLiveConnect(WebEngine engine) {
        JSObject window = (JSObject) engine.executeScript("window");
        window.setMember("bridge", new Bridge());
        assertEquals("hello!,5,item,3:5,caught,42", engine.executeScript("exerciseBridge()"));
        assertEquals(170, engine.executeScript("draw()"), "the blue channel of the drawn canvas");
        JSObject list = (JSObject) engine.executeScript("document.getElementById('list')");
        assertEquals("UL", list.getMember("tagName"));
        window.removeMember("bridge");
    }

    /*
     * Runs a navigation on the FX thread and waits for the load worker to finish it. The listener
     * goes on before the navigation starts, so a load that completes at once is not missed.
     */
    private static void navigate(WebEngine engine, Runnable navigation) throws Exception {
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<Worker.State> outcome = new AtomicReference<>();
        ChangeListener<Worker.State> listener = (observable, before, after) -> {
            if (after == Worker.State.SUCCEEDED || after == Worker.State.FAILED
                    || after == Worker.State.CANCELLED) {
                outcome.set(after);
                finished.countDown();
            }
        };
        submit(() -> {
            engine.getLoadWorker().stateProperty().addListener(listener);
            navigation.run();
            return null;
        });
        assertTrue(finished.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "a navigation did not finish");
        submit(() -> {
            engine.getLoadWorker().stateProperty().removeListener(listener);
            return null;
        });
        assertEquals(Worker.State.SUCCEEDED, outcome.get(), "the outcome of a navigation");
    }

    private static int settle() throws Exception {
        List<Integer> reads = new ArrayList<>();
        int agreeing = 0;
        for (int round = 0; round < MAX_SETTLE_ROUNDS; round++) {
            collectJavaGarbage();
            submit(() -> {
                WebPageShim.collectJavaScriptGarbage();
                return null;
            });
            int read = WebKitNativeShim.registrySize();
            agreeing = !reads.isEmpty() && read == reads.getLast() ? agreeing + 1 : 0;
            reads.add(read);
            if (agreeing == AGREEING_READS) {
                return read;
            }
        }
        throw new AssertionError("the registry did not settle: " + (AGREEING_READS + 1)
                + " reads in a row never agreed in " + MAX_SETTLE_ROUNDS + " rounds, which read " + reads);
    }

    /*
     * Collects until the Disposer, which releases native peers on the FX thread, has run a record
     * registered here for an object nothing else references. What the same collection queued
     * before it has normally been released by then; a straggler is caught by the next round.
     */
    private static void collectJavaGarbage() throws InterruptedException {
        CountDownLatch disposed = new CountDownLatch(1);
        Disposer.addRecord(new Object(), disposed::countDown);
        for (int attempt = 0; attempt < MAX_SETTLE_ROUNDS; attempt++) {
            System.gc();
            if (disposed.await(DISPOSER_WAIT_MILLIS, TimeUnit.MILLISECONDS)) {
                return;
            }
        }
        throw new AssertionError("the Disposer ran nothing in " + MAX_SETTLE_ROUNDS + " collections");
    }

    private static <T> T submit(Callable<T> job) throws Exception {
        FutureTask<T> task = new FutureTask<>(job);
        Platform.runLater(task);
        try {
            return task.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof Error error) {
                throw error;
            }
            throw e;
        }
    }

    /** The Java object the page script calls. Public, because only public members are bridged. */
    public static final class Bridge {
        public String echo(String s) {
            return s + "!";
        }

        public int add(int a, int b) {
            return a + b;
        }

        public Item makeItem(String name) {
            return new Item(name);
        }

        public int[] values() {
            return new int[] {4, 5, 6};
        }

        public void fail() {
            throw new IllegalStateException("thrown on purpose, and caught by the page script");
        }

        public Object callBack(JSObject function) {
            return function.call("call", null, 21);
        }
    }

    /** A Java object handed to script by {@link Bridge#makeItem}, which script then calls. */
    public static final class Item {
        private final String name;

        Item(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private static final class CountingListener implements EventListener {
        private final AtomicInteger count = new AtomicInteger();

        @Override
        public void handleEvent(Event event) {
            count.incrementAndGet();
        }
    }

    private static final String FIRST_PAGE = """
            <!doctype html>
            <html>
            <head><title>first</title></head>
            <body style="font: 14px serif">
            <ul id="list"><li>one</li><li>two</li><li>three</li></ul>
            <button id="button">press</button>
            <canvas id="canvas" width="64" height="32"></canvas>
            <img width="4" height="4" alt="pixel" src="data:image/png;base64,\
            iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==">
            <iframe src="frame.html" width="100" height="40"></iframe>
            <script>
            function draw() {
                var g = document.getElementById('canvas').getContext('2d');
                g.fillStyle = 'rgb(0, 0, 170)';
                g.fillRect(0, 0, 64, 32);
                g.font = '12px sans-serif';
                g.fillText('registry', 2, 30);
                g.beginPath();
                g.arc(40, 16, 10, 0, Math.PI * 2);
                g.stroke();
                return g.getImageData(0, 0, 1, 1).data[2];
            }
            function exerciseBridge() {
                var parts = [];
                parts.push(bridge.echo('hello'));
                parts.push(bridge.add(2, 3));
                parts.push(bridge.makeItem('item').getName());
                var values = bridge.values();
                parts.push(values.length + ':' + values[1]);
                try {
                    bridge.fail();
                    parts.push('not thrown');
                } catch (e) {
                    parts.push('caught');
                }
                parts.push(bridge.callBack(function (v) { return v * 2; }));
                return parts.join(',');
            }
            </script>
            </body>
            </html>
            """;

    private static final String SECOND_PAGE = """
            <!doctype html>
            <html><head><title>second</title></head><body><p>second page</p></body></html>
            """;

    private static final String FRAME_PAGE = """
            <!doctype html>
            <html><head><title>frame</title></head><body><p>framed page</p></body></html>
            """;

    /*
     * transfer(n) makes n bitmaps on the main thread and transfers each to the worker, which holds
     * them until it is told to close them all. window.state is the worker's last report.
     */
    private static final String WORKER_PAGE = """
            <!doctype html>
            <html><head><title>bitmaps</title></head><body>
            <script>
            var worker = new Worker('bitmaps.js');
            window.state = 'idle';
            worker.onmessage = function (e) { window.state = e.data; };
            async function send(n) {
                for (var i = 0; i < n; i++) {
                    var bitmap = await createImageBitmap(new ImageData(64, 64));
                    worker.postMessage(bitmap, [bitmap]);
                }
                worker.postMessage('report');
            }
            function transfer(n) { window.state = 'sending'; send(n); return n; }
            function closeAll() { window.state = 'closing'; worker.postMessage('close'); return 0; }
            </script>
            </body></html>
            """;

    private static final String WORKER_SCRIPT = """
            var held = [];
            onmessage = function (e) {
                if (e.data === 'report') {
                    postMessage('held ' + held.length);
                } else if (e.data === 'close') {
                    var closed = held.length;
                    held.forEach(function (bitmap) { bitmap.close(); });
                    held = [];
                    postMessage('closed ' + closed);
                } else {
                    held.push(e.data);
                }
            };
            """;
}
