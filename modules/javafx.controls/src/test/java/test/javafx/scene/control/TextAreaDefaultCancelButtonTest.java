/*
 * Copyright (c) 2019, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.control;

import javafx.scene.control.TextArea;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for interplay of ENTER/ESCAPE handlers on TextArea with
 * default/cancel button actions.
 */
public class TextAreaDefaultCancelButtonTest extends DefaultCancelButtonTestBase<TextArea> {
    /**
     * Overridden to back out for ENTER (which is handled internally always)
     */
    @ParameterizedTest
    @MethodSource("parameters")
    @Override
    public void testFallbackFilter(ButtonType buttonType, boolean consume, boolean registerAfterShowing) {
        if (isEnter(buttonType)) return;
        super.testFallbackFilter(buttonType, consume, registerAfterShowing);
    }

    /**
     * Overridden to back out for ENTER (which is handled internally always)
     */
    @ParameterizedTest
    @MethodSource("parameters")
    @Override
    public void testFallbackHandler(ButtonType buttonType, boolean consume, boolean registerAfterShowing) {
        if (isEnter(buttonType)) return;
        super.testFallbackHandler(buttonType, consume, registerAfterShowing);
    }

    /**
     * Overridden to back out for ENTER (which is handled internally always)
     */
    @ParameterizedTest
    @MethodSource("parameters")
    @Override
    public void testFallbackSingletonHandler(ButtonType buttonType, boolean consume, boolean registerAfterShowing) {
        if (isEnter(buttonType)) return;
        super.testFallbackSingletonHandler(buttonType, consume, registerAfterShowing);
    }

    /**
     * Overridden to back out for ENTER (which is handled internally always)
     */
    @ParameterizedTest
    @MethodSource("parameters")
    @Override
    public void testFallbackNoHandler(ButtonType buttonType, boolean consume, boolean registerAfterShowing) {
        if (isEnter(buttonType)) return;
        super.testFallbackNoHandler(buttonType, consume, registerAfterShowing);
    }

    @Override
    protected TextArea createControl() {
        return new TextArea();
    }
}
