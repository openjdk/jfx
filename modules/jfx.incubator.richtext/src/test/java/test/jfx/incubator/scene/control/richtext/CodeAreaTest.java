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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import jfx.incubator.scene.control.richtext.CodeArea;
import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.model.CodeTextModel;
import jfx.incubator.scene.control.richtext.model.RichTextModel;
import jfx.incubator.scene.control.richtext.skin.CodeAreaSkin;
import test.jfx.incubator.scene.util.TUtil;

/**
 * Tests the CodeArea control.
 */
public class CodeAreaTest {
    private CodeArea control;

    @BeforeEach
    public void beforeEach() {
        TUtil.setUncaughtExceptionHandler();
        control = new CodeArea();
        control.setSkin(new CodeAreaSkin(control));
    }

    @AfterEach
    public void afterEach() {
        TUtil.removeUncaughtExceptionHandler();
    }

    // constructors

    @Test
    public void defaultModelIsCodeTextModel() {
        assertTrue(control.getModel() instanceof CodeTextModel);
    }

    @Test
    public void nullModelInConstructor() {
        control = new CodeArea(null);
        assertTrue(control.getModel() == null);

        control = new CodeArea(null);
        CodeTextModel m = new CodeTextModel() { };
        control.setModel(m);
        assertSame(m, control.getModel());
    }

    // properties

    @Test
    public void propertiesSettersAndGetters() {
        TUtil.testProperty(control.fontProperty(), control::getFont, control::setFont, new Font("Bogus", 22));
        TUtil.testBooleanProperty(control.lineNumbersEnabledProperty(), control::isLineNumbersEnabled, control::setLineNumbersEnabled);
        TUtil.testProperty(control.lineSpacingProperty(), control::getLineSpacing, (n) -> control.setLineSpacing(n.doubleValue()), 10.0, 22.0);
        TUtil.testProperty(control.tabSizeProperty(), control::getTabSize, (n) -> control.setTabSize(n.intValue()), 1, 2);
    }

    // default values

    @Test
    public void defaultPropertyValues() {
        TUtil.checkDefaultValue(control.fontProperty(), control::getFont, (f) -> {
            Font expected = new Font("Monospace", -1);
            assertEquals(f.getFamily(), expected.getFamily());
            assertEquals(f.getSize(), expected.getSize());
            return true;
        });
        TUtil.testDefaultValue(control.lineNumbersEnabledProperty(), control::isLineNumbersEnabled, false);
        TUtil.testDefaultValue(control.lineSpacingProperty(), control::getLineSpacing, 0.0);
        TUtil.testDefaultValue(control.tabSizeProperty(), control::getTabSize, 8);
    }

    // css

    @Test
    public void testFontCSS() {
        Scene s = new Scene(control);
        control.setStyle("-fx-font: 24 Amble");
        control.applyCss();
        assertEquals(Font.font("Amble", 24), control.getFont());
    }

    @Test
    public void testLineSpacingCSS() {
        Scene s = new Scene(control);
        control.setStyle("-fx-line-spacing: 5.55");
        control.applyCss();
        assertEquals(5.55, control.getLineSpacing());
    }

    @Test
    public void testTabSizeCSS() {
        Scene s = new Scene(control);
        control.setStyle("-fx-tab-size: 17");
        control.applyCss();
        assertEquals(17, control.getTabSize());
    }

    // property binding

    @Test
    public void testPropertyBinding() {
        TUtil.testBinding(control.fontProperty(), control::getFont, new Font("Bogus", 22));
        TUtil.testBinding(control.lineNumbersEnabledProperty(), control::isLineNumbersEnabled);
        TUtil.testBinding(control.lineSpacingProperty(), control::getLineSpacing, 10.0, 22.0);
        TUtil.testBinding(control.tabSizeProperty(), control::getTabSize, 1, 2, 5, 17);
    }

    // functional API tests

    @Test
    public void getControlCssMetaData() {
        List<CssMetaData<? extends Styleable, ?>> md = control.getControlCssMetaData();
        // CodeArea:395
        int styleablesCount = 3;
        assertEquals(md.size(), RichTextArea.getClassCssMetaData().size() + styleablesCount);
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
    public void modelNull() {
        control.setModel(null);
        control.setModel(new CodeTextModel());
    }

    /** disallows setting model other than CodeTextModel */
    @Test
    public void modelWrong() {
        var m = control.getModel();
        assertThrows(IllegalArgumentException.class, () -> {
            control.setModel(new RichTextModel());
        });
        assertTrue(control.getModel() == m);
    }

    /** acceptable custom model */
    @Test
    public void modelAcceptable() {
        CustomCodeTextModel m = new CustomCodeTextModel();
        control.setModel(m);
        assertTrue(control.getModel() == m);
    }
}
