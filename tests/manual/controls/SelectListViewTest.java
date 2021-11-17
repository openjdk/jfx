import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class SelectListViewTest extends Application {

    final int ROW_COUNT = 70_000;
    //	final int ROW_COUNT = 400_000;
    //	final int ROW_COUNT = 10_000_000;
	// final int ROW_COUNT = 7_000;

    @Override
    public void start(Stage stage) {
        ListView<String> listView = new ListView<>();
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        ObservableList<String> items = listView.getItems();
        for(int i=0; i<ROW_COUNT; i++) {
            String rec = String.valueOf(i);
            items.add(rec);
        }

        BorderPane root = new BorderPane(listView);
        Button selectAll = new Button("selectAll");
        Button clearSelection = new Button("clearSelection");
        Button selectToStart = new Button("selectToStart");
        Button selectToEnd = new Button("selectToEnd");
        Button selectPrevious = new Button("selectPrevious");
        Button selectNext= new Button("selectNext");

        selectAll.setFocusTraversable(true);
        clearSelection.setFocusTraversable(true);
        selectToStart.setFocusTraversable(true);
        selectToEnd.setFocusTraversable(true);
        selectPrevious.setFocusTraversable(true);
        selectNext.setFocusTraversable(true);

        root.setRight(new VBox(6, selectAll, selectToStart, selectToEnd, selectPrevious, selectNext, clearSelection));
        stage.setScene(new Scene(root, 600, 600));

        selectAll.setOnAction((e)->selectAll(listView));
        clearSelection.setOnAction((e)->clearSelection(listView));
        selectToStart.setOnAction((e)->selectToStart(listView));
        selectToEnd.setOnAction((e)->selectToLast(listView));
        selectPrevious.setOnAction((e)->selectPrevious(listView));
        selectNext.setOnAction((e)->selectNext(listView));

        stage.show();
    }

    private void selectAll(ListView listView) {
        long t = System.currentTimeMillis();
        listView.getSelectionModel().selectAll();
        System.out.println("time:"+ (System.currentTimeMillis() - t));
    }
    private void clearSelection(ListView listView) {
        long t = System.currentTimeMillis();
        listView.getSelectionModel().clearSelection();
        System.out.println("time:"+ (System.currentTimeMillis() - t));
    }
    private void selectToStart(ListView listView) {
        long t = System.currentTimeMillis();
        listView.getSelectionModel().selectRange(0, listView.getSelectionModel().getSelectedIndex());
        System.out.println("time:"+ (System.currentTimeMillis() - t));
    }
    private void selectToLast(ListView listView) {
        long t = System.currentTimeMillis();
        listView.getSelectionModel().selectRange(listView.getSelectionModel().getSelectedIndex(), listView.getItems().size());
        System.out.println("time:"+ (System.currentTimeMillis() - t));
    }

    private void selectPrevious(ListView listView) {
        long t = System.currentTimeMillis();
        listView.getSelectionModel().selectPrevious();
        System.out.println("time:"+ (System.currentTimeMillis() - t));
    }

    private void selectNext(ListView listView) {
        long t = System.currentTimeMillis();
        listView.getSelectionModel().selectNext();
        System.out.println("time:"+ (System.currentTimeMillis() - t));
    }
    public static void main(String[] args) {
        Application.launch(args);
    }
}
