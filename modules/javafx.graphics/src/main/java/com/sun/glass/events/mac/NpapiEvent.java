/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.glass.events.mac;

import java.lang.annotation.Native;
import java.util.Map;
import com.sun.glass.ui.Window;

// https://wiki.mozilla.org/NPAPI:CocoaEventModel

// used by Mac OS X impl for handling an NPAPI event sent from plugin to Glass process
public class NpapiEvent {

    // draw
    @Native final static public int NPCocoaEventDrawRect            = 1;
    // mouse
    @Native final static public int NPCocoaEventMouseDown           = 2;
    @Native final static public int NPCocoaEventMouseUp             = 3;
    @Native final static public int NPCocoaEventMouseMoved          = 4;
    @Native final static public int NPCocoaEventMouseEntered        = 5;
    @Native final static public int NPCocoaEventMouseExited         = 6;
    @Native final static public int NPCocoaEventMouseDragged        = 7;
    // key
    @Native final static public int NPCocoaEventKeyDown             = 8;
    @Native final static public int NPCocoaEventKeyUp               = 9;
    @Native final static public int NPCocoaEventFlagsChanged        = 10;
    // focus
    @Native final static public int NPCocoaEventFocusChanged        = 11;
    @Native final static public int NPCocoaEventWindowFocusChanged  = 12;
    // mouse
    @Native final static public int NPCocoaEventScrollWheel         = 13;
    // text input
    @Native final static public int NPCocoaEventTextInput           = 14;

    private native static void _dispatchCocoaNpapiDrawEvent(long windowPtr, int type,
            long context, double x, double y, double width, double height);
    private native static void _dispatchCocoaNpapiMouseEvent(long windowPtr, int type,
            int modifierFlags, double pluginX, double pluginY, int buttonNumber, int clickCount,
            double deltaX, double deltaY, double deltaZ);
    private native static void _dispatchCocoaNpapiKeyEvent(long windowPtr, int type,
            int modifierFlags, String characters, String charactersIgnoringModifiers,
            boolean isARepeat, int keyCode, boolean needsKeyTyped);
    private native static void _dispatchCocoaNpapiFocusEvent(long windowPtr, int type,
            boolean hasFocus);
    private native static void _dispatchCocoaNpapiTextInputEvent(long windowPtr, int type,
            String text);

    final private static boolean getBoolean(Map eventInfo, String key) {
        boolean value = false;
        {
            if (eventInfo.containsKey(key) == true ) {
                try {
                    value = ((Boolean)eventInfo.get(key)).booleanValue();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return value;
    }
    final private static int getInt(Map eventInfo, String key) {
        int value = 0;
        {
            if (eventInfo.containsKey(key) == true ) {
                try {
                    value = ((Integer)eventInfo.get(key)).intValue();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return value;
    }
    final private static long getLong(Map eventInfo, String key) {
        long value = 0;
        {
            if (eventInfo.containsKey(key) == true ) {
                try {
                    value = ((Long)eventInfo.get(key)).longValue();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return value;
    }
    final private static double getDouble(Map eventInfo, String key) {
        double value = 0;
        {
            if (eventInfo.containsKey(key) == true ) {
                try {
                    value = ((Double)eventInfo.get(key)).doubleValue();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return value;
    }
    final private static String getString(Map eventInfo, String key) {
        String value = null;
        {
            if (eventInfo.containsKey(key) == true ) {
                try {
                    value = (String)eventInfo.get(key);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return value;
    }
    public static void dispatchCocoaNpapiEvent(Window window, Map eventInfo) {
        final long windowPtr = window.getNativeWindow();
        //System.err.println(">>>>>>>>>>>>>>>>>>>>>>> eventInfo: "+eventInfo);
        int type = ((Integer)eventInfo.get("type")).intValue();
        switch (type) {
            case NPCocoaEventDrawRect: {
                    long context = getLong(eventInfo, "context");
                    double x = getDouble(eventInfo, "x");
                    double y = getDouble(eventInfo, "y");
                    double width = getDouble(eventInfo, "width");
                    double height = getDouble(eventInfo, "height");
                    _dispatchCocoaNpapiDrawEvent(windowPtr, type,
                            context, x, y, width, height);
                }
                break;
            case NPCocoaEventMouseDown:
            case NPCocoaEventMouseUp:
            case NPCocoaEventMouseMoved:
            case NPCocoaEventMouseEntered:
            case NPCocoaEventMouseExited:
            case NPCocoaEventMouseDragged:
            case NPCocoaEventScrollWheel: {
                    int modifierFlags = getInt(eventInfo, "modifierFlags");
                    double pluginX = getDouble(eventInfo, "pluginX");
                    double pluginY = getDouble(eventInfo, "pluginY");
                    int buttonNumber = getInt(eventInfo, "buttonNumber");
                    int clickCount = getInt(eventInfo, "clickCount");
                    double deltaX = getDouble(eventInfo, "deltaX");
                    double deltaY = getDouble(eventInfo, "deltaY");
                    double deltaZ = getDouble(eventInfo, "deltaZ");
                    _dispatchCocoaNpapiMouseEvent(windowPtr, type,
                            modifierFlags, pluginX, pluginY, buttonNumber, clickCount,
                            deltaX, deltaY, deltaZ);
                }
                break;
            case NPCocoaEventKeyDown:
            case NPCocoaEventKeyUp:
            case NPCocoaEventFlagsChanged: {
                    int modifierFlags = getInt(eventInfo, "modifierFlags");
                    String characters = getString(eventInfo, "characters");
                    String charactersIgnoringModifiers = getString(eventInfo, "charactersIgnoringModifiers");
                    boolean isARepeat = getBoolean(eventInfo, "isARepeat");
                    int keyCode = getInt(eventInfo, "keyCode");
                    boolean needsKeyTyped = getBoolean(eventInfo, "needsKeyTyped");

                    _dispatchCocoaNpapiKeyEvent(windowPtr, type,
                            modifierFlags, characters, charactersIgnoringModifiers,
                            isARepeat, keyCode, needsKeyTyped);
                }
                break;
            case NPCocoaEventFocusChanged:
            case NPCocoaEventWindowFocusChanged: {
                    boolean hasFocus = getBoolean(eventInfo, "hasFocus");
                    _dispatchCocoaNpapiFocusEvent(windowPtr, type,
                            hasFocus);
                }
                break;
            case NPCocoaEventTextInput: {
                    String text = getString(eventInfo, "text");
                    _dispatchCocoaNpapiTextInputEvent(windowPtr, type,
                            text);
                }
                break;
        }
    }
}
