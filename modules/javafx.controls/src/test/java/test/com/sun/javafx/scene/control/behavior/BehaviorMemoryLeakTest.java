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

package test.com.sun.javafx.scene.control.behavior;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static test.com.sun.javafx.scene.control.infrastructure.ControlSkinFactory.attemptGC;
import static test.com.sun.javafx.scene.control.infrastructure.ControlSkinFactory.createBehavior;
import static test.com.sun.javafx.scene.control.infrastructure.ControlSkinFactory.createControl;
import static test.com.sun.javafx.scene.control.infrastructure.ControlSkinFactory.getControlClassesWithBehavior;
import java.lang.ref.WeakReference;
import java.util.List;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeTableView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import com.sun.javafx.scene.control.behavior.BehaviorBase;

/**
 * Test for memory leaks in Behavior implementations.
 * <p>
 * This test is parameterized on control type.
 */
public final class BehaviorMemoryLeakTest {

    /**
     * Create control -> create behavior -> dispose behavior -> gc
     */
    @ParameterizedTest
    @MethodSource("parameters")
    public void testMemoryLeakDisposeBehavior(Class<Control> controlClass) {
        Control control = createControl(controlClass);
        assertNotNull(control);
        WeakReference<BehaviorBase<?>> weakRef = new WeakReference<>(createBehavior(control));
        assertNotNull(weakRef.get());
        weakRef.get().dispose();
        attemptGC(weakRef);
        assertNull(weakRef.get(), "behavior must be gc'ed");
    }

    //---------------- parameterized

    private static List<Class<Control>> parameters() {
        List<Class<Control>> controlClasses = getControlClassesWithBehavior();
        // FIXME as part of JDK-8241364
        // The behaviors of these controls are leaking
        // step 1: file issues (where not yet done), add informal ignore to entry
        // step 2: fix and remove from list
        List<Class<? extends Control>> leakingClasses = List.of(
            // the following use Behavior that must be installed by Skin.install()
            TabPane.class,
            TextField.class,
            TextArea.class,
            PasswordField.class,
            ColorPicker.class,
            DatePicker.class,
            ComboBox.class,
            // FIX as part of JDK-8241364
            TableView.class,
            // FIX as part of JDK-8241364
            TreeTableView.class
         );
        // remove the known issues to make the test pass
        controlClasses.removeAll(leakingClasses);
        return controlClasses;
    }

//------------------- setup

    @AfterEach
    public void cleanup() {
        Thread.currentThread().setUncaughtExceptionHandler(null);
    }

    @BeforeEach
    public void setup() {
        Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException)throwable;
            } else {
                Thread.currentThread().getThreadGroup().uncaughtException(thread, throwable);
            }
        });
    }
}
