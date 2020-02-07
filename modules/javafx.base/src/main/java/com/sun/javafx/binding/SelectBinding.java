/*
 * Copyright (c) 2010, 2019, Oracle and/or its affiliates. All rights reserved.
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
import javafx.beans.WeakInvalidationListener;
import javafx.beans.binding.Binding;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.FloatBinding;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.binding.LongBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableNumberValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import com.sun.javafx.property.JavaBeanAccessHelper;
import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.logging.PlatformLogger.Level;
import com.sun.javafx.property.PropertyReference;
import java.util.Arrays;

/**
 * A binding used to get a member, such as <code>a.b.c</code>. The value of the
 * binding will be "c", or null if c could not be reached (due to "b" not having
 * a "c" property, or "b" being null). "a" must be passed to the constructor of
 * the SelectBinding and may be any dependency. All subsequent links are simply
 * PropertyReferences.
 * <p>
 * With a SelectBinding, "a" must always exist. Usually "a" will refer to
 * "this", or some concrete object. "b"* will be some intermediate step in the
 * select binding.
 */
public class SelectBinding {

    private SelectBinding() {}

    public static class AsObject<T> extends ObjectBinding<T> {

        private final SelectBindingHelper helper;

        public AsObject(ObservableValue<?> root, String... steps) {
            helper = new SelectBindingHelper(this, root, steps);
        }

        public AsObject(Object root, String... steps) {
            helper = new SelectBindingHelper(this, root, steps);
        }

        @Override
        public void dispose() {
            helper.unregisterListener();
        }

        @Override
        protected void onInvalidating() {
            helper.unregisterListener();
        }

        @SuppressWarnings("unchecked")
        @Override
        protected T computeValue() {
            final ObservableValue<?> observable = helper.getObservableValue();
            if (observable == null) {
                return null;
            }
            try {
                return (T)observable.getValue();
            } catch (ClassCastException ex) {
                Logging.getLogger().warning("Value of select-binding has wrong type, returning null.", ex);
            }
            return null;
        }


        @Override
        public ObservableList<ObservableValue<?>> getDependencies() {
            return helper.getDependencies();
        }

    }

    public static class AsBoolean extends BooleanBinding {

        private static final boolean DEFAULT_VALUE = false;

        private final SelectBindingHelper helper;

        public AsBoolean(ObservableValue<?> root, String... steps) {
            helper = new SelectBindingHelper(this, root, steps);
        }

        public AsBoolean(Object root, String... steps) {
            helper = new SelectBindingHelper(this, root, steps);
        }

        @Override
        public void dispose() {
            helper.unregisterListener();
        }

        @Override
        protected void onInvalidating() {
            helper.unregisterListener();
        }

        @Override
        protected boolean computeValue() {
            final ObservableValue<?> observable = helper.getObservableValue();
            if (observable == null) {
                return DEFAULT_VALUE;
            }
            if (observable instanceof ObservableBooleanValue) {
                return ((ObservableBooleanValue)observable).get();
            }
            try {
                return (Boolean)observable.getValue();
            } catch (NullPointerException ex) {
                Logging.getLogger().fine("Value of select binding is null, returning default value", ex);
            } catch (ClassCastException ex) {
                Logging.getLogger().warning("Value of select-binding has wrong type, returning default value.", ex);
            }
            return DEFAULT_VALUE;
        }

        @Override
        public ObservableList<ObservableValue<?>> getDependencies() {
            return helper.getDependencies();
        }

    }

    public static class AsDouble extends DoubleBinding {

        private static final double DEFAULT_VALUE = 0.0;

        private final SelectBindingHelper helper;

        public AsDouble(ObservableValue<?> root, String... steps) {
            helper = new SelectBindingHelper(this, root, steps);
        }

        public AsDouble(Object root, String... steps) {
            helper = new SelectBindingHelper(this, root, steps);
        }

        @Override
        public void dispose() {
            helper.unregisterListener();
        }

        @Override
        protected void onInvalidating() {
            helper.unregisterListener();
        }

        @Override
        protected double computeValue() {
            final ObservableValue<?> observable = helper.getObservableValue();
            if (observable == null) {
                return DEFAULT_VALUE;
            }
            if (observable instanceof ObservableNumberValue) {
                return ((ObservableNumberValue)observable).doubleValue();
            }
            try {
                return ((Number)observable.getValue()).doubleValue();
            } catch (NullPointerException ex) {
                Logging.getLogger().fine("Value of select binding is null, returning default value", ex);
            } catch (ClassCastException ex) {
                Logging.getLogger().warning("Exception while evaluating select-binding", ex);
            }
            return DEFAULT_VALUE;
        }

        @Override
        public ObservableList<ObservableValue<?>> getDependencies() {
            return helper.getDependencies();
        }

    }

    public static class AsFloat extends FloatBinding {

        private static final float DEFAULT_VALUE = 0.0f;

        private final SelectBindingHelper helper;

        public AsFloat(ObservableValue<?> root, String... steps) {
            helper = new SelectBindingHelper(this, root, steps);
        }

        public AsFloat(Object root, String... steps) {
            helper = new SelectBindingHelper(this, root, steps);
        }

        @Override
        public void dispose() {
            helper.unregisterListener();
        }

        @Override
        protected void onInvalidating() {
            helper.unregisterListener();
        }

        @Override
        protected float computeValue() {
            final ObservableValue<?> observable = helper.getObservableValue();
            if (observable == null) {
                return DEFAULT_VALUE;
            }
            if (observable instanceof ObservableNumberValue) {
                return ((ObservableNumberValue)observable).floatValue();
            }
            try {
                return ((Number)observable.getValue()).floatValue();
            } catch (NullPointerException ex) {
                Logging.getLogger().fine("Value of select binding is null, returning default value", ex);
            } catch (ClassCastException ex) {
                Logging.getLogger().warning("Exception while evaluating select-binding", ex);
            }
            return DEFAULT_VALUE;
        }

        @Override
        public ObservableList<ObservableValue<?>> getDependencies() {
            return helper.getDependencies();
        }

    }

    public static class AsInteger extends IntegerBinding {

        private static final int DEFAULT_VALUE = 0;

        private final SelectBindingHelper helper;

        public AsInteger(ObservableValue<?> root, String... steps) {
            helper = new SelectBindingHelper(this, root, steps);
        }

        public AsInteger(Object root, String... steps) {
            helper = new SelectBindingHelper(this, root, steps);
        }

        @Override
        public void dispose() {
            helper.unregisterListener();
        }

        @Override
        protected void onInvalidating() {
            helper.unregisterListener();
        }

        @Override
        protected int computeValue() {
            final ObservableValue<?> observable = helper.getObservableValue();
            if (observable == null) {
                return DEFAULT_VALUE;
            }
            if (observable instanceof ObservableNumberValue) {
                return ((ObservableNumberValue)observable).intValue();
            }
            try {
                return ((Number)observable.getValue()).intValue();
            } catch (NullPointerException ex) {
                Logging.getLogger().fine("Value of select binding is null, returning default value", ex);
            } catch (ClassCastException ex) {
                Logging.getLogger().warning("Exception while evaluating select-binding", ex);
            }
            return DEFAULT_VALUE;
        }

        @Override
        public ObservableList<ObservableValue<?>> getDependencies() {
            return helper.getDependencies();
        }

    }

    public static class AsLong extends LongBinding {

        private static final long DEFAULT_VALUE = 0L;

        private final SelectBindingHelper helper;

        public AsLong(ObservableValue<?> root, String... steps) {
            helper = new SelectBindingHelper(this, root, steps);
        }

        public AsLong(Object root, String... steps) {
            helper = new SelectBindingHelper(this, root, steps);
        }

        @Override
        public void dispose() {
            helper.unregisterListener();
        }

        @Override
        protected void onInvalidating() {
            helper.unregisterListener();
        }

        @Override
        protected long computeValue() {
            final ObservableValue<?> observable = helper.getObservableValue();
            if (observable == null) {
                return DEFAULT_VALUE;
            }
            if (observable instanceof ObservableNumberValue) {
                return ((ObservableNumberValue)observable).longValue();
            }
            try {
                return ((Number)observable.getValue()).longValue();
            } catch (NullPointerException ex) {
                Logging.getLogger().fine("Value of select binding is null, returning default value", ex);
            } catch (ClassCastException ex) {
                Logging.getLogger().warning("Exception while evaluating select-binding", ex);
            }
            return DEFAULT_VALUE;
        }

        @Override
        public ObservableList<ObservableValue<?>> getDependencies() {
            return helper.getDependencies();
        }

    }

    public static class AsString extends StringBinding {

        private static final String DEFAULT_VALUE = null;

        private final SelectBindingHelper helper;

        public AsString(ObservableValue<?> root, String... steps) {
            helper = new SelectBindingHelper(this, root, steps);
        }

        public AsString(Object root, String... steps) {
            helper = new SelectBindingHelper(this, root, steps);
        }

        @Override
        public void dispose() {
            helper.unregisterListener();
        }

        @Override
        protected void onInvalidating() {
            helper.unregisterListener();
        }

        @Override
        protected String computeValue() {
            final ObservableValue<?> observable = helper.getObservableValue();
            if (observable == null) {
                return DEFAULT_VALUE;
            }
            try {
                return observable.getValue().toString();
            } catch (RuntimeException ex) {
                Logging.getLogger().warning("Exception while evaluating select-binding", ex);
                // return default
                return DEFAULT_VALUE;
            }
        }

        @Override
        public ObservableList<ObservableValue<?>> getDependencies() {
            return helper.getDependencies();
        }

    }

    private static class SelectBindingHelper implements InvalidationListener {

        private final Binding<?> binding;
        private final String[] propertyNames;
        private final ObservableValue<?>[] properties;
        private final PropertyReference<?>[] propRefs;
        private final WeakInvalidationListener observer;

        private ObservableList<ObservableValue<?>> dependencies;

        private SelectBindingHelper(Binding<?> binding, ObservableValue<?> firstProperty, String... steps) {
            if (firstProperty == null) {
                throw new NullPointerException("Must specify the root");
            }
            if (steps == null) {
                steps = new String[0];
            }

            this.binding = binding;

            final int n = steps.length;
            for (int i = 0; i < n; i++) {
                if (steps[i] == null) {
                    throw new NullPointerException("all steps must be specified");
                }
            }

            observer = new WeakInvalidationListener(this);
            propertyNames = new String[n];
            System.arraycopy(steps, 0, propertyNames, 0, n);
            propRefs = new PropertyReference<?>[n];
            properties = new ObservableValue<?>[n + 1];
            properties[0] = firstProperty;
            properties[0].addListener(observer);
        }

        private static ObservableValue<?> checkAndCreateFirstStep(Object root, String[] steps) {
            if (root == null || steps == null || steps[0] == null) {
                throw new NullPointerException("Must specify the root and the first property");
            }
            try {
                return JavaBeanAccessHelper.createReadOnlyJavaBeanProperty(root, steps[0]);
            } catch (NoSuchMethodException ex) {
                throw new IllegalArgumentException("The first property '" + steps[0] + "' doesn't exist");
            }
        }

        private SelectBindingHelper(Binding<?> binding, Object root, String... steps) {
            this(binding, checkAndCreateFirstStep(root, steps), Arrays.copyOfRange(steps, 1, steps.length));
        }

        @Override
        public void invalidated(Observable observable) {
            binding.invalidate();
        }

        public ObservableValue<?> getObservableValue() {
            // Step through each of the steps, and at each step add a listener as
            // appropriate, accumulating the result.
            final int n = properties.length;
            for (int i = 0; i < n - 1; i++) {
                final Object obj = properties[i].getValue();
                try {
                    if ((propRefs[i] == null)
                            || (!obj.getClass().equals(
                            propRefs[i].getContainingClass()))) {
                        propRefs[i] = new PropertyReference<Object>(obj.getClass(),
                                propertyNames[i]);
                    }
                    if (propRefs[i].hasProperty()) {
                        properties[i + 1] = propRefs[i].getProperty(obj);
                    } else {
                        properties[i + 1] = JavaBeanAccessHelper.createReadOnlyJavaBeanProperty(obj, propRefs[i].getName());
                    }
                } catch (NoSuchMethodException ex) {
                    Logging.getLogger().warning("Exception while evaluating select-binding " + stepsToString(), ex);
                    // return default
                    updateDependencies();
                    return null;
                } catch (RuntimeException ex) {
                    final PlatformLogger logger = Logging.getLogger();
                    if (logger.isLoggable(Level.WARNING)) {
                        String msg = "Exception while evaluating select-binding " + stepsToString();
                        if (ex instanceof IllegalStateException) {
                            logger.warning(msg);
                            logger.warning("Property '" + propertyNames[i] + "' does not exist in " + obj.getClass(), ex);
                        } else if (ex instanceof NullPointerException) {
                            logger.fine(msg);
                            logger.fine("Property '" + propertyNames[i] + "' in " + properties[i] + " is null", ex);
                        } else {
                            logger.warning(msg, ex);
                        }
                    }
                    // return default
                    updateDependencies();
                    return null;
                }
                properties[i + 1].addListener(observer);
            }
            updateDependencies();
            final ObservableValue<?> result = properties[n-1];
            if (result == null) {
                Logging.getLogger().fine("Property '" + propertyNames[n-1] + "' in " + properties[n-1] + " is null", new NullPointerException());
            }
            return result;
        }

        private String stepsToString() {
            return Arrays.toString(propertyNames);
        }

        private void unregisterListener() {
            final int n = properties.length;
            for (int i = 1; i < n; i++) {
                if (properties[i] == null) {
                    break;
                }
                properties[i].removeListener(observer);
                properties[i] = null;
            }
            updateDependencies();
        }

        private void updateDependencies() {
            if (dependencies != null) {
                dependencies.clear();
                final int n = properties.length;
                for (int i = 0; i < n; i++) {
                    if (properties[i] == null) {
                        break;
                    }
                    dependencies.add(properties[i]);
                }
            }
        }

        public ObservableList<ObservableValue<?>> getDependencies() {
            if (dependencies == null) {
                dependencies = FXCollections.observableArrayList();
                updateDependencies();
            }

            return FXCollections.unmodifiableObservableList(dependencies);
        }

    }

}
