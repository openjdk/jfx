/*
 * Copyright (c) 2013 Oracle and/or its affiliates.
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

package nodecount;

import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.util.Duration;
import java.util.List;

/**
 */
public class RotatingGrid extends BenchTest {
    private ParallelTransition tx;

    RotatingGrid(BenchBase benchmark, int rows, int cols) {
        super(benchmark, rows, cols, false);
    }

    @Override public void setup(Scene scene) {
        super.setup(scene);
        List<Node> children = scene.getRoot().getChildrenUnmodifiable();
        tx = new ParallelTransition();
        for (int i=1; i<children.size(); i++) {
            Node n = children.get(i);
            RotateTransition rot = new RotateTransition(Duration.seconds(5), n);
            rot.setToAngle(360);
            tx.getChildren().add(rot);
        }
        tx.setCycleCount(ParallelTransition.INDEFINITE);
        tx.play();
    }

    @Override public void tearDown() {
        tx.stop();
    }
}
