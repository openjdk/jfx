/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.Collection;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.control.Control;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import test.com.sun.javafx.scene.control.infrastructure.ControlSkinFactory;

/**
 * Tests whether queryAccessibleAttribute() in every Control handles all of the
 * AccessibleAttribute values without throwing an exception.
 */
public class QueryAccessibleAttributeTest {
    private Node node;

    private static Collection<Class<Control>> parameters() {
        return ControlSkinFactory.getControlClasses();
    }

    // @BeforeEach
    // junit5 does not support parameterized class-level tests yet
    public void setup(Class<Node> nodeClass) {
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

    @AfterEach
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

    @ParameterizedTest
    @MethodSource("parameters")
    public void queryAllAttributes(Class<Node> nodeClass) {
        setup(nodeClass);
        for (AccessibleAttribute a: AccessibleAttribute.values()) {
            // should throw no exceptions
            Object val = node.queryAccessibleAttribute(a);
        }
    }
}
