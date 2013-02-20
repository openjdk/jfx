/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.fxml;

import com.sun.javafx.beans.IDProperty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.beans.DefaultProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

@IDProperty("id")
@DefaultProperty("children")
public class Widget {
    private StringProperty id = new SimpleStringProperty();
    private StringProperty name = new SimpleStringProperty();
    private IntegerProperty number = new SimpleIntegerProperty();
    private ObservableList<Widget> children = FXCollections.observableArrayList();
    private ObservableMap<String, Object> properties = FXCollections.observableHashMap();
    private BooleanProperty enabledProperty = new SimpleBooleanProperty(true);
    private ArrayList<String> styles = new ArrayList<String>();
    private ArrayList<String> values = new ArrayList<String>();
    private float[] ratios = new float[]{};
    private String[] names = new String[]{};

    public static final String ALIGNMENT_KEY = "alignment";
    public static final int TEN = 10;

    public Widget() {
        this(null);
    }

    public Widget(String name) {
        setName(name);
    }

    public String getId() {
        return id.get();
    }

    public void setId(String value) {
        id.set(value);
    }

    public StringProperty idProperty() {
        return id;
    }

    public String getName() {
        return name.get();
    }

    public void setName(String value) {
        name.set(value);
    }

    public StringProperty nameProperty() {
        return name;
    }

    public int getNumber() {
        return number.get();
    }

    public void setNumber(int value) {
        number.set(value);
    }

    public IntegerProperty numberProperty() {
        return number;
    }

    public ObservableList<Widget> getChildren() {
        return children;
    }

    public ObservableMap<String, Object> getProperties() {
        return properties;
    }

    public boolean isEnabled() {
        return enabledProperty.get();
    }

    public void setEnabled(boolean value) {
        enabledProperty.set(value);
    }

    public BooleanProperty enabledProperty() {
        return enabledProperty;
    }

    public List<String> getStyles() {
        return styles;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        if (values == null) {
            throw new IllegalArgumentException();
        }

        this.values = new ArrayList<String>();
        this.values.addAll(values);
    }

    public float[] getRatios() {
        return Arrays.copyOf(ratios, ratios.length);
    }

    public void setRatios(float[] ratios) {
        this.ratios = Arrays.copyOf(ratios, ratios.length);
    }
    
    public String[] getNames() {
        return Arrays.copyOf(names, names.length);
    }

    public void setNames(String[] names) {
        this.names = Arrays.copyOf(names, names.length);
    }


    public static Alignment getAlignment(Widget widget) {
        return (Alignment)widget.getProperties().get(ALIGNMENT_KEY);
    }

    public static void setAlignment(Widget widget, Alignment alignment) {
        widget.getProperties().put(ALIGNMENT_KEY, alignment);
    }
}
