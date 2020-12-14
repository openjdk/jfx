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

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static javafx.scene.control.ControlShim.*;
import static org.junit.Assert.*;
import static test.com.sun.javafx.scene.control.infrastructure.ControlSkinFactory.*;

import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.shape.Rectangle;

/**
 * Test skin cleanup for Labeled JDK-8247576
 * <p>
 * This test is parameterized on class of Labeled.
 *
 */
@RunWith(Parameterized.class)
public class SkinLabeledCleanupTest {

    private Class<Labeled> labeledClass;
    private Labeled labeled;

    /**
     * First step was cleanup of graphicListener: removed guard against null skinnable.
     */
    @Test
    public void testLabeledGraphicDispose() {
        Rectangle graphic = (Rectangle) labeled.getGraphic();
        installDefaultSkin(labeled);
        labeled.getSkin().dispose();
        graphic.setWidth(500);
    }

    @Test
    public void testMemoryLeakAlternativeSkin() {
        installDefaultSkin(labeled);
        WeakReference<?> weakRef = new WeakReference<>(replaceSkin(labeled));
        assertNotNull(weakRef.get());
        attemptGC(weakRef);
        assertEquals("Skin must be gc'ed", null, weakRef.get());
    }

//----------- parameterized

    @Parameterized.Parameters //(name = "{index}: {0} ")
    public static Collection<Object[]> data() {
        List<Class> labeledClasses = List.of(
               Button.class,
               CheckBox.class,
               Hyperlink.class,
               Label.class,
               // MenuButton is-a Labeled but its skin is-not-a LabeledSkinBase
               // leaking has different reason/s
               // MenuButton.class,
               ToggleButton.class,
               RadioButton.class,
               TitledPane.class
                );
        return asArrays(labeledClasses);
    }

    public SkinLabeledCleanupTest(Class<Labeled> labeledClass) {
        this.labeledClass = labeledClass;
    }

//---------------- setup/cleanup

    @Test
    public void testSetupState() {
        assertNotNull(labeled);
        assertNotNull(labeled.getGraphic());
    }

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

        labeled = createControl(labeledClass);
        labeled.setGraphic(new Rectangle());
    }

}
