/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package javafx.scene.input;

/**
 * Represents a single run in which the characters have the same 
 * set of highlights in the input method text.
 * <p>
 * Note: this is a conditional feature. See
 * {@link javafx.application.ConditionalFeature#INPUT_METHOD ConditionalFeature.INPUT_METHOD}
 * for more information.
 */
public class InputMethodTextRun {
    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static InputMethodTextRun impl_inputMethodTextRun(String text,
            InputMethodHighlight highlight) {
        InputMethodTextRun run = new InputMethodTextRun();
        run.text = text;
        run.highlight = highlight;
        return run;
    }
    /**
     * The text in this run.
     *
     * @defaultvalue empty string
     */
    private String text = new String();

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
     * @defaultvalue null
     */
    private InputMethodHighlight highlight;

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
