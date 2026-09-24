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

package test.com.sun.glass.ui.gtk;

import com.sun.glass.ui.Application;
import com.sun.glass.ui.ClipboardAssistance;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.gtk.GtkGlassShim;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code GtkSystemClipboard} on a running toolkit, as {@code GlassSystemClipboard.cpp} answered it at commit
 * {@code 033187ad90}, pinned so that the {@code java.lang.foreign} replacement can be held to it. The peer
 * methods are called directly, not through {@code SystemClipboard.getData}, which never reaches
 * {@code popFromSystem} while this process owns the clipboard.
 * <p>
 * What another process sees is what the clipboard is for, so the flavours are exchanged between two child JVMs on
 * one X11 display, in both directions: one pushes, the other reads every flavour, then they swap. A single process
 * cannot prove any of it - the X selection machinery short-circuits and the get callback may never run.
 * <ul>
 * <li>text is served as UTF-8 up to its first NUL and read back the same way, a lone surrogate having become
 * {@code ?} on the way out;</li>
 * <li>a {@code text/*} mime other than {@code text/plain} is served as its own target and read back as a
 * {@code String}, any other mime as a {@code ByteBuffer} over exactly the bytes served, which are the whole
 * backing array of the pushed buffer whatever its position and limit;</li>
 * <li>a file list and a URL share the {@code text/uri-list} target: the files leave as {@code file://} URIs and
 * come back as paths, the URL comes back as the URI list with the file URIs left out; a file URI that follows a
 * non-file one comes back with them, where commit {@code 033187ad90} dropped it with a reported
 * {@code ArrayIndexOutOfBoundsException};</li>
 * <li>an image is served as a pixbuf and comes back as a {@code GtkPixels} of the same size and bytes;</li>
 * <li>the mime list names {@code text/plain} for the first text target, {@code application/x-java-rawimage} for
 * the first image target, and both java file list and {@code text/uri-list} when the URI list has both kinds;</li>
 * <li>an empty map clears the clipboard, and the empty clipboard answers {@code null} mimes and {@code null} for
 * every flavour;</li>
 * <li>{@code init} and {@code dispose} connect and disconnect the owner-change notification, and
 * {@code pushTargetActionToSystem} and {@code supportedSourceActionsFromSystem} are the no-op and the 0 they
 * always were.</li>
 * </ul>
 */
@EnabledOnOs(OS.LINUX)
@Timeout(600)
public class GtkClipboardNativeTest {

    /** The directory the two child JVMs hand their markers through. */
    static final String EXCHANGE_PROPERTY = "gtk.clipboard.exchange";

    /** Text with a non-ASCII character, a supplementary character and a lone surrogate, which becomes {@code ?}. */
    static final String TEXT = "clip " + (char) 0xE9 + " " + (char) 0xD83D + (char) 0xDE00 + " "
            + (char) 0xD800 + " end";

    /** Text with an embedded U+0000; everything from the NUL on is cut off by {@code strlen}. */
    static final String TEXT_WITH_NUL = "before" + (char) 0 + "after";

    static final String HTML = "<b>" + (char) 0xE9 + "</b>";

    static final String URL = "https://example.invalid/a%20b";

    static final List<String> FILES = List.of("/tmp/jfx-clipboard-a.txt", "/tmp/jfx clipboard b.txt");

    static final String CUSTOM_MIME = "application/x-jfx-clipboard-test";

    /** The one file of {@link #MIXED_URI_LIST}; it need not exist, the URI is only parsed. */
    static final String MIXED_FILE = "/tmp/jfx-clipboard-mixed.txt";

    /**
     * A URI list whose file URI follows a non-file one - the order another application easily produces, and the
     * one that tells apart a file list indexed by the position of the URI from one indexed by the number of
     * files. It travels as the URL of one push, which {@code gtk_selection_data_set_uris} writes out as the two
     * CR LF separated lines it already is.
     */
    static final String MIXED_URI_LIST = URL + "\r\n" + "file://" + MIXED_FILE;

    /** The whole backing array of the custom-mime buffer; its position and limit are set away from both ends. */
    static final byte[] CUSTOM_BYTES = customBytes();

    static final int IMAGE_WIDTH = 3;

    static final int IMAGE_HEIGHT = 2;

    private static GtkGlassChildJvm.Run producer;

    private static GtkGlassChildJvm.Run consumer;

    private static GtkGlassChildJvm.Run single;

    @BeforeAll
    @Timeout(3 * GtkGlassChildJvm.BEFORE_ALL_SECONDS)
    static void runScenarios() {
        GtkGlassChildJvm.requireDisplay();
        Path exchange = exchangeDirectory();
        List<String> options = List.of("-D" + EXCHANGE_PROPERTY + "=" + exchange);
        List<GtkGlassChildJvm.Run> runs = GtkGlassChildJvm.runTogether(List.of(
                GtkGlassChildJvm.start(GtkClipboardNativeTest.class, "producerScenario", options),
                GtkGlassChildJvm.start(GtkClipboardNativeTest.class, "consumerScenario", options)));
        producer = runs.get(0);
        consumer = runs.get(1);
        single = GtkGlassChildJvm.run(GtkClipboardNativeTest.class, "singleProcessScenario", List.of());
    }

    private static Path exchangeDirectory() {
        try {
            Path exchange = Path.of("target", "gtk-glass-child", "clipboard-exchange").toAbsolutePath();
            if (Files.isDirectory(exchange)) {
                try (var files = Files.list(exchange)) {
                    for (Path file : files.toList()) {
                        Files.delete(file);
                    }
                }
            }
            Files.createDirectories(exchange);
            return exchange;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String value(GtkGlassChildJvm.Run run, String key) {
        String result = run.values().get(key);
        if (result == null && !run.values().containsKey(key)) {
            throw new AssertionError("the child recorded no " + key + ": " + run.describe());
        }
        return result;
    }

    /** What the other process read is what this one pushed, both ways round. */
    @Test
    public void textCrossesTheProcessBoundary() {
        for (GtkGlassChildJvm.Run reader : List.of(consumer, producer)) {
            assertEquals(TEXT.replace(String.valueOf((char) 0xD800), "?"), value(reader, "read.text/plain"));
        }
    }

    /** The bytes of a text flavour stop at the first NUL, as {@code strlen} of the pushed buffer did. */
    @Test
    public void textIsCutAtTheFirstNul() {
        for (GtkGlassChildJvm.Run reader : List.of(consumer, producer)) {
            assertEquals("before", value(reader, "read.text/x-jfx-nul"));
        }
    }

    /** A {@code text/*} mime other than {@code text/plain} keeps its own target and comes back as a String. */
    @Test
    public void anotherTextMimeIsServedAsItself() {
        for (GtkGlassChildJvm.Run reader : List.of(consumer, producer)) {
            assertEquals(HTML, value(reader, "read.text/html"));
        }
    }

    /** A non-text mime comes back as a ByteBuffer over exactly the whole backing array that was pushed. */
    @Test
    public void aCustomMimeCrossesAsItsWholeBackingArray() {
        String expected = HexFormat.of().formatHex(CUSTOM_BYTES);
        for (GtkGlassChildJvm.Run reader : List.of(consumer, producer)) {
            assertEquals(expected, value(reader, "read." + CUSTOM_MIME));
        }
    }

    /** The file list leaves as {@code file://} URIs and comes back as the paths themselves. */
    @Test
    public void theFileListCrossesAsPaths() {
        for (GtkGlassChildJvm.Run reader : List.of(consumer, producer)) {
            assertEquals(String.join("|", FILES), value(reader, "read.application/x-java-file-list"));
        }
    }

    /** The URL comes back as the URI list of everything that is not a file. */
    @Test
    public void theUrlCrossesAsTheUriList() {
        for (GtkGlassChildJvm.Run reader : List.of(consumer, producer)) {
            assertEquals(URL, value(reader, "read.text/uri-list"));
        }
    }

    /** The image crosses as a pixbuf and comes back with the same size and the same bytes. */
    @Test
    public void theImageCrossesAsAPixbuf() {
        for (GtkGlassChildJvm.Run reader : List.of(consumer, producer)) {
            assertEquals(IMAGE_WIDTH + "x" + IMAGE_HEIGHT, value(reader, "read.image.size"));
            assertEquals(imageHex(), value(reader, "read.image.bytes"));
        }
    }

    /**
     * The mime list names the java mime for the first text and the first image target, both URI-list mimes when
     * the list holds files and a URL, and passes everything else through - the X11 meta targets GTK adds for
     * every owner, every image type gdk-pixbuf can decode, and the mimes that were pushed. {@code text/plain}
     * appears twice, once for the first text target and once as the target of that name.
     */
    @Test
    public void theMimeListNamesTheJavaMimes() {
        assertEquals(value(consumer, "read.mimes"), value(producer, "read.mimes"),
                "the two processes offered the same content but were told different mimes");
        for (GtkGlassChildJvm.Run reader : List.of(consumer, producer)) {
            List<String> mimes = List.of(value(reader, "read.mimes").split("\\|"));
            for (String expected : List.of("TIMESTAMP", "TARGETS", "MULTIPLE", "text/html", "text/x-jfx-nul",
                    CUSTOM_MIME, "UTF8_STRING", "image/png")) {
                assertTrue(mimes.contains(expected), expected + " missing from " + mimes);
            }
            assertEquals(1, count(mimes, "application/x-java-rawimage"), mimes.toString());
            assertEquals(1, count(mimes, "application/x-java-file-list"), mimes.toString());
            assertEquals(1, count(mimes, "text/uri-list"), mimes.toString());
            assertEquals(2, count(mimes, "text/plain"), mimes.toString());
        }
    }

    private static long count(List<String> mimes, String mime) {
        return mimes.stream().filter(mime::equals).count();
    }

    /**
     * A URI list whose file URI follows a non-file one: the path is the one element of the file list, and nothing
     * is reported, while the URI list itself answers the non-file URI as always. Commit {@code 033187ad90} wrote
     * the path at the index of the URI within the whole list, past the end of an array sized by the number of
     * files, so the {@code ArrayIndexOutOfBoundsException} was reported and the entry left {@code null}.
     */
    @Test
    public void aFileUriAfterANonFileUriKeepsItsPlace() {
        assertEquals("[" + MIXED_FILE + "]", value(consumer, "mixed.files"));
        assertEquals(URL, value(consumer, "mixed.uriList"));
        assertEquals("", value(consumer, "mixed.reported"), "the file list reported an exception");
        List<String> mimes = List.of(value(consumer, "mixed.mimes").split("\\|"));
        assertEquals(1, count(mimes, "application/x-java-file-list"), mimes.toString());
        assertEquals(1, count(mimes, "text/uri-list"), mimes.toString());
    }

    /** Only the process that pushed last owns the clipboard. */
    @Test
    public void onlyTheLastPusherIsTheOwner() {
        assertEquals("false", value(consumer, "ownerBeforePush"));
        assertEquals("true", value(consumer, "ownerAfterPush"));
        assertEquals("false", value(producer, "ownerAfterTheirPush"));
    }

    /** The owner-change signal reaches the peer: the clipboard tells its assistants the content changed. */
    @Test
    public void theOwnerChangeNotificationArrives() {
        assertTrue(Integer.parseInt(value(single, "contentChanged.afterPush")) > 0,
                "no contentChanged after pushing");
        assertEquals(value(single, "contentChanged.afterPush"), value(single, "contentChanged.afterDispose"),
                "dispose left the owner-change handler connected");
        assertTrue(Integer.parseInt(value(single, "contentChanged.afterReinit"))
                > Integer.parseInt(value(single, "contentChanged.afterDispose")),
                "init did not connect the owner-change handler again");
    }

    /**
     * An empty map clears the clipboard: the offered targets are the three X11 meta targets GTK adds for every
     * owner and nothing else, and every flavour answers {@code null}. The list is not empty and not {@code null}
     * because this process is still the owner - pushing an empty map takes ownership with an empty target table.
     */
    @Test
    public void anEmptyMapClearsTheClipboard() {
        assertEquals("TIMESTAMP|TARGETS|MULTIPLE", value(single, "empty.mimes"));
        for (String mime : List.of("text/plain", "text/html", "text/uri-list", "application/x-java-file-list",
                "application/x-java-rawimage", CUSTOM_MIME)) {
            assertNull(value(single, "empty.pop." + mime), mime);
        }
    }

    /** The two no-ops of the peer stayed no-ops. */
    @Test
    public void theDragAndDropOnlyMethodsDoNothing() {
        assertEquals("0", value(single, "supportedSourceActions"));
        assertEquals("true", value(single, "pushTargetAction.returned"));
    }

    private static String imageHex() {
        return HexFormat.of().formatHex(imageBytes());
    }

    /** {@code IMAGE_WIDTH * IMAGE_HEIGHT} BGRA pixels with a distinct, fully opaque colour each. */
    static byte[] imageBytes() {
        byte[] bytes = new byte[IMAGE_WIDTH * IMAGE_HEIGHT * 4];
        for (int i = 0; i < IMAGE_WIDTH * IMAGE_HEIGHT; i++) {
            bytes[i * 4] = (byte) (0x10 + i);
            bytes[i * 4 + 1] = (byte) (0x40 + i);
            bytes[i * 4 + 2] = (byte) (0x80 + i);
            bytes[i * 4 + 3] = (byte) 0xFF;
        }
        return bytes;
    }

    private static byte[] customBytes() {
        byte[] bytes = new byte[16];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (i * 17);
        }
        return bytes;
    }

    /** The map both children push, differing only in the text so that each can tell whose content it read. */
    static HashMap<String, Object> content() {
        HashMap<String, Object> data = new HashMap<>();
        data.put("text/plain", TEXT);
        data.put("text/html", HTML);
        data.put("text/x-jfx-nul", TEXT_WITH_NUL);
        data.put("text/uri-list", URL);
        data.put("application/x-java-file-list", FILES.toArray(new String[0]));
        ByteBuffer custom = ByteBuffer.wrap(CUSTOM_BYTES.clone());
        custom.position(4);
        custom.limit(8);
        data.put(CUSTOM_MIME, custom);
        Pixels pixels = Application.GetApplication().createPixels(IMAGE_WIDTH, IMAGE_HEIGHT,
                ByteBuffer.wrap(imageBytes()));
        data.put("application/x-java-rawimage", pixels);
        return data;
    }

    /** Reads every flavour of the clipboard through the peer and records what came back. */
    static void readEverything(Map<String, String> out) throws Exception {
        GtkGlassChild.onFx(() -> {
            String[] mimes = GtkGlassShim.clipboardMimesFromSystem();
            out.put("read.mimes", mimes == null ? null : String.join("|", mimes));
            out.put("read.text/plain", (String) GtkGlassShim.clipboardPopFromSystem("text/plain"));
            out.put("read.text/html", (String) GtkGlassShim.clipboardPopFromSystem("text/html"));
            out.put("read.text/x-jfx-nul", (String) GtkGlassShim.clipboardPopFromSystem("text/x-jfx-nul"));
            out.put("read.text/uri-list", (String) GtkGlassShim.clipboardPopFromSystem("text/uri-list"));
            Object files = GtkGlassShim.clipboardPopFromSystem("application/x-java-file-list");
            out.put("read.application/x-java-file-list",
                    files == null ? null : String.join("|", (String[]) files));
            Object custom = GtkGlassShim.clipboardPopFromSystem(CUSTOM_MIME);
            out.put("read." + CUSTOM_MIME, custom == null ? null : hex((ByteBuffer) custom));
            Object image = GtkGlassShim.clipboardPopFromSystem("application/x-java-rawimage");
            if (image instanceof Pixels pixels) {
                out.put("read.image.size", pixels.getWidth() + "x" + pixels.getHeight());
                out.put("read.image.bytes", hex(pixels.asByteBuffer()));
            } else {
                out.put("read.image.size", String.valueOf(image));
                out.put("read.image.bytes", String.valueOf(image));
            }
            return null;
        });
    }

    private static String hex(ByteBuffer buffer) {
        ByteBuffer copy = buffer.duplicate();
        byte[] bytes = new byte[copy.remaining()];
        copy.get(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    /** Runs in {@link GtkGlassChild}: pushes first, then reads what the other one pushed. */
    static void producerScenario(Map<String, String> out) throws Exception {
        Path exchange = Path.of(System.getProperty(EXCHANGE_PROPERTY));
        GtkGlassChild.onFx(() -> {
            GtkGlassShim.clipboardPushToSystem(content(), 0);
            return null;
        });
        Files.writeString(exchange.resolve("producer.pushed"), "1");
        await(exchange.resolve("consumer.pushed"));
        out.put("ownerAfterTheirPush", GtkGlassChild.onFx(() ->
                Boolean.toString(GtkGlassShim.clipboardIsOwner())));
        readEverything(out);
        Files.writeString(exchange.resolve("producer.read"), "1");
        await(exchange.resolve("consumer.read"));

        HashMap<String, Object> mixed = new HashMap<>();
        mixed.put("text/uri-list", MIXED_URI_LIST);
        GtkGlassChild.onFx(() -> {
            GtkGlassShim.clipboardPushToSystem(mixed, 0);
            return null;
        });
        Files.writeString(exchange.resolve("producer.mixed"), "1");
        await(exchange.resolve("consumer.mixed"));
    }

    /**
     * Reads the mixed URI list the other process pushed. The file URI comes after the URL, so it is the first and
     * only file of the list: {@code uris_to_java} writes it at index 0, where commit {@code 033187ad90} wrote it
     * at the index of the URI within the whole list and reported an {@code ArrayIndexOutOfBoundsException}.
     */
    static void readMixedUriList(Map<String, String> out) throws Exception {
        GtkGlassChild.onFx(() -> {
            // Application.reportException hands the throwable to this thread's uncaught exception handler
            List<String> reported = new ArrayList<>();
            Thread thread = Thread.currentThread();
            Thread.UncaughtExceptionHandler previous = thread.getUncaughtExceptionHandler();
            thread.setUncaughtExceptionHandler((ignored, throwable) -> reported.add(throwable.getClass().getName()));
            try {
                Object files = GtkGlassShim.clipboardPopFromSystem("application/x-java-file-list");
                out.put("mixed.files", files == null ? null : Arrays.toString((String[]) files));
                out.put("mixed.uriList", String.valueOf(GtkGlassShim.clipboardPopFromSystem("text/uri-list")));
                String[] mimes = GtkGlassShim.clipboardMimesFromSystem();
                out.put("mixed.mimes", mimes == null ? null : String.join("|", mimes));
            } finally {
                thread.setUncaughtExceptionHandler(previous);
            }
            out.put("mixed.reported", String.join(",", reported));
            return null;
        });
    }

    /** Runs in {@link GtkGlassChild}: reads what the other one pushed, then pushes its own. */
    static void consumerScenario(Map<String, String> out) throws Exception {
        Path exchange = Path.of(System.getProperty(EXCHANGE_PROPERTY));
        await(exchange.resolve("producer.pushed"));
        out.put("ownerBeforePush", GtkGlassChild.onFx(() -> Boolean.toString(GtkGlassShim.clipboardIsOwner())));
        readEverything(out);
        Files.writeString(exchange.resolve("consumer.read"), "1");
        GtkGlassChild.onFx(() -> {
            GtkGlassShim.clipboardPushToSystem(content(), 0);
            return null;
        });
        out.put("ownerAfterPush", GtkGlassChild.onFx(() -> Boolean.toString(GtkGlassShim.clipboardIsOwner())));
        Files.writeString(exchange.resolve("consumer.pushed"), "1");
        await(exchange.resolve("producer.read"));

        await(exchange.resolve("producer.mixed"));
        readMixedUriList(out);
        Files.writeString(exchange.resolve("consumer.mixed"), "1");
    }

    /**
     * Runs in {@link GtkGlassChild}: everything that needs only this process - the owner-change notification,
     * {@code dispose} and {@code init}, the empty clipboard and the two methods that do nothing.
     */
    static void singleProcessScenario(Map<String, String> out) throws Exception {
        List<String> changes = new ArrayList<>();
        GtkGlassChild.onFx(() -> {
            new ClipboardAssistance(com.sun.glass.ui.Clipboard.SYSTEM) {
                @Override
                public void contentChanged() {
                    changes.add("x");
                }
            };
            GtkGlassShim.clipboardPushToSystem(content(), 0);
            return null;
        });
        GtkGlassChild.waitFor(10_000, () -> !changes.isEmpty());
        out.put("contentChanged.afterPush", Integer.toString(changes.size()));

        GtkGlassChild.onFx(() -> {
            GtkGlassShim.clipboardDispose();
            GtkGlassShim.clipboardPushToSystem(content(), 0);
            return null;
        });
        Thread.sleep(1000);
        out.put("contentChanged.afterDispose", Integer.toString(changes.size()));

        GtkGlassChild.onFx(() -> {
            GtkGlassShim.clipboardInit();
            GtkGlassShim.clipboardPushToSystem(content(), 0);
            return null;
        });
        GtkGlassChild.waitFor(10_000,
                () -> changes.size() > Integer.parseInt(out.get("contentChanged.afterDispose")));
        out.put("contentChanged.afterReinit", Integer.toString(changes.size()));

        GtkGlassChild.onFx(() -> {
            out.put("supportedSourceActions", Integer.toString(GtkGlassShim.clipboardSupportedSourceActions()));
            GtkGlassShim.clipboardPushTargetAction(1);
            out.put("pushTargetAction.returned", "true");
            GtkGlassShim.clipboardPushToSystem(new HashMap<>(), 0);
            return null;
        });
        Thread.sleep(500);
        GtkGlassChild.onFx(() -> {
            String[] mimes = GtkGlassShim.clipboardMimesFromSystem();
            out.put("empty.mimes", mimes == null ? null : String.join("|", mimes));
            for (String mime : List.of("text/plain", "text/html", "text/uri-list",
                    "application/x-java-file-list", "application/x-java-rawimage", CUSTOM_MIME)) {
                Object value = GtkGlassShim.clipboardPopFromSystem(mime);
                out.put("empty.pop." + mime, value == null ? null : describe(value));
            }
            return null;
        });
    }

    private static String describe(Object value) {
        if (value instanceof String[] array) {
            return Arrays.toString(array);
        }
        if (value instanceof ByteBuffer buffer) {
            return hex(buffer);
        }
        return value.toString();
    }

    /** Waits up to two minutes for the other child to create {@code marker}. */
    private static void await(Path marker) throws Exception {
        if (!GtkGlassChild.waitFor(120_000, () -> Files.isRegularFile(marker))) {
            throw new IllegalStateException("the other child JVM never created " + marker);
        }
    }

    private static String hex(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }

}
