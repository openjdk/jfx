/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit.graphics;

public abstract class WCGradient<G> {

    /* The GradientSpreadMethod should be compliant with
     * WebCore/platform/graphics/GraphicsTypes.h
     */
    public static final int PAD = 1;
    public static final int REFLECT = 2;
    public static final int REPEAT = 3;

    private int spreadMethod = PAD;
    private boolean proportional;

    void setSpreadMethod(int spreadMethod) {
        if (spreadMethod != REFLECT && spreadMethod != REPEAT) {
            spreadMethod = PAD;
        }
        this.spreadMethod = spreadMethod;
    }

    public int getSpreadMethod() {
        return this.spreadMethod;
    }

    void setProportional(boolean proportional) {
        this.proportional = proportional;
    }

    public boolean isProportional() {
        return this.proportional;
    }

    protected abstract void addStop(int argb, float offset);

    public abstract G getPlatformGradient();
}
