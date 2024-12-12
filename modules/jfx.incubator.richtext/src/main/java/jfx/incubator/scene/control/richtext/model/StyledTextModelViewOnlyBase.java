/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

package jfx.incubator.scene.control.richtext.model;

import java.util.function.Supplier;
import javafx.scene.layout.Region;
import jfx.incubator.scene.control.richtext.TextPos;

/**
 * The base class for view-only {@link StyledTextModel}s.
 * <p>
 * Models extending this class will not be user editable.
 *
 * @since 24
 */
public abstract class StyledTextModelViewOnlyBase extends StyledTextModel {
    /** The constructor. */
    public StyledTextModelViewOnlyBase() {
        registerDataFormatHandler(RichTextFormatHandler.getInstance(), true, false, 2000);
    }

    /**
     * @return always returns {@code false}
     */
    @Override
    public final boolean isWritable() {
        return false;
    }

    @Override
    protected void removeRange(TextPos start, TextPos end) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected int insertTextSegment(int index, int offset, String text, StyleAttributeMap attrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void insertLineBreak(int index, int offset) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void insertParagraph(int index, Supplier<Region> generator) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected final void setParagraphStyle(int ix, StyleAttributeMap a) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected final void applyStyle(int ix, int start, int end, StyleAttributeMap a, boolean merge) {
        throw new UnsupportedOperationException();
    }
}
