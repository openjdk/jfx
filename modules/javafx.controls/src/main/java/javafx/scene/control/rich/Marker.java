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
package javafx.scene.control.rich;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.rich.impl.Markers;

/**
 * Tracks text position in the text document in the presence of edits.
 * 
 * TODO part of the model?  pass model to the constructor?
 */
public class Marker implements Comparable<Marker> {
    private final ReadOnlyObjectWrapper<TextPos> pos;
    @Deprecated // FIX debugging, remove later
    private static int sequence;
    @Deprecated // FIX debugging, remove later
    private int seq;
    
    private Marker(TextPos pos) {
        this.pos = new ReadOnlyObjectWrapper<>(pos);
        this.seq = sequence++;
    }
    
    public static Marker create(Markers owner, TextPos pos) {
        if (owner == null) {
            throw new IllegalArgumentException("must specify the owner");
        }

        return new Marker(pos);
    }

    public String toString() {
        return "Marker{" + getIndex() + "," + getOffset() + "}";
    }

    public ReadOnlyObjectProperty<TextPos> textPosProperty() {
        return pos.getReadOnlyProperty();
    }

    @Override
    public int compareTo(Marker m) {
        return getTextPos().compareTo(m.getTextPos());
    }
    
    @Override
    public int hashCode() {
        int h = Marker.class.hashCode();
        h = h * 31 + getTextPos().hashCode();
        return h;
    }
    
    public TextPos getTextPos() {
        return pos.get();
    }

    public int getIndex() {
        return getTextPos().index();
    }

    public int getOffset() {
        return getTextPos().offset();
    }

    // TODO should not be public, must be called only from Markers.update()
    public void set(TextPos p) {
        //System.out.println("Marker.set seq=" + seq + " pos=" + p);
        pos.set(p);
    }
}
