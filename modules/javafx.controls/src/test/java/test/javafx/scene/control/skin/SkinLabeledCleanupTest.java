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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static test.com.sun.javafx.scene.control.infrastructure.ControlSkinFactory.attemptGC;
import static test.com.sun.javafx.scene.control.infrastructure.ControlSkinFactory.createControl;
import static test.com.sun.javafx.scene.control.infrastructure.ControlSkinFactory.replaceSkin;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.List;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.shape.Rectangle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test skin cleanup for Labeled JDK-8247576
 * <p>
 * This test is parameterized on class of Labeled.
 *
 */
public class SkinLabeledCleanupTest {

    private Labeled labeled;

    /**
     * First step was cleanup of graphicListener: removed guard against null skinnable.
     */
    @ParameterizedTest
    @MethodSource("parameters")
    public void testLabeledGraphicDispose(Class<Labeled> labeledClass) {
        setup(labeledClass);
        Rectangle graphic = (Rectangle) labeled.getGraphic();
        installDefaultSkin(labeled);
        labeled.getSkin().dispose();
        graphic.setWidth(500);
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testMemoryLeakAlternativeSkin(Class<Labeled> labeledClass) {
        setup(labeledClass);
        installDefaultSkin(labeled);
        WeakReference<?> weakRef = new WeakReference<>(replaceSkin(labeled));
        assertNotNull(weakRef.get());
        attemptGC(weakRef);
        assertEquals(null, weakRef.get(), "Skin must be gc'ed");
    }

//----------- parameterized

    private static Collection<Class<?>> parameters() {
        return List.of(
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
    }

//---------------- setup/cleanup

    @ParameterizedTest
    @MethodSource("parameters")
    public void testSetupState(Class<Labeled> labeledClass) {
        setup(labeledClass);
        assertNotNull(labeled);
        assertNotNull(labeled.getGraphic());
    }

    @AfterEach
    public void cleanup() {
        Thread.currentThread().setUncaughtExceptionHandler(null);
    }

    // @BeforeEach
    // junit5 does not support parameterized class-level tests yet
    public void setup(Class<Labeled> labeledClass) {
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
