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

import com.sun.glass.ui.gtk.screencast.ScreencastShim;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The screen capture binding against the glass GTK library this build produced: the version and layout of
 * {@code native-glass/gtk/screencast_api.h} it was written for, one bound symbol per export, what
 * {@code sc_load_pipewire} answers for a method it does not know and for one it does, and the restore token from
 * the library's callback table through to the file {@code TokenStorage} keeps.
 * <p>
 * Both scenarios run in a child JVM with {@code user.home} in this build's {@code target}, so that a test never
 * writes into the tokens of the account that runs it. The portal itself is not reachable here: what the calls
 * answer once a session exists needs a Wayland desktop with an xdg-desktop-portal.
 */
@EnabledOnOs(OS.LINUX)
@Timeout(600)
public class GtkScreencastNativeTest {

    /** The {@code user.home} of the children: {@code TokenStorage} keeps its properties file under it. */
    private static final Path HOME = Path.of("target", "gtk-screencast-home").toAbsolutePath();

    /** Where {@code TokenStorage} puts that file for the screen capture method. */
    private static final String TOKENS = ".java/robot/screencast-tokens.properties";

    /** {@code SC_UPCALL_OK} and {@code SC_UPCALL_THREW}, as the stub answers them. */
    private static final String UPCALL_OK = "0";
    private static final String UPCALL_THREW = "1";

    /** The screens of the first token: two rectangles, as the C lays them out. */
    private static final int[] TWO_SCREENS = {11, 22, 333, 444, 5, 6, 77, 88};

    /** The screens of the second: one rectangle. */
    private static final int[] ONE_SCREEN = {0, 0, 1920, 1080};

    /** The token of the slot dialled on a thread of the JVM's own making, and the screen of that session. */
    private static final String DAEMON_TOKEN = "daemon-thread-token";
    private static final int[] DAEMON_SCREEN = {7, 8, 9, 10};

    /** A token with characters outside ASCII, written as code units so that this file stays ASCII. */
    private static final String NON_ASCII = new String(new char[] {'c', 'a', 'f', 0x00E9, '-', 0x20AC, 0x00DF});

    /** Every symbol the binding must bind, in the order it binds them. */
    private static final List<String> SYMBOLS = List.of(
            "libc.so.6!strlen",
            "libglassgtk3.so!sc_abi_version",
            "libglassgtk3.so!sc_sizeof_token_callbacks",
            "libglassgtk3.so!sc_set_token_callbacks",
            "libglassgtk3.so!sc_load_pipewire",
            "libglassgtk3.so!sc_init_xdg_desktop_portal",
            "libglassgtk3.so!sc_close_session",
            "libglassgtk3.so!sc_get_rgb_pixels",
            "libglassgtk3.so!sc_remote_desktop_mouse_move",
            "libglassgtk3.so!sc_remote_desktop_mouse_button",
            "libglassgtk3.so!sc_remote_desktop_mouse_wheel",
            "libglassgtk3.so!sc_remote_desktop_key");

    private static GtkGlassChildJvm.Run library;
    private static GtkGlassChildJvm.Run token;

    @BeforeAll
    @Timeout(2 * GtkGlassChildJvm.BEFORE_ALL_SECONDS)
    static void runScenarios() {
        GtkGlassChildJvm.requireDisplay();
        prepareHome();
        List<String> options = List.of("-Duser.home=" + HOME, "-Djavafx.robot.screenshotDebug=true");
        library = GtkGlassChildJvm.run(GtkScreencastNativeTest.class, "libraryScenario", options);
        token = GtkGlassChildJvm.runWithoutToolkit(GtkScreencastNativeTest.class, "tokenScenario", options, false);
    }

    /** The library answers the ABI version this build was written for, and lays the table out as it does. */
    @Test
    public void theLibraryHasTheAbiTheBindingWasWrittenFor() {
        assertEquals(value(library, "abi.java"), value(library, "abi.library"), library::describe);
        assertEquals(value(library, "sizeof.java"), value(library, "sizeof.library"), library::describe);
        assertEquals("8", value(library, "sizeof.library"), library::describe);
    }

    /** One bound symbol per export of the header, and the C string length the token codec needs. */
    @Test
    public void everyFunctionOfTheHeaderIsBound() {
        assertEquals(SYMBOLS, List.of(value(library, "bound").split(",")), library::describe);
    }

    /**
     * {@code sc_load_pipewire} answers {@code false} for a method it does not know, before anything else, and
     * {@code false} for a known one where the PipeWire library is missing - which is what makes
     * {@code ScreencastHelper} report itself unavailable.
     */
    @Test
    public void loadPipewireAnswersFalseWithoutPipewire() {
        assertEquals("false", value(library, "load.unknownMethod"), library::describe);
        assertEquals("false", value(library, "load.screencast"), library::describe);
        assertEquals("false", value(library, "available"), library::describe);
        assertEquals("false", value(library, "helper.native"), library::describe);
    }

    /** The token callback table is handed over once, where the JNI looked its class and method up. */
    @Test
    public void theTokenCallbackTableIsInstalled() {
        assertEquals("false", value(library, "installed.before"), library::describe);
        assertEquals("true", value(library, "installed.after"), library::describe);
        assertEquals("true", value(library, "installed.again"), library::describe);
    }

    /**
     * A token the library hands the slot reaches {@code TokenStorage} as the {@code String} the JNI built with
     * {@code NewStringUTF} and is written to its file, with the screen bounds of the session: the two tokens the
     * scenario stores, and neither the one it passed no bounds for (which {@code TokenStorage} drops) nor the
     * empty one (which it rejects).
     */
    @Test
    public void theTokenReachesTheStorageAsTheJniBuiltIt() {
        assertEquals(UPCALL_OK, value(token, "status.pair"), token::describe);
        assertEquals(UPCALL_OK, value(token, "status.nonAscii"), token::describe);
        assertEquals(UPCALL_OK, value(token, "status.nullBounds"), token::describe);
        assertEquals("3", value(token, "stored.count"), token::describe);
        assertEquals(hex(NON_ASCII) + "=_0_0_1920_1080", value(token, "stored.0"), token::describe);
        assertEquals(hex(DAEMON_TOKEN) + "=_7_8_9_10", value(token, "stored.1"), token::describe);
        assertEquals(hex("new-token") + "=_11_22_333_444_5_6_77_88", value(token, "stored.2"), token::describe);
        assertTrue(lines(token.stdout()).contains("// storeToken old: |old-token| new |new-token| "
                + "allowed bounds " + Arrays.toString(TWO_SCREENS)), token::describe);
    }

    /**
     * The declared behaviour difference of this binding, as far as a Java test reaches it: the slot dialled on a
     * thread the JVM made for it, with no context class loader, stores its token like any other. Commit
     * {@code 033187ad90} asked {@code glass_jvm} for the {@code JNIEnv} of the thread the portal answered on and
     * dropped the token when there was none; an upcall stub attaches such a thread instead, so
     * {@code TokenStorage.storeTokenFromNative} - a lock and a file write - runs where nothing used to be
     * written. A thread the JVM has never seen cannot be made from Java; that the stub attaches one is the C
     * probe's measurement, and a portal session is still the outstanding acceptance.
     */
    @Test
    public void theTokenIsStoredFromAThreadTheJvmMade() {
        assertEquals(UPCALL_OK, value(token, "status.daemonThread"), token::describe);
        assertEquals("true", value(token, "daemonThread.daemon"), token::describe);
        assertEquals("null", value(token, "daemonThread.contextClassLoader"), token::describe);
        assertEquals(hex(DAEMON_TOKEN) + "=_7_8_9_10", value(token, "stored.1"), token::describe);
    }

    /**
     * A target that throws lets nothing out of the stub: the exception is described as JNI
     * {@code ExceptionDescribe} described it - {@code Exception in thread "<name>" } and the stack trace on the
     * error stream - the slot answers {@code SC_UPCALL_THREW}, and the C goes on, as it went on with the
     * exception that macro had cleared.
     */
    @Test
    public void aThrowingTargetIsDescribedAndAnsweredWithTheStatus() {
        assertEquals(UPCALL_THREW, value(token, "status.empty"), token::describe);
        String stderr = new String(token.stderrBytes(), StandardCharsets.ISO_8859_1);
        assertTrue(stderr.contains("Exception in thread \"main\" java.lang.RuntimeException: empty or null tokens"
                + " are not allowed"), token::describe);
        assertTrue(stderr.contains("TokenItem"), token::describe);
        assertEquals(0, token.exitCode(), token::describe);
    }

    // ---------------------------------------------------------------------------------------------
    // The scenarios (child JVMs)
    // ---------------------------------------------------------------------------------------------

    /** Runs in {@link GtkGlassChild} with the toolkit started for it, so the glass GTK library is loaded. */
    static void libraryScenario(Map<String, String> out) {
        out.put("abi.library", Integer.toString(ScreencastShim.abiVersion()));
        out.put("abi.java", String.valueOf(ScreencastShim.constants().get("ABI_VERSION")));
        out.put("sizeof.library", Integer.toString(ScreencastShim.sizeofTokenCallbacks()));
        out.put("sizeof.java", Long.toString(ScreencastShim.tokenCallbacksLayoutSize()));
        out.put("installed.before", Boolean.toString(ScreencastShim.tokenCallbacksInstalled()));
        out.put("load.unknownMethod", Boolean.toString(ScreencastShim.loadPipewire(99, false)));
        out.put("load.screencast", Boolean.toString(
                ScreencastShim.loadPipewire(ScreencastShim.constants().get("METHOD_SCREENCAST"), false)));
        ScreencastShim.installTokenCallbacks();
        out.put("installed.after", Boolean.toString(ScreencastShim.tokenCallbacksInstalled()));
        ScreencastShim.installTokenCallbacks();
        out.put("installed.again", Boolean.toString(ScreencastShim.tokenCallbacksInstalled()));
        out.put("bound", String.join(",", ScreencastShim.boundSymbols()));
        out.put("helper.native", Boolean.toString(ScreencastShim.helperUsesNativeMethods()));
        out.put("available", Boolean.toString(ScreencastShim.helperIsAvailable()));
    }

    /**
     * Runs in {@link GtkGlassChild} without a toolkit and without a display: the token stub needs no library, and
     * this child's {@code user.home} is the one {@link #HOME} names.
     */
    static void tokenScenario(Map<String, String> out) throws IOException {
        out.put("home", System.getProperty("user.home"));
        out.put("status.pair", Integer.toString(ScreencastShim.fireStoreToken(utf8("old-token"),
                utf8("new-token"), TWO_SCREENS, TWO_SCREENS.length)));
        out.put("status.nonAscii", Integer.toString(ScreencastShim.fireStoreToken(null, utf8(NON_ASCII),
                ONE_SCREEN, ONE_SCREEN.length)));
        out.put("status.nullBounds", Integer.toString(ScreencastShim.fireStoreToken(utf8("new-token"),
                utf8("no-bounds-token"), null, 0)));
        storeFromADaemonThread(out);
        out.put("status.empty", Integer.toString(ScreencastShim.fireStoreToken(null, utf8(""), ONE_SCREEN,
                ONE_SCREEN.length)));
        Properties stored = new Properties();
        Path file = Path.of(System.getProperty("user.home")).resolve(TOKENS);
        if (Files.isRegularFile(file)) {
            try (BufferedReader reader = Files.newBufferedReader(file)) {
                stored.load(reader);
            }
        }
        List<String> keys = new ArrayList<>(new TreeSet<>(stored.stringPropertyNames()));
        keys.sort(Comparator.naturalOrder());
        out.put("stored.count", Integer.toString(keys.size()));
        for (int i = 0; i < keys.size(); i++) {
            out.put("stored." + i, hex(keys.get(i)) + "=" + stored.getProperty(keys.get(i)));
        }
    }

    /**
     * The slot dialled on a thread the JVM made, with no context class loader: the shape an upcall stub gives
     * {@code TokenStorage.storeTokenFromNative} when the portal answers on a thread commit {@code 033187ad90}
     * had no {@code JNIEnv} for - where it printed {@code !!! Could not get env} and dropped the token. The lock
     * and the file write of that method must work from there too. It is not the same thing as a thread the JVM
     * has never seen, which no Java test can make; what the stub does with one is measured in the C.
     */
    private static void storeFromADaemonThread(Map<String, String> out) {
        int[] status = new int[1];
        Thread thread = Thread.ofPlatform().daemon().name("sc-token-daemon").unstarted(
                () -> status[0] = ScreencastShim.fireStoreToken(null, utf8(DAEMON_TOKEN), DAEMON_SCREEN,
                        DAEMON_SCREEN.length));
        thread.setContextClassLoader(null);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        out.put("status.daemonThread", Integer.toString(status[0]));
        out.put("daemonThread.contextClassLoader", String.valueOf(thread.getContextClassLoader()));
        out.put("daemonThread.daemon", Boolean.toString(thread.isDaemon()));
    }

    // ---------------------------------------------------------------------------------------------
    // Support
    // ---------------------------------------------------------------------------------------------

    /** The bytes the binding hands the C for {@code token}, without the terminator the caller adds. */
    private static byte[] utf8(String token) {
        byte[] bytes = ScreencastShim.tokenBytes(token);
        return Arrays.copyOf(bytes, bytes.length - 1);
    }

    /** The code units of {@code text} in hex, so that a value file and a failure name them, never print them. */
    private static String hex(String text) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            out.append(HexFormat.of().toHexDigits(text.charAt(i)));
        }
        return out.toString();
    }

    private static void prepareHome() {
        try {
            if (Files.exists(HOME)) {
                try (var walk = Files.walk(HOME)) {
                    for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                        Files.deleteIfExists(path);
                    }
                }
            }
            Files.createDirectories(HOME);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String value(GtkGlassChildJvm.Run run, String key) {
        String value = run.values().get(key);
        if (value == null) {
            throw new AssertionError("the child recorded no " + key + ": " + run.describe());
        }
        return value;
    }

    private static List<String> lines(Path file) {
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
