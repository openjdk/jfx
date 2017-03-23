/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.web;


import com.sun.javafx.scene.control.ControlHelper;
import javafx.css.StyleableProperty;

import javafx.scene.control.Control;

import javafx.print.PrinterJob;
import javafx.scene.control.Skin;


/**
 * A control that allows for users to edit text, and apply styling to this text.
 * The underlying data model is HTML, although this is not shown visually to the
 * end-user.
 * @since JavaFX 2.0
 */
public class HTMLEditor extends Control {

    /**
     * Creates a new instance of the HTMLEditor control.
     */
    public HTMLEditor() {
        ((StyleableProperty) ControlHelper.skinClassNameProperty(this)).applyStyle(
            null,
            "javafx.scene.web.HTMLEditorSkin"
        );
        getStyleClass().add("html-editor");
    }

    @Override protected Skin<?> createDefaultSkin() {
        return new HTMLEditorSkin(this);
    }

    /**
     * Returns the HTML content of the editor.
     * @return the HTML content
     */
    public String getHtmlText() {
        return ((HTMLEditorSkin)getSkin()).getHTMLText();
    }

    /**
     * Sets the HTML content of the editor. Note that if the contentEditable
     * property on the {@code <body>} tag of the provided HTML is not set to true, the
     * HTMLEditor will become read-only. You can ensure that the text remains
     * editable by ensuring the body appears as such:
     * <pre>{@code
     * <body contentEditable="true">
     * }</pre>
     *
     * @param htmlText the full HTML markup to put into the editor. This should
     *      include all normal HTML elements, starting with
     *      {@code <html>}, and including a {@code <body>}.
     */
    public void setHtmlText(String htmlText) {
        ((HTMLEditorSkin)getSkin()).setHTMLText(htmlText);
    }

    /**
     * Prints the content of the editor using the given printer job.
     * <p>This method does not modify the state of the job, nor does it call
     * {@link PrinterJob#endJob}, so the job may be safely reused afterwards.
     *
     * @param job printer job used for printing
     * @since JavaFX 8.0
     */
    public void print(PrinterJob job) {
        ((HTMLEditorSkin)getSkin()).print(job);
    }
}
