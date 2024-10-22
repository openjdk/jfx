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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static test.com.sun.javafx.scene.control.infrastructure.ControlSkinFactory.attemptGC;
import static test.com.sun.javafx.scene.control.infrastructure.ControlSkinFactory.createControl;
import static test.com.sun.javafx.scene.control.infrastructure.ControlSkinFactory.getControlClasses;
import static test.com.sun.javafx.scene.control.infrastructure.ControlSkinFactory.replaceSkin;
import java.lang.ref.WeakReference;
import java.util.Collection;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import com.sun.javafx.tk.Toolkit;

/**
 * Test memory leaks in Skin implementations.
 * <p>
 * This test is parameterized on control type.
 */
public class SkinMemoryLeakTest {

    private Control control;

//--------- tests

    /**
     * default skin -> set another instance of default skin
     */
    @ParameterizedTest
    @MethodSource("parameters")
    public void testMemoryLeakSameSkinClass(Class<Control> controlClass) {
        setup(controlClass);
        installDefaultSkin(control);
        Skin<?> skin = control.getSkin();
        WeakReference<?> weakRef = new WeakReference<>(skin);

        installDefaultSkin(control);

        skin = null;
        Toolkit.getToolkit().firePulse();

        attemptGC(weakRef);
        assertNull(weakRef.get(), "Unused Skin must be gc'ed");
    }

    /**
     * default skin -> set another instance of default skin,
     * with scene property set.
     */
    @ParameterizedTest
    @MethodSource("parameters")
    public void testMemoryLeakSameSkinClassWithScene(Class<Control> controlClass) {
        setup(controlClass);
        showControl(control, true);
        installDefaultSkin(control);
        Skin<?> skin = control.getSkin();
        WeakReference<?> weakRef = new WeakReference<>(skin);

        installDefaultSkin(control);

        skin = null;
        Toolkit.getToolkit().firePulse();

        attemptGC(weakRef);
        assertNull(weakRef.get(), "Unused Skin must be gc'ed");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testControlChildrenSameSkinClass(Class<Control> controlClass) {
        setup(controlClass);
        installDefaultSkin(control);
        int childCount = control.getChildrenUnmodifiable().size();
        installDefaultSkin(control);
        assertEquals(
                childCount, control.getChildrenUnmodifiable().size(),
                "Old skin should dispose children when a new skin is set");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testSetSkinOfSameClass(Class<Control> controlClass) {
        setup(controlClass);
        installDefaultSkin(control);
        Skin<?> oldSkin = control.getSkin();
        installDefaultSkin(control);
        Skin<?> newSkin = control.getSkin();

        assertNotEquals(oldSkin, newSkin, "New skin was not set");
    }

    /**
     * default skin -> set alternative
     */
    @ParameterizedTest
    @MethodSource("parameters")
    public void testMemoryLeakAlternativeSkin(Class<Control> controlClass) {
        setup(controlClass);
        installDefaultSkin(control);
        Skin<?> replacedSkin = replaceSkin(control);
        WeakReference<?> weakRef = new WeakReference<>(replacedSkin);
        assertNotNull(weakRef.get());

        // beware: this is important - we might get false reds without!
        replacedSkin = null;
        Toolkit.getToolkit().firePulse();

        attemptGC(weakRef);
        assertEquals(null, weakRef.get(), "Skin must be gc'ed");
    }

    /**
     * default skin -> set alternative,
     * with scene property set
     */
    @ParameterizedTest
    @MethodSource("parameters")
    public void testMemoryLeakAlternativeSkinWithScene(Class<Control> controlClass) {
        setup(controlClass);
        showControl(control, true);
        installDefaultSkin(control);
        Skin<?> replacedSkin = replaceSkin(control);
        WeakReference<?> weakRef = new WeakReference<>(replacedSkin);
        assertNotNull(weakRef.get());

        // beware: this is important - we might get false reds without!
        replacedSkin = null;
        Toolkit.getToolkit().firePulse();

        attemptGC(weakRef);
        assertEquals(null, weakRef.get(), "Skin must be gc'ed");
    }

    /**
     * default skin -> set alternative while showing
     */
    @ParameterizedTest
    @MethodSource("parameters")
    public void testMemoryLeakAlternativeSkinShowing(Class<Control> controlClass) {
        setup(controlClass);
        showControl(control, true);
        Skin<?> replacedSkin = replaceSkin(control);
        WeakReference<?> weakRef = new WeakReference<>(replacedSkin);
        assertNotNull(weakRef.get());

        // beware: this is important - we might get false reds without!
        replacedSkin = null;
        Toolkit.getToolkit().firePulse();

        attemptGC(weakRef);
        assertEquals(null, weakRef.get(), "Skin must be gc'ed");
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testControlChildren(Class<Control> controlClass) {
        setup(controlClass);
        installDefaultSkin(control);
        int childCount = control.getChildrenUnmodifiable().size();
        String skinClass = control.getSkin().getClass().getSimpleName();
        replaceSkin(control);
        assertEquals(
                childCount, control.getChildrenUnmodifiable().size(),
                skinClass + " must remove direct children that it has added");
    }

//------------ parameters

    private static Collection<Class<Control>> parameters() {
        return getControlClasses();
    }

//------------ setup

    private Scene scene;
    private Stage stage;
    private Pane root;

   /**
     * Ensures the control is shown in an active scenegraph. Requests
     * focus on the control if focused == true.
     *
     * @param control the control to show
     * @param focused if true, requests focus on the added control
     */
    protected void showControl(Control control, boolean focused) {
        if (root == null) {
            root = new VBox();
            scene = new Scene(root);
            stage = new Stage();
            stage.setScene(scene);
        }
        if (!root.getChildren().contains(control)) {
            root.getChildren().add(control);
        }
        stage.show();
        if (focused) {
            stage.requestFocus();
            control.requestFocus();
            assertTrue(control.isFocused());
            assertSame(control, scene.getFocusOwner());
        }
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
        this.control = createControl(controlClass);
        assertNotNull(control);
    }

    @AfterEach
    public void cleanup() {
        if (stage != null) stage.hide();
        Thread.currentThread().setUncaughtExceptionHandler(null);
    }
}
