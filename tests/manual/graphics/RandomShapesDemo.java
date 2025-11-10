import java.util.Random;

import javafx.animation.Animation;
import javafx.animation.Animation.Status;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.DrawingContext;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Shows a WritableImage and Canvas side by side performing
 * the same drawing operations.
 */
public class RandomShapesDemo extends Application {
    private static final int WIDTH = 400;
    private static final int HEIGHT = 400;
    private static final int SHAPES = 20;

    private final Random rnd = new Random();

    @Override
    public void start(Stage primaryStage) {
        WritableImage wimg = new WritableImage(WIDTH, HEIGHT);
        DrawingContext dc = wimg.getDrawingContext();
        ImageView imageView = new ImageView(wimg);
        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        dc.setFill(Color.WHITE);
        dc.fillRect(0, 0, WIDTH, HEIGHT);

        drawRandomShapes(dc, gc);

        BorderPane root = new BorderPane();
        Label label = new Label("WritableImage in ImageView");
        Label label2 = new Label("Canvas");

        label.setStyle("-fx-text-fill: white; -fx-font-weight: bold");
        label2.setStyle("-fx-text-fill: white; -fx-font-weight: bold");

        HBox hbox = new HBox(10, new VBox(label, imageView), new VBox(label2, canvas));
        Button button = new Button("Toggle Animation");

        Timeline timeline = new Timeline(
          new KeyFrame(Duration.ZERO, e -> addRandomShape(dc, gc)),
          new KeyFrame(Duration.millis(200))
        );

        timeline.setCycleCount(Animation.INDEFINITE);

        button.setOnAction(e -> {
          if(timeline.getStatus() == Status.RUNNING) {
            timeline.stop();
          }
          else {
            timeline.playFromStart();
          }
        });

        hbox.setSpacing(10);
        hbox.setStyle("-fx-background-color: BLACK; -fx-border-width: 10; -fx-border-color: BLACK;");

        root.setTop(button);
        root.setCenter(hbox);

        Scene scene = new Scene(root);

        primaryStage.setScene(scene);
        primaryStage.setTitle("Random Shapes: WritableImage vs Canvas");
        primaryStage.show();
    }

    private void drawRandomShapes(DrawingContext ctx, GraphicsContext gc) {
        for (int i = 0; i < SHAPES; i++) {
            addRandomShape(ctx, gc);
        }
    }

    private void addRandomShape(DrawingContext dc, GraphicsContext gc) {
      Color randomFill = randomColor();
      Color randomStroke = randomColor();
      double width = rnd.nextDouble() * 10;

      dc.setFill(randomFill);
      dc.setStroke(randomStroke);
      dc.setLineWidth(width);
      gc.setFill(randomFill);
      gc.setStroke(randomStroke);
      gc.setLineWidth(width);

      double x = rnd.nextDouble() * WIDTH;
      double y = rnd.nextDouble() * HEIGHT;
      double w = 20 + rnd.nextDouble() * 80;
      double h = 20 + rnd.nextDouble() * 80;

      if (rnd.nextBoolean()) {
          dc.fillRect(x, y, w, h);
          gc.fillRect(x, y, w, h);
      }
      else {
          dc.strokeOval(x, y, w, h);
          gc.strokeOval(x, y, w, h);
      }
    }

    private Color randomColor() {
        return Color.color(rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
