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

import com.sun.javafx.iio.ImageLoader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.com.sun.javafx.iio.JpegTestSupport.Decoded;
import test.com.sun.javafx.iio.JpegTestSupport.RecordingListener;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the decoder from {@code InputStream}s that behave badly in every way the contract permits,
 * and pins what comes out.
 * <p>
 * This is the part of the decoder a migration is most likely to break. The stream is not read by
 * Java: libjpeg pulls from it through the {@code read} and {@code skip} slots of the decoder's
 * callback table, upcall stubs that call {@code InputStream.read} and {@code InputStream.skip} from
 * inside a {@code setjmp} scope, several C frames below the Java call.
 * A short read, a zero-length read, an early end of stream and a thrown {@code IOException} each
 * take a different route out of that scope, and only one of them is an error. Getting the callback
 * mechanism right but the exhaustion cases wrong produces a decoder that works on files and
 * misbehaves on sockets.
 * <p>
 * These tests need no golden file: each one derives its expectation from a decode of the same bytes
 * through a well-behaved stream, so they state a relationship ("chunking the reads does not change
 * the image") rather than a captured constant. Where the outcome is genuinely a property of the
 * current implementation rather than of the format - an empty read being treated as end of file, an
 * under-sized skip being ignored - the test says so and pins the behaviour rather than claiming it
 * is specified.
 */
public class JpegStreamCallbackTest {

    /**
     * Big enough to span several 4096-byte {@code STREAMBUF_SIZE} fills: high-quality noise, so the
     * encoder cannot compress it down to a couple of kilobytes and the exhaustion cases land in the
     * middle of the entropy-coded data rather than in the header.
     */
    private static byte[] fixture;

    /** The same bytes through a stream that does nothing unusual. */
    private static Decoded reference;

    /** The fixture with an 8 KB comment segment, the only marker that reaches {@code skip}. */
    private static byte[] withComment;

    private static Decoded commentReference;

    /** Serves the first read in full and then goes wrong; 4096 is exactly one buffer fill. */
    private static final int ONE_BUFFER = 4096;

    @BeforeAll
    static void decodeReference() throws IOException {
        JpegNatives.require();
        fixture = JpegTestSupport.writeJpeg(JpegTestSupport.noisePattern(160, 120, 20260101L),
                0.95f, false);
        assertTrue(fixture.length > 2 * ONE_BUFFER, () -> "the fixture must span several buffer"
                + " fills for these tests to mean anything; it is " + fixture.length + " bytes");
        reference = JpegTestSupport.decode(new Fixture(fixture), JpegTestSupport.FULL, null);
        withComment = JpegTestSupport.insertAfterFirstSegment(fixture,
                JpegTestSupport.commentSegment(2 * ONE_BUFFER));
        commentReference = JpegTestSupport.decode(new Fixture(withComment), JpegTestSupport.FULL,
                null);
    }

    /** A clean decode of a valid file reports no warnings at all - the baseline for the rest. */
    @Test
    void aWellBehavedStreamProducesNoWarnings() throws IOException {
        RecordingListener listener = new RecordingListener();
        JpegTestSupport.decode(new Fixture(fixture), JpegTestSupport.FULL, listener);
        assertEquals(List.of(), listener.warnings(),
                "decoding a valid JPEG from a well-behaved stream must not warn");
    }

    /**
     * A stream that never returns more than seven bytes at a time decodes to exactly the same image.
     * The C fills its buffer from whatever a single {@code read} returns and carries on, so short
     * reads are a normal event, not an error - which is what makes reading from a socket work.
     */
    @Test
    void shortReadsProduceTheSameImage() throws IOException {
        Fixture stream = new Fixture(fixture) {
            @Override
            protected int transfer(byte[] buffer, int offset, int length) {
                return super.transfer(buffer, offset, Math.min(length, 7));
            }
        };
        Decoded decoded = JpegTestSupport.decode(stream, JpegTestSupport.FULL, null);
        assertSameImage(reference, decoded, "seven-byte reads");
        // A stream that filled the 4096-byte buffer every time would be read about
        // fixture.length / 4096 times; a tenth of the file's length in calls is far past anything a
        // whole-buffer reader could produce, and leaves room for the decoder stopping at EOI rather
        // than at the last byte.
        assertTrue(stream.arrayReads > fixture.length / 10,
                () -> "the stream should have been read in small pieces, but read(byte[],int,int)"
                        + " was called only " + stream.arrayReads + " times for " + fixture.length
                        + " bytes");
    }

    /**
     * <b>Behaviour pin, not a specification.</b> {@code InputStream.read(byte[], int, int)} is
     * allowed to return 0 only when the requested length is 0, so a stream that returns 0 here is
     * misbehaving - but {@code imageio_fill_input_buffer} tests {@code ret <= 0} and treats the two
     * alike: it warns, fabricates an {@code FF D9} EOI marker, and lets libjpeg finish the image from
     * what it already has. The decode therefore succeeds, with a truncated picture and a warning,
     * rather than failing.
     * <p>
     * The consequence pinned here is that 0 and -1 are indistinguishable: cutting the same stream at
     * the same offset by either route produces byte-identical output. A rewrite that distinguishes
     * them - by looping on 0, say, which is arguably more correct - changes what applications get
     * from a slow stream, so it must be a deliberate, separate change.
     */
    @Test
    void aReadReturningZeroIsTreatedAsEndOfStream() throws IOException {
        RecordingListener afterZero = new RecordingListener();
        Decoded zeroCut = JpegTestSupport.decode(new StopsAfter(fixture, ONE_BUFFER, 0),
                JpegTestSupport.FULL, afterZero);
        RecordingListener afterEof = new RecordingListener();
        Decoded eofCut = JpegTestSupport.decode(new StopsAfter(fixture, ONE_BUFFER, -1),
                JpegTestSupport.FULL, afterEof);

        assertSameImage(zeroCut, eofCut, "a read returning 0 against a read returning -1");
        assertEquals(afterEof.warnings(), afterZero.warnings(),
                "a read returning 0 must warn exactly as an end of stream does");
        assertTrue(afterZero.warnings().contains(null), () -> "the source manager reports a missing"
                + " EOI by passing an int 0 through a String parameter, so a null warning is what"
                + " arrives; see JpegWarningOrderTest for why that is pinned rather than fixed."
                + " Warnings were " + afterZero.warnings());
    }

    /**
     * End of stream in the middle of the image is a warning, not a failure: the image comes back,
     * short of data and different from the reference, and the caller is told through the listener.
     * That is the behaviour {@code LoadCorruptJPEGTest} depends on and every truncated download
     * relies on.
     */
    @Test
    void endOfStreamPartwayThroughStillProducesAnImage() throws IOException {
        RecordingListener listener = new RecordingListener();
        Decoded decoded = JpegTestSupport.decode(new StopsAfter(fixture, ONE_BUFFER, -1),
                JpegTestSupport.FULL, listener);

        assertEquals(reference.width(), decoded.width(), "the size comes from the header, which was"
                + " read before the stream ended");
        assertEquals(reference.height(), decoded.height());
        assertEquals(reference.pixels().length, decoded.pixels().length,
                "a short stream still fills the whole buffer");
        assertFalse(Arrays.equals(reference.pixels(), decoded.pixels()),
                "losing everything after the first 4096 bytes must change the decoded pixels;"
                        + " identical output would mean the stream was not really cut");
        assertFalse(listener.warnings().isEmpty(), "a truncated stream must warn");
        assertEquals(100.0f, listener.progress().get(listener.progress().size() - 1).floatValue(),
                "the decode runs to the end of the image even though the data ran out");
    }

    /**
     * An {@code IOException} thrown while the entropy-coded data is being read comes back out of
     * {@code load} as the very same object.
     * <p>
     * The route it takes is the reason this test exists. The exception is thrown by Java code called
     * from C, inside libjpeg's {@code setjmp} scope; the {@code read} stub catches it, stashes it in
     * the loader's pending slot and returns an error; the source manager calls {@code error_exit},
     * which {@code longjmp}s out of libjpeg to the {@code setjmp} of the same {@code iio_decompress}
     * call, which reports {@code IIO_ERR_PENDING} rather than a JPEG error; {@code JPEGNative} takes
     * the stashed exception back out of the slot, and {@code JPEGImageLoader.load} rethrows it
     * unchanged. Preserving the identity - not merely the type - is what
     * {@code assertSame} is for: an implementation that wraps, replaces or reorders it would still
     * throw {@code IOException} and would still be a change.
     */
    @Test
    void anIOExceptionFromReadPropagatesUnchanged() {
        IOException failure = new IOException("stream failed mid-image");
        Fixture stream = new ThrowsAfter(fixture, ONE_BUFFER, failure);
        IOException thrown = assertThrows(IOException.class,
                () -> JpegTestSupport.decode(stream, JpegTestSupport.FULL, null),
                "a stream failure during decoding must not be swallowed");
        assertSame(failure, thrown, () -> "load must rethrow the stream's own IOException, but threw "
                + thrown + " caused by " + thrown.getCause());
    }

    /**
     * The same, one stage earlier: a failure during header parsing surfaces from the constructor,
     * because that is where {@code initDecompressor} reads. The loader is disposed on the way out,
     * so the native decompressor is not leaked - and, in particular, the JVM is still alive to run
     * the next assertion.
     */
    @Test
    void anIOExceptionFromTheFirstReadPropagatesUnchanged() {
        IOException failure = new IOException("stream failed before the header");
        Fixture stream = new ThrowsAfter(fixture, 0, failure);
        IOException thrown = assertThrows(IOException.class,
                () -> JpegTestSupport.newLoader(stream),
                "a stream failure while reading the header must not be swallowed");
        assertSame(failure, thrown, () -> "the constructor must rethrow the stream's own"
                + " IOException, but threw " + thrown + " caused by " + thrown.getCause());
    }

    /**
     * The same, at the stage in between: a failure inside {@code iio_start_decompression}. For a
     * baseline image that call reads nothing - the entropy-coded data is pulled scanline by scanline
     * from {@code iio_decompress} - so it takes a progressive image, all of whose scans libjpeg
     * consumes inside {@code jpeg_start_decompress}, to make the stream fail there. The exception
     * comes back out of {@code load} as the same object, and before any progress was reported: the
     * listener has seen the metadata and nothing else, which is how this test tells its stage apart
     * from the one two tests up.
     */
    @Test
    void anIOExceptionFromStartDecompressionPropagatesUnchanged() {
        byte[] progressive = JpegTestSupport.writeJpeg(
                JpegTestSupport.noisePattern(160, 120, 20260102L), 0.95f, true);
        int scan = JpegTestSupport.startOfScan(progressive);
        int prefix = scan + ONE_BUFFER;
        assertTrue(progressive.length > prefix + ONE_BUFFER, () -> "the progressive fixture must"
                + " carry more than a buffer of scan data past the cut for the cut to land inside"
                + " jpeg_start_decompress; it is " + progressive.length + " bytes with SOS at " + scan);
        IOException failure = new IOException("stream failed between the scans");
        Fixture stream = new ThrowsAfter(progressive, prefix, failure);
        RecordingListener listener = new RecordingListener();

        IOException thrown = assertThrows(IOException.class,
                () -> JpegTestSupport.decode(stream, JpegTestSupport.FULL, listener),
                "a stream failure while starting decompression must not be swallowed");
        assertSame(failure, thrown, () -> "load must rethrow the stream's own IOException, but threw "
                + thrown + " caused by " + thrown.getCause());
        assertEquals(List.of("META"), listener.events(), "the failure must come before the first"
                + " scanline is reported, that is, from start_decompression rather than decompress");
    }

    /**
     * <b>Behaviour pin, not a specification.</b> {@code InputStream.skip} may skip fewer bytes than
     * asked, and {@code imageio_skip_input_data} does not loop: it calls {@code skip} once and
     * accepts whatever it gets. The bytes that were not skipped are then read as if they were the
     * next marker, and libjpeg's marker scanner discards them as "extraneous bytes before marker" -
     * a warning, not an error - so the image still decodes, byte for byte, as it would have.
     * <p>
     * Reaching this path at all takes some doing: the skip has to be larger than the 4096-byte
     * buffer, and the only marker large enough that the decoder skips rather than saves is a comment,
     * so the fixture carries an 8 KB comment segment full of zeros. Zeros matter - the marker scanner
     * has to be able to discard them all without finding a stray {@code 0xFF}.
     * <p>
     * The warning libjpeg raises for those bytes ({@code JWRN_EXTRANEOUS_DATA}) cannot be observed
     * from Java, and that is pinned too. A comment sits before SOS, so it is consumed by
     * {@code jpeg_read_header} inside {@code initDecompressor} - which runs in the
     * {@code JPEGImageLoader} constructor, before any caller has had the chance to attach a listener.
     * The warning is delivered to a loader with no listeners and dropped. It has a second effect that
     * outlives it: libjpeg reports only the first warning per decompress object, so this dropped
     * warning silences every later one for this image.
     * <p>
     * A rewrite that loops until the skip is complete would be more correct and would also make the
     * extraneous bytes - and hence that whole chain - disappear. That is a behaviour change and
     * belongs in its own commit.
     */
    @Test
    void aSkipThatSkipsTooLittleIsIgnoredAndTheImageIsUnchanged() throws IOException {
        RecordingListener listener = new RecordingListener();
        ShortSkip stream = new ShortSkip(withComment, 100);
        Decoded decoded = JpegTestSupport.decode(stream, JpegTestSupport.FULL, listener);

        assertTrue(stream.skips > 0, "the fixture's comment segment should have forced a call to"
                + " InputStream.skip; without one this test proves nothing");
        assertSameImage(commentReference, decoded, "a skip that skips 100 bytes at a time");
        assertEquals(List.of(), listener.warnings(), () -> "a short skip must not change what the"
                + " application sees: the bytes the stream failed to skip are read back and"
                + " discarded by libjpeg's marker scanner while the constructor is still parsing the"
                + " header, so no listener exists to hear about it. Warnings were "
                + listener.warnings());
    }

    /**
     * The {@code skip} callback's first failure branch: {@code InputStream.skip} returns 0. The source
     * manager treats it as the stream ending ({@code ret <= 0}): it warns of the missing EOI - the
     * {@code null} warning, {@code emit_warning(NULL)} - fabricates an {@code FF D9} marker and lets
     * libjpeg finish from what it has. To watch that from a listener the skip has to happen after
     * {@code load} attached one, so the fixture is progressive with the 8 KB comment placed between its
     * first and second scans: libjpeg consumes every scan of a progressive image inside
     * {@code jpeg_start_decompress}, reaches the comment after scan one, asks the stream to skip it,
     * gets 0, sees the synthetic EOI and completes the image from the DC scan alone. The decode still
     * reports {@code P:100}; the picture is the coarse one that scan alone describes, so it differs from
     * the same bytes through a stream whose skip works - and those bytes, with the comment in place,
     * decode identically to the image without it, which is the control.
     */
    @Test
    void aSkipReturningZeroWarnsOfTheMissingEoiAndStillFinishesTheImage() throws IOException {
        byte[] progressive = JpegTestSupport.writeJpeg(
                JpegTestSupport.noisePattern(160, 120, 20260103L), 0.95f, true);
        byte[] betweenScans = insertBeforeSecondScan(progressive,
                JpegTestSupport.commentSegment(2 * ONE_BUFFER));
        Decoded progressiveReference = JpegTestSupport.decode(new Fixture(progressive),
                JpegTestSupport.FULL, null);
        Fixture control = new Fixture(betweenScans);
        assertSameImage(progressiveReference, JpegTestSupport.decode(control, JpegTestSupport.FULL, null),
                "a comment between two scans, skipped by a working skip,");
        assertEquals(1, control.skips, "the comment between the scans must reach InputStream.skip once");

        RecordingListener listener = new RecordingListener();
        ZeroSkip stream = new ZeroSkip(betweenScans);
        Decoded decoded = JpegTestSupport.decode(stream, JpegTestSupport.FULL, listener);

        assertEquals(1, stream.skips, "one skip, answered with 0; nothing is asked for after the synthetic EOI");
        assertEquals(Arrays.asList((String) null), listener.warnings(), () -> "exactly the missing-EOI"
                + " warning, which arrives as null (see JpegWarningOrderTest); warnings were "
                + listener.warnings());
        List<String> events = listener.events();
        assertEquals("META", events.get(0), "metadata first: " + events);
        assertEquals("W:" + JpegGoldens.NULL, events.get(1), "the warning comes from start_decompression,"
                + " before any scanline: " + events);
        assertEquals("P:100.0", events.get(events.size() - 1), "the decode runs to the end: " + events);
        assertTrue(events.subList(2, events.size()).stream().allMatch(event -> event.startsWith("P:")),
                "nothing but progress after the warning: " + events);
        assertEquals(progressiveReference.width(), decoded.width());
        assertEquals(progressiveReference.height(), decoded.height());
        assertEquals(progressiveReference.pixels().length, decoded.pixels().length);
        assertFalse(Arrays.equals(progressiveReference.pixels(), decoded.pixels()),
                "the image must be the DC scan alone; identical output would mean the skip was never cut");
    }

    /**
     * <b>Behaviour pin.</b> The same zero skip one stage earlier: over {@code withComment} the comment
     * precedes SOF, so the synthetic EOI arrives inside {@code jpeg_read_header}, which was asked not to
     * require an image ({@code jpeg_read_header(cinfo, FALSE)}) and answers
     * {@code JPEG_HEADER_TABLES_ONLY}. The C pushes the buffer back and hands over a decoder whose header
     * is all zero; the constructor keeps it and sets no input attributes; the missing-EOI warning went to
     * a loader that had no listener yet. What a caller sees is a loader that constructs and then cannot
     * load: {@code jpeg_read_header} reset the object with {@code jpeg_abort} on the way out, so
     * {@code jpeg_start_decompress} refuses with {@code JERR_BAD_STATE} in {@code DSTATE_START} (200).
     */
    @Test
    void aSkipReturningZeroInsideTheHeaderYieldsATablesOnlyLoader() throws IOException {
        ZeroSkip stream = new ZeroSkip(withComment);
        ImageLoader loader = JpegTestSupport.newLoader(stream);
        try {
            assertEquals(1, stream.skips, "the comment forces exactly one skip");
            RecordingListener listener = new RecordingListener();
            loader.addListener(listener);
            IOException thrown = assertThrows(IOException.class, () -> loader.load(0, 0, 0, true, true, 1, 1),
                    "a tables-only datastream has no image to load");
            assertEquals("Improper call to JPEG library in state 200", thrown.getMessage(),
                    "libjpeg's JERR_BAD_STATE for DSTATE_START, the state jpeg_abort left");
            assertEquals(List.of(), listener.warnings(),
                    "the missing-EOI warning was emitted before any listener existed, and was dropped");
        } finally {
            loader.dispose();
        }
    }

    /**
     * The {@code skip} callback's second failure branch: {@code InputStream.skip} throws. The stub catches
     * the exception, stashes it in the loader's pending slot and returns the {@code -2} sentinel
     * ({@code IIO_READ_ERROR}); the source manager aborts through {@code error_exit}, the downcall reports
     * {@code IIO_ERR_PENDING}, and the same object comes back out. Over {@code withComment} the comment
     * precedes SOS, so the skip runs inside {@code jpeg_read_header} and the exception surfaces from the
     * constructor, through {@code initDecompressor}, which disposes the loader on the way out.
     */
    @Test
    void anIOExceptionFromSkipPropagatesUnchanged() {
        IOException failure = new IOException("stream failed inside skip");
        ThrowingSkip stream = new ThrowingSkip(withComment, failure);
        IOException thrown = assertThrows(IOException.class,
                () -> JpegTestSupport.newLoader(stream),
                "a stream failure inside skip must not be swallowed");
        assertSame(failure, thrown, () -> "the constructor must rethrow the stream's own IOException,"
                + " but threw " + thrown + " caused by " + thrown.getCause());
        assertEquals(1, stream.skips, "the comment forces exactly one skip, and it is the one that threw");
    }

    /**
     * <b>Behaviour pin.</b> A stream that claims to have read more than it was asked for is clamped
     * to the buffer length rather than believed - {@code if (ret > sb->bufferLength) ret =
     * sb->bufferLength;} - so the decoder never reads past the end of its own buffer. The clamp is
     * one line, easy to lose in a rewrite, and losing it reads uninitialised memory into the image.
     */
    @Test
    void aReadThatOverstatesItsCountIsClamped() throws IOException {
        Fixture stream = new Fixture(fixture) {
            @Override
            protected int transfer(byte[] buffer, int offset, int length) {
                int count = super.transfer(buffer, offset, length);
                // Only lie about a completely filled buffer: overstating a partial read would make
                // the decoder consume stale bytes, which is a different bug from the one under test.
                return count == length ? count + 1000 : count;
            }
        };
        Decoded decoded = JpegTestSupport.decode(stream, JpegTestSupport.FULL, null);
        assertSameImage(reference, decoded, "a stream that overstates its read count");
    }

    /**
     * The decoder touches the stream through exactly two methods. It never asks how much is
     * {@code available} - a stream that returns 0 from {@code available()}, as many do, is not
     * special-cased anywhere - never uses the single-byte {@code read()}, and never closes the
     * stream: closing belongs to whoever opened it.
     */
    @Test
    void onlyBulkReadAndSkipAreEverCalled() throws IOException {
        Fixture stream = new Fixture(fixture) {
            @Override
            public int available() {
                super.available();
                return 0; // A stream that reports nothing available must still decode.
            }
        };
        JpegTestSupport.decode(stream, JpegTestSupport.FULL, null);

        assertTrue(stream.arrayReads > 0, "the image has to come from somewhere");
        assertEquals(0, stream.availableCalls, "the decoder must not consult available()");
        assertEquals(0, stream.singleByteReads, "the decoder must not use the single-byte read()");
        assertEquals(0, stream.closeCalls, "the decoder must not close a stream it did not open");
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    private static void assertSameImage(Decoded expected, Decoded actual, String what) {
        assertNotNull(actual, what + " produced no image at all");
        assertEquals(expected.type(), actual.type(), what + " changed the image type");
        assertEquals(expected.width(), actual.width(), what + " changed the width");
        assertEquals(expected.height(), actual.height(), what + " changed the height");
        assertEquals(expected.stride(), actual.stride(), what + " changed the stride");
        assertArrayEquals(expected.pixels(), actual.pixels(), what + " changed the decoded pixels");
    }

    /**
     * Inserts {@code segment} at the first marker after the first scan's entropy-coded data - between scan
     * one and scan two of a progressive image, where libjpeg reads markers again and a comment is skipped
     * through {@code skip_input_data} with a listener already attached. The entropy-coded data is walked
     * byte by byte: {@code FF 00} is a stuffed byte, {@code FF D0..D7} a restart marker, {@code FF FF} fill.
     */
    private static byte[] insertBeforeSecondScan(byte[] jpeg, byte[] segment) {
        int at = JpegTestSupport.startOfScan(jpeg);
        while (at + 1 < jpeg.length) {
            int next = jpeg[at + 1] & 0xFF;
            if ((jpeg[at] & 0xFF) == 0xFF && next != 0x00 && next != 0xFF && !(next >= 0xD0 && next <= 0xD7)) {
                break;
            }
            at++;
        }
        assertTrue(at + 1 < jpeg.length && (jpeg[at + 1] & 0xFF) != 0xD9,
                "the progressive fixture must have a second scan for the comment to sit before");
        byte[] out = new byte[jpeg.length + segment.length];
        System.arraycopy(jpeg, 0, out, 0, at);
        System.arraycopy(segment, 0, out, at, segment.length);
        System.arraycopy(jpeg, at, out, at + segment.length, jpeg.length - at);
        return out;
    }

    /**
     * A plain, correct stream over a byte array that counts what the decoder does to it. Subclasses
     * override {@link #transfer} to misbehave; overriding that rather than
     * {@code read(byte[], int, int)} keeps the counting intact.
     */
    private static class Fixture extends InputStream {

        protected final byte[] data;
        protected int position;

        int singleByteReads;
        int arrayReads;
        int skips;
        int availableCalls;
        int closeCalls;

        Fixture(byte[] data) {
            this.data = data;
        }

        @Override
        public int read() {
            singleByteReads++;
            return position < data.length ? data[position++] & 0xFF : -1;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            arrayReads++;
            return transfer(buffer, offset, length);
        }

        protected int transfer(byte[] buffer, int offset, int length) {
            if (position >= data.length) {
                return -1;
            }
            int count = Math.min(length, data.length - position);
            System.arraycopy(data, position, buffer, offset, count);
            position += count;
            return count;
        }

        @Override
        public long skip(long count) throws IOException {
            skips++;
            long skipped = Math.max(0, Math.min(count, data.length - position));
            position += (int) skipped;
            return skipped;
        }

        @Override
        public int available() {
            availableCalls++;
            return data.length - position;
        }

        @Override
        public void close() {
            closeCalls++;
        }
    }

    /** Serves {@code prefix} bytes, then returns {@code ending} - 0 or -1 - for ever. */
    private static final class StopsAfter extends Fixture {

        private final int prefix;
        private final int ending;

        StopsAfter(byte[] data, int prefix, int ending) {
            super(data);
            this.prefix = prefix;
            this.ending = ending;
        }

        @Override
        protected int transfer(byte[] buffer, int offset, int length) {
            if (position >= prefix) {
                return ending;
            }
            return super.transfer(buffer, offset, Math.min(length, prefix - position));
        }
    }

    /** Serves {@code prefix} bytes, then throws one particular exception object. */
    private static final class ThrowsAfter extends Fixture {

        private final int prefix;
        private final IOException failure;

        ThrowsAfter(byte[] data, int prefix, IOException failure) {
            super(data);
            this.prefix = prefix;
            this.failure = failure;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            arrayReads++;
            if (position >= prefix) {
                throw failure;
            }
            return transfer(buffer, offset, Math.min(length, prefix - position));
        }
    }

    /** Skips at most {@code limit} bytes per call, however many were asked for. */
    private static final class ShortSkip extends Fixture {

        private final long limit;

        ShortSkip(byte[] data, long limit) {
            super(data);
            this.limit = limit;
        }

        @Override
        public long skip(long count) throws IOException {
            return super.skip(Math.min(count, limit));
        }
    }

    /** Serves reads normally and answers every {@code skip} with 0, as a stream at its end would. */
    private static final class ZeroSkip extends Fixture {

        ZeroSkip(byte[] data) {
            super(data);
        }

        @Override
        public long skip(long count) {
            skips++;
            return 0;
        }
    }

    /** Serves reads normally and throws one particular exception object from {@code skip}. */
    private static final class ThrowingSkip extends Fixture {

        private final IOException failure;

        ThrowingSkip(byte[] data, IOException failure) {
            super(data);
            this.failure = failure;
        }

        @Override
        public long skip(long count) throws IOException {
            skips++;
            throw failure;
        }
    }
}
