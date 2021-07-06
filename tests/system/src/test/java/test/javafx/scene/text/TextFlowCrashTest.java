package test.javafx.scene.text;

import org.junit.BeforeClass;
import javafx.application.Platform;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.layout.VBox;
import org.junit.Test;
import test.util.Util;

public class TextFlowCrashTest {

    private boolean exceptionWasThrown;

    @BeforeClass
    public static void initFX() throws Exception {
        CountDownLatch startupLatch = new CountDownLatch(1);
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            startupLatch.countDown();
        });
        assertTrue("Timeout waiting for FX runtime to start", startupLatch.await(15, TimeUnit.SECONDS));
    }

    @Test
    public void testTextflowCrash() {
        Util.runAndWait(() -> {
            Stage stage = new Stage();
            VBox root = new VBox();
            onEveryNode(root);
            Platform.runLater(() -> {
                root.getChildren().add(getBuggyNode());
            });
            stage.setScene(new Scene(root,
                    200,
                    200));
            stage.show();
        });

        Util.runAndWait(() -> {
            assertFalse(exceptionWasThrown);
        });
    }

    public ScrollPane getBuggyNode() {
        ListView<String> listView = new ListView();
        listView.getItems().add("AAA");
        listView.setCellFactory((view) -> {
            ListCell cell = new ListCell();
            TextFlow flow = new TextFlow();
            flow.getChildren().add(new Text("a"));
            Text text2 = new Text("b");
            text2.sceneProperty().addListener((p,o,n) -> {
                try {
                    text2.getBoundsInParent();
                } catch (Throwable e) {
                    exceptionWasThrown = true;
                    throw e;
                }
            });
            flow.getChildren().add(text2);
            cell.setGraphic(flow);
            onEveryNode(cell);
            return cell;
        });
        ScrollPane scrollPane = new ScrollPane(listView);
        onEveryNode(listView);
        onEveryNode(scrollPane);
        return scrollPane;
    }

    public void onEveryNode(Node node) {
        node.boundsInParentProperty().addListener((p,o,n) -> {
        });
    }
}
