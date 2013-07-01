/*
 * Copyright (c) 2008, 2013 Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ensemble.samplepage;

import javafx.animation.TranslateTransitionBuilder;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.ObjectExpression;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.shape.Polygon;

/**
 *
 */
public class SlidingPages extends Region {
    
    private Region frontPage;
    private Region backPage;
    private ImageView actionNode;
    private SelfPageSlide slide;
    private Polygon clip;

    public SlidingPages() {
        actionNode = new ImageView("ensemble/images/corner-top.png");
        actionNode.setFitWidth(100);
        actionNode.setFitHeight(100);
        actionNode.setPickOnBounds(true);
        getChildren().add(actionNode);
    }

    @Override protected void layoutChildren() {
        super.layoutChildren();
        if (frontPage != null) {
            frontPage.resize(getWidth(), getHeight());
        }
//        if (actionNode != null) {
//            actionNode.relocate(
//                    getWidth() - actionNode.getLayoutBounds().getWidth(), 
//                    getHeight() - actionNode.getLayoutBounds().getHeight());
//        }
        if (backPage != null) {
            backPage.resize(getWidth(), getHeight());
        }
    }

    public void setFrontPage(Region frontPage) {
        if (this.frontPage != null) {
            getChildren().remove(this.frontPage);
            actionNode.layoutXProperty().unbind();
            actionNode.layoutYProperty().unbind();
        }
        this.frontPage = frontPage;
        if (frontPage != null) {
            getChildren().add(getChildren().indexOf(actionNode), this.frontPage);
            actionNode.translateXProperty().bind(frontPage.translateXProperty().add(frontPage.widthProperty()).subtract(actionNode.getLayoutBounds().getWidth()));
            actionNode.translateYProperty().bind(frontPage.translateYProperty().add(frontPage.heightProperty()).subtract(actionNode.getLayoutBounds().getHeight()));
            frontPage.clipProperty().bind(new ObjectBinding<Node>() {
               
                {
                    bind(widthProperty(), heightProperty());
                }

                @Override
                protected Node computeValue() {
                    if (clip == null) {
                        clip = new Polygon();
                    }
                    
                    clip.getPoints().setAll(
                            0d, 0d, 
                            0d, getHeight(), 
                            getWidth() - 68, getHeight(),
                            getWidth() - 53, getHeight() - 8,
                            getWidth() - 2, getHeight() - 66,
                            getWidth(), getHeight() - 73,
                            getWidth(), 0d
                    );
                    return clip;
                }
            });
        }
        setupSlide();
    }

    public void setBackPage(Region backPage) {
        if (this.backPage != null) {
            getChildren().remove(this.backPage);
        }
        this.backPage = backPage;
        if (backPage != null) {
            getChildren().add(0, this.backPage);
        }
        setupSlide();
    }

    private void setupSlide() {
        if (frontPage == null || backPage == null) {
            slide = null;
            return;
        }
        slide = new SelfPageSlide(backPage, frontPage, actionNode, 0, 0, 
                widthProperty().negate().add(actionNode.getLayoutBounds().getWidth()), 
                heightProperty().negate().add(actionNode.getLayoutBounds().getHeight()));
    }
    
    public static class SelfPageSlide {
        private double fromX, fromY;
        private DoubleExpression toX, toY;
        private double delta;
        private double step, prev;
        private double actualFromX, actualFromY;
        private Node targetScreen;
        private Node baseScreen;
        private ObjectExpression<Point2D> v2;
        private DoubleExpression quadLenV2;

//        protected void stopCurrentScreen() {
//            screenNavigator.stop(currentScreen, false);
//        }
        
        private double calcX(double v) {
            return fromX * (1 - v) + toX.get() * v;
        }
        
        private double calcY(double v) {
            return fromY * (1 - v) + toY.get() * v;
        }
        
        private double calcValue(double x, double y) {
            return v2.get().dotProduct(x - fromX, y - fromY) / quadLenV2.get();
        }

        public SelfPageSlide(Node inCurrentScreen, Node inTargetScreen, final Node actionNode, final double fromX, final double fromY, final DoubleExpression toX, final DoubleExpression toY) {
//            super(inCurrentScreen);
            this.fromX = fromX;
            this.fromY = fromY;
            this.toX = toX;
            this.toY = toY;
            this.targetScreen = inTargetScreen;
//            removeScreen = false;
            v2 = new ObjectBinding<Point2D>() {
                { bind(toX, toY); }
                @Override protected Point2D computeValue() {
                    return new Point2D(toX.get() - fromX, toY.get() - fromY);
                }
            };
            quadLenV2 = new DoubleBinding() {
                { bind(v2); }
                @Override protected double computeValue() {
                    return v2.get().getX() * v2.get().getX() + v2.get().getY() * v2.get().getY();
                }
            };
            
            actionNode.setOnMousePressed(new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent me) {
                    identifyScreens();
//                    stopCurrentScreen();
//                    onTransitionStart();
//                    targetScreen.toFront();
                    delta = calcValue(
                            targetScreen.getTranslateX() - me.getSceneX(), 
                            targetScreen.getTranslateY() - me.getSceneY());
                    actualFromX = targetScreen.getTranslateX();
                    actualFromY = targetScreen.getTranslateY();
                    prev = calcValue(me.getSceneX(), me.getSceneY());
                    step = 0;
                    me.consume();
                }
            });
            actionNode.setOnMouseDragged(new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent me) {
                    double value = Math.min(Math.max(calcValue(me.getSceneX(), me.getSceneY()) + delta, 0), 1);
                    targetScreen.setTranslateX(calcX(value));
                    targetScreen.setTranslateY(calcY(value));
                    if (value != prev) {
                        step = value - prev;
                        prev = value;
                    }
                    me.consume();
                }
            });
            actionNode.setOnMouseReleased(new EventHandler<MouseEvent>() {
                @Override public void handle(MouseEvent me) {
                    double targetX;
                    double targetY;
                    if (me.isStillSincePress()) {
                        double curValue = calcValue(targetScreen.getTranslateX(), targetScreen.getTranslateY());
                        targetX = curValue < 0.5 ? toX.get() : fromX;
                        targetY = curValue < 0.5 ? toY.get() : fromY;
                    } else {
                        targetX = step > 0 ? toX.get() : fromX;
                        targetY = step > 0 ? toY.get() : fromY;
                    }
                    TranslateTransitionBuilder.create()
                        .node(targetScreen)
                        .toX(targetX)
                        .toY(targetY)
//                        .onFinished(targetY == actualFromY && targetX == actualFromX ? onTransitionCanceled : onTransitionFinished)
                        .build().play();
                    me.consume();
                }
            });
        }

        public void identifyScreens() {
//            if (baseScreen == null) {
//                CompoundScreen compoundScreen = targetScreen.getParentScreen();
//                if (compoundScreen != null) {
//                    baseScreen = compoundScreen.getBase();
//                }
//            }
//            if (!(targetScreen.getTranslateY() == fromY && targetScreen.getTranslateX() == fromX)) {
//                currentScreen = baseScreen;
//                nextScreen = targetScreen;
//            } else {
//                currentScreen = targetScreen;
//                nextScreen = baseScreen;
//            }
        }
    }
    
}
