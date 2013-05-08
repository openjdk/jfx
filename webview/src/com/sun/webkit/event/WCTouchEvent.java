/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit.event;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

public final class WCTouchEvent {

    public final static int TOUCH_START = 0;
    public final static int TOUCH_MOVE = 1;
    public final static int TOUCH_END = 2;
    public final static int TOUCH_CANCEL = 3;

    private final int id;
    private final long when;

    private final boolean shift;
    private final boolean control;
    private final boolean alt;
    private final boolean meta;

    private final ByteBuffer data;

    public WCTouchEvent(int id, List<WCTouchPoint> points,
                        long when, boolean shift, boolean control, boolean alt, boolean meta) {
        this.id = id;
        this.when = when;
        this.shift = shift;
        this.control = control;
        this.alt = alt;
        this.meta = meta;
        // each point holds 6 data items of type int which is 4 bytes
        this.data = ByteBuffer.allocateDirect(6 * 4 * points.size());
        this.data.order(ByteOrder.nativeOrder());
        for (WCTouchPoint point : points) {
            point.putTo(this.data);
        }
    }

    public int getID() { return this.id; }
    public long getWhen() { return this.when; }

    public boolean isShiftDown() { return this.shift; }
    public boolean isControlDown() { return this.control; }
    public boolean isAltDown() { return this.alt; }
    public boolean isMetaDown() { return this.meta; }

    public ByteBuffer getTouchData() { return this.data; }
}
