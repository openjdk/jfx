/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control.rich;

import java.util.List;
import java.util.function.Consumer;
import javafx.scene.control.rich.model.RichParagraph;
import javafx.scene.control.rich.model.StyleAttrs;
import javafx.scene.control.rich.model.StyledSegment;
import com.sun.javafx.util.Utils;

/**
 * Provides access to some internal methods in RichParagraph.
 */
public class RichParagraphHelper {
    public interface Accessor {
        public List<Consumer<TextCell>> getHighlights(RichParagraph p);

        public List<StyledSegment> getSegments(RichParagraph p);

        public StyleAttrs getParagraphAttributes(RichParagraph p);
    }

    static {
        Utils.forceInit(RichParagraph.class);
    }

    private static Accessor accessor;

    public static void setAccessor(Accessor a) {
        if (accessor != null) {
            throw new IllegalStateException();
        }
        accessor = a;
    }

    public static List<Consumer<TextCell>> getHighlights(RichParagraph p) {
        return accessor.getHighlights(p);
    }

    public static List<StyledSegment> getSegments(RichParagraph p) {
        return accessor.getSegments(p);
    }

    public static StyleAttrs getParagraphAttributes(RichParagraph p) {
        return accessor.getParagraphAttributes(p);
    }
}