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

package javafx.scene.control;

// editor and converter code in sync with ComboBox 2708:a3e606ef6ead

import java.time.LocalDate;
import java.time.DateTimeException;
import java.time.chrono.Chronology;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DecimalStyle;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableProperty;
import javafx.util.Callback;
import javafx.util.StringConverter;

import com.sun.javafx.css.converters.BooleanConverter;
import com.sun.javafx.scene.control.skin.DatePickerSkin;
import com.sun.javafx.scene.control.skin.resources.ControlResources;



/**
 * The DatePicker control allows the user to enter a date as text or
 * to select a date from a calendar popup. The calendar is based on
 * either the standard ISO-8601 chronology or any of the other
 * chronology classes defined in the java.time.chrono package.
 *
 * <p>The {@link #valueProperty() value} property represents the
 * currently selected {@link java.time.LocalDate}.  An initial date can
 * be set via the {@link #DatePicker(java.time.LocalDate) constructor}
 * or by calling {@link #setValue(java.time.LocalDate) setValue()}.  The
 * default value is null.
 *
 * <pre><code>
 * final DatePicker datePicker = new DatePicker();
 * datePicker.setOnAction(new EventHandler() {
 *     public void handle(Event t) {
 *         LocalDate date = datePicker.getValue();
 *         System.err.println("Selected date: " + date);
 *     }
 * });
 * </code></pre>
 *
 * The {@link #chronologyProperty() chronology} property specifies a
 * calendar system to be used for parsing, displaying, and choosing
 * dates.
 * The {@link #valueProperty() value} property is always defined in
 * the ISO calendar system, however, so applications based on a
 * different chronology may use the conversion methods provided in the
 * {@link java.time.chrono.Chronology} API to get or set the
 * corresponding {@link java.time.chrono.ChronoLocalDate} value. For
 * example:
 *
 * <pre><code>
 * LocalDate isoDate = datePicker.getValue();
 * ChronoLocalDate chronoDate =
 *     ((isoDate != null) ? datePicker.getChronology().date(isoDate) : null);
 * System.err.println("Selected date: " + chronoDate);
 * </code></pre>
 *
 *
 * @since JavaFX 8.0
 */
public class DatePicker extends ComboBoxBase<LocalDate> {

    /**
     * Creates a default DatePicker instance with a <code>null</code> date value set.
     */
    public DatePicker() {
        this(null);
    }

    /**
     * Creates a DatePicker instance and sets the
     * {@link #valueProperty() value} to the given date.
     *
     * @param localDate to be set as the currently selected date in the DatePicker. Can be null.
     */
    public DatePicker(LocalDate localDate) {
        setValue(localDate);
        getStyleClass().add(DEFAULT_STYLE_CLASS);
        setEditable(true);
    }


    /***************************************************************************
     *                                                                         *
     * Properties                                                                 *
     *                                                                         *
     **************************************************************************/


    /**
     * A custom cell factory can be provided to customize individual
     * day cells in the DatePicker popup. Refer to {@link DateCell}
     * and {@link Cell} for more information on cell factories.
     * Example:
     *
     * <pre><code>
     * final Callback&lt;DatePicker, DateCell&gt; dayCellFactory = new Callback&lt;DatePicker, DateCell&gt;() {
     *     public DateCell call(final DatePicker datePicker) {
     *         return new DateCell() {
     *             &#064;Override public void updateItem(LocalDate item, boolean empty) {
     *                 super.updateItem(item, empty);
     *
     *                 if (MonthDay.from(item).equals(MonthDay.of(9, 25))) {
     *                     setTooltip(new Tooltip("Happy Birthday!"));
     *                     setStyle("-fx-background-color: #ff4444;");
     *                 }
     *                 if (item.equals(LocalDate.now().plusDays(1))) {
     *                     // Tomorrow is too soon.
     *                     setDisable(true);
     *                 }
     *             }
     *         };
     *     }
     * };
     * datePicker.setDayCellFactory(dayCellFactory);
     * </code></pre>
     *
     * @defaultValue null
     */
    private ObjectProperty<Callback<DatePicker, DateCell>> dayCellFactory;
    public final void setDayCellFactory(Callback<DatePicker, DateCell> value) {
        dayCellFactoryProperty().set(value);
    }
    public final Callback<DatePicker, DateCell> getDayCellFactory() {
        return (dayCellFactory != null) ? dayCellFactory.get() : null;
    }
    public final ObjectProperty<Callback<DatePicker, DateCell>> dayCellFactoryProperty() {
        if (dayCellFactory == null) {
            dayCellFactory = new SimpleObjectProperty<Callback<DatePicker, DateCell>>(this, "dayCellFactory");
        }
        return dayCellFactory;
    }



    /**
     * The calendar system used for parsing, displaying, and choosing
     * dates in the DatePicker control.
     *
     * <p>The default value is returned from a call to
     * {@code Chronology.ofLocale(Locale.getDefault(Locale.Category.FORMAT))}.
     * The default is usually {@link java.time.chrono.IsoChronology} unless
     * provided explicitly in the {@link java.util.Locale} by use of a
     * Locale calendar extension.
     *
     * Setting the value to <code>null</code> will restore the default
     * chronology.
     */
    public final ObjectProperty<Chronology> chronologyProperty() {
        return chronology;
    }
    private ObjectProperty<Chronology> chronology =
        new SimpleObjectProperty<Chronology>(this, "chronology", null);
    public final Chronology getChronology() {
        Chronology chrono = chronology.get();
        if (chrono == null) {
            try {
                chrono = Chronology.ofLocale(Locale.getDefault(Locale.Category.FORMAT));
            } catch (Exception ex) {
                System.err.println(ex);
            }
            if (chrono == null) {
                chrono = IsoChronology.INSTANCE;
            }
            //System.err.println(chrono);
        }
        return chrono;
    }
    public final void setChronology(Chronology value) {
        chronology.setValue(value);
    }


    /**
     * Whether the DatePicker popup should display a column showing
     * week numbers.
     *
     * <p>The default value is false unless otherwise defined in a
     * resource bundle for the current locale.
     *
     * <p>This property may be toggled by the end user by using a
     * context menu in the DatePicker popup, so it is recommended that
     * applications save and restore the value between sessions.
     */
    public final BooleanProperty showWeekNumbersProperty() {
        if (showWeekNumbers == null) {
            String country = Locale.getDefault(Locale.Category.FORMAT).getCountry();
            boolean localizedDefault =
                (!country.isEmpty() &&
                 ControlResources.getNonTranslatableString("DatePicker.showWeekNumbers").contains(country));
            showWeekNumbers = new StyleableBooleanProperty(localizedDefault) {
                @Override public CssMetaData<DatePicker,Boolean> getCssMetaData() {
                    return StyleableProperties.SHOW_WEEK_NUMBERS;
                }

                @Override public Object getBean() {
                    return DatePicker.this;
                }

                @Override public String getName() {
                    return "showWeekNumbers";
                }
            };
        }
        return showWeekNumbers;
    }
    private BooleanProperty showWeekNumbers;
    public final void setShowWeekNumbers(boolean value) {
        showWeekNumbersProperty().setValue(value);
    }
    public final boolean isShowWeekNumbers() {
        return showWeekNumbersProperty().getValue();
    }


    // --- string converter
    /**
     * Converts the input text to an object of type LocalDate and vice
     * versa.
     *
     * <p>If not set by the application, the DatePicker skin class will
     * set a converter based on a {@link java.time.DateTimeFormatter}
     * for the current {@link java.util.Locale} and
     * {@link #chronologyProperty() chronology}. This formatter is
     * then used to parse and display the current date value.
     *
     * Setting the value to <code>null</code> will restore the default
     * converter.
     *
     * <p>Example using an explicit formatter:
     * <pre><code>
     * datePicker.setConverter(new StringConverter&lt;LocalDate&gt;() {
     *     String pattern = "yyyy-MM-dd";
     *     DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(pattern);
     *
     *     {
     *         datePicker.setPromptText(pattern.toLowerCase());
     *     }
     *
     *     &#064;Override public String toString(LocalDate date) {
     *         if (date != null) {
     *             return dateFormatter.format(date);
     *         } else {
     *             return "";
     *         }
     *     }
     *
     *     &#064;Override public LocalDate fromString(String string) {
     *         if (string != null && !string.isEmpty()) {
     *             return LocalDate.parse(string, dateFormatter);
     *         } else {
     *             return null;
     *         }
     *     }
     * });
     * </code></pre>
     * <p>Example that wraps the default formatter and catches parse exceptions:
     * <pre><code>
     *   final StringConverter&lt;LocalDate&gt; defaultConverter = datePicker.getConverter();
     *   datePicker.setConverter(new StringConverter&lt;LocalDate&gt;() {
     *       &#064;Override public String toString(LocalDate value) {
     *           return defaultConverter.toString(value);
     *       }
     *
     *       &#064;Override public LocalDate fromString(String text) {
     *           try {
     *               return defaultConverter.fromString(text);
     *           } catch (DateTimeParseException ex) {
     *               System.err.println("HelloDatePicker: "+ex.getMessage());
     *               throw ex;
     *           }
     *       }
     *   });
     * </code></pre>
     *
     * @see javafx.scene.control.ComboBox#converterProperty
     */
    public final ObjectProperty<StringConverter<LocalDate>> converterProperty() { return converter; }
    private ObjectProperty<StringConverter<LocalDate>> converter =
            new SimpleObjectProperty<StringConverter<LocalDate>>(this, "converter", null);
    public final void setConverter(StringConverter<LocalDate> value) { converterProperty().set(value); }
    public final StringConverter<LocalDate> getConverter() {
        StringConverter<LocalDate> converter = converterProperty().get();
        if (converter != null) {
            return converter;
        } else {
            return defaultConverter;
        }
    }

    private StringConverter<LocalDate> defaultConverter = new StringConverter<LocalDate>() {
        @Override public String toString(LocalDate value) {
            if (value != null) {
                Locale locale = Locale.getDefault(Locale.Category.FORMAT);
                Chronology chrono = getChronology();
                ChronoLocalDate cDate;
                try {
                    cDate = chrono.date(value);
                } catch (DateTimeException ex) {
                    System.err.println(ex);
                    chrono = IsoChronology.INSTANCE;
                    cDate = value;
                }
                DateTimeFormatter dateFormatter =
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
                                     .withLocale(locale)
                                     .withChronology(chrono)
                                     .withDecimalStyle(DecimalStyle.of(locale));

                String pattern =
                    DateTimeFormatterBuilder.getLocalizedDateTimePattern(FormatStyle.SHORT,
                                                                         null, chrono, locale);

                if (pattern.contains("yy") && !pattern.contains("yyy")) {
                    // Modify pattern to show four-digit year, including leading zeros.
                    String newPattern = pattern.replace("yy", "yyyy");
                    //System.err.println("Fixing pattern ("+forParsing+"): "+pattern+" -> "+newPattern);
                    dateFormatter = DateTimeFormatter.ofPattern(newPattern)
                                                     .withDecimalStyle(DecimalStyle.of(locale));
                }

                return dateFormatter.format(cDate);
            } else {
                return "";
            }
        }

        @Override public LocalDate fromString(String text) {
            if (text != null && !text.isEmpty()) {
                Locale locale = Locale.getDefault(Locale.Category.FORMAT);
                Chronology chrono = getChronology();

                String pattern =
                    DateTimeFormatterBuilder.getLocalizedDateTimePattern(FormatStyle.SHORT,
                                                                         null, chrono, locale);
                DateTimeFormatter df =
                    new DateTimeFormatterBuilder().parseLenient()
                                                  .appendPattern(pattern)
                                                  .toFormatter()
                                                  .withChronology(chrono)
                                                  .withDecimalStyle(DecimalStyle.of(locale));
                TemporalAccessor temporal = df.parse(text);
                ChronoLocalDate cDate = chrono.date(temporal);
                return LocalDate.from(cDate);
            }
            return null;
        }
    };


    // --- Editor
    /**
     * The editor for the DatePicker.
     *
     * @see javafx.scene.control.ComboBox#editorProperty
     */
    private ReadOnlyObjectWrapper<TextField> editor;
    public final TextField getEditor() {
        return editorProperty().get();
    }
    public final ReadOnlyObjectProperty<TextField> editorProperty() {
        if (editor == null) {
            editor = new ReadOnlyObjectWrapper<TextField>(this, "editor");
            editor.set(new TextField());
        }
        return editor.getReadOnlyProperty();
    }

    /** {@inheritDoc} */
    @Override protected Skin<?> createDefaultSkin() {
        return new DatePickerSkin(this);
    }


    /***************************************************************************
     *                                                                         *
     * Stylesheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "date-picker";

     /**
      * @treatAsPrivate implementation detail
      */
    private static class StyleableProperties {
        private static final String country =
            Locale.getDefault(Locale.Category.FORMAT).getCountry();
        private static final CssMetaData<DatePicker, Boolean> SHOW_WEEK_NUMBERS =
              new CssMetaData<DatePicker, Boolean>("-fx-show-week-numbers",
                   BooleanConverter.getInstance(),
                   (!country.isEmpty() &&
                    ControlResources.getNonTranslatableString("DatePicker.showWeekNumbers").contains(country))) {
            @Override public boolean isSettable(DatePicker n) {
                return n.showWeekNumbers == null || !n.showWeekNumbers.isBound();
            }

            @Override public StyleableProperty<Boolean> getStyleableProperty(DatePicker n) {
                return (StyleableProperty)n.showWeekNumbersProperty();
            }
        };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;

        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables =
                new ArrayList<CssMetaData<? extends Styleable, ?>>(Control.getClassCssMetaData());
            Collections.addAll(styleables,
                SHOW_WEEK_NUMBERS
            );
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    /**
     * @return The CssMetaData associated with this class, which may include the
     * CssMetaData of its super classes.
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return getClassCssMetaData();
    }
}
