/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit.event;

public final class WCFocusEvent {

    // id
    public final static int WINDOW_ACTIVATED = 0;
    public final static int WINDOW_DEACTIVATED = 1;
    public final static int FOCUS_GAINED = 2;
    public final static int FOCUS_LOST = 3;

    // direction
    public final static int UNKNOWN = -1;
    public final static int FORWARD = 0;
    public final static int BACKWARD = 1;

    private final int id;
    private final int direction;

    public WCFocusEvent(int id, int direction) {
        this.id = id;
        this.direction = direction;
    }

    public int getID() { return id; }

    public int getDirection() { return direction; }

    @Override
    public String toString() {
        return "WCFocusEvent(" + id + ", " + direction + ")";
    }
}
