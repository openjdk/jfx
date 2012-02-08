/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.test;

import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ObservableValue;

import org.junit.Assert;
import org.junit.Test;

public abstract class PropertiesTestBase {

    private final Configuration configuration;

    public PropertiesTestBase(final Configuration configuration) {
        this.configuration = configuration;
    }

    @Test
    public void testGetBean() {
        configuration.getBeanTest();
    }

    @Test
    public void testGetName() {
        configuration.getNameTest();
    }

    @Test
    public void testBasicAccess() {
        configuration.basicAccessTest();
    }

    @Test
    public void testBuilder() {
        configuration.builderTest();
    }

    @Test
    public void testBinding() {
        configuration.bindingTest();
    }

    /**
     * Single bean, single property configuration.
     */
    public static Object[] config(final Object bean,
                                  final String propertyName,
                                  final Object propertyValue1,
                                  final Object propertyValue2) {
        return config(new Configuration(bean,
                                        propertyName,
                                        propertyValue1,
                                        propertyValue2));
    }

    /**
     * Single bean, single property configuration with custom value comparator.
     */
    public static Object[] config(final Object bean,
                                  final String propertyName,
                                  final Object propertyValue1,
                                  final Object propertyValue2,
                                  final ValueComparator comparator) {
        return config(new Configuration(bean,
                                        propertyName,
                                        propertyValue1,
                                        propertyValue2,
                                        comparator));
    }

    /**
     * Single bean, propertyA (source) and propertyB (dependent) property
     * configuration.
     */
    public static Object[] config(final Object beanA,
                                  final String propertyAName,
                                  final Object propertyAValue1,
                                  final Object propertyAValue2,
                                  final String propertyBName,
                                  final Object propertyBValue1,
                                  final Object propertyBValue2) {
        return config(new Configuration(beanA,
                                        propertyAName,
                                        propertyAValue1,
                                        propertyAValue2,
                                        propertyBName,
                                        propertyBValue1,
                                        propertyBValue2));
    }

    /**
     * BeanA with propertyA (source) and BeanB with propertyB (dependent)
     * configuration.
     */
    public static Object[] config(final Object beanA,
                                  final String propertyAName,
                                  final Object propertyAValue1,
                                  final Object propertyAValue2,
                                  final Object beanB,
                                  final String propertyBName,
                                  final Object propertyBValue1,
                                  final Object propertyBValue2) {
        return config(new Configuration(beanA,
                                        propertyAName,
                                        propertyAValue1,
                                        propertyAValue2,
                                        beanB,
                                        propertyBName,
                                        propertyBValue1,
                                        propertyBValue2));
    }

    /**
     * BeanA with propertyA (source) and BeanB with propertyB (dependent)
     * with custom value comparator configuration.
     */
    public static Object[] config(final Object beanA,
                                  final String propertyAName,
                                  final Object propertyAValue1,
                                  final Object propertyAValue2,
                                  final Object beanB,
                                  final String propertyBName,
                                  final Object propertyBValue1,
                                  final Object propertyBValue2,
                                  final ValueComparator propertyBComparator) {
        return config(new Configuration(beanA,
                                        propertyAName,
                                        propertyAValue1,
                                        propertyAValue2,
                                        beanB,
                                        propertyBName,
                                        propertyBValue1,
                                        propertyBValue2,
                                        propertyBComparator));
    }

    public static Object[] config(final Configuration configuration) {
        return new Object[] { configuration };
    }

    public static class Configuration {
        private final Object beanA;

        private final PropertyReference propertyAReference;

        private final Object propertyAValue1;

        private final Object propertyAValue2;

        private final Object beanB;

        private final PropertyReference propertyBReference;

        private final Object propertyBValue1;

        private final Object propertyBValue2;

        private final ValueComparator propertyBComparator;

        private boolean allowMultipleNotifications;

        public Configuration(final Object bean,
                             final String propertyName,
                             final Object propertyValue1,
                             final Object propertyValue2) {
            this(bean, propertyName, propertyValue1, propertyValue2,
                 bean, propertyName, propertyValue1, propertyValue2,
                 ValueComparator.DEFAULT);
        }

        public Configuration(final Object bean,
                             final String propertyName,
                             final Object propertyValue1,
                             final Object propertyValue2,
                             final ValueComparator valueComparator) {
            this(bean, propertyName, propertyValue1, propertyValue2,
                 bean, propertyName, propertyValue1, propertyValue2,
                 valueComparator);
        }

        public Configuration(final Object bean,
                             final String propertyAName,
                             final Object propertyAValue1,
                             final Object propertyAValue2,
                             final String propertyBName,
                             final Object propertyBValue1,
                             final Object propertyBValue2) {
            this(bean, propertyAName, propertyAValue1, propertyAValue2,
                 bean, propertyBName, propertyBValue1, propertyBValue2,
                 ValueComparator.DEFAULT);
        }

        public Configuration(final Object beanA,
                             final String propertyAName,
                             final Object propertyAValue1,
                             final Object propertyAValue2,
                             final Object beanB,
                             final String propertyBName,
                             final Object propertyBValue1,
                             final Object propertyBValue2) {
            this(beanA, propertyAName, propertyAValue1, propertyAValue2,
                 beanB, propertyBName, propertyBValue1, propertyBValue2,
                 ValueComparator.DEFAULT);
        }
        
        public Configuration(final Object beanA,
                             final String propertyAName,
                             final Object propertyAValue1,
                             final Object propertyAValue2,
                             final Object beanB,
                             final String propertyBName,
                             final Object propertyBValue1,
                             final Object propertyBValue2,
                             final ValueComparator propertyBComparator) {
            this.beanA = beanA;
            this.propertyAReference = PropertyReference.createForBean(
                                              beanA.getClass(),
                                              propertyAName);
            this.propertyAValue1 = propertyAValue1;
            this.propertyAValue2 = propertyAValue2;
            this.beanB = beanB;
            this.propertyBReference = PropertyReference.createForBean(
                                              beanB.getClass(),
                                              propertyBName);
            this.propertyBValue1 = propertyBValue1;
            this.propertyBValue2 = propertyBValue2;
            this.propertyBComparator = propertyBComparator;
        }

        public void setAllowMultipleNotifications(
                final boolean allowMultipleNotifications) {
            this.allowMultipleNotifications = allowMultipleNotifications;
        }

        public void getBeanTest() {
            final ReadOnlyProperty<?> propertyA =
                    (ReadOnlyProperty<?>) BindingHelper.getPropertyModel(
                                                  beanA, propertyAReference);
            final ReadOnlyProperty<?> propertyB =
                    (ReadOnlyProperty<?>) BindingHelper.getPropertyModel(
                                                  beanB, propertyBReference);

            Assert.assertSame(beanA, propertyA.getBean());
            Assert.assertSame(beanB, propertyB.getBean());
        }
        
        public void getNameTest() {
            final ReadOnlyProperty<?> propertyA =
                    (ReadOnlyProperty<?>) BindingHelper.getPropertyModel(
                                                  beanA, propertyAReference);
            final ReadOnlyProperty<?> propertyB =
                    (ReadOnlyProperty<?>) BindingHelper.getPropertyModel(
                                                  beanB, propertyBReference);

            Assert.assertEquals(propertyAReference.getPropertyName(),
                                propertyA.getName());
            Assert.assertEquals(propertyBReference.getPropertyName(),
                                propertyB.getName());
        }

        public void basicAccessTest() {
            // set to first value and verify dependet value
            propertyAReference.setValue(beanA, propertyAValue1);
            propertyBComparator.assertEquals(
                    propertyBValue1,
                    propertyBReference.getValue(beanB));

            final ValueInvalidationListener valueInvalidationListener =
                    new ValueInvalidationListener(allowMultipleNotifications);
            final ObservableValue observableValueB =
                    (ObservableValue) BindingHelper.getPropertyModel(
                                                  beanB, propertyBReference);

            // register listener
            observableValueB.addListener(valueInvalidationListener);

            // set to second value
            propertyAReference.setValue(beanA, propertyAValue2);

            // verify that the listener has been called
            valueInvalidationListener.assertCalled();
            valueInvalidationListener.reset();

            // test whether the second dependent value is set
            propertyBComparator.assertEquals(
                    propertyBValue2,
                    propertyBReference.getValue(beanB));

            // set to the second value again
            propertyAReference.setValue(beanA, propertyAValue2);

            // verify that the listener has not been called
            valueInvalidationListener.assertNotCalled();

            // unregister listener
            observableValueB.removeListener(valueInvalidationListener);

            // set to the first value again and test
            propertyAReference.setValue(beanA, propertyAValue1);
            propertyBComparator.assertEquals(
                    propertyBValue1,
                    propertyBReference.getValue(beanB));

            // verify that the listener has not been called
            valueInvalidationListener.assertNotCalled();
        }

        public void builderTest() {
            final BuilderProxy builderProxy =
                    BuilderProxy.createForBean(beanA.getClass());

            if (builderProxy == null) {
                // no builder, no test
                return;
            }

            final Object builder = builderProxy.createBuilder();
            final PropertyReference builderPropRefA =
                    builderProxy.createPropertyReference(
                            propertyAReference.getPropertyName(),
                            propertyAReference.getValueType());

            // set to first value and verify dependet value
            builderPropRefA.setValue(builder, propertyAValue1);
            builderProxy.applyTo(builder, beanA);
            propertyBComparator.assertEquals(
                    propertyBValue1,
                    propertyBReference.getValue(beanB));

            final ValueInvalidationListener valueInvalidationListener =
                    new ValueInvalidationListener(allowMultipleNotifications);
            final ObservableValue observableValueB =
                    (ObservableValue) BindingHelper.getPropertyModel(
                                                  beanB, propertyBReference);

            // register listener
            observableValueB.addListener(valueInvalidationListener);

            // set to second value
            builderPropRefA.setValue(builder, propertyAValue2);
            builderProxy.applyTo(builder, beanA);

            // verify that the listener has been called
            valueInvalidationListener.assertCalled();
            valueInvalidationListener.reset();

            // test whether the second dependent value is set
            propertyBComparator.assertEquals(
                    propertyBValue2,
                    propertyBReference.getValue(beanB));

            // set to the second value again
            builderProxy.applyTo(builder, beanA);

            // verify that the listener has not been called
            valueInvalidationListener.assertNotCalled();

            // unregister listener
            observableValueB.removeListener(valueInvalidationListener);

            // set to the first value again and test
            builderPropRefA.setValue(builder, propertyAValue1);
            builderProxy.applyTo(builder, beanA);
            propertyBComparator.assertEquals(
                    propertyBValue1,
                    propertyBReference.getValue(beanB));

            // verify that the listener has not been called
            valueInvalidationListener.assertNotCalled();
        }

        public void bindingTest() {
            // set to the first value
            propertyAReference.setValue(beanA, propertyAValue1);

            // bind to a variable set to second value
            final Object firstVariable =
                    BindingHelper.createVariable(propertyAValue2);
            BindingHelper.bind(beanA, propertyAReference, firstVariable);

            // test what we get
            propertyBComparator.assertEquals(
                    propertyBValue2,
                    propertyBReference.getValue(beanB));

            final ValueInvalidationListener valueInvalidationListener =
                    new ValueInvalidationListener(allowMultipleNotifications);
            final ObservableValue observableValue =
                    (ObservableValue) BindingHelper.getPropertyModel(
                                                  beanB, propertyBReference);

            // register listener
            observableValue.addListener(valueInvalidationListener);

            // change the value of the bound variable
            BindingHelper.setWritableValue(propertyAReference.getValueType(),
                                           firstVariable, propertyAValue1);

            // verify that the listener has been called
            valueInvalidationListener.assertCalled();
            valueInvalidationListener.reset();

            // check the value
            propertyBComparator.assertEquals(
                    propertyBValue1,
                    propertyBReference.getValue(beanB));

            // change binding
            final Object secondVariable =
                    BindingHelper.createVariable(propertyAValue2);
            BindingHelper.bind(beanA, propertyAReference, secondVariable);

            // verify that the listener has been called
            valueInvalidationListener.assertCalled();
            valueInvalidationListener.reset();

            // check the value
            propertyBComparator.assertEquals(
                    propertyBValue2,
                    propertyBReference.getValue(beanB));

            // unbind
            BindingHelper.unbind(beanA, propertyAReference);

            // verify that the listener has not been called
            valueInvalidationListener.assertNotCalled();

            // change the value of the last bound variable
            BindingHelper.setWritableValue(propertyAReference.getValueType(),
                                           secondVariable, propertyAValue1);

            // verify that the listener has not been called
            valueInvalidationListener.assertNotCalled();

            // check that the current property value equals to the last
            // value set by binding
            propertyBComparator.assertEquals(
                    propertyBValue2,
                    propertyBReference.getValue(beanB));

            // set to the first value again
            propertyAReference.setValue(beanA, propertyAValue1);

            // verify that the listener has been called
            valueInvalidationListener.assertCalled();
            valueInvalidationListener.reset();

            // check the value
            propertyBComparator.assertEquals(
                    propertyBValue1,
                    propertyBReference.getValue(beanB));

            // unregister listener
            observableValue.removeListener(valueInvalidationListener);
        }
    }

    private static final class ValueInvalidationListener
            implements InvalidationListener {
        private final boolean allowMultipleNotifications;

        private int counter;

        public ValueInvalidationListener(
                final boolean allowMultipleNotifications) {
            this.allowMultipleNotifications = allowMultipleNotifications;
        }

        public void reset() {
            counter = 0;
        }

        public void assertCalled() {
            if (counter == 0) {
                Assert.fail("Listener has not been called!");
                return;
            }

            if (!allowMultipleNotifications && (counter > 1)) {
                Assert.fail("Listener called multiple times!");
            }
        }

        public void assertNotCalled() {
            if (counter != 0) {
                Assert.fail("Listener has been called!");
                return;
            }
        }

        @Override
        public void invalidated(final Observable valueModel) {
            ++counter;
        }
    }
}
