/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package modena;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.GroupBuilder;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TabBuilder;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPaneBuilder;
import javafx.scene.control.ToolBar;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineBuilder;
import javafx.scene.shape.RectangleBuilder;

/**
 * Helper static methods for Sample Page
 */
public class SamplePageHelpers {
    
    static ObservableList<String> sampleItems() {
        return FXCollections.observableArrayList("Item A","Item B","Item C",
                "Item D","Item E","Item F","Item G");
    }
    
    static Node scrollPaneContent() {
        return GroupBuilder.create().children(
                RectangleBuilder.create().width(200).height(200).fill(Color.PALETURQUOISE).build(),
                LineBuilder.create().endX(200).endY(200).stroke(Color.DODGERBLUE).build(),
                LineBuilder.create().startX(200).endX(0).endY(200).stroke(Color.DODGERBLUE).build()
            ).build();
    }
    
    static TabPane createTabPane(int numOfTabs, int prefWidth, String firstTabText, boolean floating) {
        TabPane tabPane = new TabPane();
        if (floating) tabPane.getStyleClass().add("floating");
        for (int i=1; i<=numOfTabs; i++) {
            tabPane.getTabs().add(
                TabBuilder.create()
                    .text("Tab "+i)
                    .content(new Label((i==1 && firstTabText!=null)? firstTabText :"Tab "+i+" Content"))
                    .build()
            );
        }
        tabPane.setPrefWidth(prefWidth);
        tabPane.setPrefHeight(100);
        return tabPane;
    }
    
    static ToolBar createToolBar(boolean vertical, boolean overFlow) {
        ToolBar toolBar = new ToolBar();
        if (vertical) toolBar.setOrientation(Orientation.VERTICAL);
        toolBar.getItems().addAll(
                new Button("A"),
                new Button("B"),
                new Separator()
        );
        if (vertical) {
            toolBar.getItems().addAll(
                new Button("C"),
                new Button("D")
            );
        } else {
            toolBar.getItems().addAll(
                new Label("Search:"),
                new TextField()
            );
        }
        if (overFlow) {
            if (vertical) {
                toolBar.setPrefHeight(80);
            } else {
                toolBar.setPrefWidth(80);
            }
        }
        return toolBar;
    }
    
    static Accordion createAccordion() {
        Accordion accordian = new Accordion();
        accordian.getPanes().addAll(
            TitledPaneBuilder.create().text("Title 1").content(new Label("Content\nLine2.")).build(),
            TitledPaneBuilder.create().text("Title 2").content(new Label("Content\nLine2.")).build(),
            TitledPaneBuilder.create().text("Title 3").content(new Label("Content\nLine2.")).build()
        );
        return accordian;
    }
}
