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

package jfx.incubator.scene.control.richtext.model;

import java.util.Set;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import jfx.incubator.scene.control.richtext.SyntaxDecorator;
import jfx.incubator.scene.control.richtext.TextPos;

/**
 * Editable plain text model with optional syntax highlighting for use with the
 * {@link jfx.incubator.scene.control.richtext.CodeArea CodeArea} control.
 * <p>
 * This model supports custom content storage mechanism via {@link BasicTextModel.Content}.  By default,
 * the model provides an in-memory storage via its {@link BasicTextModel.InMemoryContent} implementation.
 *
 * @since 24
 */
public class CodeTextModel extends BasicTextModel {
    private SimpleObjectProperty<SyntaxDecorator> decorator;
    private static final Set<StyleAttribute<?>> SUPPORTED = initSupportedAttributes();

    /**
     * Constructs the CodeTextModel with an in-memory content.
     */
    public CodeTextModel() {
    }

    /**
     * Constructs the CodeTextModel with the specified content.
     * @param c the content
     */
    public CodeTextModel(BasicTextModel.Content c) {
        super(c);
    }

    // only a subset of attributes are supported
    private static Set<StyleAttribute<?>> initSupportedAttributes() {
        return Set.of(
            StyleAttributeMap.BOLD,
            StyleAttributeMap.ITALIC,
            StyleAttributeMap.STRIKE_THROUGH,
            StyleAttributeMap.TEXT_COLOR,
            StyleAttributeMap.UNDERLINE
        );
    }

    @Override
    protected Set<StyleAttribute<?>> getSupportedAttributes() {
        return SUPPORTED;
    }

    @Override
    public final RichParagraph getParagraph(int index) {
        SyntaxDecorator d = getDecorator();
        if (d == null) {
            return super.getParagraph(index);
        } else {
            return d.createRichParagraph(this, index);
        }
    }

    /**
     * Syntax decorator applies styling to the plain text stored in the model.
     * @return the syntax decorator value (may be null)
     */
    public final ObjectProperty<SyntaxDecorator> decoratorProperty() {
        if (decorator == null) {
            decorator = new SimpleObjectProperty<>() {
                @Override
                protected void invalidated() {
                    TextPos end = getDocumentEnd();
                    SyntaxDecorator d = get();
                    if (d != null) {
                        d.handleChange(CodeTextModel.this, TextPos.ZERO, end, 0, 0, 0);
                    }
                    fireStyleChangeEvent(TextPos.ZERO, end);
                }
            };
        }
        return decorator;
    }

    public final SyntaxDecorator getDecorator() {
        return decorator == null ? null : decorator.get();
    }

    public final void setDecorator(SyntaxDecorator d) {
        decoratorProperty().set(d);
    }

    @Override
    public void fireChangeEvent(TextPos start, TextPos end, int charsTop, int linesAdded, int charsBottom) {
        SyntaxDecorator d = getDecorator();
        if (d != null) {
            d.handleChange(this, start, end, charsTop, linesAdded, charsBottom);
        }
        super.fireChangeEvent(start, end, charsTop, linesAdded, charsBottom);
    }
}
