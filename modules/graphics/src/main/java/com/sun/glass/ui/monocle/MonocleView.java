/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.monocle;

import com.sun.glass.events.ViewEvent;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.View;
import com.sun.glass.ui.Window;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Map;

public final class MonocleView extends View {

    MonocleView() {
    }

    // Constants
    private static long multiClickTime =  500;
    private static int multiClickMaxX = 20;
    private static int multiClickMaxY = 20;

    // view variables
    private int x;
    private int y;

    protected static long _getMultiClickTime() {
        return multiClickTime;
    }

    protected static int _getMultiClickMaxX() {
        return multiClickMaxX;
    }

    protected static int _getMultiClickMaxY() {
        return multiClickMaxY;
    }

    @Override
    protected void _enableInputMethodEvents(long ptr, boolean enable) {
    }

    @Override
    protected long _getNativeView(long ptr) {
        return ptr;
    }

    @Override
    protected int _getX(long ptr) {
        return x;
    }

    @Override
    protected int _getY(long ptr) {
        return y;
    }



    @Override
    protected void _scheduleRepaint(long ptr) {
    }



    @Override protected void _uploadPixels(long nativeViewPtr, Pixels pixels) {
        if (getWindow() != null) {
            NativeScreen screen =
                    NativePlatformFactory.getNativePlatform().getScreen();
            Window window = getWindow();
            screen.uploadPixels(pixels.asByteBuffer(), // TODO: asByteBuffer is inefficient
                                x + window.getX(), y + window.getY(),
                                pixels.getWidth(), pixels.getHeight());
        }
    }

    /**
     * Events
     */

    public void _notifyMove(int x, int y) {
        this.x = x;
        this.y = y;
        notifyView(ViewEvent.MOVE);
    }

    public void _notifyKey(int type, int keyCode, char[] keyChars,
                              int modifiers) {
        notifyKey(type, keyCode, keyChars, modifiers);
    }

    public void _notifyMouse(int type, int button,
                                int x, int y, int xAbs, int yAbs, int modifiers,
                                boolean isPopupTrigger, boolean isSynthesized) {
        notifyMouse(type, button, x, y, xAbs, yAbs, modifiers, isPopupTrigger,
                    isSynthesized);
    }

    public void _notifyScroll(int x, int y, int xAbs, int yAbs,
                                 double deltaX, double deltaY, int modifiers,
                                 int lines, int chars,
                                 int defaultLines, int defaultChars,
                                 double xMultiplier, double yMultiplier) {
        notifyScroll(x, y, xAbs, yAbs, deltaX, deltaY,
                     modifiers, lines, chars,
                     defaultLines, defaultChars, xMultiplier, yMultiplier);
    }

    protected void _notifyRepaint(int x, int y, int width, int height) {
        notifyRepaint(x, y, width, height);
    }

    protected void _notifyResize(int width, int height) {
        notifyResize(width, height);
    }

    protected void _notifyViewEvent(int viewEvent) {
        notifyView(viewEvent);
    }

    //DnD
    protected void _notifyDragEnter(int x, int y, int absx, int absy, int recommendedDropAction) {
        notifyDragEnter(x, y, absx, absy, recommendedDropAction);
    }
    protected void _notifyDragLeave() {
        notifyDragLeave();
    }
    protected void _notifyDragDrop(int x, int y, int absx, int absy, int recommendedDropAction) {
        notifyDragDrop(x, y, absx, absy, recommendedDropAction);
    }
    protected void _notifyDragOver(int x, int y, int absx, int absy, int recommendedDropAction) {
        notifyDragOver(x, y, absx, absy, recommendedDropAction);
    }

    //Menu event - i.e context menu hint (usually mouse right click) 
    protected void _notifyMenu(int x, int y, int xAbs, int yAbs, boolean isKeyboardTrigger) {
        notifyMenu(x, y, xAbs, yAbs, isKeyboardTrigger);
    }

    @Override
    protected long _create(Map caps) {
        return 1l;
    }

    @Override
    protected void _setParent(long ptr, long parentPtr) {
    }

    @Override
    protected boolean _close(long ptr) {
        return true;
    }

    @Override
    protected boolean _enterFullscreen(long ptr, boolean animate,
                                       boolean keepRatio,
                                       boolean hideCursor) {
        return true;
    }

    @Override
    protected void _exitFullscreen(long ptr, boolean animate) {
    }

    @Override
    public String toString() {
        return "MonocleView["
                + x + "," + y
                + "+" + getWidth() + "x" + getHeight()
                + "]";
    }

    /**
    * Assuming this is used to lock the surface for painting
    */
    @Override
    protected void _begin(long ptr) {
    }

    /**
     * Assuming this is used to unlock the surface after painting is
     * done
     */
    @Override
    protected  void _end(long ptr) {
    }
}
