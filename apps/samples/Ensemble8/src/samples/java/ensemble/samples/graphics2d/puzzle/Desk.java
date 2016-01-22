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
package ensemble.samples.graphics2d.puzzle;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

public class Desk extends Pane {

    /**
     * Node that represents the playing area/desktop where the puzzle pieces sit
     */
    Desk(int numOfColumns, int numOfRows) {
        setStyle("-fx-background-color: #cccccc; "
                + "-fx-border-color: #464646; "
                + "-fx-effect: innershadow( two-pass-box , rgba(0,0,0,0.8) , 15, 0.0 , 0 , 4 );");
        double DESK_WIDTH = Piece.SIZE * numOfColumns;
        double DESK_HEIGHT = Piece.SIZE * numOfRows;
        setPrefSize(DESK_WIDTH, DESK_HEIGHT);
        setMaxSize(DESK_WIDTH, DESK_HEIGHT);
        autosize();
        // create path for lines
        Path grid = new Path();
        grid.setStroke(Color.rgb(70, 70, 70));
        getChildren().add(grid);
        // create vertical lines
        for (int col = 0; col < numOfColumns - 1; col++) {
            grid.getElements().addAll(
                    new MoveTo(Piece.SIZE + Piece.SIZE * col, 5),
                    new LineTo(Piece.SIZE + Piece.SIZE * col, Piece.SIZE * numOfRows - 5));
        }
        // create horizontal lines
        for (int row = 0; row < numOfRows - 1; row++) {
            grid.getElements().addAll(
                    new MoveTo(5, Piece.SIZE + Piece.SIZE * row),
                    new LineTo(Piece.SIZE * numOfColumns - 5, Piece.SIZE + Piece.SIZE * row));
        }
    }

    @Override protected void layoutChildren() {}
}
