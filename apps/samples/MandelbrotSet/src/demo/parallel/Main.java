/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package demo.parallel;


import java.util.List;
import java.util.Locale;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.HPos;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Background;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import static java.lang.Math.*;
import javafx.beans.property.BooleanProperty;
import javafx.concurrent.Worker;
import javafx.scene.control.ProgressIndicator;


/**
 * UI main class for MandelbrotSet demo.
 *
 * <p><i>
 * This source code is provided to illustrate the usage of a given feature
 * or technique and has been deliberately simplified. Additional steps
 * required for a production-quality application, such as security checks,
 * input validation and proper error handling, might not be present in
 * this sample code.</i>
 *
 * @author Alexander Kouznetsov, Tristan Yan
 */
public class Main extends Application {

    /**
     * Current position in fractal
     */
    private Position position;

    /**
     * Position that shows the whole fractal
     */
    private Position global;

    /**
     * Window width
     */
    private double winWidth = 800;

    /**
     * Window height
     */
    private double winHeight = 600;

    /**
     * Horizontal position of Drag-and-Pressed gesture start point
     */
    private double gestureX = 0;

    /**
     * Vertical position of Drag-and-Pressed gesture start point
     */
    private double gestureY = 0;

    /**
     * Root pane of the scene content
     */
    private Pane rootPane;

    /**
     * Calculation task
     */
    private MandelbrotSetTask task;

    /**
     * Sequential calculation task
     */
    private MandelbrotSetTask sequentialTask;

    /**
     * Parallel calculation task
     */
    private MandelbrotSetTask parallelTask;

    /**
     * Image to draw fractal offscreen
     */
    private WritableImage wiOffscreen;

    /**
     * Snapshot of the last fractal image
     */
    private WritableImage wiSnapshot;

    /**
     * Snapshot of the whole fractal
     */
    private WritableImage wiGlobalSnapshot;

    /**
     * Parameters used to make fractal snapshots
     */
    private final SnapshotParameters snapshotParameters = new SnapshotParameters();

    /**
     * Canvas to present fractal on screen
     */
    private Canvas canvas;

    /**
     * Image view to present canvas snapshot on screen
     */
    private ImageView ivCanvasSnapshot = new ImageView();

    /**
     * Image view to present whole fractal snapshot during fly animation
     */
    private ImageView ivGlobalSnapshot = new ImageView();

    /**
     * Old rootPane origin coordinates
     */
    private double oldX, oldY;

    /**
     * New rootPane origin coordinates
     */
    private double newX, newY;

    /**
     * Property to disable all app controls during fly animation
     */
    private BooleanProperty disable;

    /**
     * Property to update stage title
     */
    private StringProperty stageTitle;

    /**
     * Time bar relative length for parallel calculation
     */
    private DoubleProperty parallelTimeBar;

    /**
     * Time bar relative length for sequential calculation
     */
    private DoubleProperty sequentialTimeBar;

    /**
     * Progress of the current task
     */
    private DoubleProperty progress;

    /**
     * Time in milliseconds of parallel calculation
     */
    private final LongProperty parallelTimeValue = new SimpleLongProperty();

    /**
     * Time in milliseconds of sequential calculation
     */
    private final LongProperty sequentialTimeValue = new SimpleLongProperty();

    /**
     * Total time of sequential calculation (for comparison)
     */
    private double sequentialTotalTime;

    /**
     * Instance of current flying animation
     */
    private FlyingAnimation flyingAnimation;

    /**
     * Creates control pane controls on top
     */
    private Parent createControlPane() {
        ProgressIndicator progressIndicator = new ProgressIndicator(0);
        progressIndicator.setOnMouseClicked(t -> rerender());
        progress = progressIndicator.progressProperty();

        Button loc0Button = new Button("0");
        loc0Button.setOnAction(t -> flyToPosition(0));

        Button loc1Button = new Button("1");
        loc1Button.setOnAction(t -> flyToPosition(1));

        Button loc2Button = new Button("2");
        loc2Button.setOnAction(t -> flyToPosition(2));

        Button loc3Button = new Button("3");
        loc3Button.setOnAction(t -> flyToPosition(3));

        Button compareButton = new Button("Compare");
        compareButton.setId("compare-button");
        compareButton.setOnAction(t -> startComparison());

        Label sequentialLabel = new Label("Sequential");

        Label sequentialTime = new Label("0:00.00");
        sequentialTime.textProperty().bind(new TimeToStringBinding(sequentialTimeValue));

        ProgressBar sequentialProgressBar = new ProgressBar(0);
        sequentialProgressBar.getStyleClass().add("time");
        sequentialTimeBar = sequentialProgressBar.progressProperty();

        Label parallelLabel = new Label("Parallel");

        Label parallelTime = new Label("0:00.00");
        parallelTime.textProperty().bind(new TimeToStringBinding(parallelTimeValue));

        ProgressBar parallelProgressBar = new ProgressBar(0);
        parallelProgressBar.getStyleClass().add("time");
        parallelTimeBar = parallelProgressBar.progressProperty();

        Region region = new Region();
        region.setId("spacer");

        ToggleButton openCloseButton = new ToggleButton("V");
        openCloseButton.setId("open-close-toggle-button");

        GridPane grid = new GridPane();
        int rowIndex = 0;
        int colIndex = 0;
        grid.add(loc0Button, colIndex++, rowIndex, 1, 2);
        grid.add(loc1Button, colIndex++, rowIndex, 1, 2);
        grid.add(loc2Button, colIndex++, rowIndex, 1, 2);
        grid.add(loc3Button, colIndex++, rowIndex, 1, 2);
        grid.add(progressIndicator, colIndex++, rowIndex, 1, 2);
        grid.add(compareButton, colIndex++, rowIndex, 1, 2);
        int colNonSpan = colIndex;
        grid.add(sequentialLabel, colIndex++, rowIndex);
        grid.add(sequentialProgressBar, colIndex++, rowIndex);
        grid.add(sequentialTime, colIndex++, rowIndex);
        int totalColumns = colIndex;
        rowIndex++;
        colIndex = colNonSpan;
        grid.add(parallelLabel, colIndex++, rowIndex);
        grid.add(parallelProgressBar, colIndex++, rowIndex);
        grid.add(parallelTime, colIndex++, rowIndex);
        rowIndex++;
        colIndex = 0;
        grid.add(region, colIndex, rowIndex);
        rowIndex++;
        grid.add(openCloseButton, colIndex, rowIndex, totalColumns, 1);
        GridPane.setHalignment(openCloseButton, HPos.CENTER);
        grid.setId("grid");
        grid.getStylesheets().add("/demo/parallel/ControlPane.css");

        sequentialTime.translateXProperty().bind(
                sequentialTimeBar.add(-1.1).multiply(sequentialProgressBar.widthProperty()));
        parallelTime.translateXProperty().bind(
                parallelTimeBar.add(-1.1).multiply(parallelProgressBar.widthProperty()));

        openCloseButton.setOnAction(t -> {
            grid.setLayoutY(openCloseButton.isSelected() ? -grid.sceneToLocal(openCloseButton.localToScene(0, 0)).getY() : 0);
        });

        disable = grid.disableProperty();

        return grid;
    }

    /**
     * Creates content of the scene.
     */
    private Parent createContent(double minR, double minI, double maxR, double maxI) {

        Parent controlPane = createControlPane();

        wiOffscreen = new WritableImage((int) winWidth, (int) winHeight);
        wiSnapshot = new WritableImage((int) winWidth, (int) winHeight);
        canvas = new Canvas(winWidth, winHeight);
        render(() -> {
            wiGlobalSnapshot = new WritableImage(wiOffscreen.getPixelReader(), (int) winWidth, (int) winHeight);
            ivGlobalSnapshot.setImage(wiGlobalSnapshot);
            flyToPosition(minR, minI, maxR, maxI);
        });
        global = new Position(position);

        /**
         * When user triggered scroll event, we zoom in/out windows by given
         * direction. Here scroll down means zoom out, scroll up means zoom in
         */
        canvas.setOnScroll(t -> {
            if (disable.get()) {
                return;
            }
            double x = t.getX();
            double y = t.getY();
            double scaleBase = t.isControlDown() ? 1.1 : t.isShiftDown() ? 10 : 2;
            double byScale = (t.getDeltaY() > 0) ? 1 / scaleBase : scaleBase;
            handleContentZoomed(x, y, byScale);
            t.consume();
        });

        //Fetch position when Mouse pressed
        canvas.setOnMousePressed(e -> {
            if (disable.get()) {
                return;
            }
            gestureX = e.getSceneX();
            gestureY = e.getSceneY();
        });

        canvas.setOnMouseDragged(e -> {
            if (disable.get()) {
                return;
            }
            canvas.setTranslateX(e.getSceneX() - gestureX);
            canvas.setTranslateY(e.getSceneY() - gestureY);
        });

        Translate antiTranslate = new Translate();
        antiTranslate.xProperty().bind(canvas.translateXProperty().negate());
        antiTranslate.yProperty().bind(canvas.translateYProperty().negate());
        snapshotParameters.setTransform(antiTranslate);
        snapshotParameters.setFill(Color.BLACK);

        //Fetch position when Mouse released
        canvas.setOnMouseReleased(e -> {
            if (disable.get()) {
                return;
            }
            double moveX = Math.min(Math.max(e.getSceneX() - gestureX, -winWidth), winWidth * 2);
            double moveY = Math.min(Math.max(e.getSceneY() - gestureY, -winHeight), winHeight * 2);
            //Only redraw when there is a movement
            if (moveX != 0 || moveY != 0) {
                handleContentMoved(moveX, moveY);
            }
            e.consume();
        });

        rootPane = new Pane(canvas, controlPane) {

            {
                setBackground(Background.EMPTY);
            }

            @Override
            protected void layoutChildren() {
                super.layoutChildren();
                controlPane.setLayoutX((rootPane.getWidth() - controlPane.getLayoutBounds().getWidth()) / 2);
            }
        };

        new AnimationTimer() {

            @Override
            public void handle(long l) {
                handleFrame();
            }
        }.start();
        return rootPane;
    }

    private void flyToPosition(int loc) {
        clearComparisonValues();
        switch (loc) {
            case 0:
                flyToPosition(-2.4451320039285465, -1.3061943784663503, 0.9425352568739851, 1.2879652356695286);
                break;
            case 1:
                flyToPosition(-1.4831212866723549, -0.026946715467747517, -1.4831211655199326, -0.026946649881416845);
                break;
            case 2:
                flyToPosition(-0.6512456310112382, -0.4797642161720457, -0.651219785161165, -0.4797444243048724);
                break;
            case 3:
                flyToPosition(0.38835929484388515, -0.23577130937499838, 0.39102329484388804, -0.2337313093749984);
                break;
        }
    }

    /**
     * The following method is invoked each JavaFX frame to update current image
     * with what was done on other threads.
     *
     * It also reacts on window resize
     */
    private void handleFrame() {
        if (winWidth != rootPane.getWidth() || winHeight != rootPane.getHeight()) {
            handleWindowResize();
        }
        if (task != null) {
            progress.set(task.getProgress());
            if (!task.isCancelled() && task.hasUpdates()) {
                task.clearHasUpdates();
                canvas.getGraphicsContext2D().drawImage(wiOffscreen, 0, 0, wiOffscreen.getWidth(), wiOffscreen.getHeight(), 0, 0, winWidth, winHeight);
            }
            updateTime();
            if (task.isDone()) {
                task = null;
            }
        }
        oldX = newX;
        oldY = newY;
    }

    /**
     * Updates bars and labels with the current task time in comparison mode
     */
    private void updateTime() {
        if (task != null) {
            if (task == parallelTask) {
                long time = task.getTime();
                parallelTimeBar.set(time / sequentialTotalTime);
                parallelTimeValue.set(time);
            } else if (task == sequentialTask) {
                long time = task.getTime();
                sequentialTimeBar.set(task.getProgress());
                sequentialTimeValue.set(time);
            }
        }
    }

    private void handleContentZoomed(final double x, final double y, final double byScale) {
        stopTask();

        double oldMinR = position.getMinReal();
        double oldMinI = position.getMinImg();
        double oldMaxR = position.getMaxReal();
        double oldMaxI = position.getMaxImg();
        double oldScale = position.scale;

        double zoomCenterReal = position.real + (x - winWidth / 2) * position.scale;
        double zoomCenterImg = position.img + (y - winHeight / 2) * position.scale;

        double newScale = oldScale * byScale;
        double newMinR = zoomCenterReal - x * newScale;
        double newMinI = zoomCenterImg - y * newScale;
        double newMaxR = newMinR + newScale * winWidth;
        double newMaxI = newMinI + newScale * winHeight;
        setPosition(newMinR, newMinI, newMaxR, newMaxI);

        canvas.snapshot(snapshotParameters, wiSnapshot);

        double minR = Math.max(position.getMinReal(), oldMinR);
        double minI = Math.max(position.getMinImg(), oldMinI);
        double maxR = Math.min(position.getMaxReal(), oldMaxR);
        double maxI = Math.min(position.getMaxImg(), oldMaxI);

        // x = (re - minR) / scale
        double sx = Math.max(0, (minR - oldMinR) / oldScale);
        double sy = Math.max(0, (minI - oldMinI) / oldScale);
        double sw = Math.min(winWidth, (maxR - oldMinR) / oldScale) - sx;
        double sh = Math.min(winHeight, (maxI - oldMinI) / oldScale) - sy;
        double dx = Math.max(0, (minR - newMinR) / newScale);
        double dy = Math.max(0, (minI - newMinI) / newScale);
        double dw = Math.min(winWidth, (maxR - newMinR) / newScale) - dx;
        double dh = Math.min(winHeight, (maxI - newMinI) / newScale) - dy;

        canvas.getGraphicsContext2D().clearRect(0, 0, winWidth, winHeight);
        canvas.getGraphicsContext2D().drawImage(wiSnapshot, sx, sy, sw, sh, dx, dy, dw, dh);

        render(null);
    }

    private void handleContentMoved(double moveX, double moveY) {
        boolean cancelled = stopTask();

        double realMove = position.scale * moveX;
        double imgMove = position.scale * moveY;
        position.real -= realMove;
        position.img -= imgMove;
        double sx = Math.max(0, -moveX);
        double sy = Math.max(0, -moveY);
        double sw = Math.min(winWidth, -moveX + winWidth) - sx;
        double sh = Math.min(winHeight, -moveY + winHeight) - sy;
        double dx = Math.max(0, moveX);
        double dy = Math.max(0, moveY);
        canvas.snapshot(snapshotParameters, wiSnapshot);
        canvas.getGraphicsContext2D().clearRect(0, 0, winWidth, winHeight);
        canvas.getGraphicsContext2D().drawImage(wiSnapshot, sx, sy, sw, sh, dx, dy, sw, sh);
        canvas.setTranslateX(0);
        canvas.setTranslateY(0);

        if (cancelled) {
            render(null);
        } else {
            render(null, dx, dy, dx + sw, dy + sh);
        }
    }

    private void handleWindowResize() {
        boolean cancelled;
        if (flyingAnimation != null) {
            flyingAnimation.abort();
            cancelled = true;
        } else {
            cancelled = stopTask();
        }

        double moveX = newX - oldX;
        double moveY = newY - oldY;

        double minR = position.getMinReal() + position.scale * moveX;
        double minI = position.getMinImg() + position.scale * moveY;

        double oldWidth = winWidth;
        double oldHeight = winHeight;
        winWidth = rootPane.getWidth();
        winHeight = rootPane.getHeight();

        canvas.snapshot(snapshotParameters, wiSnapshot);

        canvas.setWidth(winWidth);
        canvas.setHeight(winHeight);

        wiOffscreen = new WritableImage((int) winWidth, (int) winHeight);

        double maxR = minR + position.scale * winWidth;
        double maxI = minI + position.scale * winHeight;
        setPosition(minR, minI, maxR, maxI);

        // make sure global snapshot is centered
        ivGlobalSnapshot.setLayoutX((winWidth - ivGlobalSnapshot.getLayoutBounds().getWidth()) / 2);
        ivGlobalSnapshot.setLayoutY((winHeight - ivGlobalSnapshot.getLayoutBounds().getHeight()) / 2);

        // all coordinates in "after move" coordinate space
        double minX = Math.max(0, -moveX);
        double minY = Math.max(0, -moveY);
        double maxX = Math.min(winWidth, -moveX + oldWidth);
        double maxY = Math.min(winHeight, -moveY + oldHeight);

        if (maxX > minX && maxY > minY) {
            double sx = minX + moveX;
            double sy = minY + moveY;
            double sw = maxX - minX;
            double sh = maxY - minY;
            double dx = minX;
            double dy = minY;
            canvas.getGraphicsContext2D().clearRect(0, 0, winWidth, winHeight);
            canvas.getGraphicsContext2D().drawImage(wiSnapshot, sx, sy, sw, sh, dx, dy, sw, sh);

            if (cancelled) {
                render(null);
            } else {
                render(null, dx, dy, dx + sw, dy + sh);
            }
        } else {
            render(null);
        }

        wiSnapshot = new WritableImage((int) winWidth, (int) winHeight);

        oldX = newX;
        oldY = newY;
    }

    /**
     * Stops (cancels) the current task.
     * @return true if there was unfinished task running
     */
    private boolean stopTask() {
        if (task != null) {
            task.cancel();
            boolean cancelled = task.isCancelled() || task.getState() == Worker.State.READY;
            task = null;
            return cancelled;
        }
        return false;
    }

    /**
     * Renders the whole image for the current position in parallel mode.
     * @param onDone Runnable to execute when task finishes
     */
    private void render(Runnable onDone) {
        render(false, true, onDone, 0, 0, 0, 0, false);
    }

    /**
     * Renders the whole image for the current position with given parameters.
     * @param compareMode comparison mode
     * @param parallel parallel mode vs. sequential
     * @param onDone Runnable to execute when task is finished
     */
    private void render(boolean compareMode, boolean parallel, Runnable onDone) {
        render(compareMode, parallel, onDone, 0, 0, 0, 0, false);
    }

    /**
     * Renders the whole image for the current position in fast mode (not
     * antialiased)
     * @param onDone Runnable to execute when task is finished
     */
    private void renderFast(Runnable onDone) {
        render(false, true, onDone, 0, 0, 0, 0, true);
    }

    /**
     * Renders the whole image except for a rectangular area
     * for the current position in parallel mode
     *
     * @param onDone Runnable to execute when task is finished
     * @param minX min x coordinate of a rectangular area to be skipped
     * @param minY min y coordinate of a rectangular area to be skipped
     * @param maxX max x coordinate of a rectangular area to be skipped
     * @param maxY max y coordinate of a rectangular area to be skipped
     */
    private void render(Runnable onDone, double minX, double minY, double maxX, double maxY) {
        render(false, true, onDone, minX, minY, maxX, maxY, false);
    }

    /**
     * Renders a MandelbrotSet image using provided parameters. See {@link
     * MandelbrotSetTask} for more information.
     *
     * @param compareMode true if in comparison mode
     * @param parallel true for parallel, false for sequential
     * @param onDone Runnable to execute when task is finished
     * @param minX min x coordinate of a rectangular area to be skipped
     * @param minY min y coordinate of a rectangular area to be skipped
     * @param maxX max x coordinate of a rectangular area to be skipped
     * @param maxY max y coordinate of a rectangular area to be skipped
     * @param fast true to disable antialiasing
     */
    private void render(boolean compareMode, boolean parallel, Runnable onDone, double minX, double minY, double maxX, double maxY, boolean fast) {
        // double checking
        stopTask();

        task = new MandelbrotSetTask(parallel, wiOffscreen.getPixelWriter(),
                (int) winWidth, (int) winHeight,
                position.getMinReal(), position.getMinImg(),
                position.getMaxReal(), position.getMaxImg(),
                minX, minY, maxX, maxY, fast);
        if (compareMode) {
            if (parallel) {
                parallelTask = task;
            } else {
                sequentialTask = task;
            }
        }
        new Thread(task, "Task to render MandelbrotSet").start();
        stageTitle.set("Mandelbrot Set Demo (RENDERING...)");
        task.setOnSucceeded(t -> {
            stageTitle.set("Mandelbrot Set Demo");
            progress.set(1);

            if (onDone != null) {
                onDone.run();
            }

            updateTime();
        });
    }

    /**
     * {@inheritDoc}
     * @param primaryStage
     */
    @Override public void start(Stage primaryStage) {
        stageTitle = primaryStage.titleProperty();

        double minR = -2.4451320039285465;
        double maxR = 0.9425352568739851;
        double minI = -1.3061943784663503;
        double maxI = 1.2879652356695286;
        winWidth = 800.0;
        winHeight = 600.0;
        setPosition(minR, minI, maxR, maxI);

        final Parameters params = getParameters();
        List<String> parameters = params.getRaw();

        if ((parameters.size() & 0x01) == 0x01) {
            parameters = parameters.subList(0, parameters.size() - 1);
        }

        for (int paramPos = 0; paramPos + 1 < parameters.size(); paramPos += 2) {
            try {
                switch (parameters.get(paramPos)) {
                    case "-windowSize":
                        String[] windowSizes = parameters.get(paramPos + 1).split("x");
                        winWidth = Double.parseDouble(windowSizes[0]);
                        winHeight = Double.parseDouble(windowSizes[1]);
                        break;
                    case "-max":
                        String[] maxComplex = parameters.get(paramPos + 1).split(",");
                        maxR = Double.parseDouble(maxComplex[0]);
                        maxI = Double.parseDouble(maxComplex[1]);
                        break;
                    case "-min":
                        String[] minComplex = parameters.get(paramPos + 1).split(",");
                        minR = Double.parseDouble(minComplex[0]);
                        minI = Double.parseDouble(minComplex[1]);
                        break;
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid parameters: " + e.getMessage());
                return;
            }
        }

        Scene scene = new Scene(createContent(minR, minI, maxR, maxI), MandelbrotSetTask.colors[1]);
        scene.setOnKeyPressed(t -> {
            if (t.getCode() == KeyCode.I) {
                printInfo();
            } else if (t.getCode() == KeyCode.R) {
                rerender();
            }
        });

        primaryStage.setScene(scene);

        primaryStage.xProperty().addListener(o -> {
            Point2D p = rootPane.localToScreen(0, 0);
            oldX = newX;
            newX = p.getX();
        });
        primaryStage.yProperty().addListener(o -> {
            Point2D p = rootPane.localToScreen(0, 0);
            oldY = newY;
            newY = p.getY();
        });

        primaryStage.show();
    }

    private void startComparison() {
        stopTask();
        clearComparisonValues();
        rerender(true, false, () -> {
            sequentialTotalTime = sequentialTask.getValue();
            rerender(true, true, null);
        });
    }

    private void clearComparisonValues() {
        parallelTimeValue.set(0);
        parallelTimeBar.set(0);
        sequentialTimeValue.set(0);
        sequentialTimeBar.set(0);
    }

    private void rerender() {
        rerender(false, true, null);
    }

    private void rerender(boolean compareMode, boolean parallel, Runnable onDone) {
        stopTask();
        canvas.getGraphicsContext2D().setFill(Color.rgb(0, 0, 0, 0.5));
        canvas.getGraphicsContext2D().fillRect(0, 0, winWidth, winHeight);
        render(compareMode, parallel, onDone);
    }

    private void printInfo() {
        System.out.println("Use the following parameters to get to the same position");
        System.out.println("-min " + position.getMinReal() + "," + position.getMinImg());
        System.out.println("-max " + position.getMaxReal() + "," + position.getMaxImg());
        System.out.println("-windowSize " + winWidth + "x" + winHeight + ";");
    }

    /**
     * {@inheritDoc }
     * @throws java.lang.Exception
     */
    @Override public void stop() throws Exception {
        super.stop();
        if (task != null) {
            task.cancel(true);
        }
    }

    /**
     * Java main for when running without JavaFX launcher
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    private void flyToPosition(double minR, double minI, double maxR, double maxI) {
        Position from = new Position(position);
        Position to = new Position(minR, minI, maxR, maxI);
        if (!from.equals(to)) {
            flyingAnimation = new FlyingAnimation(from, to);
            flyingAnimation.start();
        }
    }

    private void setPosition(double minR, double minI, double maxR, double maxI) {
        position = new Position(minR, minI, maxR, maxI);
    }

    /**
     * Represents a viewport position with a fractal
     */
    private class Position {

        /**
         * Real and imaginary coordinates of the center of the window
         */
        double real, img;

        /**
         * Scale of the fractal in terms of real/imaginary value change per 1 px
         */
        double scale;

        /**
         * Fits the given real and imaginary intervals into the current viewport
         *
         * @param minR
         * @param minI
         * @param maxR
         * @param maxI
         */
        public Position(double minR, double minI, double maxR, double maxI) {
            real = (minR + maxR) / 2;
            img = (minI + maxI) / 2;
            double scaleR = (maxR - minR) / winWidth;
            double scaleI = (maxI - minI) / winHeight;
            scale = Math.max(scaleR, scaleI);
        }

        public Position(double real, double img, double scale) {
            this.real = real;
            this.img = img;
            this.scale = scale;
        }

        public Position(Position pos) {
            this.real = pos.real;
            this.img = pos.img;
            this.scale = pos.scale;
        }

        public Position copyOf(Position pos) {
            this.real = pos.real;
            this.img = pos.img;
            this.scale = pos.scale;
            return this;
        }

        /**
         * @return real value corresponding to the left side of the viewport
         */
        private double getMinReal() {
            return real - scale * winWidth / 2;
        }

        /**
         * @return real value corresponding to the right side of the viewport
         */
        private double getMaxReal() {
            return real + scale * winWidth / 2;
        }

        /**
         * @return imaginary value corresponding to the top side of the viewport
         */
        private double getMinImg() {
            return img - scale * winHeight / 2;
        }

        /**
         * @return imaginary value corresponding to the bottom side of the
         * viewport
         */
        private double getMaxImg() {
            return img + scale * winHeight / 2;
        }

        @Override
        public String toString() {
            return "Position{" + "real=" + real + ", img=" + img + ", scale=" + scale + '}';
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 59 * hash + (int) (Double.doubleToLongBits(this.real) ^ (Double.doubleToLongBits(this.real) >>> 32));
            hash = 59 * hash + (int) (Double.doubleToLongBits(this.img) ^ (Double.doubleToLongBits(this.img) >>> 32));
            hash = 59 * hash + (int) (Double.doubleToLongBits(this.scale) ^ (Double.doubleToLongBits(this.scale) >>> 32));
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Position other = (Position) obj;
            if (Double.doubleToLongBits(this.real) != Double.doubleToLongBits(other.real)) {
                return false;
            }
            if (Double.doubleToLongBits(this.img) != Double.doubleToLongBits(other.img)) {
                return false;
            }
            if (Double.doubleToLongBits(this.scale) != Double.doubleToLongBits(other.scale)) {
                return false;
            }
            return true;
        }
    }

    /**
     * Binding for long value in milliseconds to nice textual format
     */
    private class TimeToStringBinding extends StringBinding {

        private final LongProperty timeValue;

        public TimeToStringBinding(LongProperty timeValue) {
            this.timeValue = timeValue;
            bind(timeValue);
        }

        @Override
        protected String computeValue() {
            long time = timeValue.get();
            return String.format(Locale.US, "%01d:%05.2f", time / 1000 / 60, (time % 60_000) / 1000d);
        }
    }

    private class FlyingAnimation extends AnimationTimer {

        private final Position from;
        private final Position middle;
        private final Position to;
        private Position nextTarget;
        private final Position snapshotPos = new Position(0, 0, 1);
        private final double w;
        private final double v;
        private final double u;
        private final double p1x;
        private final double p2x;
        private double real, img;

        private long prev;
        private int phase = 0;
        private double s;
        private final double DS = 3;
        private final double RATE = 5;
        private double speed = DS;
        private boolean running = false;

        public FlyingAnimation(Position from, Position to) {
            this.from = from;
            this.to = to;

            middle = new Position(
                    Math.min(from.getMinReal(), to.getMinReal()),
                    Math.min(from.getMinImg(), to.getMinImg()),
                    Math.max(from.getMaxReal(), to.getMaxReal()),
                    Math.max(from.getMaxImg(), to.getMaxImg()));

            // Bezier curve control points
            // double p0x = 0;
            double p0y = from.scale;
            p2x = 1;
            double p2y = to.scale;
            double py = middle.scale;
            double p1y = py + sqrt((p0y - py) * (p2y - py));
            p1x = (p0y - p1y) / (p2y - 2 * p1y + p0y);
            // double p1x = 0.5;

            // Parameters to solve bezier curve equation for y -> t
            double a = p0y;
            double b = p1y;
            double c = p2y;

            u = a - 2 * b + c;
            v = b * b - a * c;
            w = a - b;
        }

        private void prepareNextFrame() {
            stopTask();

            snapshotPos.copyOf(position);

            canvas.setOpacity(1);
            canvas.setTranslateX(0);
            canvas.setTranslateY(0);
            canvas.setScaleX(1);
            canvas.setScaleY(1);
            canvas.snapshot(snapshotParameters, wiSnapshot);
            canvas.getGraphicsContext2D().clearRect(0, 0, winWidth, winHeight);

            int canvasIndex = rootPane.getChildren().indexOf(canvas);
            boolean putSnaphotBehindCanvas;
            if (phase == 0) {
                position.scale = Math.min(s * RATE, nextTarget.scale);
                putSnaphotBehindCanvas = false;
            } else {
                position.scale = Math.max(s / RATE, nextTarget.scale * RATE / 2);
                putSnaphotBehindCanvas = true;
            }
            int snapshotIndex = rootPane.getChildren().indexOf(ivCanvasSnapshot);
            if (snapshotIndex != (putSnaphotBehindCanvas ? canvasIndex - 1 : canvasIndex + 1)) {
                rootPane.getChildren().remove(ivCanvasSnapshot);
                canvasIndex = rootPane.getChildren().indexOf(canvas);
            }
            if (!rootPane.getChildren().contains(ivCanvasSnapshot)) {
                rootPane.getChildren().add(putSnaphotBehindCanvas ? canvasIndex : canvasIndex + 1, ivCanvasSnapshot);
            }

            solveXfromY(position.scale);
            position.real = real;
            position.img = img;
            renderFast(() -> {
                canvas.setOpacity(1);
            });
        }

        @Override
        public void start() {
            running = true;
            disable.set(true);
            stopTask();
            rootPane.getChildren().add(0, ivGlobalSnapshot);
            ivCanvasSnapshot.setImage(wiSnapshot);

            if (from.scale != middle.scale) {
                phase = 0;
                nextTarget = middle;
            } else {
                phase = 1;
                nextTarget = to;
            }
            s = from.scale;

            prepareNextFrame();

            prev = System.nanoTime();
            speed = DS;
            super.start();
        }

        @Override
        public void handle(long l) {
            if (!running) {
                return;
            }
            double dt = (l - prev) / 1e9d; // in ms
            prev = l;

            switch (phase) {
                case 0:
                    double val = log(s) - log(from.scale);
                    if (val < 0.1) {
                        val *= 10;
                        speed = DS * val + 0.01 * (1 - val);
                    }
                    s *= 1 + speed * dt;
                    if (s >= middle.scale) {
                        s = middle.scale;
                        phase++;
                        nextTarget = to;
                        speed = DS;
                    } else if (s > position.scale / 2 && s < nextTarget.scale / 2) {
                        prepareNextFrame();
                    }

                    break;
                case 1:
                    val = log(s) - log(to.scale);
                    if (val < 1) {
                        speed = DS * val + 0.01 * (1 - val);
                    }
                    s /= 1 + speed * dt;
                    if (s <= to.scale) {
                        s = to.scale;
                        phase++;
                    } else if (s < position.scale / RATE) {
                        prepareNextFrame();
                    }
                    break;
                case 2:
                    stop();
                    finish();
                    return;
            }

            solveXfromY(s);

            canvas.setTranslateX((position.real - real) / s);
            canvas.setTranslateY((position.img - img) / s);
            canvas.setScaleX(position.scale / s);
            canvas.setScaleY(position.scale / s);
            ivGlobalSnapshot.setTranslateX((global.real - real) / s);
            ivGlobalSnapshot.setTranslateY((global.img - img) / s);
            ivGlobalSnapshot.setScaleX(global.scale / s);
            ivGlobalSnapshot.setScaleY(global.scale / s);
            ivCanvasSnapshot.setTranslateX((snapshotPos.real - real) / s);
            ivCanvasSnapshot.setTranslateY((snapshotPos.img - img) / s);
            ivCanvasSnapshot.setScaleX(snapshotPos.scale / s);
            ivCanvasSnapshot.setScaleY(snapshotPos.scale / s);

        }

        private void solveXfromY(double y) {
            // Solving for t
            double t1 = (w + sqrt(v + y * u)) / u;
            double t2 = (w - sqrt(v + y * u)) / u;
            double t = phase == 0 ? t1 : t2;
            double x = 2 * t * (1 - t) * p1x + t * t * p2x;

            // animated position
            real = from.real * (1 - x) + to.real * x;
            img = from.img * (1 - x) + to.img * x;
        }

        public void abort() {
            if (running) {
                stopTask();
                stop();
                position = to;
                disable.set(false);
                rootPane.getChildren().removeAll(ivGlobalSnapshot, ivCanvasSnapshot);
                canvas.setOpacity(1);
                canvas.setTranslateX(0);
                canvas.setTranslateY(0);
                canvas.setScaleX(1);
                canvas.setScaleY(1);
                reset();
            }
        }

        @Override
        public void stop() {
            super.stop();
        }

        private void reset() {
            running = false;
            flyingAnimation = null;
            disable.set(false);
        }

        private void finish() {
            stopTask();
            ivCanvasSnapshot.setTranslateX(canvas.getTranslateX());
            ivCanvasSnapshot.setTranslateY(canvas.getTranslateY());
            ivCanvasSnapshot.setScaleX(canvas.getScaleX());
            ivCanvasSnapshot.setScaleY(canvas.getScaleY());
            canvas.setTranslateX(0);
            canvas.setTranslateY(0);
            canvas.setScaleX(1);
            canvas.setScaleY(1);
            canvas.snapshot(snapshotParameters, wiSnapshot);
            canvas.getGraphicsContext2D().clearRect(0, 0, winWidth, winHeight);
            position = to;
            render(() -> {
                rootPane.getChildren().remove(ivCanvasSnapshot);
                reset();
            });
            rootPane.getChildren().remove(ivGlobalSnapshot);
        }
    }
}
