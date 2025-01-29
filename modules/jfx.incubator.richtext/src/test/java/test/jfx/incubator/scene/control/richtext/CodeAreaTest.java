/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import jfx.incubator.scene.control.richtext.CodeArea;
import jfx.incubator.scene.control.richtext.model.CodeTextModel;
import jfx.incubator.scene.control.richtext.model.RichTextModel;
import jfx.incubator.scene.control.richtext.skin.CodeAreaSkin;

/**
 * Tests CodeArea.
 */
public class CodeAreaTest {
    private CodeArea control;

    @BeforeEach
    public void beforeEach() {
        setUncaughtExceptionHandler();
        control = new CodeArea();
        control.setSkin(new CodeAreaSkin(control));
    }

    @AfterEach
    public void afterEach() {
        removeUncaughtExceptionHandler();
    }

    private void setUncaughtExceptionHandler() {
        Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException)throwable;
            } else {
                Thread.currentThread().getThreadGroup().uncaughtException(thread, throwable);
            }
        });
    }

    private void removeUncaughtExceptionHandler() {
        Thread.currentThread().setUncaughtExceptionHandler(null);
    }

    @Test
    public void getText() {
        control.setText("123");
        String s = control.getText();
        assertEquals("123", s);

        control.setText(null);
        s = control.getText();
        assertEquals("", s);

        control.setText("1\n2\n3\n4");
        s = control.getText();
        assertEquals("1\n2\n3\n4", s);

        control.setText("1\r\n2\r\n3\r\n4");
        s = control.getText();
        assertEquals("1\n2\n3\n4", s);

        control.setModel(null);
        s = control.getText();
        assertEquals("", s);
    }

    /** can set a null and non-null CodeTextModel */
    @Test
    public void nullModel() {
        CodeArea t = new CodeArea();
        t.setModel(null);
        t.setModel(new CodeTextModel());

    }

    /** disallows setting model other than CodeTextModel */
    @Test
    public void wrongModel() {
        CodeArea t = new CodeArea();
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            t.setModel(new RichTextModel());
        });
        Assertions.assertTrue(t.getModel() instanceof CodeTextModel);
    }

    /** acceptable custom model */
    @Test
    public void acceptableModel() {
        class M extends CodeTextModel { }
        M custom = new M();
        CodeArea t = new CodeArea();
        t.setModel(custom);
        Assertions.assertTrue(t.getModel() instanceof M);
    }
}
