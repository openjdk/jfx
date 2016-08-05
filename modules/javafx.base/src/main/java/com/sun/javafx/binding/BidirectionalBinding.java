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

package com.sun.javafx.binding;

import javafx.beans.Observable;
import javafx.beans.WeakListener;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.util.StringConverter;

import java.lang.ref.WeakReference;
import java.text.Format;
import java.text.ParseException;

public abstract class BidirectionalBinding<T> implements ChangeListener<T>, WeakListener {

    private static void checkParameters(Object property1, Object property2) {
        if ((property1 == null) || (property2 == null)) {
            throw new NullPointerException("Both properties must be specified.");
        }
        if (property1 == property2) {
            throw new IllegalArgumentException("Cannot bind property to itself");
        }
    }

    public static <T> BidirectionalBinding bind(Property<T> property1, Property<T> property2) {
        checkParameters(property1, property2);
        final BidirectionalBinding binding =
                ((property1 instanceof DoubleProperty) && (property2 instanceof DoubleProperty)) ?
                        new BidirectionalDoubleBinding((DoubleProperty) property1, (DoubleProperty) property2)
                : ((property1 instanceof FloatProperty) && (property2 instanceof FloatProperty)) ?
                        new BidirectionalFloatBinding((FloatProperty) property1, (FloatProperty) property2)
                : ((property1 instanceof IntegerProperty) && (property2 instanceof IntegerProperty)) ?
                        new BidirectionalIntegerBinding((IntegerProperty) property1, (IntegerProperty) property2)
                : ((property1 instanceof LongProperty) && (property2 instanceof LongProperty)) ?
                        new BidirectionalLongBinding((LongProperty) property1, (LongProperty) property2)
                : ((property1 instanceof BooleanProperty) && (property2 instanceof BooleanProperty)) ?
                        new BidirectionalBooleanBinding((BooleanProperty) property1, (BooleanProperty) property2)
                : new TypedGenericBidirectionalBinding<T>(property1, property2);
        property1.setValue(property2.getValue());
        property1.addListener(binding);
        property2.addListener(binding);
        return binding;
    }

    public static Object bind(Property<String> stringProperty, Property<?> otherProperty, Format format) {
        checkParameters(stringProperty, otherProperty);
        if (format == null) {
            throw new NullPointerException("Format cannot be null");
        }
        final StringConversionBidirectionalBinding<?> binding = new StringFormatBidirectionalBinding(stringProperty, otherProperty, format);
        stringProperty.setValue(format.format(otherProperty.getValue()));
        stringProperty.addListener(binding);
        otherProperty.addListener(binding);
        return binding;
    }

    public static <T> Object bind(Property<String> stringProperty, Property<T> otherProperty, StringConverter<T> converter) {
        checkParameters(stringProperty, otherProperty);
        if (converter == null) {
            throw new NullPointerException("Converter cannot be null");
        }
        final StringConversionBidirectionalBinding<T> binding = new StringConverterBidirectionalBinding<T>(stringProperty, otherProperty, converter);
        stringProperty.setValue(converter.toString(otherProperty.getValue()));
        stringProperty.addListener(binding);
        otherProperty.addListener(binding);
        return binding;
    }

    public static <T> void unbind(Property<T> property1, Property<T> property2) {
        checkParameters(property1, property2);
        final BidirectionalBinding binding = new UntypedGenericBidirectionalBinding(property1, property2);
        property1.removeListener(binding);
        property2.removeListener(binding);
    }

    public static void unbind(Object property1, Object property2) {
        checkParameters(property1, property2);
        final BidirectionalBinding binding = new UntypedGenericBidirectionalBinding(property1, property2);
        if (property1 instanceof ObservableValue) {
            ((ObservableValue) property1).removeListener(binding);
        }
        if (property2 instanceof ObservableValue) {
            ((ObservableValue) property2).removeListener(binding);
        }
    }

    public static BidirectionalBinding bindNumber(Property<Integer> property1, IntegerProperty property2) {
        return bindNumber(property1, (Property<Number>)property2);
    }

    public static BidirectionalBinding bindNumber(Property<Long> property1, LongProperty property2) {
        return bindNumber(property1, (Property<Number>)property2);
    }

    public static BidirectionalBinding bindNumber(Property<Float> property1, FloatProperty property2) {
        return bindNumber(property1, (Property<Number>)property2);
    }

    public static BidirectionalBinding bindNumber(Property<Double> property1, DoubleProperty property2) {
        return bindNumber(property1, (Property<Number>)property2);
    }

    public static BidirectionalBinding bindNumber(IntegerProperty property1, Property<Integer> property2) {
        return bindNumberObject(property1, property2);
    }

    public static BidirectionalBinding bindNumber(LongProperty property1, Property<Long> property2) {
        return bindNumberObject(property1, property2);
    }

    public static BidirectionalBinding bindNumber(FloatProperty property1, Property<Float> property2) {
        return bindNumberObject(property1, property2);
    }

    public static BidirectionalBinding bindNumber(DoubleProperty property1, Property<Double> property2) {
        return bindNumberObject(property1, property2);
    }

    private static <T extends Number> BidirectionalBinding bindNumberObject(Property<Number> property1, Property<T> property2) {
        checkParameters(property1, property2);

        final BidirectionalBinding<Number> binding = new TypedNumberBidirectionalBinding<T>(property2, property1);

        property1.setValue(property2.getValue());
        property1.addListener(binding);
        property2.addListener(binding);
        return binding;
    }

    private static <T extends Number> BidirectionalBinding bindNumber(Property<T> property1, Property<Number> property2) {
        checkParameters(property1, property2);

        final BidirectionalBinding<Number> binding = new TypedNumberBidirectionalBinding<T>(property1, property2);

        property1.setValue((T)property2.getValue());
        property1.addListener(binding);
        property2.addListener(binding);
        return binding;
    }

    public static <T extends Number> void unbindNumber(Property<T> property1, Property<Number> property2) {
        checkParameters(property1, property2);
        final BidirectionalBinding binding = new UntypedGenericBidirectionalBinding(property1, property2);
        if (property1 instanceof ObservableValue) {
            ((ObservableValue) property1).removeListener(binding);
        }
        if (property2 instanceof Observable) {
            ((ObservableValue) property2).removeListener(binding);
        }
    }

    private final int cachedHashCode;

    private BidirectionalBinding(Object property1, Object property2) {
        cachedHashCode = property1.hashCode() * property2.hashCode();
    }

    protected abstract Object getProperty1();

    protected abstract Object getProperty2();

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

        final Object propertyA1 = getProperty1();
        final Object propertyA2 = getProperty2();
        if ((propertyA1 == null) || (propertyA2 == null)) {
            return false;
        }

        if (obj instanceof BidirectionalBinding) {
            final BidirectionalBinding otherBinding = (BidirectionalBinding) obj;
            final Object propertyB1 = otherBinding.getProperty1();
            final Object propertyB2 = otherBinding.getProperty2();
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

    private static class BidirectionalBooleanBinding extends BidirectionalBinding<Boolean> {
        private final WeakReference<BooleanProperty> propertyRef1;
        private final WeakReference<BooleanProperty> propertyRef2;
        private boolean updating = false;

        private BidirectionalBooleanBinding(BooleanProperty property1, BooleanProperty property2) {
            super(property1, property2);
            propertyRef1 = new WeakReference<BooleanProperty>(property1);
            propertyRef2 = new WeakReference<BooleanProperty>(property2);
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
        public void changed(ObservableValue<? extends Boolean> sourceProperty, Boolean oldValue, Boolean newValue) {
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
                            property2.set(newValue);
                        } else {
                            property1.set(newValue);
                        }
                    } catch (RuntimeException e) {
                        try {
                            if (property1 == sourceProperty) {
                                property1.set(oldValue);
                            } else {
                                property2.set(oldValue);
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

    private static class BidirectionalDoubleBinding extends BidirectionalBinding<Number> {
        private final WeakReference<DoubleProperty> propertyRef1;
        private final WeakReference<DoubleProperty> propertyRef2;
        private boolean updating = false;

        private BidirectionalDoubleBinding(DoubleProperty property1, DoubleProperty property2) {
            super(property1, property2);
            propertyRef1 = new WeakReference<DoubleProperty>(property1);
            propertyRef2 = new WeakReference<DoubleProperty>(property2);
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
        public void changed(ObservableValue<? extends Number> sourceProperty, Number oldValue, Number newValue) {
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
                            property2.set(newValue.doubleValue());
                        } else {
                            property1.set(newValue.doubleValue());
                        }
                    } catch (RuntimeException e) {
                        try {
                            if (property1 == sourceProperty) {
                                property1.set(oldValue.doubleValue());
                            } else {
                                property2.set(oldValue.doubleValue());
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

    private static class BidirectionalFloatBinding extends BidirectionalBinding<Number> {
        private final WeakReference<FloatProperty> propertyRef1;
        private final WeakReference<FloatProperty> propertyRef2;
        private boolean updating = false;

        private BidirectionalFloatBinding(FloatProperty property1, FloatProperty property2) {
            super(property1, property2);
            propertyRef1 = new WeakReference<FloatProperty>(property1);
            propertyRef2 = new WeakReference<FloatProperty>(property2);
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
        public void changed(ObservableValue<? extends Number> sourceProperty, Number oldValue, Number newValue) {
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
                            property2.set(newValue.floatValue());
                        } else {
                            property1.set(newValue.floatValue());
                        }
                    } catch (RuntimeException e) {
                        try {
                            if (property1 == sourceProperty) {
                                property1.set(oldValue.floatValue());
                            } else {
                                property2.set(oldValue.floatValue());
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

    private static class BidirectionalIntegerBinding extends BidirectionalBinding<Number>{
        private final WeakReference<IntegerProperty> propertyRef1;
        private final WeakReference<IntegerProperty> propertyRef2;
        private boolean updating = false;

        private BidirectionalIntegerBinding(IntegerProperty property1, IntegerProperty property2) {
            super(property1, property2);
            propertyRef1 = new WeakReference<IntegerProperty>(property1);
            propertyRef2 = new WeakReference<IntegerProperty>(property2);
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
        public void changed(ObservableValue<? extends Number> sourceProperty, Number oldValue, Number newValue) {
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
                            property2.set(newValue.intValue());
                        } else {
                            property1.set(newValue.intValue());
                        }
                    } catch (RuntimeException e) {
                        try {
                            if (property1 == sourceProperty) {
                                property1.set(oldValue.intValue());
                            } else {
                                property2.set(oldValue.intValue());
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

    private static class BidirectionalLongBinding extends BidirectionalBinding<Number> {
        private final WeakReference<LongProperty> propertyRef1;
        private final WeakReference<LongProperty> propertyRef2;
        private boolean updating = false;

        private BidirectionalLongBinding(LongProperty property1, LongProperty property2) {
            super(property1, property2);
            propertyRef1 = new WeakReference<LongProperty>(property1);
            propertyRef2 = new WeakReference<LongProperty>(property2);
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
        public void changed(ObservableValue<? extends Number> sourceProperty, Number oldValue, Number newValue) {
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
                            property2.set(newValue.longValue());
                        } else {
                            property1.set(newValue.longValue());
                        }
                    } catch (RuntimeException e) {
                        try {
                            if (property1 == sourceProperty) {
                                property1.set(oldValue.longValue());
                            } else {
                                property2.set(oldValue.longValue());
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

    private static class TypedGenericBidirectionalBinding<T> extends BidirectionalBinding<T> {
        private final WeakReference<Property<T>> propertyRef1;
        private final WeakReference<Property<T>> propertyRef2;
        private boolean updating = false;

        private TypedGenericBidirectionalBinding(Property<T> property1, Property<T> property2) {
            super(property1, property2);
            propertyRef1 = new WeakReference<Property<T>>(property1);
            propertyRef2 = new WeakReference<Property<T>>(property2);
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
        public void changed(ObservableValue<? extends T> sourceProperty, T oldValue, T newValue) {
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
                            property2.setValue(newValue);
                        } else {
                            property1.setValue(newValue);
                        }
                    } catch (RuntimeException e) {
                        try {
                            if (property1 == sourceProperty) {
                                property1.setValue(oldValue);
                            } else {
                                property2.setValue(oldValue);
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

    private static class TypedNumberBidirectionalBinding<T extends Number> extends BidirectionalBinding<Number> {
        private final WeakReference<Property<T>> propertyRef1;
        private final WeakReference<Property<Number>> propertyRef2;
        private boolean updating = false;

        private TypedNumberBidirectionalBinding(Property<T> property1, Property<Number> property2) {
            super(property1, property2);
            propertyRef1 = new WeakReference<Property<T>>(property1);
            propertyRef2 = new WeakReference<Property<Number>>(property2);
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
        public void changed(ObservableValue<? extends Number> sourceProperty, Number oldValue, Number newValue) {
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
                            property2.setValue(newValue);
                        } else {
                            property1.setValue((T)newValue);
                        }
                    } catch (RuntimeException e) {
                        try {
                            if (property1 == sourceProperty) {
                                property1.setValue((T)oldValue);
                            } else {
                                property2.setValue(oldValue);
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

    private static class UntypedGenericBidirectionalBinding extends BidirectionalBinding<Object> {

        private final Object property1;
        private final Object property2;

        public UntypedGenericBidirectionalBinding(Object property1, Object property2) {
            super(property1, property2);
            this.property1 = property1;
            this.property2 = property2;
        }

        @Override
        protected Object getProperty1() {
            return property1;
        }

        @Override
        protected Object getProperty2() {
            return property2;
        }

        @Override
        public void changed(ObservableValue<? extends Object> sourceProperty, Object oldValue, Object newValue) {
            throw new RuntimeException("Should not reach here");
        }
    }

    public abstract static class StringConversionBidirectionalBinding<T> extends BidirectionalBinding<Object> {

        private final WeakReference<Property<String>> stringPropertyRef;
        private final WeakReference<Property<T>> otherPropertyRef;
        private boolean updating;

        public StringConversionBidirectionalBinding(Property<String> stringProperty, Property<T> otherProperty) {
            super(stringProperty, otherProperty);
            stringPropertyRef = new WeakReference<Property<String>>(stringProperty);
            otherPropertyRef = new WeakReference<Property<T>>(otherProperty);
        }

        protected abstract String toString(T value);

        protected abstract T fromString(String value) throws ParseException;

        @Override
        protected Object getProperty1() {
            return stringPropertyRef.get();
        }

        @Override
        protected Object getProperty2() {
            return otherPropertyRef.get();
        }

        @Override
        public void changed(ObservableValue<? extends Object> observable, Object oldValue, Object newValue) {
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
                            } catch (Exception e) {
                                Logging.getLogger().warning("Exception while parsing String in bidirectional binding", e);
                                property2.setValue(null);
                            }
                        } else {
                            try {
                                property1.setValue(toString(property2.getValue()));
                            } catch (Exception e) {
                                Logging.getLogger().warning("Exception while converting Object to String in bidirectional binding", e);
                                property1.setValue("");
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
