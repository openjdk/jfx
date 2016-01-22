/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates.
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
package ensemble.samples.scenegraph.events.multitouch;

import javafx.event.EventHandler;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.RotateEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

public class MultiTouchImageView extends StackPane {

    private ImageView imageView;
    private double lastX, lastY, startScale, startRotate;

    public MultiTouchImageView(Image img) {
        setEffect(new DropShadow(BlurType.GAUSSIAN, Color.rgb(0, 0, 0, 0.5), 8, 0, 0, 2));

        imageView = new ImageView(img);
        imageView.setSmooth(true);
        getChildren().add(imageView);

        setOnMousePressed((MouseEvent event) -> {
            lastX = event.getX();
            lastY = event.getY();
            toFront();
            //  postView.toFront();
        });
        setOnMouseDragged((MouseEvent event) -> {
            double layoutX = getLayoutX() + (event.getX() - lastX);
            double layoutY = getLayoutY() + (event.getY() - lastY);

            if ((layoutX >= 0) && (layoutX <= (getParent().getLayoutBounds().getWidth()))) {
                setLayoutX(layoutX);
            }

            if ((layoutY >= 0) && (layoutY <= (getParent().getLayoutBounds().getHeight()))) {
                setLayoutY(layoutY);
            }

            if ((getLayoutX() + (event.getX() - lastX) <= 0)) {
                setLayoutX(0);
            }
        });
        addEventHandler(ZoomEvent.ZOOM_STARTED, (ZoomEvent event) -> {
            startScale = getScaleX();
        });
        addEventHandler(ZoomEvent.ZOOM, (ZoomEvent event) -> {
            setScaleX(startScale * event.getTotalZoomFactor());
            setScaleY(startScale * event.getTotalZoomFactor());
        });
        addEventHandler(RotateEvent.ROTATION_STARTED, (RotateEvent event) -> {
            startRotate = getRotate();
        });
        addEventHandler(RotateEvent.ROTATE, (RotateEvent event) -> {
            setRotate(startRotate + event.getTotalAngle());
        });

    }
}
