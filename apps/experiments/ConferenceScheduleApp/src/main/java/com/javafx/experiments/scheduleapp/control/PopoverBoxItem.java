/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.javafx.experiments.scheduleapp.control;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.layout.Region;

// TODO really should just be a Cell, then it has Graphic etc for free
public class PopoverBoxItem<T> extends Region {
    private final ObjectProperty<T> item = new SimpleObjectProperty<T>(this, "item");
    public final T getItem() { return item.get(); }
    public final void setItem(T value) { item.set(value); }
    public final ObjectProperty<T> itemProperty() { return item; }

    private final StringProperty name = new SimpleStringProperty(this, "name");
    public final String getName() { return name.get(); }
    public final void setName(String value) { name.set(value); }
    public final StringProperty nameProperty() { return name; }

    private final ObjectProperty<Node> graphic = new SimpleObjectProperty<Node>(this, "graphic") {
        Node old;
        @Override protected void invalidated() {
            Node n = get();
            if (old != n) {
                if (old != null && n != null) {
                    int index = getChildren().indexOf(old);
                    getChildren().set(index, n);
                } else if (old != null) {
                    getChildren().remove(old);
                } else {
                    getChildren().add(n);
                }
                old = n;
                requestLayout();
            }
        }
    };
    public final Node getGraphic() { return graphic.get(); }
    public final void setGraphic(Node value) { graphic.set(value); }
    public final ObjectProperty<Node> graphicProperty() { return graphic; }

    public PopoverBoxItem() {
        this("", null);
    }

    public PopoverBoxItem(T item) {
        this("", item);
    }

    public PopoverBoxItem(String name, T item) {
        this.name.set(name);
        this.item.set(item);
        getStyleClass().setAll("popover-box-cell");
    }
}
