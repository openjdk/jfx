/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package modena;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBuilder;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleButtonBuilder;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.HBoxBuilder;

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
        box.getChildren().addAll(children);
        setConstraints(sectionLabel, 0, rowIndex);
        setConstraints(box, 1, rowIndex++);
        getChildren().addAll(sectionLabel,box);
    }
    
    public SamplePage() {
        setVgap(15);
        setHgap(15);
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
        newSection("Slider:", 
                new Slider(0,100,50),
                withState(new Slider(0,100,50), null, ".thumb", "hover"),
                withState(new Slider(0,100,50), null, ".thumb", "hover, pressed"),
                withState(new Slider(0,100,50), "disabled"));
        newSection("Slider Focused:", 
                withState(new Slider(0,100,50), "focused"),
                withState(new Slider(0,100,50), "focused", ".thumb", "hover"),
                withState(new Slider(0,100,50), "focused", ".thumb", "hover, pressed"));
    }
}
