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

package com.sun.glass.ui.gtk;

import com.sun.glass.ui.Screen;
import com.sun.glass.ui.Window;
import java.util.function.Consumer;

/**
 * A real {@code GtkWindow} peer - a subclass of the class {@code GtkApplication} creates - that reports
 * every call the GTK glass event code makes into a window, with its arguments, before it runs the production
 * method: {@code Window.isEnabled}, the {@code Window.notify*} methods and {@code GtkWindow.notifyStateChanged}. The
 * C reaches them by virtual calls on the peer it was created with ({@code WindowContextTop}'s {@code jwindow},
 * {@code glass_window.cpp} at commit {@code 033187ad90}, or the window id of the callback tables), so these
 * overrides see exactly the arguments the C passed, whatever mechanism carries the call.
 * <p>
 * {@code sink} receives one line per call - {@code notifyMove 100 120}, {@code isEnabled=true}, ... - and may throw
 * a {@code RuntimeException} to make that call throw into the C, which is how the exception paths of the event code
 * are exercised. {@code GtkWindow.nonClientHitTest} is {@code private} and cannot be overridden; it reaches the
 * view's {@code View.EventHandler.pickHeaderArea}, where a recording handler sees it. Scenarios only; not a
 * production class.
 */
public final class GtkTraceWindow extends GtkWindow {

    private final Consumer<String> sink;

    public GtkTraceWindow(Window owner, Screen screen, int styleMask, Consumer<String> sink) {
        // assigned before the super constructor: _createWindow already hands this peer to the C
        this.sink = sink;
        super(owner, screen, styleMask);
    }

    /** {@code w,h at x,y visible vx,vy vw,vh scale sx,sy res rx,ry depth d} of {@code screen}, or {@code null}. */
    public static String describe(Screen screen) {
        if (screen == null) {
            return "null";
        }
        return screen.getWidth() + "x" + screen.getHeight() + "@" + screen.getX() + "," + screen.getY()
                + " platform " + screen.getPlatformWidth() + "x" + screen.getPlatformHeight() + "@"
                + screen.getPlatformX() + "," + screen.getPlatformY()
                + " visible " + screen.getVisibleWidth() + "x" + screen.getVisibleHeight() + "@"
                + screen.getVisibleX() + "," + screen.getVisibleY()
                + " scale " + screen.getPlatformScaleX() + "," + screen.getPlatformScaleY()
                + " output " + screen.getRecommendedOutputScaleX() + "," + screen.getRecommendedOutputScaleY()
                + " res " + screen.getResolutionX() + "," + screen.getResolutionY()
                + " depth " + screen.getDepth() + " native " + screen.getNativeScreen();
    }

    @Override
    public boolean isEnabled() {
        boolean enabled = super.isEnabled();
        sink.accept("isEnabled=" + enabled);
        return enabled;
    }

    @Override
    protected void notifyClose() {
        sink.accept("notifyClose");
        super.notifyClose();
    }

    @Override
    protected void notifyDestroy() {
        sink.accept("notifyDestroy");
        super.notifyDestroy();
    }

    @Override
    protected void notifyMove(int x, int y) {
        sink.accept("notifyMove " + x + " " + y);
        super.notifyMove(x, y);
    }

    @Override
    protected void notifyMoveToAnotherScreen(Screen newScreen) {
        sink.accept("notifyMoveToAnotherScreen " + describe(newScreen));
        super.notifyMoveToAnotherScreen(newScreen);
    }

    @Override
    protected void notifyResize(int type, int width, int height) {
        sink.accept("notifyResize " + type + " " + width + " " + height);
        super.notifyResize(type, width, height);
    }

    @Override
    protected void notifyFocus(int event) {
        sink.accept("notifyFocus " + event);
        super.notifyFocus(event);
    }

    @Override
    protected void notifyFocusDisabled() {
        sink.accept("notifyFocusDisabled");
        super.notifyFocusDisabled();
    }

    @Override
    protected void notifyFocusUngrab() {
        sink.accept("notifyFocusUngrab");
        super.notifyFocusUngrab();
    }

    @Override
    protected void notifyLevelChanged(int level) {
        sink.accept("notifyLevelChanged " + level);
        super.notifyLevelChanged(level);
    }

    @Override
    protected void notifyStateChanged(int state) {
        sink.accept("notifyStateChanged " + state);
        super.notifyStateChanged(state);
    }
}
