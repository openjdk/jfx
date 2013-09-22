/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.javafx.experiments.dukepad.core;

import javafx.application.Platform;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Nice Bindable JavaFX properties for current date and time
 */
public class DateTimeHelper {
    public static final SimpleStringProperty LONG_DATE = new SimpleStringProperty();
    public static final SimpleStringProperty TIME = new SimpleStringProperty();
    public static final SimpleStringProperty LONG_DATE_TIME = new SimpleStringProperty();
    public static final SimpleIntegerProperty HOUR = new SimpleIntegerProperty();
    public static final SimpleIntegerProperty MINUTE = new SimpleIntegerProperty();
    public static final SimpleIntegerProperty SECONDS = new SimpleIntegerProperty();
    public static final SimpleIntegerProperty DAY = new SimpleIntegerProperty();
    public static final SimpleIntegerProperty MONTH = new SimpleIntegerProperty();
    public static final SimpleIntegerProperty YEAR = new SimpleIntegerProperty();
    public static final SimpleObjectProperty<Date> DATE = new SimpleObjectProperty<>(new Date());
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMMM d yyyy");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("h:mm a");
    private static final Calendar calendar = Calendar.getInstance();
    private static final String[] SUFFIXES =
    //    0     1     2     3     4     5     6     7     8     9
       { "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th",
    //    10    11    12    13    14    15    16    17    18    19
         "th", "th", "th", "th", "th", "th", "th", "th", "th", "th",
    //    20    21    22    23    24    25    26    27    28    29
         "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th",
    //    30    31
         "th", "st" };

    static {
        LONG_DATE.bind(new StringBinding() {
            { bind(DAY,MONTH,YEAR); }
            @Override protected String computeValue() {
                DAY.get(); // Side effect required!? :-)
                MONTH.get();
                YEAR.get();
                String dateText = DATE_FORMAT.format(DATE.get());
                dateText = dateText.substring(0,dateText.length()-5) + SUFFIXES[calendar.get(Calendar.DAY_OF_MONTH)] + dateText.substring(dateText.length()-5);
                return dateText;
            }
        });
        TIME.bind(new StringBinding() {
            { bind(HOUR, MINUTE); }
            @Override protected String computeValue() {
                MINUTE.get(); // Side effect required!? :-)
                HOUR.get();
                return TIME_FORMAT.format(DATE.get());
            }
        });
        LONG_DATE_TIME.bind(new StringBinding() {
            { bind(TIME,DATE); }
            @Override protected String computeValue() {
                DATE.get(); // Side effect required!? :-)
                String time = TIME.get();
                time = time.replaceAll("AM","am").replaceAll("PM","pm");
                return time + ", " + LONG_DATE.get();
            }
        });

        Timer timer = new Timer("Clock Updater",true);
        timer.schedule(new TimerTask() {
            @Override public void run() {
                Platform.runLater(new Runnable() {
                    @Override public void run() {
                        Date date = new Date();
                        DATE.set(date);
                        calendar.setTime(date);
                        HOUR.set(calendar.get(Calendar.HOUR));
                        MINUTE.set(calendar.get(Calendar.MINUTE));
                        SECONDS.set(calendar.get(Calendar.SECOND));
                        DAY.set(calendar.get(Calendar.DAY_OF_MONTH));
                        MONTH.set(calendar.get(Calendar.MONTH));
                        YEAR.set(calendar.get(Calendar.YEAR));
                    }
                });
            }
        }, 1000-Calendar.getInstance().get(Calendar.MILLISECOND),1000);
    }
}
