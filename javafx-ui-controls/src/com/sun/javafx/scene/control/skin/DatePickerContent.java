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

package com.sun.javafx.scene.control.skin;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DecimalStyle;
import java.time.chrono.Chronology;
import java.time.chrono.ChronoLocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.ValueRange;
import java.time.temporal.WeekFields;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static java.time.temporal.ChronoField.*;
import static java.time.temporal.ChronoUnit.*;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.DateCell;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;

import com.sun.javafx.scene.control.skin.resources.ControlResources;
import com.sun.javafx.scene.traversal.Direction;

/**
 * The full content for the DatePicker popup. This class could
 * probably be used more or less as-is with an embeddable type of date
 * picker that doesn't use a popup.
 */
public class DatePickerContent extends VBox {
    protected DatePicker datePicker;
    private Button backMonthButton;
    private Label monthLabel;
    private Label yearLabel;
    protected GridPane gridPane;

    private int daysPerWeek;
    private List<DateCell> dayNameCells = new ArrayList<DateCell>();
    private List<DateCell> weekNumberCells = new ArrayList<DateCell>();
    protected List<DateCell> dayCells = new ArrayList<DateCell>();
    private LocalDate[] dayCellDates;
    private DateCell lastFocusedDayCell = null;

    final DateTimeFormatter monthFormatter =
        DateTimeFormatter.ofPattern("MMMM");

    final DateTimeFormatter monthFormatterSO =
            DateTimeFormatter.ofPattern("LLLL"); // Standalone month name

    final DateTimeFormatter yearFormatter =
        DateTimeFormatter.ofPattern("y");

    final DateTimeFormatter yearWithEraFormatter =
        DateTimeFormatter.ofPattern("GGGGy"); // For Japanese. What to use for others??

    final DateTimeFormatter weekNumberFormatter =
        DateTimeFormatter.ofPattern("w");

    final DateTimeFormatter weekDayNameFormatter =
            DateTimeFormatter.ofPattern("ccc"); // Standalone day name

    final DateTimeFormatter dayCellFormatter =
        DateTimeFormatter.ofPattern("d");

    final ContextMenu contextMenu = new ContextMenu();

    static String getString(String key) {
        return ControlResources.getString("DatePicker."+key);
    }

    DatePickerContent(final DatePicker datePicker) {
        this.datePicker = datePicker;

        getStyleClass().add("date-picker-popup");

        daysPerWeek = getDaysPerWeek();

        contextMenu.getItems().addAll(
            new MenuItem(getString("contextMenu.showToday")) {{
                setOnAction(new EventHandler<ActionEvent>() {
                    @Override public void handle(ActionEvent t) {
                        displayedYearMonth.set(YearMonth.now());
                    }
                });
            }},
            new SeparatorMenuItem(),
            new CheckMenuItem(getString("contextMenu.showWeekNumbers")) {{
                selectedProperty().bindBidirectional(datePicker.showWeekNumbersProperty());
            }}
        );

        setOnContextMenuRequested(new EventHandler<ContextMenuEvent>() {
            @Override public void handle(ContextMenuEvent me) {
                contextMenu.show(DatePickerContent.this, me.getScreenX(), me.getScreenY());
                me.consume();
            }
        });

        {
            LocalDate date = datePicker.getValue();
            displayedYearMonth.set((date != null) ? YearMonth.from(date) : YearMonth.now());
        }

        displayedYearMonth.addListener(new ChangeListener<YearMonth>() {
            @Override public void changed(ObservableValue<? extends YearMonth> observable,
                                          YearMonth oldValue, YearMonth newValue) {
                updateValues();
            }
        });


        getChildren().add(createMonthYearPane());

        gridPane = new GridPane();
        gridPane.setFocusTraversable(true);
        gridPane.getStyleClass().add("calendar-grid");
        gridPane.setVgap(-1);
        gridPane.setHgap(-1);

        gridPane.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean hasFocus) {
                if (hasFocus) {
                    if (lastFocusedDayCell != null) {
                        Platform.runLater(new Runnable() {
                            @Override public void run() {
                                lastFocusedDayCell.requestFocus();
                            }
                        });
                    } else {
                        clearFocus();
                    }
                }
            }
        });

        // get the weekday labels starting with the weekday that is the
        // first-day-of-the-week according to the locale in the
        // displayed LocalDate
        for (int i = 0; i < daysPerWeek; i++) {
            DateCell cell = new DateCell();
            cell.getStyleClass().add("day-name-cell");
            dayNameCells.add(cell);
        }

        // Week number column
        for (int i = 0; i < 6; i++) {
            DateCell cell = new DateCell();
            cell.getStyleClass().add("week-number-cell");
            weekNumberCells.add(cell);
        }

        createDayCells();
        updateGrid();
        getChildren().add(gridPane);

        refresh();

        // RT-30511: This enables traversal (not sure why Scene doesn't handle this),
        // plus it prevents key events from reaching the popup's owner.
        addEventHandler(KeyEvent.ANY, new EventHandler<KeyEvent>() {
            @Override public void handle(KeyEvent e) {
                Node node = getScene().getFocusOwner();
                if (e.getEventType() == KeyEvent.KEY_PRESSED) {
                    switch (e.getCode()) {
                      case TAB:
                          node.impl_traverse(e.isShiftDown() ? Direction.PREVIOUS : Direction.NEXT);
                          e.consume();
                          break;

                      case UP:
                          if (!e.isAltDown()) {
                              node.impl_traverse(Direction.UP);
                              e.consume();
                          }
                          break;

                      case DOWN:
                          if (!e.isAltDown()) {
                              node.impl_traverse(Direction.DOWN);
                              e.consume();
                          }
                          break;

                      case LEFT:
                          node.impl_traverse(Direction.LEFT);
                          e.consume();
                          break;

                      case RIGHT:
                          node.impl_traverse(Direction.RIGHT);
                          e.consume();
                          break;
                    }
                    if (e.isConsumed() && node instanceof DateCell) {
                        lastFocusedDayCell = (DateCell)node;
                    }
                }

                // our little secret... borrowed from Scene.java
                if (!e.isConsumed() && e.getCode() == KeyCode.DIGIT8 &&
                     e.getEventType() == KeyEvent.KEY_PRESSED && e.isControlDown() && e.isShiftDown()) {
                    try {
                        Class scenicview = Class.forName("com.javafx.experiments.scenicview.ScenicView");
                        Class params[] = new Class[] { getScene().getClass() };
                        java.lang.reflect.Method method = scenicview.getDeclaredMethod("show", params);
                        method.invoke(null, getScene());
                    } catch (Exception ex) {
                        //System.out.println("exception instantiating ScenicView:"+ex);
                    }
                }

                // Consume all key events except those that control
                // showing the popup.
                switch (e.getCode()) {
                  case ESCAPE:
                  case F4:
                  case F10:
                  case UP:
                  case DOWN:
                      break;

                  default:
                    e.consume();
                }
            }
        });
    }

    private ObjectProperty<YearMonth> displayedYearMonth =
        new SimpleObjectProperty<YearMonth>(this, "displayedYearMonth");

    ObjectProperty<YearMonth> displayedYearMonthProperty() {
        return displayedYearMonth;
    }


    protected BorderPane createMonthYearPane() {
        BorderPane monthYearPane = new BorderPane();
        monthYearPane.getStyleClass().add("month-year-pane");

        // Month spinner

        HBox monthSpinner = new HBox();
        monthSpinner.getStyleClass().add("spinner");

        backMonthButton = new Button();
        backMonthButton.getStyleClass().add("left-button");

        Button forwardMonthButton = new Button();
        forwardMonthButton.getStyleClass().add("right-button");

        StackPane leftMonthArrow = new StackPane();
        leftMonthArrow.getStyleClass().add("left-arrow");
        leftMonthArrow.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        backMonthButton.setGraphic(leftMonthArrow);

        StackPane rightMonthArrow = new StackPane();
        rightMonthArrow.getStyleClass().add("right-arrow");
        rightMonthArrow.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        forwardMonthButton.setGraphic(rightMonthArrow);


        backMonthButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent t) {
                displayedYearMonth.set(displayedYearMonth.get().minusMonths(1));
            }
        });

        monthLabel = new Label();
        monthLabel.getStyleClass().add("spinner-label");

        forwardMonthButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent t) {
                displayedYearMonth.set(displayedYearMonth.get().plusMonths(1));
            }
        });

        monthSpinner.getChildren().addAll(backMonthButton, monthLabel, forwardMonthButton);
        monthYearPane.setLeft(monthSpinner);

        // Year spinner

        HBox yearSpinner = new HBox();
        yearSpinner.getStyleClass().add("spinner");

        Button backYearButton = new Button();
        backYearButton.getStyleClass().add("left-button");

        Button forwardYearButton = new Button();
        forwardYearButton.getStyleClass().add("right-button");

        StackPane leftYearArrow = new StackPane();
        leftYearArrow.getStyleClass().add("left-arrow");
        leftYearArrow.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        backYearButton.setGraphic(leftYearArrow);

        StackPane rightYearArrow = new StackPane();
        rightYearArrow.getStyleClass().add("right-arrow");
        rightYearArrow.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        forwardYearButton.setGraphic(rightYearArrow);


        backYearButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent t) {
                displayedYearMonth.set(displayedYearMonth.get().minusYears(1));
            }
        });

        yearLabel = new Label();
        yearLabel.getStyleClass().add("spinner-label");

        forwardYearButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent t) {
                displayedYearMonth.set(displayedYearMonth.get().plusYears(1));
            }
        });


        yearSpinner.getChildren().addAll(backYearButton, yearLabel, forwardYearButton);
yearSpinner.setFillHeight(false);
        monthYearPane.setRight(yearSpinner);

        return monthYearPane;
    }

    private void refresh() {
        updateMonthLabelWidth();
        updateDayNameCells();
        updateValues();
    }

    void updateValues() {
        // Note: Preserve this order, as DatePickerHijrahContent needs
        // updateDayCells before updateMonthYearPane().
        updateWeeknumberDateCells();
        updateDayCells();
        updateMonthYearPane();
    }

    void updateGrid() {
        gridPane.getColumnConstraints().clear();
        gridPane.getChildren().clear();

        int nCols = daysPerWeek + (datePicker.isShowWeekNumbers() ? 1 : 0);

        ColumnConstraints columnConstraints = new ColumnConstraints();
        columnConstraints.setPercentWidth(100); // Treated as weight
        for (int i = 0; i < nCols; i++) {
            gridPane.getColumnConstraints().add(columnConstraints);
        }

        for (int i = 0; i < daysPerWeek; i++) {
            gridPane.add(dayNameCells.get(i), i + nCols - daysPerWeek, 1);  // col, row
        }

        // Week number column
        if (datePicker.isShowWeekNumbers()) {
            for (int i = 0; i < 6; i++) {
                gridPane.add(weekNumberCells.get(i), 0, i + 2);  // col, row
            }
        }

        // setup: 6 rows of daysPerWeek (which is the maximum number of cells required in the worst case layout)
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < daysPerWeek; col++) {
                gridPane.add(dayCells.get(row*daysPerWeek+col), col + nCols - daysPerWeek, row + 2);
            }
        }
    }

    void updateDayNameCells() {
        // first day of week, 1 = monday, 7 = sunday
        int firstDayOfWeek = WeekFields.of(getLocale()).getFirstDayOfWeek().getValue();

        // july 13th 2009 is a Monday, so a firstDayOfWeek=1 must come out of the 13th
        LocalDate date = LocalDate.of(2009, 7, 12 + firstDayOfWeek);
        for (int i = 0; i < daysPerWeek; i++) {
            String name = weekDayNameFormatter.withLocale(getLocale()).format(date.plus(i, DAYS));
            dayNameCells.get(i).setText(titleCaseWord(name));
        }
    }

    void updateWeeknumberDateCells() {
        if (datePicker.isShowWeekNumbers()) {
            final Locale locale = getLocale();
            final int maxWeeksPerMonth = 6; // TODO: Get this from chronology?

            LocalDate firstOfMonth = displayedYearMonth.get().atDay(1);
            for (int i = 0; i < maxWeeksPerMonth; i++) {
                LocalDate date = firstOfMonth.plus(i, WEEKS);
                // Use a formatter to ensure correct localization,
                // such as when Thai numerals are required.
                String cellText =
                    weekNumberFormatter.withLocale(locale)
                                       .withDecimalStyle(DecimalStyle.of(locale))
                                       .format(date);
                weekNumberCells.get(i).setText(cellText);
            }
        }
    }

    void updateDayCells() {
        Locale locale = getLocale();
        Chronology chrono = getChronology();
        int firstOfMonthIdx = determineFirstOfMonthDayOfWeek();
        YearMonth curMonth = displayedYearMonth.get();
        YearMonth prevMonth = curMonth.minusMonths(1);
        YearMonth nextMonth = curMonth.plusMonths(1);
        int daysInCurMonth = determineDaysInMonth(curMonth);
        int daysInPrevMonth = determineDaysInMonth(prevMonth);
        int daysInNextMonth = determineDaysInMonth(nextMonth);

        for (int i = 0; i < 6 * daysPerWeek; i++) {
            DateCell dayCell = dayCells.get(i);
            dayCell.getStyleClass().setAll("cell", "day-cell");
            dayCell.setDisable(false);
            dayCell.setStyle(null);
            dayCell.setGraphic(null);
            dayCell.setTooltip(null);

            YearMonth month = curMonth;
            int day = i - firstOfMonthIdx + 1;
            //int index = firstOfMonthIdx + i - 1;
            if (i < firstOfMonthIdx) {
                month = prevMonth;
                day = i + daysInPrevMonth - firstOfMonthIdx + 1;
                dayCell.getStyleClass().add("previous-month");
            } else if (i >= firstOfMonthIdx + daysInCurMonth) {
                month = nextMonth;
                day = i - daysInCurMonth - firstOfMonthIdx + 1;
                dayCell.getStyleClass().add("next-month");
            }
            LocalDate date = month.atDay(day);
            dayCellDates[i] = date;
            ChronoLocalDate cDate = toChrono(date);

            if (isToday(date)) {
                dayCell.getStyleClass().add("today");
            }

            if (date.equals(datePicker.getValue())) {
                dayCell.getStyleClass().add("selected");
            }

            String cellText =
                dayCellFormatter.withLocale(locale)
                                .withChronology(chrono)
                                .withDecimalStyle(DecimalStyle.of(locale))
                                .format(cDate);
            dayCell.setText(cellText);

            dayCell.updateItem(date, false);
        }
    }

    private int getDaysPerWeek() {
        ValueRange range = getChronology().range(DAY_OF_WEEK);
        return (int)(range.getMaximum() - range.getMinimum() + 1);
    }

    private int getMonthsPerYear() {
        ValueRange range = getChronology().range(MONTH_OF_YEAR);
        return (int)(range.getMaximum() - range.getMinimum() + 1);
    }

    private void updateMonthLabelWidth() {
        if (monthLabel != null) {
            int monthsPerYear = getMonthsPerYear();
            double width = 0;
            for (int i = 0; i < monthsPerYear; i++) {
                YearMonth yearMonth = displayedYearMonth.get().withMonth(i + 1);
                String name = monthFormatterSO.withLocale(getLocale()).format(yearMonth);
                if (Character.isDigit(name.charAt(0))) {
                    // Fallback. The standalone format returned a number, so use standard format instead.
                    name = monthFormatter.withLocale(getLocale()).format(yearMonth);
                }
                width = Math.max(width, Utils.computeTextWidth(monthLabel.getFont(), name, 0));
            }
            monthLabel.setMinWidth(width);
        }
    }

    protected void updateMonthYearPane() {
        String str = formatMonth(displayedYearMonth.get());
        monthLabel.setText(str);

        str = formatYear(displayedYearMonth.get());
        yearLabel.setText(str);
        double width = Utils.computeTextWidth(yearLabel.getFont(), str, 0);
        if (width > yearLabel.getMinWidth()) {
            yearLabel.setMinWidth(width);
        }
    }

    private String formatMonth(YearMonth yearMonth) {
        Locale locale = getLocale();
        ChronoLocalDate cDate = toChrono(yearMonth.atDay(1));

        String str = monthFormatterSO.withLocale(getLocale())
                                     .withChronology(getChronology())
                                     .format(cDate);
        if (Character.isDigit(str.charAt(0))) {
            // Fallback. The standalone format returned a number, so use standard format instead.
            str = monthFormatter.withLocale(getLocale())
                                .withChronology(getChronology())
                                .format(cDate);
        }
        return titleCaseWord(str);
    }

    private String formatYear(YearMonth yearMonth) {
        Locale locale = getLocale();
        DateTimeFormatter formatter = yearFormatter;
        ChronoLocalDate cDate = toChrono(yearMonth.atDay(1));
        int era = cDate.getEra().getValue();
        int nEras = getChronology().eras().size();

        /*if (cDate.get(YEAR) < 0) {
            formatter = yearForNegYearFormatter;
        } else */
        if ((nEras == 2 && era == 0) || nEras > 2) {
            formatter = yearWithEraFormatter;
        }

        // Fixme: Format Japanese era names with Japanese text.
        String str = formatter.withLocale(getLocale())
                              .withChronology(getChronology())
                              .withDecimalStyle(DecimalStyle.of(getLocale()))
                              .format(cDate);

        return str;
    }

    // Ensures that month and day names are titlecased (capitalized).
    private String titleCaseWord(String str) {
        if (str.length() > 0) {
            int firstChar = str.codePointAt(0);
            if (!Character.isTitleCase(firstChar)) {
                str = new String(new int[] { Character.toTitleCase(firstChar) }, 0, 1) +
                      str.substring(Character.offsetByCodePoints(str, 0, 1));
            }
        }
        return str;
    }



    /**
     * determine on which day of week idx the first of the months is
     */
    private int determineFirstOfMonthDayOfWeek() {
        // determine with which cell to start
        int firstDayOfWeek = WeekFields.of(getLocale()).getFirstDayOfWeek().getValue();
        int firstOfMonthIdx = displayedYearMonth.get().atDay(1).getDayOfWeek().getValue() - firstDayOfWeek;
        if (firstOfMonthIdx < 0) {
            firstOfMonthIdx += daysPerWeek;
        }
        return firstOfMonthIdx;
    }

    private int determineDaysInMonth(YearMonth month) {
        return month.atDay(1).plusMonths(1).minusDays(1).getDayOfMonth();
    }

    private boolean isToday(LocalDate localDate) {
        return (localDate.equals(LocalDate.now()));
    }

    protected LocalDate dayCellDate(DateCell dateCell) {
        assert (dayCellDates != null);
        return dayCellDates[dayCells.indexOf(dateCell)];
    }

    // public for behavior class
    public void goToDayCell(DateCell dateCell, int offset, ChronoUnit unit) {
        goToDate(dayCellDate(dateCell).plus(offset, unit));
    }

    // public for behavior class
    public void goToDate(LocalDate date) {
        displayedYearMonth.set(YearMonth.from(date));
        findDayCellForDate(date).requestFocus();
    }

    // public for behavior class
    public void selectDayCell(DateCell dateCell) {
        datePicker.setValue(dayCellDate(dateCell));
        datePicker.hide();
    }

    private DateCell findDayCellForDate(LocalDate date) {
        for (int i = 0; i < dayCellDates.length; i++) {
            if (date.equals(dayCellDates[i])) {
                return dayCells.get(i);
            }
        }
        return dayCells.get(dayCells.size()/2+1);
    }

    void clearFocus() {
        LocalDate focusDate = datePicker.getValue();
        if (focusDate == null) {
            focusDate = LocalDate.now();
        }
        if (YearMonth.from(focusDate).equals(displayedYearMonth.get())) {
            // focus date
            goToDate(focusDate);
        } else {
            // focus month spinner (should not happen)
            backMonthButton.requestFocus();
        }
    }

    protected void createDayCells() {
        final EventHandler<MouseEvent> dayCellActionHandler = new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent ev) {
                if (ev.getButton() != MouseButton.PRIMARY) {
                    return;
                }

                DateCell dayCell = (DateCell)ev.getSource();
                LocalDate date = dayCellDate(dayCell);
                YearMonth yearMonth = YearMonth.from(date);

                if (yearMonth.equals(displayedYearMonth.get())) {
                    selectDayCell(dayCell);
                } else {
                    // previous or next month
                    goToDate(date);
                }
            }
        };

        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < daysPerWeek; col++) {
                DateCell dayCell = createDayCell();
                dayCell.setOnMouseClicked(dayCellActionHandler);
                dayCells.add(dayCell);
            }
        }

        dayCellDates = new LocalDate[6 * daysPerWeek];
    }

    private DateCell createDayCell() {
        DateCell cell = null;
        if (datePicker.getDayCellFactory() != null) {
            cell = datePicker.getDayCellFactory().call(datePicker);
        }
        if (cell == null) {
            cell = new DateCell();
        }

        return cell;
    }

    protected Locale getLocale() {
        return Locale.getDefault(Locale.Category.FORMAT);
    }

    protected Chronology getChronology() {
        return datePicker.getChronology();
    }

    protected ChronoLocalDate toChrono(LocalDate date) {
        return getChronology().date(date);
    }


}
