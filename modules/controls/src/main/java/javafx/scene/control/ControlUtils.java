/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control;

import com.sun.javafx.scene.control.skin.Utils;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.util.List;

class ControlUtils {
    private ControlUtils() { }

    public static void scrollToIndex(final Control control, int index) {
        Utils.executeOnceWhenPropertyIsNonNull(control.skinProperty(), (Skin<?> skin) -> {
            Event.fireEvent(control, new ScrollToEvent<>(control, control, ScrollToEvent.scrollToTopIndex(), index));
        });
    }

    public static void scrollToColumn(final Control control, final TableColumnBase<?, ?> column) {
        Utils.executeOnceWhenPropertyIsNonNull(control.skinProperty(), (Skin<?> skin) -> {
            control.fireEvent(new ScrollToEvent<TableColumnBase<?, ?>>(control, control, ScrollToEvent.scrollToColumn(), column));
        });
    }

    static void requestFocusOnControlOnlyIfCurrentFocusOwnerIsChild(Control c) {
        Scene scene = c.getScene();
        final Node focusOwner = scene == null ? null : scene.getFocusOwner();
        if (focusOwner == null) {
            c.requestFocus();
        } else if (! c.equals(focusOwner)) {
            Parent p = focusOwner.getParent();
            while (p != null) {
                if (c.equals(p)) {
                    c.requestFocus();
                    break;
                }
                p = p.getParent();
            }
        }
    }

    static <T> ListChangeListener.Change<T> buildClearAndSelectChange(
            ObservableList<T> list, List<T> removed, int retainedRow) {
        return new ListChangeListener.Change<T>(list) {
            private final int[] EMPTY_PERM = new int[0];

            private final int removedSize = removed.size();

            private final List<T> firstRemovedRange;
            private final List<T> secondRemovedRange;

            private boolean invalid = true;
            private boolean atFirstRange = true;

            private int from = -1;

            {
                int midIndex = retainedRow >= removedSize ? removedSize :
                               retainedRow < 0 ? 0 :
                               retainedRow;
                firstRemovedRange = removed.subList(0, midIndex);
                secondRemovedRange = removed.subList(midIndex, removedSize);
            }

            @Override public int getFrom() {
                checkState();
                return from;
            }

            @Override public int getTo() {
                return getFrom();
            }

            @Override public List<T> getRemoved() {
                checkState();
                return atFirstRange ? firstRemovedRange : secondRemovedRange;
            }

            @Override public int getRemovedSize() {
                return atFirstRange ? firstRemovedRange.size() : secondRemovedRange.size();
            }

            @Override protected int[] getPermutation() {
                checkState();
                return EMPTY_PERM;
            }

            @Override public boolean next() {
                if (invalid && atFirstRange) {
                    invalid = false;

                    // point 'from' to the first position, relative to
                    // the underlying selectedCells index.
                    from = 0;
                    return true;
                }

                if (atFirstRange) {
                    atFirstRange = false;

                    // point 'from' to the second position, relative to
                    // the underlying selectedCells index.
                    from = secondRemovedRange.isEmpty() ? 0 : 1;
                    return true;
                }

                return false;
            }

            @Override public void reset() {
                invalid = true;
                atFirstRange = true;
            }

            private void checkState() {
                if (invalid) {
                    throw new IllegalStateException("Invalid Change state: next() must be called before inspecting the Change.");
                }
            }
        };
    }
}
