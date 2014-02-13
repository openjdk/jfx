/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package hello;


import java.text.NumberFormat;
import java.util.AbstractList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;

import javafx.application.Application;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ListView.EditEvent;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.ChoiceBoxListCell;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.input.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;

public class HelloListView extends Application implements InvalidationListener {

    private static ObservableList<String> data = FXCollections.<String>observableArrayList();
    private static ObservableList<String> names = FXCollections.<String>observableArrayList();
    private static ObservableList<Number> money = FXCollections.<Number>observableArrayList();
    private static ObservableList<Map<String, String>> mapData = FXCollections.<Map<String, String>>observableArrayList();

    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";

    static {
        data.addAll("Row 1", "Row 2",
//                "Long Long Long Long Long Long Long Long Long Long Long Long Long Long Long Long Long Long Long Long Long Long Long Long Long Long Long Long Long Long Long Row 3",
                "Row 4", "Row 5", "Row 6",
                "Row 7", "Row 8", "Row 9", "Row 10", "Row 11", "Row 12", "Row 13",
                "Row 14", "Row 15", "Row 16", "Row 17", "Row 18", "Row 19", "Row 20",

                "Row 1", "Row 2", "Long Row 3", "Row 4", "Row 5", "Row 6",
                "Row 7", "Row 8", "Row 9", "Row 10", "Row 11", "Row 12", "Row 13",
                "Row 14", "Row 15", "Row 16", "Row 17", "Row 18", "Row 19", "Row 20",

                "Row 1", "Row 2", "Long Row 3", "Row 4", "Row 5", "Row 6",
                "Row 7", "Row 8", "Row 9", "Row 10", "Row 11", "Row 12", "Row 13",
                "Row 14", "Row 15", "Row 16", "Row 17", "Row 18", "Row 19", "Row 20",

                "Row 1", "Row 2", "Long Row 3", "Row 4", "Row 5", "Row 6",
                "Row 7", "Row 8", "Row 9", "Row 10", "Row 11", "Row 12", "Row 13",
                "Row 14", "Row 15", "Row 16", "Row 17", "Row 18", "Row 19", "Row 20",

                "Row 1", "Row 2", "Long Row 3", "Row 4", "Row 5", "Row 6",
                "Row 7", "Row 8", "Row 9", "Row 10", "Row 11", "Row 12", "Row 13",
                "Row 14", "Row 15", "Row 16", "Row 17", "Row 18", "Row 19", "Row 20",

                "Row 1", "Row 2", "Long Row 3", "Row 4", "Row 5", "Row 6",
                "Row 7", "Row 8", "Row 9", "Row 10", "Row 11", "Row 12", "Row 13",
                "Row 14", "Row 15", "Row 16", "Row 17", "Row 18", "Row 19", "Row 20",

                "Row 1", "Row 2", "Long Row 3", "Row 4", "Row 5", "Row 6",
                "Row 7", "Row 8", "Row 9", "Row 10", "Row 11", "Row 12", "Row 13",
                "Row 14", "Row 15", "Row 16", "Row 17", "Row 18", "Row 19", "===== Row 20 ====",

                "Row 1", "Row 2", "Long Row 3", "Row 4", "Row 5", "Row 6",
                "Row 7", "Row 8", "Row 9", "Row 10", "Row 11", "Row 12", "Row 13",
                "Row 14", "Row 15", "Row 16", "Row 17", "Row 18", "Row 19", "Row 20",

                "Row 1", "Row 2", "Long Row 3", "Row 4", "Row 5", "Row 6",
                "Row 7", "Row 8", "Row 9", "Row 10", "Row 11", "Row 12", "Row 13",
                "Row 14", "Row 15", "Row 16", "Row 17", "Row 18", "Row 19", "Row 20",

                "Row 1", "Row 2", "Long Row 3", "Row 4", "Row 5", "Row 6",
                "Row 7", "Row 8", "Row 9", "Row 10", "Row 11", "Row 12", "Row 13",
                "Row 14", "Row 15", "Row 16", "Row 17", "Row 18", "Row 19", "Row 20"
        );

        names.addAll(
            // Top 100 boys names for 2010, taken from
            // http://www.babycenter.com/top-baby-names-2010
            "Aiden", "Jacob", "Jackson", "Ethan", "Jayden", "Noah", "Logan", "Caden",
//            "Lucas", "Liam", "Mason", "Caleb", "Jack", "Brayden", "Connor", "Ryan",
//            "Matthew", "Michael", "Alexander", "Landon,Nicholas", "Nathan", "Dylan",
//            "Evan", "Benjamin", "Andrew", "Joshua", "Luke", "Gabriel", "William",
//            "James", "Elijah", "Owen", "Tyler", "Gavin", "Carter", "Cameron", "Daniel",
//            "Zachary", "Christian", "Joseph", "Wyatt", "Anthony", "Samuel", "Chase",
//            "Max", "Isaac", "Christopher", "John", "Eli", "Austin", "Colton",
//            "Hunter", "Tristan", "Jonathan", "David", "Alex", "Colin", "Dominic",
//            "Cooper", "Henry", "Carson", "Isaiah", "Charlie", "Julian", "Grayson",
//            "Cole", "Oliver", "Jordan", "Thomas", "Sean", "Brody", "Adam", "Levi",
//            "Aaron", "Parker", "Sebastian", "Xavier", "Ian", "Miles", "Blake", "Jake",
//            "Riley", "Jason", "Nathaniel", "Adrian", "Brandon", "Justin", "Nolan",
//            "Jeremiah", "Hayden", "Devin", "Brady", "Robert", "Josiah", "Hudson",
//            "Ryder", "Bryce", "Micah", "Sam",
//
//            // Top 100 girls names for 2010, taken from
//            // http://www.babycenter.com/top-baby-names-2010
            "Sophia", "Isabella", "Olivia", "Emma", "Chloe", "Ava", "Lily", "Madison"
//            "Addison", "Abigail", "Madelyn", "Emily", "Zoe", "Hailey", "Riley",
//            "Ella", "Mia", "Kaitlyn", "Kaylee", "Peyton", "Layla,Avery", "Hannah",
//            "Mackenzie", "Elizabeth", "Kylie", "Sarah", "Anna", "Grace", "Brooklyn",
//            "Natalie", "Alyssa", "Alexis", "Aubrey", "Samantha", "Isabelle",
//            "Arianna", "Charlotte,Makayla", "Claire", "Lillian", "Gabriella",
//            "Lyla", "Amelia", "Sophie", "Aaliyah", "Taylor", "Audrey", "Bella",
//            "Leah", "Allison", "Sydney", "Alana", "Maya", "Keira", "Lucy", "Kayla",
//            "Lauren", "Savannah", "Brianna", "Ellie", "Reagan", "Evelyn", "Carly",
//            "Julia", "Bailey", "Jordyn", "Victoria", "Annabelle", "Cadence",
//            "Katherine", "Stella", "Molly", "Kennedy", "Jasmine,Gianna", "Abby",
//            "Makenna", "Morgan", "Caroline", "Maria", "Brooke", "Nora", "Alexa",
//            "Camryn", "Paige", "Eva", "Scarlett", "Adriana", "Juliana", "Ashlyn",
//            "Megan", "Kendall", "Harper", "Jada", "Violet", "Alexandra", "Gracie",
//            "Nevaeh", "Sadie"
        );

        money.addAll(43.68, 102.35, -23.67, 110.23, -43.93, 87.21);

        Map<String, String> map1 = new HashMap<String, String>();
        map1.put(FIRST_NAME, "Jonathan");
        map1.put(LAST_NAME, "Giles");
        Map<String, String> map2 = new HashMap<String, String>();
        map2.put(FIRST_NAME, "Brian");
        map2.put(LAST_NAME, "Beck");
        Map<String, String> map3 = new HashMap<String, String>();
        map3.put(FIRST_NAME, "Richard");
        map3.put(LAST_NAME, "Bair");
        Map<String, String> map4 = new HashMap<String, String>();
        map4.put(FIRST_NAME, "Jasper");
        map4.put(LAST_NAME, "Potts");
        Map<String, String> map5 = new HashMap<String, String>();
        map5.put(FIRST_NAME, "Will");
        map5.put(LAST_NAME, "Walker");
        mapData.addAll(map1, map2, map3, map4, map5);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override public void start(Stage stage) {
        stage.setTitle("Hello ListView");
        final Scene scene = new Scene(new Group(), 875, 700);
        scene.setFill(Color.LIGHTGRAY);
        Group root = (Group)scene.getRoot();


        // TabPane
        final TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setPrefWidth(scene.getWidth());
        tabPane.setPrefHeight(scene.getHeight());

        InvalidationListener sceneListener = new InvalidationListener() {
            @Override public void invalidated(Observable ov) {
                tabPane.setPrefWidth(scene.getWidth());
                tabPane.setPrefHeight(scene.getHeight());
            }
        };
        scene.widthProperty().addListener(sceneListener);
        scene.heightProperty().addListener(sceneListener);

        // simple list view example
        Tab simpleTab = new Tab("Simple");
        buildSimpleTab(simpleTab);
        tabPane.getTabs().add(simpleTab);

        // horizontal list view example
        Tab horizontalTab = new Tab("Horizontal");
        buildHorizontalTab(horizontalTab);
        tabPane.getTabs().add(horizontalTab);

        // Cell Factory Tab
        Tab cellFactoriesTab = new Tab("Cell Factories");
        buildCellFactoriesTab(cellFactoriesTab);
        tabPane.getTabs().add(cellFactoriesTab);

        // Cell Editing Tab
        Tab cellEditingTab = new Tab("Cell Editing");
        buildCellEditingTab(cellEditingTab);
        tabPane.getTabs().add(cellEditingTab);

        // Cell Editing Tab
        Tab disappearingNodesTab = new Tab("RT-12822");
        buildDisappearingNodesTab(disappearingNodesTab);
        tabPane.getTabs().add(disappearingNodesTab);

        // sorted and filtered list view example
        Tab sortAndFilterTab = new Tab("Sort & Filter");
        buildSortAndFilterTab(sortAndFilterTab);
        tabPane.getTabs().add(sortAndFilterTab);

        // big list view example
        Tab bigListTab = new Tab("Big List");
        buildBigListTab(bigListTab);
        tabPane.getTabs().add(bigListTab);

        // big DnD example
        Tab dndTab = new Tab("DnD");
        buildDndTab(dndTab);
        tabPane.getTabs().add(dndTab);

        root.getChildren().add(tabPane);

        stage.setScene(scene);
        stage.show();
    }

    public void invalidated(Observable observable) {
        System.out.println("Event: " + observable);
    }

    private void buildSimpleTab(Tab tab) {
        GridPane grid = new GridPane();
//        grid.setGridLinesVisible(true);
        grid.setPadding(new Insets(5, 5, 5, 5));
        grid.setHgap(5);
        grid.setVgap(5);

        // simple list view
        final ListView<String> listView = new ListView<String>();
        listView.setItems(data);
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        listView.setOnEditStart(new EventHandler<EditEvent<String>>() {
            @Override public void handle(EditEvent<String> t) {
                System.out.println("Edit Start: " + t.getIndex());
            }
        });
        listView.setOnEditCancel(new EventHandler<EditEvent<String>>() {
            @Override public void handle(EditEvent<String> t) {
                System.out.println("Edit Cancel: " + t.getIndex());
            }
        });
        listView.setOnEditCommit(new EventHandler<EditEvent<String>>() {
            @Override public void handle(EditEvent<String> t) {
                System.out.println("Edit Commit: " + t.getIndex());
            }
        });

        grid.add(listView, 0, 0, 1, 10);
        GridPane.setVgrow(listView, Priority.ALWAYS);
        GridPane.setHgrow(listView, Priority.ALWAYS);
        // --- simple listview


        // control buttons
        Button row5btn = new Button("Select 'Row 5'");
        row5btn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                listView.getSelectionModel().clearSelection();
                listView.getSelectionModel().select("Row 5");
            }
        });
        grid.add(row5btn, 1, 0);

        Button deselectRow5btn = new Button("Deselect item in 5th row");
        deselectRow5btn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                listView.getSelectionModel().clearSelection(4);
            }
        });
        grid.getChildren().add(deselectRow5btn);
        GridPane.setConstraints(deselectRow5btn, 1, 1);


        Button row20focusBtn = new Button("Focus on item in 20th row");
        row20focusBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                listView.getFocusModel().focus(19);
            }
        });
        grid.getChildren().add(row20focusBtn);
        GridPane.setConstraints(row20focusBtn, 1, 2);

        Button insertBeforeRow1btn = new Button("Add row before 0th row");
        insertBeforeRow1btn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                data.add(0, new Date().toString());
            }
        });
        grid.getChildren().add(insertBeforeRow1btn);
        GridPane.setConstraints(insertBeforeRow1btn, 1, 3);

        Button insertBeforeRow5btn = new Button("Add row before 5th row");
        insertBeforeRow5btn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                data.add(5, new Date().toString());
            }
        });
        grid.getChildren().add(insertBeforeRow5btn);
        GridPane.setConstraints(insertBeforeRow5btn, 1, 4);

        Button delete0thRow = new Button("Delete 0th row");
        delete0thRow.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                data.remove(0);
            }
        });
        grid.getChildren().add(delete0thRow);
        GridPane.setConstraints(delete0thRow, 1, 5);

        Button delete5thRow = new Button("Delete 5th row");
        delete5thRow.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                data.remove(4);
            }
        });
        grid.getChildren().add(delete5thRow);
        GridPane.setConstraints(delete5thRow, 1, 6);

        Button moveToRow40btn = new Button("Move to row 40");
        moveToRow40btn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                listView.scrollTo(39);
            }
        });
        grid.getChildren().add(moveToRow40btn);
        GridPane.setConstraints(moveToRow40btn, 1, 8);

        tab.setContent(grid);

        //
        // observation code for debugging the simple list view multiple selection
        listView.getSelectionModel().selectedIndexProperty().addListener(new InvalidationListener() {
            public void invalidated(Observable ov) {
                System.out.println("SelectedIndex: " + listView.getSelectionModel().getSelectedIndex());
            }
        });
////        listView.getSelectionModel().selectedItemProperty().addListener(new InvalidationListener() {
////            public void invalidated(ObservableValue ov) {
////                System.out.println("\tSelectedItem: " + listView.getSelectionModel().getSelectedItem());
////            }
////        });
////        listView.getFocusModel().focusedIndexProperty().addListener(new InvalidationListener() {
////            public void invalidated(ObservableValue ov) {
////                System.out.println("\tFocusedIndex: " + listView.getFocusModel().getFocusedIndex());
////            }
////        });
////        listView.getFocusModel().focusedItemProperty().addListener(new InvalidationListener() {
////            public void invalidated(ObservableValue ov) {
////                System.out.println("\tFocusedItem: " + listView.getFocusModel().getFocusedItem());
////            }
////        });
//////        listView.getFocusModel().addInvalidationListener(FocusModel.FOCUSED_ITEM, this);
////
        listView.getSelectionModel().getSelectedIndices().addListener(new ListChangeListener<Integer>() {
            public void onChanged(Change<? extends Integer> change) {
                while (change.next()) {
                    System.out.println("SelectedIndices: " + change.getList() +
                            ", removed: " + change.getRemoved() +
                            ", addedFrom: " + change.getFrom() +
                            ", addedTo: " + change.getTo());
                }
            }
        });
////        ((MultipleSelectionModel)listView.getSelectionModel()).getSelectedItems().addListener(new ListChangeListener<String>() {
////            public void onChanged(Change<? extends String> c) {
////                System.out.println("SelectedIndices: " + c.getList() +
////                        ", removed: " + c.getRemoved() +
////                        ", addedFrom: " + c.getFrom() +
////                        ", addedTo: " + c.getTo());
////            }
////        });
    }

    private void buildHorizontalTab(Tab tab) {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(5, 5, 5, 5));
        grid.setHgap(5);
        grid.setVgap(5);

        // simple list view with content
        ListView horizontalListView = new ListView();
        horizontalListView.setOrientation(Orientation.HORIZONTAL);
        horizontalListView.setItems(data);

        grid.add(horizontalListView, 0, 0);
        GridPane.setVgrow(horizontalListView, Priority.ALWAYS);
        GridPane.setHgrow(horizontalListView, Priority.ALWAYS);

        // simple list view with content
        ListView emptyListView = new ListView();
        emptyListView.setOrientation(Orientation.HORIZONTAL);

        grid.add(emptyListView, 1, 0);
        GridPane.setVgrow(emptyListView, Priority.ALWAYS);
        GridPane.setHgrow(emptyListView, Priority.ALWAYS);

        tab.setContent(grid);
    }

    private void buildCellFactoriesTab(Tab tab) {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(5, 5, 5, 5));
        grid.setHgap(5);
        grid.setVgap(5);

        // number format listview
        final ListView<Number> listView = new ListView<Number>(money);
        listView.setCellFactory(new Callback<ListView<Number>, ListCell<Number>>() {
            @Override public ListCell<Number> call(ListView<Number> list) {
                return new MoneyFormatCell();
            }
        });
        grid.add(listView, 0, 0);
        GridPane.setVgrow(listView, Priority.ALWAYS);
        GridPane.setHgrow(listView, Priority.ALWAYS);
        // --- number format listview


        // expanding cells listview
        final ListView<String> listView2 = new ListView<String>(data);
        listView2.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
            @Override public ListCell<String> call(ListView<String> list) {
                return new ExpandOnSelectionCell<String>();
            }
        });
        grid.add(listView2, 1, 0);
        GridPane.setVgrow(listView2, Priority.ALWAYS);
        GridPane.setHgrow(listView2, Priority.ALWAYS);
        // --- expanding cells listview


        tab.setContent(grid);
    }

    private void buildDisappearingNodesTab(Tab tab) {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(5, 5, 5, 5));
        grid.setHgap(5);
        grid.setVgap(5);

        // Create a ListView<Button>
        ListView<Button> listView = new ListView<Button>();
        listView.setPrefWidth(100.0F);
        listView.setPrefHeight(100.0F);
        Button button = new Button("Apples");
        Tooltip tooltip = new Tooltip();
        tooltip.setText("Tooltip Apples");
        button.setTooltip(tooltip);
        Button button2 = new Button("Oranges");
        Tooltip tooltip2 = new Tooltip();
        tooltip2.setText("Tooltip Oranges");
        button2.setTooltip(tooltip2);
        Button button3 = new Button("Peaches");
        Tooltip tooltip3 = new Tooltip();
        tooltip3.setText("Tooltip Peaches");
        button3.setTooltip(tooltip3);
        Button button4 = new Button("Plums");
        Tooltip tooltip4 = new Tooltip();
        tooltip4.setText("Tooltip Plums");
        button4.setTooltip(tooltip4);
        Button button5 = new Button("Apricots");
        Tooltip tooltip5 = new Tooltip();
        tooltip5.setText("Tooltip Apricots");
        button5.setTooltip(tooltip5);
        Button button6 = new Button("Lemons");
        Tooltip tooltip6 = new Tooltip();
        tooltip6.setText("Tooltip Lemons");
        button6.setTooltip(tooltip6);
        Button button7 = new Button("Grapefruit");
        Tooltip tooltip7 = new Tooltip();
        tooltip7.setText("Tooltip Grapefruit");
        button7.setTooltip(tooltip7);
        Button button8 = new Button("Cherries");
        Tooltip tooltip8 = new Tooltip();
        tooltip8.setText("Tooltip Cherries");
        button8.setTooltip(tooltip8);
        listView.setItems(javafx.collections.FXCollections.observableArrayList(
                button, button2, button3, button4, button5, button6, button7, button8));

        grid.add(listView, 0, 0, 1, 10);
        tab.setContent(grid);
    }

    private void buildCellEditingTab(Tab tab) {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(5, 5, 5, 5));
        grid.setHgap(5);
        grid.setVgap(5);

        // simple textfield list view
        final ListView<String> textFieldListView = new ListView<String>();
        textFieldListView.setEditable(true);
        textFieldListView.setItems(data);
        textFieldListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        textFieldListView.setCellFactory(TextFieldListCell.forListView());
        textFieldListView.setOnEditStart(new EventHandler<EditEvent<String>>() {
            @Override public void handle(EditEvent<String> t) {
                System.out.println("On Edit Start: " + t);
            }
        });
        textFieldListView.setOnEditCancel(new EventHandler<EditEvent<String>>() {
            @Override public void handle(EditEvent<String> t) {
                System.out.println("On Edit Cancel: " + t);
            }
        });
        grid.add(textFieldListView, 0, 0, 1, 10);
        GridPane.setVgrow(textFieldListView, Priority.ALWAYS);
        GridPane.setHgrow(textFieldListView, Priority.ALWAYS);
        // --- simple listview

        // simple choicebox list view
        final ObservableList<String> options = FXCollections.observableArrayList("Jenny", "Billy", "Timmy");
        final ListView<String> choiceBoxListView = new ListView<String>();
        choiceBoxListView.setEditable(true);
        choiceBoxListView.setItems(data);
        choiceBoxListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        choiceBoxListView.setCellFactory(ChoiceBoxListCell.forListView(options));
        grid.add(choiceBoxListView, 1, 0, 1, 10);
        GridPane.setVgrow(choiceBoxListView, Priority.ALWAYS);
        GridPane.setHgrow(choiceBoxListView, Priority.ALWAYS);
        // --- simple listview


        // control buttons
        final Button editRow3btn = new Button("Edit row 3");
        editRow3btn.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent t) {
                textFieldListView.edit(2);
                choiceBoxListView.edit(2);
            }
        });
        grid.getChildren().add(editRow3btn);
        GridPane.setConstraints(editRow3btn, 2, 0);

        final Button cancelEditBtn = new Button("Cancel edit");
        cancelEditBtn.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent t) {
                textFieldListView.edit(-1);
                choiceBoxListView.edit(-1);
            }
        });
        grid.getChildren().add(cancelEditBtn);
        GridPane.setConstraints(cancelEditBtn, 2, 1);

        tab.setContent(grid);
    }

//    private void buildCellsTab(Tab tab) {
//        GridPane grid = new GridPane();
//        grid.setPadding(new Insets(5, 5, 5, 5));
//        grid.setHgap(5);
//        grid.setVgap(5);
//
//        // add a complex listview (using pre-built cell factory)
//        final ListView<Number> listView2 = new ListView<Number>();
//        listView2.setItems(money);
//        listView2.setCellFactory(MoneyFormatCellFactory.listView());
//        grid.getChildren().add(listView2);
//        GridPane.setVgrow(listView2, Priority.ALWAYS);
//        GridPane.setConstraints(listView2, 0, 0);
//        // --- complex listview
//
//        // add another complex listview (using pre-built cell factory)
//        final ListView<Map<String, String>> listView3 = new ListView<Map<String, String>>();
//        listView3.setItems(mapData);
////        listView3.setCellFactory(Cells.ListView.mapProperty(FIRST_NAME));
//        listView3.setCellFactory(MapValueCellFactory.listView("First Name: %1$s\r\nLast Name: %2$s", FIRST_NAME, LAST_NAME));
//        grid.getChildren().add(listView3);
//        GridPane.setVgrow(listView3, Priority.ALWAYS);
//        GridPane.setConstraints(listView3, 1, 0);
//        // --- complex listview
//
//        tab.setContent(grid);
//    }


    private Comparator<String> alphabeticalComparator = new Comparator<String>() {
        @Override public int compare(String o1, String o2) {
            return o1.compareTo(o2);
        }
    };

    private Comparator<String> reverseAlphabeticalComparator = new Comparator<String>() {
        @Override public int compare(String o1, String o2) {
            return o2.compareTo(o1);
        }
    };

    private void buildSortAndFilterTab(Tab tab) {
        // initially we match everything in the filter list

        final SortedList<String> sortedList = new SortedList<String>(names);
        final FilteredList<String> filteredList = new FilteredList<String>(names, e -> true);

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(5, 5, 5, 5));
        grid.setHgap(5);
        grid.setVgap(5);

        // --- unmodified listview
        final ListView<String> unmodifiedListView = new ListView<String>();
        unmodifiedListView.setId("Unmodified list");
        unmodifiedListView.setItems(names);
//        unmodifiedListView.setCellFactory(TextFieldCellFactory.listView());
        Node unmodifiedLabel = createLabel("Original ListView:");
        grid.getChildren().addAll(unmodifiedLabel, unmodifiedListView);
        GridPane.setConstraints(unmodifiedLabel, 0, 0);
        GridPane.setConstraints(unmodifiedListView, 0, 1);
        GridPane.setVgrow(unmodifiedListView, Priority.ALWAYS);
        // --- unmodified listview


        // --- sorted listview
        final ListView<String> sortedListView = new ListView<String>();
        sortedListView.setId("sorted list");
        sortedListView.setItems(sortedList);
//        sortedListView.setCellFactory(TextFieldCellFactory.listView());
        Node sortedLabel = createLabel("Sorted ListView:");
        grid.getChildren().addAll(sortedLabel, sortedListView);
        GridPane.setConstraints(sortedLabel, 1, 0);
        GridPane.setConstraints(sortedListView, 1, 1);
        GridPane.setVgrow(sortedListView, Priority.ALWAYS);
        // --- sorted listview


        // --- filtered listview
        final ListView<String> filteredListView = new ListView<String>();
        filteredListView.setId("filtered list");
        filteredListView.setItems(filteredList);
//        filteredListView.setCellFactory(TextFieldCellFactory.listView());
        Node filteredLabel = createLabel("Filtered (and sorted) ListView:");
        grid.getChildren().addAll(filteredLabel, filteredListView);
        GridPane.setConstraints(filteredLabel, 2, 0);
        GridPane.setConstraints(filteredListView, 2, 1);
        GridPane.setVgrow(filteredListView, Priority.ALWAYS);
        // --- filtered listview


        // control buttons
        VBox vbox = new VBox(10);

        vbox.getChildren().add(new Label("Note: Double-click list cells to edit."));

        final TextField filterInput = new TextField();
        filterInput.setPromptText("Enter filter text");
//        filterInput.setColumns(35);
        filterInput.setOnKeyReleased(new EventHandler<KeyEvent>() {
            public void handle(KeyEvent t) {
                filteredList.setPredicate((String e) ->
                        e.toUpperCase().contains(filterInput.getText().toUpperCase()));
            }
        });
        vbox.getChildren().add(filterInput);

        final TextField newItemInput = new TextField();
        newItemInput.setPromptText("Enter text, then press enter to add item to list");
//        newItemInput.setColumns(35);
        newItemInput.setOnKeyReleased(new EventHandler<KeyEvent>() {
            public void handle(KeyEvent t) {
                if (t.getCode() == KeyCode.ENTER) {
                    names.add(newItemInput.getText());
                    newItemInput.setText("");
                }
            }
        });
        vbox.getChildren().add(newItemInput);

        // sort ascending
        final ToggleGroup toggleGroup = new ToggleGroup();
        toggleGroup.selectedToggleProperty().addListener(new InvalidationListener() {
            public void invalidated(Observable ov) {
                if (toggleGroup.getSelectedToggle() == null) return;
//                sortedList.setComparator((Comparator<String>)toggleGroup.getSelectedToggle().getUserData());
                System.out.println("Disabled in HelloListView due to FilteredList bug");
            }
        });
        final RadioButton sortAscBtn = new RadioButton("Sort Ascending");
        sortAscBtn.setUserData(alphabeticalComparator);
        sortAscBtn.setToggleGroup(toggleGroup);
        sortAscBtn.setSelected(true);

        final RadioButton sortDescBtn = new RadioButton("Sort Descending");
        sortDescBtn.setUserData(reverseAlphabeticalComparator);
        sortDescBtn.setToggleGroup(toggleGroup);

        vbox.getChildren().addAll(sortAscBtn, sortDescBtn);

        grid.setConstraints(vbox, 3, 1);
        grid.getChildren().add(vbox);

        tab.setContent(grid);
    }

    private void buildBigListTab(Tab tab) {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(5, 5, 5, 5));
        grid.setHgap(5);
        grid.setVgap(5);

        BigList intList = new BigList();

        // simple list view
        final ListView<Integer> listView = new ListView<Integer>();
        listView.setItems(intList);
//        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE_INTERVAL_SELECTION);
        grid.add(listView, 0, 0);
        GridPane.setVgrow(listView, Priority.ALWAYS);
        GridPane.setHgrow(listView, Priority.ALWAYS);

        tab.setContent(grid);
    }

    private void buildDndTab(Tab tab) {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(5, 5, 5, 5));
        grid.setHgap(5);
        grid.setVgap(5);

        ObservableList<String> listOneItems = FXCollections.observableArrayList(names.subList(0, 8));
        ObservableList<String> listTwoItems = FXCollections.observableArrayList(names.subList(8, 16));

        Label introLabel = new Label("By default, DnD is a MOVE, hold ctrl/cmd whilst dragging for COPY.");
        introLabel.setWrapText(true);
        introLabel.setFont(Font.font(18));
        grid.add(introLabel, 0, 0, 2, 1);

        // --- list one
        final ListView<String> listOne = new ListView<String>();
        listOne.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        listOne.setItems(listOneItems);
        Node listOneLabel = createLabel("List One:");
        grid.getChildren().addAll(listOneLabel, listOne);
        GridPane.setConstraints(listOneLabel, 0, 1);
        GridPane.setConstraints(listOne, 0, 2);
        GridPane.setVgrow(listOne, Priority.ALWAYS);
        // --- list one

        // --- list two
        final ListView<String> listTwo = new ListView<String>();
        listTwo.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        listTwo.setItems(listTwoItems);
        Node listTwoLabel = createLabel("List Two:");
        grid.getChildren().addAll(listTwoLabel, listTwo);
        GridPane.setConstraints(listTwoLabel, 1, 1);
        GridPane.setConstraints(listTwo, 1, 2);
        GridPane.setVgrow(listTwo, Priority.ALWAYS);
        // --- list two

        // set up Dnd in both directions
        EventHandler<MouseEvent> dragDetected = new EventHandler<MouseEvent>() {
            public void handle(MouseEvent event) {
                ListView<String> list = (ListView) event.getSource();
                Dragboard db = list.startDragAndDrop(TransferMode.ANY);

                ClipboardContent content = new ClipboardContent();
                content.putString(list.getSelectionModel().getSelectedItem());
                db.setContent(content);

                event.consume();
            }
        };
        EventHandler<DragEvent> dragOver = new EventHandler<DragEvent>() {
            public void handle(DragEvent event) {
                if (event.getGestureSource() != event.getTarget() && event.getDragboard().hasString()) {
                    event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                }

                event.consume();
            }
        };
        EventHandler<DragEvent> dragDropped = new EventHandler<DragEvent>() {
            public void handle(DragEvent event) {
                ListView<String> list = (ListView) event.getGestureTarget();

                Dragboard db = event.getDragboard();
                boolean success = false;
                if (db.hasString()) {
                    list.getItems().add(db.getString());
                    success = true;
                }

                event.setDropCompleted(success);
                event.consume();
            }
        };
        EventHandler<DragEvent> dragDone = new EventHandler<DragEvent>() {
            public void handle(DragEvent event) {
                if (event.getTransferMode() == TransferMode.MOVE) {
                    ListView<String> list = (ListView) event.getGestureSource();
                    list.getItems().remove(event.getDragboard().getString());
                }
                event.consume();
            }
        };

        listOne.setOnDragDetected(dragDetected);
        listOne.setOnDragOver(dragOver);
        listOne.setOnDragDropped(dragDropped);
        listOne.setOnDragDone(dragDone);

        listTwo.setOnDragDetected(dragDetected);
        listTwo.setOnDragOver(dragOver);
        listTwo.setOnDragDropped(dragDropped);
        listTwo.setOnDragDone(dragDone);

        tab.setContent(grid);
    }

    private Node createLabel(String text) {
        Label label = new Label(text);
        return label;
    }

//    public void handle(Bean bean, PropertyReference<?> pr) {
//        SelectionModel sm = null;
//        FocusModel fm = null;
//        if (bean instanceof SelectionModel) {
//            System.out.print("Selection Event: ");
//            sm = (SelectionModel) bean;
//        } else if (bean instanceof FocusModel) {
//            System.out.print("Focus Event: ");
//            fm = (FocusModel) bean;
//        }
//
//
//        if (pr == SelectionModel.SELECTED_INDEX) {
//            System.out.println("\tSelectedIndex: " + sm.getSelectedIndex());
//        } else if (pr == SelectionModel.SELECTED_ITEM) {
//            System.out.println("\tSelectedItem: " + sm.getSelectedItem());
//        } else if (pr == FocusModel.FOCUSED_INDEX) {
//            System.out.println("\tFocusedIndex: " + fm.getFocusedIndex());
//        } else if (pr == FocusModel.FOCUSED_ITEM) {
//            System.out.println("\tFocusedItem: " + fm.getFocusedItem());
//        }
//    }

    private static class BigList extends AbstractList<Integer> implements ObservableList<Integer> {

        @Override
        public Integer get(int index) {
            return index;
        }

        @Override
        public int size() {
            return Integer.MAX_VALUE;
        }

        @Override
        public void addListener(InvalidationListener listener) {
            // no-op
        }

        @Override
        public void removeListener(InvalidationListener listener) {
            // no-op
        }

        public void addListener(ListChangeListener<? super Integer> ll) {
            // no-op
        }

        public void removeListener(ListChangeListener<? super Integer> ll) {
            // no-op
        }

        public boolean addAll(Integer... es) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean setAll(Integer... es) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean setAll(Collection<? extends Integer> clctn) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean removeAll(Integer... es) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean retainAll(Integer... es) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void remove(int from, int to) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

    }

    private static class MoneyFormatCell extends ListCell<Number> {
        @Override
        public void updateItem(Number item, boolean empty) {
            super.updateItem(item, empty);

            // format the number as if it were a monetary value using the
            // formatting relevant to the current locale. This would format
            // 43.68 as "$43.68", and -23.67 as "-$23.67"
            setText(item == null ? "" : NumberFormat.getCurrencyInstance().format(item));

            // change the label colour based on whether it is positive (green)
            // or negative (red). If the cell is selected, the text will
            // always be white (so that it can be read against the blue
            // background), and if the value is zero, we'll make it black.
            if (item != null) {
                double value = item.doubleValue();
                setTextFill(isSelected() ? Color.WHITE :
                    value == 0 ? Color.BLACK :
                    value < 0 ? Color.RED : Color.GREEN);
            }
        }
    }



    private static class ExpandOnSelectionCell<T> extends ListCell<T> {
        private static final double PREF_HEIGHT = 24;
        private static final double EXPAND_HEIGHT = PREF_HEIGHT * 3;

        private static final int duration = 350;

        // records all expanded cells
        private static final BitSet expandedCells = new BitSet();


        public ExpandOnSelectionCell() {

            prefHeightProperty().addListener(new InvalidationListener() {
                public void invalidated(Observable o) {
                    requestLayout();
                }
            });

            addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent t) {

                    double startHeight = getHeight();

                    // the end height is the opposite of the current state -
                    // we are animating out of this state after all
                    double endHeight = isExpanded() ? PREF_HEIGHT : EXPAND_HEIGHT;

                    // flip whether this cell index is expanded or not
                    expandedCells.flip(getIndex());

                    // create a timeline to expand/collapse the cell. All this
                    // really does is modify the height of the content
                    Timeline timeline = new Timeline();
                    timeline.setCycleCount(1);
                    timeline.setAutoReverse(false);

                    timeline.getKeyFrames().addAll(
                        new KeyFrame(Duration.ZERO, new KeyValue(prefHeightProperty(), startHeight, Interpolator.EASE_BOTH)),
                        new KeyFrame(Duration.millis(duration), new KeyValue(prefHeightProperty(), endHeight, Interpolator.EASE_BOTH))
                    );

                    timeline.playFromStart();
                }
            });
        }

        @Override
        public void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);
            setText(item == null ? "null" : item.toString());
        }

        @Override
        public void updateIndex(int i) {
            super.updateIndex(i);

            if (isExpanded()) {
                // immediately expand this cell
                setPrefHeight(EXPAND_HEIGHT);
            } else {
                // immediately collapse this cell
                setPrefHeight(PREF_HEIGHT);
            }
        }

        private boolean isExpanded() {
            if (getIndex() < 0) return false;
            return expandedCells.get(getIndex());
        }

        @Override
        protected double computePrefHeight(double width) {
            double ph = 0;
            if (isExpanded()) {
                ph = getPrefHeight();
            } else {
                ph = super.computePrefHeight(width);
            }

            return ph;
        }

        @Override protected double computeMinHeight(double width) {
            return computePrefHeight(width);
        }
    }
}

