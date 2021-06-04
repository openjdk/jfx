/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.test;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.List;

import javafx.scene.Node;
import test.javafx.scene.NodeTest;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Transform;

import org.junit.Test;

import javafx.css.CssMetaData;
import com.sun.javafx.scene.DirtyBits;

public abstract class OnInvalidateMethodsTestBase {

    private final Configuration configuration;

    public OnInvalidateMethodsTestBase(final Configuration configuration) {
        this.configuration = configuration;
    }

    @Test
    public void testFireOnInvalidate() throws Exception {
        configuration.testFireOnInvalidate();
    }

    public static class Configuration {
        Class clazz;
        String propertyName;
        Object inValue;
        Object[] expectedDirtyBits;

        public Configuration(Class clazz, String parameterName,
                Object inValue, Object[] expectedDirtyBits) {
            this.clazz = clazz;
            this.propertyName = parameterName;
            this.inValue = inValue;
            this.expectedDirtyBits = expectedDirtyBits;
        }

        static final String GET_PREFIX = "get";
        static final String IS_PREFIX = "is";
        static final String SET_PREFIX = "set";

        public void testFireOnInvalidate() throws Exception {
            StringBuilder sb = new StringBuilder(this.propertyName);
            sb.setCharAt(0, Character.toUpperCase(propertyName.charAt(0)));

            if (clazz.getSuperclass().equals(PathElement.class)) {
                PathElement e = (PathElement)clazz.getDeclaredConstructor().newInstance();
                Path path = new Path();
                path.getElements().addAll(new MoveTo(0,0), e);
                NodeTest.syncNode(path);
                getSetter(clazz, sb.toString()).invoke(e, this.inValue);
                assertTrue(NodeTest.isDirty(path, (DirtyBits[])this.expectedDirtyBits));
            } else if (clazz.getSuperclass().equals(Transform.class)) {
                Transform tr = (Transform)clazz.getDeclaredConstructor().newInstance();
                Rectangle rect = new Rectangle();
                rect.getTransforms().add(tr);
                NodeTest.syncNode(rect);
                getSetter(clazz, sb.toString()).invoke(tr, this.inValue);
                assertTrue(NodeTest.isDirty(rect, (DirtyBits[])this.expectedDirtyBits));
            } else {
                Node node = (Node)clazz.getDeclaredConstructor().newInstance();
                NodeTest.syncNode(node);
                getSetter(clazz, sb.toString()).invoke(node, this.inValue);
                if (this.expectedDirtyBits instanceof DirtyBits[]) {
                    assertTrue(NodeTest.isDirty(node, (DirtyBits[])this.expectedDirtyBits));
                } else if (this.expectedDirtyBits instanceof CssMetaData[]) {
                    for(CssMetaData key:(CssMetaData[])this.expectedDirtyBits) {
                        assertTrue(key.isSettable(node));
                    }
                }
            }

        }

        private Path getPathNode(Class<? extends PathElement> pathElementClazz) throws Exception {
            Path p = new Path();
            p.getElements().addAll(new MoveTo(0,0), pathElementClazz.getDeclaredConstructor().newInstance());
            return p;
        }

        private Method getSetter(Class cls, String name) throws Exception {
            Method getter = (inValue instanceof Boolean) ?
                cls.getMethod(IS_PREFIX + name, new Class[]{}) :
                cls.getMethod(GET_PREFIX + name, new Class[]{});
            return cls.getMethod(SET_PREFIX + name, getter.getReturnType());
        }
    }

//    protected static CssMetaData findCssKey(Class clazz, String propertyName) {
//        final List<CSSProperty> STYLEABLES = CssMetaData.getStyleables(clazz);
//        for(CssMetaData styleable : STYLEABLES) {
//            if (styleable.getProperty().equals(propertyName)) return styleable;
//        }
//        return null;
//    }


}
