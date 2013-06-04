package animations;

import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

/** User: rbair Date: 5/31/13 Time: 2:06 PM */
public class TranslatedRectangle extends Application {

    @Override public void start(Stage primaryStage) throws Exception {
        Rectangle r = new Rectangle(100, 100);
        Group root = new Group(r);
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();

        TranslateTransition tx = new TranslateTransition(Duration.seconds(5), r);
        tx.setInterpolator(Interpolator.LINEAR); // So as to make any Jitter obvious
        tx.setFromX(0);
        tx.setFromY(250);
        tx.setToX(700);
        tx.setToY(250);
        tx.setCycleCount(TranslateTransition.INDEFINITE);
        tx.setAutoReverse(true);
        tx.play();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
