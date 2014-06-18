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


import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

import javafx.application.Application;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class HelloTableView extends Application {

    // for performance test tab
    private static int NB_COL = 30;
    private static int NB_LINE = 10000;

    public static class Person {

        private BooleanProperty invited;
        private StringProperty firstName;
        private StringProperty lastName;
        private StringProperty email;

        private final String country = "New Zealand";

        public Person(String fName, String lName) {
            this(fName, lName, null);
        }

        public Person(String fName, String lName, String email) {
            this(fName, lName, email, false);
        }

        public Person(String fName, String lName, String email, boolean invited) {
            this.firstName = new SimpleStringProperty(fName);
            this.lastName = new SimpleStringProperty(lName);
            this.email = new SimpleStringProperty(email);
            this.invited = new SimpleBooleanProperty(invited);

            this.invited.addListener((ov, t, t1) -> System.out.println(getFirstName() + " invited: " + t1));
        }

        public Boolean isInvited() { return invited.get(); }
        public BooleanProperty invitedProperty() { return invited; }

        public String getFirstName() {
            return firstName.get();
        }

        public void setFirstName(String firstName) {
            this.firstName.set(firstName);
        }

        public StringProperty firstNameProperty() {
            return firstName;
        }

        public String getLastName() {
            return lastName.get();
        }

        public void setLastName(String lastName) {
            this.lastName.set(lastName);
        }

        public StringProperty lastNameProperty() {
            return lastName;
        }

        public String getEmail() {
            return email.get();
        }

        public void setEmail(String email) {
            this.email.set(email);
        }

        public StringProperty emailProperty() {
            return email;
        }

        public String getCountry() {
            return country;
        }

        public String toString() {
            return "Person [ " + getFirstName() + " " + getLastName()/* + ", " + getEmail()*/ + " ]";
        }
    }

    private static final String MULTI_SELECT = "multiSelect";
    private static final String CELL_SELECT = "cellSelect";

    private static ObservableList<Person> createTestData() {
        ObservableList<Person> data = FXCollections.observableArrayList();
        data.addAll(
            new Person("Jacob",     "Smith\nSmith\nSmith",    "jacob.smith<at>example.com", true ),
            new Person("Isabella",  "Johnson",  "isabella.johnson<at>example.com" ),
            new Person("Ethan",     "Williams", "ethan.williams<at>example.com", true ),
            new Person("Emma",      "Jones",    "emma.jones<at>example.com" ),
            new Person("Michael",   "Brown",    "michael.brown<at>example.com", true ),
            new Person("Olivia",    "Davis",    "olivia.davis<at>example.com" ),
            new Person("Alexander", "Miller",   "alexander.miller<at>example.com", true ),
            new Person("Sophia",    "Wilson",   "sophia.wilson<at>example.com" ),
            new Person("William",   "Moore",    "william.moore<at>example.com", true ),
            new Person("Ava",       "Taylor",   "ava.taylor<at>example.com" ),

            new Person("Joshua",    "Anderson", "joshua.anderson<at>example.com" ),
            new Person("Emily",     "Thomas",   "emily.thomas<at>example.com" ),
            new Person("Daniel",    "Jackson",  "daniel.jackson<at>example.com" ),
            new Person("Madison",   "White",    "madison.white<at>example.com" ),
            new Person("Jayden",    "Harris",   "jayden.harris<at>example.com" ),
            new Person("Abigail",   "Martin",   "abigail.martin<at>example.com" ),
            new Person("Noah",      "Thompson", "noah.thompson<at>example.com" ),
            new Person("Chloe",     "Garcia",   "chloe.garcia<at>example.com" ),
            new Person("Anthony",   "Martinez", "anthony.martinez<at>example.com" ),
            new Person("Mia",       "Robinson", "mia.robinson<at>example.com" ),

            new Person("Jacob",     "Smith" ),
            new Person("Isabella",  "Johnson" ),
            new Person("Ethan",     "Williams" ),
            new Person("Emma",      "Jones" ),
            new Person("Michael",   "Brown" ),
            new Person("Olivia",    "Davis" ),
            new Person("Alexander", "Miller" ),
            new Person("Sophia",    "Wilson" ),
            new Person("William",   "Moore" ),
            new Person("Ava",       "Taylor" ),
            new Person("Joshua",    "Anderson" ),
            new Person("Emily",     "Thomas" ),
            new Person("Daniel",    "Jackson" ),
            new Person("Madison",   "White" ),
            new Person("Jayden",    "Harris" ),
            new Person("Abigail",   "Martin" ),
            new Person("Noah",      "Thompson" ),
            new Person("Chloe",     "Garcia" ),
            new Person("Anthony",   "Martinez" ),
            new Person("Mia",       "Robinson" )
        );
        return data;
    }

    private final static TableColumn<Person, String> firstNameCol;
    private final static TableColumn<Person, String> lastNameCol;
    private final static TableColumn<Person, String> nameCol;
    private final static TableColumn<Person, String> emailCol;
    private final static TableColumn<Person, String> countryCol;
    private final static TableColumn<Person, Boolean> invitedCol;

    static {

        // Popup Menus
//        emailMenu = new ContextMenu();
//        MenuItem option1 = new MenuItem("Option 1");
//        MenuItem option2 = new MenuItem("Option 2");
//        emailMenu.getItems().addAll(option1, option2);
        // -- Popup Menus


        // Columns
        // first name, last name, and wrapper parent column
        firstNameCol = new TableColumn<>();
        firstNameCol.setText("First");

        Rectangle sortNode = new Rectangle(10, 10, Color.RED);
        sortNode.fillProperty().bind(new ObjectBinding<Paint>() {
            { bind(firstNameCol.sortTypeProperty()); }

            @Override protected Paint computeValue() {
                switch (firstNameCol.getSortType()) {
                    case ASCENDING: return Color.GREEN;
                    case DESCENDING: return Color.RED;
                    default: return Color.BLACK;
                }
            }
        });
        firstNameCol.setSortNode(sortNode);
//        firstNameCol.setMinWidth(300);
//        firstNameCol.setProperty("firstName");
//        firstNameCol.setCellFactory(TextFieldCellFactory.tableView());
//        firstNameCol.setGetCellData(new Runnable2<Object, Integer, Integer>() {
//            @Override public Object run(Integer row, Integer col) {
//                return data.get(row).firstName;
//            }
//        });
        firstNameCol.setCellValueFactory(new PropertyValueFactory<Person,String>("firstName"));
//        firstNameCol.setCellValueFactory(new Callback<CellDataFeatures<Person,String>, ObservableValue<String>>() {
//            public ObservableValue<String> call(CellDataFeatures<Person,String> p) {
//                return p.getValue().firstName;
//            }
//        });
        firstNameCol.setOnEditCommit(t -> System.out.println("Edit commit event: " + t.getNewValue()));

//        final Node graphic1 = new ImageView(new Image("file:src/hello/about_16.png"));
        lastNameCol = new TableColumn<Person, String>();
//        lastNameCol.setGraphic(graphic1);
        lastNameCol.setText("Last");
//        lastNameCol.setResizable(false);
//        lastNameCol.setProperty("lastName");
        lastNameCol.setCellValueFactory(p -> p.getValue().lastNameProperty());

        nameCol = new TableColumn<Person, String>();
        nameCol.setText("Name");
//        nameCol.setResizable(false);
        nameCol.getColumns().addAll(firstNameCol, lastNameCol);
        // -- end

        emailCol = new TableColumn<Person, String>();
        emailCol.setText("Email");
//        emailCol.setContextMenu(emailMenu);
        emailCol.setMinWidth(200);
//        emailCol.setResizable(false);
        emailCol.setCellValueFactory(p -> p.getValue().emailProperty());

        countryCol = new TableColumn<Person, String>();
        countryCol.setText("Country");
        countryCol.setCellValueFactory(p -> new ReadOnlyObjectWrapper<String>("New Zealand"));
//        countryCol.setProperty("country");

        invitedCol = new TableColumn<Person, Boolean>();
        invitedCol.setText("Invited");
        invitedCol.setMaxWidth(50);
        invitedCol.setCellValueFactory(new PropertyValueFactory("invited"));
        invitedCol.setCellFactory(p -> new CheckBoxTableCell<Person, Boolean>());
        invitedCol.setEditable(true);
        /*
        ContextMenu ctx = new ContextMenu();
        final MenuItem cutMI    = new MenuItem("Cut");
        final MenuItem copyMI   = new MenuItem("Copy");
        final MenuItem pasteMI  = new MenuItem("Paste");
        final MenuItem deleteMI = new MenuItem("DeleteSelection");
        final MenuItem selectMI = new MenuItem("SelectAll");
        final ContextMenu cm = new ContextMenu(cutMI, copyMI, pasteMI, deleteMI,
                                               new SeparatorMenuItem(), selectMI);
        invitedCol.setContextMenu(cm);
        */
        // -- Columns
    }


    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override public void start(Stage stage) {
        stage.setTitle("Hello TableView");
        final Scene scene = new Scene(new Group(), 875, 700);
        scene.setFill(Color.LIGHTGRAY);
        Group root = (Group)scene.getRoot();

        // TabPane
        final TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setPrefWidth(scene.getWidth());
        tabPane.setPrefHeight(scene.getHeight());

        InvalidationListener sceneListener = ov -> {
            tabPane.setPrefWidth(scene.getWidth());
            tabPane.setPrefHeight(scene.getHeight());
        };
        scene.widthProperty().addListener(sceneListener);
        scene.heightProperty().addListener(sceneListener);

        // simple table view example
        Tab simpleTab = new Tab("Simple");
        buildSimpleTab(simpleTab);
        tabPane.getTabs().add(simpleTab);

        // unsorted support example
        Tab unsortedTab = new Tab("Unsorting");
        buildUnsortedTab(unsortedTab);
        tabPane.getTabs().add(unsortedTab);

//        // Cell Factory Tab
//        Tab cellFactoryTab = new Tab("Cell Factory");
//        buildCellsTab(cellFactoryTab);
//        tabPane.getTabs().add(cellFactoryTab);

        // sorted and filtered table view example
        Tab sortAndFilterTab = new Tab("Sort & Filter");
        buildSortAndFilterTab(sortAndFilterTab);
        tabPane.getTabs().add(sortAndFilterTab);

        // performance test table view example
        Tab perfTestTab = new Tab("Performance Test");
        buildPerformanceTestTab(perfTestTab, false);
        tabPane.getTabs().add(perfTestTab);

        // performance test table view example
        Tab customCellPerfTestTab = new Tab("Custom Cell Performance Test");
        buildPerformanceTestTab(customCellPerfTestTab, true);
        tabPane.getTabs().add(customCellPerfTestTab);

        root.getChildren().add(tabPane);

        stage.setScene(scene);
        stage.show();
    }

    private void buildSimpleTab(Tab tab) {
        GridPane grid = new GridPane();
//        grid.setGridLinesVisible(true);
        grid.setPadding(new Insets(5, 5, 5, 5));
        grid.setHgap(5);
        grid.setVgap(5);

        ObservableList<Person> data = createTestData();

        // simple table view
        final TableView<Person> tableView = new TableView<Person>();
        tableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        tableView.getSelectionModel().setCellSelectionEnabled(false);
        tableView.setTableMenuButtonVisible(false);
        tableView.setItems(data);
//        tableView.getColumns().addAll(nameCol, emailCol);
        tableView.getColumns().addAll(invitedCol, nameCol, emailCol, countryCol);
        tableView.setPrefSize(485, 300);
        tableView.setEditable(true);


        ContextMenu ctx = new ContextMenu();
        final MenuItem cutMI    = new MenuItem("Cut");
        final MenuItem copyMI   = new MenuItem("Copy");
        final MenuItem pasteMI  = new MenuItem("Paste");
        final MenuItem deleteMI = new MenuItem("DeleteSelection");
        final MenuItem selectMI = new MenuItem("SelectAll");
        final ContextMenu cm = new ContextMenu(cutMI, copyMI, pasteMI, deleteMI,
                                               new SeparatorMenuItem(), selectMI);
        invitedCol.setContextMenu(cm);

        tableView.setPlaceholder(new ProgressBar(-1));

        tableView.setRowFactory(p -> new TableRow<Person>() {
            @Override public void updateItem(Person item, boolean empty) {
                super.updateItem(item, empty);

                TableView.TableViewSelectionModel sm = tableView.getSelectionModel();

                // get the row and column currently being editing
                TablePosition editCell = tableView.getEditingCell();

                // set the columns to only be shown if we aren't currently
                // editing this row. If we are editing this row, we
                // turn off the columns to show our custom node, which
                // is set below.
                boolean showColumns = sm == null ||
                        sm.isCellSelectionEnabled() ||
                        editCell == null ||
                        editCell.getColumn() != -1 ||
                        editCell.getRow() != getIndex();

                if (showColumns) {
                    setGraphic(null);
                } else {
                    Button stopEditingBtn = new Button("Stop editing");
                    stopEditingBtn.setPrefSize(200, 50);
                    stopEditingBtn.setOnAction(t -> tableView.edit(-1, null));
                    setGraphic(stopEditingBtn);
                }
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
        });

//        tableView.setOnMouseClicked(new EventHandler<MouseEvent>() {
//            @Override public void handle(MouseEvent event) {
//                System.out.println("Clicked: " + ((TablePosition) tableView.getSelectionModel().getSelectedCells().get(0)).getTableColumn().getText());
//                System.out.println("Clicked-2: " + tableView.getFocusModel().getFocusedCell().getTableColumn().getText());
//            }
//        });

//        tableView.setOnEditStart(new EventHandler<EditEvent<Person>>() {
//            @Override public void handle(EditEvent<Person> t) {
//                System.out.println("Edit Start: " + t.getTablePosition());
//            }
//        });
//        tableView.setOnEditCancel(new EventHandler<EditEvent<Person>>() {
//            @Override public void handle(EditEvent<Person> t) {
//                System.out.println("Edit Cancel: " + t.getTablePosition());
//            }
//        });
//        tableView.setOnEditCommit(new EventHandler<EditEvent<Person>>() {
//            @Override public void handle(EditEvent<Person> t) {
//                System.out.println("Edit Commit: " + t);
//            }
//        });


        grid.getChildren().addAll(tableView);
        GridPane.setConstraints(tableView, 0, 0, 1, 12);
        GridPane.setVgrow(tableView, Priority.ALWAYS);
        GridPane.setHgrow(tableView, Priority.ALWAYS);
        // --- simple listview




        // add some controls to manipulate the table state
        // selection mode
        final ToggleGroup tg = new ToggleGroup();

        RadioButton singleRow = new RadioButton("Single Row");
        singleRow.getProperties().put(MULTI_SELECT, SelectionMode.SINGLE);
        singleRow.getProperties().put(CELL_SELECT, false);
        singleRow.setToggleGroup(tg);
        tg.selectToggle(singleRow);
        grid.getChildren().add(singleRow);
        GridPane.setConstraints(singleRow, 1, 0);

        RadioButton singleCell = new RadioButton("Single Cell");
        singleCell.getProperties().put(MULTI_SELECT, SelectionMode.SINGLE);
        singleCell.getProperties().put(CELL_SELECT, true);
        singleCell.setToggleGroup(tg);
        grid.getChildren().add(singleCell);
        GridPane.setConstraints(singleCell, 1, 1);

        RadioButton multipleRow = new RadioButton("Multiple Rows");
        multipleRow.getProperties().put(MULTI_SELECT, SelectionMode.MULTIPLE);
        multipleRow.getProperties().put(CELL_SELECT, false);
        multipleRow.setToggleGroup(tg);
        grid.getChildren().add(multipleRow);
        GridPane.setConstraints(multipleRow, 1, 2);

        RadioButton multipleCell = new RadioButton("Multiple Cells");
        multipleCell.getProperties().put(MULTI_SELECT, SelectionMode.MULTIPLE);
        multipleCell.getProperties().put(CELL_SELECT, true);
        multipleCell.setToggleGroup(tg);
        grid.getChildren().add(multipleCell);
        GridPane.setConstraints(multipleCell, 1, 3);

        tg.selectedToggleProperty().addListener(ov -> {
            RadioButton toggle = (RadioButton) tg.getSelectedToggle();
            if (toggle == null) return;

            Map<Object, Object> properties = toggle.getProperties();
            SelectionMode selectMode = (SelectionMode) properties.get(MULTI_SELECT);
            boolean cellSelect = (Boolean) properties.get(CELL_SELECT);

            tableView.getSelectionModel().setSelectionMode(selectMode);
            tableView.getSelectionModel().setCellSelectionEnabled(cellSelect);
        });
        // -- end of selection mode controls

        // toggles
//        // nested columns Toggle
//        final ToggleButton nestColumnsBtn = new ToggleButton("Nest Columns");
//        nestColumnsBtn.setSelected(false);
//        nestColumnsBtn.selectedProperty().addListener(new InvalidationListener() {
//            public void invalidated(Observable ov) {
//                if (nestColumnsBtn.isSelected()) {
//                    tableView.getColumns().setAll(nameCol, emailCol, countryCol);
//                } else {
//                    tableView.getColumns().setAll(firstNameCol, lastNameCol, emailCol, countryCol);
//                }
////                firstNameCol.setVisible(! firstNameCol.isVisible());
//            }
//        });
//        grid.getChildren().add(nestColumnsBtn);
//        GridPane.setConstraints(nestColumnsBtn, 1, 4);
//        // -- end of nested columns toggle

        // show/hide column control button
        final ToggleButton columnControlBtn = new ToggleButton("Column Control");
        columnControlBtn.setSelected(false);
        columnControlBtn.selectedProperty().addListener(ov -> tableView.setTableMenuButtonVisible(columnControlBtn.isSelected()));
        grid.getChildren().add(columnControlBtn);
        GridPane.setConstraints(columnControlBtn, 1, 5);
        // -- end of show/hide column control button

        // pannable button
//        final ToggleButton pannableBtn = new ToggleButton("Pannable");
//        pannableBtn.setSelected(true);
//        pannableBtn.selectedProperty().addListener(new InvalidationListener() {
//            public void invalidated(ObservableValue ov) {
//                tableView.setPannable(pannableBtn.isSelected());
//            }
//        });
//        grid.add(pannableBtn, 1, 6);
        // -- end of pannable button

        // constrained resize button
        final ToggleButton constrainResizeBtn = new ToggleButton("Constrained Resize");
        constrainResizeBtn.setSelected(false);
        constrainResizeBtn.selectedProperty().addListener(ov -> {
             if (constrainResizeBtn.isSelected()) {
                 tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
             } else {
                 tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
             }
        });
        grid.add(constrainResizeBtn, 1, 7);
        // -- end of constrained resize button

        // -- toggles

        // actions
        // Dump to console button (for debugging the tableView observableArrayList)
        Button dumpButton = new Button("Dump to console");
        dumpButton.setOnAction(e -> {
            System.out.println("================================");
            System.out.println("TableView dump:");
            System.out.println("Columns: " + tableView.getColumns());
//                System.out.println("View Columns: " + tableView.getViewColumns());
//                System.out.println("Leaf Columns: " + tableView.getLeafColumns());
            System.out.println("Visible Leaf Columns: " + tableView.getVisibleLeafColumns());
            System.out.println("================================");
        });
        grid.getChildren().add(dumpButton);
        GridPane.setConstraints(dumpButton, 1, 8);
        // -- dump button

        // insert button
        final Button insertBtn = new Button("Insert row");
        insertBtn.setOnAction(t -> data.add(0, new Person("First Name", "Last Name", "Email")));
        grid.getChildren().add(insertBtn);
        GridPane.setConstraints(insertBtn, 1, 9);

        final Button renameEthanBtn = new Button("Rename Ethan");
        renameEthanBtn.setOnAction(t -> data.get(2).setFirstName(new BigInteger(40, new Random()).toString(32)));
        grid.getChildren().add(renameEthanBtn);
        GridPane.setConstraints(renameEthanBtn, 1, 10);

        tab.setContent(grid);


//        tableView.getSelectionModel().selectedIndexProperty().addListener(new InvalidationListener() {
//            public void invalidated(ObservableValue ov) {
//                System.out.println("SelectedIndex: " + tableView.getSelectionModel().getSelectedIndex());
//            }
//        });

//        tableView.getSelectionModel().getSelectedIndices().addListener(new ListChangeListener<Integer>() {
//            public void onChanged(Change<? extends Integer> change) {
//                System.out.println("SelectedIndices: " + change.getList() +
//                        ", removed: " + change.getRemoved() +
//                        ", addedFrom: " + change.getFrom() +
//                        ", addedTo: " + change.getTo());
//            }
//        });

//        tableView.getSelectionModel().getSelectedCells().addListener(new ListChangeListener<TablePosition>() {
//            public void onChanged(Change<? extends TablePosition> change) {
//                System.out.println("SelectedIndices: " + change.getList() +
//                        ", removed: " + change.getRemoved() +
//                        ", addedFrom: " + change.getFrom() +
//                        ", addedTo: " + change.getTo());
//            }
//        });
    }


    private void buildUnsortedTab(Tab tab) {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(5, 5, 5, 5));
        grid.setHgap(5);
        grid.setVgap(5);

        // simple table view with a normal ObservableList
        TableView tableView = buildUnsortedTable(FXCollections.observableArrayList(2, 1, 3));
        grid.add(new Label("TableView w/ ObservableList:"), 0, 0);
        grid.add(tableView, 0, 1);
        GridPane.setVgrow(tableView, Priority.ALWAYS);
        GridPane.setHgrow(tableView, Priority.ALWAYS);
        // --- simple tableview

        // simple table view with a SortedList
        SortedList sortedList = new SortedList(FXCollections.observableArrayList(2, 1, 3));
        TableView tableView1 = buildUnsortedTable(sortedList);
        sortedList.comparatorProperty().bind(tableView1.comparatorProperty());
        grid.add(new Label("TableView w/ SortedList:"), 1, 0);
        grid.add(tableView1, 1, 1);
        GridPane.setVgrow(tableView1, Priority.ALWAYS);
        GridPane.setHgrow(tableView1, Priority.ALWAYS);
        // --- simple tableview

        tab.setContent(grid);
    }

    private TableView buildUnsortedTable(ObservableList collection) {
        final TableView<Integer> tableView = new TableView<Integer>(collection);

        TableColumn<Integer, Number> numberColumn = new TableColumn<>("Numbers");
        numberColumn.setPrefWidth(81);
        numberColumn.setCellValueFactory(param -> new ReadOnlyIntegerWrapper(param.getValue()));
        tableView.getColumns().add(numberColumn);

        return tableView;
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
//
//
//    private Comparator<String> alphabeticalComparator = new Comparator<String>() {
//        @Override public int compare(String o1, String o2) {
//            return o1.compareTo(o2);
//        }
//    };
//
//    private Comparator<String> reverseAlphabeticalComparator = new Comparator<String>() {
//        @Override public int compare(String o1, String o2) {
//            return o2.compareTo(o1);
//        }
//    };

    private void buildSortAndFilterTab(Tab tab) {
        // initially we match everything in the filter list
        Predicate<Person> NO_MATCHER = e -> true;
        Comparator<Person> NO_COMPARATOR = (o1, o2) -> 0;

        final ObservableList<Person> data = createTestData();

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(5, 5, 5, 5));
        grid.setHgap(5);
        grid.setVgap(5);

        // --- unmodified tableView
        final TableView<Person> unmodifiedTableView = new TableView<>();
        unmodifiedTableView.setId("Unmodified table");
        unmodifiedTableView.setItems(data);
        unmodifiedTableView.getColumns().setAll(createFirstNameCol(), createLastNameCol());
        Node unmodifiedLabel = createLabel("Original TableView:");
        grid.getChildren().addAll(unmodifiedLabel, unmodifiedTableView);
        GridPane.setConstraints(unmodifiedLabel, 0, 0);
        GridPane.setConstraints(unmodifiedTableView, 0, 1);
        GridPane.setVgrow(unmodifiedTableView, Priority.ALWAYS);
        // --- unmodified tableView


        // --- sorted tableView
        final SortedList<Person> sortedList1 = new SortedList<Person>(data, NO_COMPARATOR);
        final TableView<Person> sortedTableView = new TableView<Person>();
        sortedTableView.setId("sorted table");
        sortedTableView.setItems(sortedList1);
        sortedList1.comparatorProperty().bind(sortedTableView.comparatorProperty());
        sortedTableView.getColumns().setAll(createFirstNameCol(), createLastNameCol());
        Node sortedLabel = createLabel("Sorted TableView:");
        grid.getChildren().addAll(sortedLabel, sortedTableView);
        GridPane.setConstraints(sortedLabel, 1, 0);
        GridPane.setConstraints(sortedTableView, 1, 1);
        GridPane.setVgrow(sortedTableView, Priority.ALWAYS);
        // --- sorted tableview


        // --- filtered tableview
        final SortedList<Person> sortedList2 = new SortedList<Person>(data, NO_COMPARATOR);
        final FilteredList<Person> filteredList2 = new FilteredList<Person>(sortedList2, NO_MATCHER);
        final TableView<Person> filteredTableView = new TableView<Person>();
        filteredTableView.setId("filtered table");
        filteredTableView.setItems(filteredList2);
        filteredTableView.getColumns().setAll(createFirstNameCol(), createLastNameCol());
        Node filteredLabel = createLabel("Filtered (and sorted) TableView:");
        grid.getChildren().addAll(filteredLabel, filteredTableView);
        GridPane.setConstraints(filteredLabel, 2, 0);
        GridPane.setConstraints(filteredTableView, 2, 1);
        GridPane.setVgrow(filteredTableView, Priority.ALWAYS);
        // --- filtered tableview


        // control buttons
        VBox vbox = new VBox(10);

        vbox.getChildren().add(new Label("Note: Double-click table cells to edit."));

        final TextField filterInput = new TextField();
        filterInput.setPromptText("Enter filter text");
//        filterInput.setColumns(35);
        filterInput.setOnKeyReleased(t -> filteredList2.setPredicate((Person e) -> {
                String input = filterInput.getText().toUpperCase();

                // match against the first and last names
                return e.getFirstName().toUpperCase().contains(input) || e.getLastName().toUpperCase().contains(input);
            }
        ));
        vbox.getChildren().add(filterInput);

        final TextField newItemInput = new TextField();
        newItemInput.setPromptText("Enter \"firstName lastName\", then press enter to add person to table");
//        newItemInput.setColumns(35);
        newItemInput.setOnKeyReleased(t -> {
            if (t.getCode() == KeyCode.ENTER) {
                String[] name = newItemInput.getText().split(" ");
                data.add(new Person(name[0], name[1]));
                newItemInput.setText("");
            }
        });
        vbox.getChildren().add(newItemInput);

        grid.setConstraints(vbox, 3, 1);
        grid.getChildren().add(vbox);

        tab.setContent(grid);
    }

    private TableColumn createFirstNameCol() {
        // first name, last name, and wrapper parent column
        TableColumn<Person, String> firstNameCol = new TableColumn<>();
        firstNameCol.setText("First");
        firstNameCol.setCellValueFactory(new PropertyValueFactory<Person,String>("firstName"));
        firstNameCol.setOnEditCommit(t -> System.out.println("Edit commit event: " + t.getNewValue()));

        return firstNameCol;
    }

    private TableColumn createLastNameCol() {
        TableColumn<Person, String> lastNameCol = new TableColumn<Person, String>();
        lastNameCol.setText("Last");
        lastNameCol.setCellValueFactory(p -> p.getValue().lastNameProperty());
        return lastNameCol;
    }


    private void buildPerformanceTestTab(Tab tab, boolean customCell) {
        GridPane grid = new GridPane();
//        grid.setGridLinesVisible(true);
        grid.setPadding(new Insets(5, 5, 5, 5));
        grid.setHgap(5);
        grid.setVgap(5);

        final ObservableList<List<Double>> bigData = FXCollections.observableArrayList();

        TableView<List<Double>> tableView = new TableView<List<Double>>();
        tableView.setItems(bigData);
        tableView.getColumns().addAll(getColumns(customCell));
        tableView.setLayoutX(30);
        tableView.setLayoutY(150);
        tableView.setPrefSize(1100, 300);
        tableView.setTableMenuButtonVisible(true);

        getLines(bigData);

        tableView.setOnMouseReleased(t -> {
            if (t.getClickCount() == 3) {
                System.out.println("resetting data...");
                bigData.clear();
                getLines(bigData);
                System.out.println("Done");
            }
        });

        int row = 0;
        if (customCell) {
            grid.add(new Label("Note: the CheckBox cells do not persist their state in this demo!" +
                    "\n(This means that if you select checkboxes and scroll, they may not be in the same " +
                    "state once you scroll back."), 0, row++);
        }

        grid.getChildren().addAll(tableView);
        GridPane.setConstraints(tableView,0, row++);
        GridPane.setVgrow(tableView, Priority.ALWAYS);
        GridPane.setHgrow(tableView, Priority.ALWAYS);

        tab.setContent(grid);
    }

    public void getLines(ObservableList<List<Double>> bigData) {
//        List<List<Double>> data = new ArrayList<List<Double>>();
        for (int row = 0; row < NB_LINE; row++) {
            List<Double> line = new ArrayList<Double>();
            for (int col = 0; col <= NB_COL; col++) {
                if(col == 0) line.add((double)row);
                else line.add(Math.random() * 1000);
            }
            bigData.add(line);
        }
    }


    public List<TableColumn<List<Double>,Double>> getColumns(boolean customCell) {
        List<TableColumn<List<Double>,Double>> cols = new ArrayList<TableColumn<List<Double>,Double>>();
        for (int i = 0; i <= NB_COL; i++) {
            TableColumn<List<Double>,Double> col = new TableColumn<List<Double>,Double>("Col" + i);
            final int coli = i;
            col.setCellValueFactory(p -> new ReadOnlyObjectWrapper<Double>(((List<Double>) p.getValue()).get(coli)));

            if (customCell) {
                col.setCellFactory(p -> new TableCell<List<Double>,Double>() {
                    CheckBox chk;

                    @Override public void updateItem(Double item, boolean empty) {
                        super.updateItem(item, empty);

                        if (empty) {
                            setGraphic(null);
                        } else {
                            if (chk == null) {
                                chk = new CheckBox();
                            }

                            chk.setText(item.toString());
                            setGraphic(chk);
                        }
                        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    }
                });
            }

            cols.add(col);
        }
        return cols;
    }


    private Node createLabel(String text) {
        Label label = new Label(text);
        return label;
    }
}
