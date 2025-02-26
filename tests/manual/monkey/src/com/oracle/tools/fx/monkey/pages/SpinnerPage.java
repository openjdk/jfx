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
package com.oracle.tools.fx.monkey.pages;

import java.text.DecimalFormat;
import java.text.Format;
import java.text.ParseException;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.scene.AccessibleAttribute;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.util.StringConverter;
import com.oracle.tools.fx.monkey.Loggers;
import com.oracle.tools.fx.monkey.options.BooleanOption;
import com.oracle.tools.fx.monkey.options.DoubleSpinner;
import com.oracle.tools.fx.monkey.options.DurationOption;
import com.oracle.tools.fx.monkey.sheets.ControlPropertySheet;
import com.oracle.tools.fx.monkey.sheets.Options;
import com.oracle.tools.fx.monkey.util.ObjectSelector;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;

/**
 * Spinner Page.
 */
public class SpinnerPage extends TestPaneBase {
    enum Mode {
        INTEGER,
        DOUBLE,
        LIST,
        NULL
    }
    enum Converter {
        NULL,
        NUMBER,
        PERCENT,
        QUOTED
    }

    private final Spinner<Object> control;
    private final SimpleBooleanProperty wrapAround = new SimpleBooleanProperty();
    private final ObjectSelector<Mode> mode;
    private final ObjectSelector<Converter> converter;
    private final BooleanOption wrapOption;
    private final DoubleSpinner minOption;
    private final DoubleSpinner maxOption;
    private final DoubleSpinner stepOption;
    private static final Format NUMBER = new DecimalFormat("0.##");
    private static final Format PERCENT = new DecimalFormat("0.##%");
    private static final Format QUOTED = new DecimalFormat("\".##\"");

    public SpinnerPage() {
        super("SpinnerPage");

        control = new Spinner<Object>() {
            @Override
            public Object queryAccessibleAttribute(AccessibleAttribute a, Object... ps) {
                Object v = super.queryAccessibleAttribute(a, ps);
                Loggers.accessibility.log(a, v);
                return v;
            }
        };
        control.valueProperty().addListener((s,p,c) -> {
            // TODO show in UI?
            System.out.println("Value=" + c);
        });

        wrapOption = new BooleanOption("wrapAround", "wrap around", wrapAround);

        mode = createModeOptions("mode");

        converter = createConverterOptions("converter");

        minOption = new DoubleSpinner("min", Integer.MIN_VALUE, Integer.MAX_VALUE, 0);

        maxOption = new DoubleSpinner("max", Integer.MIN_VALUE, Integer.MAX_VALUE, 100);

        stepOption = new DoubleSpinner("step", Integer.MIN_VALUE, Integer.MAX_VALUE, 1);

        OptionPane op = new OptionPane();
        op.section("Spinner");
        op.option(new BooleanOption("editable", "editable", control.editableProperty()));
        op.option("Initial Delay:", new DurationOption("initialDelay", control.initialDelayProperty()));
        op.option("Prompt Text:", Options.promptText("promptText", true, control.promptTextProperty()));
        op.option("Repeat Delay:", new DurationOption("repeatDelay", control.repeatDelayProperty()));
        op.option("Value Factory:", mode);
        op.separator();
        op.option("Converter:", converter);
        op.option(wrapOption);
        op.option("Min:", minOption);
        op.option("Max:", maxOption);
        op.option("Amount to Step By:", stepOption);
        ControlPropertySheet.appendTo(op, control);

        setContent(control);
        setOptions(op);

        mode.selectFirst();
        converter.selectFirst();
    }

    private StringConverter<Object> createConverter(Converter c, Mode mode) {
        if (c != null) {
            switch (c) {
            case PERCENT:
                switch(mode) {
                case DOUBLE:
                case INTEGER:
                    return new SConverter(mode, PERCENT);
                }
            case QUOTED:
                switch(mode) {
                case DOUBLE:
                case INTEGER:
                    return new SConverter(mode, QUOTED);
                }
            case NUMBER:
                switch(mode) {
                case DOUBLE:
                case INTEGER:
                    return new SConverter(mode, NUMBER);
                }
            }
        }
        return null;
    }

    private ObjectSelector<Mode> createModeOptions(String name) {
        ObjectSelector<Mode> op = new ObjectSelector<>(name, this::handleModeChange);
        op.addChoice("Integer", Mode.INTEGER);
        op.addChoice("Double", Mode.DOUBLE);
        op.addChoice("List", Mode.LIST);
        op.addChoice("<null>", Mode.NULL);
        return op;
    }

    private ObjectSelector<Converter> createConverterOptions(String name) {
        ObjectSelector<Converter> op = new ObjectSelector<>(name, this::handleConverterChange);
        op.addChoice("<null>", Converter.NULL);
        op.addChoice("Number (0.##)", Converter.NUMBER);
        op.addChoice("Percent (0.##%)", Converter.PERCENT);
        op.addChoice("\"Quoted\"", Converter.QUOTED);
        return op;
    }

    private IntegerBinding toIntBinding(ReadOnlyProperty<Double> p) {
        return Bindings.createIntegerBinding(
            () -> {
                Double v = p.getValue();
                return v == null ? 0 : v.intValue();
            },
            p
        );
    }

    private void handleModeChange(Mode mode) {
        if (mode == null) {
            mode = Mode.NULL;
        }

        SpinnerValueFactory f;
        boolean disableConverter;

        switch (mode) {
        case DOUBLE:
            {
                SpinnerValueFactory.DoubleSpinnerValueFactory df =
                    new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 100, 50, 1);
                df.minProperty().bind(minOption.valueProperty());
                df.maxProperty().bind(maxOption.valueProperty());
                df.amountToStepByProperty().bind(stepOption.valueProperty());
                f = df;
                disableConverter = false;
            }
            break;
        case INTEGER:
            {
                SpinnerValueFactory.IntegerSpinnerValueFactory df =
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 50, 1);
                df.minProperty().bind(toIntBinding(minOption.valueProperty()));
                df.maxProperty().bind(toIntBinding(maxOption.valueProperty()));
                df.amountToStepByProperty().bind(toIntBinding(stepOption.valueProperty()));
                f = df;
                disableConverter = false;
            }
            break;
        case LIST:
            f = new SpinnerValueFactory.ListSpinnerValueFactory(FXCollections.observableArrayList(
                null,
                "one",
                "two",
                "three",
                "four",
                "five",
                "six",
                "seven",
                "eight",
                "nine",
                "ten"));
            disableConverter = true;
            break;
        case NULL:
        default:
            f = null;
            disableConverter = true;
            break;
        }

        control.setValueFactory(f);

        if (f == null) {
            wrapOption.setDisable(true);
        } else {
            f.wrapAroundProperty().bind(wrapAround);
            wrapOption.setDisable(false);
        }

        Converter c = converter.getSelectedValue();
        handleConverterChange(c);

        converter.setDisable(disableConverter);
        minOption.setDisable(disableConverter);
        maxOption.setDisable(disableConverter);
        stepOption.setDisable(disableConverter);
    }

    private void handleConverterChange(Converter c) {
        Mode m = mode.getSelectedValue();
        StringConverter<Object> conv = createConverter(c, m);
        if (control.getValueFactory() != null) {
            control.getValueFactory().setConverter(conv);
        }
    }

    // string converter
    static class SConverter extends StringConverter<Object> {
        private final Mode mode;
        private final Format format;

        public SConverter(Mode c, Format f) {
            this.mode = c;
            this.format = f;
        }

        @Override
        public String toString(Object v) {
            return v == null ? "" : format.format(v);
        }

        @Override
        public Object fromString(String s) {
            if (s == null) {
                return null;
            }

            Object v;
            try {
                v = format.parseObject(s);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }

            switch (mode) {
            case DOUBLE:
                if(v instanceof Number n) {
                    return n.doubleValue();
                }
                throw new RuntimeException("Not a double: " + v);
            case INTEGER:
                if(v instanceof Number n) {
                    return n.intValue();
                }
                throw new RuntimeException("Not an integer: " + v);
            case LIST:
            case NULL:
            default:
                return s;
            }
        }
    }
}
