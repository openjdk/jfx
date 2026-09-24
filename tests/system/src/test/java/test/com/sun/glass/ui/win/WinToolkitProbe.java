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
package test.com.sun.glass.ui.win;

import com.sun.glass.ui.Application;
import com.sun.glass.ui.Screen;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.module.ResolvedModule;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import test.util.Util;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.abort;

/**
 * What the screen and preferences toolkit tests of this package share: the parity-oracle gate, reflective access to
 * {@code com.sun.glass.ui.win.WinGlassNativeShim}, a provenance line naming the code under test, and the screen
 * comparison of {@code WinScreenStartupParityTest} and {@code WinScreenScaledParityTest}.
 * <p>
 * <b>Why the shim is reached reflectively.</b> This module compiles against the javafx jars installed in
 * the local Maven repository, which need not contain the graphics shim at all, while its test JVM puts
 * {@code modules/javafx.graphics/target/shims/javafx.graphics} on {@code --upgrade-module-path}. The
 * package {@code com.sun.glass.ui.win} belongs to that named module, so at run time the shim resolves
 * from the current build whatever the installed jar holds. A shim class or method that is missing is a
 * broken test setup, not an unavailable platform feature, and fails naming what is missing.
 * <p>
 * <b>The gate.</b> {@link #requireOracle} follows the graphics module's {@code ParityGate}, which is not
 * on this module's class path: an oracle that cannot run is a skip that names what is missing, or a
 * failure under {@code -Djfx.parity.require=true} on the machine that owns the oracle.
 */
final class WinToolkitProbe {

    static final String REQUIRE_PROPERTY = "jfx.parity.require";

    /** How often a screen A/B is repeated because the display changed under it; then it fails. */
    static final int MAX_SCREEN_RETRIES = 3;

    /** {@code Screen}'s own override of both resolutions ({@code Screen.java:41-43}). */
    static final String SCREEN_DPI_PROPERTY = "com.sun.javafx.screenDPI";

    private static final String SHIM = "com.sun.glass.ui.win.WinGlassNativeShim";
    private static final String WIN_APPLICATION = "com.sun.glass.ui.win.WinApplication";
    private static final String GLASS_LIBRARY = "glass.dll";
    private static final int MAX_PATH_CHARS = 32768;
    private static final int S_OK = 0;
    private static final Class<?>[] NO_PARAMETERS = new Class<?>[0];

    private WinToolkitProbe() {
    }

    /**
     * Skips the calling test with {@code whatIsMissing} when {@code available} is false, or fails it when
     * {@code -Djfx.parity.require=true} says this machine owns the oracle.
     */
    static void requireOracle(Class<?> testClass, boolean available, Supplier<String> whatIsMissing) {
        if (available) {
            return;
        }
        String missing = whatIsMissing.get();
        if (Boolean.getBoolean(REQUIRE_PROPERTY)) {
            fail("-D" + REQUIRE_PROPERTY + "=true says this machine owns the oracle of "
                    + testClass.getSimpleName() + ", and it could not run: " + missing);
        }
        abort(missing);
    }

    /** {@code WinGlassNative.GWIN_PT_ALL}, through the shim's constant table. */
    static int preferenceTypeAll() {
        return (Integer) callShim("constant", new Class<?>[] {String.class}, "GWIN_PT_ALL");
    }

    /** {@code WinPreferences.collect(types)}: the Java arm of the preference map. Toolkit thread only. */
    @SuppressWarnings("unchecked")
    static Map<String, Object> collectPreferences(int types) {
        return (Map<String, Object>) callShim("collect", new Class<?>[] {int.class}, types);
    }

    /** {@code gwin_prefs_query_ui_settings(...).available}: the WinRT {@code UISettings} object exists. */
    static boolean uiSettingsAvailable() {
        return (Boolean) recordComponent(callShim("uiSettings", NO_PARAMETERS), "available");
    }

    /** {@code gwin_prefs_query_network(...).available}: the WinRT {@code NetworkInformation} factory exists. */
    static boolean networkInfoAvailable() {
        return (Boolean) recordComponent(callShim("networkInfo", NO_PARAMETERS), "available");
    }

    /** Whether {@code WinApplication}'s static initializer installed the {@code GwinScreenCallbacks} table. */
    static boolean screenCallbacksInstalled() {
        return (Boolean) callShim("screenCallbacksInstalled", NO_PARAMETERS);
    }

    /** {@code WinScreenLayout.arrange(WinGlassNative.collectMonitors(), WinApplication.overrideUIScale)}. */
    static Screen[] screensThroughJava() {
        return (Screen[]) callShim("screensThroughJava", NO_PARAMETERS);
    }

    /** What {@code WinGlassNative.collectMonitors()} returns now, one record's text per monitor. */
    @SuppressWarnings("unchecked")
    static List<String> collectedMonitors() {
        return (List<String>) callShim("collectedMonitors", NO_PARAMETERS);
    }

    /** {@code WinApplication.overrideUIScale}, which its static initializer read from {@code glass.win.uiScale}. */
    static float javaUiScaleOverride() {
        return (Float) callShim("uiScaleOverride", NO_PARAMETERS);
    }

    /** Whether the facade resolved {@code shcore}'s three DPI functions, i.e. which arm its enumeration took. */
    static boolean dpiFunctionsResolved() {
        return (Boolean) callShim("dpiFunctionsResolved", NO_PARAMETERS);
    }

    /** {@code {hresult, value}} of {@code shcore!GetProcessDpiAwareness(NULL, &value)}, through the shim. */
    static int[] processDpiAwareness() {
        return (int[]) callShim("processDpiAwareness", NO_PARAMETERS);
    }

    /** Whether {@code WinApplication.name(parameterTypes)} is still a JNI method in the code under test. */
    static boolean isWinApplicationNative(String name, Class<?>... parameterTypes) {
        try {
            Method method = Class.forName(WIN_APPLICATION).getDeclaredMethod(name, parameterTypes);
            return Modifier.isNative(method.getModifiers());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(WIN_APPLICATION + "." + name + " is not declared in the code under test", e);
        }
    }

    /**
     * One line per fact that says which build a run exercised: the {@code glass.dll} this process really
     * mapped (by module handle, not by search path) with its md5 and modification time, the ABI version that
     * library answers, the location javafx.graphics and the shim were loaded from, the toolkit class, and
     * whether each formerly native method of {@code WinApplication} it asks about is still JNI, Java, or no longer
     * declared.
     */
    static String provenance() {
        StringBuilder text = new StringBuilder();
        Path glass = loadedModulePath(GLASS_LIBRARY);
        text.append("glass.dll loaded: ").append(glass).append('\n');
        try {
            text.append("glass.dll md5: ").append(md5(glass))
                    .append(" size ").append(Files.size(glass))
                    .append(" mtime ").append(Files.getLastModifiedTime(glass)).append('\n');
        } catch (IOException e) {
            throw new AssertionError("cannot read " + glass, e);
        }
        text.append("glass abi: gwin_abi_version()=").append(callShim("abiVersion", NO_PARAMETERS))
                .append(" WinGlassNative.ABI_VERSION=").append(callShim("expectedAbiVersion", NO_PARAMETERS))
                .append('\n');
        text.append("java.library.path: ").append(System.getProperty("java.library.path")).append('\n');
        text.append("javafx.graphics module: ").append(moduleLocation("javafx.graphics")).append('\n');
        text.append("shim: ").append(shimClass().getProtectionDomain().getCodeSource().getLocation())
                .append(" collect(int) ").append(shimMethod("collect", int.class)).append('\n');
        text.append("toolkit: ").append(Application.GetApplication().getClass().getName()).append('\n');
        text.append("native: getPlatformPreferences=").append(winApplicationMethodKind("getPlatformPreferences"))
                .append(" _init=").append(winApplicationMethodKind("_init", int.class))
                .append(" _setClassLoader=").append(winApplicationMethodKind("_setClassLoader", ClassLoader.class))
                .append(" initIDs=").append(winApplicationMethodKind("initIDs", float.class))
                .append(" staticScreen_getScreens=").append(winApplicationMethodKind("staticScreen_getScreens"));
        return text.toString();
    }

    /** What a started toolkit's screen state was before any screen A/B ran. */
    record ScreenStartup(List<Screen> screens, int[] awareness, float javaOverride) {
    }

    /**
     * Reads, in one toolkit-thread runnable right after {@code Util.startup}, the {@code Screen.getScreens()} list
     * the toolkit built, {@code GetProcessDpiAwareness}, {@code WinApplication.overrideUIScale} and the
     * provenance, and prints all four.
     */
    static ScreenStartup screenStartup(String testName) {
        AtomicReference<ScreenStartup> startup = new AtomicReference<>();
        AtomicReference<String> provenance = new AtomicReference<>();
        Util.runAndWait(() -> {
            startup.set(new ScreenStartup(Screen.getScreens(), processDpiAwareness(), javaUiScaleOverride()));
            provenance.set(provenance());
        });
        ScreenStartup state = startup.get();
        System.out.println(testName + " provenance\n" + provenance.get());
        System.out.print(testName + " startup: WinApplication.overrideUIScale=" + state.javaOverride()
                + " (" + bits(state.javaOverride()) + "), GetProcessDpiAwareness {hresult, value}="
                + describe(state.awareness()) + ", Screen.getScreens() identity "
                + System.identityHashCode(state.screens()) + "\n"
                + screenTable(state.screens().toArray(new Screen[0])));
        return state;
    }

    /** The two tables a screen comparison found equal, and how it got there. */
    record ScreenParity(Screen[] startup, Screen[] viaJava, int attempts, boolean startupInstance) {
    }

    /**
     * The screen wiring check on a started toolkit. In one toolkit-thread runnable: {@code Screen.getScreens()},
     * {@link #collectedMonitors()} for the log, {@link #screensThroughJava()} twice and {@code Screen.getScreens()}
     * again, with a {@code GetProcessDpiAwareness} reading before the first enumeration and after each. A second
     * runnable reads {@code Screen.getScreens()} once more, after any display message that was pending has been
     * dispatched.
     * <p>
     * The bracket is compared only if nothing moved under it: the two Java tables are equal and all three reads of
     * {@code Screen.getScreens()} are the same instance ({@code Screen.notifySettingsChanged} replaces it).
     * Otherwise it is repeated, at most {@value #MAX_SCREEN_RETRIES} more times, and then fails - it never passes.
     * A stable bracket must satisfy {@code Screen.getScreens() == viaJava} on every one of the twenty constructor
     * values, {@code nativeScreen} first (the enumeration order), floats by their raw bits; the startup screens must
     * be at least one, with distinct non-zero {@code HMONITOR}s, positive sizes and positive scales; and every
     * {@code GetProcessDpiAwareness} reading must succeed and equal the one taken at startup.
     * <p>
     * Before the JNI was removed this bracket had a third arm, {@code staticScreen_getScreens()} called again
     * while it was still {@code GlassScreen::CreateJavaScreens}; the classes that use it record that comparison.
     */
    static ScreenParity compareScreenArms(String testName, ScreenStartup startup) {
        for (int attempt = 1; attempt <= 1 + MAX_SCREEN_RETRIES; attempt++) {
            String label = testName + " attempt " + attempt;
            AtomicReference<ScreenBracket> captured = new AtomicReference<>();
            Util.runAndWait(() -> captured.set(captureScreenBracket()));
            AtomicReference<List<Screen>> flushed = new AtomicReference<>();
            Util.runAndWait(() -> flushed.set(Screen.getScreens()));
            ScreenBracket bracket = captured.get();
            print(label, bracket, flushed.get());

            for (int i = 0; i < bracket.awareness().size(); i++) {
                int[] reading = bracket.awareness().get(i);
                String which = label + ": GetProcessDpiAwareness reading " + i + " " + describe(reading);
                assertEquals(S_OK, reading[0], which + " failed");
                assertEquals(startup.awareness()[1], reading[1], which + " is not the startup value "
                        + describe(startup.awareness()) + ": an enumeration's SetProcessDpiAwareness was not refused,"
                        + " so the tables describe a different process");
            }
            assertNotNull(bracket.viaJava(), label + ": WinScreenLayout.arrange returned null (no monitor)");
            assertNotNull(bracket.viaJavaAgain(), label + ": the second WinScreenLayout.arrange returned null");

            boolean sameList = bracket.listBefore() == bracket.listAfter() && bracket.listBefore() == flushed.get();
            if (!sameList || !screenTable(bracket.viaJava()).equals(screenTable(bracket.viaJavaAgain()))) {
                System.out.println(label + ": the display changed during the bracket (same Screen.getScreens()"
                        + " instance: " + sameList + "); retrying");
                continue;
            }

            Screen[] startupScreens = bracket.listBefore().toArray(new Screen[0]);
            assertValidScreens(label + ": Screen.getScreens()", startupScreens);
            assertSameScreens(label, "Screen.getScreens(), built by the toolkit", startupScreens,
                    "WinScreenLayout.arrange(collectMonitors(), overrideUIScale)", bracket.viaJava());
            boolean startupInstance = bracket.listBefore() == startup.screens();
            System.out.println(label + ": Screen.getScreens() == WinScreenLayout.arrange, " + startupScreens.length
                    + " screen(s), twenty values each; Screen.getScreens() is the instance read at startup: "
                    + startupInstance);
            return new ScreenParity(startupScreens, bracket.viaJava(), attempt, startupInstance);
        }
        return fail(testName + ": the display changed during all " + (1 + MAX_SCREEN_RETRIES)
                + " brackets; nothing was compared");
    }

    /** Fails unless {@code -Dcom.sun.javafx.screenDPI} is unset: it replaces both resolutions in every arm. */
    static void assertNoScreenDpiOverride() {
        assertTrue(Integer.getInteger(SCREEN_DPI_PROPERTY, 0) <= 0, "-D" + SCREEN_DPI_PROPERTY + "="
                + System.getProperty(SCREEN_DPI_PROPERTY) + " replaces both resolutions in Screen's constructor,"
                + " so no arm would show its own");
    }

    /**
     * The text of the module's screen snapshot goldens (captured by {@code WinScreenSnapshotTest}) and of the tables
     * its {@code WinScreenParityTest} writes: one line per screen, the twenty constructor arguments by getter, ints and
     * the long in decimal, floats as raw bits.
     */
    static String screenTable(Screen[] screens) {
        StringBuilder text = new StringBuilder(256 * screens.length);
        for (int i = 0; i < screens.length; i++) {
            text.append(screenLine(i, screens[i])).append('\n');
        }
        return text.toString();
    }

    static void writeScreenTable(Path file, Screen[] screens) {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.writeString(file, screenTable(screens), StandardCharsets.US_ASCII);
        } catch (IOException e) {
            throw new AssertionError("cannot write " + file.toAbsolutePath(), e);
        }
        System.out.println("wrote " + file.toAbsolutePath());
    }

    static String bits(float value) {
        return String.format("0x%08x", Float.floatToRawIntBits(value));
    }

    /** What one bracket of {@link #compareScreenArms} read on the toolkit thread. */
    private record ScreenBracket(List<Screen> listBefore, List<String> monitors, Screen[] viaJava,
            Screen[] viaJavaAgain, List<Screen> listAfter, List<int[]> awareness) {
    }

    private static ScreenBracket captureScreenBracket() {
        List<int[]> awareness = new ArrayList<>();
        List<Screen> listBefore = Screen.getScreens();
        awareness.add(processDpiAwareness());
        List<String> monitors = collectedMonitors();
        awareness.add(processDpiAwareness());
        Screen[] viaJava = screensThroughJava();
        awareness.add(processDpiAwareness());
        Screen[] viaJavaAgain = screensThroughJava();
        awareness.add(processDpiAwareness());
        List<Screen> listAfter = Screen.getScreens();
        return new ScreenBracket(listBefore, monitors, viaJava, viaJavaAgain, listAfter, awareness);
    }

    private static void print(String label, ScreenBracket bracket, List<Screen> flushed) {
        StringBuilder text = new StringBuilder();
        text.append(label).append(": GetProcessDpiAwareness {hresult, value} before and after each enumeration:");
        for (int[] reading : bracket.awareness()) {
            text.append(' ').append(describe(reading));
        }
        text.append('\n').append(label).append(": Screen.getScreens() identity ")
                .append(System.identityHashCode(bracket.listBefore())).append(" / after ")
                .append(System.identityHashCode(bracket.listAfter())).append(" / next runnable ")
                .append(System.identityHashCode(flushed)).append('\n');
        text.append(label).append(": (a) Screen.getScreens()\n")
                .append(screenTable(bracket.listBefore().toArray(new Screen[0])));
        text.append(label).append(": collectMonitors() ").append(bracket.monitors()).append('\n');
        text.append(label).append(": (c) WinScreenLayout.arrange(collectMonitors(), overrideUIScale)\n")
                .append(table(bracket.viaJava()));
        text.append(label).append(": (c') WinScreenLayout.arrange(collectMonitors(), overrideUIScale) once more\n")
                .append(table(bracket.viaJavaAgain()));
        System.out.print(text);
    }

    private static String table(Screen[] screens) {
        return screens == null ? "null\n" : screenTable(screens);
    }

    private static void assertValidScreens(String what, Screen[] screens) {
        assertTrue(screens.length >= 1, what + ": no screen");
        Set<Long> monitors = new HashSet<>();
        for (int i = 0; i < screens.length; i++) {
            Screen screen = screens[i];
            String which = what + ": " + screenLine(i, screen);
            assertNotEquals(0L, screen.getNativeScreen(), which + ": no HMONITOR");
            assertTrue(monitors.add(screen.getNativeScreen()), which + ": the HMONITOR of an earlier screen");
            assertTrue(screen.getWidth() > 0 && screen.getHeight() > 0, which + ": empty bounds");
            assertTrue(screen.getPlatformWidth() > 0 && screen.getPlatformHeight() > 0,
                    which + ": empty platform bounds");
            assertTrue(screen.getVisibleWidth() > 0 && screen.getVisibleHeight() > 0, which + ": empty work area");
            assertTrue(screen.getPlatformScaleX() > 0 && screen.getPlatformScaleY() > 0, which + ": platform scale");
            assertTrue(screen.getRecommendedOutputScaleX() > 0 && screen.getRecommendedOutputScaleY() > 0,
                    which + ": output scale");
        }
    }

    /** Equal length, then the {@code nativeScreen} sequence (the enumeration order), then every line. */
    private static void assertSameScreens(String label, String expectedName, Screen[] expected, String actualName,
            Screen[] actual) {
        String what = label + ": " + expectedName + " (expected) vs " + actualName + " (actual)";
        assertEquals(expected.length, actual.length, what + ": number of screens");
        assertEquals(nativeScreens(expected), nativeScreens(actual), what + ": nativeScreen in array order");
        for (int i = 0; i < expected.length; i++) {
            assertEquals(screenLine(i, expected[i]), screenLine(i, actual[i]), what + ": screen " + i);
        }
    }

    private static List<Long> nativeScreens(Screen[] screens) {
        List<Long> handles = new ArrayList<>(screens.length);
        for (Screen screen : screens) {
            handles.add(screen.getNativeScreen());
        }
        return handles;
    }

    /** The twenty arguments of {@code Screen}'s constructor, in its order, named after its getters. */
    private static String screenLine(int index, Screen screen) {
        if (screen == null) {
            return "screen " + index + " null";
        }
        return "screen " + index
                + " nativeScreen=" + screen.getNativeScreen()
                + " depth=" + screen.getDepth()
                + " x=" + screen.getX()
                + " y=" + screen.getY()
                + " width=" + screen.getWidth()
                + " height=" + screen.getHeight()
                + " platformX=" + screen.getPlatformX()
                + " platformY=" + screen.getPlatformY()
                + " platformWidth=" + screen.getPlatformWidth()
                + " platformHeight=" + screen.getPlatformHeight()
                + " visibleX=" + screen.getVisibleX()
                + " visibleY=" + screen.getVisibleY()
                + " visibleWidth=" + screen.getVisibleWidth()
                + " visibleHeight=" + screen.getVisibleHeight()
                + " resolutionX=" + screen.getResolutionX()
                + " resolutionY=" + screen.getResolutionY()
                + " platformScaleX=" + bits(screen.getPlatformScaleX())
                + " platformScaleY=" + bits(screen.getPlatformScaleY())
                + " outputScaleX=" + bits(screen.getRecommendedOutputScaleX())
                + " outputScaleY=" + bits(screen.getRecommendedOutputScaleY());
    }

    private static String describe(int[] hresultAndValue) {
        return String.format("{0x%08x, %d}", hresultAndValue[0], hresultAndValue[1]);
    }

    /**
     * {@code native} or {@code java} for a method {@code WinApplication} declares, {@code absent} for one it no
     * longer declares - so the provenance line still prints once a flip deletes the method.
     */
    static String winApplicationMethodKind(String name, Class<?>... parameterTypes) {
        Class<?> winApplication;
        try {
            winApplication = Class.forName(WIN_APPLICATION);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(WIN_APPLICATION + " is not loadable", e);
        }
        try {
            Method method = winApplication.getDeclaredMethod(name, parameterTypes);
            return Modifier.isNative(method.getModifiers()) ? "native" : "java";
        } catch (NoSuchMethodException e) {
            return "absent";
        }
    }

    private static Object callShim(String name, Class<?>[] parameterTypes, Object... arguments) {
        Method method = shimMethod(name, parameterTypes);
        try {
            return method.invoke(null, arguments);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new AssertionError(SHIM + "." + name + " threw", cause);
        } catch (IllegalAccessException e) {
            throw new AssertionError(SHIM + "." + name + " is not accessible; is com.sun.glass.ui.win exported?", e);
        }
    }

    private static Method shimMethod(String name, Class<?>... parameterTypes) {
        try {
            return shimClass().getMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(SHIM + " has no public " + name + " - the graphics shims on the module path"
                    + " predate the oracle this test needs (" + moduleLocation("javafx.graphics") + ")", e);
        }
    }

    private static Class<?> shimClass() {
        try {
            return Class.forName(SHIM);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(SHIM + " is not loadable - javafx.graphics was not taken from the shims"
                    + " directory (" + moduleLocation("javafx.graphics") + ")", e);
        }
    }

    private static Object recordComponent(Object record, String accessor) {
        try {
            return record.getClass().getMethod(accessor).invoke(record);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(record.getClass().getName() + " has no accessor " + accessor, e);
        }
    }

    private static String moduleLocation(String moduleName) {
        return ModuleLayer.boot().configuration().findModule(moduleName)
                .map(ResolvedModule::reference)
                .flatMap(reference -> reference.location())
                .map(Object::toString)
                .orElse("<" + moduleName + " not in the boot layer>");
    }

    private static String md5(Path file) throws IOException {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(Files.readAllBytes(file)));
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    /** {@code GetModuleFileNameW(GetModuleHandleW(name))}: the file this process mapped for {@code name}. */
    private static Path loadedModulePath(String name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment module = (MemorySegment) Kernel32.GET_MODULE_HANDLE.invokeExact(
                    arena.allocateFrom(name, StandardCharsets.UTF_16LE));
            if (module.address() == 0L) {
                fail(name + " is not loaded in this process");
            }
            MemorySegment buffer = arena.allocate(JAVA_CHAR, MAX_PATH_CHARS);
            int length = (int) Kernel32.GET_MODULE_FILE_NAME.invokeExact(module, buffer, MAX_PATH_CHARS);
            if (length <= 0 || length >= MAX_PATH_CHARS) {
                fail("GetModuleFileNameW(" + name + ") answered " + length);
            }
            return Path.of(buffer.getString(0, StandardCharsets.UTF_16LE));
        } catch (Throwable t) {
            if (t instanceof Error error) {
                throw error;
            }
            throw new AssertionError(t);
        }
    }

    /** The two kernel32 symbols of {@link #loadedModulePath}, bound on first use. */
    private static final class Kernel32 {

        /** {@code HMODULE GetModuleHandleW(LPCWSTR lpModuleName)}. */
        static final MethodHandle GET_MODULE_HANDLE = bind("GetModuleHandleW",
                FunctionDescriptor.of(ADDRESS, ADDRESS));

        /** {@code DWORD GetModuleFileNameW(HMODULE hModule, LPWSTR lpFilename, DWORD nSize)}. */
        static final MethodHandle GET_MODULE_FILE_NAME = bind("GetModuleFileNameW",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT));

        private Kernel32() {
        }

        @SuppressWarnings("restricted")
        private static MethodHandle bind(String symbol, FunctionDescriptor descriptor) {
            MemorySegment address = SymbolLookup.libraryLookup("kernel32.dll", Arena.global()).find(symbol)
                    .orElseThrow(() -> new UnsatisfiedLinkError("kernel32.dll has no " + symbol));
            return Linker.nativeLinker().downcallHandle(address, descriptor);
        }
    }
}
