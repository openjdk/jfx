/*
 * Copyright (c) 2008, 2012 Oracle and/or its affiliates.
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
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBuilder;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckBoxBuilder;
import javafx.scene.control.ChoiceBoxBuilder;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ColorPickerBuilder;
import javafx.scene.control.ComboBoxBuilder;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.LabelBuilder;
import javafx.scene.control.ListViewBuilder;
import javafx.scene.control.MenuButtonBuilder;
import javafx.scene.control.PasswordFieldBuilder;
import javafx.scene.control.ProgressBarBuilder;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ProgressIndicatorBuilder;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollBarBuilder;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPaneBuilder;
import javafx.scene.control.SeparatorBuilder;
import javafx.scene.control.Slider;
import javafx.scene.control.SliderBuilder;
import javafx.scene.control.SplitMenuButtonBuilder;
import javafx.scene.control.TableViewBuilder;
import javafx.scene.control.TextAreaBuilder;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFieldBuilder;
import javafx.scene.control.TitledPaneBuilder;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleButtonBuilder;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TooltipBuilder;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeViewBuilder;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.HBoxBuilder;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.VBoxBuilder;
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
        newSection("Default Button:",
                ButtonBuilder.create().text("Button").defaultButton(true).build(),
                withState(new Button("Hover"), "default, hover"),
                withState(new Button("Armed"), "default, armed"),
                withState(new Button("Focused"), "default, focused"),
                withState(new Button("Focused & Hover"), "default, focused, hover"),
                withState(new Button("Focused & Armed"), "default, focused, armed"),
                withState(new Button("Disabled"), "default, disabled"));
        newSection("Nice Colors:",
                ButtonBuilder.create().text("Button").style("-fx-base: #f3622d;").build(),
                ButtonBuilder.create().text("Button").style("-fx-base: #fba71b;").build(),
                ButtonBuilder.create().text("Button").style("-fx-base: #57b757;").build(),
                ButtonBuilder.create().text("Button").style("-fx-base: #41a9c9;").build(),
                ButtonBuilder.create().text("Button").style("-fx-base: #888;").build());
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
        newSection("Pill Toggle\nButtons:",
                HBoxBuilder.create()
                    .children(
                        ToggleButtonBuilder.create().text("Left").styleClass("left-pill").toggleGroup(tg1).build(),
                        ToggleButtonBuilder.create().text("Center").styleClass("center-pill").toggleGroup(tg1).build(),
                        ToggleButtonBuilder.create().text("Right").styleClass("right-pill").toggleGroup(tg1).build()
                    )
                    .build(),
                HBoxBuilder.create()
                    .children(
                        ToggleButtonBuilder.create().text("Left").styleClass("left-pill").toggleGroup(tg2).selected(true).build(),
                        ToggleButtonBuilder.create().text("Center").styleClass("center-pill").toggleGroup(tg2).build(),
                        ToggleButtonBuilder.create().text("Right").styleClass("right-pill").toggleGroup(tg2).build()
                    )
                    .build(),
                HBoxBuilder.create()
                    .children(
                        ToggleButtonBuilder.create().text("Left").styleClass("left-pill").toggleGroup(tg3).build(),
                        ToggleButtonBuilder.create().text("Center").styleClass("center-pill").toggleGroup(tg3).selected(true).build(),
                        ToggleButtonBuilder.create().text("Right").styleClass("right-pill").toggleGroup(tg3).build()
                    )
                    .build(),
                HBoxBuilder.create()
                    .children(
                        ToggleButtonBuilder.create().text("Left").styleClass("left-pill").toggleGroup(tg4).build(),
                        ToggleButtonBuilder.create().text("Center").styleClass("center-pill").toggleGroup(tg4).build(),
                        ToggleButtonBuilder.create().text("Right").styleClass("right-pill").toggleGroup(tg4).selected(true).build()
                    )
                    .build());
        ToggleGroup tg5 = new ToggleGroup();
        ToggleGroup tg6 = new ToggleGroup();
        ToggleGroup tg7 = new ToggleGroup();
        ToggleGroup tg8 = new ToggleGroup();
        newSection("Pill Toggle\nButtons\nFocused:",
                HBoxBuilder.create()
                    .children(
                        ToggleButtonBuilder.create().text("#").styleClass("left-pill").toggleGroup(tg5).build(),
                        ToggleButtonBuilder.create().text("#").styleClass("center-pill").toggleGroup(tg5).build(),
                        ToggleButtonBuilder.create().text("#").styleClass("right-pill").toggleGroup(tg5).build()
                    )
                    .build(),
                HBoxBuilder.create()
                    .children(
                        withState(ToggleButtonBuilder.create().text("L").styleClass("left-pill").toggleGroup(tg5).build(),"focused"),
                        ToggleButtonBuilder.create().text("C").styleClass("center-pill").toggleGroup(tg5).build(),
                        ToggleButtonBuilder.create().text("R").styleClass("right-pill").toggleGroup(tg5).build()
                    )
                    .build(),
                HBoxBuilder.create()
                    .children(
                        ToggleButtonBuilder.create().text("L").styleClass("left-pill").toggleGroup(tg5).build(),
                        withState(ToggleButtonBuilder.create().text("C").styleClass("center-pill").toggleGroup(tg5).build(),"focused"),
                        ToggleButtonBuilder.create().text("R").styleClass("right-pill").toggleGroup(tg5).build()
                    )
                    .build(),
                HBoxBuilder.create()
                    .children(
                        ToggleButtonBuilder.create().text("L").styleClass("left-pill").toggleGroup(tg5).build(),
                        ToggleButtonBuilder.create().text("C").styleClass("center-pill").toggleGroup(tg5).build(),
                        withState(ToggleButtonBuilder.create().text("R").styleClass("right-pill").toggleGroup(tg5).build(),"focused")
                    )
                    .build(),
                HBoxBuilder.create()
                    .children(
                        withState(ToggleButtonBuilder.create().text("L").styleClass("left-pill").toggleGroup(tg6).selected(true).build(),"focused"),
                        ToggleButtonBuilder.create().text("C").styleClass("center-pill").toggleGroup(tg6).build(),
                        ToggleButtonBuilder.create().text("R").styleClass("right-pill").toggleGroup(tg6).build()
                    )
                    .build(),
                HBoxBuilder.create()
                    .children(
                        ToggleButtonBuilder.create().text("L").styleClass("left-pill").toggleGroup(tg7).build(),
                        withState(ToggleButtonBuilder.create().text("C").styleClass("center-pill").toggleGroup(tg7).selected(true).build(),"focused"),
                        ToggleButtonBuilder.create().text("R").styleClass("right-pill").toggleGroup(tg7).build()
                    )
                    .build(),
                HBoxBuilder.create()
                    .children(
                        ToggleButtonBuilder.create().text("L").styleClass("left-pill").toggleGroup(tg8).build(),
                        ToggleButtonBuilder.create().text("C").styleClass("center-pill").toggleGroup(tg8).build(),
                        withState(ToggleButtonBuilder.create().text("R").styleClass("right-pill").toggleGroup(tg8).selected(true).build(),"focused")
                    )
                    .build());
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
        newSection("CheckBox\nIndeterminate:",
                CheckBoxBuilder.create().text("CheckBox").selected(true).indeterminate(true).allowIndeterminate(true).build(),
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
        newSection(
                "ChoiceBox:",
                ChoiceBoxBuilder.<String>create().items(sampleItems()).value("Item A").build(),
                ChoiceBoxBuilder.<String>create().items(choiceBoxLongList).value("Long List").build(),
                withState(ChoiceBoxBuilder.<String>create().items(sampleItems()).value("Item B").build(), "hover"),
                withState(ChoiceBoxBuilder.<String>create().items(sampleItems()).value("Item B").build(), "showing"),
                withState(ChoiceBoxBuilder.<String>create().items(sampleItems()).value("Item B").build(), "focused"),
                ChoiceBoxBuilder.<String>create().items(sampleItems()).value("Item C").disable(true).build()
        );
        newSection(
                "ComboBox:",
                ComboBoxBuilder.<String>create().items(sampleItems()).value("Item A").build(),
                ComboBoxBuilder.<String>create().items(choiceBoxLongList).value("Long List").build(),
                withState(ComboBoxBuilder.<String>create().items(sampleItems()).value("Item B").build(), "hover"),
                withState(ComboBoxBuilder.<String>create().items(sampleItems()).value("Item B").build(), "showing"),
                withState(ComboBoxBuilder.<String>create().items(sampleItems()).value("Item B").build(), "focused"),
                ComboBoxBuilder.<String>create().items(sampleItems()).value("Item C").disable(true).build()
                );
        newSection(
                "ComboBox\nEditable:",
                ComboBoxBuilder.<String>create().items(sampleItems()).value("Item A").editable(true).build(),
                withState(ComboBoxBuilder.<String>create().items(sampleItems()).value("Item B").editable(true).build(), "editable", ".arrow-button", "hover"),
                withState(ComboBoxBuilder.<String>create().items(sampleItems()).value("Item B").editable(true).build(), "editable", ".arrow-button", "pressed")
                );
        newSection(
                "ComboBox\nEditable\n(More):",
                withState(ComboBoxBuilder.<String>create().items(sampleItems()).value("Item B").editable(true).build(), "editable,contains-focus", ".text-field", "focused"),
                ComboBoxBuilder.<String>create().items(sampleItems()).value("Item C").editable(true).disable(true).build()
                );
        newSection(
                "Color Picker:",
                ColorPickerBuilder.create().value(Color.RED).build(),
                withState(ColorPickerBuilder.create().value(Color.RED).build(), "hover"),
                withState(ColorPickerBuilder.create().value(Color.RED).build(), "showing"),
                withState(ColorPickerBuilder.create().value(Color.RED).build(), "focused"),
                withState(ColorPickerBuilder.create().value(Color.RED).build(), "disabled")
                );
        newSection(
                "Color Picker\n Split Button:",
                ColorPickerBuilder.create().value(Color.RED).styleClass(ColorPicker.STYLE_CLASS_SPLIT_BUTTON).build(),
                withState(ColorPickerBuilder.create().value(Color.RED).styleClass(ColorPicker.STYLE_CLASS_SPLIT_BUTTON).build(), "hover"),
                withState(ColorPickerBuilder.create().value(Color.RED).styleClass(ColorPicker.STYLE_CLASS_SPLIT_BUTTON).build(), "showing"),
                withState(ColorPickerBuilder.create().value(Color.RED).styleClass(ColorPicker.STYLE_CLASS_SPLIT_BUTTON).build(), "focused"),
                withState(ColorPickerBuilder.create().value(Color.RED).styleClass(ColorPicker.STYLE_CLASS_SPLIT_BUTTON).build(), "disabled")
                );
        newSection(
                "MenuButton:",
                MenuButtonBuilder.create().items(createMenuItems(20)).text("right").popupSide(Side.RIGHT).build(),
                MenuButtonBuilder.create().items(createMenuItems(20)).text("normal").build(),
                withState(MenuButtonBuilder.create().items(createMenuItems(20)).text("hover").build(), "openvertically,hover"),
                withState(MenuButtonBuilder.create().items(createMenuItems(20)).text("armed").build(), "openvertically,armed"),
                withState(MenuButtonBuilder.create().items(createMenuItems(20)).text("focused").build(), "openvertically,focused"),
                withState(MenuButtonBuilder.create().items(createMenuItems(20)).text("disabled").build(), "openvertically,disabled")
                );
        newSection(
                "SplitMenuButton:",
                SplitMenuButtonBuilder.create().items(createMenuItems(20)).text("right").popupSide(Side.RIGHT).build(),
                SplitMenuButtonBuilder.create().items(createMenuItems(20)).text("normal").build(),
                withState(SplitMenuButtonBuilder.create().items(createMenuItems(20)).text("hover").build(),"openvertically",".label", "hover"),
                withState(SplitMenuButtonBuilder.create().items(createMenuItems(20)).text("armed").build(),"armed,openvertically",".label", "armed")
                );
        newSection(
                "SplitMenuButton\nMore:",
                withState(SplitMenuButtonBuilder.create().items(createMenuItems(20)).text("arrow hover").build(),"openvertically",".arrow-button", "hover"),
                withState(SplitMenuButtonBuilder.create().items(createMenuItems(20)).text("showing").build(), "openvertically,showing"),
                withState(SplitMenuButtonBuilder.create().items(createMenuItems(20)).text("focused").build(), "openvertically,focused"),
                withState(SplitMenuButtonBuilder.create().items(createMenuItems(20)).text("disabled").build(), "openvertically,disabled")
                );
        newDetailedSection(
                new String[]{"Slider (H):", "normal", "hover", "pressed", "disabled", "tickmarks"},
                withState(SliderBuilder.create().maxWidth(90).min(0).max(100).value(50).build(), null),
                withState(SliderBuilder.create().maxWidth(90).min(0).max(100).value(50).build(), null, ".thumb", "hover"),
                withState(SliderBuilder.create().maxWidth(90).min(0).max(100).value(50).build(), null, ".thumb", "hover, pressed"),
                withState(SliderBuilder.create().maxWidth(90).min(0).max(100).value(50).build(), "disabled"),
                SliderBuilder.create().min(0).max(100).value(50).showTickMarks(true).showTickLabels(true).build());
        newDetailedSection(
                new String[]{"Slider (H) Focused:", "normal", "hover", "pressed"},
                withState(new Slider(0, 100, 50), "focused"),
                withState(new Slider(0, 100, 50), "focused", ".thumb", "hover"),
                withState(new Slider(0, 100, 50), "focused", ".thumb", "hover, pressed"));
        newSection("Slider - V:",
                SliderBuilder.create().min(0).max(100).value(50).orientation(Orientation.VERTICAL).build(),
                withState(SliderBuilder.create().min(0).max(100).value(50).orientation(Orientation.VERTICAL).build(), null, ".thumb", "hover"),
                withState(SliderBuilder.create().min(0).max(100).value(50).orientation(Orientation.VERTICAL).build(), null, ".thumb", "hover, pressed"),
                withState(SliderBuilder.create().min(0).max(100).value(50).orientation(Orientation.VERTICAL).build(), "disabled"),
                SliderBuilder.create().min(0).max(100).value(50).showTickMarks(true).showTickLabels(true).orientation(Orientation.VERTICAL).build());
        newDetailedSection(
                new String[] {"Scrollbar - H: ", "normal", "focused", "small", "big thumb"},
                new ScrollBar(),
                withState(ScrollBarBuilder.create().build(), "focused"),
                ScrollBarBuilder.create().minWidth(30).prefWidth(30).build(),
                ScrollBarBuilder.create().visibleAmount(60).max(100).build()
                );
        newDetailedSection(
                new String[] {"Scrollbar - V: ", "normal", "focused", "small", "btn hover", "btn pressed", ".thumb hover", ".thumb pressed"},
                withState(ScrollBarBuilder.create().orientation(Orientation.VERTICAL).build(), "vertical"),
                withState(ScrollBarBuilder.create().orientation(Orientation.VERTICAL).build(), "focused"),
                withState(ScrollBarBuilder.create().orientation(Orientation.VERTICAL).minHeight(30).prefHeight(30).build(), "vertical"),
                withState(ScrollBarBuilder.create().orientation(Orientation.VERTICAL).build(), "vertical", ".decrement-button", "hover"),
                withState(ScrollBarBuilder.create().orientation(Orientation.VERTICAL).build(), "vertical", ".decrement-button", "pressed"),
                withState(ScrollBarBuilder.create().orientation(Orientation.VERTICAL).build(), "vertical", ".thumb", "hover"),
                withState(ScrollBarBuilder.create().orientation(Orientation.VERTICAL).build(), "vertical", ".thumb", "pressed")
                );
        newDetailedSection(
                new String[] {"ScrollPane: ", "normal", "small", "focused", "empty"},
                ScrollPaneBuilder.create().content(scrollPaneContent()).build(),
                ScrollPaneBuilder.create().content(scrollPaneContent()).minWidth(40).prefWidth(40).minHeight(40).prefHeight(40).build(),
                withState(ScrollPaneBuilder.create().content(scrollPaneContent()).build(), "focused"),
                ScrollPaneBuilder.create().build()
                );
        newDetailedSection(
                new String[] {"ScrollPane H/V: ", "H Bar", "V bar"},
                ScrollPaneBuilder.create().content(scrollPaneContent()).vbarPolicy(ScrollPane.ScrollBarPolicy.NEVER).build(),
                ScrollPaneBuilder.create().content(scrollPaneContent()).hbarPolicy(ScrollPane.ScrollBarPolicy.NEVER).build()
                );
        newDetailedSection(
                new String[] {"Separator: ", "horizontal", "vertical"},
                SeparatorBuilder.create().prefWidth(100).build(),
                SeparatorBuilder.create().orientation(Orientation.VERTICAL).prefHeight(50).build()
                );
        newDetailedSection(
                new String[] {"ProgressBar: ", "normal", "disabled", "indeterminate"},
                ProgressBarBuilder.create().progress(0.6).prefWidth(200).build(),
                withState(ProgressBarBuilder.create().progress(0.2).prefWidth(200).build(), "disabled"),
                ProgressBarBuilder.create().progress(-1).prefWidth(200).build()
                );
        newDetailedSection(
                new String[] {"ProgressIndicator: ", "normal 0%", "normal 60%", "normal 100%", "disabled"},
                new ProgressIndicator(0),
                new ProgressIndicator(0.6),
                new ProgressIndicator(1),
                withState(new ProgressIndicator(0.5), "disabled")
                );
        newDetailedSection(
                new String[] {"ProgressIndicator\nIndeterminate: ", "normal", "small", "large", "disabled"},
                ProgressIndicatorBuilder.create().progress(-1).maxWidth(USE_PREF_SIZE).maxHeight(USE_PREF_SIZE).build(),
                ProgressIndicatorBuilder.create().progress(-1).prefWidth(30).prefHeight(30).build(),
                ProgressIndicatorBuilder.create().progress(-1).prefWidth(60).prefHeight(60).build(),
                ProgressIndicatorBuilder.create().progress(-1).maxWidth(USE_PREF_SIZE).maxHeight(USE_PREF_SIZE).disable(true).build()
                );
        newSection(
                "TextField:",
                new TextField("TextField"),
                TextFieldBuilder.create().promptText("Prompt Text").build(),
                withState(new TextField("Focused"), "focused"),
                withState(new TextField("Disabled"), "disabled")
        );
        newSection(
                "PasswordField:",
                PasswordFieldBuilder.create().text("Password").build(),
                PasswordFieldBuilder.create().promptText("Prompt Text").build(),
                withState(PasswordFieldBuilder.create().text("Password").build(), "focused"),
                withState(PasswordFieldBuilder.create().text("Password").build(), "disabled")
        );
        newSection(
                "TextArea:",
                TextAreaBuilder.create().text("TextArea").prefColumnCount(10).prefRowCount(2).build(),
                TextAreaBuilder.create().text("Many Lines of\nText.\n#3\n#4\n#5\n#6\n#7\n#8\n#9\n#10").prefColumnCount(10).prefRowCount(5).build(),
                TextAreaBuilder.create().text("Many Lines of\nText.\n#3\n#4\n#5\n#6\n#7\n#8\n#9\n#10").prefColumnCount(6).prefRowCount(5).build(),
                TextAreaBuilder.create().promptText("Prompt Text").prefColumnCount(10).prefRowCount(2).build(),
                withState(TextAreaBuilder.create().text("Focused").prefColumnCount(7).prefRowCount(2).build(), "focused"),
                withState(TextAreaBuilder.create().text("Disabled").prefColumnCount(8).prefRowCount(2).build(), "disabled")
        );
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
        newDetailedSection(
                new String[] { "TitledPane:", "normal", "not collapsible", "hover", "focused", "disabled" },
                TitledPaneBuilder.create().text("Title").content(new Label("Content\nLine2.")).build(),
                TitledPaneBuilder.create().text("Not Collapsible").content(new Label("Content\nLine2.")).collapsible(false).build(),
                withState(TitledPaneBuilder.create().text("Title").content(new Label("Content\nLine2.")).build(), "hover"),
                withState(TitledPaneBuilder.create().text("Title").content(new Label("Content\nLine2.")).build(), "focused"),
                withState(TitledPaneBuilder.create().text("Title").content(new Label("Content\nLine2.")).build(), "disabled")
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
        newDetailedSection(
                new String[] {"Empty:", "ListView", "TableView", "TreeView", "TreeTableView"},
                ListViewBuilder.<String>create().prefWidth(150).prefHeight(100).build(),
                TableViewBuilder.create().prefWidth(150).prefHeight(100).build(),
                TreeViewBuilder.create().prefWidth(150).prefHeight(100).build(),
                new TreeTableView() {{
                    setPrefSize(150, 100);
                }}
                );
        newDetailedSection(
                new String[] {"ToolTip:","inline","inline + graphic", "popup"},
                LabelBuilder.create().text("This is a simple Tooltip.").styleClass("tooltip").build(),
                LabelBuilder.create().text("This is a simple Tooltip\nwith graphic.").graphic(createGraphic()).styleClass("tooltip").build(),
                VBoxBuilder.create().fillWidth(true).spacing(4).children(
                    ButtonBuilder.create().text("Hover over me").tooltip(new Tooltip("This is a simple Tooltip.")).build(),
                    ButtonBuilder.create().text("me too").tooltip(new Tooltip("This is a simple Tooltip\nwith more than one line.")).build(),
                    ButtonBuilder.create().text("or me").tooltip(TooltipBuilder.create().text("This is a simple Tooltip\nwith graphic.").graphic(createGraphic()).build()).build()
                ).build()
                );
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
