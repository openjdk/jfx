/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control;

/**
 * Defines the behavior of a labeled Control when the space for rendering the
 * text is smaller than the space needed to render the entire string.
 * @since JavaFX 2.0
 */
public enum OverrunStyle {
    /**
     * Any text which exceeds the bounds of the label will be clipped.
     */
    CLIP,
    /**
     * If the text of the label exceeds the bounds of the label, then the last
     * three characters which can be displayed will be "...". If the label is
     * too short to even display that, then only as many "." as possible will
     * be shown.
     */
    ELLIPSIS,
    /**
     * Same as ELLIPSIS, but first removes any partial words at the label
     * boundary and then applies the ellipsis. This ensures that the last
     * characters displayed prior to the ellipsis are part of a full word.
     * Where a full word cannot be displayed, this acts like ELLIPSIS and
     * displays as many characters as possible.
     */
    WORD_ELLIPSIS,
    /**
     * Trims out the center of the string being displayed and replaces the
     * middle three characters with "...". The first and last characters of
     * the string are always displayed in the label, unless the label becomes
     * so short that it cannot display anything other than the ellipsis.
     */
    CENTER_ELLIPSIS,
    /**
     * Same as CENTER_ELLIPSIS but ensures that the "..." occurs between full
     * words. If the label becomes so short that it is not possible to trim
     * any additional words, then partial words are displayed and this behaves
     * the same as CENTER_ELLIPSIS
     */
    CENTER_WORD_ELLIPSIS,
    /**
     * Same as ELLIPSIS but puts the "..." at the beginning of the text instead
     * of at the end
     */
    LEADING_ELLIPSIS,
    /**
     * Same as WORD_ELLIPSIS but puts the "..." at the beginning of the text
     * instead of at the end
     */
    LEADING_WORD_ELLIPSIS
    /**
     * Indicates that the entire text should be available, but should scroll
     * as if in a ticker tape
     */
    //TICKER
}

