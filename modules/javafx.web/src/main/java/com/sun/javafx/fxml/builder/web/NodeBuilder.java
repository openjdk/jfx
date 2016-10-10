/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.fxml.builder.web;

// TODO: remove this class as part of fixing RT-40037.

/**
Builder class for javafx.scene.Node
@see javafx.scene.Node
@deprecated This class is deprecated and will be removed in the next version
* @since JavaFX 2.0
*/
@Deprecated
public abstract class NodeBuilder<B extends NodeBuilder<B>> {
    protected NodeBuilder() {
    }


    java.util.BitSet __set = new java.util.BitSet();
    private void __set(int i) {
        __set.set(i);
    }
    public void applyTo(javafx.scene.Node x) {
        java.util.BitSet set = __set;
        for (int i = -1; (i = set.nextSetBit(i + 1)) >= 0; ) {
            switch (i) {
                case 0: x.setBlendMode(this.blendMode); break;
                case 1: x.setCache(this.cache); break;
                case 2: x.setCacheHint(this.cacheHint); break;
                case 3: x.setClip(this.clip); break;
                case 4: x.setCursor(this.cursor); break;
                case 5: x.setDepthTest(this.depthTest); break;
                case 6: x.setDisable(this.disable); break;
                case 7: x.setEffect(this.effect); break;
                case 8: x.setEventDispatcher(this.eventDispatcher); break;
                case 9: x.setFocusTraversable(this.focusTraversable); break;
                case 10: x.setId(this.id); break;
                case 11: x.setInputMethodRequests(this.inputMethodRequests); break;
                case 12: x.setLayoutX(this.layoutX); break;
                case 13: x.setLayoutY(this.layoutY); break;
                case 14: x.setManaged(this.managed); break;
                case 15: x.setMouseTransparent(this.mouseTransparent); break;
                case 16: x.setOnContextMenuRequested(this.onContextMenuRequested); break;
                case 17: x.setOnDragDetected(this.onDragDetected); break;
                case 18: x.setOnDragDone(this.onDragDone); break;
                case 19: x.setOnDragDropped(this.onDragDropped); break;
                case 20: x.setOnDragEntered(this.onDragEntered); break;
                case 21: x.setOnDragExited(this.onDragExited); break;
                case 22: x.setOnDragOver(this.onDragOver); break;
                case 23: x.setOnInputMethodTextChanged(this.onInputMethodTextChanged); break;
                case 24: x.setOnKeyPressed(this.onKeyPressed); break;
                case 25: x.setOnKeyReleased(this.onKeyReleased); break;
                case 26: x.setOnKeyTyped(this.onKeyTyped); break;
                case 27: x.setOnMouseClicked(this.onMouseClicked); break;
                case 28: x.setOnMouseDragEntered(this.onMouseDragEntered); break;
                case 29: x.setOnMouseDragExited(this.onMouseDragExited); break;
                case 30: x.setOnMouseDragged(this.onMouseDragged); break;
                case 31: x.setOnMouseDragOver(this.onMouseDragOver); break;
                case 32: x.setOnMouseDragReleased(this.onMouseDragReleased); break;
                case 33: x.setOnMouseEntered(this.onMouseEntered); break;
                case 34: x.setOnMouseExited(this.onMouseExited); break;
                case 35: x.setOnMouseMoved(this.onMouseMoved); break;
                case 36: x.setOnMousePressed(this.onMousePressed); break;
                case 37: x.setOnMouseReleased(this.onMouseReleased); break;
                case 38: x.setOnRotate(this.onRotate); break;
                case 39: x.setOnRotationFinished(this.onRotationFinished); break;
                case 40: x.setOnRotationStarted(this.onRotationStarted); break;
                case 41: x.setOnScroll(this.onScroll); break;
                case 42: x.setOnScrollFinished(this.onScrollFinished); break;
                case 43: x.setOnScrollStarted(this.onScrollStarted); break;
                case 44: x.setOnSwipeDown(this.onSwipeDown); break;
                case 45: x.setOnSwipeLeft(this.onSwipeLeft); break;
                case 46: x.setOnSwipeRight(this.onSwipeRight); break;
                case 47: x.setOnSwipeUp(this.onSwipeUp); break;
                case 48: x.setOnTouchMoved(this.onTouchMoved); break;
                case 49: x.setOnTouchPressed(this.onTouchPressed); break;
                case 50: x.setOnTouchReleased(this.onTouchReleased); break;
                case 51: x.setOnTouchStationary(this.onTouchStationary); break;
                case 52: x.setOnZoom(this.onZoom); break;
                case 53: x.setOnZoomFinished(this.onZoomFinished); break;
                case 54: x.setOnZoomStarted(this.onZoomStarted); break;
                case 55: x.setOpacity(this.opacity); break;
                case 56: x.setPickOnBounds(this.pickOnBounds); break;
                case 57: x.setRotate(this.rotate); break;
                case 58: x.setRotationAxis(this.rotationAxis); break;
                case 59: x.setScaleX(this.scaleX); break;
                case 60: x.setScaleY(this.scaleY); break;
                case 61: x.setScaleZ(this.scaleZ); break;
                case 62: x.setStyle(this.style); break;
                case 63: x.getStyleClass().addAll(this.styleClass); break;
                case 64: x.getTransforms().addAll(this.transforms); break;
                case 65: x.setTranslateX(this.translateX); break;
                case 66: x.setTranslateY(this.translateY); break;
                case 67: x.setTranslateZ(this.translateZ); break;
                case 68: x.setUserData(this.userData); break;
                case 69: x.setVisible(this.visible); break;
            }
        }
    }

    private javafx.scene.effect.BlendMode blendMode;
    /**
    Set the value of the {@link javafx.scene.Node#getBlendMode() blendMode} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B blendMode(javafx.scene.effect.BlendMode x) {
        this.blendMode = x;
        __set(0);
        return (B) this;
    }

    private boolean cache;
    /**
    Set the value of the {@link javafx.scene.Node#isCache() cache} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B cache(boolean x) {
        this.cache = x;
        __set(1);
        return (B) this;
    }

    private javafx.scene.CacheHint cacheHint;
    /**
    Set the value of the {@link javafx.scene.Node#getCacheHint() cacheHint} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B cacheHint(javafx.scene.CacheHint x) {
        this.cacheHint = x;
        __set(2);
        return (B) this;
    }

    private javafx.scene.Node clip;
    /**
    Set the value of the {@link javafx.scene.Node#getClip() clip} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B clip(javafx.scene.Node x) {
        this.clip = x;
        __set(3);
        return (B) this;
    }

    private javafx.scene.Cursor cursor;
    /**
    Set the value of the {@link javafx.scene.Node#getCursor() cursor} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B cursor(javafx.scene.Cursor x) {
        this.cursor = x;
        __set(4);
        return (B) this;
    }

    private javafx.scene.DepthTest depthTest;
    /**
    Set the value of the {@link javafx.scene.Node#getDepthTest() depthTest} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B depthTest(javafx.scene.DepthTest x) {
        this.depthTest = x;
        __set(5);
        return (B) this;
    }

    private boolean disable;
    /**
    Set the value of the {@link javafx.scene.Node#isDisable() disable} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B disable(boolean x) {
        this.disable = x;
        __set(6);
        return (B) this;
    }

    private javafx.scene.effect.Effect effect;
    /**
    Set the value of the {@link javafx.scene.Node#getEffect() effect} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B effect(javafx.scene.effect.Effect x) {
        this.effect = x;
        __set(7);
        return (B) this;
    }

    private javafx.event.EventDispatcher eventDispatcher;
    /**
    Set the value of the {@link javafx.scene.Node#getEventDispatcher() eventDispatcher} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B eventDispatcher(javafx.event.EventDispatcher x) {
        this.eventDispatcher = x;
        __set(8);
        return (B) this;
    }

    private boolean focusTraversable;
    /**
    Set the value of the {@link javafx.scene.Node#isFocusTraversable() focusTraversable} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B focusTraversable(boolean x) {
        this.focusTraversable = x;
        __set(9);
        return (B) this;
    }

    private java.lang.String id;
    /**
    Set the value of the {@link javafx.scene.Node#getId() id} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B id(java.lang.String x) {
        this.id = x;
        __set(10);
        return (B) this;
    }

    private javafx.scene.input.InputMethodRequests inputMethodRequests;
    /**
    Set the value of the {@link javafx.scene.Node#getInputMethodRequests() inputMethodRequests} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B inputMethodRequests(javafx.scene.input.InputMethodRequests x) {
        this.inputMethodRequests = x;
        __set(11);
        return (B) this;
    }

    private double layoutX;
    /**
    Set the value of the {@link javafx.scene.Node#getLayoutX() layoutX} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B layoutX(double x) {
        this.layoutX = x;
        __set(12);
        return (B) this;
    }

    private double layoutY;
    /**
    Set the value of the {@link javafx.scene.Node#getLayoutY() layoutY} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B layoutY(double x) {
        this.layoutY = x;
        __set(13);
        return (B) this;
    }

    private boolean managed;
    /**
    Set the value of the {@link javafx.scene.Node#isManaged() managed} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B managed(boolean x) {
        this.managed = x;
        __set(14);
        return (B) this;
    }

    private boolean mouseTransparent;
    /**
    Set the value of the {@link javafx.scene.Node#isMouseTransparent() mouseTransparent} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B mouseTransparent(boolean x) {
        this.mouseTransparent = x;
        __set(15);
        return (B) this;
    }

    private javafx.event.EventHandler<? super javafx.scene.input.ContextMenuEvent> onContextMenuRequested;
    /**
    Set the value of the {@link javafx.scene.Node#getOnContextMenuRequested() onContextMenuRequested} property for the instance constructed by this builder.
    * @since JavaFX 2.1
    */
    @SuppressWarnings("unchecked")
    public B onContextMenuRequested(javafx.event.EventHandler<? super javafx.scene.input.ContextMenuEvent> x) {
        this.onContextMenuRequested = x;
        __set(16);
        return (B) this;
    }

    private javafx.event.EventHandler<? super javafx.scene.input.MouseEvent> onDragDetected;
    /**
    Set the value of the {@link javafx.scene.Node#getOnDragDetected() onDragDetected} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B onDragDetected(javafx.event.EventHandler<? super javafx.scene.input.MouseEvent> x) {
        this.onDragDetected = x;
        __set(17);
        return (B) this;
    }

    private javafx.event.EventHandler<? super javafx.scene.input.DragEvent> onDragDone;
    /**
    Set the value of the {@link javafx.scene.Node#getOnDragDone() onDragDone} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B onDragDone(javafx.event.EventHandler<? super javafx.scene.input.DragEvent> x) {
        this.onDragDone = x;
        __set(18);
        return (B) this;
    }

    private javafx.event.EventHandler<? super javafx.scene.input.DragEvent> onDragDropped;
    /**
    Set the value of the {@link javafx.scene.Node#getOnDragDropped() onDragDropped} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B onDragDropped(javafx.event.EventHandler<? super javafx.scene.input.DragEvent> x) {
        this.onDragDropped = x;
        __set(19);
        return (B) this;
    }

    private javafx.event.EventHandler<? super javafx.scene.input.DragEvent> onDragEntered;
    /**
    Set the value of the {@link javafx.scene.Node#getOnDragEntered() onDragEntered} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B onDragEntered(javafx.event.EventHandler<? super javafx.scene.input.DragEvent> x) {
        this.onDragEntered = x;
        __set(20);
        return (B) this;
    }

    private javafx.event.EventHandler<? super javafx.scene.input.DragEvent> onDragExited;
    /**
    Set the value of the {@link javafx.scene.Node#getOnDragExited() onDragExited} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B onDragExited(javafx.event.EventHandler<? super javafx.scene.input.DragEvent> x) {
        this.onDragExited = x;
        __set(21);
        return (B) this;
    }

    private javafx.event.EventHandler<? super javafx.scene.input.DragEvent> onDragOver;
    /**
    Set the value of the {@link javafx.scene.Node#getOnDragOver() onDragOver} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B onDragOver(javafx.event.EventHandler<? super javafx.scene.input.DragEvent> x) {
        this.onDragOver = x;
        __set(22);
        return (B) this;
    }

    private javafx.event.EventHandler<? super javafx.scene.input.InputMethodEvent> onInputMethodTextChanged;
    /**
    Set the value of the {@link javafx.scene.Node#getOnInputMethodTextChanged() onInputMethodTextChanged} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B onInputMethodTextChanged(javafx.event.EventHandler<? super javafx.scene.input.InputMethodEvent> x) {
        this.onInputMethodTextChanged = x;
        __set(23);
        return (B) this;
    }

    private javafx.event.EventHandler<? super javafx.scene.input.KeyEvent> onKeyPressed;
    /**
    Set the value of the {@link javafx.scene.Node#getOnKeyPressed() onKeyPressed} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B onKeyPressed(javafx.event.EventHandler<? super javafx.scene.input.KeyEvent> x) {
        this.onKeyPressed = x;
        __set(24);
        return (B) this;
    }

    private javafx.event.EventHandler<? super javafx.scene.input.KeyEvent> onKeyReleased;
    /**
    Set the value of the {@link javafx.scene.Node#getOnKeyReleased() onKeyReleased} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B onKeyReleased(javafx.event.EventHandler<? super javafx.scene.input.KeyEvent> x) {
        this.onKeyReleased = x;
        __set(25);
        return (B) this;
    }

    private javafx.event.EventHandler<? super javafx.scene.input.KeyEvent> onKeyTyped;
    /**
    Set the value of the {@link javafx.scene.Node#getOnKeyTyped() onKeyTyped} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B onKeyTyped(javafx.event.EventHandler<? super javafx.scene.input.KeyEvent> x) {
        this.onKeyTyped = x;
        __set(26);
        return (B) this;
    }

    private javafx.event.EventHandler<? super javafx.scene.input.MouseEvent> onMouseClicked;
    /**
    Set the value of the {@link javafx.scene.Node#getOnMouseClicked() onMouseClicked} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B onMouseClicked(javafx.event.EventHandler<? super javafx.scene.input.MouseEvent> x) {
        this.onMouseClicked = x;
        __set(27);
        return (B) this;
    }

    private javafx.event.EventHandler<? super javafx.scene.input.MouseDragEvent> onMouseDragEntered;
    /**
    Set the value of the {@link javafx.scene.Node#getOnMouseDragEntered() onMouseDragEntered} property for the instance constructed by this builder.
    * @since JavaFX 2.1
    */
    @SuppressWarnings("unchecked")
    public B onMouseDragEntered(javafx.event.EventHandler<? super javafx.scene.input.MouseDragEvent> x) {
        this.onMouseDragEntered = x;
        __set(28);
        return (B) this;
    }

    private javafx.event.EventHandler<? super javafx.scene.input.MouseDragEvent> onMouseDragExited;
    /**
    Set the value of the {@link javafx.scene.Node#getOnMouseDragExited() onMouseDragExited} property for the instance constructed by this builder.
    * @since JavaFX 2.1
    */
    @SuppressWarnings("unchecked")
    public B onMouseDragExited(javafx.event.EventHandler<? super javafx.scene.input.MouseDragEvent> x) {
        this.onMouseDragExited = x;
        __set(29);
        return (B) this;
    }

    private javafx.event.EventHandler<? super javafx.scene.input.MouseEvent> onMouseDragged;
    /**
    Set the value of the {@link javafx.scene.Node#getOnMouseDragged() onMouseDragged} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B onMouseDragged(javafx.event.EventHandler<? super javafx.scene.input.MouseEvent> x) {
        this.onMouseDragged = x;
        __set(30);
        return (B) this;
    }

    private javafx.event.EventHandler<? super javafx.scene.input.MouseDragEvent> onMouseDragOver;
    /**
    Set the value of the {@link javafx.scene.Node#getOnMouseDragOver() onMouseDragOver} property for the instance constructed by this builder.
    * @since JavaFX 2.1
    */
    @SuppressWarnings("unchecked")
    public B onMouseDragOver(javafx.event.EventHandler<? super javafx.scene.input.MouseDragEvent> x) {
        this.onMouseDragOver = x;
        __set(31);
        return (B) this;
    }

    private javafx.event.EventHandler<? super javafx.scene.input.MouseDragEvent> onMouseDragReleased;
    /**
    Set the value of the {@link javafx.scene.Node#getOnMouseDragReleased() onMouseDragReleased} property for the instance constructed by this builder.
    * @since JavaFX 2.1
    */
    @SuppressWarnings("unchecked")
    public B onMouseDragReleased(javafx.event.EventHandler<? super javafx.scene.input.MouseDragEvent> x) {
        this.onMouseDragReleased = x;
        __set(32);
        return (B) this;
    }

    private javafx.event.EventHandler<? super javafx.scene.input.MouseEvent> onMouseEntered;
    /**
    Set the value of the {@link javafx.scene.Node#getOnMouseEntered() onMouseEntered} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B onMouseEntered(javafx.event.EventHandler<? super javafx.scene.input.MouseEvent> x) {
        this.onMouseEntered = x;
        __set(33);
        return (B) this;
    }

    private javafx.event.EventHandler<? super javafx.scene.input.MouseEvent> onMouseExited;
    /**
    Set the value of the {@link javafx.scene.Node#getOnMouseExited() onMouseExited} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B onMouseExited(javafx.event.EventHandler<? super javafx.scene.input.MouseEvent> x) {
        this.onMouseExited = x;
        __set(34);
        return (B) this;
    }

    private javafx.event.EventHandler<? super javafx.scene.input.MouseEvent> onMouseMoved;
    /**
    Set the value of the {@link javafx.scene.Node#getOnMouseMoved() onMouseMoved} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B onMouseMoved(javafx.event.EventHandler<? super javafx.scene.input.MouseEvent> x) {
        this.onMouseMoved = x;
        __set(35);
        return (B) this;
    }

    private javafx.event.EventHandler<? super javafx.scene.input.MouseEvent> onMousePressed;
    /**
    Set the value of the {@link javafx.scene.Node#getOnMousePressed() onMousePressed} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B onMousePressed(javafx.event.EventHandler<? super javafx.scene.input.MouseEvent> x) {
        this.onMousePressed = x;
        __set(36);
        return (B) this;
    }

    private javafx.event.EventHandler<? super javafx.scene.input.MouseEvent> onMouseReleased;
    /**
    Set the value of the {@link javafx.scene.Node#getOnMouseReleased() onMouseReleased} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B onMouseReleased(javafx.event.EventHandler<? super javafx.scene.input.MouseEvent> x) {
        this.onMouseReleased = x;
        __set(37);
        return (B) this;
    }

    private javafx.event.EventHandler<? super javafx.scene.input.RotateEvent> onRotate;
    /**
    Set the value of the {@link javafx.scene.Node#getOnRotate() onRotate} property for the instance constructed by this builder.
    * @since JavaFX 2.2
    */
    @SuppressWarnings("unchecked")
    public B onRotate(javafx.event.EventHandler<? super javafx.scene.input.RotateEvent> x) {
        this.onRotate = x;
        __set(38);
        return (B) this;
    }

    private javafx.event.EventHandler<? super javafx.scene.input.RotateEvent> onRotationFinished;
    /**
    Set the value of the {@link javafx.scene.Node#getOnRotationFinished() onRotationFinished} property for the instance constructed by this builder.
    * @since JavaFX 2.2
    */
    @SuppressWarnings("unchecked")
    public B onRotationFinished(javafx.event.EventHandler<? super javafx.scene.input.RotateEvent> x) {
        this.onRotationFinished = x;
        __set(39);
        return (B) this;
    }

    private javafx.event.EventHandler<? super javafx.scene.input.RotateEvent> onRotationStarted;
    /**
    Set the value of the {@link javafx.scene.Node#getOnRotationStarted() onRotationStarted} property for the instance constructed by this builder.
    * @since JavaFX 2.2
    */
    @SuppressWarnings("unchecked")
    public B onRotationStarted(javafx.event.EventHandler<? super javafx.scene.input.RotateEvent> x) {
        this.onRotationStarted = x;
        __set(40);
        return (B) this;
    }

    private javafx.event.EventHandler<? super javafx.scene.input.ScrollEvent> onScroll;
    /**
    Set the value of the {@link javafx.scene.Node#getOnScroll() onScroll} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B onScroll(javafx.event.EventHandler<? super javafx.scene.input.ScrollEvent> x) {
        this.onScroll = x;
        __set(41);
        return (B) this;
    }

    private javafx.event.EventHandler<? super javafx.scene.input.ScrollEvent> onScrollFinished;
    /**
    Set the value of the {@link javafx.scene.Node#getOnScrollFinished() onScrollFinished} property for the instance constructed by this builder.
    * @since JavaFX 2.2
    */
    @SuppressWarnings("unchecked")
    public B onScrollFinished(javafx.event.EventHandler<? super javafx.scene.input.ScrollEvent> x) {
        this.onScrollFinished = x;
        __set(42);
        return (B) this;
    }

    private javafx.event.EventHandler<? super javafx.scene.input.ScrollEvent> onScrollStarted;
    /**
    Set the value of the {@link javafx.scene.Node#getOnScrollStarted() onScrollStarted} property for the instance constructed by this builder.
    * @since JavaFX 2.2
    */
    @SuppressWarnings("unchecked")
    public B onScrollStarted(javafx.event.EventHandler<? super javafx.scene.input.ScrollEvent> x) {
        this.onScrollStarted = x;
        __set(43);
        return (B) this;
    }

    private javafx.event.EventHandler<? super javafx.scene.input.SwipeEvent> onSwipeDown;
    /**
    Set the value of the {@link javafx.scene.Node#getOnSwipeDown() onSwipeDown} property for the instance constructed by this builder.
    * @since JavaFX 2.2
    */
    @SuppressWarnings("unchecked")
    public B onSwipeDown(javafx.event.EventHandler<? super javafx.scene.input.SwipeEvent> x) {
        this.onSwipeDown = x;
        __set(44);
        return (B) this;
    }

    private javafx.event.EventHandler<? super javafx.scene.input.SwipeEvent> onSwipeLeft;
    /**
    Set the value of the {@link javafx.scene.Node#getOnSwipeLeft() onSwipeLeft} property for the instance constructed by this builder.
    * @since JavaFX 2.2
    */
    @SuppressWarnings("unchecked")
    public B onSwipeLeft(javafx.event.EventHandler<? super javafx.scene.input.SwipeEvent> x) {
        this.onSwipeLeft = x;
        __set(45);
        return (B) this;
    }

    private javafx.event.EventHandler<? super javafx.scene.input.SwipeEvent> onSwipeRight;
    /**
    Set the value of the {@link javafx.scene.Node#getOnSwipeRight() onSwipeRight} property for the instance constructed by this builder.
    * @since JavaFX 2.2
    */
    @SuppressWarnings("unchecked")
    public B onSwipeRight(javafx.event.EventHandler<? super javafx.scene.input.SwipeEvent> x) {
        this.onSwipeRight = x;
        __set(46);
        return (B) this;
    }

    private javafx.event.EventHandler<? super javafx.scene.input.SwipeEvent> onSwipeUp;
    /**
    Set the value of the {@link javafx.scene.Node#getOnSwipeUp() onSwipeUp} property for the instance constructed by this builder.
    * @since JavaFX 2.2
    */
    @SuppressWarnings("unchecked")
    public B onSwipeUp(javafx.event.EventHandler<? super javafx.scene.input.SwipeEvent> x) {
        this.onSwipeUp = x;
        __set(47);
        return (B) this;
    }

    private javafx.event.EventHandler<? super javafx.scene.input.TouchEvent> onTouchMoved;
    /**
    Set the value of the {@link javafx.scene.Node#getOnTouchMoved() onTouchMoved} property for the instance constructed by this builder.
    * @since JavaFX 2.2
    */
    @SuppressWarnings("unchecked")
    public B onTouchMoved(javafx.event.EventHandler<? super javafx.scene.input.TouchEvent> x) {
        this.onTouchMoved = x;
        __set(48);
        return (B) this;
    }

    private javafx.event.EventHandler<? super javafx.scene.input.TouchEvent> onTouchPressed;
    /**
    Set the value of the {@link javafx.scene.Node#getOnTouchPressed() onTouchPressed} property for the instance constructed by this builder.
    * @since JavaFX 2.2
    */
    @SuppressWarnings("unchecked")
    public B onTouchPressed(javafx.event.EventHandler<? super javafx.scene.input.TouchEvent> x) {
        this.onTouchPressed = x;
        __set(49);
        return (B) this;
    }

    private javafx.event.EventHandler<? super javafx.scene.input.TouchEvent> onTouchReleased;
    /**
    Set the value of the {@link javafx.scene.Node#getOnTouchReleased() onTouchReleased} property for the instance constructed by this builder.
    * @since JavaFX 2.2
    */
    @SuppressWarnings("unchecked")
    public B onTouchReleased(javafx.event.EventHandler<? super javafx.scene.input.TouchEvent> x) {
        this.onTouchReleased = x;
        __set(50);
        return (B) this;
    }

    private javafx.event.EventHandler<? super javafx.scene.input.TouchEvent> onTouchStationary;
    /**
    Set the value of the {@link javafx.scene.Node#getOnTouchStationary() onTouchStationary} property for the instance constructed by this builder.
    * @since JavaFX 2.2
    */
    @SuppressWarnings("unchecked")
    public B onTouchStationary(javafx.event.EventHandler<? super javafx.scene.input.TouchEvent> x) {
        this.onTouchStationary = x;
        __set(51);
        return (B) this;
    }

    private javafx.event.EventHandler<? super javafx.scene.input.ZoomEvent> onZoom;
    /**
    Set the value of the {@link javafx.scene.Node#getOnZoom() onZoom} property for the instance constructed by this builder.
    * @since JavaFX 2.2
    */
    @SuppressWarnings("unchecked")
    public B onZoom(javafx.event.EventHandler<? super javafx.scene.input.ZoomEvent> x) {
        this.onZoom = x;
        __set(52);
        return (B) this;
    }

    private javafx.event.EventHandler<? super javafx.scene.input.ZoomEvent> onZoomFinished;
    /**
    Set the value of the {@link javafx.scene.Node#getOnZoomFinished() onZoomFinished} property for the instance constructed by this builder.
    * @since JavaFX 2.2
    */
    @SuppressWarnings("unchecked")
    public B onZoomFinished(javafx.event.EventHandler<? super javafx.scene.input.ZoomEvent> x) {
        this.onZoomFinished = x;
        __set(53);
        return (B) this;
    }

    private javafx.event.EventHandler<? super javafx.scene.input.ZoomEvent> onZoomStarted;
    /**
    Set the value of the {@link javafx.scene.Node#getOnZoomStarted() onZoomStarted} property for the instance constructed by this builder.
    * @since JavaFX 2.2
    */
    @SuppressWarnings("unchecked")
    public B onZoomStarted(javafx.event.EventHandler<? super javafx.scene.input.ZoomEvent> x) {
        this.onZoomStarted = x;
        __set(54);
        return (B) this;
    }

    private double opacity;
    /**
    Set the value of the {@link javafx.scene.Node#getOpacity() opacity} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B opacity(double x) {
        this.opacity = x;
        __set(55);
        return (B) this;
    }

    private boolean pickOnBounds;
    /**
    Set the value of the {@link javafx.scene.Node#isPickOnBounds() pickOnBounds} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B pickOnBounds(boolean x) {
        this.pickOnBounds = x;
        __set(56);
        return (B) this;
    }

    private double rotate;
    /**
    Set the value of the {@link javafx.scene.Node#getRotate() rotate} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B rotate(double x) {
        this.rotate = x;
        __set(57);
        return (B) this;
    }

    private javafx.geometry.Point3D rotationAxis;
    /**
    Set the value of the {@link javafx.scene.Node#getRotationAxis() rotationAxis} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B rotationAxis(javafx.geometry.Point3D x) {
        this.rotationAxis = x;
        __set(58);
        return (B) this;
    }

    private double scaleX;
    /**
    Set the value of the {@link javafx.scene.Node#getScaleX() scaleX} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B scaleX(double x) {
        this.scaleX = x;
        __set(59);
        return (B) this;
    }

    private double scaleY;
    /**
    Set the value of the {@link javafx.scene.Node#getScaleY() scaleY} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B scaleY(double x) {
        this.scaleY = x;
        __set(60);
        return (B) this;
    }

    private double scaleZ;
    /**
    Set the value of the {@link javafx.scene.Node#getScaleZ() scaleZ} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B scaleZ(double x) {
        this.scaleZ = x;
        __set(61);
        return (B) this;
    }

    private java.lang.String style;
    /**
    Set the value of the {@link javafx.scene.Node#getStyle() style} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B style(java.lang.String x) {
        this.style = x;
        __set(62);
        return (B) this;
    }

    private java.util.Collection<? extends java.lang.String> styleClass;
    /**
    Add the given items to the List of items in the {@link javafx.scene.Node#getStyleClass() styleClass} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B styleClass(java.util.Collection<? extends java.lang.String> x) {
        this.styleClass = x;
        __set(63);
        return (B) this;
    }

    /**
    Add the given items to the List of items in the {@link javafx.scene.Node#getStyleClass() styleClass} property for the instance constructed by this builder.
    */
    public B styleClass(java.lang.String... x) {
        return styleClass(java.util.Arrays.asList(x));
    }

    private java.util.Collection<? extends javafx.scene.transform.Transform> transforms;
    /**
    Add the given items to the List of items in the {@link javafx.scene.Node#getTransforms() transforms} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B transforms(java.util.Collection<? extends javafx.scene.transform.Transform> x) {
        this.transforms = x;
        __set(64);
        return (B) this;
    }

    /**
    Add the given items to the List of items in the {@link javafx.scene.Node#getTransforms() transforms} property for the instance constructed by this builder.
    */
    public B transforms(javafx.scene.transform.Transform... x) {
        return transforms(java.util.Arrays.asList(x));
    }

    private double translateX;
    /**
    Set the value of the {@link javafx.scene.Node#getTranslateX() translateX} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B translateX(double x) {
        this.translateX = x;
        __set(65);
        return (B) this;
    }

    private double translateY;
    /**
    Set the value of the {@link javafx.scene.Node#getTranslateY() translateY} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B translateY(double x) {
        this.translateY = x;
        __set(66);
        return (B) this;
    }

    private double translateZ;
    /**
    Set the value of the {@link javafx.scene.Node#getTranslateZ() translateZ} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B translateZ(double x) {
        this.translateZ = x;
        __set(67);
        return (B) this;
    }

    private java.lang.Object userData;
    /**
    Set the value of the {@link javafx.scene.Node#getUserData() userData} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B userData(java.lang.Object x) {
        this.userData = x;
        __set(68);
        return (B) this;
    }

    private boolean visible;
    /**
    Set the value of the {@link javafx.scene.Node#isVisible() visible} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B visible(boolean x) {
        this.visible = x;
        __set(69);
        return (B) this;
    }

}
