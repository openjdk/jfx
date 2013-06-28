/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit.graphics;

public final class WCPoint {
    final float x;
    final float y;
    public WCPoint(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public int getIntX() {
        return (int)x;
    }

    public int getIntY() {
        return (int)y;
    }
}
