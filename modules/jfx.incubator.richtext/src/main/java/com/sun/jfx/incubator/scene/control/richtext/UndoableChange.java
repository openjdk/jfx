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

package com.sun.jfx.incubator.scene.control.richtext;

import java.io.IOException;
import jfx.incubator.scene.control.richtext.StyleResolver;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.StyledSegment;
import jfx.incubator.scene.control.richtext.model.StyledTextModel;

/**
 * Represents an undo-able and redo-able change.
 */
public class UndoableChange {
    private final StyledTextModel model;
    private final TextPos start;
    private final StyledSegment[] undo;
    private StyledSegment[] redo;
    private final TextPos endBefore;
    private TextPos endAfter;
    private UndoableChange prev;
    private UndoableChange next;

    private UndoableChange(StyledTextModel model, TextPos start, TextPos end, StyledSegment[] undo) {
        this.model = model;
        this.start = start;
        this.endBefore = end;
        this.undo = undo;
    }

    /**
     * Creates an UndoableChange object.
     * This method might return null if an error happened during creation, for example, if the model
     * could not export the affected area as a sequence of StyledSegments.
     * <p>
     * TODO perhaps it should throw an exception which will be handled by the control, in order to provide
     * user feedback.
     * @param model source model
     * @param start start text position
     * @param end end text position
     * @throws IOException if the save point cannot be created
     */
    public static UndoableChange create(StyledTextModel model, TextPos start, TextPos end) {
        try {
            SegmentStyledOutput out = new SegmentStyledOutput(128);
            model.export(start, end, out);
            StyledSegment[] ss = out.getSegments();
            return new UndoableChange(model, start, end, ss);
        } catch (IOException e) {
            // TODO log
            return null;
        }
    }

    public static UndoableChange createHead() {
        return new UndoableChange(null, null, null, null);
    }

    @Override
    public String toString() {
        return
            "UndoableChange{" +
            "start=" + start +
            ", endBefore=" + endBefore +
            ", endAfter=" + endAfter;
    }

    public void setEndAfter(TextPos p) {
        endAfter = p;
    }

    public void undo(StyleResolver resolver) throws IOException {
        if (redo == null) {
            // create redo
            SegmentStyledOutput out = new SegmentStyledOutput(128);
            model.export(start, endAfter, out);
            redo = out.getSegments();
        }

        // undo
        SegmentStyledInput in = new SegmentStyledInput(undo);
        model.replace(resolver, start, endAfter, in, false);
    }

    public void redo(StyleResolver resolver) throws IOException {
        SegmentStyledInput in = new SegmentStyledInput(redo);
        model.replace(resolver, start, endBefore, in, false);
    }

    public UndoableChange getPrev() {
        return prev;
    }

    public void setPrev(UndoableChange ch) {
        prev = ch;
    }

    public UndoableChange getNext() {
        return next;
    }

    public void setNext(UndoableChange ch) {
        next = ch;
    }

    public TextPos[] getSelectionBefore() {
        return new TextPos[] {
            start,
            endBefore
        };
    }

    public TextPos[] getSelectionAfter() {
        return new TextPos[] {
            start,
            endAfter
        };
    }
}
