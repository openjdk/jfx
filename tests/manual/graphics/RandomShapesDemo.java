/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.transform.Affine;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Shows a WritableImage and Canvas side by side performing
 * the same drawing operations.
 */
public class RandomShapesDemo extends Application {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 800;
    private static final int SHAPES = 40;
    private static final String[] TEXTS = {"WritableImage", "Canvas", "&", "@", "1\n2\n3"};

    private final int seed = (int)(1000 * Math.random());
    private final Random rnd = new Random(seed);

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
            new KeyFrame(Duration.ZERO, _ -> addRandomShape(dc, gc)),
            new KeyFrame(Duration.millis(200))
        );

        timeline.setCycleCount(Animation.INDEFINITE);

        button.setOnAction(_ -> {
            if (timeline.getStatus() == Status.RUNNING) {
                timeline.stop();
            }
            else {
                timeline.playFromStart();
            }
        });

        hbox.setSpacing(10);
        hbox.setStyle("-fx-background-color: BLACK; -fx-border-width: 10; -fx-border-color: BLACK;");

        root.setTop(new HBox(button, new Label("seed: " + seed)));
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
        dc.save();
        gc.save();

        double x = rnd.nextDouble() * WIDTH;
        double y = rnd.nextDouble() * HEIGHT;
        double w = 20 + rnd.nextDouble() * 120;
        double h = 20 + rnd.nextDouble() * 120;

        if (rnd.nextBoolean()) {
            Affine rot = new Affine();

            rot.appendRotation(rnd.nextInt(360), x + w / 2, y + h / 2);
            dc.setTransform(rot);
            gc.setTransform(rot);
        }

        Paint fill = randomPaint();
        Paint stroke = randomColor();
        double lineWidth = rnd.nextDouble() * 12 + 1;

        dc.setFill(fill);
        dc.setStroke(stroke);
        dc.setLineWidth(lineWidth);
        gc.setFill(fill);
        gc.setStroke(stroke);
        gc.setLineWidth(lineWidth);

        switch (rnd.nextInt(6)) {
            case 0 -> {
                dc.fillRect(x, y, w, h);
                gc.fillRect(x, y, w, h);
            }
            case 1 -> {
                dc.strokeOval(x, y, w, h);
                gc.strokeOval(x, y, w, h);
            }
            case 2 -> {
                dc.fillOval(x, y, w, h);
                gc.fillOval(x, y, w, h);
            }
            case 3 -> {
                int extent = rnd.nextInt(360);
                ArcType arcType = ArcType.values()[rnd.nextInt(ArcType.values().length)];

                dc.fillArc(x, y, w, h, 0, extent, arcType);
                gc.fillArc(x, y, w, h, 0, extent, arcType);
            }
            case 4 -> {
                Font font = randomFont();
                String text = TEXTS[rnd.nextInt(TEXTS.length)];

                dc.setFont(font);
                gc.setFont(font);
                dc.fillText(text, x, y);
                gc.fillText(text, x, y);
            }
            case 5 -> {
                double dash1 = 2 + rnd.nextDouble() * 15;
                double dash2 = 2 + rnd.nextDouble() * 15;
                double offset = rnd.nextDouble() * (dash1 + dash2);

                dc.setLineDashes(dash1, dash2);
                gc.setLineDashes(dash1, dash2);
                dc.setLineDashOffset(offset);
                gc.setLineDashOffset(offset);
                dc.strokeRect(x, y, w, h);
                gc.strokeRect(x, y, w, h);
            }
        }

        dc.restore();
        gc.restore();
    }

    private Font randomFont() {
        double size = 12 + rnd.nextInt(30);

        return rnd.nextBoolean() ? new Font(size) : Font.font("System", FontWeight.BOLD, size);
    }

    private Paint randomPaint() {
        if (rnd.nextBoolean()) {
            return randomColor();
        }

        double x1 = rnd.nextDouble() * 100;
        double y1 = rnd.nextDouble() * 100;
        double x2 = rnd.nextDouble() * 100;
        double y2 = rnd.nextDouble() * 100;
        Stop stop1 = new Stop(0, randomColor());
        Stop stop2 = new Stop(1, randomColor());

        if (rnd.nextBoolean()) {
            return new LinearGradient(x1, y1, x2, y2, false, CycleMethod.NO_CYCLE, stop1, stop2);
        }

        return new RadialGradient(0, 0, x1, y1, rnd.nextDouble() * 60 + 10, false, CycleMethod.NO_CYCLE, stop1, stop2);
    }

    private Color randomColor() {
        return Color.color(rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
