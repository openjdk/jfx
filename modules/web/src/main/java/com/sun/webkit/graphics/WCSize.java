/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit.graphics;

public final class WCSize {
    private final float width;
    private final float height;

    public WCSize(float width, float height) {
        this.width = width;
        this.height = height;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public int getIntWidth() {
        return (int)width;
    }

    public int getIntHeight() {
        return (int)height;
    }
}
