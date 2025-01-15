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
package com.sun.jfx.incubator.scene.control.richtext;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import jfx.incubator.scene.control.richtext.Marker;
import jfx.incubator.scene.control.richtext.TextPos;

/**
 * Manages Markers.
 */
public class Markers {
    private HashMap<TextPos,List<WeakReference<Marker>>> markers;

    public Markers() {
        markers = new HashMap<>();
    }

    public Marker getMarker(TextPos pos) {
        List<WeakReference<Marker>> refs = markers.get(pos);
        if (refs != null) {
            for (int i = 0; i < refs.size(); i++) {
                WeakReference<Marker> ref = refs.get(i);
                Marker m = ref.get();
                if (m != null) {
                    return m;
                }
            }
        }

        Marker m = MarkerHelper.createMarker(pos);
        if (refs == null) {
            refs = new ArrayList<>(2);
        }
        refs.add(new WeakReference<>(m));
        markers.put(pos, refs);
        return m;
    }

    @Override
    public String toString() {
        ArrayList<TextPos> list = new ArrayList<>(markers.keySet());
        Collections.sort(list);

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        boolean sep = false;
        int sz = list.size();
        for (int i = 0; i < sz; i++) {
            TextPos p = list.get(i);
            if (sep) {
                sb.append(',');
            } else {
                sep = true;
            }
            sb.append('{');
            sb.append(p.index());
            sb.append(',');
            sb.append(p.offset());
            sb.append('}');
        }
        sb.append(']');
        return sb.toString();
    }

    // TODO unit test
    // TODO do we need (leading/trailing) bias in TextPos?
    public void update(TextPos start, TextPos end, int charsTop, int linesAdded, int charsBottom) {
//        System.out.println(
//            "start=" + start +
//            " end=" + end +
//            " top=" + charsTop +
//            " lines=" + linesAdded +
//            " btm=" + charsBottom
//        ); // FIX
        if (start.compareTo(end) > 0) {
            TextPos p = start;
            start = end;
            end = p;
        }

        HashMap<TextPos, List<WeakReference<Marker>>> m2 = new HashMap<>(markers.size());

        for (TextPos pos : markers.keySet()) {
            List<WeakReference<Marker>> refs = markers.get(pos);
            TextPos p;
            if (pos.compareTo(start) <= 0) {
                // position before the change: keep unchanged
                p = pos;
            } else if (pos.compareTo(end) < 0) {
                // position inside the change: section removed, move marker to start
                p = start;
            } else {
                // position after the change: shift
                int ix = pos.index();
                int off;
                if (ix == end.index()) {
                    if ((linesAdded == 0) && (start.index() == end.index())) {
                        // all on the same line
                        off = pos.offset() - (end.offset() - start.offset()) + charsTop + charsBottom;
                    } else {
                        off = pos.offset() - end.offset() + charsBottom;
                    }
                } else {
                    // edit happened earlier, offset is unchanged
                    off = pos.offset();
                }

                ix += (linesAdded - end.index() + start.index());
                p = TextPos.ofLeading(ix, off);
            }

            // update markers with the new position, removing gc'ed
            for (int i = refs.size() - 1; i >= 0; i--) {
                Marker m = refs.get(i).get();
                if (m == null) {
                    refs.remove(i);
                } else {
                    MarkerHelper.setMarkerPos(m, p);
                }
            }

            if (refs.size() > 0) {
                m2.put(p, refs);
            }
        }

        markers = m2;
    }
}
