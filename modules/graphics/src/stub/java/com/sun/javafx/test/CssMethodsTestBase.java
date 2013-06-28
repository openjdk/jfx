/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Iterator;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import javafx.scene.Node;

import org.junit.Test;

import javafx.css.StyleConverter;
import javafx.css.CssMetaData;
import java.util.List;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;

public abstract class CssMethodsTestBase {
    private final Configuration configuration;

    public CssMethodsTestBase(final Configuration configuration) {
        this.configuration = configuration;
    }

    
    @Test // This _must_ be the first test!
    public void testCssDefaultSameAsPropertyDefault() {
        configuration.cssDefaultsTest();
    }
    
    @Test
    public void testCSSPropertyAndCSSPropertyReferenceEachOther() {
        configuration.cssPropertyReferenceIntegrityTest();
    }
    
    @Test
    public void testCssSettable() throws Exception {
        configuration.cssSettableTest();
    }

    @Test
    public void testCssSet() {
        configuration.cssSetTest();
    }

    public static Object[] config(
            final Node node,
            final String propertyName,
            final Object initialValue,
            final String cssPropertyKey,
            final Object cssPropertyValue) {
        return config(new Configuration(node,
                                        propertyName,
                                        initialValue,
                                        cssPropertyKey,
                                        cssPropertyValue));
    }

    public static Object[] config(
            final Node node,
            final String propertyName,
            final Object initialValue,
            final String cssPropertyKey,
            final Object cssPropertyValue,
            final Object expectedFinalValue) {
        return config(new Configuration(node,
                                        propertyName,
                                        initialValue,
                                        cssPropertyKey,
                                        cssPropertyValue,
                                        expectedFinalValue));
    }

    public static Object[] config(
            final Node node,
            final String propertyName,
            final Object initialValue,
            final String cssPropertyKey,
            final Object cssPropertyValue,
            final ValueComparator comparator) {
        return config(new Configuration(node,
                                        propertyName,
                                        initialValue,
                                        cssPropertyKey,
                                        cssPropertyValue,
                                        comparator));
    }

    public static Object[] config(final Configuration configuration) {
        return new Object[] { configuration };
    }
    
    private static CssMetaData<? extends Styleable, ?> getCssMetaData(Node node, String cssProperty) {
        
        List<CssMetaData<? extends Styleable, ?>> styleables = node.getCssMetaData();
        for(CssMetaData<? extends Styleable, ?> styleable : styleables) {
            if (styleable.getProperty().equals(cssProperty)) {
                return styleable;
            }
        }
        fail(node.toString() + ": CSSProperty" + cssProperty + " not found");
        return null;
    }

    public static class Configuration {
        private static final StyleConverter<String, Object> TEST_TYPE =
                new StyleConverter<String, Object>();

        private static final CssMetaData<Node, Object> UNDEFINED_KEY =
                new CssMetaData<Node,Object>("U-N-D-E-F-I-N-E-D", TEST_TYPE, "") {

            @Override
            public boolean isSettable(Node n) {
                return false;
            }

            @Override
            public StyleableProperty<Object> getStyleableProperty(Node n) {
                return null;
            }
        };

        private final Node node;

        private final PropertyReference nodePropertyReference;

        private final Object initialValue;
        
        private final Object defaultValue;

        private final CssMetaData cssPropertyKey;

        private final Object cssPropertyValue;

        private final Object expectedFinalValue;

        private final ValueComparator comparator;

        public Configuration(final Node node,
                             final String propertyName,
                             final Object initialValue,
                             final String cssPropertyKey,
                             final Object cssPropertyValue) {
            this(node,
                 propertyName,
                 initialValue,
                 cssPropertyKey,
                 cssPropertyValue,
                 ValueComparator.DEFAULT);
        }

        public Configuration(final Node node,
                             final String propertyName,
                             final Object initialValue,
                             final String cssPropertyKey,
                             final Object cssPropertyValue,
                             final Object finalExpectedValue) {
            this(node,
                 propertyName,
                 initialValue,
                 getCssMetaData(node, cssPropertyKey),
                 cssPropertyValue,
                 finalExpectedValue,
                 ValueComparator.DEFAULT);
        }

        public Configuration(final Node node,
                             final String propertyName,
                             final Object initialValue,
                             final String cssPropertyKey,
                             final Object cssPropertyValue,
                             final ValueComparator comparator) {
            this(node,
                 propertyName,
                 initialValue,
                 getCssMetaData(node, cssPropertyKey),
                 cssPropertyValue,
                 cssPropertyValue,
                 comparator);
        }

        public Configuration(final Node node,
                             final String propertyName,
                             final Object initialValue,
                             final CssMetaData cssPropertyKey,
                             final Object cssPropertyValue,
                             final Object expectedFinalValue,
                             final ValueComparator comparator) {
            this.node = node;
            this.nodePropertyReference = 
                    PropertyReference.createForBean(node.getClass(),
                                                    propertyName);
            this.initialValue = initialValue;
            this.defaultValue = this.nodePropertyReference.getValue(this.node);
            this.cssPropertyKey = cssPropertyKey;
            this.cssPropertyValue = cssPropertyValue;
            this.expectedFinalValue = expectedFinalValue;
            this.comparator = comparator;
        }

        public void cssSettableTest() throws Exception {
            assertFalse(UNDEFINED_KEY.isSettable(node));
            assertTrue(cssPropertyKey.isSettable(node));

            final Object propertyModel = BindingHelper.getPropertyModel(
                                                 node, nodePropertyReference);
            assertTrue(cssPropertyKey.isSettable(node));

            final Class<?> typeClass = nodePropertyReference.getValueType();

            final Object variable = BindingHelper.createVariable(typeClass);
            BindingHelper.setWritableValue(typeClass, variable, initialValue);

            BindingHelper.bind(typeClass, propertyModel, variable);
            assertFalse(cssPropertyKey.isSettable(node));

            BindingHelper.unbind(typeClass, propertyModel);
            assertTrue(cssPropertyKey.isSettable(node));
        }

        public void cssSetTest() {
            nodePropertyReference.setValue(node, initialValue);
            StyleableProperty styleableProperty = cssPropertyKey.getStyleableProperty(node);
            styleableProperty.applyStyle(null, cssPropertyValue);

            final Object nodePropertyValue = 
                    nodePropertyReference.getValue(node);
            comparator.assertEquals(expectedFinalValue,
                                    nodePropertyValue);
        }
        
        public void cssDefaultsTest() {
            
            // is the cssInitialValue the same as the node property's default?
            final Object cssInitialValue = 
                    cssPropertyKey.getInitialValue(node);
            
            ValueComparator.DEFAULT.assertEquals(defaultValue, cssInitialValue);
        }
        
        public void cssPropertyReferenceIntegrityTest() {
            
            StyleableProperty prop  = cssPropertyKey.getStyleableProperty(node);
            
            CssMetaData styleable = prop.getCssMetaData();
            
            ValueComparator.DEFAULT.assertEquals(cssPropertyKey, styleable);
        }
    }
}
