/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static test.com.sun.javafx.scene.control.infrastructure.ControlSkinFactory.asArrays;
import static test.com.sun.javafx.scene.control.infrastructure.ControlSkinFactory.attemptGC;
import static test.com.sun.javafx.scene.control.infrastructure.ControlSkinFactory.createControl;
import static test.com.sun.javafx.scene.control.infrastructure.ControlSkinFactory.getControlClasses;
import static test.com.sun.javafx.scene.control.infrastructure.ControlSkinFactory.replaceSkin;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.List;

import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.sun.javafx.tk.Toolkit;

/**
 * Test memory leaks in Skin implementations.
 * <p>
 * This test is parameterized on control type.
 */
@RunWith(Parameterized.class)
public class SkinMemoryLeakTest {

    private Class<Control> controlClass;
    private Control control;

//--------- tests

    /**
     * default skin -> set another instance of default skin
     */
    @Test
    public void testMemoryLeakSameSkinClass() {
        installDefaultSkin(control);
        Skin<?> skin = control.getSkin();
        WeakReference<?> weakRef = new WeakReference<>(skin);

        installDefaultSkin(control);

        skin = null;
        Toolkit.getToolkit().firePulse();

        attemptGC(weakRef);
        assertNull("Unused Skin must be gc'ed", weakRef.get());
    }

    /**
     * default skin -> set another instance of default skin,
     * with scene property set.
     */
    @Test
    public void testMemoryLeakSameSkinClassWithScene() {
        showControl(control, true);
        installDefaultSkin(control);
        Skin<?> skin = control.getSkin();
        WeakReference<?> weakRef = new WeakReference<>(skin);

        installDefaultSkin(control);

        skin = null;
        Toolkit.getToolkit().firePulse();

        attemptGC(weakRef);
        assertNull("Unused Skin must be gc'ed", weakRef.get());
    }

    @Test
    public void testControlChildrenSameSkinClass() {
        installDefaultSkin(control);
        int childCount = control.getChildrenUnmodifiable().size();
        installDefaultSkin(control);
        assertEquals("Old skin should dispose children when a new skin is set",
                childCount, control.getChildrenUnmodifiable().size());
    }

    @Test
    public void testSetSkinOfSameClass() {
        installDefaultSkin(control);
        Skin<?> oldSkin = control.getSkin();
        installDefaultSkin(control);
        Skin<?> newSkin = control.getSkin();

        assertNotEquals("New skin was not set", oldSkin, newSkin);
    }

    /**
     * default skin -> set alternative
     */
    @Test
    public void testMemoryLeakAlternativeSkin() {
        installDefaultSkin(control);
        Skin<?> replacedSkin = replaceSkin(control);
        WeakReference<?> weakRef = new WeakReference<>(replacedSkin);
        assertNotNull(weakRef.get());

        // beware: this is important - we might get false reds without!
        replacedSkin = null;
        Toolkit.getToolkit().firePulse();

        attemptGC(weakRef);
        assertEquals("Skin must be gc'ed", null, weakRef.get());
    }

    /**
     * default skin -> set alternative,
     * with scene property set
     */
    @Test
    public void testMemoryLeakAlternativeSkinWithScene() {
        showControl(control, true);
        installDefaultSkin(control);
        Skin<?> replacedSkin = replaceSkin(control);
        WeakReference<?> weakRef = new WeakReference<>(replacedSkin);
        assertNotNull(weakRef.get());

        // beware: this is important - we might get false reds without!
        replacedSkin = null;
        Toolkit.getToolkit().firePulse();

        attemptGC(weakRef);
        assertEquals("Skin must be gc'ed", null, weakRef.get());
    }

    /**
     * default skin -> set alternative while showing
     */
    @Test
    public void testMemoryLeakAlternativeSkinShowing() {
        showControl(control, true);
        Skin<?> replacedSkin = replaceSkin(control);
        WeakReference<?> weakRef = new WeakReference<>(replacedSkin);
        assertNotNull(weakRef.get());

        // beware: this is important - we might get false reds without!
        replacedSkin = null;
        Toolkit.getToolkit().firePulse();

        attemptGC(weakRef);
        assertEquals("Skin must be gc'ed", null, weakRef.get());
    }

    @Test
    public void testControlChildren() {
        installDefaultSkin(control);
        int childCount = control.getChildrenUnmodifiable().size();
        String skinClass = control.getSkin().getClass().getSimpleName();
        replaceSkin(control);
        assertEquals(skinClass + " must remove direct children that it has added",
                childCount, control.getChildrenUnmodifiable().size());
    }

//------------ parameters

    // Note: name property not supported before junit 4.11
    @Parameterized.Parameters //(name = "{index}: {0} ")
    public static Collection<Object[]> data() {
        List<Class<Control>> controlClasses = getControlClasses();
        return asArrays(controlClasses);
    }

    public SkinMemoryLeakTest(Class<Control> controlClass) {
        this.controlClass = controlClass;
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

    @Before
    public void setup() {
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

    @After
    public void cleanup() {
        if (stage != null) stage.hide();
        Thread.currentThread().setUncaughtExceptionHandler(null);
    }

}
