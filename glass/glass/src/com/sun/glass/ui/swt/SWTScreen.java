/*
 * Copyright (c) 2012, 2013, Oracle  and/or its affiliates. All rights reserved.
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
package com.sun.glass.ui.swt;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

import com.sun.glass.ui.Screen;

//TODO - implement multiple screens
//TODO - implement resolution changed

final class SWTScreen {

    static Screen getScreen() {
        Display display = Display.getDefault();
        if (display.getThread() == Thread.currentThread()) {
            Monitor monitor = display.getPrimaryMonitor();
            Rectangle bounds = monitor.getBounds();
            Rectangle client = monitor.getClientArea();
            int depth = display.getDepth();
            Point dpi = display.getDPI();
            return new_Screen(
                1L,
                depth,
                bounds.x, bounds.y,
                bounds.width, bounds.height,
                client.x, client.y,
                client.width, client.height,
                dpi.x, dpi.y,
                1.0f);
        } else {
            final Screen screen = new_Screen(
                1L,
                32,
                0, 0,
                1024, 768,
                0, 0,
                1024, 768,
                1, 72,
                1.0f);
            display.asyncExec(new Runnable () {
                public void run () {
                    try {
                        // TODO - get rid of reflection
                        Method method = Screen.class.getDeclaredMethod("notifySettingsChanged");
                        method.setAccessible(true);
                        method.invoke(Screen.class);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            return screen;
        }
    }

    //TODO - get rid of reflection
    static void setField(Object object, String name, int value) {
        try {
            Field field = Screen.class.getDeclaredField(name);
            field.setAccessible(true);
            field.setInt(object, value);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    //TODO - get rid of reflection
    static void setField(Object object, String name, long value) {
        try {
            Field field = Screen.class.getDeclaredField(name);
            field.setAccessible(true);
            field.setLong(object, value);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    //TODO - get rid of reflection
    static void setField(Object object, String name, float value) {
        try {
            Field field = Screen.class.getDeclaredField(name);
            field.setAccessible(true);
            field.setFloat(object, value);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    static Screen new_Screen(long ptr, int depth, int x, int y, int width, int height, int visibleX, int visibleY, int visibleWidth, int visibleHeight, int resolutionX, int resolutionY, float scale) {
        Screen screen = new Screen();
        setField(screen, "ptr", ptr);
        setField(screen, "depth", depth);
        setField(screen, "x", x);
        setField(screen, "y", y);
        setField(screen, "width", width);
        setField(screen, "height", height);
        setField(screen, "visibleX", visibleX);
        setField(screen, "visibleY", visibleY);
        setField(screen, "visibleWidth", visibleWidth);
        setField(screen, "visibleHeight", visibleHeight);
        setField(screen, "resolutionX", resolutionX);
        setField(screen, "resolutionY", resolutionY);
        setField(screen, "scale", scale);
        return screen;
    }
}

