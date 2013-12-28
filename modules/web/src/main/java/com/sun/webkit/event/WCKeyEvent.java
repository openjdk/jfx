/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit.event;

public final class WCKeyEvent {

    // Event types
    public static final int KEY_TYPED       = 0;
    public static final int KEY_PRESSED     = 1;
    public static final int KEY_RELEASED    = 2;

    // Windows virtual key code constants used by WebKit Java port
    public static final int VK_BACK         = 0x08;
    public static final int VK_TAB          = 0x09;
    public static final int VK_RETURN       = 0x0D;
    public static final int VK_ESCAPE       = 0x1B;
    public static final int VK_PRIOR        = 0x21;
    public static final int VK_NEXT         = 0x22;
    public static final int VK_END          = 0x23;
    public static final int VK_HOME         = 0x24;
    public static final int VK_LEFT         = 0x25;
    public static final int VK_UP           = 0x26;
    public static final int VK_RIGHT        = 0x27;
    public static final int VK_DOWN         = 0x28;
    public static final int VK_INSERT       = 0x2D;
    public static final int VK_DELETE       = 0x2E;
    public static final int VK_OEM_PERIOD   = 0xBE;


    private final int type;
    private final String text;
    private final String keyIdentifier;
    private final int windowsVirtualKeyCode;
    private final boolean shift;
    private final boolean ctrl;
    private final boolean alt;
    private final boolean meta;


    public WCKeyEvent(int type, String text, String keyIdentifier,
                      int windowsVirtualKeyCode, boolean shift, boolean ctrl,
                      boolean alt, boolean meta)
    {
        this.type = type;
        this.text = text;
        this.keyIdentifier = keyIdentifier;
        this.windowsVirtualKeyCode = windowsVirtualKeyCode;
        this.shift = shift;
        this.ctrl = ctrl;
        this.alt = alt;
        this.meta = meta;
    }


    public int getType() { return type; }
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
