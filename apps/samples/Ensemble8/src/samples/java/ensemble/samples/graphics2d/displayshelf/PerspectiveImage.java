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
package ensemble.samples.graphics2d.displayshelf;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Parent;
import javafx.scene.effect.PerspectiveTransform;
import javafx.scene.effect.Reflection;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * A Node that displays a image with some 2.5D perspective rotation around the Y
 * axis.
 */
public class PerspectiveImage extends Parent {

    private static final double REFLECTION_SIZE = 0.25;

    public static final double WIDTH = 200;
    public static final double HEIGHT = WIDTH + (WIDTH * REFLECTION_SIZE);

    private static final double RADIUS_H = WIDTH / 2;
    private static final double BACK = WIDTH / 10;
    private PerspectiveTransform transform = new PerspectiveTransform();
    /**
     * Angle Property
     */
    public final DoubleProperty angle = new SimpleDoubleProperty(45) {
        @Override
        protected void invalidated() {
            // when angle changes calculate new transform
            double lx = (RADIUS_H - Math.sin(Math.toRadians(angle.get())) * RADIUS_H - 1);
            double rx = (RADIUS_H + Math.sin(Math.toRadians(angle.get())) * RADIUS_H + 1);
            double uly = (-Math.cos(Math.toRadians(angle.get())) * BACK);
            double ury = -uly;
            transform.setUlx(lx);
            transform.setUly(uly);
            transform.setUrx(rx);
            transform.setUry(ury);
            transform.setLrx(rx);
            transform.setLry(HEIGHT + uly);
            transform.setLlx(lx);
            transform.setLly(HEIGHT + ury);
        }
    };

    public final double getAngle() {
        return angle.getValue();
    }

    public final void setAngle(double value) {
        angle.setValue(value);
    }

    public final DoubleProperty angleModel() {
        return angle;
    }

    public PerspectiveImage(Image image) {
        ImageView imageView = new ImageView(image);
        Reflection reflection = new Reflection();
        reflection.setFraction(REFLECTION_SIZE);
        imageView.setEffect(reflection);
        setEffect(transform);
        getChildren().addAll(imageView);
    }
}