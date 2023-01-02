package checkboxTreeView;

import java.io.IOException;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class CheckBoxTreeEditor extends Application {

    private final TreeView<String> tree = new TreeView<>(new CheckBoxTreeItem<>("root"));
    private int childNum;

    @Override
    public void start(Stage stage) throws IOException {
        setupTree();
        var borderPane = new BorderPane(tree);
        borderPane.setTop(createToolbar());
        var scene = new Scene(borderPane);
        stage.setScene(scene);
        stage.show();
    }

    private void setupTree() {
        tree.setCellFactory(CheckBoxTreeCell.forTreeView());

        var button = new Button("0");
        tree.getRoot().setGraphic(button);
        tree.getRoot().setExpanded(true);
        tree.getSelectionModel().select(tree.getRoot());

        // add children for initial setup as needed
        addChild(true, true);
//      var c2 = addChild(true, false);
//
//      c1.setSelected(true);
//      c1.setIndeterminate(true);
//
//      c2.setSelected(false);
//      c2.setIndeterminate(true);
//      
//      c1.setIndeterminate(false);
//      c2.setIndeterminate(false);
    }

    private Parent createToolbar() {
        var indeterminate = new CheckBox("Indeterminate");
        var selected = new CheckBox("Selected");

        var add = new Button("Add");
        add.setOnAction(e -> addChild(indeterminate.isSelected(), selected.isSelected()));

        var remove = new Button("Remove");
        remove.setOnAction(e -> removeChild());

        var toolbar = new HBox(5, add, remove, indeterminate, selected);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        return toolbar;
    }

    private CheckBoxTreeItem<String> addChild(boolean indeterminate, boolean selected) {
        var item = new CheckBoxTreeItem<>("child " + childNum++);
        var button = new Button("" + childNum);
        item.setGraphic(button);
        item.setSelected(selected);
        item.setIndeterminate(indeterminate);
        item.setExpanded(true);
        tree.getSelectionModel().getSelectedItem().getChildren().add(item);
        return item;
    }

    private void removeChild() {
        var selectedItem = tree.getSelectionModel().getSelectedItem();
        selectedItem.getParent().getChildren().remove(selectedItem);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
