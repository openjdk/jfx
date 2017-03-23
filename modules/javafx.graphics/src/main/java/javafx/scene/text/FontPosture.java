/*
 * Copyright (c) 2008, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.text;

/**
 * Specifies whether the font is italicized
 * @since JavaFX 2.0
 */
public enum FontPosture {
    /**
     * represents regular.
     */
    REGULAR("", "regular"),
    /**
     * represents italic.
     */
    ITALIC("italic");

    private final String[] names;

    private FontPosture(String... names) {
        this.names = names;
    }

    /**
     * Returns {@code FontPosture} by its name.
     *
     * @param name name of the {@code FontPosture}
     * @return the FontPosture by its name
     */
    public static FontPosture findByName(String name) {
        if (name == null) return null;

        for (FontPosture s : FontPosture.values()) {
            for (String n : s.names) {
                if (n.equalsIgnoreCase(name)) return s;
            }
        }

        return null;
    }
}
