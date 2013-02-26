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
package ensemble.samples.graphics3d.xylophone;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.RectangleBuilder;
import javafx.scene.transform.Rotate;

public class Cube extends Group {

    final Rotate rx = new Rotate(0, Rotate.X_AXIS);
    final Rotate ry = new Rotate(0, Rotate.Y_AXIS);
    final Rotate rz = new Rotate(0, Rotate.Z_AXIS);

    public Cube(double size, Color color, double shade) {
        getTransforms().addAll(rz, ry, rx);
        getChildren().addAll(
                RectangleBuilder.create() // back face
                .width(size).height(size)
                .fill(color.deriveColor(0.0, 1.0, (1 - 0.5 * shade), 1.0))
                .translateX(-0.5 * size)
                .translateY(-0.5 * size)
                .translateZ(0.5 * size)
                .build(),
                RectangleBuilder.create() // bottom face
                .width(size).height(size)
                .fill(color.deriveColor(0.0, 1.0, (1 - 0.4 * shade), 1.0))
                .translateX(-0.5 * size)
                .translateY(0)
                .rotationAxis(Rotate.X_AXIS)
                .rotate(90)
                .build(),
                RectangleBuilder.create() // right face
                .width(size).height(size)
                .fill(color.deriveColor(0.0, 1.0, (1 - 0.3 * shade), 1.0))
                .translateX(-1 * size)
                .translateY(-0.5 * size)
                .rotationAxis(Rotate.Y_AXIS)
                .rotate(90)
                .build(),
                RectangleBuilder.create() // left face
                .width(size).height(size)
                .fill(color.deriveColor(0.0, 1.0, (1 - 0.2 * shade), 1.0))
                .translateX(0)
                .translateY(-0.5 * size)
                .rotationAxis(Rotate.Y_AXIS)
                .rotate(90)
                .build(),
                RectangleBuilder.create() // top face
                .width(size).height(size)
                .fill(color.deriveColor(0.0, 1.0, (1 - 0.1 * shade), 1.0))
                .translateX(-0.5 * size)
                .translateY(-1 * size)
                .rotationAxis(Rotate.X_AXIS)
                .rotate(90)
                .build(),
                RectangleBuilder.create() // top face
                .width(size).height(size)
                .fill(color)
                .translateX(-0.5 * size)
                .translateY(-0.5 * size)
                .translateZ(-0.5 * size)
                .build());
    }
}
