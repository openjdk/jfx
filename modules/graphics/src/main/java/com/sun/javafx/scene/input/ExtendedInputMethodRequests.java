/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.input;

import javafx.scene.input.InputMethodRequests;

/**
 * ExtendedInputMethodRequests extends the {@link InputMethodRequests} interface
 * to provide more requests that a text editing node could handle.
 * The text editing node is not required to implement an extended interface,
 * but it could implement it to support input methods when embedded into the
 * JFXPanel
 *
 * @since JavaFX 8.0
 */
public interface ExtendedInputMethodRequests extends InputMethodRequests {


    /**
     * Returns the offset of the insert position in the committed text contained
     * in the text editing node.
     *
     * @return the offset of the insert position
     */
    int getInsertPositionOffset();

    /**
     * Gets the entire text contained in the text editing node except the uncommitted
     * text. The uncommitted text is ignored for index calculations.
     *
     * @param begin the index of the first character
     * @param end the index of the character following the last character
     * @return the committed text
     */
    String getCommittedText(int begin, int end);

    /**
     * Gets the length of the entire text contained in the text editing node except
     * the uncommitted text.
     *
     * @return length of the text except the uncommitted text
     */
    int getCommittedTextLength();
}
