/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.scenario.animation.shared;

import com.sun.javafx.animation.KeyValueHelper;
import javafx.animation.Interpolator;
import javafx.animation.KeyValue;
import javafx.beans.value.WritableBooleanValue;
import javafx.beans.value.WritableDoubleValue;
import javafx.beans.value.WritableFloatValue;
import javafx.beans.value.WritableIntegerValue;
import javafx.beans.value.WritableLongValue;
import javafx.beans.value.WritableValue;

import com.sun.scenario.animation.NumberTangentInterpolator;

public abstract class InterpolationInterval {

    protected final long ticks;
    protected final Interpolator rightInterpolator;

    protected InterpolationInterval(long ticks,
            Interpolator rightInterpolator) {
        this.ticks = ticks;
        this.rightInterpolator = rightInterpolator;
    }

    public abstract void interpolate(double frac);

    public abstract void recalculateStartValue();

    public static InterpolationInterval create(KeyValue rightKeyValue,
            long ticks, KeyValue leftKeyValue, long duration) {
        switch (KeyValueHelper.getType(rightKeyValue)) {
            case BOOLEAN:
                return new BooleanInterpolationInterval(rightKeyValue, ticks,
                        leftKeyValue.getEndValue());
            case DOUBLE:
                return ((leftKeyValue.getInterpolator() instanceof NumberTangentInterpolator) || (rightKeyValue
                        .getInterpolator() instanceof NumberTangentInterpolator)) ? new TangentDoubleInterpolationInterval(
                        rightKeyValue, ticks, leftKeyValue, duration)
                        : new DoubleInterpolationInterval(rightKeyValue,
                                ticks, leftKeyValue.getEndValue());
            case FLOAT:
                return ((leftKeyValue.getInterpolator() instanceof NumberTangentInterpolator) || (rightKeyValue
                        .getInterpolator() instanceof NumberTangentInterpolator)) ? new TangentFloatInterpolationInterval(
                        rightKeyValue, ticks, leftKeyValue, duration)
                        : new FloatInterpolationInterval(rightKeyValue, ticks,
                                leftKeyValue.getEndValue());
            case INTEGER:
                return ((leftKeyValue.getInterpolator() instanceof NumberTangentInterpolator) || (rightKeyValue
                        .getInterpolator() instanceof NumberTangentInterpolator)) ? new TangentIntegerInterpolationInterval(
                        rightKeyValue, ticks, leftKeyValue, duration)
                        : new IntegerInterpolationInterval(rightKeyValue,
                                ticks, leftKeyValue.getEndValue());
            case LONG:
                return ((leftKeyValue.getInterpolator() instanceof NumberTangentInterpolator) || (rightKeyValue
                        .getInterpolator() instanceof NumberTangentInterpolator)) ? new TangentLongInterpolationInterval(
                        rightKeyValue, ticks, leftKeyValue, duration)
                        : new LongInterpolationInterval(rightKeyValue, ticks,
                                leftKeyValue.getEndValue());
            case OBJECT:
                return new ObjectInterpolationInterval(rightKeyValue, ticks,
                        leftKeyValue.getEndValue());
        }
        throw new RuntimeException("Should not reach here");
    }

    public static InterpolationInterval create(KeyValue rightKeyValue,
            long ticks) {
        switch (KeyValueHelper.getType(rightKeyValue)) {
            case BOOLEAN:
                return new BooleanInterpolationInterval(rightKeyValue, ticks);
            case DOUBLE:
                return (rightKeyValue.getInterpolator() instanceof NumberTangentInterpolator) ? new TangentDoubleInterpolationInterval(
                        rightKeyValue, ticks)
                        : new DoubleInterpolationInterval(rightKeyValue, ticks);
            case FLOAT:
                return (rightKeyValue.getInterpolator() instanceof NumberTangentInterpolator) ? new TangentFloatInterpolationInterval(
                        rightKeyValue, ticks)
                        : new FloatInterpolationInterval(rightKeyValue, ticks);
            case INTEGER:
                return (rightKeyValue.getInterpolator() instanceof NumberTangentInterpolator) ? new TangentIntegerInterpolationInterval(
                        rightKeyValue, ticks)
                        : new IntegerInterpolationInterval(rightKeyValue,
                                ticks);
            case LONG:
                return (rightKeyValue.getInterpolator() instanceof NumberTangentInterpolator) ? new TangentLongInterpolationInterval(
                        rightKeyValue, ticks) : new LongInterpolationInterval(
                        rightKeyValue, ticks);
            case OBJECT:
                return new ObjectInterpolationInterval(rightKeyValue, ticks);
        }
        throw new RuntimeException("Should not reach here");
    }

    private static abstract class TangentInterpolationInterval extends
            InterpolationInterval {

        private final double duration;
        private final double p2;
        protected final double p3;
        private final NumberTangentInterpolator leftInterpolator;

        protected double p0;
        private double p1;

        private TangentInterpolationInterval(KeyValue rightKeyValue,
                long ticks, KeyValue leftKeyValue, long duration) {
            super(ticks, rightKeyValue.getInterpolator());
            assert (rightKeyValue.getEndValue() instanceof Number)
                    && (leftKeyValue.getEndValue() instanceof Number);

            this.duration = duration;
            final Interpolator rawLeftInterpolator = leftKeyValue
                    .getInterpolator();
            leftInterpolator = (rawLeftInterpolator instanceof NumberTangentInterpolator) ? (NumberTangentInterpolator) rawLeftInterpolator
                    : null;
            recalculateStartValue(((Number) leftKeyValue.getEndValue())
                    .doubleValue());

            final NumberTangentInterpolator interpolator = (rightInterpolator instanceof NumberTangentInterpolator) ? (NumberTangentInterpolator) rightInterpolator
                    : null;
            p3 = ((Number) rightKeyValue.getEndValue()).doubleValue();
            final double p2Delta = (interpolator == null) ? 0 : (interpolator
                    .getInValue() - p3)
                    * duration
                    / interpolator.getInTicks()
                    / 3;
            p2 = p3 + p2Delta;
        }

        private TangentInterpolationInterval(KeyValue rightKeyValue,
                long ticks) {
            super(ticks, rightKeyValue.getInterpolator());
            assert rightKeyValue.getEndValue() instanceof Number;

            this.duration = ticks;
            leftInterpolator = null;

            final NumberTangentInterpolator interpolator = (rightInterpolator instanceof NumberTangentInterpolator) ? (NumberTangentInterpolator) rightInterpolator
                    : null;
            p3 = ((Number) rightKeyValue.getEndValue()).doubleValue();
            final double p2Delta = (interpolator == null) ? 0 : (interpolator
                    .getInValue() - p3)
                    * duration
                    / interpolator.getInTicks()
                    / 3;
            p2 = p3 + p2Delta;
        }

        protected double calculate(double t) {
            final double oneMinusT = 1.0 - t;
            final double tSquared = t * t;
            final double oneMinusTSquared = oneMinusT * oneMinusT;

            return oneMinusTSquared * oneMinusT * p0 + 3 * oneMinusTSquared * t
                    * p1 + 3 * oneMinusT * tSquared * p2 + tSquared * t * p3;
        }

        protected final void recalculateStartValue(double leftValue) {
            p0 = leftValue;
            final double p1Delta = (leftInterpolator == null) ? 0
                    : (leftInterpolator.getOutValue() - p0) * duration
                            / leftInterpolator.getOutTicks() / 3;
            p1 = p0 + p1Delta;
        }
    }

    private static class BooleanInterpolationInterval extends
            InterpolationInterval {

        private final WritableBooleanValue target;
        private boolean leftValue;
        private final boolean rightValue;

        private BooleanInterpolationInterval(KeyValue keyValue, long ticks,
                Object leftValue) {
            super(ticks, keyValue.getInterpolator());
            assert (keyValue.getTarget() instanceof WritableBooleanValue)
                    && (keyValue.getEndValue() instanceof Boolean)
                    && (leftValue instanceof Boolean);
            this.target = (WritableBooleanValue) keyValue.getTarget();
            this.rightValue = (Boolean) keyValue.getEndValue();
            this.leftValue = (Boolean) leftValue;
        }

        private BooleanInterpolationInterval(KeyValue keyValue, long ticks) {
            super(ticks, keyValue.getInterpolator());
            assert (keyValue.getTarget() instanceof WritableBooleanValue)
                    && (keyValue.getEndValue() instanceof Boolean);
            this.target = (WritableBooleanValue) keyValue.getTarget();
            this.rightValue = (Boolean) keyValue.getEndValue();
            this.leftValue = target.get();
        }

        @Override
        public void interpolate(double frac) {
            final boolean value = rightInterpolator.interpolate(leftValue,
                    rightValue, frac);
            target.set(value);
        }

        @Override
        public void recalculateStartValue() {
            leftValue = target.get();
        }
    }

    private static class DoubleInterpolationInterval extends
            InterpolationInterval {

        private final WritableDoubleValue target;
        private double leftValue;
        private final double rightValue;

        private DoubleInterpolationInterval(KeyValue keyValue, long ticks,
                Object leftValue) {
            super(ticks, keyValue.getInterpolator());
            assert (keyValue.getTarget() instanceof WritableDoubleValue)
                    && (keyValue.getEndValue() instanceof Number)
                    && (leftValue instanceof Number);
            this.target = (WritableDoubleValue) keyValue.getTarget();
            this.rightValue = ((Number) keyValue.getEndValue()).doubleValue();
            this.leftValue = ((Number) leftValue).doubleValue();
        }

        private DoubleInterpolationInterval(KeyValue keyValue, long ticks) {
            super(ticks, keyValue.getInterpolator());
            assert (keyValue.getTarget() instanceof WritableDoubleValue)
                    && (keyValue.getEndValue() instanceof Number);
            this.target = (WritableDoubleValue) keyValue.getTarget();
            this.rightValue = ((Number) keyValue.getEndValue()).doubleValue();
            this.leftValue = target.get();
        }

        @Override
        public void interpolate(double frac) {
            final double value = rightInterpolator.interpolate(leftValue,
                    rightValue, frac);
            target.set(value);
        }

        @Override
        public void recalculateStartValue() {
            leftValue = target.get();
        }
    }

    private static class TangentDoubleInterpolationInterval extends
            TangentInterpolationInterval {

        private final WritableDoubleValue target;

        private TangentDoubleInterpolationInterval(KeyValue rightKeyValue,
                long ticks, KeyValue leftKeyValue, long duration) {
            super(rightKeyValue, ticks, leftKeyValue, duration);
            assert rightKeyValue.getTarget() instanceof WritableDoubleValue;
            this.target = (WritableDoubleValue) rightKeyValue.getTarget();
        }

        private TangentDoubleInterpolationInterval(KeyValue rightKeyValue,
                long ticks) {
            super(rightKeyValue, ticks);
            assert rightKeyValue.getTarget() instanceof WritableDoubleValue;
            this.target = (WritableDoubleValue) rightKeyValue.getTarget();
            recalculateStartValue(target.get());
        }

        @Override
        public void interpolate(double frac) {
            target.set(calculate(frac));
        }

        @Override
        public void recalculateStartValue() {
            recalculateStartValue(target.get());
        }
    }

    private static class FloatInterpolationInterval extends
            InterpolationInterval {

        private final WritableFloatValue target;
        private float leftValue;
        private final float rightValue;

        private FloatInterpolationInterval(KeyValue keyValue, long ticks,
                Object leftValue) {
            super(ticks, keyValue.getInterpolator());
            assert (keyValue.getTarget() instanceof WritableFloatValue)
                    && (keyValue.getEndValue() instanceof Number)
                    && (leftValue instanceof Number);
            this.target = (WritableFloatValue) keyValue.getTarget();
            this.rightValue = ((Number) keyValue.getEndValue()).floatValue();
            this.leftValue = ((Number) leftValue).floatValue();
        }

        private FloatInterpolationInterval(KeyValue keyValue, long ticks) {
            super(ticks, keyValue.getInterpolator());
            assert (keyValue.getTarget() instanceof WritableFloatValue)
                    && (keyValue.getEndValue() instanceof Number);
            this.target = (WritableFloatValue) keyValue.getTarget();
            this.rightValue = ((Number) keyValue.getEndValue()).floatValue();
            this.leftValue = target.get();
        }

        @Override
        public void interpolate(double frac) {
            final float value = (float) rightInterpolator.interpolate(
                    leftValue, rightValue, frac);
            target.set(value);
        }

        @Override
        public void recalculateStartValue() {
            leftValue = target.get();
        }
    }

    private static class TangentFloatInterpolationInterval extends
            TangentInterpolationInterval {

        private final WritableFloatValue target;

        private TangentFloatInterpolationInterval(KeyValue rightKeyValue,
                long ticks, KeyValue leftKeyValue, long duration) {
            super(rightKeyValue, ticks, leftKeyValue, duration);
            assert rightKeyValue.getTarget() instanceof WritableFloatValue;
            this.target = (WritableFloatValue) rightKeyValue.getTarget();
        }

        private TangentFloatInterpolationInterval(KeyValue rightKeyValue,
                long ticks) {
            super(rightKeyValue, ticks);
            assert rightKeyValue.getTarget() instanceof WritableFloatValue;
            this.target = (WritableFloatValue) rightKeyValue.getTarget();
            recalculateStartValue(target.get());
        }

        @Override
        public void interpolate(double frac) {
            target.set((float) calculate(frac));
        }

        @Override
        public void recalculateStartValue() {
            recalculateStartValue(target.get());
        }
    }

    private static class IntegerInterpolationInterval extends
            InterpolationInterval {

        private final WritableIntegerValue target;
        private int leftValue;
        private final int rightValue;

        private IntegerInterpolationInterval(KeyValue keyValue, long ticks,
                Object leftValue) {
            super(ticks, keyValue.getInterpolator());
            assert (keyValue.getTarget() instanceof WritableIntegerValue)
                    && (keyValue.getEndValue() instanceof Number)
                    && (leftValue instanceof Number);
            this.target = (WritableIntegerValue) keyValue.getTarget();
            this.rightValue = ((Number) keyValue.getEndValue()).intValue();
            this.leftValue = ((Number) leftValue).intValue();
        }

        private IntegerInterpolationInterval(KeyValue keyValue, long ticks) {
            super(ticks, keyValue.getInterpolator());
            assert (keyValue.getTarget() instanceof WritableIntegerValue)
                    && (keyValue.getEndValue() instanceof Number);
            this.target = (WritableIntegerValue) keyValue.getTarget();
            this.rightValue = ((Number) keyValue.getEndValue()).intValue();
            this.leftValue = target.get();
        }

        @Override
        public void interpolate(double frac) {
            final int value = rightInterpolator.interpolate(leftValue,
                    rightValue, frac);
            target.set(value);
        }

        @Override
        public void recalculateStartValue() {
            leftValue = target.get();
        }
    }

    private static class TangentIntegerInterpolationInterval extends
            TangentInterpolationInterval {

        private final WritableIntegerValue target;

        private TangentIntegerInterpolationInterval(KeyValue rightKeyValue,
                long ticks, KeyValue leftKeyValue, long duration) {
            super(rightKeyValue, ticks, leftKeyValue, duration);
            assert rightKeyValue.getTarget() instanceof WritableIntegerValue;
            this.target = (WritableIntegerValue) rightKeyValue.getTarget();
        }

        private TangentIntegerInterpolationInterval(KeyValue rightKeyValue,
                long ticks) {
            super(rightKeyValue, ticks);
            assert rightKeyValue.getTarget() instanceof WritableIntegerValue;
            this.target = (WritableIntegerValue) rightKeyValue.getTarget();
            recalculateStartValue(target.get());
        }

        @Override
        public void interpolate(double frac) {
            target.set((int) Math.round(calculate(frac)));
        }

        @Override
        public void recalculateStartValue() {
            recalculateStartValue(target.get());
        }
    }

    private static class LongInterpolationInterval extends
            InterpolationInterval {

        private final WritableLongValue target;
        private long leftValue;
        private final long rightValue;

        private LongInterpolationInterval(KeyValue keyValue, long ticks,
                Object leftValue) {
            super(ticks, keyValue.getInterpolator());
            assert (keyValue.getTarget() instanceof WritableLongValue)
                    && (keyValue.getEndValue() instanceof Number)
                    && (leftValue instanceof Number);
            this.target = (WritableLongValue) keyValue.getTarget();
            this.rightValue = ((Number) keyValue.getEndValue()).longValue();
            this.leftValue = ((Number) leftValue).longValue();
        }

        private LongInterpolationInterval(KeyValue keyValue, long ticks) {
            super(ticks, keyValue.getInterpolator());
            assert (keyValue.getTarget() instanceof WritableLongValue)
                    && (keyValue.getEndValue() instanceof Number);
            this.target = (WritableLongValue) keyValue.getTarget();
            this.rightValue = ((Number) keyValue.getEndValue()).longValue();
            this.leftValue = target.get();
        }

        @Override
        public void interpolate(double frac) {
            final long value = rightInterpolator.interpolate(leftValue,
                    rightValue, frac);
            target.set(value);
        }

        @Override
        public void recalculateStartValue() {
            leftValue = target.get();
        }
    }

    private static class TangentLongInterpolationInterval extends
            TangentInterpolationInterval {

        private final WritableLongValue target;

        private TangentLongInterpolationInterval(KeyValue rightKeyValue,
                long ticks, KeyValue leftKeyValue, long duration) {
            super(rightKeyValue, ticks, leftKeyValue, duration);
            assert rightKeyValue.getTarget() instanceof WritableLongValue;
            this.target = (WritableLongValue) rightKeyValue.getTarget();
        }

        private TangentLongInterpolationInterval(KeyValue rightKeyValue,
                long ticks) {
            super(rightKeyValue, ticks);
            assert rightKeyValue.getTarget() instanceof WritableLongValue;
            this.target = (WritableLongValue) rightKeyValue.getTarget();
            recalculateStartValue(target.get());
        }

        @Override
        public void interpolate(double frac) {
            target.set(Math.round(calculate(frac)));
        }

        @Override
        public void recalculateStartValue() {
            recalculateStartValue(target.get());
        }
    }

    private static class ObjectInterpolationInterval extends
            InterpolationInterval {

        @SuppressWarnings("rawtypes")
        private final WritableValue target;
        private Object leftValue;
        private final Object rightValue;

        private ObjectInterpolationInterval(KeyValue keyValue, long ticks,
                Object leftValue) {
            super(ticks, keyValue.getInterpolator());
            this.target = keyValue.getTarget();
            this.rightValue = keyValue.getEndValue();
            this.leftValue = leftValue;
        }

        private ObjectInterpolationInterval(KeyValue keyValue, long ticks) {
            super(ticks, keyValue.getInterpolator());
            this.target = keyValue.getTarget();
            this.rightValue = keyValue.getEndValue();
            this.leftValue = target.getValue();
        }

        @SuppressWarnings("unchecked")
        @Override
        public void interpolate(double frac) {
            final Object value = rightInterpolator.interpolate(leftValue,
                    rightValue, frac);
            target.setValue(value);
        }

        @Override
        public void recalculateStartValue() {
            leftValue = target.getValue();
        }
    }

}
