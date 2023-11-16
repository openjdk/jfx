/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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

package javafx.incubator.scene.control.rich;

import javafx.beans.property.ReadOnlyProperty;

/**
 * A Selection model that maintains a single {@link SelectionSegment}.
 * <pre>
 * TODO perhaps we should support, at least theoretically, the concept of multiple selection
 * and multiple carets.  The impacted areas:
 * - this interface
 * - changes in VFlow to handle multiple carets and decorations
 * - changes in RichTextAreaBehavior to handle selection and keyboard navigation
 * </pre>
 */
public interface SelectionModel {
    /**
     * Clears the selection.
     */
    public void clear();

    /**
     * Replaced existing selection, if any, with the new one.
     * @param anchor anchor position
     * @param caret caret position
     */
    public void setSelection(Marker anchor, Marker caret);

    /**
     * Replaces the existing selection, if any, with a new one
     * from the anchor to the specified position.
     * @param pos the new caret position
     */
    public void extendSelection(Marker pos);
    
    /**
     * Caret position property.  The value can be null.
     * <p>
     * Important note: setting a {@link SelectionSegment} causes an update to both anchor and caret properties.
     * Typically, they both should be either null (corresponding to a null selection segment) or non-null.
     * However, it is possible to read one null value and one non-null value in a listener.  To lessen the impact,
     * the caretProperty is updated last, so any listener monitoring the caret property would read the right anchor
     * value.  A listener monitoring the anchorProperty might see erroneous value for the caret, so keep that in mind.
     *
     * @return the caret position property
     */
    public ReadOnlyProperty<TextPos> caretPositionProperty();
    
    /**
     * Anchor position property.  The value can be null.
     * <p>
     * Important note: setting a {@link SelectionSegment} causes an update to both anchor and caret properties.
     * Typically, they both should be either null (corresponding to a null selection segment) or non-null.
     * However, it is possible to read one null value and one non-null value in a listener.  To lessen the impact,
     * the caretProperty is updated last, so any listener monitoring the caret property would read the right anchor
     * value.  A listener monitoring the anchorProperty might see erroneous value for the caret, so keep that in mind.
     *
     * @return the anchor position property
     */
    public ReadOnlyProperty<TextPos> anchorPositionProperty();

    /**
     * Selection segment property.  The value can be null.
     * @return the selection segment property
     */
    public ReadOnlyProperty<SelectionSegment> selectionSegmentProperty();

    /**
     * Returns the current selection segment, or null.
     * @return current selection segment, or null
     */
    public SelectionSegment getSelectionSegment();
}
