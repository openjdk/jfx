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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A Web Worker thread calls into Java for the platform objects behind {@code Path2D} and
 * {@code FontFace}, against the real jfxwebkit library, and the calls complete.
 * <p>
 * In the JNI build that commit 939aa61ead replaced, a {@code WebCore: Worker} thread was attached
 * to the JVM only inside {@code WorkerThread::createGlobalScope}. Once the worker ran script it had
 * no {@code JNIEnv}, and {@code PathJava} and {@code FontCustomPlatformData} dereferenced the null
 * one: each case below brought the JVM down. An FFM upcall stub attaches the thread by itself, so
 * the port runs both, deliberately; FFM-ABI-CONTRACT.md section 13.3 records the difference. A
 * crash ends the forked test JVM and fails the run, so a pass means the JVM survived, and each case
 * also checks that the worker still answers afterwards.
 * <p>
 * The page and the worker script are written to a temporary directory and loaded from there, as in
 * {@code WebKitRegistryLeakTest}. The font is Ahem, the test font DumpRenderTree ships, read from
 * the module's source tree. The worker fetches it and builds the {@code FontFace} from the
 * {@code ArrayBuffer}, which WebCore loads in the constructor. FFM-STATUS.md section 3 lists the
 * worker cases that wait for a rebuilt jfxwebkit.
 */
public class WebWorkerUpcallTest extends TestBase {

    private static final Path AHEM = Path.of("src/main/native/Tools/DumpRenderTree/fonts/AHEM____.TTF");

    private static final long TIMEOUT_SECONDS = 30;
    private static final long POLL_MILLIS = 20;

    @TempDir
    static Path pages;

    private static String page;

    @BeforeAll
    static void writePages() throws IOException {
        assertTrue(Files.isRegularFile(AHEM), "the Ahem test font is missing: " + AHEM.toAbsolutePath());
        Files.copy(AHEM, pages.resolve("ahem.ttf"));
        Files.writeString(pages.resolve("worker.js"), WORKER_SCRIPT, StandardCharsets.UTF_8);
        Path file = pages.resolve("worker.html");
        Files.writeString(file, PAGE, StandardCharsets.UTF_8);
        page = file.toUri().toASCIIString();
    }

    /**
     * {@code addPath} needs the platform path of both {@code Path2D}s, so the worker reaches
     * {@code create_path} and the {@code path_} slots. A bare {@code new Path2D('M0 0 L10 10 Z')}
     * would not: it keeps a {@code PathStream}, and it did not crash under JNI.
     */
    @Test
    public void path2dAddPathRunsOnAWorker() throws InterruptedException {
        load(page);
        assertEquals("path2d ok", ask("path2d"));
        assertEquals("pong", ask("ping"), "the worker after the Path2D calls");
    }

    /**
     * A {@code FontFace} built from an {@code ArrayBuffer} loads in its constructor, through
     * {@code create_shared_buffer} and {@code create_font_custom_platform_data} on the worker.
     */
    @Test
    public void fontFaceFromAnArrayBufferLoadsOnAWorker() throws IOException, InterruptedException {
        load(page);
        assertEquals("font loaded from " + Files.size(AHEM) + " bytes", ask("font"));
        assertEquals("pong", ask("ping"), "the worker after the FontFace calls");
    }

    /* Posts a request to the worker and polls the page until the worker's reply arrives. */
    private String ask(String request) throws InterruptedException {
        executeScript("ask('" + request + "')");
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
        while (System.nanoTime() < deadline) {
            if (executeScript("window.reply") instanceof String reply) {
                return reply;
            }
            Thread.sleep(POLL_MILLIS);
        }
        throw new AssertionError("the worker did not answer " + request + " in " + TIMEOUT_SECONDS + " seconds");
    }

    /* ask(request) clears window.reply and posts the request; the worker's answer lands there. */
    private static final String PAGE = """
            <!doctype html>
            <html><head><title>worker upcalls</title></head><body>
            <script>
            var worker = new Worker('worker.js');
            window.reply = null;
            worker.onmessage = function (e) { window.reply = e.data; };
            worker.onerror = function (e) { window.reply = 'worker error: ' + e.message; };
            function ask(request) { window.reply = null; worker.postMessage(request); return request; }
            </script>
            </body></html>
            """;

    private static final String WORKER_SCRIPT = """
            onmessage = async function (e) {
                var reply;
                try {
                    if (e.data === 'path2d') {
                        var path = new Path2D();
                        path.moveTo(1, 1);
                        path.addPath(new Path2D('M0 0 L10 10 Z'));
                        reply = 'path2d ok';
                    } else if (e.data === 'font') {
                        var bytes = await (await fetch('ahem.ttf')).arrayBuffer();
                        var face = new FontFace('WorkerAhem', bytes);
                        await face.load();
                        reply = 'font ' + face.status + ' from ' + bytes.byteLength + ' bytes';
                    } else if (e.data === 'ping') {
                        reply = 'pong';
                    } else {
                        reply = 'unknown request ' + e.data;
                    }
                } catch (error) {
                    reply = e.data + ' failed: ' + error;
                }
                postMessage(reply);
            };
            """;
}
