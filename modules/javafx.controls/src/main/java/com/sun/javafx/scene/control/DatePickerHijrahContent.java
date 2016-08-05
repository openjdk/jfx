/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control;

import java.time.LocalDate;
import java.time.DateTimeException;
import java.time.YearMonth;
import java.time.format.DecimalStyle;
import java.time.chrono.Chronology;
import java.time.chrono.HijrahChronology;
import java.time.chrono.HijrahDate;
import java.time.chrono.IsoChronology;
import java.util.Locale;

import static java.time.temporal.ChronoField.*;

import javafx.geometry.Pos;
import javafx.scene.control.DatePicker;
import javafx.scene.control.DateCell;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;

/**
 * Extends DatePickerContent to add a secondary calendar, allowing the
 * ISO and Islamic calendars to be displayed simultaneously.  The
 * secondary month, day, and year values are displayed as a colored
 * overlay, offset from the primary values.
 *
 * If the current DatePicker Chronology is HijrahChronology, then sets
 * the content's primary Chronology to be IsoChronology, and sets
 * HijrahChronology as the secondary.
 */
public class DatePickerHijrahContent extends DatePickerContent {
    private Label hijrahMonthYearLabel;

    public DatePickerHijrahContent(final DatePicker datePicker) {
        super(datePicker);
    }

    /**
     * The primary chronology for display. This is overridden because
     * DatePickerHijrahContent uses ISO as primary and Hijrah as a
     * secondary chronology.
     */
    @Override protected Chronology getPrimaryChronology() {
        return IsoChronology.INSTANCE;
    }

    @Override protected BorderPane createMonthYearPane() {
        BorderPane monthYearPane = super.createMonthYearPane();

        hijrahMonthYearLabel = new Label();
        hijrahMonthYearLabel.getStyleClass().add("secondary-label");
        monthYearPane.setBottom(hijrahMonthYearLabel);
        BorderPane.setAlignment(hijrahMonthYearLabel, Pos.CENTER);

        return monthYearPane;
    }


    @Override protected void updateMonthYearPane() {
        super.updateMonthYearPane();

        Locale locale = getLocale();
        HijrahChronology chrono = HijrahChronology.INSTANCE;
        long firstMonth = -1;
        long firstYear = -1;
        String firstMonthStr = null;
        String firstYearStr = null;
        String hijrahStr = null;
        YearMonth displayedYearMonth = displayedYearMonthProperty().get();

        for (DateCell dayCell : dayCells) {
            LocalDate date = dayCellDate(dayCell);

            // Display Hijra month names only for current ISO month.
            if (!displayedYearMonth.equals(YearMonth.from(date))) {
                continue;
            }

            try {
                HijrahDate cDate = chrono.date(date);
                long month = cDate.getLong(MONTH_OF_YEAR);
                long year = cDate.getLong(YEAR);

                if (hijrahStr == null || month != firstMonth) {
                    String monthStr = monthFormatter.withLocale(locale)
                                                    .withChronology(chrono)
                                                    .withDecimalStyle(DecimalStyle.of(locale))
                                                    .format(cDate);
                    String yearStr = yearFormatter.withLocale(locale)
                                                    .withChronology(chrono)
                                                    .withDecimalStyle(DecimalStyle.of(locale))
                                                    .format(cDate);
                    if (hijrahStr == null) {
                        firstMonth = month;
                        firstYear = year;
                        firstMonthStr = monthStr;
                        firstYearStr = yearStr;
                        hijrahStr = firstMonthStr + " " + firstYearStr;
                    } else {
                        if (year > firstYear) {
                            hijrahStr = firstMonthStr + " " + firstYearStr + " - " + monthStr + " " + yearStr;
                        } else {
                            hijrahStr = firstMonthStr + " - " + monthStr + " " + firstYearStr;
                        }
                        break;
                    }
                }
            } catch (DateTimeException ex) {
                // Date is out of range, ignore.

                //System.err.println(dayCellDate(dayCell) + " " + ex);
            }
        }

        hijrahMonthYearLabel.setText(hijrahStr);
    }

    @Override protected void createDayCells() {
        super.createDayCells();

        for (DateCell dayCell : dayCells) {
            Text secondaryText = new Text();
            dayCell.getProperties().put("DateCell.secondaryText", secondaryText);
        }
    }

    @Override public void updateDayCells() {
        super.updateDayCells();

        Locale locale = getLocale();
        HijrahChronology chrono = HijrahChronology.INSTANCE;

        int majorityMonth = -1;
        int visibleDaysInMajorityMonth = -1;
        int curMonth = -1;
        int visibleDaysInCurMonth = 0;

        for (DateCell dayCell : dayCells) {
            Text secondaryText = (Text)dayCell.getProperties().get("DateCell.secondaryText");
            dayCell.getStyleClass().add("hijrah-day-cell");
            secondaryText.getStyleClass().setAll("text", "secondary-text");

            try {
                HijrahDate cDate = chrono.date(dayCellDate(dayCell));
//             long month = cDate.getLong(MONTH_OF_YEAR);

                String hijrahStr =
                    dayCellFormatter.withLocale(locale)
                                    .withChronology(chrono)
                                    .withDecimalStyle(DecimalStyle.of(locale))
                                    .format(cDate);

                secondaryText.setText(hijrahStr);
                dayCell.requestLayout();
            } catch (DateTimeException ex) {
                // Date is out of range.
                // System.err.println(dayCellDate(dayCell) + " " + ex);
                secondaryText.setText(" ");
                dayCell.setDisable(true);
            }

//             if (month == curMonth) {
//                 visibleDaysInCurMonth++;
//             } else {
//                 curMonth = month;
//                 visibleDaysInCurMonth = 1;
//             }
//             if (visibleDaysInCurMonth > visibleDaysInMajorityMonth) {
//                 majorityMonth = month;
//                 visibleDaysInMajorityMonth = visibleDaysInCurMonth;
//             }
        }

//         boolean seenMajorityMonth = false;
//         for (DateCell dayCell : dayCells) {
//             HijrahDate cDate = chrono.date(dayCellDate(dayCell));
//             int month = cDate.get(MONTH_OF_YEAR);
//             Text secondaryText = (Text)dayCell.getProperties().get("DateCell.secondaryText");

//             if (month == majorityMonth) {
//                 seenMajorityMonth = true;
//                 secondaryText.getStyleClass().add("current-month");
//             } else {
//                 secondaryText.getStyleClass().add(seenMajorityMonth ? "next-month" : "previous-month");
//             }
//         }
    }
}
