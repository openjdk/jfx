/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.css.media;

import javafx.css.StyleConverter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public final class MediaQueryList extends ArrayList<MediaQuery> {

    public MediaQueryList() {}

    public MediaQueryList(int capacity) {
        super(capacity);
    }

    /**
     * Attempts to determine the result of this media query list without a {@link MediaQueryContext}, and
     * returns {@link TriState#TRUE} if at least one media query is {@code TRUE}, {@link TriState#FALSE}
     * if all media queries are {@code FALSE}, or {@link TriState#UNKNOWN} otherwise.
     *
     * @return {@link TriState#TRUE} if the media query list is always true, {@link TriState#FALSE}
     *         if the media query list is always false, otherwise {@link TriState#UNKNOWN}
     */
    public TriState evaluate() {
        if (isEmpty()) {
            return TriState.TRUE;
        }

        boolean unknown = false;

        for (int i = 0, max = size(); i < max; i++) {
            switch (get(i).evaluate()) {
                case TRUE -> { return TriState.TRUE; }
                case UNKNOWN -> unknown = true;
            }
        }

        return unknown ? TriState.UNKNOWN : TriState.FALSE;
    }

    /**
     * Evaluates whether any of the media queries is {@code true}.
     *
     * @param context the evaluation context
     * @return {@code true} if any of the media queries is {@code true} or if the list is empty,
     *         {@code false} otherwise
     */
    public boolean evaluate(MediaQueryContext context) {
        if (isEmpty()) {
            return true;
        }

        for (int i = 0, max = size(); i < max; i++) {
            MediaQuery query = get(i);
            boolean value = query.evaluate(context);
            context.notifyQueryEvaluated(query, value);

            if (value) {
                return true;
            }
        }

        return false;
    }

    public void writeBinary(DataOutputStream stream, StyleConverter.StringStore stringStore) throws IOException {
        stream.writeInt(size());

        for (MediaQuery query : this) {
            MediaQuerySerializer.writeBinary(query, stream, stringStore);
        }
    }

    public static MediaQueryList readBinary(DataInputStream stream, String[] strings) throws IOException {
        int size = stream.readInt();
        var queries = new MediaQueryList(size);

        for (int i = 0; i < size; i++) {
            queries.add(MediaQuerySerializer.readBinary(stream, strings));
        }

        return queries;
    }
}
