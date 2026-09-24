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

package test.com.sun.javafx.iio;

import com.sun.javafx.iio.ImageLoadListener;
import com.sun.javafx.iio.ImageLoader;
import com.sun.javafx.iio.ImageMetadata;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.com.sun.javafx.iio.JpegTestSupport.RecordingListener;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Pins the order and content of the {@code ImageLoadListener} callbacks - metadata, warnings and
 * progress - that the JNI decoder delivers.
 * <p>
 * Warnings reach Java from two unrelated places in {@code jpegloader.c}, and only one of them is
 * libjpeg's: {@code sun_jpeg_output_message} formats a real libjpeg message and passes it as a
 * string, while the source manager calls {@code emitWarning} directly when a read or a skip comes
 * back empty. A rewrite that keeps one route and loses the other still decodes every image in the
 * corpus correctly and still fails here, which is the point.
 * <p>
 * Note which warnings can be observed at all: {@code ImageStorage} - and this test - construct the
 * loader, which parses the header, and only then attach a listener. Any warning libjpeg emits while
 * reading the header is delivered to a loader with no listeners and is dropped. That is the shipped
 * behaviour of the API, so the recorded sequences start at {@code load}.
 */
public class JpegWarningOrderTest {

    private static JpegGoldens goldens;
    private static Map<String, byte[]> corpus;

    @BeforeAll
    static void loadCorpusAndGoldens() {
        JpegNatives.require();
        goldens = JpegGoldens.load();
        corpus = JpegTestSupport.assembleCorpus(JpegGoldens.splitList(goldens.require("images")),
                JpegTestSupport::requireResource);
    }

    /**
     * The golden assertion: for every corpus member, the exact callback sequence the JNI build
     * delivered, in order, including the truncated file whose decode is the interesting one.
     */
    @Test
    void everyCorpusMemberReportsTheCallbackSequenceTheJniBuildDid() {
        List<String> mismatches = new ArrayList<>();
        for (Map.Entry<String, byte[]> member : corpus.entrySet()) {
            String key = "events." + member.getKey();
            List<String> expected = JpegGoldens.splitList(goldens.require(key));
            List<String> actual = record(member.getValue()).events();
            if (!expected.equals(actual)) {
                mismatches.add(key + ":\n      golden " + expected + "\n      now    " + actual);
            }
        }
        if (!mismatches.isEmpty()) {
            fail("the listener callbacks no longer match the JNI build:\n  "
                    + String.join("\n  ", mismatches));
        }
    }

    /**
     * A truncated JPEG - cut inside its entropy-coded data, before EOI - delivers at least one
     * warning whose message is {@code null}.
     * <p>
     * <b>This is a known bug, and it is pinned here on purpose rather than fixed.</b> When a read
     * comes back empty, {@code imageio_fill_input_buffer} reports the missing EOI with
     * <pre>
     *     (*env)-&gt;CallVoidMethod(env, reader, JPEGImageLoader_emitWarningID, READ_NO_EOI);
     * </pre>
     * where {@code READ_NO_EOI} is {@code #define READ_NO_EOI 0} - an {@code int} pushed into a
     * varargs slot that {@code emitWarning(String)} reads as a reference. On x86-64 that argument
     * register is zero, so the listener is handed a null {@code String}; {@code imageio_skip_input_data}
     * does the same thing on an empty skip. Every JavaFX application that has ever listened for JPEG
     * warnings has received these nulls.
     * <p>
     * The migration's job is to prove the new implementation behaves identically, so it has to
     * reproduce this, null and all. Fixing it here would mean the parity tests could no longer tell
     * "the rewrite is faithful" apart from "the rewrite is faithful and also changed this". The fix -
     * passing a real message such as the JDK's "Premature end of JPEG file" - is a one-line change to
     * {@code jpegloader.c} (or to whatever replaces it) and belongs in its own commit, with this
     * assertion updated in the same commit and the behaviour change called out in its message.
     */
    @Test
    void truncatedStreamDeliversTheNullWarningTheCBuildSends() {
        RecordingListener listener = new RecordingListener();
        Throwable failure = null;
        try {
            JpegTestSupport.decode(new ByteArrayInputStream(corpus.get(JpegTestSupport.TRUNCATED)),
                    JpegTestSupport.FULL, listener);
        } catch (Throwable thrown) {
            failure = thrown;
        }

        List<String> warnings = listener.warnings();
        assertFalse(warnings.isEmpty(),
                "a JPEG cut short before EOI must produce at least one warning; got none");
        assertTrue(warnings.contains(null), () -> "expected the null warning that"
                + " imageio_fill_input_buffer sends when a read comes back empty - see this test's"
                + " documentation for why the null is pinned rather than fixed - but the warnings"
                + " were " + warnings);

        // Whether the truncation also fails the load is captured, not assumed: the source manager
        // fabricates an EOI marker after the null warning, so the decode is expected to finish.
        String outcome = failure == null ? "ok" : failure.getClass().getName();
        assertEquals(goldens.require("decode." + JpegTestSupport.TRUNCATED + ".full.outcome"), outcome,
                () -> "the truncated decode ends differently than it did in the JNI build");
    }

    /**
     * The two warning routes differ in what a throwing listener can do, and the difference is the
     * JNI glue's, preserved on purpose. The missing-EOI warning - the {@code null} one - was raised by
     * the source manager with a plain {@code ExceptionCheck} afterwards, so a listener that throws on
     * it aborts the decode: the {@code emit_warning} stub stashes the exception and returns an error,
     * the library unwinds, and {@code load} surfaces it as the cause of an {@code IOException} (it is
     * unchecked, so {@code load} wraps it). The decode never reaches the end of the image.
     */
    @Test
    void aListenerThrowingOnTheNullWarningAbortsTheDecode() {
        byte[] truncated = corpus.get(JpegTestSupport.TRUNCATED);
        ThrowingListener listener = new ThrowingListener(true);

        IOException thrown = assertThrows(IOException.class,
                () -> JpegTestSupport.decode(new ByteArrayInputStream(truncated), JpegTestSupport.FULL,
                        listener),
                "a listener throwing on the missing-EOI warning must abort the decode");
        assertSame(listener.failure, thrown.getCause(), () -> "load must carry the listener's own"
                + " exception as the cause, but the cause was " + thrown.getCause());
        assertTrue(listener.threw, "the listener must have been handed the null warning");
        assertFalse(listener.progress.contains(100.0f), () -> "an aborted decode must not report"
                + " 100%, but progress was " + listener.progress);
    }

    /**
     * A libjpeg-formatted warning - "Corrupt JPEG data: premature end of data segment", which follows
     * the {@code null} one on the truncated file - went through the JNI glue's
     * {@code checkAndClearException}, which cleared and dropped whatever the listener threw. The stub
     * swallows the same way, and the library ignores its return value there, so the decode carries on
     * to 100% as if the listener had returned normally. The asymmetry is the shipped behaviour:
     * pinned, not fixed.
     */
    @Test
    void aListenerThrowingOnALibjpegWarningIsIgnored() {
        byte[] truncated = corpus.get(JpegTestSupport.TRUNCATED);
        ThrowingListener listener = new ThrowingListener(false);

        assertDoesNotThrow(() -> JpegTestSupport.decode(new ByteArrayInputStream(truncated),
                JpegTestSupport.FULL, listener),
                "a listener throwing on a libjpeg warning must not abort the decode");
        assertTrue(listener.threw, "the listener must have been handed a libjpeg warning and thrown");
        assertEquals(100.0f, listener.progress.get(listener.progress.size() - 1).floatValue(),
                () -> "the decode must run to completion regardless, but progress was "
                        + listener.progress);
    }

    /**
     * Derived invariant, independent of any golden: progress is reported as whole multiples of five,
     * strictly increasing, starting at 0 and finishing at exactly 100.
     * <p>
     * The shape comes from {@code ImageLoaderImpl.updateImageProgress}, which drops every value that
     * is not a multiple of {@code ImageTools.PROGRESS_INTERVAL} or that repeats the last one, and
     * from {@code decompressIndirect}, which reports the scanline count before each row and then
     * reports the full height once the loop ends. A rewrite that reports raw percentages, reports
     * from a different thread, or forgets the final call would still decode correct pixels and would
     * still be visible to every application with a progress bar.
     */
    @Test
    void progressIsAscendingMultiplesOfFiveEndingAtOneHundred() {
        for (Map.Entry<String, byte[]> member : corpus.entrySet()) {
            if (!"ok".equals(goldens.get("decode." + member.getKey() + ".full.outcome"))) {
                continue; // Captured as a failure; the failure is asserted by JpegDecodeParityTest.
            }
            List<Float> progress = record(member.getValue()).progress();
            assertFalse(progress.isEmpty(),
                    () -> member.getKey() + ": a successful decode reported no progress at all");
            assertEquals(0.0f, progress.get(0).floatValue(),
                    () -> member.getKey() + ": progress must start at 0, was " + progress);
            assertEquals(100.0f, progress.get(progress.size() - 1).floatValue(),
                    () -> member.getKey() + ": progress must finish at 100, was " + progress);
            for (int i = 0; i < progress.size(); i++) {
                float value = progress.get(i);
                assertEquals(0.0f, value % 5.0f,
                        () -> member.getKey() + ": " + value + " is not a multiple of 5, was "
                                + progress);
                if (i > 0) {
                    assertTrue(value > progress.get(i - 1),
                            () -> member.getKey() + ": progress went backwards or repeated: "
                                    + progress);
                }
            }
        }
    }

    /**
     * Derived invariant: metadata arrives exactly once, before anything else. {@code load} publishes
     * it before it touches the native decompressor, so a listener can size a buffer from it; a
     * rewrite that moves the call after {@code startDecompression} would break that without changing
     * a pixel.
     */
    @Test
    void metadataArrivesExactlyOnceAndFirst() {
        for (Map.Entry<String, byte[]> member : corpus.entrySet()) {
            if (!"ok".equals(goldens.get("decode." + member.getKey() + ".full.outcome"))) {
                continue;
            }
            RecordingListener listener = record(member.getValue());
            List<String> events = listener.events();
            assertEquals(1, listener.metadata().size(),
                    () -> member.getKey() + ": metadata must be delivered exactly once, events were "
                            + events);
            assertEquals("META", events.get(0),
                    () -> member.getKey() + ": metadata must be the first callback, events were "
                            + events);
        }
    }

    /** One natural-size decode with a listener attached; failures are the caller's business. */
    private static RecordingListener record(byte[] jpeg) {
        RecordingListener listener = new RecordingListener();
        try {
            JpegTestSupport.decode(new ByteArrayInputStream(jpeg), JpegTestSupport.FULL, listener);
        } catch (Throwable expected) {
            // Some corpus members are captured as failures; this method records what arrived first.
        }
        return listener;
    }

    /**
     * Throws {@link #failure} from the first warning whose message is {@code null} ({@code onNull}) or
     * is not ({@code !onNull}), and records progress so a test can tell how far the decode got.
     */
    private static final class ThrowingListener implements ImageLoadListener {

        private final boolean onNull;
        final RuntimeException failure = new IllegalStateException("listener refused the warning");
        final List<Float> progress = new ArrayList<>();
        boolean threw;

        ThrowingListener(boolean onNull) {
            this.onNull = onNull;
        }

        @Override
        public void imageLoadProgress(ImageLoader loader, float percentageComplete) {
            progress.add(percentageComplete);
        }

        @Override
        public void imageLoadWarning(ImageLoader loader, String message) {
            if ((message == null) == onNull) {
                threw = true;
                throw failure;
            }
        }

        @Override
        public void imageLoadMetaData(ImageLoader loader, ImageMetadata metadata) {
        }
    }
}
