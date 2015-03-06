/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates.
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
package modena;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.HTMLEditor;

import static modena.SamplePageChartHelper.*;
import static modena.SamplePageHelpers.*;
import static modena.SamplePageTableHelper.*;
import static modena.SamplePageTreeHelper.createTreeView;
import static modena.SamplePageTreeTableHelper.createTreeTableView;

/**
 * Page showing every control in every state.
 */
public class SamplePage extends GridPane {
    private int rowIndex = 0;
    private Map<String, Node> content = new HashMap<>();
    private List<Section> sections = new ArrayList<>();

    public SamplePage() {
        setVgap(25);
        setHgap(15);
        setPadding(new Insets(15));
        newSection("Label:",
                new Label("Label"),
                withState(new Label("Disabled"), "disabled"));
        newSection("Button:",
                new Button("Button"),
                withState(new Button("Hover"), "hover"),
                withState(new Button("Armed"), "armed"),
                withState(new Button("Focused"), "focused"),
                withState(new Button("Focused & Hover"), "focused, hover"),
                withState(new Button("Focused & Armed"), "focused, armed"),
                withState(new Button("Disabled"), "disabled"));
        Button defaultButton = new Button("Button");
        defaultButton.setDefaultButton(true);
        newSection("Default Button:",
                defaultButton,
                withState(new Button("Hover"), "default, hover"),
                withState(new Button("Armed"), "default, armed"),
                withState(new Button("Focused"), "default, focused"),
                withState(new Button("Focused & Hover"), "default, focused, hover"),
                withState(new Button("Focused & Armed"), "default, focused, armed"),
                withState(new Button("Disabled"), "default, disabled"));
        Button b1 = new Button("Button");
        b1.setStyle("-fx-base: #f3622d;");
        Button b2 = new Button("Button");
        b2.setStyle("-fx-base: #fba71b;");
        Button b3 = new Button("Button");
        b3.setStyle("-fx-base: #57b757;");
        Button b4 = new Button("Button");
        b4.setStyle("-fx-base: #41a9c9;");
        Button b5 = new Button("Button");
        b5.setStyle("-fx-base: #888;");
        newSection("Nice Colors:", b1, b2, b3, b4, b5);
        newSection("Greys:",0,
                createGreyButton(0),
                createGreyButton(0.1),
                createGreyButton(0.2),
                createGreyButton(0.3),
                createGreyButton(0.4),
                createGreyButton(0.5),
                createGreyButton(0.6),
                createGreyButton(0.7),
                createGreyButton(0.8),
                createGreyButton(0.9),
                createGreyButton(1));
        ToggleGroup tg1 = new ToggleGroup();
        ToggleGroup tg2 = new ToggleGroup();
        ToggleGroup tg3 = new ToggleGroup();
        ToggleGroup tg4 = new ToggleGroup();

        ToggleButton left1 = new ToggleButton("Left");
        left1.getStyleClass().add("left-pill");
        left1.setToggleGroup(tg1);
        ToggleButton center1 = new ToggleButton("Center");
        center1.getStyleClass().add("center-pill");
        center1.setToggleGroup(tg1);
        ToggleButton right1 = new ToggleButton("Right");
        right1.getStyleClass().add("right-pill");
        right1.setToggleGroup(tg1);

        ToggleButton left2 = new ToggleButton("Left");
        left2.getStyleClass().add("left-pill");
        left2.setToggleGroup(tg2);
        left2.setSelected(true);
        ToggleButton center2 = new ToggleButton("Center");
        center2.getStyleClass().add("center-pill");
        center2.setToggleGroup(tg2);
        ToggleButton right2 = new ToggleButton("Right");
        right2.getStyleClass().add("right-pill");
        right2.setToggleGroup(tg2);

        ToggleButton left3 = new ToggleButton("Left");
        left3.getStyleClass().add("left-pill");
        left3.setToggleGroup(tg3);
        ToggleButton center3 = new ToggleButton("Center");
        center3.getStyleClass().add("center-pill");
        center3.setToggleGroup(tg3);
        center3.setSelected(true);
        ToggleButton right3 = new ToggleButton("Right");
        right3.getStyleClass().add("right-pill");
        right3.setToggleGroup(tg3);

        ToggleButton left4 = new ToggleButton("Left");
        left4.getStyleClass().add("left-pill");
        left4.setToggleGroup(tg4);
        ToggleButton center4 = new ToggleButton("Center");
        center4.getStyleClass().add("center-pill");
        center4.setToggleGroup(tg4);
        ToggleButton right4 = new ToggleButton("Right");
        right4.getStyleClass().add("right-pill");
        right4.setToggleGroup(tg4);
        right4.setSelected(true);

        newSection("Pill Toggle\nButtons:",
                new HBox(left1, center1, right1),
                new HBox(left2, center2, right2),
                new HBox(left3, center3, right3),
                new HBox(left4, center4, right4)
        );

        ToggleGroup tg5 = new ToggleGroup();
        ToggleGroup tg6 = new ToggleGroup();
        ToggleGroup tg7 = new ToggleGroup();
        ToggleGroup tg8 = new ToggleGroup();

        ToggleButton left5 = new ToggleButton("#");
        left5.getStyleClass().add("left-pill");
        left5.setToggleGroup(tg5);
        ToggleButton center5 = new ToggleButton("#");
        center5.getStyleClass().add("center-pill");
        center5.setToggleGroup(tg5);
        ToggleButton right5 = new ToggleButton("#");
        right5.getStyleClass().add("right-pill");
        right5.setToggleGroup(tg5);

        ToggleButton left5_1 = new ToggleButton("L");
        left5_1.getStyleClass().add("left-pill");
        left5_1.setToggleGroup(tg5);
        withState(left5_1, "focused");
        ToggleButton center5_1 = new ToggleButton("C");
        center5_1.getStyleClass().add("center-pill");
        center5_1.setToggleGroup(tg5);
        ToggleButton right5_1 = new ToggleButton("R");
        right5_1.getStyleClass().add("right-pill");
        right5_1.setToggleGroup(tg5);

        ToggleButton left5_2 = new ToggleButton("L");
        left5_2.getStyleClass().add("left-pill");
        left5_2.setToggleGroup(tg5);
        ToggleButton center5_2 = new ToggleButton("C");
        center5_2.getStyleClass().add("center-pill");
        center5_2.setToggleGroup(tg5);
        withState(center5_2, "focused");
        ToggleButton right5_2 = new ToggleButton("R");
        right5_2.getStyleClass().add("right-pill");
        right5_2.setToggleGroup(tg5);

        ToggleButton left5_3 = new ToggleButton("L");
        left5_3.getStyleClass().add("left-pill");
        left5_3.setToggleGroup(tg5);
        ToggleButton center5_3 = new ToggleButton("C");
        center5_3.getStyleClass().add("center-pill");
        center5_3.setToggleGroup(tg5);
        ToggleButton right5_3 = new ToggleButton("R");
        right5_3.getStyleClass().add("right-pill");
        right5_3.setToggleGroup(tg5);
        withState(right5_3, "focused");

        ToggleButton left6 = new ToggleButton("L");
        left6.getStyleClass().add("left-pill");
        left6.setToggleGroup(tg6);
        left6.setSelected(true);
        withState(left6, "focused");
        ToggleButton center6 = new ToggleButton("C");
        center6.getStyleClass().add("center-pill");
        center6.setToggleGroup(tg6);
        ToggleButton right6 = new ToggleButton("R");
        right6.getStyleClass().add("right-pill");
        right6.setToggleGroup(tg6);

        ToggleButton left7 = new ToggleButton("L");
        left7.getStyleClass().add("left-pill");
        left7.setToggleGroup(tg7);
        ToggleButton center7 = new ToggleButton("C");
        center7.getStyleClass().add("center-pill");
        center7.setToggleGroup(tg7);
        center7.setSelected(true);
        withState(center7, "focused");
        ToggleButton right7 = new ToggleButton("R");
        right7.getStyleClass().add("right-pill");
        right7.setToggleGroup(tg7);

        ToggleButton left8 = new ToggleButton("L");
        left8.getStyleClass().add("left-pill");
        left8.setToggleGroup(tg8);
        ToggleButton center8 = new ToggleButton("C");
        center8.getStyleClass().add("center-pill");
        center8.setToggleGroup(tg8);
        ToggleButton right8 = new ToggleButton("R");
        right8.getStyleClass().add("right-pill");
        right8.setToggleGroup(tg8);
        right8.setSelected(true);
        withState(right8, "focused");

        newSection("Pill Toggle\nButtons\nFocused:",
                new HBox(left5, center5, right5),
                new HBox(left5_1, center5_1, right5_1),
                new HBox(left5_2, center5_2, right5_2),
                new HBox(left5_3, center5_3, right5_3),
                new HBox(left6, center6, right6),
                new HBox(left7, center7, right7),
                new HBox(left8, center8, right8)
        );
        newSection("ToggleButton:",
                new ToggleButton("Button"),
                withState(new ToggleButton("Hover"), "hover"),
                withState(new ToggleButton("Armed"), "armed"),
                withState(new ToggleButton("Focused"), "focused"),
                withState(new ToggleButton("Focused & Hover"), "focused, hover"),
                withState(new ToggleButton("Focused & Armed"), "focused, armed"),
                withState(new ToggleButton("Disabled"), "disabled"));
        newSection("ToggleButton Selected:",
                withState(new ToggleButton("Button"), "selected"),
                withState(new ToggleButton("Hover"), "selected, hover"),
                withState(new ToggleButton("Armed"), "selected, armed"),
                withState(new ToggleButton("Focused"), "selected, focused"),
                withState(new ToggleButton("Focused & Hover"), "selected, focused, hover"),
                withState(new ToggleButton("Focused & Armed"), "selected, focused, armed"),
                withState(new ToggleButton("Disabled"), "selected, disabled"));
        newSection("CheckBox:",
                new CheckBox("CheckBox"),
                withState(new CheckBox("Hover"), "hover"),
                withState(new CheckBox("Armed"), "armed"),
                withState(new CheckBox("Focused"), "focused"),
                withState(new CheckBox("Focused & Hover"), "focused, hover"),
                withState(new CheckBox("Focused & Armed"), "focused, armed"),
                withState(new CheckBox("Disabled"), "disabled"));
        newSection("CheckBox Selected:",
                withState(new CheckBox("CheckBox"), "selected"),
                withState(new CheckBox("Hover"), "selected, hover"),
                withState(new CheckBox("Armed"), "selected, armed"),
                withState(new CheckBox("Focused"), "selected, focused"),
                withState(new CheckBox("Focused & Hover"), "selected, focused, hover"),
                withState(new CheckBox("Focused & Armed"), "selected, focused, armed"),
                withState(new CheckBox("Disabled"), "selected, disabled"));

        CheckBox checkB = new CheckBox("CheckBox");
        checkB.setSelected(true);
        checkB.setIndeterminate(true);
        checkB.setAllowIndeterminate(true);
        newSection("CheckBox\nIndeterminate:",
                checkB,
                withState(new CheckBox("Hover"), "indeterminate, selected, hover"),
                withState(new CheckBox("Armed"), "indeterminate, selected, armed"),
                withState(new CheckBox("Focused"), "indeterminate, selected, focused"),
                withState(new CheckBox("Focused & Hover"), "indeterminate, selected, focused, hover"),
                withState(new CheckBox("Focused & Armed"), "indeterminate, selected, focused, armed"),
                withState(new CheckBox("Disabled"), "indeterminate, selected, disabled"));
        newSection("RadioButton:",
                new RadioButton("RadioButton"),
                withState(new RadioButton("Hover"), "hover"),
                withState(new RadioButton("Armed"), "armed"),
                withState(new RadioButton("Focused"), "focused"),
                withState(new RadioButton("Focused & Hover"), "focused, hover"),
                withState(new RadioButton("Focused & Armed"), "focused, armed"),
                withState(new RadioButton("Disabled"), "disabled"));
        newSection("RadioButton\nSelected:",
                withState(new RadioButton("RadioButton"), "selected"),
                withState(new RadioButton("Hover"), "selected, hover"),
                withState(new RadioButton("Armed"), "selected, armed"),
                withState(new RadioButton("Focused"), "selected, focused"),
                withState(new RadioButton("Focused & Hover"), "selected, focused, hover"),
                withState(new RadioButton("Focused & Armed"), "selected, focused, armed"),
                withState(new RadioButton("Disabled"), "selected, disabled"));
        newSection("HyperLink:",
                new Hyperlink("Hyperlink"),
                withState(new Hyperlink("Visited"), "visited"),
                withState(new Hyperlink("Hover"), "hover"),
                withState(new Hyperlink("Armed"), "armed"),
                withState(new Hyperlink("Focused"), "focused"),
                withState(new Hyperlink("F & Visited"), "focused, visited"),
                withState(new Hyperlink("F & Hover"), "focused, hover"),
                withState(new Hyperlink("F & Armed"), "focused, armed"),
                withState(new Hyperlink("Disabled"), "disabled"));
        ObservableList<String> choiceBoxLongList = sampleItems(200);
        choiceBoxLongList.add(100, "Long List");
        ChoiceBox<String> cb1 = new ChoiceBox<String>(sampleItems());
        cb1.setValue("Item A");
        ChoiceBox<String> cb2 = new ChoiceBox<String>(choiceBoxLongList);
        cb2.setValue("Long List");
        ChoiceBox<String> cb3 = new ChoiceBox<String>(sampleItems());
        cb3.setValue("Item B");
        ChoiceBox<String> cb4 = new ChoiceBox<String>(sampleItems());
        cb4.setValue("Item B");
        ChoiceBox<String> cb5 = new ChoiceBox<String>(sampleItems());
        cb5.setValue("Item B");
        ChoiceBox<String> cb6 = new ChoiceBox<String>(sampleItems());
        cb6.setValue("Item C");
        cb6.setDisable(true);
        newSection(
                "ChoiceBox:", cb1, cb2,
                withState(cb3, "hover"),
                withState(cb4,"showing"),
                withState(cb5,"focused"),
                cb6);
        ChoiceBox<String> c1 = new ChoiceBox<String>(sampleItems());
        c1.setValue("Item A");
        ChoiceBox<String> c2 = new ChoiceBox<String>(choiceBoxLongList);
        c2.setValue("Long List");
        ChoiceBox<String> c3 = new ChoiceBox<String>(sampleItems());
        c3.setValue("Item B");
        ChoiceBox<String> c4 = new ChoiceBox<String>(sampleItems());
        c4.setValue("Item B");
        ChoiceBox<String> c5 = new ChoiceBox<String>(sampleItems());
        c5.setValue("Item B");
        ChoiceBox<String> c6 = new ChoiceBox<String>(sampleItems());
        c6.setValue("Item C");
        c6.setDisable(true);
        newSection(
                "ChoiceBox:", c1, c2,
                withState(c3, "hover"),
                withState(c4, "showing"),
                withState(c5,"focused"),
                c6);
        ComboBox<String> com1 = new ComboBox<String>(sampleItems());
        com1.setValue("Item A");
        com1.setEditable(true);
        ComboBox<String> com2 = new ComboBox<String>(sampleItems());
        com2.setValue("Item B");
        com2.setEditable(true);
        ComboBox<String> com3 = new ComboBox<String>(sampleItems());
        com3.setValue("Item B");
        com3.setEditable(true);
        newSection(
                "ComboBox\nEditable:", com1,
                withState(com2, "editable", ".arrow-button", "hover"),
                withState(com3, "editable", ".arrow-button", "pressed"));
        ComboBox<String> co1 = new ComboBox<String>(sampleItems());
        co1.setValue("Item B");
        co1.setEditable(true);
        ComboBox<String> co2 = new ComboBox<String>(sampleItems());
        co2.setValue("Item C");
        co2.setEditable(true);
        co2.setDisable(true);
        newSection(
                "ComboBox\nEditable\n(More):",
                withState(co1, "editable,contains-focus", ".text-field", "focused"),
                co2);
        String[] spinnerStyles = new String[] {
                "default",
                Spinner.STYLE_CLASS_ARROWS_ON_RIGHT_HORIZONTAL,
                Spinner.STYLE_CLASS_ARROWS_ON_LEFT_VERTICAL,
                Spinner.STYLE_CLASS_ARROWS_ON_LEFT_HORIZONTAL,
                Spinner.STYLE_CLASS_SPLIT_ARROWS_VERTICAL,
                Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL
        };
        for (String style: spinnerStyles) {
            final Spinner[] spinners = new Spinner[3];
            for (int i=0; i<spinners.length; i++) {
                spinners[i] = new Spinner();
                spinners[i].getStyleClass().add(style);
                spinners[i].setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 10));
            }
            spinners[2].setDisable(true);
            newSection(
                    "Spinner ("+style+"):",
                    spinners[0],
                    withState(spinners[1], "focused"),
                    spinners[2]
            );
        }
        newSection("Color Picker:", new ColorPicker(Color.RED), withState(new ColorPicker(Color.RED), "hover"),
                withState(new ColorPicker(Color.RED), "showing"), withState(new ColorPicker(Color.RED), "focused"),
                withState(new ColorPicker(Color.RED), "disabled"));
        ColorPicker cp1 = new ColorPicker(Color.RED);
        cp1.getStyleClass().add(ColorPicker.STYLE_CLASS_SPLIT_BUTTON);
        ColorPicker cp2 = new ColorPicker(Color.RED);
        cp2.getStyleClass().add(ColorPicker.STYLE_CLASS_SPLIT_BUTTON);
        ColorPicker cp3 = new ColorPicker(Color.RED);
        cp3.getStyleClass().add(ColorPicker.STYLE_CLASS_SPLIT_BUTTON);
        ColorPicker cp4 = new ColorPicker(Color.RED);
        cp4.getStyleClass().add(ColorPicker.STYLE_CLASS_SPLIT_BUTTON);
        ColorPicker cp5 = new ColorPicker(Color.RED);
        cp5.getStyleClass().add(ColorPicker.STYLE_CLASS_SPLIT_BUTTON);
        newSection(
                "Color Picker\n Split Button:", cp1,
                withState(cp2, "hover"),
                withState(cp3,"showing"),
                withState(cp4, "focused"),
                withState(cp5,"disabled"));
        MenuButton mb1 = new MenuButton();
        mb1.getItems().addAll(createMenuItems(20));
        mb1.setText("right");
        mb1.setPopupSide(Side.RIGHT);
        MenuButton mb2 = new MenuButton();
        mb2.getItems().addAll(createMenuItems(20));
        mb2.setText("normal");
        MenuButton mb3 = new MenuButton();
        mb3.getItems().addAll(createMenuItems(20));
        mb3.setText("hover");
        MenuButton mb4 = new MenuButton();
        mb4.getItems().addAll(createMenuItems(20));
        mb4.setText("armed");
        MenuButton mb5 = new MenuButton();
        mb5.getItems().addAll(createMenuItems(20));
        mb5.setText("focused");
        MenuButton mb6 = new MenuButton();
        mb6.getItems().addAll(createMenuItems(20));
        mb6.setText("disabled");
        newSection("MenuButton:", mb1, mb2,
                withState(mb3, "openvertically, hover"),
                withState(mb4, "openvertically, armed"),
                withState(mb5, "openvertically, focused"),
                withState(mb6, "openvertically, disabled"));
        SplitMenuButton splitmb1 = new SplitMenuButton(createMenuItems(20));
        splitmb1.setText("right");
        splitmb1.setPopupSide(Side.RIGHT);
        SplitMenuButton splitmb2 = new SplitMenuButton(createMenuItems(20));
        splitmb2.setText("normal");
        SplitMenuButton splitmb3 = new SplitMenuButton(createMenuItems(20));
        splitmb3.setText("hover");
        SplitMenuButton splitmb4 = new SplitMenuButton(createMenuItems(20));
        splitmb4.setText("armed");
        newSection(
                "SplitMenuButton:", splitmb1, splitmb2,
                withState(splitmb3, "openvertically",".label", "hover"),
                withState(splitmb4,"armed,openvertically",".label", "armed"));
        SplitMenuButton splitmb_m1 = new SplitMenuButton(createMenuItems(20));
        splitmb_m1.setText("arrow hover");
        SplitMenuButton splitmb_m2 = new SplitMenuButton(createMenuItems(20));
        splitmb_m2.setText("showing");
        SplitMenuButton splitmb_m3 = new SplitMenuButton(createMenuItems(20));
        splitmb_m3.setText("focused");
        SplitMenuButton splitmb_m4 = new SplitMenuButton(createMenuItems(20));
        splitmb_m4.setText("disabled");
        newSection(
                "SplitMenuButton\nnMore:",
                withState(splitmb_m1, "openvertically", ".arrow-button", "hover"),
                withState(splitmb_m2, "openvertically,showing"),
                withState(splitmb_m3, "openvertically,focused"),
                withState(splitmb_m4, "openvertically,disabled"));
        newSection(
                "DatePicker:",
                new DatePicker(),
                withState(new DatePicker(),"hover"),
                withState(new DatePicker(), "showing"),
                withState(new DatePicker(), "focused"),
                withState(new DatePicker(), "disabled"));
        Slider s1 = new Slider(0, 100, 50);
        s1.setMaxWidth(90);
        Slider s2 = new Slider(0, 100, 50);
        s2.setMaxWidth(90);
        Slider s3 = new Slider(0, 100, 50);
        s3.setMaxWidth(90);
        Slider s4 = new Slider(0, 100, 50);
        s4.setMaxWidth(90);
        Slider s5 = new Slider(0, 100, 50);
        s5.setShowTickLabels(true);
        s5.setShowTickMarks(true);
        newDetailedSection(
                new String[]{"Slider (H):", "normal", "hover", "pressed", "disabled", "tickmarks"},
                withState(s1, null),
                withState(s2, null, ".thumb", "hover"),
                withState(s3, null, ".thumb", "hover, pressed"),
                withState(s4, "disabled"),
                s5);
        newDetailedSection(
                new String[]{"Slider (H) Focused:", "normal", "hover", "pressed"},
                withState(new Slider(0, 100, 50), "focused"),
                withState(new Slider(0, 100, 50), "focused", ".thumb", "hover"),
                withState(new Slider(0, 100, 50), "focused", ".thumb", "hover, pressed"));
        Slider s_v1 = new Slider(0, 100, 50);
        s_v1.setOrientation(Orientation.VERTICAL);
        Slider s_v2 = new Slider(0, 100, 50);
        s_v2.setOrientation(Orientation.VERTICAL);
        Slider s_v3 = new Slider(0, 100, 50);
        s_v3.setOrientation(Orientation.VERTICAL);
        Slider s_v4 = new Slider(0, 100, 50);
        s_v4.setOrientation(Orientation.VERTICAL);
        Slider s_v5 = new Slider(0, 100, 50);
        s_v5.setOrientation(Orientation.VERTICAL);
        s_v5.setShowTickLabels(true);
        s_v5.setShowTickMarks(true);
        newSection("Slider - V:",
                s_v1,
                withState(s_v2, null, ".thumb", "hover"),
                withState(s_v3, null, ".thumb", "hover, pressed"),
                withState(s_v4, "disabled"),
                s_v5);
        ScrollBar sb1 = new ScrollBar();
        sb1.setMinWidth(30);
        sb1.setPrefWidth(30);
        ScrollBar sb2 = new ScrollBar();
        sb2.setVisibleAmount(60);
        sb1.setMax(100);
        newDetailedSection(
                new String[]{"Scrollbar - H: ", "normal", "focused", "small", "big thumb"},
                new ScrollBar(),
                withState(new ScrollBar(), "focused"), sb1, sb2);
        ScrollBar sb3 = new ScrollBar();
        sb3.setOrientation(Orientation.VERTICAL);
        ScrollBar sb4 = new ScrollBar();
        sb4.setOrientation(Orientation.VERTICAL);
        ScrollBar sb5 = new ScrollBar();
        sb5.setMinHeight(30);
        sb5.setPrefHeight(30);
        sb5.setOrientation(Orientation.VERTICAL);
        ScrollBar sb6 = new ScrollBar();
        sb6.setOrientation(Orientation.VERTICAL);
        ScrollBar sb7 = new ScrollBar();
        sb7.setOrientation(Orientation.VERTICAL);
        ScrollBar sb8 = new ScrollBar();
        sb8.setOrientation(Orientation.VERTICAL);
        ScrollBar sb9 = new ScrollBar();
        sb9.setOrientation(Orientation.VERTICAL);
        newDetailedSection(
                new String[]{"Scrollbar - V: ", "normal", "focused", "small", "btn hover", "btn pressed", ".thumb hover", ".thumb pressed"},
                withState(sb3, "vertical"),
                withState(sb4, "focused"),
                withState(sb5, "vertical"),
                withState(sb6, "vertical", ".decrement-button", "hover"),
                withState(sb7, "vertical", ".decrement-button", "pressed"),
                withState(sb8, "vertical", ".thumb", "hover"),
                withState(sb9, "vertical", ".thumb", "pressed")
        );
        ScrollPane scrollPane = new ScrollPane(scrollPaneContent());
        scrollPane.setMinWidth(40);
        scrollPane.setPrefWidth(40);
        scrollPane.setMinHeight(40);
        scrollPane.setPrefHeight(40);
        newDetailedSection(
                new String[] {"ScrollPane: ", "normal", "small", "focused", "empty"},
                new ScrollPane(scrollPaneContent()),
                scrollPane,
                withState(new ScrollPane(scrollPaneContent()), "focused"),
                new ScrollPane()
        );
        ScrollPane scrollPaneVbar = new ScrollPane(scrollPaneContent());
        scrollPaneVbar.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        ScrollPane scrollPaneHbar = new ScrollPane(scrollPaneContent());
        scrollPaneHbar.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        newDetailedSection(
                new String[] {"ScrollPane H/V: ", "H Bar", "V bar"},
                scrollPaneVbar,
                scrollPaneHbar);
        Separator sep1 = new Separator();
        sep1.setPrefWidth(100);
        Separator sep2 = new Separator(Orientation.VERTICAL);
        sep2.setPrefHeight(50);
        newDetailedSection(
                new String[]{"Separator: ", "horizontal", "vertical"},
                sep1, sep2);
        ProgressBar pb1 = new ProgressBar(0.6);
        pb1.setPrefWidth(200);
        ProgressBar pb2 = new ProgressBar(0.2);
        pb2.setPrefWidth(200);
        ProgressBar pb3 = new ProgressBar(-1);
        pb3.setPrefWidth(200);
        newDetailedSection(
                new String[] {"ProgressBar: ", "normal", "disabled", "indeterminate"},
                pb1,
                withState(pb2, "disabled"),
                pb3);
        newDetailedSection(
                new String[] {"ProgressIndicator: ", "normal 0%", "normal 60%", "normal 100%", "disabled"},
                new ProgressIndicator(0),
                new ProgressIndicator(0.6),
                new ProgressIndicator(1),
                withState(new ProgressIndicator(0.5), "disabled"));
        ProgressIndicator pi1 = new ProgressIndicator(-1);
        pi1.setMaxWidth(USE_PREF_SIZE);
        pi1.setMaxHeight(USE_PREF_SIZE);
        ProgressIndicator pi2 = new ProgressIndicator(-1);
        pi2.setPrefWidth(30);
        pi2.setPrefHeight(30);
        ProgressIndicator pi3 = new ProgressIndicator(-1);
        pi3.setPrefWidth(60);
        pi3.setPrefHeight(60);
        ProgressIndicator pi4 = new ProgressIndicator(-1);
        pi4.setMaxWidth(USE_PREF_SIZE);
        pi4.setMaxHeight(USE_PREF_SIZE);
        pi4.setDisable(true);
        newDetailedSection(
                new String[]{"ProgressIndicator\nIndeterminate: ", "normal", "small", "large", "disabled"},
                pi1, pi2, pi3, pi4);
        TextField textF = new TextField();
        textF.setPromptText("Prompt Text");
        newSection(
                "TextField:",
                new TextField("TextField"),
                textF,
                withState(new TextField("Focused"), "focused"),
                withState(new TextField("Disabled"), "disabled"));
        PasswordField pField1 = new PasswordField();
        pField1.setText("Password");
        PasswordField pField2 = new PasswordField();
        pField2.setPromptText("Prompt Text");
        PasswordField pField3 = new PasswordField();
        pField3.setText("Password");
        PasswordField pField4 = new PasswordField();
        pField4.setText("Password");
        newSection(
                "PasswordField:",
                pField1,
                pField2,
                withState(pField3, "focused"),
                withState(pField4, "disabled"));
        TextArea tArea1 = new TextArea("TextArea");
        tArea1.setPrefColumnCount(10);
        tArea1.setPrefRowCount(2);
        TextArea tArea2 = new TextArea("Many Lines of\nText.\n#3\n#4\n#5\n#6\n#7\n#8\n#9\n#10");
        tArea2.setPrefColumnCount(10);
        tArea2.setPrefRowCount(5);
        TextArea tArea3 = new TextArea("Many Lines of\nText.\n#3\n#4\n#5\n#6\n#7\n#8\n#9\n#10");
        tArea3.setPrefColumnCount(6);
        tArea3.setPrefRowCount(5);
        TextArea tArea4 = new TextArea("Prompt Text");
        tArea4.setPrefColumnCount(10);
        tArea4.setPrefRowCount(2);

        TextArea tArea5 = new TextArea("Focused");
        tArea5.setPrefColumnCount(7);
        tArea5.setPrefRowCount(2);
        TextArea tArea6 = new TextArea("Disabled");
        tArea6.setPrefColumnCount(8);
        tArea6.setPrefRowCount(2);
        newSection(
                "TextArea:",
                tArea1, tArea2, tArea3, tArea4,
                withState(tArea5, "focused"),
                withState(tArea6, "disabled"));
        newSection(
                "HTMLEditor:",
                new HTMLEditor() {{
                    setHtmlText("Hello <b>Bold</b> Text");
                    setPrefSize(650, 120);
                }});
        newSection(
                "HTMLEditor\nFocused:",
                withState(new HTMLEditor() {{
                    setHtmlText("<i>Focused</i>");
                    setPrefSize(650, 120);
                }}, "focused")
                );
        newDetailedSection(
                new String[] { "ToolBar (H|TOP):", "normal", "overflow", "disabled" },
                createToolBar(Side.TOP, false, false),
                createToolBar(Side.TOP, true, false),
                createToolBar(Side.TOP, false, true)
        );
        newDetailedSection(
                new String[] { "ToolBar (H|BOTTOM):", "normal", "overflow", "disabled" },
                createToolBar(Side.BOTTOM, false, false),
                createToolBar(Side.BOTTOM, true, false),
                createToolBar(Side.BOTTOM, false, true)
        );
        newDetailedSection(
                new String[] { "ToolBar (V|LEFT):", "normal", "overflow", "disabled" },
                createToolBar(Side.LEFT, false, false),
                createToolBar(Side.LEFT, true, false),
                createToolBar(Side.LEFT, false, true)
        );
        newDetailedSection(
                new String[] {"ToolBar (V|RIGHT):", "normal", "overflow", "disabled"},
                createToolBar(Side.RIGHT,false,false),
                createToolBar(Side.RIGHT,true,false),
                createToolBar(Side.RIGHT,false,true)
                );
        newSection(
                "Tabs\n(Top):",
                wrapBdr(createTabPane(4, 250, 100, null, false, false, Side.TOP)),
                wrapBdr(withState(createTabPane(5, 200, 100, "Tab Disabled &\nMany Tabs", false, true, Side.TOP), null, ".tab", "disabled")),
                wrapBdr(withState(createTabPane(5, 200, 100, "Disabled", false, false, Side.TOP), "disabled"))
        );
        newSection(
                "Tabs Floating\n(Top):",
                createTabPane(4, 250, 100, null, true, false, Side.TOP),
                withState(createTabPane(5, 200, 100, "Tab Disabled &\nMany Tabs", true, true, Side.TOP), null, ".tab", "disabled"),
                withState(createTabPane(5, 200, 100, "Disabled", true, false, Side.TOP), "disabled")
                );
        newSection(
                "Tabs\n(Bottom):",
                wrapBdr(createTabPane(4, 250, 100, null, false, false, Side.BOTTOM)),
                wrapBdr(withState(createTabPane(5, 200, 100, "Tab Disabled &\nMany Tabs", false, true, Side.BOTTOM), null, ".tab", "disabled")),
                wrapBdr(withState(createTabPane(5, 200, 100, "Disabled", false, false, Side.BOTTOM), "disabled"))
                );
        newSection(
                "Tabs Floating\n(Bottom):",
                createTabPane(4, 250, 100, null, true, false, Side.BOTTOM),
                withState(createTabPane(5, 200, 100, "Tab Disabled &\nMany Tabs", true, true, Side.BOTTOM), null, ".tab", "disabled"),
                withState(createTabPane(5, 200, 100, "Disabled", true, false, Side.BOTTOM), "disabled")
                );
        newSection(
                "Tabs\n(Left):",
                wrapBdr(createTabPane(4, 250, 250, null, false, false, Side.LEFT)),
                wrapBdr(withState(createTabPane(5, 200, 250, "Tab Disabled &\nMany Tabs", false, true, Side.LEFT), null, ".tab", "disabled")),
                wrapBdr(withState(createTabPane(5, 200, 250, "Disabled", false, false, Side.LEFT), "disabled"))
                );
        newSection(
                "Tabs Floating\n(Left):",
                createTabPane(4, 250, 250, null, true, false, Side.LEFT),
                withState(createTabPane(5, 200, 250, "Tab Disabled &\nMany Tabs", true, true, Side.LEFT), null, ".tab", "disabled"),
                withState(createTabPane(5, 200, 250, "Disabled", true, false, Side.LEFT), "disabled")
                );
        newSection(
                "Tabs\n(Right):",
                wrapBdr(createTabPane(4, 250, 250, null, false, false, Side.RIGHT)),
                wrapBdr(withState(createTabPane(5, 200, 250, "Tab Disabled &\nMany Tabs", false, true, Side.RIGHT), null, ".tab", "disabled")),
                wrapBdr(withState(createTabPane(5, 200, 250, "Disabled", false, false, Side.RIGHT), "disabled"))
                );
        newSection(
                "Tabs Floating\n(Right):",
                createTabPane(4, 250, 250, null, true, false, Side.RIGHT),
                withState(createTabPane(5, 200, 250, "Tab Disabled &\nMany Tabs", true, true, Side.RIGHT), null, ".tab", "disabled"),
                withState(createTabPane(5, 200, 250, "Disabled", true, false, Side.RIGHT), "disabled")
        );
        TitledPane tPane = new TitledPane("Not Collapsible", new Label("Content\nLine2."));
        tPane.setCollapsible(false);
        newDetailedSection(
                new String[]{"TitledPane:", "normal", "not collapsible", "hover", "focused", "disabled"},
                new TitledPane("Title", new Label("Content\nLine2.")),
                tPane,
                withState(new TitledPane("Title", new Label("Content\nLine2.")), "hover"),
                withState(new TitledPane("Title", new Label("Content\nLine2.")), "focused"),
                withState(new TitledPane("Title", new Label("Content\nLine2.")), "disabled")
        );
        newDetailedSection(
                new String[] {"Accordion:", "normal", "hover", "focused", "disabled"},
                createAccordion(),
                withState(createAccordion(), null, ".titled-pane", "hover"),
                withState(createAccordion(), null, ".titled-pane", "focused"),
                withState(createAccordion(), "disabled")
                );
        newDetailedSection(
                new String[] {"SplitPane (H):", "simple", "many", "complex"},
                createSplitPane(2, false, null),
                createSplitPane(4, false, null),
                createSplitPane(2, false, createSplitPane(2, true, null))
                );
        newDetailedSection(
                new String[] {"SplitPane (V):", "simple", "many", "complex"},
                createSplitPane(2,true,null),
                createSplitPane(4,true,null),
                createSplitPane(2,true,createSplitPane(2,false,null))
                );
        newDetailedSection(
                new String[] {"Pagination:", "simple", "infinate"},
                createPagination(5, false, true),
                createPagination(Integer.MAX_VALUE, false, true)
                );
        newDetailedSection(
                new String[] {"Pagination\nBullet Style:", "simple", "infinate"},
                createPagination(5, true, true),
                createPagination(Integer.MAX_VALUE, true, true)
                );
        newSection(
                "Pagination\nNo Arrows:",
                createPagination(Integer.MAX_VALUE, false, false)
        );
        newDetailedSection(
                new String[] { "ListView\n2 items\nsingle selection:", "normal", "focused", "disabled" },
                createListView(3, false, false, false),
                withState(createListView(3, false, false, false), "focused"),
                createListView(3, false, true, false)
        );
        newDetailedSection(
                new String[] {"ListView\n10,000 items\nmultiple selection:","normal", "focused", "disabled"},
                createListView(10000, true, false, false),
                withState(createListView(10000, true, false, false), "focused"),
                createListView(10000, true, true, false)
                );
        newDetailedSection(
                new String[] {"ListView (H)\n10,000 items\nmultiple selection:","normal", "focused", "disabled"},
                createListView(10000, true, false, true),
                withState(createListView(10000, true, false, true), "focused"),
                createListView(10000, true, true, true)
                );
        newSection(
                "TableView Simple:\n(Row Selection)",
                createTableViewSimple(550, true, false),
                withState(createTableViewSimple(150, true, false), "focused")
        );
        newSection(
                "TableView Simple:\n(Constrained Resize)",
                createTableViewSimple(550, true, true),
                withState(createTableViewSimple(150, true, true), "focused")
        );
        newSection(
                "TableView:\n(Row Selection)",
                createTableView(550, true),
                withState(createTableView(150, true), "focused")
        );
        newSection(
                "TableView:\n(Cell Selection)",
                createTableView(550, false),
                withState(createTableView(150, false), "focused")
                );
        newSection(
                "TreeView:",
                createTreeView(350),
                withState(createTreeView(350), "focused")
                );
        newSection(
                "TreeTableView:\n" +
                        "(Row Selection)",
                createTreeTableView(550, false),
                withState(createTreeTableView(200, false), "focused")
                );
        newSection(
                "TreeTableView:\n(Cell Selection)",
                createTreeTableView(550, true),
                withState(createTreeTableView(200, true), "focused")
                );
        ListView<String> lv = new ListView<>();
        lv.setPrefWidth(150);
        lv.setPrefHeight(100);
        TableView tv = new TableView();
        tv.setPrefWidth(150);
        tv.setPrefHeight(100);
        TreeView treev = new TreeView();
        treev.setPrefWidth(150);
        treev.setPrefHeight(100);
        newDetailedSection(
                new String[]{"Empty:", "ListView", "TableView", "TreeView", "TreeTableView"},
                lv, tv, treev,
                new TreeTableView() {{
                    setPrefSize(150, 100);
                }});
        Label label1 = new Label("This is a simple Tooltip.");
        label1.getStyleClass().add("tooltip");
        Label label2 = new Label("This is a simple Tooltip\nwith graphic.", createGraphic());
        label2.getStyleClass().add("tooltip");
        VBox vb = new VBox(4);
        vb.setFillWidth(true);
        Button button1 = new Button("Hover over me");
        button1.setTooltip(new Tooltip("This is a simple Tooltip."));
        Button button2 = new Button("me too");
        button2.setTooltip(new Tooltip("This is a simple Tooltip\nwith more than one line."));
        Button button3 = new Button("or me");
        Tooltip tip = new Tooltip("This is a simple Tooltip\nwith graphic.");
        tip.setGraphic(createGraphic());
        button3.setTooltip(tip);
        vb.getChildren().addAll(button1, button2, button3);

        newDetailedSection(
                new String[]{"ToolTip:", "inline", "inline + graphic", "popup"},
                label1, label2, vb);
        newSection(
                "MenuBar & ContextMenu:",
                createMenuBar(),
                createContextMenu()
                );
        newSection(
                "Menus:",
                createInlineMenu(false),
                createInlineMenu(true)
                );
        newSection(
                "AreaChart:",
                createAreaChart(false)
                );
        newSection(
                "StackedAreaChart:",
                createAreaChart(true)
                );
        newSection(
                "BarChart\nSimple:",
                createBarChart(false,true)
                );
        newSection(
                "BarChart:",
                createBarChart(false, false)
                );
        newSection(
                "BarChart\n(H, Simple):",
                createBarChart(true, true)
                );
        newSection(
                "BarChart\n(H):",
                createBarChart(true, false)
                );
        newSection(
                "StackedBarChart\nSimple:",
                createStackedBarChart(false,true)
                );
        newSection(
                "StackedBarChart\n(H, Simple):",
                createStackedBarChart(true, true)
        );
        newSection(
                "BubbleChart:",
                createBubbleChart(false)
        );
        newSection(
                "BubbleChart\nTop & Right Axis:",
                createBubbleChart(true)
        );
        newSection(
                "LineChart:",
                createLineChart()
        );
        newSection(
                "PieChar:",
                createPieChart()
        );
        newSection(
                "ScatterChart:",
                createScatterChart()
        );
    }

    public List<Section> getSections() {
        return sections;
    }

    private void newSection(String name, Node ...children) {
        newSection(name, 10, children);
    }

    private void newSection(String name, int spacing, Node ...children) {
        Label sectionLabel = new Label(name);
        sectionLabel.getStyleClass().add("section-label");
        sectionLabel.setMaxSize(Double.MAX_VALUE,Double.MAX_VALUE);
        HBox box = new HBox(spacing);
        box.getStyleClass().add("section-border");
        box.getChildren().addAll(children);
        setConstraints(sectionLabel, 0, rowIndex, 1, 1, HPos.CENTER, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS);
        setConstraints(box, 1, rowIndex++);
        getChildren().addAll(sectionLabel, box);
        sections.add(new Section(name, sectionLabel, box));
        content.put(name, box);
    }

    private void newDetailedSection(String[] labels, Node ...children) {
        Label sectionLabel = new Label(labels[0]);
        sectionLabel.getStyleClass().add("section-label");
        sectionLabel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        HBox hbox = new HBox(10);
        for (int n = 0; n < children.length; n++ ) {
            VBox vbox = new VBox(10);
            vbox.getStyleClass().add("section-border");
            vbox.setAlignment(Pos.CENTER);
            Label stateLabel = new Label(labels[n+1]);
            stateLabel.getStyleClass().add("section-label");
            vbox.getChildren().add(stateLabel);
            vbox.getChildren().add(children[n]);
            hbox.getChildren().addAll(vbox);
        }
        setConstraints(sectionLabel, 0, rowIndex,1,1, HPos.CENTER, VPos.CENTER, Priority.ALWAYS,Priority.ALWAYS);
        setConstraints(hbox, 1, rowIndex++);
        getChildren().addAll(sectionLabel, hbox);
        sections.add(new Section(labels[0], sectionLabel, hbox));
        content.put(labels[0], hbox);
    }

    public Map<String, Node> getContent() {
        return content;
    }

    public static class Section {
        public final String name;
        public final Label label;
        public final Node box;

        public Section(String name, Label label, Node box) {
            this.name = name;
            this.label = label;
            this.box = box;
        }

        @Override public String toString() {
            return name.replaceAll("\n"," ");
        }
    }
}
