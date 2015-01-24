/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.lens;

import java.util.Map;

import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.View;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.Buffer;

import com.sun.glass.events.ViewEvent;

final class LensView extends View {

    protected LensView() {
        super();
    }

    // Constants
    private static long multiClickTime =  300;
    private static int multiClickMaxX = LensTouchInputSupport.touchTapRadius;
    private static int multiClickMaxY = LensTouchInputSupport.touchTapRadius;

    // view variables
    private int x;
    private int y;
    private long nativePtr;

    protected static long _getMultiClickTime() {
        if (multiClickTime == -1) {
            //multiClickTime = _getMultiClickTime_impl();
            //currently calling a native function is meaningless
            multiClickTime = 300;
        }
        return multiClickTime;
    }

    protected static int _getMultiClickMaxX() {
        if (multiClickMaxX == -1) {
            //multiClickMaxX = _getMultiClickMaxX_impl();
            //currently calling a native function is meaningless
            multiClickMaxX = 2;
        }
        return multiClickMaxX;
    }

    protected static int _getMultiClickMaxY() {
        if (multiClickMaxY == -1) {
            //multiClickMaxY = _getMultiClickMaxY_impl();
            //currently calling a native function is meaningless
            multiClickMaxY = 2;
        }
        return multiClickMaxY;
    }

    native private void _paintInt(long ptr, int w, int h, IntBuffer ints,
                                  int[] array, int offset);
    native private void _paintByte(long ptr, int w, int h, ByteBuffer bytes,
                                   byte[] array, int offset);
    native private void _paintIntDirect(long ptr, int w, int h, Buffer buffer);

    @Override
    protected void _enableInputMethodEvents(long ptr, boolean enable) {
    }



    @Override
    protected long _getNativeView(long ptr) {
        //this method is a Windows hack, see View.java for more details
        // we just ignore it
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
        // native code is there but does nothing yet
        //throw new UnsupportedOperationException("Not supported yet.");
        LensLogger.getLogger().info("Ignoring repaint");
    }



    @Override protected void _uploadPixels(long nativeViewPtr, Pixels pixels) {
        if (getWindow() != null) {
            Buffer data = pixels.getPixels();
            int width = pixels.getWidth();
            int height = pixels.getHeight();

            if (data.isDirect() == true) {
                _paintIntDirect(nativeViewPtr, width, height, data);
            } else if (data.hasArray() == true) {
                if (pixels.getBytesPerComponent() == 1) {
                    ByteBuffer bytes = (ByteBuffer)data;
                    _paintByte(nativeViewPtr, width, height, bytes,
                               bytes.array(), bytes.arrayOffset());
                } else {
                    IntBuffer ints = (IntBuffer)data;
                    int[] intArray = ints.array();

                    _paintInt(nativeViewPtr, width, height, ints,
                              intArray, ints.arrayOffset());
                }
            }
        }
    }

    /**
     * Events
     */

    protected void _notifyMove(int x, int y) {
        // used to update x,y for _getX(), _getY()
        this.x = x;
        this.y = y;
        notifyView(ViewEvent.MOVE);
    }

    protected void _notifyKey(int type, int keyCode, char[] keyChars,
                              int modifiers) {
        notifyKey(type, keyCode, keyChars, modifiers);
    }

    protected void _notifyMouse(int type, int button,
                                int x, int y, int xAbs, int yAbs, int modifiers,
                                boolean isPopupTrigger, boolean isSynthesized) {
        notifyMouse(type, button, x, y, xAbs, yAbs, modifiers, isPopupTrigger,
                    isSynthesized);
    }

    protected void _notifyScroll(int x, int y, int xAbs, int yAbs,
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
    protected int _getNativeFrameBuffer(long ptr) {
        return 0;
    }

    /**
     * Native methods
     */

    @Override
    protected long _create(Map caps) {
        this.nativePtr = _createNativeView(caps);
        return this.nativePtr;
    }

    private native long _createNativeView(Map caps);

    /**
    * Assuming this is used to lock the surface for painting
    */
    @Override
    native protected void _begin(long ptr);

    /**
     * Assuming this is used to unlock the surface after painting is
     * done
     */
    @Override
    native protected  void _end(long ptr);


    @Override
    native protected void _setParent(long ptr, long parentPtr);

    @Override
    native protected boolean _close(long ptr);

    @Override
    native protected boolean _enterFullscreen(long ptr, boolean animate,
                                              boolean keepRatio,
                                              boolean hideCursor);

    @Override
    native protected void _exitFullscreen(long ptr, boolean animate);

    @Override
    public String toString() {
        return "LensView[nativePtr=0x" + Long.toHexString(nativePtr) + "]";
    }

}
