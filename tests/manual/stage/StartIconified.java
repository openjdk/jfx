import javafx.application.Application;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;

/***
 * Stage must initially only show on the OS taskbar, but not on the Screen.
 * If the stage pops on the Screen and then iconifies, it's wrong.
 */
public class StartIconified extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Iconified Window Test");
        primaryStage.setWidth(600);
        primaryStage.setHeight(150);
        primaryStage.setIconified(true);

        Text text = new Text("""
                1. The stage must initially appear on the OS taskbar (iconified), but not on the Screen
                2. Observe if the stage pops and then iconifies (wrong)""");

        Scene scene = new Scene(new StackPane(text));
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(StartIconified.class, args);
    }
}
