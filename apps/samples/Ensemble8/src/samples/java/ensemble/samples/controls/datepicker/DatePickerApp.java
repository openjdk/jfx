/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates.
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
package ensemble.samples.controls.datepicker;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.Locale;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Callback;


/**
 * A sample that demonstrates the DatePicker. There is an option to switch
 * locales and see how DatePicker control picks it up.
 *
 * @sampleName DatePicker
 * @preview preview.png
 * @see javafx.scene.control.DateCell
 * @see javafx.scene.control.DatePicker
 */
public class DatePickerApp extends Application {

    private final static ObservableList<String> locales = FXCollections.observableArrayList();
    private DatePicker datePicker;
    private MenuBar datePickerMenuBar;
    private final LocalDate today = LocalDate.now();
    private final LocalDate tomorrow = today.plusDays(1);
    private Locale originalLocale;
    private HBox hbox;
    static {
        locales.addAll(new String[]{
            "en-US",
            "ar-SA",
            "en-GB",
            "cs-CZ",
            "el-GR",
            "he-IL",
            "hi-IN",
            "ja-JP",
            "ja-JP-u-ca-japanese",
            "ru-RU",
            "sv-SE",
            "th-TH",
            "th-TH-u-ca-buddhist",
            "th-TH-u-ca-buddhist-nu-thai",
            "zh-CN",
            "en-US-u-ca-islamic-umalqura",
            "ar-SA-u-ca-islamic-umalqura",
            "en-u-ca-japanese-nu-thai"
        });
    }

    public Parent createContent() {
        Text datePickerText = new Text("Date:");

        hbox = new HBox(18);
        hbox.setAlignment(Pos.CENTER);
        hbox.getChildren().add(datePickerText);

        datePicker = createDatePicker();

        VBox vbox = new VBox(22);
        vbox.getChildren().addAll(datePickerMenuBar, hbox);
        vbox.setPrefSize(300, 200);
        vbox.setMinSize(VBox.USE_PREF_SIZE, VBox.USE_PREF_SIZE);
        return vbox;
    }

    private DatePicker createDatePicker() {
        hbox.getChildren().remove(datePicker);
        LocalDate value = null;
        if (datePicker != null) {
            value = datePicker.getValue();
        }
        DatePicker picker = new DatePicker();
        // day cell factory
        final Callback<DatePicker, DateCell> dayCellFactory = new Callback<DatePicker, DateCell>() {
            @Override
            public DateCell call(final DatePicker datePicker) {
                return new DateCell() {
                    @Override
                    public void updateItem(LocalDate item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item.isBefore(today)) {
                            setStyle("-fx-background-color: #8099ff;");
                        } else {
                            if (item.equals(tomorrow)) {
                                setTooltip(new Tooltip("Tomorrow is important"));
                            }
                        }
                    }
                };
            }
        };
        //Create the menubar to experiment with the DatePicker
        datePickerMenuBar = createMenuBar(dayCellFactory);
        // Listen for DatePicker actions
        picker.setOnAction((ActionEvent t) -> {
            LocalDate isoDate = picker.getValue();
            if ((isoDate != null) && (!isoDate.equals(LocalDate.now()))) {
                for (Menu menu : datePickerMenuBar.getMenus()) {
                    if (menu.getText().equals("Options for Locale")) {
                        for (MenuItem menuItem : menu.getItems()) {
                            if (menuItem.getText().equals("Set date to today")) {
                                if ((menuItem instanceof CheckMenuItem) && ((CheckMenuItem) menuItem).isSelected()) {
                                    ((CheckMenuItem) menuItem).setSelected(false);
                                }
                            }
                        }
                    }
                }
            }
        });
        hbox.getChildren().add(picker);
        if (value != null) {
            picker.setValue(value);
        }
        return picker;
    }

    private MenuBar createMenuBar(final Callback<DatePicker, DateCell> dayCellFac) {
        final MenuBar menuBar = new MenuBar();
        final ToggleGroup localeToggleGroup = new ToggleGroup();
        // Locales
        Menu localeMenu = new Menu("Locales");
        Iterator<String> localeIterator = locales.iterator();
        while (localeIterator.hasNext()) {
            RadioMenuItem localeMenuItem = new RadioMenuItem(localeIterator.next());
            localeMenuItem.setToggleGroup(localeToggleGroup);
            localeMenu.getItems().add(localeMenuItem);
        }

        Menu optionsMenu = new Menu("Options for Locale");
        //Style DatePicker with cell factory
        // XXX - localize
        final String MSG = "Use cell factory to color past days and add tooltip to tomorrow";
        final CheckMenuItem cellFactoryMenuItem = new CheckMenuItem(MSG);
        optionsMenu.getItems().add(cellFactoryMenuItem);
        cellFactoryMenuItem.setOnAction((ActionEvent t) -> {
            if (cellFactoryMenuItem.isSelected()) {
                datePicker.setDayCellFactory(dayCellFac);
            } else {
                datePicker.setDayCellFactory(null);
            }
        });

        //Set date to today
        final CheckMenuItem todayMenuItem = new CheckMenuItem("Set date to today");
        optionsMenu.getItems().add(todayMenuItem);
        todayMenuItem.setOnAction((ActionEvent t) -> {
            if (todayMenuItem.isSelected()) {
                datePicker.setValue(today);
            }
        });

        //Set date to today
        final CheckMenuItem showWeekNumMenuItem = new CheckMenuItem("Show week numbers");
        optionsMenu.getItems().add(showWeekNumMenuItem);
        showWeekNumMenuItem.setOnAction((ActionEvent t) -> {
            datePicker.setShowWeekNumbers(showWeekNumMenuItem.isSelected());
        });

        localeToggleGroup.selectedToggleProperty().addListener((ObservableValue<? extends Toggle> ov,
                                                                Toggle oldToggle, Toggle newToggle) -> {
            if (localeToggleGroup.getSelectedToggle() != null) {
                String selectedLocale = ((RadioMenuItem) localeToggleGroup.getSelectedToggle()).getText();
                Locale locale = Locale.forLanguageTag(selectedLocale.replace('_', '-'));
                Locale.setDefault(locale);
                datePicker = createDatePicker();
                datePicker.setShowWeekNumbers(showWeekNumMenuItem.isSelected());
            }
        });

        menuBar.getMenus().addAll(localeMenu, optionsMenu);
        return menuBar;
    }

    public void play() {
        originalLocale = Locale.getDefault();
    }

    @Override
    public void stop() {
        Locale.setDefault(originalLocale);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setScene(new Scene(createContent()));
        primaryStage.show();
        play();
    }

    /**
     * Java main for when running without JavaFX launcher
     * @param args command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
