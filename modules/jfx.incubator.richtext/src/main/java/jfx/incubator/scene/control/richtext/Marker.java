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

import java.util.Objects;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import com.sun.jfx.incubator.scene.control.richtext.MarkerHelper;

/**
 * Tracks the text position in a document in the presence of edits.
 *
 * @since 24
 */
public final class Marker implements Comparable<Marker> {
    static {
        MarkerHelper.setAccessor(new MarkerHelper.Accessor() {
            @Override
            public void setMarkerPos(Marker m, TextPos p) {
                m.setTextPos(p);
            }

            @Override
            public Marker createMarker(TextPos p) {
                return new Marker(p);
            }
        });
    }

    private final ReadOnlyObjectWrapper<TextPos> pos;

    private Marker(TextPos pos) {
        Objects.nonNull(pos);
        this.pos = new ReadOnlyObjectWrapper<>(pos);
    }

    @Override
    public String toString() {
        return "Marker{index=" + getIndex() + ", offset=" + getOffset() + "}";
    }

    /**
     * This property tracks the marker's position within the model (value is never null).
     * @return the text position property
     */
    public final ReadOnlyObjectProperty<TextPos> textPosProperty() {
        return pos.getReadOnlyProperty();
    }

    public final TextPos getTextPos() {
        return pos.get();
    }

    private final void setTextPos(TextPos p) {
        pos.set(p);
    }

    @Override
    public final int compareTo(Marker m) {
        return getTextPos().compareTo(m.getTextPos());
    }

    @Override
    public int hashCode() {
        int h = Marker.class.hashCode();
        h = h * 31 + getTextPos().hashCode();
        return h;
    }

    @Override
    public boolean equals(Object x) {
        if (x == this) {
            return true;
        } else if (x instanceof Marker m) {
            return getTextPos().equals(m.getTextPos());
        }
        return false;
    }

    /**
     * Returns the paragraph index.
     * @return the paragraph index
     */
    public final int getIndex() {
        return getTextPos().index();
    }

    /**
     * Returns the text offset within the paragraph.
     * @return the offset value
     */
    public final int getOffset() {
        return getTextPos().offset();
    }
}
