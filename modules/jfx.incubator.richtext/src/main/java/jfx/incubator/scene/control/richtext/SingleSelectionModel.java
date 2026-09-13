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

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.value.ChangeListener;
import jfx.incubator.scene.control.richtext.model.StyledTextModel;

/**
 * This {@link SelectionModel} supports a single selection segment.
 *
 * @since 24
 */
public final class SingleSelectionModel implements SelectionModel {
    private final ReadOnlyObjectWrapper<SelectionSegment> segment = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyObjectWrapper<TextPos> anchorPosition = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyObjectWrapper<TextPos> caretPosition = new ReadOnlyObjectWrapper<>();
    private final ChangeListener<TextPos> anchorListener;
    private final ChangeListener<TextPos> caretListener;
    private Marker anchorMarker;
    private Marker caretMarker;
    private StyledTextModel model;

    /** The constructor. */
    public SingleSelectionModel() {
        anchorListener = (src, old, pos) -> {
            anchorPosition.set(pos);
            segment.set(new SelectionSegment(pos, caretPosition.get()));
        };
        caretListener = (src, old, pos) -> {
            caretPosition.set(pos);
            segment.set(new SelectionSegment(anchorPosition.get(), pos));
        };
    }

    @Override
    public void clear() {
        setSelectionSegment(null, null);
    }

    @Override
    public void setSelection(StyledTextModel model, TextPos anchor, TextPos caret) {
        // non-null values are enforced by clamp()
        anchor = model.clamp(anchor);
        caret = model.clamp(caret);
        SelectionSegment sel = new SelectionSegment(anchor, caret);
        setSelectionSegment(model, sel);
    }

    @Override
    public void extendSelection(StyledTextModel model, TextPos pos) {
        // reset selection if model is different
        if (isFlippingModel(model)) {
            setSelection(model, pos, pos);
            return;
        }

        pos = model.clamp(pos);
        SelectionSegment sel = getSelection();
        TextPos a = sel == null ? null : sel.getAnchor();
        if (a == null) {
            a = pos;
        } else {
            if (pos.compareTo(sel.getMin()) < 0) {
                // extend before
                a = sel.getMax();
            } else if (pos.compareTo(sel.getMax()) > 0) {
                // extend after
                a = sel.getMin();
            } else {
                // extend from anchor to pos
                a = sel.getAnchor();
            }
        }
        setSelection(model, a, pos);
    }

    private boolean isFlippingModel(StyledTextModel m) {
        if (model == null) {
            return false;
        } else if (m == null) {
            return false;
        }
        return m != model;
    }

    @Override
    public ReadOnlyProperty<TextPos> anchorPositionProperty() {
        return anchorPosition.getReadOnlyProperty();
    }

    @Override
    public ReadOnlyProperty<TextPos> caretPositionProperty() {
        return caretPosition;
    }

    private void setSelectionSegment(StyledTextModel model, SelectionSegment sel) {
        this.model = model;

        if (anchorMarker != null) {
            anchorMarker.textPosProperty().removeListener(anchorListener);
            anchorMarker = null;
        }

        if (caretMarker != null) {
            caretMarker.textPosProperty().removeListener(caretListener);
            caretMarker = null;
        }

        // since caretPosition, anchorPosition, and selectionSegment are separate properties,
        // there is a possibility that one is null and another is not (for example, in a listener).
        // this code guarantees a specific order of updates:
        // 1. anchor
        // 2. caret
        // 3. selection segment
        if (sel == null) {
            anchorPosition.set(null);
            caretPosition.set(null);
        } else {
            TextPos p = sel.getAnchor();
            anchorMarker = model.getMarker(p);
            anchorPosition.set(p);
            anchorMarker.textPosProperty().addListener(anchorListener);

            p = sel.getCaret();
            caretMarker = model.getMarker(p);
            caretPosition.set(p);
            caretMarker.textPosProperty().addListener(caretListener);
        }

        segment.set(sel);
    }

    @Override
    public ReadOnlyProperty<SelectionSegment> selectionProperty() {
        return segment.getReadOnlyProperty();
    }

    @Override
    public SelectionSegment getSelection() {
        return segment.get();
    }
}
