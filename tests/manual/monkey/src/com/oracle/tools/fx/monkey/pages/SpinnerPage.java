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
package com.oracle.tools.fx.monkey.pages;

import java.text.DecimalFormat;
import java.text.ParseException;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.util.StringConverter;

/**
 * Spinner Page
 */
public class SpinnerPage extends TestPaneBase {
    enum Mode {
        DOUBLE,
        INTEGER,
    }

    enum Converter {
        NULL("null"),
        PERCENT("0.##%"),
        QUOTED("\"quoted\""),
        ;
        private final String text;
        Converter(String text) { this.text = text; }
        public String toString() { return text; }
    }

    private final ComboBox<Mode> modeChoice;
    private final ComboBox<Converter> converterChoice;
    private final CheckBox editable;
    private Spinner<Number> control;

    public SpinnerPage() {
        FX.name(this, "SpinnerPage");

        modeChoice = new ComboBox<>();
        FX.name(modeChoice, "modeChoice");
        modeChoice.getItems().addAll(Mode.values());
        modeChoice.setEditable(false);
        modeChoice.getSelectionModel().selectedItemProperty().addListener((s, p, c) -> {
            updateControl();
        });

        converterChoice = new ComboBox<>();
        FX.name(converterChoice, "converterChoice");
        converterChoice.getItems().addAll(Converter.values());
        converterChoice.setEditable(false);
        converterChoice.getSelectionModel().selectedItemProperty().addListener((s, p, c) -> {
            updateControl();
        });

        editable = new CheckBox("editable");
        FX.name(editable, "editable");
        editable.selectedProperty().addListener((s, p, c) -> {
            if (control != null) {
                control.setEditable(c);
            }
        });

        OptionPane p = new OptionPane();
        p.label("Mode:");
        p.option(modeChoice);
        p.option(editable);
        p.label("Converter:");
        p.option(converterChoice);

        setOptions(p);
        updateControl();
        FX.select(modeChoice, Mode.DOUBLE);
    }

    protected void updateControl() {
        Mode m = modeChoice.getSelectionModel().getSelectedItem();
        if (m == null) {
            m = Mode.DOUBLE;
        }

        switch (m) {
        case DOUBLE:
            control = new Spinner(-10.5, 10.5, 0.5);
            break;
        case INTEGER:
            control = new Spinner(-10, 10, 0);
            break;
        }

        Converter c = converterChoice.getSelectionModel().getSelectedItem();
        StringConverter<Number> conv = createConverter(c);
        control.getValueFactory().setConverter(conv);
        control.setEditable(editable.isSelected());

        setContent(control);
    }

    protected StringConverter<Number> createConverter(Converter c) {
        if (c != null) {
            switch (c) {
            case PERCENT:
                return new StringConverter<Number>() {
                    private final DecimalFormat f = new DecimalFormat("0.##%");

                    @Override
                    public String toString(Number v) {
                        return v == null ? "" : f.format(v);
                    }

                    @Override
                    public Number fromString(String s) {
                        if (s == null) {
                            return null;
                        }

                        try {
                            return f.parse(s);
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
            case QUOTED:
                return new StringConverter<Number>() {
                    private final DecimalFormat f = new DecimalFormat("\".##\"");

                    @Override
                    public String toString(Number v) {
                        return v == null ? "" : f.format(v);
                    }

                    @Override
                    public Number fromString(String s) {
                        if (s == null) {
                            return null;
                        }

                        try {
                            return f.parse(s);
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
            }
        }
        return null;
    }
}
