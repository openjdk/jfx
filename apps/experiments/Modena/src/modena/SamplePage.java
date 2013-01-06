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

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.GroupBuilder;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBuilder;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBoxBuilder;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.PasswordFieldBuilder;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ProgressIndicatorBuilder;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollBarBuilder;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPaneBuilder;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorBuilder;
import javafx.scene.control.Slider;
import javafx.scene.control.SliderBuilder;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextAreaBuilder;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFieldBuilder;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleButtonBuilder;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.HBoxBuilder;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineBuilder;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.RectangleBuilder;

/**
 * Page showing every control in every state
 */
public class SamplePage extends GridPane {
    private int rowIndex = 0;
    
    private Node withState(Node node, String state) {
        node.getProperties().put("javafx.scene.Node.pseudoClassOverride", state);
        return node;
    }
    
    private Node withState(final Node node, final String state, final String subNodeStyleClass, final String subNodeState) {
        if (state!=null) node.getProperties().put("javafx.scene.Node.pseudoClassOverride", state);
        Platform.runLater(new Runnable() {
            @Override public void run() {
                node.lookup(subNodeStyleClass).getProperties().put("javafx.scene.Node.pseudoClassOverride", subNodeState);
            }
        });
        return node;
    }
    
    private void newSection(String name, Node ...children) {
        Label sectionLabel = new Label(name);
        sectionLabel.getStyleClass().add("section-label");
        HBox box = new HBox(10);
        box.getStyleClass().add("section-border");
        box.getChildren().addAll(children);
        setConstraints(sectionLabel, 0, rowIndex);
        setConstraints(box, 1, rowIndex++);
        getChildren().addAll(sectionLabel,box);
    }
    
    private void newDetailedSection(String[] labels, Node ...children) {
        Label sectionLabel = new Label(labels[0]);
        sectionLabel.getStyleClass().add("section-label");
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
        setConstraints(sectionLabel, 0, rowIndex);
        setConstraints(hbox, 1, rowIndex++);
        getChildren().addAll(sectionLabel,hbox);
    }
    
    private static ObservableList<String> sampleItems() {
        return FXCollections.observableArrayList("Item A","Item B","Item C",
                "Item D","Item E","Item F","Item G");
    }
    
    private static Node scrollPaneContent() {
        return GroupBuilder.create().children(
                RectangleBuilder.create().width(200).height(200).fill(Color.PALETURQUOISE).build(),
                LineBuilder.create().endX(200).endY(200).stroke(Color.DODGERBLUE).build(),
                LineBuilder.create().startX(200).endX(0).endY(200).stroke(Color.DODGERBLUE).build()
            ).build();
    }
    
    public SamplePage() {
        setVgap(25);
        setHgap(25);
        setPadding(new Insets(20));
        newSection("Button:", 
                new Button("Button"),
                withState(new Button("Hover"), "hover"),
                withState(new Button("Armed"), "armed"),
                withState(new Button("Focused"), "focused"),
                withState(new Button("Focused & Hover"), "focused, hover"),
                withState(new Button("Focused & Armed"), "focused, armed"),
                withState(new Button("Disabled"), "disabled"));
        newSection("Default Button:", 
                withState(new Button("Button"), "default, hover"),
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
                ButtonBuilder.create().text("Button").style("-fx-base: #57b757;").build(),
                ButtonBuilder.create().text("Button").style("-fx-base: #41a9c9;").build(),
                ButtonBuilder.create().text("Button").style("-fx-base: #888;").build());
        ToggleGroup tg1 = new ToggleGroup();
        ToggleGroup tg2 = new ToggleGroup();
        ToggleGroup tg3 = new ToggleGroup();
        ToggleGroup tg4 = new ToggleGroup();
        newSection("Pill Buttons:", 
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
        newSection("CheckBox Indeterminate:", 
                withState(new CheckBox("CheckBox"), "indeterminate, selected"),
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
        newSection("RadioButton Selected:", 
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
        newSection("Slider:", 
                new Slider(0,100,50),
                withState(new Slider(0,100,50), null, ".thumb", "hover"),
                withState(new Slider(0,100,50), null, ".thumb", "hover, pressed"),
                withState(new Slider(0,100,50), "disabled"));
        newSection("Slider Focused:", 
                withState(new Slider(0,100,50), "focused"),
                withState(new Slider(0,100,50), "focused", ".thumb", "hover"),
                withState(new Slider(0,100,50), "focused", ".thumb", "hover, pressed"));
        newSection("Slider (V):", 
                SliderBuilder.create().min(0).max(100).value(50).orientation(Orientation.VERTICAL).build(),
                withState(SliderBuilder.create().min(0).max(100).value(50).orientation(Orientation.VERTICAL).build(), null, ".thumb", "hover"),
                withState(SliderBuilder.create().min(0).max(100).value(50).orientation(Orientation.VERTICAL).build(), null, ".thumb", "hover, pressed"),
                withState(SliderBuilder.create().min(0).max(100).value(50).orientation(Orientation.VERTICAL).build(), "disabled"));
        newDetailedSection(
                new String[] {"Scrollbar - H: ", "normal", "small", "big thumb"}, 
                new ScrollBar(),
                ScrollBarBuilder.create().minWidth(30).prefWidth(30).build(),
                ScrollBarBuilder.create().visibleAmount(60).max(100).build()
                );
        newDetailedSection(
                new String[] {"Scrollbar - V: ", "normal", "small", "btn hover", "btn pressed", ".thumb hover", ".thumb pressed"}, 
                withState(ScrollBarBuilder.create().orientation(Orientation.VERTICAL).build(), "vertical"),
                withState(ScrollBarBuilder.create().orientation(Orientation.VERTICAL).minHeight(30).prefHeight(30).build(), "vertical"),
                withState(ScrollBarBuilder.create().orientation(Orientation.VERTICAL).build(), "vertical", ".decrement-button", "hover"),
                withState(ScrollBarBuilder.create().orientation(Orientation.VERTICAL).build(), "vertical", ".decrement-button", "pressed"),
                withState(ScrollBarBuilder.create().orientation(Orientation.VERTICAL).build(), "vertical", ".thumb", "hover"),
                withState(ScrollBarBuilder.create().orientation(Orientation.VERTICAL).build(), "vertical", ".thumb", "pressed")
                );
        newDetailedSection(
                new String[] {"ScrollPane: ", "normal", "small", "focused"}, 
                ScrollPaneBuilder.create().content(scrollPaneContent()).build(),
                ScrollPaneBuilder.create().content(scrollPaneContent()).minWidth(40).prefWidth(40).minHeight(40).prefHeight(40).build(),
                withState(ScrollPaneBuilder.create().content(scrollPaneContent()).build(), "focused")
                ); 
        newDetailedSection(
                new String[] {"Separator: ", "horizontal", "vertical"}, 
                SeparatorBuilder.create().prefWidth(100).build(),
                SeparatorBuilder.create().orientation(Orientation.VERTICAL).prefHeight(50).build()
                );
        newDetailedSection(
                new String[] {"ProgressBar: ", "normal", "disabled", "indeterminate"}, 
                new ProgressBar(0.6),
                withState(new ProgressBar(), "disabled"),
                new ProgressBar(-1)
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
                TextAreaBuilder.create().text("Many Lines of\nText.\n#3\n#4\n#5\n#6\n#7\n#8\n#9\n#10").prefColumnCount(10).prefRowCount(3).build(),
                TextAreaBuilder.create().text("Many Lines of\nText.\n#3\n#4\n#5\n#6\n#7\n#8\n#9\n#10").prefColumnCount(6).prefRowCount(3).build(),
                TextAreaBuilder.create().promptText("Prompt Text").prefColumnCount(10).prefRowCount(2).build(),
                withState(TextAreaBuilder.create().text("Focused").prefColumnCount(7).prefRowCount(2).build(), "focused"),
                withState(TextAreaBuilder.create().text("Disabled").prefColumnCount(8).prefRowCount(2).build(), "disabled")
                );
        newSection(      
                "ChoiceBox:", 
                ChoiceBoxBuilder.create(String.class).items(sampleItems()).value("Item A").build(),
                withState(ChoiceBoxBuilder.create(String.class).items(sampleItems()).value("Item B").build(), "hover"),
                withState(ChoiceBoxBuilder.create(String.class).items(sampleItems()).value("Item B").build(), "showing"),
                withState(ChoiceBoxBuilder.create(String.class).items(sampleItems()).value("Item B").build(), "focused"),
                withState(ChoiceBoxBuilder.create(String.class).items(sampleItems()).value("Item C").build(), "disabled")
                );
    }
}
