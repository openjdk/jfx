package table;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.skin.TableViewSkin;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 * Testing the refresh() method of a TableView with many columns and rows.
 * This takes a little bit since all rows and cells need to be refreshed,
 * that is the updateItem(..) method is called.
 */
public class TableRefreshTest {

    public static void main(String[] args) {
        Application.launch(FxApp.class, args);
    }

    public static class FxApp extends Application {

        @Override
        public void start(Stage primaryStage) {
            TableView<String> tv = new TableView<>();
            tv.setSkin(new CTableViewSkin<>(tv));

            for (int i = 0; i < 1000; i++) {
                tv.getItems().add("str: " + i);
            }

            for (int index = 0; index < 100; index++) {
                TableColumn<String, String> tc = new TableColumn<>("title: " + index);
                tc.setCellValueFactory(cdf -> new SimpleStringProperty(cdf.getValue()));

                tv.getColumns().add(tc);
            }

            BorderPane root = new BorderPane();
            root.setPadding(new Insets(4));
            root.setCenter(tv);

            Button refreshBtn = new Button("Refresh");
            refreshBtn.setOnAction(_ -> tv.refresh());
            Button recreateBtn = new Button("Recreate");
            recreateBtn.setOnAction(_ -> tv.getProperties().put("recreateKey", true));
            root.setBottom(new HBox(4, refreshBtn, recreateBtn));

            Scene scene = new Scene(root, 1800, 960);
            primaryStage.setScene(scene);
            primaryStage.show();
        }
    }

    private static class CTableViewSkin<T> extends TableViewSkin<T> {

        CTableViewSkin(TableView<T> control) {
            super(control);
        }

        @Override
        protected VirtualFlow<TableRow<T>> createVirtualFlow() {
            return new CVirtualFlow<>();
        }
    }

    private static class CVirtualFlow<S extends IndexedCell> extends VirtualFlow<S> {

        @Override
        protected void layoutChildren() {
            long startTime = System.nanoTime();
            super.layoutChildren();
            System.out.println("Took: " + ((System.nanoTime() - startTime) / 1_000_000) + " ms");
        }
    }

}
