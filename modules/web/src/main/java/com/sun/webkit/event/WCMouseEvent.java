/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit.event;

public final class WCMouseEvent {

    // id
    public final static int MOUSE_PRESSED = 0;
    public final static int MOUSE_RELEASED = 1;
    public final static int MOUSE_MOVED = 2;
    public final static int MOUSE_DRAGGED = 3;
    public final static int MOUSE_WHEEL = 4;

    // button
    public final static int NOBUTTON = 0;
    public final static int BUTTON1 = 1;
    public final static int BUTTON2 = 2;
    public final static int BUTTON3 = 4;

    private final int id;
    private final long when;

    private final int button;
    private final int clickCount;

    private final int x;
    private final int y;
    private final int screenX;
    private final int screenY;

    private final boolean shift;
    private final boolean control;
    private final boolean alt;
    private final boolean meta;

    private final boolean popupTrigger;

    public WCMouseEvent(int id, int button, int clickCount, int x, int y, int screenX, int screenY,
                        long when, boolean shift, boolean control, boolean alt, boolean meta, boolean popupTrigger)
    {
        this.id = id;
        this.button = button;
        this.clickCount = clickCount;
        this.x = x;
        this.y = y;
        this.screenX = screenX;
        this.screenY = screenY;
        this.when = when;
        this.shift = shift;
        this.control = control;
        this.alt = alt;
        this.meta = meta;
        this.popupTrigger = popupTrigger;
    }

    public int getID() { return id; }
    public long getWhen() { return when; }

    public int getButton() { return button; }
    public int getClickCount() { return clickCount; }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getScreenX() { return screenX; }
    public int getScreenY() { return screenY; }

    public boolean isShiftDown() { return shift; }
    public boolean isControlDown() { return control; }
    public boolean isAltDown() { return alt; }
    public boolean isMetaDown() { return meta; }

    public boolean isPopupTrigger() { return popupTrigger; }
}
