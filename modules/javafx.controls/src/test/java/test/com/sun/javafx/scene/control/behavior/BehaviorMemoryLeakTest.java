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

package test.com.sun.javafx.scene.control.behavior;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.sun.javafx.scene.control.behavior.BehaviorBase;

import static org.junit.Assert.*;
import static test.com.sun.javafx.scene.control.infrastructure.ControlSkinFactory.*;

import javafx.scene.control.Control;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeTableView;

/**
 * Test for memory leaks in Behavior implementations.
 * <p>
 * This test is parameterized on control type.
 */
@RunWith(Parameterized.class)
public class BehaviorMemoryLeakTest {

    private Class<Control> controlClass;
    private Control control;

    /**
     * Create behavior -> dispose behavior -> gc
     */
    @Test
    public void testMemoryLeakDisposeBehavior() {
        WeakReference<BehaviorBase<?>> weakRef = new WeakReference<>(createBehavior(control));
        assertNotNull(weakRef.get());
        weakRef.get().dispose();
        attemptGC(weakRef);
        assertNull("behavior must be gc'ed", weakRef.get());
    }

    //---------------- parameterized

    // Note: name property not supported before junit 4.11
    @Parameterized.Parameters // (name = "{index}: {0} ")
    public static Collection<Object[]> data() {
        List<Class<Control>> controlClasses = getControlClassesWithBehavior();
        // FIXME as part of JDK-8241364
        // The behaviors of these controls are leaking
        // step 1: file issues (where not yet done), add informal ignore to entry
        // step 2: fix and remove from list
        List<Class<? extends Control>> leakingClasses = List.of(
                PasswordField.class,
                TableView.class,
                TextArea.class,
                TextField.class,
                TreeTableView.class
         );
        // remove the known issues to make the test pass
        controlClasses.removeAll(leakingClasses);
        return asArrays(controlClasses);
    }

    public BehaviorMemoryLeakTest(Class<Control> controlClass) {
        this.controlClass = controlClass;
    }

//------------------- setup

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
        assertNotNull(control);
    }

}
