/*
 * Copyright (c) 2025, 2026, Oracle and/or its affiliates. All rights reserved.
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

/**
 * CSS media rules are @-rules that contain media queries. A media query tests the "external"
 * configuration of a {@link Scene}, and is independent of its scene graph content.
 */
public final class MediaRule {

    private final MediaQueryList queries;
    private final MediaRule parent;

    public MediaRule(MediaQueryList queries, MediaRule parent) {
        this.queries = queries;
        this.parent = parent;
    }

    /**
     * Returns the list of media queries.
     *
     * @return the list of media queries
     */
    public MediaQueryList getQueries() {
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
     * Attempts to determine the result of this media rule without a {@link MediaQueryContext}, and returns
     * {@link TriState#TRUE} if at least one media query is {@code TRUE}, {@link TriState#FALSE} if all media
     * queries are {@code FALSE}, or {@link TriState#UNKNOWN} otherwise.
     *
     * @return {@link TriState#TRUE} if the rule always matches, {@link TriState#FALSE}
     *         if the rule never matches, otherwise {@link TriState#UNKNOWN}
     */
    public TriState evaluate() {
        boolean parentUnknown = false;

        if (parent != null) {
            switch (parent.evaluate()) {
                case FALSE -> { return TriState.FALSE; }
                case UNKNOWN -> parentUnknown = true;
            }
        }

        return switch (queries.evaluate()) {
            case TRUE -> parentUnknown ? TriState.UNKNOWN : TriState.TRUE;
            case FALSE -> TriState.FALSE;
            case UNKNOWN -> TriState.UNKNOWN;
        };
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

        return queries.evaluate(context);
    }

    public void writeBinary(DataOutputStream stream, StyleConverter.StringStore stringStore) throws IOException {
        queries.writeBinary(stream, stringStore);

        stream.writeBoolean(parent != null);

        if (parent != null) {
            parent.writeBinary(stream, stringStore);
        }
    }

    public static MediaRule readBinary(DataInputStream stream, String[] strings) throws IOException {
        MediaQueryList queries = MediaQueryList.readBinary(stream, strings);

        boolean hasParent = stream.readBoolean();
        MediaRule parentRule = hasParent ? readBinary(stream, strings) : null;

        return new MediaRule(queries, parentRule);
    }
}
