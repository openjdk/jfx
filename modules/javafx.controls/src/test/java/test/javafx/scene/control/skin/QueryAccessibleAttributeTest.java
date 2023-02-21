/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
package test.javafx.scene.control.skin;

import static org.junit.Assert.assertNotNull;
import java.util.Collection;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.control.Control;
import test.com.sun.javafx.scene.control.infrastructure.ControlSkinFactory;

/**
 * Tests whether queryAccessibleAttribute() in every Control handles all of the
 * AccessibleAttribute values without throwing an exception.
 */
@RunWith(Parameterized.class)
public class QueryAccessibleAttributeTest {
    private Class<Node> nodeClass;
    private Node node;

    @Parameterized.Parameters
    public static Collection<Object[]> nodesUnderTest() {
        List<Class<Control>> cs = ControlSkinFactory.getControlClasses();
        return ControlSkinFactory.asArrays(cs);
    }

    public QueryAccessibleAttributeTest(Class<Node> nodeClass) {
        this.nodeClass = nodeClass;
    }

    @Before
    public void setup() {
        Thread.currentThread().setUncaughtExceptionHandler((thread, err) -> {
            if (err instanceof RuntimeException) {
                throw (RuntimeException)err;
            } else {
                Thread.currentThread().getThreadGroup().uncaughtException(thread, err);
            }
        });

        node = createNode(nodeClass);
        assertNotNull(node);
    }

    @After
    public void cleanup() {
        Thread.currentThread().setUncaughtExceptionHandler(null);
    }

    protected static <T extends Node> T createNode(Class<T> controlClass) {
        try {
            return controlClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void queryAllAttributes() {
        for (AccessibleAttribute a: AccessibleAttribute.values()) {
            // should throw no exceptions
            Object val = node.queryAccessibleAttribute(a);
        }
    }
}
