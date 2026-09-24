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

import com.sun.glass.ui.gtk.screencast.ScreencastHelper;
import com.sun.glass.ui.gtk.screencast.ScreencastShim;
import com.sun.glass.ui.gtk.screencast.XdgDesktopPortal;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The parts of the screen capture binding that need no desktop portal and no glass GTK library: the constants it
 * shares with {@code native-glass/gtk/screencast_api.h}, the restore token's codec, the native staging copies the
 * blocking calls hand the C, the {@link ArrayIndexOutOfBoundsException} a rejected row copy becomes, and what
 * {@code ScreencastHelper} does when the natives were never loaded.
 * <p>
 * The portal paths themselves - a capture, a remote-desktop action, a token that comes back from a Start
 * response - need a Wayland session with an xdg-desktop-portal and are not reachable here.
 */
@EnabledOnOs(OS.LINUX)
@Timeout(600)
public class GtkScreencastHeadlessTest {

    /** {@code screencast_api.h}, relative to the module directory: the C side of the shared constants. */
    private static final String HEADER = "src/main/native-glass/gtk/screencast_api.h";

    /** The Java name of each shared constant and the macro of the header it must equal. */
    private static final Map<String, String> SHARED = Map.ofEntries(
            Map.entry("ABI_VERSION", "GLASS_SCREENCAST_ABI_VERSION"),
            Map.entry("OK", "SC_OK"),
            Map.entry("UPCALL_OK", "SC_UPCALL_OK"),
            Map.entry("UPCALL_THREW", "SC_UPCALL_THREW"),
            Map.entry("METHOD_SCREENCAST", "SC_METHOD_SCREENCAST"),
            Map.entry("METHOD_REMOTE_DESKTOP", "SC_METHOD_REMOTE_DESKTOP"),
            Map.entry("RESULT_OK", "SC_RESULT_OK"),
            Map.entry("RESULT_ERROR", "SC_RESULT_ERROR"),
            Map.entry("RESULT_DENIED", "SC_RESULT_DENIED"),
            Map.entry("RESULT_OUT_OF_BOUNDS", "SC_RESULT_OUT_OF_BOUNDS"),
            Map.entry("RESULT_NO_STREAMS", "SC_RESULT_NO_STREAMS"));

    /** The elements of one piece of a staging copy: {@code CHUNK_BYTES} of the binding, in ints. */
    private static final int CHUNK_ELEMENTS = (1 << 18) / 4;

    /** {@code javafx.robot.screenshotMethod}, which {@code XdgDesktopPortal} reads when it initializes. */
    private static final String METHOD_PROPERTY = "javafx.robot.screenshotMethod";

    /** The method that loads no screen capture native: {@code XdgDesktopPortal}'s {@code METHOD_GTK}. */
    private static final String METHOD_GTK = "gtk";

    /**
     * Keeps this JVM on the method that loads no screen capture native. {@code XdgDesktopPortal} picks
     * {@code dbusScreencast} or {@code dbusRemoteDesktop} whenever {@code WAYLAND_DISPLAY} is not blank - every
     * Wayland session, and every JVM started under WSLg - and {@code ScreencastHelper} then loads the natives,
     * which a JVM that has not started a toolkit cannot. The property is read once, when that class initializes,
     * so setting it here decides the method for this JVM; the property is not passed on to the child JVMs of the
     * other tests, which take only the command line of this one.
     */
    @BeforeAll
    static void keepThisJvmOffTheScreenCaptureNatives() {
        System.setProperty(METHOD_PROPERTY, METHOD_GTK);
    }

    /**
     * The constants the binding and the C share: the ABI version, the status of the callback slot, the method ids
     * of {@code loadPipewire} and the result codes. Both sides must be changed together, so this reads the macros
     * out of the header.
     */
    @Test
    public void theConstantsAreTheOnesTheCUses() {
        Map<String, Integer> header = headerConstants();
        assumeTrue(!header.isEmpty(), "no " + HEADER + " next to this build");
        Map<String, Integer> expected = new LinkedHashMap<>();
        Map<String, Integer> actual = new LinkedHashMap<>();
        Map<String, Integer> java = ScreencastShim.constants();
        for (Map.Entry<String, String> shared : new TreeMap<>(SHARED).entrySet()) {
            expected.put(shared.getValue(), header.get(shared.getValue()));
            actual.put(shared.getValue(), java.get(shared.getKey()));
        }
        assertEquals(expected, actual, "the Java constants and " + HEADER + " must be changed together");
    }

    /**
     * {@code ScreencastHelper}'s own result codes and method ids are those same values: it maps the answer of
     * every call to its debug output and its retry decisions by them.
     */
    @Test
    public void theHelperMapsTheSameResultCodes() {
        requireTheMethodThatLoadsNoNative();
        Map<String, Integer> binding = ScreencastShim.constants();
        Map<String, Integer> helper = ScreencastShim.helperConstants();
        assertEquals(binding.get("RESULT_ERROR"), helper.get("ERROR"));
        assertEquals(binding.get("RESULT_DENIED"), helper.get("DENIED"));
        assertEquals(binding.get("RESULT_OUT_OF_BOUNDS"), helper.get("OUT_OF_BOUNDS"));
        assertEquals(binding.get("RESULT_NO_STREAMS"), helper.get("NO_STREAMS"));
        assertEquals(binding.get("METHOD_SCREENCAST"), helper.get("XDG_METHOD_SCREENCAST"));
        assertEquals(binding.get("METHOD_REMOTE_DESKTOP"), helper.get("XDG_METHOD_REMOTE_DESKTOP"));
    }

    /**
     * The restore token crosses into the C as the bytes {@code GetStringUTFChars} produced: modified UTF-8,
     * NUL-terminated, and a {@code null} token as a {@code NULL} pointer. Every token the portal can answer with
     * comes back unchanged, U+0000 and unpaired surrogates included - a charset encode would lose both.
     */
    @Test
    public void theTokenCrossesAsModifiedUtf8() {
        assertNull(ScreencastShim.tokenBytes(null));
        assertNull(ScreencastShim.tokenRoundTrip(null));
        assertArrayEquals(new byte[] {0}, ScreencastShim.tokenBytes(""));
        assertEquals("", ScreencastShim.tokenRoundTrip(""));
        for (String token : List.of("token", "0123456789abcdef", withChars(0x00E9), withChars(0x20AC),
                withChars(0x0000), withChars(0xD83D, 0xDE00), withChars(0xD800), withChars(0xDFFF))) {
            assertEquals(hex(token), hex(ScreencastShim.tokenRoundTrip(token)),
                    () -> "the token did not come back as it went: " + hex(token));
        }
        // U+0000 is two bytes in modified UTF-8 (C0 80), where a charset encode would write one and end the
        // C string there.
        assertArrayEquals(new byte[] {'a', (byte) 0xC0, (byte) 0x80, 'b', 0},
                ScreencastShim.tokenBytes("a" + withChars(0x0000) + "b"));
    }

    /**
     * The pixels and the screen bounds cross as native copies, in pieces, and every element arrives - the call
     * blocks on the portal, so no array of the JVM's may be held across it.
     */
    @Test
    public void theStagingCopyMovesEveryElement() {
        int[] values = new int[3 * CHUNK_ELEMENTS + 7];
        for (int i = 0; i < values.length; i++) {
            values[i] = i * 31 + 7;
        }
        assertArrayEquals(values, ScreencastShim.stagingRoundTrip(values));
        assertEquals((long) values.length * 4, ScreencastShim.stagingByteSize(values));
        assertArrayEquals(new int[] {1, 2, 3, 4}, ScreencastShim.stagingRoundTrip(new int[] {1, 2, 3, 4}));
    }

    /**
     * An empty array is a pointer the C can tell from the {@code NULL} of a {@code null} array: the C branches on
     * the pointer and reads the length only when it is not {@code NULL}.
     */
    @Test
    public void anEmptyArrayIsNotANullPointer() {
        assertFalse(ScreencastShim.stagingIsNull(new int[0]));
        assertEquals(4L, ScreencastShim.stagingByteSize(new int[0]));
        assertArrayEquals(new int[0], ScreencastShim.stagingRoundTrip(new int[0]));
    }

    /**
     * A row that does not fit into the pixel array is not copied and becomes the exception
     * {@code SetIntArrayRegion} threw for it, in the two forms HotSpot uses - the last rejected row being the one
     * whose exception surfaced.
     */
    @Test
    public void aRejectedRowCopyIsTheExceptionSetIntArrayRegionThrew() {
        assertNull(ScreencastShim.rejectedRegionMessage(0, 0, 0, 8));
        assertEquals("Array region 8..12 out of bounds for length 8",
                ScreencastShim.rejectedRegionMessage(1, 8, 4, 8));
        assertEquals("Array region -1..3 out of bounds for length 8",
                ScreencastShim.rejectedRegionMessage(1, -1, 4, 8));
        assertEquals("Length -4 is negative", ScreencastShim.rejectedRegionMessage(1, 0, -4, 8));
        assertEquals("Array region 200..204 out of bounds for length 8",
                ScreencastShim.rejectedRegionMessage(2, 200, 4, 8));
    }

    /**
     * Where the natives never loaded - this JVM, and any X11 session with the default method - the helper reports
     * itself unavailable and every entry point returns without touching the C or the pixels it was given, which
     * is what makes {@code GtkRobot} fall back to XTest and GDK.
     */
    @Test
    public void theHelperDoesNothingWhenTheNativesAreNotLoaded() {
        requireTheMethodThatLoadsNoNative();
        assertFalse(ScreencastHelper.isAvailable());
        int[] pixels = {1, 2, 3, 4};
        ScreencastHelper.getRGBPixels(0, 0, 2, 2, pixels);
        assertArrayEquals(new int[] {1, 2, 3, 4}, pixels);
        ScreencastHelper.remoteDesktopMouseMove(10, 20);
        ScreencastHelper.remoteDesktopMouseButton(true, 1);
        ScreencastHelper.remoteDesktopMouseWheel(-1);
        ScreencastHelper.remoteDesktopKey(true, 65);
    }

    /** {@code clipRound}, which turns the FX screen bounds into the device pixels the C is given. */
    @Test
    public void theScreenBoundsAreRoundedAsBefore() {
        requireTheMethodThatLoadsNoNative();
        assertEquals(0, ScreencastHelper.clipRound(0.0));
        assertEquals(1, ScreencastHelper.clipRound(1.4));
        assertEquals(1, ScreencastHelper.clipRound(1.5));
        assertEquals(-2, ScreencastHelper.clipRound(-1.5));
        assertEquals(Integer.MIN_VALUE, ScreencastHelper.clipRound(-1e300));
        assertEquals(Integer.MAX_VALUE, ScreencastHelper.clipRound(1e300));
    }

    // ---------------------------------------------------------------------------------------------
    // Support
    // ---------------------------------------------------------------------------------------------

    /**
     * For the three tests that initialize {@code ScreencastHelper}: with any other method that class loads the
     * screen capture natives, which needs a glass GTK library this JVM has not loaded. A JVM where
     * {@code XdgDesktopPortal} was initialized before {@link #keepThisJvmOffTheScreenCaptureNatives} fails here
     * rather than skipping, because a class of binding tests that reports no test at all is the failure this
     * guards against.
     */
    private static void requireTheMethodThatLoadsNoNative() {
        assertEquals(METHOD_GTK, XdgDesktopPortal.getMethod(),
                "XdgDesktopPortal was initialized before this class set " + METHOD_PROPERTY + "; initializing"
                        + " ScreencastHelper here would load the screen capture natives");
    }

    /** A string of the given code units, written as numbers so that this file stays ASCII. */
    private static String withChars(int... units) {
        char[] chars = new char[units.length];
        for (int i = 0; i < units.length; i++) {
            chars[i] = (char) units[i];
        }
        return new String(chars);
    }

    /** The code units of {@code text} in hex, so that a failure names them instead of printing them. */
    private static String hex(String text) {
        if (text == null) {
            return "null";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            out.append(HexFormat.of().toHexDigits(text.charAt(i))).append(' ');
        }
        return out.toString().trim();
    }

    private static Map<String, Integer> headerConstants() {
        Path header = moduleDirectory().resolve(HEADER);
        if (!Files.isRegularFile(header)) {
            return Map.of();
        }
        Pattern define = Pattern.compile(
                "^#define\\s+(GLASS_SCREENCAST_ABI_VERSION|SC_\\w+)\\s+\\(?(-?\\d+)\\)?\\s*(?:/\\*.*)?$");
        Map<String, Integer> values = new LinkedHashMap<>();
        for (String line : lines(header)) {
            Matcher matcher = define.matcher(line);
            if (matcher.matches()) {
                values.put(matcher.group(1), Integer.valueOf(matcher.group(2)));
            }
        }
        return values;
    }

    private static List<String> lines(Path file) {
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path moduleDirectory() {
        Path dir = Path.of("").toAbsolutePath();
        if (Files.isDirectory(dir.resolve("src").resolve("main"))) {
            return dir;
        }
        return dir.resolve("modules").resolve("javafx.graphics");
    }
}
