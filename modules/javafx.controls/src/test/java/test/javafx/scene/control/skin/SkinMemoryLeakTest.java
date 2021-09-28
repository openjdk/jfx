/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.sun.javafx.tk.Toolkit;

import static javafx.scene.control.ControlShim.*;
import static org.junit.Assert.*;
import static test.com.sun.javafx.scene.control.infrastructure.ControlSkinFactory.*;

import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuButton;
import javafx.scene.control.Pagination;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Skin;
import javafx.scene.control.Spinner;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

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
     * default skin -> set alternative
     */
    @Test
    public void testMemoryLeakAlternativeSkin() {
        installDefaultSkin(control);
        // FIXME: JDK-8265406 - fragile test pattern
        WeakReference<?> weakRef = new WeakReference<>(replaceSkin(control));
        assertNotNull(weakRef.get());
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
        Toolkit.getToolkit().firePulse();
        replacedSkin = null;
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
        // FIXME as part of JDK-8241364
        // The default skins of these controls are leaking
        // step 1: file issues (where not yet done), add informal ignore to entry
        // step 2: fix and remove from list
        List<Class<? extends Control>> leakingClasses = List.of(
                Accordion.class,
                ButtonBar.class,
                ColorPicker.class,
                ComboBox.class,
                DatePicker.class,
                MenuBar.class,
                MenuButton.class,
                Pagination.class,
                PasswordField.class,
                ScrollBar.class,
                ScrollPane.class,
                // @Ignore("8245145")
                Spinner.class,
                SplitMenuButton.class,
                SplitPane.class,
                TableRow.class,
                TableView.class,
                TreeTableRow.class,
                TreeTableView.class
        );
        // remove the known issues to make the test pass
        controlClasses.removeAll(leakingClasses);
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
