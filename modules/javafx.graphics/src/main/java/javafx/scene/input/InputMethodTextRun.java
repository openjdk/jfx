/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.input;

import java.io.Serializable;
import javafx.beans.NamedArg;

/**
 * Represents a single run in which the characters have the same
 * set of highlights in the input method text.
 * <p>
 * Note: this is a conditional feature. See
 * {@link javafx.application.ConditionalFeature#INPUT_METHOD ConditionalFeature.INPUT_METHOD}
 * for more information.
 * @since JavaFX 2.0
 */
public class InputMethodTextRun implements Serializable {

    /**
     * Constructs a single text run of an input method.
     * @param text the text in the text run
     * @param highlight the highlighting of the text
     * @since JavaFX 8.0
     */
    public InputMethodTextRun(@NamedArg("text") String text,
            @NamedArg("highlight") InputMethodHighlight highlight) {
        this.text = text;
        this.highlight = highlight;
    }
    /**
     * The text in this run.
     *
     * @defaultValue empty string
     */
    private final String text;

    /**
     * Gets the text in this run.
     * @return the text in this run
     */
    public final String getText() {
        return text;
    }
    /**
     * The highlight used for displaying this text.
     *
     * @defaultValue null
     */
    private final InputMethodHighlight highlight;

    /**
     * Gets the highlight used for displaying this text.
     * @return the highlight used for displaying this text
     */
    public final InputMethodHighlight getHighlight() {
        return highlight;
    }

    /**
     * Returns a string representation of this {@code InputMethodTextRun} object.
     * @return a string representation of this {@code InputMethodTextRun} object.
     */
    @Override public String toString() {
        return "InputMethodTextRun text [" + getText()
                + "], highlight [" + getHighlight() + "]";
    }
}
