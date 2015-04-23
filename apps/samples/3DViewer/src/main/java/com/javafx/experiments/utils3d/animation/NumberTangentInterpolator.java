/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates.
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

package com.javafx.experiments.utils3d.animation;

import javafx.animation.Interpolator;
import javafx.util.Duration;

public class NumberTangentInterpolator extends Interpolator {

    private final double inValue, outValue;
    private final long inTicks, outTicks;

    public double getInValue() {
        return inValue;
    }

    public double getOutValue() {
        return outValue;
    }

    public double getInTicks() {
        return inTicks;
    }

    public double getOutTicks() {
        return outTicks;
    }

    public NumberTangentInterpolator(Duration inDuration, double inValue, Duration outDuration, double outValue) {
        this.inTicks = TickCalculation.fromDuration(inDuration);
        this.inValue = inValue;
        this.outTicks = TickCalculation.fromDuration(outDuration);
        this.outValue = outValue;
    }

    public NumberTangentInterpolator(Duration duration, double value) {
        this.outTicks = this.inTicks = TickCalculation.fromDuration(duration);
        this.inValue = this.outValue = value;
    }

    @Override
    public String toString() {
        return "NumberTangentInterpolator [inValue=" + inValue
                + ", inDuration=" + TickCalculation.toDuration(inTicks) + ", outValue="
                + outValue + ", outDuration=" + TickCalculation.toDuration(outTicks) + "]";
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + (int) (Double.doubleToLongBits(this.inValue) ^ (Double.doubleToLongBits(this.inValue) >>> 32));
        hash = 59 * hash + (int) (Double.doubleToLongBits(this.outValue) ^ (Double.doubleToLongBits(this.outValue) >>> 32));
        hash = 59 * hash + (int) (this.inTicks ^ (this.inTicks >>> 32));
        hash = 59 * hash + (int) (this.outTicks ^ (this.outTicks >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NumberTangentInterpolator other = (NumberTangentInterpolator) obj;
        if (Double.doubleToLongBits(this.inValue) != Double.doubleToLongBits(other.inValue)) {
            return false;
        }
        if (Double.doubleToLongBits(this.outValue) != Double.doubleToLongBits(other.outValue)) {
            return false;
        }
        if (this.inTicks != other.inTicks) {
            return false;
        }
        if (this.outTicks != other.outTicks) {
            return false;
        }
        return true;
    }

    @Override
    protected double curve(double t) {
        // Fallback: If NumberTangentInterpolator is used with a target, that is
        // not a number,
        // it behaves like linear interpolation.
        return t;
    }
}

