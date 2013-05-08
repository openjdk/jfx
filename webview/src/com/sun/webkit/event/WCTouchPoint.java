/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit.event;

import java.nio.ByteBuffer;

public final class WCTouchPoint {

    // see WebCore/platform/PlatformTouchPoint::State
    public final static int STATE_RELEASED = 0;
    public final static int STATE_PRESSED = 1;
    public final static int STATE_MOVED = 2;
    public final static int STATE_STATIONARY = 3;
    public final static int STATE_CANCELLED = 4;

    private final int id;
    private int state;
    private double x;
    private double y;
    private double screenX;
    private double screenY;

    public WCTouchPoint(int id, int state, double x, double y, double screenX, double screenY) {
        this.id = id;
        this.state = state;
        this.x = x;
        this.y = y;
        this.screenX = screenX;
        this.screenY = screenY;
    }

    public int getID() { return this.id; }
    public int getState() { return this.state; }

    public double getX() { return this.x; }
    public double getY() { return this.y; }

    public double getScreenX() { return this.screenX; }
    public double getScreenY() { return this.screenY; }

    public void update(int state) {
        this.state = state;
    }

    public void update(double x, double y) {
        x -= this.x;
        y -= this.y;
        this.x += x;
        this.y += y;
        this.screenX += x;
        this.screenY += y;
        this.state = STATE_MOVED;
    }

    void putTo(ByteBuffer buffer) {
        buffer.putInt(this.id);
        buffer.putInt(this.state);
        buffer.putInt(Math.round((float) this.x));
        buffer.putInt(Math.round((float) this.y));
        buffer.putInt(Math.round((float) this.screenX));
        buffer.putInt(Math.round((float) this.screenY));
    }
}
