/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.util.Utils;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;

/**
 * Provides access to private methods in StyleAttributeMap.
 */
public class StyleAttributeMapHelper {
    public interface Accessor {
        public StyleAttributeMap filterAttributes(StyleAttributeMap ss, boolean isParagraph);
    }

    static {
        Utils.forceInit(StyleAttributeMap.class);
    }

    private static Accessor accessor;

    public static void setAccessor(Accessor a) {
        if (accessor != null) {
            throw new IllegalStateException();
        }
        accessor = a;
    }

    /**
     * Returns a new StyleAttributeMap instance which contains only the character attributes,
     * or null if no character attributes found.
     * @param ss the style attribute map
     * @return the instance
     */
    public static StyleAttributeMap getCharacterAttrs(StyleAttributeMap ss) {
        return accessor.filterAttributes(ss, false);
    }

    /**
     * Returns a new StyleAttributeMap instance which contains only the paragraph attributes.,
     * or null if no paragraph attributes found.
     * @param ss the style attribute map
     * @return the instance
     */
    public static StyleAttributeMap getParagraphAttrs(StyleAttributeMap ss) {
        return accessor.filterAttributes(ss, true);
    }
}
