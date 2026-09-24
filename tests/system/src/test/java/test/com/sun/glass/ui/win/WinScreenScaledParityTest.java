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

import com.sun.glass.ui.Screen;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * {@code WinScreenStartupParityTest} with the UI scale forced the product way: {@code glass.win.uiScale=1.75} is set
 * before the toolkit starts, so {@code WinApplication}'s static initializer reads it into
 * {@code WinApplication.overrideUIScale} - since ABI 5 the only copy of the override - and the startup enumeration
 * divides by it.
 * <p>
 * The comparison is {@link WinToolkitProbe#compareScreenArms}: {@code Screen.getScreens()} as the toolkit built it and
 * {@code WinScreenLayout.arrange(collectMonitors(), overrideUIScale)} called again must agree on all twenty values of
 * every screen. On a 100 % monitor only a forced scale reaches the division and the {@code floor(x + 0.5f)} rounding
 * of the arrangement, which is why this class exists. Nothing is poked reflectively: the override must have arrived
 * through the static initializer, which is why every screen of both arms must carry the scale and the primary's size
 * must really have been divided by it. Since ABI 5 both arms are the same Java, so this is a wiring check.
 * <p>
 * <b>The evidence this class carried before it was a wiring check.</b> Until the JNI was deleted it compared
 * three arms - the startup list (built by the JNI, which read the override that {@code initIDs} had copied into
 * {@code GlassApplication::overrideUIScale}), the JNI {@code GlassScreen::CreateJavaScreens} called again, and the
 * Java. On 2026-09-13, on {@code glass.dll} md5 {@code 29e73f00e2e837d2930f958be2304098} (ABI 5), in two runs, all
 * three were equal on attempt 1: 1097x617 over a 1920x1080 platform rectangle, work area 1097x594, resolution 46,
 * all four scales {@code 0x3fe00000}; every {@code GetProcessDpiAwareness} reading was {@code {S_OK, 2}}; and the
 * three tables were byte for byte the snapshot the JNI implementation in commit {@code 8492cb03b0} wrote at a forced
 * 1.75 (md5
 * {@code 259cf8811be6ee60727066ca3def4beb}), which the module suite keeps as
 * {@code screen-snapshot-golden-uiscale-1.75.txt}.
 * <p>
 * The property is set in {@code @BeforeAll}, which is early enough only because this module's surefire configuration
 * forks a fresh JVM for every test class ({@code reuseForks=false}, {@code tests/system/pom.xml}); the same pattern is
 * {@code UIRenderSceneTest}'s. The tables go to {@code target/} with a {@code -uiscale-1.75} suffix.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WinScreenScaledParityTest {

    private static final String TEST_NAME = "WinScreenScaledParityTest";

    private static final String UI_SCALE_PROPERTY = "glass.win.uiScale";

    private static final float FORCED_SCALE = 1.75f;

    private static final int S_OK = 0;

    private static final CountDownLatch startupLatch = new CountDownLatch(1);

    private static WinToolkitProbe.ScreenStartup startup;

    @BeforeAll
    static void initFX() {
        assumeTrue(PlatformUtil.isWindows());
        WinToolkitProbe.assertNoScreenDpiOverride();
        System.setProperty(UI_SCALE_PROPERTY, "1.75");
        Util.startup(startupLatch, startupLatch::countDown);
        startup = WinToolkitProbe.screenStartup(TEST_NAME);
    }

    @AfterAll
    static void shutdown() {
        assumeTrue(PlatformUtil.isWindows());
        Util.shutdown();
        System.clearProperty(UI_SCALE_PROPERTY);
    }

    @Test
    @Order(1)
    public void theStaticInitializerReadTheForcedUiScale() {
        assertEquals(WinToolkitProbe.bits(FORCED_SCALE), WinToolkitProbe.bits(startup.javaOverride()),
                "WinApplication.overrideUIScale after startup with -D" + UI_SCALE_PROPERTY + "=1.75");
        assertEquals(S_OK, startup.awareness()[0], "GetProcessDpiAwareness HRESULT after startup");
    }

    @Test
    @Order(2)
    public void theStartupScreensEqualTheJavaEnumerationAtTheForcedScale() {
        assertEquals("java", WinToolkitProbe.winApplicationMethodKind("staticScreen_getScreens"),
                "staticScreen_getScreens");

        WinToolkitProbe.ScreenParity parity = WinToolkitProbe.compareScreenArms(TEST_NAME, startup);

        WinToolkitProbe.writeScreenTable(Path.of("target", "screen-p3-startup-uiscale-1.75.txt"), parity.startup());
        WinToolkitProbe.writeScreenTable(Path.of("target", "screen-p3-java-uiscale-1.75.txt"), parity.viaJava());
        assertForcedScale("Screen.getScreens(), built by the toolkit", parity.startup());
        assertForcedScale("WinScreenLayout.arrange(collectMonitors(), overrideUIScale)", parity.viaJava());

        AtomicReference<Boolean> dpiFunctionsResolved = new AtomicReference<>();
        Util.runAndWait(() -> dpiFunctionsResolved.set(WinToolkitProbe.dpiFunctionsResolved()));
        System.out.println(TEST_NAME + ": compared on attempt " + parity.attempts() + "; facade shcore trio resolved: "
                + dpiFunctionsResolved.get());
    }

    /** Every scale of every screen is the forced one, and the primary's bounds were divided by it. */
    private static void assertForcedScale(String arm, Screen[] screens) {
        String forced = WinToolkitProbe.bits(FORCED_SCALE);
        for (int i = 0; i < screens.length; i++) {
            Screen screen = screens[i];
            String which = arm + ", screen " + i;
            assertEquals(forced, WinToolkitProbe.bits(screen.getPlatformScaleX()), which + ": platformScaleX");
            assertEquals(forced, WinToolkitProbe.bits(screen.getPlatformScaleY()), which + ": platformScaleY");
            assertEquals(forced, WinToolkitProbe.bits(screen.getRecommendedOutputScaleX()), which + ": outputScaleX");
            assertEquals(forced, WinToolkitProbe.bits(screen.getRecommendedOutputScaleY()), which + ": outputScaleY");
        }
        Screen primary = screens[0];
        assertNotEquals(primary.getPlatformWidth(), primary.getWidth(),
                arm + ": the primary's width was not divided by the forced scale");
        assertNotEquals(primary.getPlatformHeight(), primary.getHeight(),
                arm + ": the primary's height was not divided by the forced scale");
    }
}
