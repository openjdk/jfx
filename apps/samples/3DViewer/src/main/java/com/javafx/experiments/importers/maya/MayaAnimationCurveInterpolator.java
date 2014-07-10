/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.javafx.experiments.importers.maya;

import javafx.animation.Interpolator;

/**
 * MayaAnimationCurveInterpolator
 * <p/>
 * Interpolator is from javafx.animation
 */
class MayaAnimationCurveInterpolator extends Interpolator {

    float p1Delta;
    float p2Delta;
    boolean zeroDuration;
    public String debug; // hack

    //=========================================================================
    // MayaAnimationCurveInterpolator
    //=========================================================================
    public MayaAnimationCurveInterpolator(
            float p1Delta, float p2Delta,
            boolean zeroDuration) {
        this.p1Delta = p1Delta;
        this.p2Delta = p2Delta;
        this.zeroDuration = zeroDuration;
    }

    //=========================================================================
    // MayaAnimationCurveInterpolator.curve
    //=========================================================================
    public double curve(double t) { return t; }

    //=========================================================================
    // MayaAnimationCurveInterpolator.interpolate
    //=========================================================================
    // [!] API Change
    public double interpolate2(
            double startValue, double endValue,
            double fraction) {
        if (Double.isNaN(fraction)) {
            return startValue;
        }
        if (zeroDuration) {
            return endValue;
        }
        float t = (float) fraction;
        float oneMinusT = 1.0f - t;
        float tSquared = t * t;
        float oneMinusTSquared = oneMinusT * oneMinusT;
        float p0 = (float) startValue;
        float p3 = (float) endValue;
        float p1 = p0 + p1Delta;
        float p2 = p3 + p2Delta;
        float ret = ((oneMinusTSquared * oneMinusT * p0) +
                (3 * oneMinusTSquared * t * p1) +
                (3 * oneMinusT * tSquared * p2) +
                (tSquared * t * p3));

        if (debug != null) {
            // if (DEBUG) System.out.println("interpolate: " + debug + ": " + t + " " + startValue + " to " + endValue + ": "+ret);
        }

        return ret;
    }

    //=========================================================================
    // MayaAnimationCurveInterpolator.interpolate
    //=========================================================================
    // [!] API Change
    public int interpolate2(int startValue, int endValue, double fraction) {
        return (int) interpolate(
                (double) startValue,
                (double) endValue,
                fraction);
    }

    //=========================================================================
    // MayaAnimationCurveInterpolator.interpolate
    //=========================================================================
    // [!] API Change
    public Object interpolate2(Object startValue, Object endValue, double fraction) {
        return interpolate(
                ((Number) startValue).doubleValue(),
                ((Number) endValue).doubleValue(),
                fraction);
    }
}
