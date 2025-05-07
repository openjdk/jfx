/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.util.function.Supplier;
import javafx.scene.layout.Region;
import jfx.incubator.scene.control.richtext.StyleResolver;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.RichParagraph;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import jfx.incubator.scene.control.richtext.model.StyledTextModel;

/**
 * "Custom" StyledTextModel for testing.
 */
public class CustomStyledTextModel extends StyledTextModel {
    public CustomStyledTextModel() {
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public String getPlainText(int index) {
        return null;
    }

    @Override
    public RichParagraph getParagraph(int index) {
        return null;
    }

    @Override
    protected void removeRange(TextPos start, TextPos end) {
    }

    @Override
    protected int insertTextSegment(int index, int offset, String text, StyleAttributeMap attrs) {
        return 0;
    }

    @Override
    protected void insertLineBreak(int index, int offset) {
    }

    @Override
    protected void insertParagraph(int index, Supplier<Region> generator) {
    }

    @Override
    protected void setParagraphStyle(int index, StyleAttributeMap paragraphAttrs) {
    }

    @Override
    protected void applyStyle(int index, int start, int end, StyleAttributeMap a, boolean merge) {
    }

    @Override
    public StyleAttributeMap getStyleAttributeMap(StyleResolver resolver, TextPos pos) {
        return null;
    }
}
