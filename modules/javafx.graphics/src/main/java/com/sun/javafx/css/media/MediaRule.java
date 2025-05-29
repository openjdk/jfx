/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.Scene;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * CSS media rules are @-rules that contain media queries. A media query tests the "external"
 * configuration of a {@link Scene}, and is independent of its scene graph content.
 */
public final class MediaRule {

    private final List<MediaQuery> queries;
    private final MediaRule parent;

    public MediaRule(List<MediaQuery> queries, MediaRule parent) {
        this.queries = List.copyOf(queries);
        this.parent = parent;
    }

    /**
     * Returns the unmodifiable list of media queries.
     *
     * @return the unmodifiable list of media queries
     */
    public List<MediaQuery> getQueries() {
        return queries;
    }

    /**
     * Returns the parent media rule if this media rule is nested.
     *
     * @return the parent {@code MediaRule}, or {@code null}
     */
    public MediaRule getParent() {
        return parent;
    }

    /**
     * Evaluates whether any of the media queries is {@code true}.
     *
     * @param context the evaluation context
     * @return {@code true} if any of the media queries is {@code true} or if the list is empty,
     *         {@code false} otherwise
     */
    public boolean evaluate(MediaQueryContext context) {
        if (parent != null && !parent.evaluate(context)) {
            return false;
        }

        if (queries.isEmpty()) {
            return true;
        }

        for (int i = 0, max = queries.size(); i < max; i++) {
            if (queries.get(i).evaluate(context)) {
                return true;
            }
        }

        return false;
    }

    public void writeBinary(DataOutputStream stream, StyleConverter.StringStore stringStore) throws IOException {
        stream.writeInt(queries.size());

        for (MediaQuery query : queries) {
            MediaQuerySerializer.writeBinary(query, stream, stringStore);
        }

        stream.writeBoolean(parent != null);

        if (parent != null) {
            parent.writeBinary(stream, stringStore);
        }
    }

    public static MediaRule readBinary(DataInputStream stream, String[] strings) throws IOException {
        int size = stream.readInt();
        var queries = new MediaQuery[size];

        for (int i = 0; i < size; i++) {
            queries[i] = MediaQuerySerializer.readBinary(stream, strings);
        }

        boolean hasParent = stream.readBoolean();
        MediaRule parentRule = hasParent ? readBinary(stream, strings) : null;

        return new MediaRule(List.of(queries), parentRule);
    }
}
