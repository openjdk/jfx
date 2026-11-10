/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package test.jfx.incubator.scene.control.richtext;

import static javafx.scene.control.ControlShim.installDefaultSkin;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import jfx.incubator.scene.control.richtext.CodeArea;
import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.skin.CodeAreaSkin;
import jfx.incubator.scene.control.richtext.skin.RichTextAreaSkin;

/// This is a duplicate of `SkinMemoryLeakTest` (with some missing methods) for testing
/// [CodeArea] and [RichTextArea] incubator only.
/// TODO merge with the main test once the incubator is integrated.
public class SkinMemoryLeakTest_INCUBATOR {

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

    private static Collection<Class<? extends Control>> parameters() {
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

    //

    /**
     * Tries to let the weakRef be gc'ed.
     * @param weakRef the weakRef to be gc'ed
     */
    public static void attemptGC(WeakReference<?> weakRef) {
        for (int i = 0; i < 10; i++) {
            System.gc();

            if (weakRef.get() == null) {
                break;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                fail("InterruptedException occurred during Thread.sleep()");
            }
        }
    }

    /**
     * Creates and returns an instance of the given control class.
     * @param <T> the type of the control
     * @param controlClass the class of the control
     * @return an instance of the class
     */
    public static <T extends Control> T createControl(Class<T> controlClass) {
        try {
            return controlClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a list of all control classes in package controls.
     *
     * @return a list control classes in package controls
     */
    public static List<Class<? extends Control>> getControlClasses() {
        return List.of(
            CodeArea.class,
            RichTextArea.class
        );
    }

    /**
     * Creates and sets an alternative skin for the given control.
     *
     * @param <T> the type of the control
     * @param control the control to set the alternative skin to
     * @return the old skin of the control.
     */
    public static <T extends Control> Skin<?> replaceSkin(T control) {
        Skin<?> old = control.getSkin();
        control.setSkin(createAlternativeSkin(control));
        return old;
    }

    /**
     * Creates and returns an alternative skin for the given control.
     * This implementation uses the alternativeSkinsMap to lookup the
     * class for the alternative skin and instantiates it.
     *
     * @param <T> the type of the control
     * @param control the control to create an alternative skin for
     * @return the alternative skin for the control
     * @throws RuntimeException with the exception thrown thrown when instantiating the skin
     */
    public static <T extends Control> Skin<?> createAlternativeSkin(T control) {
        Class<?> controlClass = control.getClass();
        try {
            Class<?> skinClass =
                alternativeSkinClassMap.get(controlClass);
             return  (Skin<?>) skinClass.getDeclaredConstructor(controlClass).newInstance(control);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // map for alternative skins
    static Map<Class<?>, Class<?>> alternativeSkinClassMap = new HashMap<>();

    // filling the map .. could do without and create the alternative
    // skin classes by naming convention
    static {
        alternativeSkinClassMap.put(CodeArea.class, CodeAreaSkin1.class);
        alternativeSkinClassMap.put(RichTextArea.class, RichTextAreaSkin1.class);
    }

    public static class CodeAreaSkin1 extends CodeAreaSkin {
        public CodeAreaSkin1(CodeArea c) {
            super(c);
        }
    }

    public static class RichTextAreaSkin1 extends RichTextAreaSkin {
        public RichTextAreaSkin1(RichTextArea c) {
            super(c);
        }
    }
}
