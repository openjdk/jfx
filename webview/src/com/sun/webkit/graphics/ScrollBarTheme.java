/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit.graphics;

public abstract class ScrollBarTheme extends Ref {

    // See constants values WebCore/platform/ScrollTypes.h
    public static final int NO_PART = 0;
    public static final int BACK_BUTTON_START_PART = 1;
    public static final int FORWARD_BUTTON_START_PART = 1 << 1;
    public static final int BACK_TRACK_PART = 1 << 2;
    public static final int THUMB_PART = 1 << 3;
    public static final int FORWARD_TRACK_PART = 1 << 4;
    public static final int BACK_BUTTON_END_PART = 1 << 5;
    public static final int FORWARD_BUTTON_END_PART = 1 << 6;

    public static final int HORIZONTAL_SCROLLBAR = 0;
    public static final int VERTICAL_SCROLLBAR = 1;

    private static int thickness;

    public static int getThickness() {
        return thickness > 0 ? thickness : 12;
    }
    
    public static void setThickness(int value) {
        thickness = value;
    }

    protected abstract Ref createWidget(long id, int w, int h, int orientation, int value, int visibleSize, int totalSize);

    public abstract void paint(WCGraphicsContext g, Ref sbRef, int x, int y, int pressedPart, int hoveredPart);

    protected abstract int hitTest(int w, int h, int orientation, int value, int visibleSize, int totalSize, int x, int y);

    protected abstract int getThumbPosition(int w, int h, int orientation, int value, int visibleSize, int totalSize);

    protected abstract int getThumbLength(int w, int h, int orientation, int value, int visibleSize, int totalSize);

    protected abstract int getTrackPosition(int w, int h, int orientation);

    protected abstract int getTrackLength(int w, int h, int orientation);
}
