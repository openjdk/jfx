/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static javafx.scene.control.ControlShim.*;
import static test.com.sun.javafx.scene.control.infrastructure.ControlSkinFactory.*;

import javafx.scene.control.Control;
import javafx.scene.control.TextArea;

/**
 * Test for https://bugs.openjdk.java.net/browse/JDK-8244112:
 * skin must not blow if dispose is called more than once.
 * <p>
 * This test is parameterized in the type of control.
 */
@RunWith(Parameterized.class)
public class SkinDisposeContractTest {

    private Control control;
    private Class<Control> controlClass;

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
    @Test
    public void testDefaultDispose() {
        installDefaultSkin(control);
        control.getSkin().dispose();
        control.getSkin().dispose();
    }

  //---------------- parameterized

    // Note: name property not supported before junit 4.11
    // Note: collection of single values supported since 4.12
    @Parameterized.Parameters //(name = "{index}: {0} ")
    public static Collection<Object[]> data() {
        List<Class<Control>> controlClasses = getControlClasses();
        // @Ignore("8244419")
        controlClasses.remove(TextArea.class);
        return asArrays(controlClasses);
    }

    public SkinDisposeContractTest(Class<Control> controlClass) {
        this.controlClass = controlClass;
    }

//----------------------

    @After
    public void cleanup() {
        Thread.currentThread().setUncaughtExceptionHandler(null);
    }

    @Before
    public void setup() {
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
