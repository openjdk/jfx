/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit.event;

public final class WCMouseWheelEvent {

    private final long when;

    private final int x;
    private final int y;
    private final int screenX;
    private final int screenY;

    private final float deltaX;
    private final float deltaY;

    private final boolean shift;
    private final boolean control;
    private final boolean alt;
    private final boolean meta;

    public WCMouseWheelEvent(int x, int y, int screenX, int screenY,
                             long when, boolean shift, boolean control, boolean alt, boolean meta,
                             float deltaX, float deltaY)
    {
        this.x = x;
        this.y = y;
        this.screenX = screenX;
        this.screenY = screenY;
        this.when = when;
        this.shift = shift;
        this.control = control;
        this.alt = alt;
        this.meta = meta;
        this.deltaX = deltaX;
        this.deltaY = deltaY;
    }

    public long getWhen() { return when; }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getScreenX() { return screenX; }
    public int getScreenY() { return screenY; }

    public boolean isShiftDown() { return shift; }
    public boolean isControlDown() { return control; }
    public boolean isAltDown() { return alt; }
    public boolean isMetaDown() { return meta; }

    public float getDeltaX() { return deltaX; }
    public float getDeltaY() { return deltaY; }
}
