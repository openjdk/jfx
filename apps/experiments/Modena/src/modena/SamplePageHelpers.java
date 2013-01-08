/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package modena;

import java.util.ArrayList;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.GroupBuilder;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.Pagination;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabBuilder;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPaneBuilder;
import javafx.scene.control.ToolBar;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineBuilder;
import javafx.scene.shape.RectangleBuilder;
import javafx.util.Callback;

/**
 * Helper static methods for Sample Page
 */
public class SamplePageHelpers {
    
    private static final String[] LETTERS = new String[]{"A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z"};
    
    static ObservableList<String> sampleItems() {
        return sampleItems(7);
    }
    
    static ObservableList<String> sampleItems(int numberOfItems) {
        ArrayList<String> items = new ArrayList<String>();
        if (numberOfItems < 26) {
            for(int i=0; i<numberOfItems; i++) {
                items.add("Item "+LETTERS[i]);
            }
        } else {
            for(int i=0; i<numberOfItems; i++) {
                items.add("Item "+i);
            }
        }
        return FXCollections.observableArrayList(items);
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
    
    static SplitPane createSplitPane(int numOfItems, boolean vertical, Node firstItem) {
        SplitPane splitPane = new SplitPane();
        if(vertical) splitPane.setOrientation(Orientation.VERTICAL);
        if (firstItem != null) splitPane.getItems().add(firstItem);
        for (int i=1; i<=numOfItems; i++) {
            splitPane.getItems().add(new Label("Item "+i));
        }
        splitPane.setPrefSize(150, 150);
        return splitPane;
    }
    
    static Pagination createPagination(int numOfPages, boolean bullet, boolean arrows) {
        Pagination pagination = new Pagination(numOfPages);
        if (bullet) pagination.getStyleClass().add("bullet");
        if (!arrows) pagination.setStyle("-fx-arrows-visible:false;");
        pagination.setPageFactory(new Callback<Integer, Node>() {
            @Override public Node call(Integer param) {
                return new Label("Page Label "+param);
            }
        });
        return pagination;
    }
    
    static ListView<String> createListView(int numOfItems, boolean multipleSelection, boolean disable) {
        ListView<String> listView = new ListView<String>();
        listView.setPrefHeight((24*7)+4);
        listView.setPrefWidth(140);
        listView.getItems().addAll(sampleItems(numOfItems));
        listView.setDisable(disable);
        if (multipleSelection) {
            listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            listView.getSelectionModel().selectRange(1, 5);
        } else {
            listView.getSelectionModel().select(1);
        }
        return listView;
    }
}
