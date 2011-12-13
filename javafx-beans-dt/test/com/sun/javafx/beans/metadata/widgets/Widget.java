/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.beans.metadata.widgets;

import com.sun.javafx.beans.metadata.Property;
import java.util.Date;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

/**
 *
 * @author Richard
 */
public class Widget {
    private StringProperty name = new SimpleStringProperty(this, "name", "");
    private BooleanProperty colorized = new SimpleBooleanProperty(this, "colorized", true);
    private ReadOnlyIntegerWrapper widgetValue = new ReadOnlyIntegerWrapper(this, "widgetValue", 100);
    private ObjectProperty<javafx.util.Callback<Date,String>> dateConverter =
            new SimpleObjectProperty<javafx.util.Callback<Date,String>>(this, "dateConverter");

    private ObjectProperty<EventHandler<ActionEvent>> onAction =
            new SimpleObjectProperty<EventHandler<ActionEvent>>(this, "onAction");

    public final String getName() { return name.get(); }
    public final void setName(String value) { name.set(value); }
    public final StringProperty nameProperty() { return name; }

    @Property(category="Special", displayName="Psychodelic", shortDescription="Colorize me!")
    public final boolean isColorized() { return colorized.get(); }
    public final void setColorized(boolean value) { colorized.set(value); }
    public final BooleanProperty colorizedProperty() { return colorized; }

    public final int getWidgetValue() { return widgetValue.get(); }
    public final ReadOnlyIntegerProperty widgetValueProperty() { return widgetValue.getReadOnlyProperty(); }
    public void increment() {
        widgetValue.set(widgetValue.get() + 1);
    }

    public final EventHandler<ActionEvent> getOnAction() { return onAction.get(); }
    public final void setOnAction(EventHandler<ActionEvent> value) { onAction.set(value); }
    public final ObjectProperty<EventHandler<ActionEvent>> onActionProperty() { return onAction; }

    public final javafx.util.Callback<Date,String> getDateConverter() { return dateConverter.get(); }
    public final void setDateConverter(javafx.util.Callback<Date,String> value) { dateConverter.set(value); }
    public final ObjectProperty<javafx.util.Callback<Date,String>> dateConverterProperty() {
        return dateConverter;
    }
}
