/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertTrue;
import javafx.geometry.Insets;
import javafx.util.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.sun.jfx.incubator.scene.control.richtext.VFlow;
import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.RichTextAreaShim;
import jfx.incubator.scene.control.richtext.model.CodeTextModel;
import jfx.incubator.scene.control.richtext.model.RichTextModel;
import test.jfx.incubator.scene.util.TUtil;

/**
 * Tests the RichTextArea control.
 */
public class TestRichTextArea {
    private RichTextArea control;

    @BeforeEach
    public void beforeEach() {
        TUtil.setUncaughtExceptionHandler();
        control = new RichTextArea();
    }

    @AfterEach
    public void afterEach() {
        TUtil.removeUncaughtExceptionHandler();
    }

    // constructors

    @Test
    void defaultModelIsRichTextModel() {
        assertTrue(control.getModel() instanceof RichTextModel);
    }

    @Test
    void nullModelInConstructor() {
        control = new RichTextArea(null);
        assertTrue(control.getModel() == null);

        control = new RichTextArea(null);
        control.setModel(new CustomStyledTextModel());
    }

    // properties

    @Test
    void propertiesSettersAndGetters() {
        TUtil.testProperty(control.caretBlinkPeriodProperty(), control::getCaretBlinkPeriod, control::setCaretBlinkPeriod, Duration.millis(10));
        TUtil.testProperty(control.contentPaddingProperty(), control::getContentPadding, control::setContentPadding, null, new Insets(40));
        TUtil.testBooleanProperty(control.displayCaretProperty(), control::isDisplayCaret, control::setDisplayCaret);
        TUtil.testBooleanProperty(control.editableProperty(), control::isEditable, control::setEditable);
        TUtil.testBooleanProperty(control.highlightCurrentParagraphProperty(), control::isHighlightCurrentParagraph, control::setHighlightCurrentParagraph);
        TUtil.testProperty(control.leftDecoratorProperty(), control::getLeftDecorator, control::setLeftDecorator, null, new CustomSideDecorator());
        TUtil.testProperty(control.modelProperty(), control::getModel, control::setModel, null, new RichTextModel(), new CodeTextModel());
        TUtil.testProperty(control.rightDecoratorProperty(), control::getRightDecorator, control::setRightDecorator, null, new CustomSideDecorator());
        TUtil.testBooleanProperty(control.useContentHeightProperty(), control::isUseContentHeight, control::setUseContentHeight);
        TUtil.testBooleanProperty(control.useContentWidthProperty(), control::isUseContentWidth, control::setUseContentWidth);
        TUtil.testBooleanProperty(control.wrapTextProperty(), control::isWrapText, control::setWrapText);
    }

    @Test
    void nonNullableProperties() {
        TUtil.testNonNullable(control.caretBlinkPeriodProperty(), control::setCaretBlinkPeriod);
    }

    // default values

    @Test
    void defaultPropertyValues() {
        TUtil.testDefaultValue(control.caretBlinkPeriodProperty(), control::getCaretBlinkPeriod, Duration.millis(1000));
        TUtil.testDefaultValue(control.contentPaddingProperty(), control::getContentPadding, null);
        TUtil.testDefaultValue(control.displayCaretProperty(), control::isDisplayCaret, true);
        TUtil.testDefaultValue(control.editableProperty(), control::isEditable, true);
        TUtil.testDefaultValue(control.highlightCurrentParagraphProperty(), control::isHighlightCurrentParagraph, false);
        TUtil.testDefaultValue(control.leftDecoratorProperty(), control::getLeftDecorator, null);
        TUtil.checkDefaultValue(control.modelProperty(), control::getModel, (v) -> (v instanceof RichTextModel));
        TUtil.testDefaultValue(control.rightDecoratorProperty(), control::getRightDecorator, null);
        TUtil.testDefaultValue(control.useContentHeightProperty(), control::isUseContentHeight, false);
        TUtil.testDefaultValue(control.useContentWidthProperty(), control::isUseContentWidth, false);
        TUtil.testDefaultValue(control.wrapTextProperty(), control::isWrapText, false);
    }

    // css
    // property binding
    // misc tests: certain steps on changing properties (clears selection on model change etc)

    /**
     * Tests the shim.
     */
    // TODO remove once a real test which needs the shim is added.
    @Test
    public void testShim() {
        VFlow f = RichTextAreaShim.vflow(control);
    }
}
