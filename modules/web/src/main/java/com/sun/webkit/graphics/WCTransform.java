/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit.graphics;

import java.util.Arrays;

public final class WCTransform extends Ref {
    private final double[] m;

    public WCTransform(double m00, double m10, double m01, double m11, 
                       double m02, double m12)
    {
        this.m = new double[6];
        m[0] = m00;
        m[1] = m10;
        m[2] = m01;
        m[3] = m11;
        m[4] = m02;
        m[5] = m12;
    }

    public double [] getMatrix() {
        return Arrays.copyOf(m, m.length);
    }
}
