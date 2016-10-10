/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit.event;

import java.lang.annotation.Native;

public final class WCKeyEvent {

    // Event types
    @Native public static final int KEY_TYPED       = 0;
    @Native public static final int KEY_PRESSED     = 1;
    @Native public static final int KEY_RELEASED    = 2;

    // Windows virtual key code constants used by WebKit Java port
    @Native public static final int VK_BACK         = 0x08;
    @Native public static final int VK_TAB          = 0x09;
    @Native public static final int VK_RETURN       = 0x0D;
    @Native public static final int VK_ESCAPE       = 0x1B;
    @Native public static final int VK_PRIOR        = 0x21;
    @Native public static final int VK_NEXT         = 0x22;
    @Native public static final int VK_END          = 0x23;
    @Native public static final int VK_HOME         = 0x24;
    @Native public static final int VK_LEFT         = 0x25;
    @Native public static final int VK_UP           = 0x26;
    @Native public static final int VK_RIGHT        = 0x27;
    @Native public static final int VK_DOWN         = 0x28;
    @Native public static final int VK_INSERT       = 0x2D;
    @Native public static final int VK_DELETE       = 0x2E;
    @Native public static final int VK_OEM_PERIOD   = 0xBE;


    private final int type;
    private final long when;
    private final String text;
    private final String keyIdentifier;
    private final int windowsVirtualKeyCode;
    private final boolean shift;
    private final boolean ctrl;
    private final boolean alt;
    private final boolean meta;


    public WCKeyEvent(int type, String text, String keyIdentifier,
                      int windowsVirtualKeyCode, boolean shift, boolean ctrl,
                      boolean alt, boolean meta, long when)
    {
        this.type = type;
        this.text = text;
        this.keyIdentifier = keyIdentifier;
        this.windowsVirtualKeyCode = windowsVirtualKeyCode;
        this.shift = shift;
        this.ctrl = ctrl;
        this.alt = alt;
        this.meta = meta;
        this.when = when;
    }


    public int getType() { return type; }
    public long getWhen() { return when; }
    public String getText() { return text; }
    public String getKeyIdentifier() { return keyIdentifier; }
    public int getWindowsVirtualKeyCode() { return windowsVirtualKeyCode; }
    public boolean isShiftDown() { return shift; }
    public boolean isCtrlDown() { return ctrl; }
    public boolean isAltDown() { return alt; }
    public boolean isMetaDown() { return meta; }

    public static boolean filterEvent(WCKeyEvent ke) {
        if (ke.getType() == KEY_TYPED) {
            String text = ke.getText();
            if (text == null || text.length() != 1) {
                return true;
            }
            char kc = text.charAt(0);
            // don't send KEY_TYPED events to WebKit for some control keys
            // like DELETE, BACKSPACE, etc.
            if ((kc == '\b') || (kc == '\n') || (kc == '\t') ||
                (kc == '\uffff') || (kc == '\u0018') || (kc == '\u001b') || (kc == '\u007f'))
            {
                return true;
            }
        }
        return false;
    }
}
