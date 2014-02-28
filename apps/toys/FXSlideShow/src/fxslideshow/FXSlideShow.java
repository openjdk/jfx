/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
package fxslideshow;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Transition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

public class FXSlideShow extends Application {

    // arguments/switches
    static boolean debug = false;
    static boolean isEmbedded =
            (Boolean.parseBoolean(System.getProperty("com.sun.javafx.isEmbedded", "false")));
    static boolean preScale = true;
    static boolean bigSize = false;
    static boolean randomize = false;
    static boolean loop = false;
    static boolean savePrevious = true;
    static int waitTimeMillis = 0;
    // internal variables
    static int stageWidth = 800;
    static int stageHeight = 800;
    static int scaleWidth;
    static int scaleHeight;
    static Files imageFiles = new Files();
    String curentImageName = null;
    static Image brokenImage;
    long lastImageTime = 0;
    Stage stage;
    private final static Logger LOGGER = Logger.getLogger(FXSlideShow.class.getName());
    ProgressBar busyIndicator = new ProgressBar();

    private class ImageLoadingTask extends Task<Image> {

        String imagePath;
        boolean scale;

        public ImageLoadingTask(String path) {
            imagePath = path;
            scale = true;
        }

        public ImageLoadingTask(String path, boolean scale) {
            imagePath = path;
            this.scale = scale;
        }

        public String getPath() {
            return imagePath;
        }

        @Override
        protected Image call() throws Exception {
            LOGGER.finest("Starting to load our Image " + imagePath );
            long start = System.currentTimeMillis();
            Image image;
            if (preScale && scale) {
                image = new Image(imagePath,
                        scaleWidth, scaleHeight,
                        true, true);
            } else {
                image = new Image(imagePath);
            }
            if (image != null && image.isError()) {
                image = null; 
            }
            long elapsed = System.currentTimeMillis() - start;
            LOGGER.fine("Image load time="+elapsed+"ms " + imagePath);
            return image;
        }

        protected void scheduled() {
            LOGGER.finer("Scheduled " + imagePath);
        }

        @Override
        protected void succeeded() {
            //note: we are on User Event Thread
            super.succeeded();
            try {
                final Image img = get();
                LOGGER.finest("applyImage this=" + img);
                if (image[CURRENT].equals(this)) {
                    if (busyIndicator.isVisible()) {
                        busyIndicator.setVisible(false);
                    }
                    setCurrentImage(image[CURRENT]);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void cancelled() {
            //note: we are on User Event Thread
            super.cancelled();
            LOGGER.finer("Cancelled" + imagePath);
        }

        @Override
        protected void failed() {
            //note: we are on User Event Thread
            super.failed();
            LOGGER.finer("FAILED " + imagePath);
        }

        public void load() {
            if (this.isRunning()) {
                // more needed here?
                this.cancel();
            }
            executor.execute(this);

        }
    }

    static ExecutorService executor = Executors.newFixedThreadPool(1,
            r -> {
                Thread t = new Thread(r);

                // Run at low priority to give User Event thread priority
                t.setPriority(Thread.MIN_PRIORITY);

                // Set this worker to daemon, so our app will exit on close.
                t.setDaemon(true);
                return t;
            });
    static final ImageLoadingTask image[] = new ImageLoadingTask[3];
    static final int PREV = 2;
    static final int CURRENT = 0;
    static final int NEXT = 1;
    final ImageView view[] = new ImageView[2];
    final Transition transition[] = new Transition[2];

    void showHelp() {
        System.out.println("FXSlideShow [arguments] files/directories");
        System.out.println("Common Arguments");
        System.out.println("  -loop     loop through images");
        System.out.println("  --wait=X  wait X seconds before auto advancing");
        System.out.println("  -random   show images in random order");
        System.out.println("");
        System.out.println(" use left/right arrows to manually advance");
        System.exit(1);
    }

    //-------------------------------------
    private void setArgument(String name, String value) {
        switch (name) {
            case "debug":
                debug = true;
                if (value.equals("true")) {
                    value = "FINER";
                }
                // set our application logger level to the provided level
                Level level = Level.parse(value.toUpperCase());
                Logger.getLogger("fxslideshow").setLevel(level);

                // "workaround" for ConsoleLogger limit to INFO
                for (Handler h : Logger.getLogger("").getHandlers()) {
                    h.setLevel(Level.ALL);
                }
                break;
            case "loop":
                loop = Boolean.parseBoolean(value);
                break;
            case "prescale":
                preScale = Boolean.parseBoolean(value);
                break;
            case "random":
                randomize = Boolean.parseBoolean(value);
                break;
            case "savePrevious":
                savePrevious = Boolean.parseBoolean(value);
                break;
            case "full":
                bigSize = Boolean.parseBoolean(value);
                break;
            case "help":
                showHelp();
            case "wait":
                if (value.equals("true")) {
                    value = "10";
                }
                waitTimeMillis = Integer.parseInt(value);
                LOGGER.finer("Setting wait time to " + waitTimeMillis);
                waitTimeMillis *= 1000;
                break;
            default:
                System.out.println("Unknown arg: " + name);
                showHelp();
                break;
        }

    }

    private void parseArguments() {
        Application.Parameters params = getParameters();

        Map<String, String> named = params.getNamed();
        Set<String> nkeys = named.keySet();
        for (String arg : nkeys) {
            setArgument(arg, named.get(arg));
        }

        for (String arg : params.getUnnamed()) {
            if (arg.startsWith("-")) {
                setArgument(arg.substring(1), "true");
            } else {
                imageFiles.add(arg);
            }
        }

        if (debug) {
            imageFiles.debugList();
        }
    }

    private void slideRightTransition(final ImageView in, final ImageView out, boolean right) {

        in.setOpacity(1.0);
        out.setOpacity(1.0);

        double shift = stageWidth;
        if (!right) {
            shift *= -1;
        }

        final TranslateTransition slideIn = new TranslateTransition(Duration.millis(3000), in);
        slideIn.setFromX(-shift);
        slideIn.setToX(0.0);
        transition[CURRENT] = slideIn;
        slideIn.setOnFinished(t -> out.setTranslateX(0.0));
        slideIn.play();

        final TranslateTransition slideOut = new TranslateTransition(Duration.millis(3000), out);
        slideOut.setFromX(0.0);
        slideOut.setToX(shift);
        transition[NEXT] = slideOut;
        slideOut.setOnFinished(t -> {
            out.setVisible(false);
            out.setTranslateX(0.0);
        });
        slideOut.play();
    }

    private void slideDownTransition(final ImageView in, final ImageView out, boolean down) {

        in.setOpacity(1.0);
        out.setOpacity(1.0);

        double shift = stageHeight;
        if (!down) {
            shift *= -1;
        }

        final TranslateTransition slideIn = new TranslateTransition(Duration.millis(3000), in);
        slideIn.setFromY(-shift);
        slideIn.setToY(0.0);
        transition[CURRENT] = slideIn;
        slideIn.play();

        final TranslateTransition slideOut = new TranslateTransition(Duration.millis(3000), out);
        slideOut.setFromY(0.0);
        slideOut.setToY(shift);
        transition[NEXT] = slideOut;
        slideOut.setOnFinished(t -> {
            out.setVisible(false);
            out.setTranslateY(0.0);
        });
        slideOut.play();
    }

    private void fadeTransition(final ImageView in, final ImageView out) {
        final FadeTransition ftIn = new FadeTransition(Duration.millis(3000), in);
        ftIn.setFromValue(0.0);
        ftIn.setToValue(1.0);
        ftIn.setCycleCount(0);
        transition[CURRENT] = ftIn;
        ftIn.setOnFinished(t -> out.setOpacity(1.0));
        ftIn.play();


        // first change - this will not be needed.
        if (out.getOpacity() != 0.0) {
            final FadeTransition ftOut = new FadeTransition(Duration.millis(3000), out);
            ftOut.setFromValue(1.0);
            ftOut.setToValue(0.0);
            ftOut.setCycleCount(0);
            transition[NEXT] = ftOut;
            ftOut.setOnFinished(t -> {
                out.setVisible(false);
                out.setOpacity(1.0);
            });
            ftOut.play();
        }
    }
    private int nextView = 1; // used to toggle between active image views
    private int trans = 0;

    private void setCurrentImage(ImageLoadingTask i) {
        int fadeOut = nextView;
        int fadeIn = (nextView == 1) ? 0 : 1;

        Image image;
        try {
            image = i.get();
        } catch (InterruptedException ex) {
            Logger.getLogger(FXSlideShow.class.getName()).log(Level.SEVERE, null, ex);
            image = null;
        } catch (ExecutionException ex) {
            Logger.getLogger(FXSlideShow.class.getName()).log(Level.SEVERE, null, ex);
            image = null;
        }

        if (image == null) {
            image = brokenImage;
        }

        // clean up any running transition
        for (int j = 0; j < 2; j++) {
            if (transition[j] != null
                    && transition[j].getStatus() == Animation.Status.RUNNING) {
                transition[j].stop();
            }
        }

        // reset the views to a known state.
        view[fadeIn].setTranslateX(0.0);
        view[fadeIn].setTranslateY(0.0);
        view[fadeIn].setOpacity(1.0);
        view[fadeOut].setTranslateX(0.0);
        view[fadeOut].setTranslateY(0.0);
        view[fadeOut].setOpacity(1.0);

        //prep the incoming view
        view[fadeIn].setImage(image);
        lastImageTime = System.currentTimeMillis();
        view[fadeIn].setVisible(true);

        trans = (trans + 1) % 5;
        switch (trans) {
            case 0:
                fadeTransition(view[fadeIn], view[fadeOut]);
                break;
            case 1:
                slideRightTransition(view[fadeIn], view[fadeOut], true);
                break;
            case 2:
                slideRightTransition(view[fadeIn], view[fadeOut], false);
                break;
            case 3:
                slideDownTransition(view[fadeIn], view[fadeOut], true);
                break;
            case 4:
                slideDownTransition(view[fadeIn], view[fadeOut], false);
                break;
            default:
                System.out.println("BAD trans");

        }

        if (busyIndicator.isVisible()) {
            busyIndicator.setVisible(false);
        }

        nextView = fadeIn;
    }

    private void nextImage(boolean forward) {

        synchronized (image) {
            int nextImage;
            int prevImage;
            String nextPath;

            if (forward) {
                if (!imageFiles.next()) {
                    LOGGER.finest("out of images");
                    return;
                }
                nextImage = NEXT;
                prevImage = PREV;
                nextPath = imageFiles.nextFilePath();
            } else {
                if (!imageFiles.previous()) {
                    LOGGER.finest("out of images");
                    return;
                }
                nextImage = PREV;
                prevImage = NEXT;
                nextPath = imageFiles.prevFilePath();
            }

            if (image[prevImage] != null) {
                if (image[prevImage].isRunning()) {
                    image[prevImage].cancel();
                }
                image[prevImage] = null;
            }
            if (savePrevious) {
                image[prevImage] = image[CURRENT];
            } else {
                if (image[CURRENT].isRunning()) {
                    image[CURRENT].cancel();
                }
            }

            image[CURRENT] = image[nextImage];
            image[nextImage] = null;

            if (image[CURRENT] == null) {
                image[CURRENT] = new ImageLoadingTask("file:" + imageFiles.currentFilePath());
                executor.execute(image[CURRENT]);
                busyIndicator.setVisible(true);
                lastImageTime = 0; // will be reset when loading is complete
            } else if (image[CURRENT].isDone()) {
                setCurrentImage(image[CURRENT]);
            } else {
                busyIndicator.setVisible(true);
                lastImageTime = 0; // will be reset when loading is complete
            }


            if (nextPath != null) {
                image[nextImage] = new ImageLoadingTask("file:" + nextPath);
                executor.execute(image[nextImage]);
            }
        }
    }
    //-------------------------------------
    final EventHandler<KeyEvent> keyEventHandler = keyEvent -> {
        switch (keyEvent.getCode()) {
            case SPACE:
            case RIGHT:
                nextImage(true);
                break;
            case LEFT:
                nextImage(false);
                break;
            //Reload/scale the image
            case R:
                if (image[CURRENT] != null) {
                    image[CURRENT].load();
                }
                break;
            case Q:
                Platform.exit();
            default:
            // do nothing
        }
    };

    //-------------------------------------
    private void sceneBoundsChanged(double width, double height) {
        if (width == 0.0 && height == 0.0) {
            // ignore the spurious initial events
            return;
        }

        stageWidth = (int) width;
        stageHeight = (int) height;
        scaleWidth = (int) width;
        scaleHeight = (int) height;
        LOGGER.fine("Scene now " + stageWidth + "x" + stageHeight);
    }

    /**
     * initiate a polling thread to manage the "auto-forward" capability.
     */
    private void startAutoForward() {
        if (waitTimeMillis > 0) {
            Thread ft = new Thread(() -> {
                LOGGER.finer("Starting Autoforward thread");
                while (true) {
                    long curr = System.currentTimeMillis();
                    long push = lastImageTime + waitTimeMillis;
                    long pause = push - curr;
                    if (lastImageTime > 0) {
                        if (pause < 0) {
                            Platform.runLater(() -> {
                                LOGGER.finer("auto forwarding");
                                nextImage(true);
                            });
                            pause = waitTimeMillis;
                        }
                    } else {
                        pause = waitTimeMillis;
                    }
                    try {
                        Thread.sleep(pause);
                    } catch (Exception e) {
                    }
                }
            });
            ft.setDaemon(true); // So FX app can exit on close
            ft.start();
        }
    }

    @Override
    public void start(Stage stage) {

        this.stage = stage;

        if (isEmbedded) {
            preScale = true;
            bigSize = true;
        }

        parseArguments();

        if (bigSize) {
            Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
            stageWidth = (int) primaryScreenBounds.getWidth();
            stageHeight = (int) primaryScreenBounds.getHeight();
            LOGGER.finer("isEmbedded - using full screen size " + stageWidth + "x" + stageHeight);
        }

        if (imageFiles.count() == 0) {
            LOGGER.warning("Error: no files specified/found");
            showHelp();
        }

        if (randomize) {
            LOGGER.finer("randomizing files");
            imageFiles.randomize();
        }

        imageFiles.setLoop(loop);

        image[PREV] = null;
        image[CURRENT] = null;
        image[NEXT] = null;

        StackPane stack = new StackPane();

        final Scene scene = new Scene(stack);

        scene.setOnKeyReleased(keyEventHandler);

        // set up our image views, and 
        // bindings to layout our ImageView properly.
        for (int i = 0; i < 2; i++) {
            view[i] = new ImageView();
            view[i].setPreserveRatio(true);
            view[i].setSmooth(true);
            view[i].setX(0);
            view[i].setY(0);
            view[i].setVisible(false);
            if (!preScale) {
                view[i].fitWidthProperty().bind(scene.widthProperty());
                view[i].fitHeightProperty().bind(scene.heightProperty());
            }
        }

        Rectangle backdrop = new Rectangle();
        backdrop.setFill(Color.BLACK);
        backdrop.widthProperty().bind(scene.widthProperty());
        backdrop.heightProperty().bind(scene.heightProperty());

        stage.widthProperty().addListener((ov, t, newval) -> sceneBoundsChanged(scene.getWidth(), scene.getHeight()));
        stage.heightProperty().addListener((ov, t, newval) -> sceneBoundsChanged(scene.getWidth(), scene.getHeight()));

        stack.getChildren().addAll(
                backdrop,
                view[0],
                view[1],
                busyIndicator);

        final int initialWidth = stageWidth;
        final int initialHeight = stageHeight;

        scaleWidth = initialWidth;
        scaleHeight = initialHeight;

        stage.setTitle("Slide Show");
        stage.setScene(scene);

        LOGGER.fine("Starting size is " + initialWidth + "x" + initialHeight);
        stage.setWidth(initialWidth);
        stage.setHeight(initialHeight);
        stage.setResizable(true);
        stage.show();

        final ImageLoadingTask bi = new ImageLoadingTask("fxslideshow/broken.png", false) {
            @Override
            protected void succeeded() {
                try {
                    brokenImage = get();
                } catch (Exception ex) {
                    LOGGER.fine("Failed to load broken image icon");
                }
            }
        };
        bi.load();

        // Now queue up our first images.
        image[CURRENT] = new ImageLoadingTask("file:" + imageFiles.currentFilePath());
        image[CURRENT].load();

        final String nf = imageFiles.nextFilePath();
        if (nf != null) {
            image[NEXT] = new ImageLoadingTask("file:" + nf);
            image[NEXT].load();
        }

        startAutoForward();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
