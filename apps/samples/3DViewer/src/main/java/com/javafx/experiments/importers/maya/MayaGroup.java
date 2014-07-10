/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates.
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

package com.javafx.experiments.importers.maya;

import javafx.scene.Group;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

/**
 * MayaGroup - A MayaGroup is equivalent to a Maya Transform Node
 * <p/>
 * If you are post-multiplying matrices, To transform a point p from object-space to world-space you would need to
 * post-multiply by the worldMatrix. (p' = p * wm) matrix = [SP-1][S][SH][SP][ST][RP-1][RA][R][RP][RT][T] where R =
 * [RX][RY][RZ]  (Note: order is determined by rotateOrder)
 * <p/>
 * If you are pre-multiplying matrices, to transform a point p from object-space to world-space you would need to
 * pre-multiply by the worldMatrix. (p' = wm * p) matrix = [T][RT][RP][R][RA][RP-1][ST][SP][SH][S][SP-1] where R =
 * [RZ][RY][RX]  (Note: order is determined by rotateOrder) Of these sub-matrices we can set [RT], [RA], [ST], and [SH]
 * to identity, so matrix = [T][RP][R][RP-1][SP][S][SP-1] matrix = [T][RP][RZ][RY][RX][RP-1][SP][S][SP-1]
 */
public class MayaGroup extends Group {
    Translate t = new Translate();
    Translate rpt = new Translate();  // rotate pivot translate
    Translate rp = new Translate();  // rotate pivot
    Translate rpi = new Translate();  // rotate pivot inverse
    Translate spt = new Translate();  // scale pivot translate
    Translate sp = new Translate();  // scale pivot
    Translate spi = new Translate();  // scale pivot inverse
    // should bind rpi = -rp, but doesn't currently work afaict

    Rotate rx = new Rotate(0, Rotate.X_AXIS);
    Rotate ry = new Rotate(0, Rotate.Y_AXIS);
    Rotate rz = new Rotate(0, Rotate.Z_AXIS);

    Scale s = new Scale();

    public MayaGroup() {
        initTransforms();
    }

    /**
     * Creates mayaGroup with the same set of transforms as given mayaGroup. Children are not copied.
     *
     * @param mayaGroup
     */
    public MayaGroup(MayaGroup mayaGroup) {
        t = mayaGroup.t.clone();
        rpt = mayaGroup.rpt.clone();
        rp = mayaGroup.rp.clone();
        rpi = mayaGroup.rpi.clone();
        sp = mayaGroup.sp.clone();
        spi = mayaGroup.spi.clone();

        rx = mayaGroup.rx.clone();
        ry = mayaGroup.ry.clone();
        rz = mayaGroup.rz.clone();

        s = mayaGroup.s.clone();

        setId(mayaGroup.getId());
        setDepthTest(mayaGroup.getDepthTest());
        setVisible(mayaGroup.isVisible());

        initTransforms();
    }

    private void initTransforms() {
        getTransforms().setAll(t, rpt, rp, rz, ry, rx, rpi, spt, sp, s, spi);
    }
}
