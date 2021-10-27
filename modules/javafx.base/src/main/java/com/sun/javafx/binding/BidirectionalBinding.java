/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.binding;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.util.StringConverter;

import java.lang.ref.WeakReference;
import java.text.Format;
import java.text.ParseException;
import java.util.Objects;

import static com.sun.javafx.beans.BeanErrors.*;

/**
 * @implNote Bidirectional bindings are implemented with InvalidationListeners, which are fired once
 *           when the changed property was valid, but elided for any future changes until the property
 *           is again validated by calling its getValue()/get() method.
 *           Since bidirectional bindings require that we observe all property changes, independent
 *           of whether the property was validated by user code, we manually validate both properties
 *           by calling their getter method in all relevant places.
 */
public abstract class BidirectionalBinding implements InvalidationListener, WeakListener {

    /**
     * Exceptions thrown here will be surfaced by {@link Bindings#bindBidirectional(Property, Property)}
     * and by {@link Property#bindBidirectional(Property)}. In the latter case, the 'target'
     * argument is the 'this' pointer of the Property class. If 'this' is a bound property, we
     * throw an IllegalStateException instead of an IllegalArgumentException, because from the
     * perspective of a user of the Property class, 'target' was not specified as an argument.
     *
     * However, when using the Bindings class to set up a bidirectional binding, we catch the
     * IllegalStateException and re-throw it as an IllegalArgumentException. From the perspective
     * of a user of the Bindings class, 'target' was specified as an argument.
     */
    private static void checkBindParameters(Property<?> target, Property<?> source) {
        if (target == null) {
            throw new NullPointerException(BINDING_TARGET_NULL.getMessage());
        }

        if (source == null) {
            throw new NullPointerException(BINDING_SOURCE_NULL.getMessage(target));
        }

        if (target == source) {
            throw new IllegalArgumentException(CANNOT_BIND_PROPERTY_TO_ITSELF.getMessage(target));
        }

        if (target.isBound()) {
            throw new IllegalStateException(BIND_CONFLICT_BIDIRECTIONAL.getMessage(target));
        }

        if (source.isBound()) {
            throw new IllegalArgumentException(BIND_CONFLICT_BIDIRECTIONAL.getMessage(source));
        }
    }

    private static void checkUnbindParameters(Property<?> target, Property<?> source) {
        if (target == null) {
            throw new NullPointerException(BINDING_TARGET_NULL.getMessage());
        }

        if (source == null) {
            throw new NullPointerException(BINDING_SOURCE_NULL.getMessage(target));
        }

        if (target == source) {
            throw new IllegalArgumentException(CANNOT_UNBIND_PROPERTY_FROM_ITSELF.getMessage(target));
        }
    }

    public static <T> BidirectionalBinding bind(Property<T> target, Property<T> source) {
        checkBindParameters(target, source);
        final BidirectionalBinding binding =
                ((target instanceof DoubleProperty) && (source instanceof DoubleProperty)) ?
                        new BidirectionalDoubleBinding((DoubleProperty) target, (DoubleProperty) source)
                : ((target instanceof FloatProperty) && (source instanceof FloatProperty)) ?
                        new BidirectionalFloatBinding((FloatProperty) target, (FloatProperty) source)
                : ((target instanceof IntegerProperty) && (source instanceof IntegerProperty)) ?
                        new BidirectionalIntegerBinding((IntegerProperty) target, (IntegerProperty) source)
                : ((target instanceof LongProperty) && (source instanceof LongProperty)) ?
                        new BidirectionalLongBinding((LongProperty) target, (LongProperty) source)
                : ((target instanceof BooleanProperty) && (source instanceof BooleanProperty)) ?
                        new BidirectionalBooleanBinding((BooleanProperty) target, (BooleanProperty) source)
                : new TypedGenericBidirectionalBinding<T>(target, source);

        // We can't know whether the two properties are already bidirectionally bound.
        // However, since BidirectionalBinding instances are equal if their endpoints are identical, removing
        // the newly-created binding has the effect of removing an already existing bidirectional binding.
        target.removeListener(binding);
        source.removeListener(binding);

        target.setValue(source.getValue());
        target.getValue();
        target.addListener(binding);
        source.addListener(binding);

        return binding;
    }

    public static Object bind(Property<String> target, Property<?> source, Format format) {
        Objects.requireNonNull(format, "Format cannot be null.");
        checkBindParameters(target, source);
        final var binding = new StringFormatBidirectionalBinding(target, source, format);
        target.removeListener(binding);
        source.removeListener(binding);
        target.setValue(format.format(source.getValue()));
        target.getValue();
        target.addListener(binding);
        source.addListener(binding);
        return binding;
    }

    public static <T> Object bind(Property<String> target, Property<T> source, StringConverter<T> converter) {
        Objects.requireNonNull(converter, "Converter cannot be null.");
        checkBindParameters(target, source);
        final var binding = new StringConverterBidirectionalBinding<>(target, source, converter);
        target.removeListener(binding);
        source.removeListener(binding);
        target.setValue(converter.toString(source.getValue()));
        target.getValue();
        target.addListener(binding);
        source.addListener(binding);
        return binding;
    }

    public static void unbind(Property<?> target, Property<?> source) {
        checkUnbindParameters(target, source);
        final BidirectionalBinding binding = new UntypedGenericBidirectionalBinding(target, source);
        target.removeListener(binding);
        source.removeListener(binding);
    }

    public static BidirectionalBinding bindNumber(Property<Integer> target, IntegerProperty source) {
        return bindNumber(target, (Property<Number>)source);
    }

    public static BidirectionalBinding bindNumber(Property<Long> target, LongProperty source) {
        return bindNumber(target, (Property<Number>)source);
    }

    public static BidirectionalBinding bindNumber(Property<Float> target, FloatProperty source) {
        return bindNumber(target, (Property<Number>)source);
    }

    public static BidirectionalBinding bindNumber(Property<Double> target, DoubleProperty source) {
        return bindNumber(target, (Property<Number>)source);
    }

    public static BidirectionalBinding bindNumber(IntegerProperty target, Property<Integer> source) {
        return bindNumberObject(target, source);
    }

    public static BidirectionalBinding bindNumber(LongProperty target, Property<Long> source) {
        return bindNumberObject(target, source);
    }

    public static BidirectionalBinding bindNumber(FloatProperty target, Property<Float> source) {
        return bindNumberObject(target, source);
    }

    public static BidirectionalBinding bindNumber(DoubleProperty target, Property<Double> source) {
        return bindNumberObject(target, source);
    }

    private static <T extends Number> BidirectionalBinding bindNumberObject(Property<Number> target, Property<T> source) {
        checkBindParameters(target, source);
        final BidirectionalBinding binding = new TypedNumberBidirectionalBinding<>(source, target);
        target.removeListener(binding);
        source.removeListener(binding);
        target.setValue(source.getValue());
        target.getValue();
        target.addListener(binding);
        source.addListener(binding);
        return binding;
    }

    private static <T extends Number> BidirectionalBinding bindNumber(Property<T> target, Property<Number> source) {
        checkBindParameters(target, source);
        final BidirectionalBinding binding = new TypedNumberBidirectionalBinding<>(target, source);
        target.removeListener(binding);
        source.removeListener(binding);
        target.setValue((T)source.getValue());
        target.getValue();
        target.addListener(binding);
        source.addListener(binding);
        return binding;
    }

    private final int cachedHashCode;

    private BidirectionalBinding(Property<?> property1, Property<?> property2) {
        cachedHashCode = property1.hashCode() * property2.hashCode();
    }

    protected abstract Property<?> getProperty1();

    protected abstract Property<?> getProperty2();

    @Override
    public int hashCode() {
        return cachedHashCode;
    }

    @Override
    public boolean wasGarbageCollected() {
        return (getProperty1() == null) || (getProperty2() == null);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        final Property<?> propertyA1 = getProperty1();
        final Property<?> propertyA2 = getProperty2();
        if ((propertyA1 == null) || (propertyA2 == null)) {
            return false;
        }

        if (obj instanceof BidirectionalBinding) {
            final BidirectionalBinding otherBinding = (BidirectionalBinding) obj;
            final Property<?> propertyB1 = otherBinding.getProperty1();
            final Property<?> propertyB2 = otherBinding.getProperty2();
            if ((propertyB1 == null) || (propertyB2 == null)) {
                return false;
            }

            if (propertyA1 == propertyB1 && propertyA2 == propertyB2) {
                return true;
            }
            if (propertyA1 == propertyB2 && propertyA2 == propertyB1) {
                return true;
            }
        }
        return false;
    }

    private static class BidirectionalBooleanBinding extends BidirectionalBinding {
        private final WeakReference<BooleanProperty> propertyRef1;
        private final WeakReference<BooleanProperty> propertyRef2;
        private boolean oldValue;
        private boolean updating;

        private BidirectionalBooleanBinding(BooleanProperty property1, BooleanProperty property2) {
            super(property1, property2);
            oldValue = property1.get();
            propertyRef1 = new WeakReference<>(property1);
            propertyRef2 = new WeakReference<>(property2);
        }

        @Override
        protected Property<Boolean> getProperty1() {
            return propertyRef1.get();
        }

        @Override
        protected Property<Boolean> getProperty2() {
            return propertyRef2.get();
        }

        @Override
        public void invalidated(Observable sourceProperty) {
            if (!updating) {
                final BooleanProperty property1 = propertyRef1.get();
                final BooleanProperty property2 = propertyRef2.get();
                if ((property1 == null) || (property2 == null)) {
                    if (property1 != null) {
                        property1.removeListener(this);
                    }
                    if (property2 != null) {
                        property2.removeListener(this);
                    }
                } else {
                    try {
                        updating = true;
                        if (property1 == sourceProperty) {
                            boolean newValue = property1.get();
                            property2.set(newValue);
                            property2.get();
                            oldValue = newValue;
                        } else {
                            boolean newValue = property2.get();
                            property1.set(newValue);
                            property1.get();
                            oldValue = newValue;
                        }
                    } catch (RuntimeException e) {
                        try {
                            if (property1 == sourceProperty) {
                                property1.set(oldValue);
                                property1.get();
                            } else {
                                property2.set(oldValue);
                                property2.get();
                            }
                        } catch (Exception e2) {
                            e2.addSuppressed(e);
                            unbind(property1, property2);
                            throw new RuntimeException(
                                "Bidirectional binding failed together with an attempt"
                                        + " to restore the source property to the previous value."
                                        + " Removing the bidirectional binding from properties " +
                                        property1 + " and " + property2, e2);
                        }
                        throw new RuntimeException(
                                "Bidirectional binding failed, setting to the previous value", e);
                    } finally {
                        updating = false;
                    }
                }
            }
        }
    }

    private static class BidirectionalDoubleBinding extends BidirectionalBinding {
        private final WeakReference<DoubleProperty> propertyRef1;
        private final WeakReference<DoubleProperty> propertyRef2;
        private double oldValue;
        private boolean updating = false;

        private BidirectionalDoubleBinding(DoubleProperty property1, DoubleProperty property2) {
            super(property1, property2);
            oldValue = property1.get();
            propertyRef1 = new WeakReference<>(property1);
            propertyRef2 = new WeakReference<>(property2);
        }

        @Override
        protected Property<Number> getProperty1() {
            return propertyRef1.get();
        }

        @Override
        protected Property<Number> getProperty2() {
            return propertyRef2.get();
        }

        @Override
        public void invalidated(Observable sourceProperty) {
            if (!updating) {
                final DoubleProperty property1 = propertyRef1.get();
                final DoubleProperty property2 = propertyRef2.get();
                if ((property1 == null) || (property2 == null)) {
                    if (property1 != null) {
                        property1.removeListener(this);
                    }
                    if (property2 != null) {
                        property2.removeListener(this);
                    }
                } else {
                    try {
                        updating = true;
                        if (property1 == sourceProperty) {
                            double newValue = property1.get();
                            property2.set(newValue);
                            property2.get();
                            oldValue = newValue;
                        } else {
                            double newValue = property2.get();
                            property1.set(newValue);
                            property1.get();
                            oldValue = newValue;
                        }
                    } catch (RuntimeException e) {
                        try {
                            if (property1 == sourceProperty) {
                                property1.set(oldValue);
                                property1.get();
                            } else {
                                property2.set(oldValue);
                                property2.get();
                            }
                        } catch (Exception e2) {
                            e2.addSuppressed(e);
                            unbind(property1, property2);
                            throw new RuntimeException(
                                "Bidirectional binding failed together with an attempt"
                                        + " to restore the source property to the previous value."
                                        + " Removing the bidirectional binding from properties " +
                                        property1 + " and " + property2, e2);
                        }
                        throw new RuntimeException(
                                        "Bidirectional binding failed, setting to the previous value", e);
                    } finally {
                        updating = false;
                    }
                }
            }
        }
    }

    private static class BidirectionalFloatBinding extends BidirectionalBinding {
        private final WeakReference<FloatProperty> propertyRef1;
        private final WeakReference<FloatProperty> propertyRef2;
        private float oldValue;
        private boolean updating = false;

        private BidirectionalFloatBinding(FloatProperty property1, FloatProperty property2) {
            super(property1, property2);
            oldValue = property1.get();
            propertyRef1 = new WeakReference<>(property1);
            propertyRef2 = new WeakReference<>(property2);
        }

        @Override
        protected Property<Number> getProperty1() {
            return propertyRef1.get();
        }

        @Override
        protected Property<Number> getProperty2() {
            return propertyRef2.get();
        }

        @Override
        public void invalidated(Observable sourceProperty) {
            if (!updating) {
                final FloatProperty property1 = propertyRef1.get();
                final FloatProperty property2 = propertyRef2.get();
                if ((property1 == null) || (property2 == null)) {
                    if (property1 != null) {
                        property1.removeListener(this);
                    }
                    if (property2 != null) {
                        property2.removeListener(this);
                    }
                } else {
                    try {
                        updating = true;
                        if (property1 == sourceProperty) {
                            float newValue = property1.get();
                            property2.set(newValue);
                            property2.get();
                            oldValue = newValue;
                        } else {
                            float newValue = property2.get();
                            property1.set(newValue);
                            property1.get();
                            oldValue = newValue;
                        }
                    } catch (RuntimeException e) {
                        try {
                            if (property1 == sourceProperty) {
                                property1.set(oldValue);
                                property1.get();
                            } else {
                                property2.set(oldValue);
                                property2.get();
                            }
                        } catch (Exception e2) {
                            e2.addSuppressed(e);
                            unbind(property1, property2);
                            throw new RuntimeException(
                                "Bidirectional binding failed together with an attempt"
                                        + " to restore the source property to the previous value."
                                        + " Removing the bidirectional binding from properties " +
                                        property1 + " and " + property2, e2);
                        }
                        throw new RuntimeException(
                                "Bidirectional binding failed, setting to the previous value", e);
                    } finally {
                        updating = false;
                    }
                }
            }
        }
    }

    private static class BidirectionalIntegerBinding extends BidirectionalBinding {
        private final WeakReference<IntegerProperty> propertyRef1;
        private final WeakReference<IntegerProperty> propertyRef2;
        private int oldValue;
        private boolean updating = false;

        private BidirectionalIntegerBinding(IntegerProperty property1, IntegerProperty property2) {
            super(property1, property2);
            oldValue = property1.get();
            propertyRef1 = new WeakReference<>(property1);
            propertyRef2 = new WeakReference<>(property2);
        }

        @Override
        protected Property<Number> getProperty1() {
            return propertyRef1.get();
        }

        @Override
        protected Property<Number> getProperty2() {
            return propertyRef2.get();
        }

        @Override
        public void invalidated(Observable sourceProperty) {
            if (!updating) {
                final IntegerProperty property1 = propertyRef1.get();
                final IntegerProperty property2 = propertyRef2.get();
                if ((property1 == null) || (property2 == null)) {
                    if (property1 != null) {
                        property1.removeListener(this);
                    }
                    if (property2 != null) {
                        property2.removeListener(this);
                    }
                } else {
                    try {
                        updating = true;
                        if (property1 == sourceProperty) {
                            int newValue = property1.get();
                            property2.set(newValue);
                            property2.get();
                            oldValue = newValue;
                        } else {
                            int newValue = property2.get();
                            property1.set(newValue);
                            property1.get();
                            oldValue = newValue;
                        }
                    } catch (RuntimeException e) {
                        try {
                            if (property1 == sourceProperty) {
                                property1.set(oldValue);
                                property1.get();
                            } else {
                                property2.set(oldValue);
                                property2.get();
                            }
                        } catch (Exception e2) {
                            e2.addSuppressed(e);
                            unbind(property1, property2);
                            throw new RuntimeException(
                                "Bidirectional binding failed together with an attempt"
                                        + " to restore the source property to the previous value."
                                        + " Removing the bidirectional binding from properties " +
                                        property1 + " and " + property2, e2);
                        }
                        throw new RuntimeException(
                                        "Bidirectional binding failed, setting to the previous value", e);
                    } finally {
                        updating = false;
                    }
                }
            }
        }
    }

    private static class BidirectionalLongBinding extends BidirectionalBinding {
        private final WeakReference<LongProperty> propertyRef1;
        private final WeakReference<LongProperty> propertyRef2;
        private long oldValue;
        private boolean updating = false;

        private BidirectionalLongBinding(LongProperty property1, LongProperty property2) {
            super(property1, property2);
            oldValue = property1.get();
            propertyRef1 = new WeakReference<>(property1);
            propertyRef2 = new WeakReference<>(property2);
        }

        @Override
        protected Property<Number> getProperty1() {
            return propertyRef1.get();
        }

        @Override
        protected Property<Number> getProperty2() {
            return propertyRef2.get();
        }

        @Override
        public void invalidated(Observable sourceProperty) {
            if (!updating) {
                final LongProperty property1 = propertyRef1.get();
                final LongProperty property2 = propertyRef2.get();
                if ((property1 == null) || (property2 == null)) {
                    if (property1 != null) {
                        property1.removeListener(this);
                    }
                    if (property2 != null) {
                        property2.removeListener(this);
                    }
                } else {
                    try {
                        updating = true;
                        if (property1 == sourceProperty) {
                            long newValue = property1.get();
                            property2.set(newValue);
                            property2.get();
                            oldValue = newValue;
                        } else {
                            long newValue = property2.get();
                            property1.set(newValue);
                            property1.get();
                            oldValue = newValue;
                        }
                    } catch (RuntimeException e) {
                        try {
                            if (property1 == sourceProperty) {
                                property1.set(oldValue);
                                property1.get();
                            } else {
                                property2.set(oldValue);
                                property2.get();
                            }
                        } catch (Exception e2) {
                            e2.addSuppressed(e);
                            unbind(property1, property2);
                            throw new RuntimeException(
                                "Bidirectional binding failed together with an attempt"
                                        + " to restore the source property to the previous value."
                                        + " Removing the bidirectional binding from properties " +
                                        property1 + " and " + property2, e2);
                        }
                        throw new RuntimeException(
                                "Bidirectional binding failed, setting to the previous value", e);
                    } finally {
                        updating = false;
                    }
                }
            }
        }
    }

    private static class TypedGenericBidirectionalBinding<T> extends BidirectionalBinding {
        private final WeakReference<Property<T>> propertyRef1;
        private final WeakReference<Property<T>> propertyRef2;
        private T oldValue;
        private boolean updating = false;

        private TypedGenericBidirectionalBinding(Property<T> property1, Property<T> property2) {
            super(property1, property2);
            oldValue = property1.getValue();
            propertyRef1 = new WeakReference<>(property1);
            propertyRef2 = new WeakReference<>(property2);
        }

        @Override
        protected Property<T> getProperty1() {
            return propertyRef1.get();
        }

        @Override
        protected Property<T> getProperty2() {
            return propertyRef2.get();
        }

        @Override
        public void invalidated(Observable sourceProperty) {
            if (!updating) {
                final Property<T> property1 = propertyRef1.get();
                final Property<T> property2 = propertyRef2.get();
                if ((property1 == null) || (property2 == null)) {
                    if (property1 != null) {
                        property1.removeListener(this);
                    }
                    if (property2 != null) {
                        property2.removeListener(this);
                    }
                } else {
                    try {
                        updating = true;
                        if (property1 == sourceProperty) {
                            T newValue = property1.getValue();
                            property2.setValue(newValue);
                            property2.getValue();
                            oldValue = newValue;
                        } else {
                            T newValue = property2.getValue();
                            property1.setValue(newValue);
                            property1.getValue();
                            oldValue = newValue;
                        }
                    } catch (RuntimeException e) {
                        try {
                            if (property1 == sourceProperty) {
                                property1.setValue(oldValue);
                                property1.getValue();
                            } else {
                                property2.setValue(oldValue);
                                property2.getValue();
                            }
                        } catch (Exception e2) {
                            e2.addSuppressed(e);
                            unbind(property1, property2);
                            throw new RuntimeException(
                                "Bidirectional binding failed together with an attempt"
                                        + " to restore the source property to the previous value."
                                        + " Removing the bidirectional binding from properties " +
                                        property1 + " and " + property2, e2);
                        }
                        throw new RuntimeException(
                                "Bidirectional binding failed, setting to the previous value", e);
                    } finally {
                        updating = false;
                    }
                }
            }
        }
    }

    private static class TypedNumberBidirectionalBinding<T extends Number> extends BidirectionalBinding {
        private final WeakReference<Property<T>> propertyRef1;
        private final WeakReference<Property<Number>> propertyRef2;
        private T oldValue;
        private boolean updating = false;

        private TypedNumberBidirectionalBinding(Property<T> property1, Property<Number> property2) {
            super(property1, property2);
            oldValue = property1.getValue();
            propertyRef1 = new WeakReference<>(property1);
            propertyRef2 = new WeakReference<>(property2);
        }

        @Override
        protected Property<T> getProperty1() {
            return propertyRef1.get();
        }

        @Override
        protected Property<Number> getProperty2() {
            return propertyRef2.get();
        }

        @Override
        public void invalidated(Observable sourceProperty) {
            if (!updating) {
                final Property<T> property1 = propertyRef1.get();
                final Property<Number> property2 = propertyRef2.get();
                if ((property1 == null) || (property2 == null)) {
                    if (property1 != null) {
                        property1.removeListener(this);
                    }
                    if (property2 != null) {
                        property2.removeListener(this);
                    }
                } else {
                    try {
                        updating = true;
                        if (property1 == sourceProperty) {
                            T newValue = property1.getValue();
                            property2.setValue(newValue);
                            property2.getValue();
                            oldValue = newValue;
                        } else {
                            T newValue = (T)property2.getValue();
                            property1.setValue(newValue);
                            property1.getValue();
                            oldValue = newValue;
                        }
                    } catch (RuntimeException e) {
                        try {
                            if (property1 == sourceProperty) {
                                property1.setValue((T)oldValue);
                                property1.getValue();
                            } else {
                                property2.setValue(oldValue);
                                property2.getValue();
                            }
                        } catch (Exception e2) {
                            e2.addSuppressed(e);
                            unbind(property1, property2);
                            throw new RuntimeException(
                                "Bidirectional binding failed together with an attempt"
                                        + " to restore the source property to the previous value."
                                        + " Removing the bidirectional binding from properties " +
                                        property1 + " and " + property2, e2);
                        }
                        throw new RuntimeException(
                                        "Bidirectional binding failed, setting to the previous value", e);
                    } finally {
                        updating = false;
                    }
                }
            }
        }
    }

    private static class UntypedGenericBidirectionalBinding extends BidirectionalBinding {
        private final Property<?> property1;
        private final Property<?> property2;

        public UntypedGenericBidirectionalBinding(Property<?> property1, Property<?> property2) {
            super(property1, property2);
            this.property1 = property1;
            this.property2 = property2;
        }

        @Override
        protected Property<?> getProperty1() {
            return property1;
        }

        @Override
        protected Property<?> getProperty2() {
            return property2;
        }

        @Override
        public void invalidated(Observable sourceProperty) {
            throw new RuntimeException("Should not reach here");
        }
    }

    public abstract static class StringConversionBidirectionalBinding<T> extends BidirectionalBinding {
        private final WeakReference<Property<String>> stringPropertyRef;
        private final WeakReference<Property<T>> otherPropertyRef;
        private boolean updating;

        public StringConversionBidirectionalBinding(Property<String> stringProperty, Property<T> otherProperty) {
            super(stringProperty, otherProperty);
            stringPropertyRef = new WeakReference<>(stringProperty);
            otherPropertyRef = new WeakReference<>(otherProperty);
        }

        protected abstract String toString(T value);

        protected abstract T fromString(String value) throws ParseException;

        @Override
        protected Property<?> getProperty1() {
            return stringPropertyRef.get();
        }

        @Override
        protected Property<?> getProperty2() {
            return otherPropertyRef.get();
        }

        @Override
        public void invalidated(Observable observable) {
            if (!updating) {
                final Property<String> property1 = stringPropertyRef.get();
                final Property<T> property2 = otherPropertyRef.get();
                if ((property1 == null) || (property2 == null)) {
                    if (property1 != null) {
                        property1.removeListener(this);
                    }
                    if (property2 != null) {
                        property2.removeListener(this);
                    }
                } else {
                    try {
                        updating = true;
                        if (property1 == observable) {
                            try {
                                property2.setValue(fromString(property1.getValue()));
                                property2.getValue();
                            } catch (Exception e) {
                                Logging.getLogger().warning("Exception while parsing String in bidirectional binding", e);
                                property2.setValue(null);
                                property2.getValue();
                            }
                        } else {
                            try {
                                property1.setValue(toString(property2.getValue()));
                                property1.getValue();
                            } catch (Exception e) {
                                Logging.getLogger().warning("Exception while converting Object to String in bidirectional binding", e);
                                property1.setValue("");
                                property1.getValue();
                            }
                        }
                    } finally {
                        updating = false;
                    }
                }
            }
        }
    }

    private static class StringFormatBidirectionalBinding extends StringConversionBidirectionalBinding {
        private final Format format;

        @SuppressWarnings("unchecked")
        public StringFormatBidirectionalBinding(Property<String> stringProperty, Property<?> otherProperty, Format format) {
            super(stringProperty, otherProperty);
            this.format = format;
        }

        @Override
        protected String toString(Object value) {
            return format.format(value);
        }

        @Override
        protected Object fromString(String value) throws ParseException {
            return format.parseObject(value);
        }
    }

    private static class StringConverterBidirectionalBinding<T> extends StringConversionBidirectionalBinding<T> {
        private final StringConverter<T> converter;

        public StringConverterBidirectionalBinding(Property<String> stringProperty, Property<T> otherProperty, StringConverter<T> converter) {
            super(stringProperty, otherProperty);
            this.converter = converter;
        }

        @Override
        protected String toString(T value) {
            return converter.toString(value);
        }

        @Override
        protected T fromString(String value) throws ParseException {
            return converter.fromString(value);
        }
    }
}
