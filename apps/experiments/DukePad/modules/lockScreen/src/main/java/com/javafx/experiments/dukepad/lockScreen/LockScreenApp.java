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

package com.javafx.experiments.dukepad.lockScreen;

import com.javafx.experiments.dukepad.core.DukeApplication;
import com.javafx.experiments.dukepad.core.Fonts;
import com.javafx.experiments.dukepad.core.LockScreen;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.geometry.VPos;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.util.Duration;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import static com.javafx.experiments.dukepad.core.DateTimeHelper.*;
import static com.javafx.experiments.dukepad.core.Palette.RED_ORANGE;

/**
 * LockScreen
 */
public class LockScreenApp extends LockScreen implements BundleActivator , ChangeListener<Number> {
    private static final Image appIcon = new Image(LockScreenApp.class.getResource("/images/ico-lock.png").toExternalForm());
    private Text date;
    private Text hour;
    private Text min;
    private Text seconds;
    private Circle centerCircle;
    private ImageView innerCircle;
    private ImageView outerCircle;
    private ImageView ticks;
    private Rotate secondRotate = new Rotate(0);
    private Rotate secondTextRotate = new Rotate(0,0,0);
    private Translate secondTranslate = new Translate();
    private Region sideBar = new Region();
    private Timeline hideShowTimeline;
    private Timeline sideBarTimeline;
    private SimpleBooleanProperty locked = new SimpleBooleanProperty(true);
    private Line testLine1 = new Line();
    private Line testLine2 = new Line();
    private final Pane root = new Pane() {
        @Override protected void layoutChildren() {
            final double cx = getWidth()/2;
            final double cy = getHeight()/2;
            centerCircle.setLayoutX(cx);
            centerCircle.setLayoutY(cy);
            innerCircle.setLayoutX(cx - (innerCircle.getLayoutBounds().getWidth() / 2));
            innerCircle.setLayoutY(cy - (innerCircle.getLayoutBounds().getHeight() / 2));
            outerCircle.setLayoutX(cx - (outerCircle.getLayoutBounds().getWidth() / 2));
            outerCircle.setLayoutY(cy - (outerCircle.getLayoutBounds().getHeight() / 2));
            ticks.setLayoutX(cx - (ticks.getLayoutBounds().getWidth() / 2));
            ticks.setLayoutY(cy - (ticks.getLayoutBounds().getHeight() / 2));
            date.setLayoutX(cx + 192 + 15);
            date.setLayoutY(cy);
            hour.setLayoutX(cx + 96 - (hour.getLayoutBounds().getWidth() / 2));
            hour.setLayoutY(cy);
            min.setLayoutX(cx + 162 - (min.getLayoutBounds().getWidth() / 2));
            min.setLayoutY(cy);
            secondTranslate.setX(cx);
            secondTranslate.setY(cy);
        }
    };

    // =================================================================================================================
    // BundleActivator Methods

    @Override public void start(BundleContext bundleContext) throws Exception {
        // Build UI
//        backgroundImg = new Image(LockScreen.class.getResource("/images/lock-screen-background.jpg").toExternalForm());
        Image backgroundImg = new Image(LockScreenApp.class.getResource("/desktop.jpg").toExternalForm());
        ImageView background = new ImageView(backgroundImg);

        date = new Text("January 16th 2013");
        date.setId("date");
        date.setFont(Fonts.dosisExtraLight(50));
        date.setFill(Color.WHITE);
        date.setTextOrigin(VPos.CENTER);
        date.setCache(true);
        date.setCacheHint(CacheHint.SPEED);

        seconds = new Text("30");
        seconds.setId("seconds");
        seconds.setFont(Fonts.dosisExtraLight(30));
        seconds.setFill(Color.WHITE);
        seconds.setTextOrigin(VPos.CENTER);
        seconds.getTransforms().addAll(secondTranslate,secondRotate,secondTextRotate);
        seconds.setX(192 + 30);
        seconds.setCache(true);
        seconds.setCacheHint(CacheHint.SPEED);

        hour = new Text("1 1");
        hour.setId("hour");
        hour.setFont(Fonts.dosisExtraLight(50));
        hour.setFill(Color.GREEN);
        hour.setTextOrigin(VPos.CENTER);
        hour.setCache(true);
        hour.setCacheHint(CacheHint.SPEED);

        min = new Text("59");
        min.setId("minute");
        min.setFont(Fonts.dosisExtraLight(50));
        min.setFill(Color.GREEN);
        min.setTextOrigin(VPos.CENTER);
        min.setCache(true);
        min.setCacheHint(CacheHint.SPEED);

        ImageView timeBackground = new ImageView(backgroundImg);
        timeBackground.setClip(new Group(hour,min));

        centerCircle = new Circle(49,RED_ORANGE);
        innerCircle = new ImageView(new Image(LockScreenApp.class.getResource("/images/Inner.png").toExternalForm()));
        outerCircle = new ImageView(new Image(LockScreenApp.class.getResource("/images/Outer.png").toExternalForm()));
        ticks = new ImageView(new Image(LockScreenApp.class.getResource("/images/Ticks.png").toExternalForm()));

        sideBar.setOpacity(0);
        sideBar.setBackground(new Background(new BackgroundFill(RED_ORANGE, CornerRadii.EMPTY, Insets.EMPTY)));

        testLine1.setStroke(Color.CYAN);
        testLine2.setStroke(Color.GREEN);
        root.getChildren().addAll(background,centerCircle,innerCircle,outerCircle, ticks,date,seconds, timeBackground,sideBar);//, testLine1,testLine2);

        hideShowTimeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(date.opacityProperty(),1),
                        new KeyValue(date.visibleProperty(),true),
                        new KeyValue(innerCircle.opacityProperty(),1),
                        new KeyValue(outerCircle.opacityProperty(),1),
                        new KeyValue(ticks.opacityProperty(),1),
                        new KeyValue(seconds.opacityProperty(),1),
                        new KeyValue(sideBar.opacityProperty(),0)
                ),
                new KeyFrame(Duration.millis(50),
                        new KeyValue(sideBar.opacityProperty(),1)
                ),
                new KeyFrame(Duration.millis(400),
                        new KeyValue(date.opacityProperty(),0.1),
                        new KeyValue(date.visibleProperty(),false),
                        new KeyValue(innerCircle.opacityProperty(),0.1),
                        new KeyValue(outerCircle.opacityProperty(),0.1),
                        new KeyValue(ticks.opacityProperty(),0.1),
                        new KeyValue(seconds.opacityProperty(),0.1)
                )
        );
        sideBarTimeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(sideBar.scaleXProperty(),1),
                        new KeyValue(sideBar.scaleYProperty(),1)
                ),
                new KeyFrame(Duration.millis(200),
                        new KeyValue(sideBar.scaleXProperty(),5, Interpolator.EASE_BOTH),
                        new KeyValue(sideBar.scaleYProperty(),5, Interpolator.EASE_BOTH)
                )
        );
        sideBarTimeline.setCycleCount(Timeline.INDEFINITE);

        root.setOnMousePressed(this::press);
        root.setOnMouseReleased(this::release);
        root.setOnMouseDragged(this::drag);

        // Register application service
        bundleContext.registerService(DukeApplication.class, this, null);
    }

    @Override public void stop(BundleContext bundleContext) throws Exception {}

    // =================================================================================================================
    // BundleActivator Methods

    @Override public void setLocked(boolean locked) {
        this.locked.set(locked);
    }

    @Override public boolean isLocked() {
        return locked.get();
    }

    public SimpleBooleanProperty lockedProperty() {
        return locked;
    }

    // =================================================================================================================
    // Application Methods

    @Override public void startApp() {
        super.startApp();
        centerCircle.setTranslateX(0);
        centerCircle.setTranslateY(0);
        setLocked(true);

        SECONDS.addListener(this);
        date.textProperty().bind(LONG_DATE);
        hour.textProperty().bind(new StringBinding() {
            { bind(HOUR); }
            @Override protected String computeValue() {
                return String.format("%02d", HOUR.get());
            }
        });
        min.textProperty().bind(new StringBinding() {
            { bind(MINUTE); }
            @Override protected String computeValue() {
                return String.format("%02d", MINUTE.get());
            }
        });
    }

    @Override public void stopApp() {
        super.stopApp();

        SECONDS.removeListener(this);
        date.textProperty().unbind();
        hour.textProperty().unbind();
        min.textProperty().unbind();
        sideBarTimeline.stop();
    }

    @Override protected Node createUI() {
        return root;
    }

    @Override public String getName() {
        return "Lock";
    }

    @Override public Node createHomeIcon() {
        return new ImageView(appIcon);
    }

    // =================================================================================================================
    // Second Listener Method

    /** Called every second while the application is running */
    @Override public void changed(ObservableValue<? extends Number> observableValue, Number lastSeconds, Number seconds) {
        //To change body of implemented methods use File | Settings | File Templates.
        int secondsVal = seconds.intValue();
        int hoursVal = HOUR.get();
        int minsVal = MINUTE.get();
        outerCircle.setRotate(6 * minsVal + (0.1*secondsVal));
        innerCircle.setRotate((30 * hoursVal) + (0.5 * minsVal) + (0.008333333333*secondsVal));
        this.seconds.setText(String.format("%02d",secondsVal));
        new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(secondRotate.angleProperty(),(6*(secondsVal-1))-90),
                        new KeyValue(ticks.rotateProperty(),(6*(secondsVal-1)))
                ),
                new KeyFrame(Duration.seconds(1),
                        new KeyValue(secondRotate.angleProperty(),(6*secondsVal)-90, Interpolator.LINEAR),
                        new KeyValue(ticks.rotateProperty(),(6*secondsVal), Interpolator.LINEAR)
                )
        ).play();
        secondTextRotate.setPivotX(192 + 30 + (this.seconds.getLayoutBounds().getWidth() / 2));
        if (secondsVal > 0 && secondsVal < 30) {
            secondTextRotate.setAngle(0);
        } else {
            secondTextRotate.setAngle(180);
        }
        switch (secondsVal) {
            case 0:
                new Timeline(
                        new KeyFrame(Duration.ZERO, new KeyValue(date.translateXProperty(),0)),
                        new KeyFrame(Duration.millis(15000), new KeyValue(date.translateXProperty(),60, Interpolator.EASE_BOTH))
                ).play();
                break;
            case 15:
                new Timeline(
                        new KeyFrame(Duration.ZERO, new KeyValue(date.translateXProperty(),60)),
                        new KeyFrame(Duration.millis(15000), new KeyValue(date.translateXProperty(),00, Interpolator.EASE_BOTH))
                ).play();
                break;
        }
    }

    // =================================================================================================================
    // Private Methods

    private double startX, startY;
    private double lastX, lastY;
    private double lastLastX, lastLastY;
    private long lastTime, lastLastTime;
    private Side side;

    private void drag(MouseEvent event) {
        final double x = event.getSceneX();
        final double y = event.getSceneY();
        lastLastX = lastX;
        lastLastY = lastY;
        lastLastTime = lastTime;
        lastX = x;
        lastY = y;
        lastTime = System.currentTimeMillis();
        double traveledX = x-startX;
        double traveledY = y-startY;
//        System.out.println("traveledY = " + traveledY);
        double distance = Math.sqrt((traveledX*traveledX)+(traveledY*traveledY));
//        double angle = Math.atan2(traveledX, traveledY);
        double angle = Math.PI-Math.atan2(traveledX, traveledY);
        double angleDeg = Math.toDegrees(angle);
//        System.out.println("distance = " + distance+"        angle = " + angleDeg);

        testLine1.setStartX(startX);
        testLine1.setStartY(startY);
        testLine1.setEndX(x);
        testLine1.setEndY(y);

        testLine2.setStartX(x);
        testLine2.setStartY(y);

        boolean allGood = false;
        switch (side) {
            case TOP:
                if (traveledY < 0) {
//                    System.out.println("y = " + y);
                    double opp = Math.tan(angle) * y;
//                    System.out.println("opp = " + opp);
                    double xHitPoint = x + opp;
                    testLine2.setEndX(xHitPoint);
                    testLine2.setEndY(0);
                }
                break;
            case BOTTOM:
                if (traveledY > 0) {
//                    System.out.println("y = " + y);
                    double opp = Math.tan(angle) * (root.getHeight()-y);
//                    System.out.println("opp = " + opp);
                    double xHitPoint = x + opp;
                    testLine2.setEndX(xHitPoint);
                    testLine2.setEndY(root.getHeight());
                }
                break;
            case LEFT:
                sideBar.resizeRelocate(0,0,5,root.getHeight());
                break;
            case RIGHT:
                sideBar.resizeRelocate(root.getWidth()-5,0,5,root.getHeight());
                break;
        }

        centerCircle.setTranslateX(x-centerCircle.getLayoutX());
        centerCircle.setTranslateY(y-centerCircle.getLayoutY());
        if (centerCircle.getBoundsInParent().intersects(sideBar.getBoundsInParent())) {
            sideBarTimeline.play();
        } else {
            sideBarTimeline.stop();
            sideBar.setScaleX(1);
            sideBar.setScaleY(1);
        }
    }

    private void press(MouseEvent event) {
        final double x = event.getSceneX();
        final double y = event.getSceneY();
        System.out.println("LockScreenApp.press(x = [" + x + "], y = [" + y + "])");
        startX = x;
        startY = y;
        SECONDS.removeListener(this);
        // pick a random side
//        side = Side.values()[(int)Math.round((Side.values().length-1)*Math.random())];
        side = Side.TOP;
        // move/resize rectangle to that side
        switch (side) {
            case TOP:
                sideBar.resizeRelocate(0,0,root.getWidth(),5);
                break;
            case BOTTOM:
                sideBar.resizeRelocate(0,root.getHeight()-5,root.getWidth(),5);
                break;
            case LEFT:
                sideBar.resizeRelocate(0,0,5,root.getHeight());
                break;
            case RIGHT:
                sideBar.resizeRelocate(root.getWidth()-5,0,5,root.getHeight());
                break;
        }
        hideShowTimeline.stop();
        hideShowTimeline.setRate(1);
        hideShowTimeline.play();

        if (centerCircle.getBoundsInParent().contains(x, y)) {
            centerCircle.setTranslateX(x-centerCircle.getLayoutX());
            centerCircle.setTranslateY(y-centerCircle.getLayoutY());
        } else {
            new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(centerCircle.translateXProperty(),centerCircle.getTranslateX()),
                            new KeyValue(centerCircle.translateYProperty(),centerCircle.getTranslateY())
                    ),
                    new KeyFrame(Duration.millis(200),
                            new KeyValue(centerCircle.translateXProperty(),x-centerCircle.getLayoutX(),Interpolator.EASE_OUT),
                            new KeyValue(centerCircle.translateYProperty(),y-centerCircle.getLayoutY(),Interpolator.EASE_OUT)
                    )
            ).play();
        }
    }

    private void release(MouseEvent event) {
        final double x = event.getSceneX();
        final double y = event.getSceneY();
        System.out.println("LockScreenApp.release(x = [" + x + "], y = [" + y + "])");
        double lastDraggedX = x - lastLastX;
        double lastDraggedY = y - lastLastY;
        double distanceOverTwoEvents = Math.sqrt((lastDraggedX*lastDraggedX)+(lastDraggedY*lastDraggedY));
        long timeOverTwoEvents = System.currentTimeMillis() - lastLastTime;
        double velocityOverTwoEvents = distanceOverTwoEvents / (double)timeOverTwoEvents;

        System.out.println("    distanceOverTwoEvents = " + distanceOverTwoEvents);
        System.out.println("    timeOverTwoEvents = " + timeOverTwoEvents);
        System.out.println("    velocityOverTwoEvents = " + velocityOverTwoEvents);

        double traveledX = x-startX;
        double traveledY = y-startY;
        System.out.println("    traveledY = " + traveledY);
        double distance = Math.sqrt((traveledX*traveledX)+(traveledY*traveledY));
        boolean intersectsBar = centerCircle.getBoundsInParent().intersects(sideBar.getBoundsInParent());
        double endX = 0, endY = 0;

        boolean allGood = false;

        if (intersectsBar) {
            allGood = true;
        } else if (distance > 15) {

            testLine1.setStartX(startX);
            testLine1.setStartY(startY);
            testLine1.setEndX(x);
            testLine1.setEndY(y);

            testLine2.setStartX(x);
            testLine2.setStartY(y);

            switch (side) {
                case TOP:
                    if (traveledY < 0) {
                        double angle = Math.PI-Math.atan2(traveledX, traveledY);
                        double angleDeg = Math.toDegrees(angle);
                        System.out.println("    distance = " + distance+"        angle = " + angleDeg);
                        double opp = Math.tan(angle) * y;
                        double xHitPoint = x + opp;
                        testLine2.setEndX(xHitPoint);
                        testLine2.setEndY(0);

                        endX = xHitPoint;
                        endY = 0;

                        if (xHitPoint > 0 && xHitPoint < root.getWidth() && velocityOverTwoEvents > .8) {
                            // ball will hit bar if keeps going at same angle
                            allGood = true;
                        }
                    }
                    break;
                case BOTTOM:
                    System.out.println("BOTTOM");
                    if (traveledY > 0) {
                        double angle =  Math.PI-Math.atan2(traveledX, traveledY);
                        double angleDeg = Math.toDegrees(angle);
                        System.out.println("    distance = " + distance+"        angle = " + angleDeg);
                        double opp = Math.tan(angle) * (root.getHeight()-y);
                        double xHitPoint = x + opp;
                        testLine2.setEndX(xHitPoint);
                        testLine2.setEndY(root.getHeight());

                        endX = xHitPoint;
                        endY = root.getHeight();

                        if (xHitPoint > 0 && xHitPoint < root.getWidth() && velocityOverTwoEvents > .8) {
                            // ball will hit bar if keeps going at same angle
                            allGood = true;
                        }
                    }
                    break;
                case LEFT:
                    sideBar.resizeRelocate(0,0,5,root.getHeight());
                    break;
                case RIGHT:
                    sideBar.resizeRelocate(root.getWidth()-5,0,5,root.getHeight());
                    break;
            }
        }

        if (allGood) {
            if (intersectsBar) {
                locked.set(false);
            } else {
                double endXDist = endX - x;
                double endYDist = endY - y;
                double distanceToEndXY = Math.sqrt((endXDist*endXDist)+(endYDist*endYDist));
                double timeAtCurrentVelocity = distanceToEndXY/velocityOverTwoEvents;
                System.out.println("    timeAtCurrentVelocity = " + timeAtCurrentVelocity);

                new Timeline(
                        new KeyFrame(Duration.ZERO,
                                new KeyValue(centerCircle.translateXProperty(),centerCircle.getTranslateX()),
                                new KeyValue(centerCircle.translateYProperty(),centerCircle.getTranslateY())
                        ),
                        new KeyFrame(Duration.millis(timeAtCurrentVelocity),
                                new EventHandler<ActionEvent>() {
                                    @Override public void handle(ActionEvent actionEvent) {
                                        locked.set(false);
                                        hideShowTimeline.stop();
                                        hideShowTimeline.setRate(-1);
                                        hideShowTimeline.play();
                                    }
                                },
                                new KeyValue(centerCircle.translateXProperty(),endX-centerCircle.getLayoutX(), Interpolator.EASE_IN),
                                new KeyValue(centerCircle.translateYProperty(),endY-centerCircle.getLayoutY(), Interpolator.EASE_IN)
                        )
                ).play();

            }
        } else {
            new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(centerCircle.translateXProperty(),centerCircle.getTranslateX()),
                            new KeyValue(centerCircle.translateYProperty(),centerCircle.getTranslateY())
                    ),
                    new KeyFrame(Duration.seconds(.3),
                            new EventHandler<ActionEvent>() {
                                @Override public void handle(ActionEvent actionEvent) {
                                    SECONDS.addListener(LockScreenApp.this);
                                }
                            },
                            new KeyValue(centerCircle.translateXProperty(),0, Interpolator.EASE_OUT),
                            new KeyValue(centerCircle.translateYProperty(),0, Interpolator.EASE_OUT)
                    )
            ).play();
            hideShowTimeline.stop();
            hideShowTimeline.setRate(-1);
            hideShowTimeline.play();
        }
    }
}
