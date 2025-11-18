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

package jfx.incubator.scene.control.richtext;

/**
 * Specifies line separator (line ending) characters.
 *
 * @since 26
 */
public enum LineEnding {
    /** Legacy Mac OS line ending, ASCII CR (0x0d). */
    CR,
    /** Windows line ending, sequence of CR/LF (0x0d 0x0a). */
    CRLF,
    /** macOS/Unix line ending, ASCII LF (0x0a). */
    LF;

    private static final LineEnding system = init();

    /**
     * Returns the line ending as a {@code String}.
     * @return the line ending string
     */
    public String getText() {
        return switch(this) {
            case CR -> "\r";
            case CRLF -> "\r\n";
            case LF -> "\n";
        };
    }

    /**
     * Returns the {@code LineEnding} based on the value of system line separator string
     * {@link System#lineSeparator()}.
     * @return the system default line ending
     */
    public static LineEnding system() {
        return system;
    }

    private static LineEnding init() {
        String s = System.lineSeparator();
        if (s != null) {
            return switch (s) {
                case "\r" -> CR;
                case "\r\n" -> CRLF;
                case "\n" -> LF;
                default -> LF;
            };
        }
        return LineEnding.LF;
    }
}
