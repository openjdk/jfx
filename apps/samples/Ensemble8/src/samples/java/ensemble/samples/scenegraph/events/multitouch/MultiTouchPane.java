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

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;

public class MultiTouchPane extends Region {

    private ImageView postView;
    private static Image[] img = new Image[3];
    private Rectangle clipRect;

    public MultiTouchPane() {
        clipRect = new Rectangle();
        clipRect.setSmooth(false);
        setClip(clipRect);

        Image post = new Image(MultiTouchApp.class.getResource("/ensemble/samples/shared-resources/warning.png").toExternalForm(), false);
        postView = new ImageView(post);

        img[0] = new Image(MultiTouchApp.class.getResource("/ensemble/samples/shared-resources/Animal1.jpg").toExternalForm(), false);
        img[1] = new Image(MultiTouchApp.class.getResource("/ensemble/samples/shared-resources/Animal2.jpg").toExternalForm(), false);
        img[2] = new Image(MultiTouchApp.class.getResource("/ensemble/samples/shared-resources/Animal3.jpg").toExternalForm(), false);

        getChildren().add(postView);

        for (int i = 0; i <= 2; i++) {
            MultiTouchImageView iv = new MultiTouchImageView(img[i]);
            getChildren().add(iv);
        }
    }

    @Override
    protected void layoutChildren() {
        final double w = getWidth();
        final double h = getHeight();
        clipRect.setWidth(w);
        clipRect.setHeight(h);
        for (Node child : getChildren()) {
            if (child == postView) {
                postView.relocate(w - 15 - postView.getLayoutBounds().getWidth(), 0);
            } else if (child.getLayoutX() == 0 && child.getLayoutY() == 0) {
                final double iw = child.getBoundsInParent().getWidth();
                final double ih = child.getBoundsInParent().getHeight();
                child.setLayoutX((w - iw) * Math.random() + 100);
                child.setLayoutY((h - ih) * Math.random() + 100);
            }
        }
    }
}