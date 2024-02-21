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

import java.util.Set;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.incubator.scene.control.rich.model.PlainTextModel;
import javafx.incubator.scene.control.rich.model.RichParagraph;
import javafx.incubator.scene.control.rich.model.StyleAttribute;
import javafx.incubator.scene.control.rich.model.StyleAttrs;

/**
 * Editable plain text model with syntax highlighting for the {@link CodeArea} control.
 */
public class CodeTextModel extends PlainTextModel {
    private SimpleObjectProperty<SyntaxDecorator> decorator;
    private static final Set<StyleAttribute<?>> SUPPORTED = initSupportedAttributes();

    /**
     * The constructor.
     */
    public CodeTextModel() {
    }

    private static Set<StyleAttribute<?>> initSupportedAttributes() {
        return Set.of(
            StyleAttrs.BOLD,
            StyleAttrs.ITALIC,
            StyleAttrs.STRIKE_THROUGH,
            StyleAttrs.TEXT_COLOR,
            StyleAttrs.UNDERLINE
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
                private SyntaxDecorator old;

                @SuppressWarnings("synthetic-access")
                @Override
                protected void invalidated() {
                    if (old != null) {
                        old.detach(CodeTextModel.this);
                    }
                    old = get();
                    if (old != null) {
                        old.attach(CodeTextModel.this);
                    }
                    fireStylingUpdate();
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
}
