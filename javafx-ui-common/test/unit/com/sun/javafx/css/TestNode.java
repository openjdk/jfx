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

import com.sun.javafx.css.converters.SizeConverter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.beans.property.DoubleProperty;
import javafx.beans.value.WritableValue;

/** Test Node with styleable property but no getClassCssMetaData method */
class TestNode extends TestNodeBase {

    private DoubleProperty xyzzy;
    private DoubleProperty xyzzyProperty() {
        if (xyzzy == null) {
            xyzzy = new StyleableDoubleProperty(.5) {

                @Override
                public Object getBean() {
                    return TestNode.this;
                }

                @Override
                public String getName() {
                    return "xyzzy";
                }

                @Override
                public CssMetaData getCssMetaData() {
                    return StyleableProperties.XYZZY;
                }
                
            };
        }
        return xyzzy;
    }
    
    public void setXyzzy(double number) {
        xyzzyProperty().set(number);
    }
    
    public double getXyzzy() {
        return (xyzzy == null ? .5 : xyzzy.get());
    }

    public TestNode() {
        super();
    }
    
     static class StyleableProperties {

         static final CssMetaData<TestNode, Number> XYZZY = 
             new CssMetaData<TestNode, Number>("-fx-xyzzy",
                 SizeConverter.getInstance(),
                 .5) {

            @Override
            public boolean isSettable(TestNode node) {
                return node.xyzzy == null || !node.xyzzy.isBound();
            }

            @Override
            public WritableValue<Number> getWritableValue(TestNode node) {
                return node.xyzzyProperty();
            }
                     
         };
            
         private static final List<CssMetaData> STYLEABLES;
         static {
            final List<CssMetaData> styleables = 
		new ArrayList<CssMetaData>(TestNodeBase.getClassCssMetaData());
            Collections.addAll(styleables, XYZZY);
            STYLEABLES = Collections.unmodifiableList(styleables);
         }
    }

    /**
     * @return The CssMetaData associated with this class, which may include the
     * CssMetaData of its super classes.
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

}
