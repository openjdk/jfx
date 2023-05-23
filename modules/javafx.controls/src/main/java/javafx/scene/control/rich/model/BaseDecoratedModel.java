/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
package javafx.scene.control.rich.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.rich.TextCell;
import javafx.scene.control.rich.TextPos;

/**
 * A StyledTextModel that applies a decorator to a virtualized plain text data source.
 */
public abstract class BaseDecoratedModel extends StyledTextModel {
    private final SimpleObjectProperty<Decorator> decorator = new SimpleObjectProperty<>();
    private final SimpleBooleanProperty editable = new SimpleBooleanProperty(true);

    public BaseDecoratedModel() {
        decorator.addListener((x) -> fireStylingUpdate());
    }

    public final TextCell createTextCell(int index) {
        String text = getPlainText(index);
        Decorator d = getDecorator();
        if (d == null) {
            TextCell c = new TextCell(index);
            c.addSegment(text);
            return c;
        } else {
            return d.createTextCell(index, text);
        }
    }

    public Decorator getDecorator() {
        return decorator.get();
    }

    public void setDecorator(Decorator d) {
        decorator.set(d);
    }

    public ObjectProperty<Decorator> decoratorProperty() {
        return decorator;
    }

    @Override
    public boolean isEditable() {
        return editable.get();
    }

    public void setEditable(boolean on) {
        editable.set(on);
    }

    public BooleanProperty editableProperty() {
        return editable;
    }

    // TODO move to base class?
    private void fireStylingUpdate() {
        TextPos end = getEndTextPos();
        fireStyleChangeEvent(TextPos.ZERO, end);
    }
}
