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

package com.sun.glass.ui.win;

import com.sun.glass.ui.Screen;
import java.util.List;

/**
 * The monitor arrangement of {@code GlassScreen.cpp}, in Java: which monitor is primary, and where each
 * monitor's rectangle and work area land in FX coordinates once every monitor has been divided by its
 * own UI scale and re-attached to the neighbour it touched in Windows pixels.
 * <p>
 * The C was {@code GlassScreen::CreateJavaScreens} from the enumeration onwards, all of it since deleted
 * (read it with {@code git show 8492cb03b0:modules/javafx.graphics/src/main/native-glass/win/GlassScreen.cpp}): the
 * primary pick and the index-0 swap, {@code anchor}, the pass loop,
 * {@code propagateAnchors}, {@code touchesLeft}, {@code touchesAbove}, {@code anchorH}, {@code anchorV},
 * {@code originOffsetFromRanges} and {@code anchorTo}, plus
 * {@code GlassApplication::GetUIScale} and the argument list {@code CreateJavaMonitorFromMIS} passed to
 * {@code Screen.<init>} ({@code FillScreenInfo}). The enumeration itself - the two
 * {@code EnumDisplayMonitors} passes and {@code GetMonitorSettings} - is
 * {@link WinGlassNative#collectMonitors()}, which hands this class one {@link MonitorSettings} per
 * monitor in enumeration order.
 * <p>
 * <b>The arithmetic is the C's, statement for statement, and must stay that way.</b> Every value here
 * ends up as a {@link Screen} bound, and so as a window position and a robot coordinate, and a one-pixel
 * drift is silent. Three things in particular:
 * <ul>
 * <li>Every division is {@code int / float} in {@code float} - {@code monW / scalex}, never through
 *     {@code double}. MSVC x64 compiles the C to scalar SSE under {@code /fp:precise} (the glass target
 *     sets neither {@code /fp} nor {@code /arch}), and the anchoring has no multiply-add a contraction
 *     could fuse, so IEEE single precision on both sides gives the same bits.</li>
 * <li>Every rounding is {@code (jint) floorf(q + 0.5f)}, which is {@link #floorToInt} of the same
 *     {@code float} sum, never {@link Math#round}: the two disagree at {@code 0.49999997f} and at every
 *     value whose {@code + 0.5f} is not exact.</li>
 * <li>The {@code (jint)} of that {@code floorf} is MSVC's {@code cvttss2si}, which answers
 *     {@code 0x80000000} for a NaN and for anything outside the {@code int} range, where a Java cast
 *     would saturate or answer 0. Reachable: {@code -Dglass.win.uiScale=0.0000001} divides a 1920-pixel
 *     monitor into {@code 1.92e10}, and a monitor whose DPI query and device context both failed
 *     divides by a scale of 0.</li>
 * </ul>
 * {@code WinScreenLayoutParityTest} holds this to the C through {@code screen-anchor-golden.txt}: the named
 * layouts and 2,000 generated ones as {@code gwin_test_screen_anchor} answered them by running the C's own
 * statements. The port was compared with that hook live, on every layout, until the hook was deleted;
 * the golden, captured from it before, is the only oracle left.
 * <p>
 * <b>No static state.</b> Nothing here reads {@code WinApplication.overrideUIScale} or any other
 * static: the override is an argument, read by the caller at enumeration time. A static initializer
 * that reached into {@code WinApplication} could run on a half-initialized class (the facade can be
 * initialized by {@link WinRobot} first).
 */
final class WinScreenLayout {

    /** winuser.h {@code USER_DEFAULT_SCREEN_DPI}: 100 %, the divisor of {@code GetUIScale}. */
    static final float USER_DEFAULT_SCREEN_DPI = 96.0f;

    /**
     * What {@code GetMonitorSettings} learned about one monitor, before any arithmetic. The rectangles are
     * {@code MONITORINFO.rcMonitor} and {@code rcWork} in Windows pixels; {@code dpiX} / {@code dpiY} are
     * what the C stored in {@code MonitorInfoStruct.dpiX/Y} (the raw DPI when {@code GetDpiForMonitor}
     * exists, else {@code LOGPIXELSX/Y}); {@code uiDpiX} / {@code uiDpiY} are the {@code UINT} values it
     * handed to {@code GetUIScale} (the effective DPI, or {@code LOGPIXELSX/Y}). {@code hMonitor} is the
     * handle value, which becomes {@link Screen#getNativeScreen()}.
     */
    record MonitorSettings(long hMonitor,
                           int monitorLeft, int monitorTop, int monitorRight, int monitorBottom,
                           int workLeft, int workTop, int workRight, int workBottom,
                           boolean primary, int colorDepth, int dpiX, int dpiY, int uiDpiX, int uiDpiY) {
    }

    /**
     * Test instrumentation: one call per entry into each C helper, in the order the C calls them, so that a
     * corpus can prove it reached the branch it was written for. Every method is a no-op by default and
     * product code passes {@link #NONE}; nothing here may change what the arrangement computes.
     */
    interface Probe {

        /** The no-op every product path passes. */
        Probe NONE = new Probe() {
        };

        /** Entry to {@code anchorTo}, before it has divided or written anything. */
        default void anchorTo(Monitor monitor, boolean xBefore, boolean yBefore, int pass) {
        }

        /** Entry to {@code anchor}: a monitor anchored where Windows put it, as the start of a group. */
        default void anchor() {
        }

        /** Entry to {@code originOffsetFromRanges}. */
        default void originOffset(int aV0, int aV1, int mV0, int mV1) {
        }

        /** Entry to {@code anchorH}. */
        default void anchorH(boolean before) {
        }

        /** Entry to {@code anchorV}. */
        default void anchorV(boolean before) {
        }

        /** What one {@code touchesLeft} answered. */
        default void touchesLeft(boolean touches) {
        }

        /** What one {@code touchesAbove} answered. */
        default void touchesAbove(boolean touches) {
        }

        /** Entry to {@code propagateAnchors}. */
        default void propagate(int pass) {
        }
    }

    /** {@code RECT} (windef.h), mutable, as the four {@code LONG}s of {@code MonitorInfoStruct} were. */
    static final class Rect {
        int left;
        int top;
        int right;
        int bottom;
    }

    /**
     * {@code MonitorInfoStruct} ({@code GlassScreen.cpp}) without its JNI global ref: the inputs, the two
     * FX rectangles {@code anchorTo} writes, and {@code anchoredInPass}, 0 until the monitor is anchored.
     * {@code dpiX} / {@code dpiY} are overwritten by {@code anchorTo} with the scaled resolution, as in C.
     */
    static final class Monitor {
        long hMonitor;
        final Rect rcMonitor = new Rect();
        final Rect rcWork = new Rect();
        final Rect fxMonitor = new Rect();
        final Rect fxWork = new Rect();
        boolean primaryScreen;
        int colorDepth;
        float uiScaleX;
        float uiScaleY;
        int dpiX;
        int dpiY;
        int anchoredInPass;

        /**
         * What {@code GetMonitorSettings} wrote into a zeroed {@code MonitorInfoStruct}, with the two
         * {@code GetUIScale} results already computed - so that a test can pass scales no DPI produces.
         */
        static Monitor of(MonitorSettings settings, float uiScaleX, float uiScaleY) {
            Monitor monitor = new Monitor();
            monitor.hMonitor = settings.hMonitor();
            monitor.rcMonitor.left = settings.monitorLeft();
            monitor.rcMonitor.top = settings.monitorTop();
            monitor.rcMonitor.right = settings.monitorRight();
            monitor.rcMonitor.bottom = settings.monitorBottom();
            monitor.rcWork.left = settings.workLeft();
            monitor.rcWork.top = settings.workTop();
            monitor.rcWork.right = settings.workRight();
            monitor.rcWork.bottom = settings.workBottom();
            monitor.primaryScreen = settings.primary();
            monitor.colorDepth = settings.colorDepth();
            monitor.dpiX = settings.dpiX();
            monitor.dpiY = settings.dpiY();
            monitor.uiScaleX = uiScaleX;
            monitor.uiScaleY = uiScaleY;
            return monitor;
        }

        /**
         * {@code CreateJavaMonitorFromMIS}: {@code Screen.<init>(JIIIIIIIIIIIIIIIFFFF)V} with the arguments
         * {@code FillScreenInfo} lists - the handle, the depth, {@code fxMonitor}, {@code rcMonitor} and
         * {@code fxWork} as origin plus size, the resolution after {@code anchorTo}, and the UI scale
         * twice, as the platform scale and as the output scale.
         */
        Screen toScreen() {
            return new Screen(hMonitor,
                    colorDepth,
                    fxMonitor.left,
                    fxMonitor.top,
                    fxMonitor.right - fxMonitor.left,
                    fxMonitor.bottom - fxMonitor.top,
                    rcMonitor.left,
                    rcMonitor.top,
                    rcMonitor.right - rcMonitor.left,
                    rcMonitor.bottom - rcMonitor.top,
                    fxWork.left,
                    fxWork.top,
                    fxWork.right - fxWork.left,
                    fxWork.bottom - fxWork.top,
                    dpiX,
                    dpiY,
                    uiScaleX,
                    uiScaleY,
                    uiScaleX,
                    uiScaleY);
        }
    }

    private final Probe probe;

    private WinScreenLayout(Probe probe) {
        this.probe = probe;
    }

    /**
     * {@code GlassScreen::CreateJavaScreens} after its two enumeration passes: {@code null} for no monitor
     * (its {@code return NULL}, which {@code Screen.initScreens} turns into "Internal graphics failed to
     * initialize"), else one {@link Screen} per monitor with the primary in slot 0.
     *
     * @param monitors what {@link WinGlassNative#collectMonitors()} returned, in enumeration order
     * @param overrideUIScale {@code WinApplication.overrideUIScale}, read by the caller now
     */
    static Screen[] arrange(List<MonitorSettings> monitors, float overrideUIScale) {
        int numMonitors = monitors.size();
        if (numMonitors <= 0) {
            return null;
        }
        Monitor[] infos = new Monitor[numMonitors];
        for (int i = 0; i < numMonitors; i++) {
            MonitorSettings settings = monitors.get(i);
            infos[i] = Monitor.of(settings, uiScale(settings.uiDpiX(), overrideUIScale),
                    uiScale(settings.uiDpiY(), overrideUIScale));
        }
        arrange(infos, Probe.NONE);
        Screen[] screens = new Screen[numMonitors];
        for (int i = 0; i < numMonitors; i++) {
            screens[i] = infos[i].toScreen();
        }
        return screens;
    }

    /**
     * {@code GlassApplication::GetUIScale(UINT dpi)}: the override when it is positive - a NaN is not - else
     * the {@code UINT} converted to {@code float} and divided by {@code 96.0f}, in {@code float}.
     */
    static float uiScale(int dpi, float overrideUIScale) {
        return overrideUIScale > 0.0f
                ? overrideUIScale
                : (float) Integer.toUnsignedLong(dpi) / USER_DEFAULT_SCREEN_DPI;
    }

    /** {@code ArrangeMonitors} over a non-empty array, in place, reporting to {@code probe}. */
    static void arrange(Monitor[] infos, Probe probe) {
        new WinScreenLayout(probe).arrangeMonitors(infos, infos.length);
    }

    /**
     * {@code (jint) floorf(value)} as MSVC x64 computes it: {@code floorf}, then {@code cvttss2si}, whose
     * answer for a NaN or a value outside {@code [-2^31, 2^31)} is {@code 0x80000000}. {@code floorf} of a
     * {@code float} is always representable as a {@code float}, so {@link Math#floor} on the widened value is
     * the same number.
     */
    static int floorToInt(float value) {
        double floor = Math.floor(value);
        return floor >= Integer.MIN_VALUE && floor < -(double) Integer.MIN_VALUE ? (int) floor : Integer.MIN_VALUE;
    }

    private void arrangeMonitors(Monitor[] infos, int n) {
        // The primary monitor should be set to the 0 index
        int primaryIndex = 0;
        for (int i = 0; i < n; i++) {
            Monitor pMIS = infos[i];
            if (pMIS.rcMonitor.left <= 0
                    && pMIS.rcMonitor.top <= 0
                    && pMIS.rcMonitor.right > 0
                    && pMIS.rcMonitor.bottom > 0) {
                primaryIndex = i;
                break;
            } else if (pMIS.primaryScreen) {
                primaryIndex = i;
            }
        }
        // Swap the primary monitor to the 0 index. The C swapped struct values; nothing holds a
        // reference across it, so swapping the references is the same.
        if (primaryIndex > 0) {
            Monitor tmpMIS = infos[primaryIndex];
            infos[primaryIndex] = infos[0];
            infos[0] = tmpMIS;
        }

        // Anchor the primary screen, then propagate its geometry to its neighbours, then theirs, one pass
        // per step. When a pass finds nothing left to propagate and a monitor is still unanchored, anchor
        // the first such monitor where Windows put it - in the same pass - and propagate from it.
        int pass = 1;
        anchor(infos[0], pass);
        do {
            boolean foundUnpropagated = false;
            for (int i = 0; i < n; i++) {
                Monitor pMIS = infos[i];
                if (pMIS.anchoredInPass == pass) {
                    foundUnpropagated = true;
                    propagateAnchors(infos, n, pMIS, pass + 1);
                }
            }
            if (foundUnpropagated) {
                pass++;
            } else {
                boolean foundUnanchored = false;
                for (int i = 0; i < n; i++) {
                    Monitor pMIS = infos[i];
                    if (pMIS.anchoredInPass == 0) {
                        foundUnanchored = true;
                        anchor(pMIS, pass);
                        break;
                    }
                }
                if (!foundUnanchored) {
                    break;
                }
                // Loop back without incrementing "pass" so that the monitor just anchored propagates.
            }
        } while (true);
    }

    private void propagateAnchors(Monitor[] infos, int n, Monitor pMIS, int pass) {
        probe.propagate(pass);
        for (int i = 0; i < n; i++) {
            Monitor pMIS2 = infos[i];
            if (pMIS2.anchoredInPass != 0) {
                continue;
            }
            if (touchesLeft(pMIS2, pMIS)) {
                anchorH(pMIS, pMIS2, false, pass);
            } else if (touchesLeft(pMIS, pMIS2)) {
                anchorH(pMIS, pMIS2, true, pass);
            } else if (touchesAbove(pMIS2, pMIS)) {
                anchorV(pMIS, pMIS2, false, pass);
            } else if (touchesAbove(pMIS, pMIS2)) {
                anchorV(pMIS, pMIS2, true, pass);
            }
        }
    }

    private boolean touchesLeft(Monitor pMISa, Monitor pMISb) {
        boolean touches = pMISa.rcMonitor.left == pMISb.rcMonitor.right
                && pMISa.rcMonitor.top < pMISb.rcMonitor.bottom
                && pMISa.rcMonitor.bottom > pMISb.rcMonitor.top;
        probe.touchesLeft(touches);
        return touches;
    }

    private boolean touchesAbove(Monitor pMISa, Monitor pMISb) {
        boolean touches = pMISa.rcMonitor.top == pMISb.rcMonitor.bottom
                && pMISa.rcMonitor.left < pMISb.rcMonitor.right
                && pMISa.rcMonitor.right > pMISb.rcMonitor.left;
        probe.touchesAbove(touches);
        return touches;
    }

    private void anchorH(Monitor pAnchor, Monitor pMon, boolean before, int pass) {
        probe.anchorH(before);
        int x = before ? pAnchor.fxMonitor.left : pAnchor.fxMonitor.right;
        int yoff = originOffsetFromRanges(pAnchor.rcMonitor.top, pAnchor.rcMonitor.bottom,
                pMon.rcMonitor.top, pMon.rcMonitor.bottom,
                pAnchor.uiScaleY, pMon.uiScaleY);
        int y = pAnchor.fxMonitor.top + yoff;
        anchorTo(pMon, x, before, y, false, pass);
    }

    private void anchorV(Monitor pAnchor, Monitor pMon, boolean before, int pass) {
        probe.anchorV(before);
        int xoff = originOffsetFromRanges(pAnchor.rcMonitor.left, pAnchor.rcMonitor.right,
                pMon.rcMonitor.left, pMon.rcMonitor.right,
                pAnchor.uiScaleX, pMon.uiScaleX);
        int x = pAnchor.fxMonitor.left + xoff;
        int y = before ? pAnchor.fxMonitor.top : pAnchor.fxMonitor.bottom;
        anchorTo(pMon, x, false, y, before, pass);
    }

    /**
     * The FX offset between the origins of two monitors that share an edge, measured around a "midpoint"
     * of their overlap: 0 when the first coordinates match, the common last coordinate when the last ones
     * match, otherwise the middle of the overlap - {@code (v0 + v1)} in {@code int}, then {@code / 2.0f}.
     */
    private int originOffsetFromRanges(int aV0, int aV1, int mV0, int mV1, float aScale, float mScale) {
        probe.originOffset(aV0, aV1, mV0, mV1);
        float mid;
        if (aV0 == mV0) {
            // mid == aV0 and mid == mV0 means rel = 0
            return 0;
        } else if (aV1 == mV1) {
            mid = (float) aV1;
        } else {
            int v0 = (aV0 > mV0) ? aV0 : mV0;
            int v1 = (aV1 < mV1) ? aV1 : mV1;
            mid = (v0 + v1) / 2.0f;
        }
        float rel = (mid - aV0) / aScale - (mid - mV0) / mScale;
        return floorToInt(rel + 0.5f);
    }

    private void anchor(Monitor pMIS, int pass) {
        probe.anchor();
        anchorTo(pMIS, pMIS.rcMonitor.left, false, pMIS.rcMonitor.top, false, pass);
    }

    private void anchorTo(Monitor pMIS, int fxX, boolean xBefore, int fxY, boolean yBefore, int pass) {
        probe.anchorTo(pMIS, xBefore, yBefore, pass);
        int monX = pMIS.rcMonitor.left;
        int monY = pMIS.rcMonitor.top;
        int monW = pMIS.rcMonitor.right - monX;
        int monH = pMIS.rcMonitor.bottom - monY;
        int wrkL = pMIS.rcWork.left - monX;
        int wrkT = pMIS.rcWork.top - monY;
        int wrkR = pMIS.rcWork.right - monX;
        int wrkB = pMIS.rcWork.bottom - monY;
        float scalex = pMIS.uiScaleX;
        float scaley = pMIS.uiScaleY;
        if (scalex != 1.0f) {
            pMIS.dpiX = floorToInt((pMIS.dpiX / scalex) + 0.5f);
            monW = floorToInt((monW / scalex) + 0.5f);
            wrkL = floorToInt((wrkL / scalex) + 0.5f);
            wrkR = floorToInt((wrkR / scalex) + 0.5f);
        }
        if (scaley != 1.0f) {
            pMIS.dpiY = floorToInt((pMIS.dpiY / scaley) + 0.5f);
            monH = floorToInt((monH / scaley) + 0.5f);
            wrkT = floorToInt((wrkT / scaley) + 0.5f);
            wrkB = floorToInt((wrkB / scaley) + 0.5f);
        }

        if (xBefore) {
            fxX -= monW;
        }
        if (yBefore) {
            fxY -= monH;
        }
        pMIS.fxMonitor.left = fxX;
        pMIS.fxMonitor.top = fxY;
        pMIS.fxMonitor.right = fxX + monW;
        pMIS.fxMonitor.bottom = fxY + monH;
        pMIS.fxWork.left = fxX + wrkL;
        pMIS.fxWork.top = fxY + wrkT;
        pMIS.fxWork.right = fxX + wrkR;
        pMIS.fxWork.bottom = fxY + wrkB;
        pMIS.anchoredInPass = pass;
    }
}
