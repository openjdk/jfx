/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

package javafx.incubator.scene.control.rich;

import javafx.incubator.scene.control.rich.model.RichParagraph;

/**
 * Decorates plain text by producing a {@link RichParagraph}.
 */
public interface SyntaxDecorator {
    /**
     * This method allows for attaching this decorator to the model.
     * Called by {@link CodeTextModel#setDecorator(SyntaxDecorator)}.
     * The implementation may add a change listener to the model if required.
     * @param m the model
     */
    public void attach(CodeTextModel m);

    /**
     * This method allows for detaching this decorator from the model and subsequent cleanup.
     * Called by {@link CodeTextModel#setDecorator(SyntaxDecorator)}.
     * @param m the model
     */
    public void detach(CodeTextModel m);

    /**
     * Converts plain text into a rich text paragraph.
     *
     * @param model the model
     * @param index the paragraph index
     * @return the decorated {@link RichParagraph} instance
     */
    public RichParagraph createRichParagraph(CodeTextModel model, int index);
}
