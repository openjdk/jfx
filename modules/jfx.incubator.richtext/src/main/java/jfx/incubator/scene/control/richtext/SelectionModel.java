/*
 * Copyright (c) 2022, 2024, Oracle and/or its affiliates. All rights reserved.
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
// This code borrows heavily from the following project, with permission from the author:
// https://github.com/andy-goryachev/FxEditor

package jfx.incubator.scene.control.richtext;

import javafx.beans.property.ReadOnlyProperty;
import jfx.incubator.scene.control.richtext.model.StyledTextModel;

/**
 * A Selection model that maintains a single {@link SelectionSegment}.
 *
 * @since 24
 */
// TODO perhaps we should support, at least theoretically, the concept of multiple selection
// and multiple carets.  The impacted areas:
// this interface
// changes in VFlow to handle multiple carets and decorations
// changes in RichTextAreaBehavior to handle selection and keyboard navigation
public interface SelectionModel {
    /**
     * Clears the selection.  This sets {@code selectionProperty},
     * {@code anchorPositionProperty}, and {@code caretPositionProperty} to null.
     */
    public void clear();

    /**
     * Replaced existing selection, if any, with the new one.  While this method will accept the text positions
     * outside of the document range, the actual values of
     * {@code anchorPositionProperty}, and {@code caretPositionProperty} must always remain within
     * the valid range for the document.
     *
     * @param model the model, must be non-null
     * @param anchor the anchor position, must be non-null
     * @param caret the caret position, must be non-null
     */
    public void setSelection(StyledTextModel model, TextPos anchor, TextPos caret);

    /**
     * Extends selection to the specified position.
     * Internally, the position will be normalized to be within the document boundaries.
     * This method will issue a {@code setSelection(model, pos, pos)}
     * call if the model instance is different from that passed before.
     * <p>
     * While this method will accept the text position
     * outside of the document range, the actual values of
     * {@code anchorPositionProperty}, and {@code caretPositionProperty} must always remain within
     * the valid range for the document.
     *
     * @param model the model, must be non-null
     * @param pos the new caret position, must be non-null
     */
    public void extendSelection(StyledTextModel model, TextPos pos);

    /**
     * Caret position property.  The value can be null, indicating no selection.  When the caret position
     * is {@code null}, the {@code selectionProperty} and the {@code anchorPositionProperty} are also {@code null}.
     * <p>
     * Note:
     * {@link #selectionProperty()}, {@link #anchorPositionProperty()}, and {@link #caretPositionProperty()}
     * are logically connected.  When a change occurs, the anchor position is updated first, followed by
     * the caret position, followed by the selection segment.
     *
     * @return the caret position property
     * @defaultValue null
     */
    public ReadOnlyProperty<TextPos> caretPositionProperty();

    /**
     * Anchor position property.  The value can be null, indicating no selection.  When the anchor position
     * is {@code null}, the {@code selectionProperty} and the {@code caretPositionProperty} are also {@code null}.
     * <p>
     * Note:
     * {@link #selectionProperty()}, {@link #anchorPositionProperty()}, and {@link #caretPositionProperty()}
     * are logically connected.  When a change occurs, the anchor position is updated first, followed by
     * the caret position, followed by the selection segment.
     *
     * @return the anchor position property
     * @defaultValue null
     */
    public ReadOnlyProperty<TextPos> anchorPositionProperty();

    /**
     * Selection property.  The value can be null, indicating no selection.  When the selection segment
     * is {@code null}, the {@code anchorPositionProperty} and the {@code caretPositionProperty} are also {@code null}.
     *
     * @return the selection property
     * @defaultValue null
     */
    public ReadOnlyProperty<SelectionSegment> selectionProperty();

    /**
     * Returns the current selection.  The value can be null, indicating no selection.  When the selection segment
     * is {@code null}, the {@code anchorPositionProperty} and the {@code caretPositionProperty} are also {@code null}.
     *
     * @return current selection, or null
     */
    public SelectionSegment getSelection();
}
