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
package com.sun.javafx.css;

import com.sun.javafx.css.converters.BooleanConverter;
import com.sun.javafx.css.converters.SizeConverter;
import com.sun.javafx.css.converters.StringConverter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.jmx.MXNodeAlgorithm;
import com.sun.javafx.jmx.MXNodeAlgorithmContext;
import com.sun.javafx.sg.PGNode;
import javafx.beans.value.WritableValue;

/** Test Node with styleable properties and an getClassCssMetaData method */
class TestNodeBase extends Node {

    protected TestNodeBase() {
    }

    @Override
    protected boolean impl_computeContains(double d, double d1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BaseBounds impl_computeGeomBounds(BaseBounds bb, BaseTransform bt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected PGNode impl_createPGNode() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private BooleanProperty test;
    private BooleanProperty testProperty() {
        if (test == null) {
            test = new StyleableBooleanProperty(true) {

                @Override
                public Object getBean() {
                    return TestNodeBase.this;
                }

                @Override
                public String getName() {
                    return "test";
                }

                @Override
                public CssMetaData getCssMetaData() {
                    return TestNodeBase.StyleableProperties.TEST;
                }
                
            };
        }
        return test;
    }
    
    public void setTest(boolean value) {
        testProperty().set(value);
    }
    
    public boolean getTest() {
        return (test == null ? true : test.get());
    }
    
    private StringProperty string;
    private StringProperty stringProperty() {
        if (string == null) {
            string = new StyleableStringProperty("init string") {

                @Override
                public Object getBean() {
                    return TestNodeBase.this;
                }

                @Override
                public String getName() {
                    return "string";
                }

                @Override
                public CssMetaData getCssMetaData() {
                    return TestNodeBase.StyleableProperties.STRING;
                }
                
            };
        }
        return string;
    }
    
    public void setString(String value) {
        stringProperty().set(value);
    }
    
    public String getString() {
        return (string == null ? "init string" : string.get());
    }  
    
    private DoubleProperty doubleProperty;
    private DoubleProperty doublePropertyProperty() {
        if (doubleProperty == null) {
            doubleProperty = new StyleableDoubleProperty(0) {

                @Override
                public Object getBean() {
                    return TestNodeBase.this;
                }

                @Override
                public String getName() {
                    return "doubleProperty";
                }

                @Override
                public CssMetaData getCssMetaData() {
                    return TestNodeBase.StyleableProperties.DOUBLE_PROPERTY;
                }
                
            };
        }
        return doubleProperty;
    }
    
    public void setDoubleProperty(double number) {
        doublePropertyProperty().set(number);
    }
    
    public double getDoubleProperty() {
        return (doubleProperty == null ? 0 : doubleProperty.get());
    }
    

    static class StyleableProperties {
        public final static CssMetaData<TestNodeBase,Boolean> TEST =
                new CssMetaData<TestNodeBase,Boolean>("-fx-test", 
                BooleanConverter.getInstance(), Boolean.TRUE) {

            @Override
            public boolean isSettable(TestNodeBase n) {
                return n.test == null || !n.test.isBound();
            }

            @Override
            public WritableValue<Boolean> getWritableValue(TestNodeBase n) {
                return n.testProperty();
            }
        };

        public final static CssMetaData<TestNodeBase,String> STRING =
                new CssMetaData<TestNodeBase,String>("-fx-string", 
                StringConverter.getInstance(), "init string") {

            @Override
            public boolean isSettable(TestNodeBase n) {
                return n.string == null || !n.string.isBound();
            }

            @Override
            public WritableValue<String> getWritableValue(TestNodeBase n) {
                return n.stringProperty();
            }
        };

        public final static CssMetaData<TestNodeBase,Number> DOUBLE_PROPERTY =
                new CssMetaData<TestNodeBase,Number>("-fx-double-property", 
                SizeConverter.getInstance(), 0) {

            @Override
            public boolean isSettable(TestNodeBase n) {
                return n.doubleProperty == null || !n.doubleProperty.isBound();
            }

            @Override
            public WritableValue<Number> getWritableValue(TestNodeBase n) {
                return n.doublePropertyProperty();
            }
        };
        
        static final List<CssMetaData> STYLEABLES;
        static {
            List<CssMetaData> list = 
                new ArrayList<CssMetaData>(Node.getClassCssMetaData());
            Collections.addAll(list,                
                TEST,
                STRING,
                DOUBLE_PROPERTY
            );
            STYLEABLES = Collections.unmodifiableList(list);
        }
    }
                
    /**
     * {@inheritDoc}
     */
    public static List<CssMetaData> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CssMetaData> getCssMetaData() {
        return getClassCssMetaData();
    }

    @Override
    public Object impl_processMXNode(MXNodeAlgorithm alg, MXNodeAlgorithmContext ctx) {
        return alg.processLeafNode(this, ctx);
    }
}
