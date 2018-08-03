/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control.skin;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.chrono.HijrahChronology;

import com.sun.javafx.scene.control.DatePickerContent;
import com.sun.javafx.scene.control.DatePickerHijrahContent;
import com.sun.javafx.scene.control.behavior.BehaviorBase;
import com.sun.javafx.scene.control.behavior.ComboBoxBaseBehavior;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import com.sun.javafx.scene.control.behavior.DatePickerBehavior;

/**
 * Default skin implementation for the {@link DatePicker} control.
 *
 * @see DatePicker
 * @since 9
 */
public class DatePickerSkin extends ComboBoxPopupControl<LocalDate> {

    /***************************************************************************
     *                                                                         *
     * Private fields                                                          *
     *                                                                         *
     **************************************************************************/

    private final DatePicker datePicker;
    private TextField displayNode;
    private DatePickerContent datePickerContent;

    private final DatePickerBehavior behavior;



    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new DatePickerSkin instance, installing the necessary child
     * nodes into the Control {@link Control#getChildren() children} list, as
     * well as the necessary input mappings for handling key, mouse, etc events.
     *
     * @param control The control that this skin should be installed onto.
     */
    public DatePickerSkin(final DatePicker control) {
        super(control);

        this.datePicker = control;

        // install default input map for the control
        this.behavior = new DatePickerBehavior(control);
//        control.setInputMap(behavior.getInputMap());

        // The "arrow" is actually a rectangular svg icon resembling a calendar.
        // Round the size of the icon to whole integers to get sharp edges.
        arrow.paddingProperty().addListener(new InvalidationListener() {
            // This boolean protects against unwanted recursion.
            private boolean rounding = false;
            @Override public void invalidated(Observable observable) {
                if (!rounding) {
                    Insets padding = arrow.getPadding();
                    Insets rounded = new Insets(Math.round(padding.getTop()), Math.round(padding.getRight()),
                                                Math.round(padding.getBottom()), Math.round(padding.getLeft()));
                    if (!rounded.equals(padding)) {
                        rounding = true;
                        arrow.setPadding(rounded);
                        rounding = false;
                    }
                }
            }
        });

        registerChangeListener(control.chronologyProperty(), e -> {
            updateDisplayNode();
            datePickerContent = null;
            popup = null;
        });
        registerChangeListener(control.converterProperty(), e -> updateDisplayNode());
        registerChangeListener(control.dayCellFactoryProperty(), e -> {
            updateDisplayNode();
            datePickerContent = null;
            popup = null;
        });
        registerChangeListener(control.showWeekNumbersProperty(), e -> {
            if (datePickerContent != null) {
                datePickerContent.updateGrid();
                datePickerContent.updateWeeknumberDateCells();
            }
        });
        registerChangeListener(control.valueProperty(), e -> {
            updateDisplayNode();
            if (datePickerContent != null) {
                LocalDate date = control.getValue();
                datePickerContent.displayedYearMonthProperty().set((date != null) ? YearMonth.from(date) : YearMonth.now());
                datePickerContent.updateValues();
            }
            control.fireEvent(new ActionEvent());
        });
        registerChangeListener(control.showingProperty(), e -> {
            if (control.isShowing()) {
                if (datePickerContent != null) {
                    LocalDate date = control.getValue();
                    datePickerContent.displayedYearMonthProperty().set((date != null) ? YearMonth.from(date) : YearMonth.now());
                    datePickerContent.updateValues();
                }
                show();
            } else {
                hide();
            }
        });

        if (control.isShowing()) {
            show();
        }
    }



    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override public void dispose() {
        super.dispose();

        if (behavior != null) {
            behavior.dispose();
        }
    }

    /** {@inheritDoc} */
    @Override public Node getPopupContent() {
        if (datePickerContent == null) {
            if (datePicker.getChronology() instanceof HijrahChronology) {
                datePickerContent = new DatePickerHijrahContent(datePicker);
            } else {
                datePickerContent = new DatePickerContent(datePicker);
            }
        }

        return datePickerContent;
    }

    /** {@inheritDoc} */
    @Override protected double computeMinWidth(double height,
                                               double topInset, double rightInset,
                                               double bottomInset, double leftInset) {
        return 50;
    }

    /** {@inheritDoc} */
    @Override public void show() {
        super.show();
        datePickerContent.clearFocus();
    }

    /** {@inheritDoc} */
    @Override protected TextField getEditor() {
        // Use getSkinnable() here because this method is called from
        // the super constructor before datePicker is initialized.
        return ((DatePicker)getSkinnable()).getEditor();
    }

    /** {@inheritDoc} */
    @Override protected StringConverter<LocalDate> getConverter() {
        return ((DatePicker)getSkinnable()).getConverter();
    }

    /** {@inheritDoc} */
    @Override public Node getDisplayNode() {
        if (displayNode == null) {
            displayNode = getEditableInputNode();
            displayNode.getStyleClass().add("date-picker-display-node");
            updateDisplayNode();
        }
        displayNode.setEditable(datePicker.isEditable());

        return displayNode;
    }



    /***************************************************************************
     *                                                                         *
     * Private implementation                                                  *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override void focusLost() {
        // do nothing
    }

    /** {@inheritDoc} */
    @Override ComboBoxBaseBehavior getBehavior() {
        return behavior;
    }
}
