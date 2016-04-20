/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates.
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
package ensemble.samples.graphics3d.cubesystem;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;

public class Cube extends Group {

    final Rotate rx = new Rotate(0, Rotate.X_AXIS);
    final Rotate ry = new Rotate(0, Rotate.Y_AXIS);
    final Rotate rz = new Rotate(0, Rotate.Z_AXIS);

    public Cube(double size, Color color, double shade) {
        getTransforms().addAll(rz, ry, rx);
        // back face
        Rectangle r1 = new Rectangle(size, size,
                                     color.deriveColor(0.0, 1.0,
                                                       (1 - 0.5 * shade), 1.0));
        r1.setTranslateX(-0.5 * size);
        r1.setTranslateY(-0.5 * size);
        r1.setTranslateZ(0.5 * size);
        // bottom face
        Rectangle r2 = new Rectangle(size, size,
                                     color.deriveColor(0.0, 1.0,
                                                       (1 - 0.4 * shade), 1.0));
        r2.setTranslateX(-0.5 * size);
        r2.setTranslateY(0);
        r2.setRotationAxis(Rotate.X_AXIS);
        r2.setRotate(90);
        // right face
        Rectangle r3 = new Rectangle(size, size,
                                     color.deriveColor(0.0, 1.0,
                                                       (1 - 0.3 * shade), 1.0));
        r3.setTranslateX(-1 * size);
        r3.setTranslateY(-0.5 * size);
        r3.setRotationAxis(Rotate.Y_AXIS);
        r3.setRotate(90);
        // left face
        Rectangle r4 = new Rectangle(size, size,
                                     color.deriveColor(0.0, 1.0,
                                                       (1 - 0.2 * shade), 1.0));
        r4.setTranslateX(0);
        r4.setTranslateY(-0.5 * size);
        r4.setRotationAxis(Rotate.Y_AXIS);
        r4.setRotate(90);
        // top face
        Rectangle r5 = new Rectangle(size, size,
                                     color.deriveColor(0.0, 1.0,
                                                       (1 - 0.1 * shade), 1.0));
        r5.setTranslateX(-0.5 * size);
        r5.setTranslateY(-1 * size);
        r5.setRotationAxis(Rotate.X_AXIS);
        r5.setRotate(90);
        // front face
        Rectangle r6 = new Rectangle(size, size, color);
        r6.setTranslateX(-0.5 * size);
        r6.setTranslateY(-0.5 * size);
        r6.setTranslateZ(-0.5 * size);

        getChildren().addAll(r1, r2, r3, r4, r5, r6);
    }
}
