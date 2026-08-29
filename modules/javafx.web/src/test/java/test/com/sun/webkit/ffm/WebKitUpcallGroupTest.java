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

package test.com.sun.webkit.ffm;

import com.sun.webkit.ContextMenuItem;
import com.sun.webkit.Invoker;
import com.sun.webkit.WebKitNativeShim;
import com.sun.webkit.WkjStubShim;
import com.sun.webkit.graphics.ScrollBarTheme;
import com.sun.webkit.graphics.WCImageDecoder;
import com.sun.webkit.graphics.WCImageFrame;
import com.sun.webkit.graphics.WCTextRun;
import java.io.File;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The seven {@code WKJHost} groups that were placeholders on the Java side and real in C -
 * {@code graphics}, {@code network}, {@code media}, {@code filesystem}, {@code theme},
 * {@code wtf} and {@code pal} - driven through the production table by the library itself.
 * <p>
 * {@link WebKitLayoutTest} proves the table has the right shape and {@link WebKitHostInstallTest}
 * proves every slot carries a stub. Neither proves that a stub reaches the Java method it names, or
 * that the {@link java.lang.foreign.FunctionDescriptor} it was created with matches the C prototype
 * - a descriptor that disagrees corrupts the stack rather than failing. This class calls the slots
 * the way the library calls them, through {@code wkjstub_fire_host_slot}, whose typed dispatch is
 * generated from the C header, and checks the answer against what the Java target should have said.
 * <p>
 * The slots exercised here are the ones whose Java target needs neither a live WebKit nor a JavaFX
 * toolkit. That is a real subset rather than a token one: every file system slot, every digest
 * slot, the whole {@code WCTextRun} and {@code WCImageDecoder} families, the context menu item
 * setters and the round trip of the main thread dispatch. Between them they cover every shape the
 * other filled slots are variations on - a target ref, an {@code int[]} or {@code float[]} out
 * parameter, a byte array in, a caller-provided string buffer, a {@code wkj_ref} return that mints
 * a new id, and a callback with no target at all.
 */
@Tag("ffm")
public class WebKitUpcallGroupTest {

    /** The result codes of {@code wkjstub_fire_host_slot}. */
    private static final int FIRED = 0;

    /** {@code WKJ_STR_OK} and {@code WKJ_STR_NULL} of contract 13. */
    private static final int STR_OK = 0;
    private static final int STR_NULL = 1;

    private static Invoker savedInvoker;

    @TempDir
    Path tempDir;

    @BeforeAll
    static void loadLibrary() {
        assumeTrue(WkjStubShim.load(), WkjStubShim.loadFailure());
        savedInvoker = Invoker.getInvoker();
    }

    @AfterAll
    static void restoreTheProcessState() {
        Invoker.setInvoker(savedInvoker);
        WebKitNativeShim.installHostTable();
    }

    @BeforeEach
    void installTheProductionTable() {
        WebKitNativeShim.installProductionHostTable();
        // A failure recorded by an earlier test on this thread would otherwise be reported as this
        // test's, which is exactly the confusion check_and_clear_exception exists to avoid.
        WebKitNativeShim.checkAndClearUpcallFailure();
    }

    // ------------------------------------------------------------------------- filesystem

    /**
     * The whole {@code filesystem} group against a real file, which is what
     * {@code wtf/java/FileSystemJava.cpp} does with it. Ten slots, one temp file: if any descriptor
     * disagreed with the C prototype the arguments would arrive shifted and the answers would be
     * nonsense rather than the file's own size and name.
     */
    @Test
    public void theFileSystemGroupReachesTheRealFileSystem() throws Exception {
        byte[] content = "the quick brown fox".getBytes(StandardCharsets.UTF_8);
        Path file = tempDir.resolve("upcall.txt");
        Files.write(file, content);

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment path = utf16(arena, file.toString());
            int pathLength = file.toString().length();

            assertEquals(1L, fire("filesystem.file_exists", path.address(), pathLength),
                    "fwkFileExists must see the file that was just written");
            assertEquals(content.length,
                    fire("filesystem.get_file_size", path.address(), pathLength),
                    "fwkGetFileSize must report the byte count, not a status code");

            MemorySegment metadata = arena.allocate(JAVA_LONG, 3);
            assertEquals(1L, fire("filesystem.get_file_metadata", path.address(), pathLength,
                    metadata.address()));
            assertEquals(content.length, metadata.getAtIndex(JAVA_LONG, 1),
                    "the second metadata slot is the length in bytes");
            assertEquals(1L, metadata.getAtIndex(JAVA_LONG, 2),
                    "the third metadata slot is FileMetadata::Type, and this one is a file");

            assertEquals("upcall.txt",
                    string("filesystem.path_get_file_name", arena, path.address(), pathLength));

            MemorySegment leaf = utf16(arena, "child");
            assertEquals(new File(file.toString(), "child").getPath(),
                    string("filesystem.path_by_appending_component", arena, path.address(),
                            pathLength, leaf.address(), 5));

            Path directory = tempDir.resolve("a/b/c");
            MemorySegment directoryPath = utf16(arena, directory.toString());
            assertEquals(1L, fire("filesystem.make_all_directories", directoryPath.address(),
                    directory.toString().length()));
            assertTrue(Files.isDirectory(directory));

            MemorySegment mode = utf16(arena, "r");
            long handle = fire("filesystem.open_file", path.address(), pathLength, mode.address(),
                    1);
            assertNotEquals(0L, handle, "0 is invalidPlatformFileHandle, so the open failed");
            try {
                MemorySegment buffer = arena.allocate(JAVA_BYTE, content.length);
                assertEquals(content.length,
                        fire("filesystem.read_from_file", handle, buffer.address(),
                                content.length));
                assertArrayEquals(content, buffer.toArray(JAVA_BYTE),
                        "the ByteBuffer the slot wraps must be the caller's own memory");

                assertEquals(FIRED, WkjStubShim.fireHost(slot("filesystem.seek_file"), handle, 4L));
                MemorySegment tail = arena.allocate(JAVA_BYTE, 5);
                assertEquals(5L, fire("filesystem.read_from_file", handle, tail.address(), 5));
                assertEquals("quick", new String(tail.toArray(JAVA_BYTE), StandardCharsets.UTF_8),
                        "the read must continue from where fwkSeekFile left the channel");
            } finally {
                assertEquals(FIRED, WkjStubShim.fireHost(slot("filesystem.close_file"), handle));
            }
        }
        assertNoUpcallFailed();
    }

    // -------------------------------------------------------------------------------- pal

    /**
     * The three digest slots, against {@link MessageDigest} directly. The digest is fed through the
     * ABI in two chunks, because {@code crypto_digest_add_bytes} is called once per
     * {@code CryptoDigest::addBytes} and the accumulating state is the whole point of keeping the
     * digest object alive behind a {@code wkj_ref}.
     */
    @Test
    public void thePalGroupComputesTheSameDigestAsTheJdk() throws Exception {
        byte[] first = "hello ".getBytes(StandardCharsets.UTF_8);
        byte[] second = "world".getBytes(StandardCharsets.UTF_8);
        MessageDigest expected = MessageDigest.getInstance("SHA-256");
        expected.update(first);
        expected.update(second);
        byte[] want = expected.digest();

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment algorithm = utf16(arena, "SHA-256");
            long digest = fire("pal.crypto_digest_create", algorithm.address(), 7);
            assertNotEquals(0L, digest, "0 means Java has no such algorithm");

            addBytes(arena, digest, first);
            addBytes(arena, digest, second);

            MemorySegment out = arena.allocate(JAVA_BYTE, 64);
            MemorySegment length = arena.allocate(JAVA_INT);
            assertEquals(STR_OK, fire("pal.crypto_digest_compute_hash", digest, out.address(), 64,
                    length.address()));
            assertEquals(want.length, length.get(JAVA_INT, 0L));
            assertArrayEquals(want, out.asSlice(0L, want.length).toArray(JAVA_BYTE),
                    "the digest computed through the ABI must be the JDK's own");
        }
        assertNoUpcallFailed();
    }

    /**
     * An algorithm Java does not have answers 0 rather than throwing, which is what the JNI code
     * produced when {@code getInstance} threw and the exception check turned the result into an
     * empty handle - and a 0 digest then makes the other two slots no-ops.
     */
    @Test
    public void anUnknownDigestAlgorithmIsZeroRatherThanAFailure() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment algorithm = utf16(arena, "SHA-9999");
            assertEquals(0L, fire("pal.crypto_digest_create", algorithm.address(), 8));

            MemorySegment out = arena.allocate(JAVA_BYTE, 64);
            MemorySegment length = arena.allocate(JAVA_INT);
            assertEquals(STR_NULL, fire("pal.crypto_digest_compute_hash", 0L, out.address(), 64,
                    length.address()), "a 0 digest computes nothing, exactly as the jDigest guard did");
        }
        assertNoUpcallFailed();
    }

    // -------------------------------------------------------------------------------- wtf

    /**
     * The round trip the {@code wtf} group exists for: the library asks Java to schedule the main
     * thread work, and {@code MainThread.fwkScheduleDispatchFunctions} posts a runnable onto the
     * event thread, which is what later calls {@code wkj_main_thread_dispatch_functions}. With this
     * slot NULL nothing would ever post that runnable and no {@code WTF::callOnMainThread} work
     * would run at all.
     */
    @Test
    public void theWtfGroupPostsTheMainThreadRunnable() {
        RecordingInvoker invoker = new RecordingInvoker();
        Invoker.setInvoker(invoker);
        try {
            assertEquals(FIRED,
                    WkjStubShim.fireHost(slot("wtf.main_thread_schedule_dispatch")));
            assertEquals(1, invoker.posted.size(),
                    "exactly one runnable must have been posted to the event thread");
            assertNotNull(invoker.posted.get(0));
        } finally {
            Invoker.setInvoker(savedInvoker);
        }
        assertNoUpcallFailed();
    }

    // ------------------------------------------------------------------------------ theme

    /** A static with no target ref at all, and the simplest proof that the group is reachable. */
    @Test
    public void theThemeGroupAnswersTheScrollBarThickness() {
        assertEquals(ScrollBarTheme.getThickness(),
                fire("theme.scroll_bar_get_thickness"),
                "the slot must answer what the Java static answers, 12 by default");
        assertNoUpcallFailed();
    }

    /**
     * {@code java.net.IDN}, which the header keeps as an upcall because its caller is WebCore URL
     * parsing rather than Java, and because IDNA2003 and ICU's UTS-46 are not the same answer.
     */
    @Test
    public void theThemeGroupConvertsAnInternationalisedHostName() {
        try (Arena arena = Arena.ofConfined()) {
            String hostname = "bücher.example";
            MemorySegment name = utf16(arena, hostname);
            assertEquals(java.net.IDN.toASCII(hostname, 0),
                    string("theme.idn_to_ascii", arena, name.address(), hostname.length(), 0));
        }
        assertNoUpcallFailed();
    }

    /**
     * The context menu item slots, which are the group's example of a target ref plus setters. The
     * item is created by the ABI, configured by the ABI and then read back through the Java object
     * the id names, so a shifted argument would show up as the wrong field being set.
     */
    @Test
    public void theThemeGroupBuildsAContextMenuItem() {
        long item = fire("theme.context_menu_item_create");
        assertNotEquals(0L, item, "ContextMenuItem.fwkCreateContextMenuItem answered nothing");
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment title = utf16(arena, "Copy");
            assertEquals(FIRED, WkjStubShim.fireHost(slot("theme.context_menu_item_set_title"),
                    item, title.address(), 4L));
            assertEquals(FIRED, WkjStubShim.fireHost(slot("theme.context_menu_item_set_action"),
                    item, 7L));
            assertEquals(FIRED, WkjStubShim.fireHost(slot("theme.context_menu_item_set_type"),
                    item, 2L));
            assertEquals(FIRED, WkjStubShim.fireHost(slot("theme.context_menu_item_set_checked"),
                    item, 1L));
            assertEquals(FIRED, WkjStubShim.fireHost(slot("theme.context_menu_item_set_enabled"),
                    item, 0L));
        }
        ContextMenuItem target = (ContextMenuItem) WebKitNativeShim.lookup(item);
        assertNotNull(target);
        assertEquals("Copy", target.getTitle());
        assertEquals(7, target.getAction());
        assertEquals(2, target.getType());
        assertTrue(target.isChecked());
        assertTrue(!target.isEnabled());
        assertNoUpcallFailed();
    }

    /**
     * A NULL title is not the empty string: the library passes NULL for an empty title, because the
     * JNI code wrote {@code title.isEmpty() ? NULL : title.toJavaString(env)} and the Java side may
     * well tell them apart. Normalising it on either side would be a behaviour change.
     */
    @Test
    public void anEmptyContextMenuTitleArrivesAsNullNotAsEmpty() {
        long item = fire("theme.context_menu_item_create");
        assertEquals(FIRED, WkjStubShim.fireHost(slot("theme.context_menu_item_set_title"), item,
                0L, 0L));
        assertEquals(null, ((ContextMenuItem) WebKitNativeShim.lookup(item)).getTitle());
        assertNoUpcallFailed();
    }

    // --------------------------------------------------------------------------- graphics

    /**
     * The {@code WCTextRun} family against a run this test registers, which is the group's shape
     * with a target ref and an out parameter array. {@code getGlyphPosAndAdvance} in particular
     * writes four floats the JNI version read back out of a {@code float[]}.
     */
    @Test
    public void theGraphicsGroupReadsATextRun() {
        long run = WebKitNativeShim.register(new StubTextRun());
        try (Arena arena = Arena.ofConfined()) {
            assertEquals(1L, fire("graphics.text_run_is_left_to_right", run));
            assertEquals(3L, fire("graphics.text_run_get_glyph_count", run));
            assertEquals(5L, fire("graphics.text_run_get_start", run));
            assertEquals(8L, fire("graphics.text_run_get_end", run));
            assertEquals(20L, fire("graphics.text_run_get_char_offset", run, 2));
            assertEquals(102L, fire("graphics.text_run_get_glyph", run, 2));

            MemorySegment out = arena.allocate(JAVA_FLOAT, 4);
            assertEquals(1L,
                    fire("graphics.text_run_get_glyph_pos_and_advance", run, 1, out.address()));
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, out.toArray(JAVA_FLOAT),
                    "the four floats must arrive in the order the Java array had them");

            MemorySegment none = arena.allocate(JAVA_FLOAT, 4);
            assertEquals(0L,
                    fire("graphics.text_run_get_glyph_pos_and_advance", run, 99, none.address()),
                    "a null Java array is 0 and writes nothing; the JNI version dereferenced it");
        } finally {
            WebKitNativeShim.unregister(run);
        }
        assertNoUpcallFailed();
    }

    /**
     * The ten {@code image_decoder_} slots, which are the only ones in the group also reached from
     * decoder threads. They are the group's third shape: a byte array in, two {@code int[2]} out
     * parameters and a caller-provided string buffer, all against one target ref.
     */
    @Test
    public void theGraphicsGroupDrivesAnImageDecoder() {
        StubImageDecoder decoder = new StubImageDecoder();
        long id = WebKitNativeShim.register(decoder);
        try (Arena arena = Arena.ofConfined()) {
            byte[] data = { 1, 2, 3, 4 };
            MemorySegment bytes = arena.allocateFrom(JAVA_BYTE, data);
            assertEquals(FIRED, WkjStubShim.fireHost(slot("graphics.image_decoder_add_image_data"),
                    id, bytes.address(), data.length));
            assertArrayEquals(data, decoder.received);

            assertEquals(FIRED, WkjStubShim.fireHost(slot("graphics.image_decoder_add_image_data"),
                    id, 0L, 0L));
            assertEquals(null, decoder.received,
                    "a NULL pointer with length 0 is the end of stream call, and reaches Java as"
                            + " the null array the JNI code passed");

            MemorySegment size = arena.allocate(JAVA_INT, 2);
            assertEquals(1L, fire("graphics.image_decoder_get_image_size", id, size.address()));
            assertArrayEquals(new int[] { 16, 9 }, size.toArray(JAVA_INT));

            assertEquals(2L, fire("graphics.image_decoder_get_frame_count", id));
            assertEquals(70L, fire("graphics.image_decoder_get_frame_duration", id, 1));
            assertEquals(1L, fire("graphics.image_decoder_get_frame_complete", id, 0));

            MemorySegment frameSize = arena.allocate(JAVA_INT, 2);
            assertEquals(1L,
                    fire("graphics.image_decoder_get_frame_size", id, 1, frameSize.address()));
            assertArrayEquals(new int[] { 8, 4 }, frameSize.toArray(JAVA_INT));
            assertEquals(0L,
                    fire("graphics.image_decoder_get_frame_size", id, 9, frameSize.address()),
                    "a null Java array is 0 and writes nothing, which the caller turns into the"
                            + " whole image size");

            assertEquals("png",
                    string("graphics.image_decoder_get_filename_extension", arena, id));

            assertEquals(FIRED, WkjStubShim.fireHost(slot("graphics.image_decoder_destroy"), id));
            assertTrue(decoder.destroyed);
        } finally {
            WebKitNativeShim.unregister(id);
        }
        assertNoUpcallFailed();
    }

    /**
     * The manager backed slots answer the default documented on each when no
     * {@code WCGraphicsManager} has been set, rather than throwing. That is the state a headless
     * process is in, and the JNI code reached the same outcome by reading a null static field.
     */
    @Test
    public void theGraphicsGroupToleratesAnAbsentManager() {
        assumeTrue(com.sun.webkit.graphics.WCGraphicsManager.getGraphicsManager() == null,
                "this test describes the no-manager case and something has set one");
        assertEquals(0L, fire("graphics.create_path"));
        assertEquals(0L, fire("graphics.get_image_decoder"));
        assertEquals(0L, fire("graphics.create_rt_image", 4, 4));
        assertEquals(-1L, fire("graphics.ref_get_id", 0L),
                "an unresolved Ref is -1, which is RQRef's own not-yet-resolved value");
        assertNoUpcallFailed();
    }

    // ---------------------------------------------------------------------------- network

    /** {@code FormDataElement.fwkCreateFromByteArray}, which needs nothing but a byte array. */
    @Test
    public void theNetworkGroupCreatesAFormDataElement() {
        byte[] body = "a=1&b=2".getBytes(StandardCharsets.UTF_8);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment bytes = arena.allocateFrom(JAVA_BYTE, body);
            long element = fire("network.form_data_create_from_bytes", bytes.address(),
                    body.length);
            assertNotEquals(0L, element);
            Object built = WebKitNativeShim.lookup(element);
            assertNotNull(built, "the id must name the object the factory built");
            // FormDataElement is package private, so the class is named rather than imported.
            assertEquals("com.sun.webkit.network.FormDataElement",
                    built.getClass().getSuperclass().getName(),
                    "fwkCreateFromByteArray builds a FormDataElement.ByteArrayElement");
            WebKitNativeShim.unregister(element);
        }
        assertNoUpcallFailed();
    }

    // ------------------------------------------------------------------------------ media

    /**
     * {@code get_supported_types} with no manager is {@code WKJ_STR_NULL}, which the C header
     * distinguishes from an empty list; the group's other fifteen slots answer their own defaults
     * for an id that names no player.
     */
    @Test
    public void theMediaGroupAnswersItsDocumentedDefaults() {
        assumeTrue(com.sun.webkit.graphics.WCGraphicsManager.getGraphicsManager() == null,
                "this test describes the no-manager case and something has set one");
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment out = arena.allocate(JAVA_CHAR, 64);
            MemorySegment length = arena.allocate(JAVA_INT);
            assertEquals(STR_NULL,
                    fire("media.get_supported_types", out.address(), 64, length.address()));
        }
        assertEquals(0L, fire("media.create_player", 1L));
        assertEquals(0L, fire("media.get_current_time", 0L),
                "an id that names no player is 0.0f, and its bits are 0");
        assertEquals(FIRED, WkjStubShim.fireHost(slot("media.pause"), 0L),
                "a void slot with no player must simply do nothing");
        assertNoUpcallFailed();
    }

    // --------------------------------------------------------------------------- plumbing

    private static int slot(String name) {
        int index = WkjStubShim.findHostSlot(name);
        assertNotEquals(-1, index, "the C header declares no host slot " + name);
        return index;
    }

    /** Fires a slot that returns a value, asserting the dispatch itself succeeded. */
    private static long fire(String name, long... args) {
        assertEquals(FIRED, WkjStubShim.fireHost(slot(name), args),
                "the library could not dispatch " + name + "; -2 means the slot is still NULL");
        return WkjStubShim.lastFireResult();
    }

    /**
     * Fires a slot whose last three parameters are the contract 13 {@code out_buf, out_cap,
     * out_length} triple, and decodes what it wrote.
     */
    private static String string(String name, Arena arena, long... leading) {
        MemorySegment buffer = arena.allocate(JAVA_CHAR, 256);
        MemorySegment length = arena.allocate(JAVA_INT);
        long[] args = new long[leading.length + 3];
        System.arraycopy(leading, 0, args, 0, leading.length);
        args[leading.length] = buffer.address();
        args[leading.length + 1] = 256L;
        args[leading.length + 2] = length.address();
        assertEquals(STR_OK, fire(name, args), name + " did not answer a string");
        return new String(buffer.toArray(JAVA_CHAR), 0, length.get(JAVA_INT, 0L));
    }

    private static void addBytes(Arena arena, long digest, byte[] data) {
        MemorySegment bytes = arena.allocateFrom(JAVA_BYTE, data);
        assertEquals(FIRED, WkjStubShim.fireHost(slot("pal.crypto_digest_add_bytes"), digest,
                bytes.address(), data.length));
    }

    private static MemorySegment utf16(Arena arena, String s) {
        return arena.allocateFrom(JAVA_CHAR, s.toCharArray());
    }

    /**
     * Every upcall above must have returned normally. A target that threw is caught and logged, so
     * the only way to see it is the flag {@code core.check_and_clear_exception} reports - which is
     * exactly the observation the C++ callers make.
     */
    private static void assertNoUpcallFailed() {
        assertEquals(0, WebKitNativeShim.checkAndClearUpcallFailure(),
                "an upcall target threw and was contained; see the severe log entry above");
    }

    /** Records what {@code MainThread.fwkScheduleDispatchFunctions} posts, and runs nothing. */
    private static final class RecordingInvoker extends Invoker {

        private final List<Runnable> posted = new ArrayList<>();

        @Override
        protected boolean isEventThread() {
            return true;
        }

        @Override
        public void invokeOnEventThread(Runnable r) {
            r.run();
        }

        @Override
        public void postOnEventThread(Runnable r) {
            posted.add(r);
        }
    }

    /** A {@link WCImageDecoder} with known answers, and a record of what it was handed. */
    private static final class StubImageDecoder extends WCImageDecoder {

        private byte[] received;
        private boolean destroyed;

        @Override
        protected void addImageData(byte[] data) {
            received = data;
        }

        @Override
        protected int[] getImageSize() {
            return new int[] { 16, 9 };
        }

        @Override
        protected int getFrameCount() {
            return 2;
        }

        @Override
        protected WCImageFrame getFrame(int index) {
            return null;
        }

        @Override
        protected int getFrameDuration(int index) {
            return 70;
        }

        @Override
        protected int[] getFrameSize(int index) {
            return index == 1 ? new int[] { 8, 4 } : null;
        }

        @Override
        protected boolean getFrameCompleteStatus(int index) {
            return index == 0;
        }

        @Override
        protected void loadFromResource(String name) {
        }

        @Override
        protected void destroy() {
            destroyed = true;
        }

        @Override
        protected String getFilenameExtension() {
            return "png";
        }
    }

    /** A {@link WCTextRun} with known answers, so that a shifted argument is visible. */
    private static final class StubTextRun implements WCTextRun {

        @Override
        public boolean isLeftToRight() {
            return true;
        }

        @Override
        public float[] getGlyphPosAndAdvance(int glyphIndex) {
            return glyphIndex == 1 ? new float[] { 1.0f, 2.0f, 3.0f, 4.0f } : null;
        }

        @Override
        public int getCharOffset(int index) {
            return 10 * index;
        }

        @Override
        public int getEnd() {
            return 8;
        }

        @Override
        public int getGlyph(int index) {
            return 100 + index;
        }

        @Override
        public int getGlyphCount() {
            return 3;
        }

        @Override
        public int getStart() {
            return 5;
        }
    }
}
