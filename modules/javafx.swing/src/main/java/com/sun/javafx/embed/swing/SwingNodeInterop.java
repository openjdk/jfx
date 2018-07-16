/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.embed.swing;

import com.sun.javafx.embed.swing.DisposerRecord;
import java.awt.AWTEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowFocusListener;
import javafx.embed.swing.SwingNode;
import javax.swing.JComponent;

public abstract class SwingNodeInterop {

    public abstract Object createLightweightFrame();
    public abstract Object getLightweightFrame();
    public abstract void disposeFrame(Object frame);

    public abstract java.awt.event.MouseEvent createMouseEvent(Object lwFrame,
                            int swingID, long swingWhen, int swingModifiers,
                            int relX, int relY, int absX, int absY,
                            int clickCount, boolean swingPopupTrigger,
                            int swingButton);

    public abstract MouseWheelEvent createMouseWheelEvent(Object lwFrame,
                            int swingModifiers, int x, int y, int wheelRotation);

    public abstract java.awt.event.KeyEvent createKeyEvent(Object lwFrame,
                                   int swingID, long swingWhen,
                                   int swingModifiers,
                                   int swingKeyCode, char swingChar);

    public abstract AWTEvent createUngrabEvent(Object lwFrame);

    public abstract void overrideNativeWindowHandle(Object frame, long handle, Runnable closeWindow);
    public abstract void addWindowFocusListener(Object frame, WindowFocusListener l);
    public abstract void notifyDisplayChanged(Object frame, double scaleX, double scaleY);
    public abstract void setHostBounds(Object frame, int windowX, int windowY,
                               int windowW, int windowH);
    public abstract void setContent(Object frame, Object content);
    public abstract void setVisible(Object frame, boolean visible);
    public abstract void setBounds(Object frame, int frameX, int frameY, int frameW, int frameH);
    public abstract void emulateActivation(Object frame, boolean activate);

    public abstract Object createSwingNodeContent(JComponent content, SwingNode node);

    public abstract DisposerRecord createSwingNodeDisposer(Object lwFrame);

}
