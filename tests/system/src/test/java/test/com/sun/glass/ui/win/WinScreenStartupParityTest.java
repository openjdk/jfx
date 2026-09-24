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

import com.sun.javafx.PlatformUtil;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import test.util.Util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The screen table of a started Windows toolkit at this machine's own UI scale, two ways: {@code Screen.getScreens()},
 * the list the toolkit built at startup through {@code WinApplication.staticScreen_getScreens()}, and
 * {@code WinScreenLayout.arrange(WinGlassNative.collectMonitors(), WinApplication.overrideUIScale)} called again
 * through the graphics shim. They must agree on every one of the twenty {@code Screen} constructor values,
 * {@code nativeScreen} first, floats by their raw bits ({@link WinToolkitProbe#compareScreenArms}). Since ABI 5 the
 * two are the same Java, so this is a wiring check: it proves that the product path - {@code runLoop}'s
 * {@code gwin_app_create}, {@code Application.run}'s {@code Screen.initScreens} and the Java override of
 * {@code staticScreen_getScreens} - builds exactly the arrangement the module suite holds to the JNI's golden
 * ({@code WinScreenParityTest}), on a real toolkit thread with the process state a started toolkit leaves.
 * {@code WinScreenScaledParityTest} does the same with {@code glass.win.uiScale=1.75}.
 * <p>
 * <b>The evidence this class carried before it was a wiring check.</b> Until the JNI was deleted it was
 * {@code WinScreenParityTest} - renamed since, so that its name no longer clashes with the module's
 * class of the same package - and compared three arms: the startup list (built by the JNI), the JNI
 * {@code GlassScreen::CreateJavaScreens} called again through the shim, and the Java. On 2026-09-13, on
 * {@code glass.dll} md5 {@code 29e73f00e2e837d2930f958be2304098} (ABI 5), in two runs, all three were equal on
 * attempt 1: one screen, {@code nativeScreen} 65537, 1920x1080 at (0, 0), work area 1920x1040, resolution 81, every
 * scale {@code 0x3f800000}; the list compared was the instance the toolkit built at startup; every
 * {@code GetProcessDpiAwareness} reading was {@code {S_OK, 2}}; and the three tables were byte for byte the snapshot
 * the JNI implementation in commit {@code 8492cb03b0} wrote (md5 {@code b4a57d1e24dd20ba27b0305cda3e8060}), which the
 * module
 * suite keeps as {@code screen-snapshot-golden.txt}.
 * <p>
 * The tables still go to {@code target/} in that format, so a run can be compared with the golden by md5. Nothing here
 * moves the mouse or the keyboard; the only windows are the toolkit's own hidden ones.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WinScreenStartupParityTest {

    private static final String TEST_NAME = "WinScreenStartupParityTest";

    private static final String UI_SCALE_PROPERTY = "glass.win.uiScale";

    /** {@code WinApplication.overrideUIScale} when {@code glass.win.uiScale} is not set. */
    private static final float NO_OVERRIDE = -1.0f;

    private static final int S_OK = 0;

    private static final CountDownLatch startupLatch = new CountDownLatch(1);

    private static WinToolkitProbe.ScreenStartup startup;

    @BeforeAll
    static void initFX() {
        assumeTrue(PlatformUtil.isWindows());
        assertNull(System.getProperty(UI_SCALE_PROPERTY), "-D" + UI_SCALE_PROPERTY + " forces the scale this class"
                + " must read from the machine");
        assertNull(System.getenv(UI_SCALE_PROPERTY), "the environment variable " + UI_SCALE_PROPERTY
                + " forces the scale this class must read from the machine");
        WinToolkitProbe.assertNoScreenDpiOverride();
        Util.startup(startupLatch, startupLatch::countDown);
        startup = WinToolkitProbe.screenStartup(TEST_NAME);
    }

    @AfterAll
    static void shutdown() {
        assumeTrue(PlatformUtil.isWindows());
        Util.shutdown();
    }

    @Test
    @Order(1)
    public void theToolkitStartedWithoutAUiScaleOverride() {
        assertEquals(WinToolkitProbe.bits(NO_OVERRIDE), WinToolkitProbe.bits(startup.javaOverride()),
                "WinApplication.overrideUIScale after startup");
        assertEquals(S_OK, startup.awareness()[0], "GetProcessDpiAwareness HRESULT after startup");
    }

    /**
     * Anti-vacuity for the comparison below: the run exercised the Java {@code staticScreen_getScreens} with the
     * four JNI methods it replaced gone, and the display-change event reaches Java through the screen callback
     * table - the path {@code WinApplicationStartupTest}'s work-area step takes since ABI 5.
     */
    @Test
    @Order(2)
    public void theScreensAreJavaAndTheDisplayChangeEventHasItsTable() {
        assertEquals("java", WinToolkitProbe.winApplicationMethodKind("staticScreen_getScreens"),
                "staticScreen_getScreens");
        assertEquals("absent", WinToolkitProbe.winApplicationMethodKind("_init", int.class), "_init");
        assertEquals("absent", WinToolkitProbe.winApplicationMethodKind("_setClassLoader", ClassLoader.class),
                "_setClassLoader");
        assertEquals("absent", WinToolkitProbe.winApplicationMethodKind("initIDs", float.class), "initIDs");
        AtomicReference<Boolean> installed = new AtomicReference<>();
        Util.runAndWait(() -> installed.set(WinToolkitProbe.screenCallbacksInstalled()));
        assertTrue(installed.get(), "GwinScreenCallbacks was not installed by WinApplication's static initializer");
    }

    @Test
    @Order(3)
    public void theStartupScreensEqualTheJavaEnumeration() {
        WinToolkitProbe.ScreenParity parity = WinToolkitProbe.compareScreenArms(TEST_NAME, startup);

        WinToolkitProbe.writeScreenTable(Path.of("target", "screen-p3-startup.txt"), parity.startup());
        WinToolkitProbe.writeScreenTable(Path.of("target", "screen-p3-java.txt"), parity.viaJava());
        AtomicReference<Boolean> dpiFunctionsResolved = new AtomicReference<>();
        Util.runAndWait(() -> dpiFunctionsResolved.set(WinToolkitProbe.dpiFunctionsResolved()));
        System.out.println(TEST_NAME + ": compared on attempt " + parity.attempts() + "; facade shcore trio resolved: "
                + dpiFunctionsResolved.get());
    }
}
