/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
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

import static javafx.scene.control.ControlShim.installDefaultSkin;
import static test.com.sun.javafx.scene.control.infrastructure.ControlSkinFactory.createControl;
import static test.com.sun.javafx.scene.control.infrastructure.ControlSkinFactory.getControlClasses;
import java.util.Collection;
import javafx.scene.control.Control;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for https://bugs.openjdk.org/browse/JDK-8244112:
 * skin must not blow if dispose is called more than once.
 * <p>
 * This test is parameterized in the type of control.
 */
public class SkinDisposeContractTest {

    private Control control;

    /**
     * Skin must support multiple calls to dispose.
     * <p>
     * default -> dispose -> dispose
     * <p>
     * Errors on second dispose are JDK-8243940.
     * Failures/errors on first dispose (or before) are other errors - controls
     * are commented with issue reference
     *
     */
    @ParameterizedTest
    @MethodSource("parameters")
    public void testDefaultDispose(Class<Control> controlClass) {
        setup(controlClass);
        installDefaultSkin(control);
        control.getSkin().dispose();
        control.getSkin().dispose();
    }

  //---------------- parameterized

    private static Collection<Class<Control>> parameters() {
        return getControlClasses();
    }

//----------------------

    @AfterEach
    public void cleanup() {
        Thread.currentThread().setUncaughtExceptionHandler(null);
    }

    // @BeforeEach
    // junit5 does not support parameterized class-level tests yet
    public void setup(Class<Control> controlClass) {
        Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException)throwable;
            } else {
                Thread.currentThread().getThreadGroup().uncaughtException(thread, throwable);
            }
        });
        control = createControl(controlClass);
    }
}
