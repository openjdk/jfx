/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates.
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

package com.oracle.javafx.scenebuilder.kit.editor.job.gridpane.v2;

import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.util.GridBounds;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

/**
 *
 */
public class GridSnapshotItem {
        
        private final Integer columnIndex;
        private final Integer rowIndex;
        private final Integer columnSpan;
        private final Integer rowSpan;
        private final Priority vgrow;
        private final Priority hgrow;
        private final VPos valignment;
        private final HPos halignment;
        
        public GridSnapshotItem(FXOMObject fxomObject) {
            assert fxomObject != null;
            assert fxomObject.getSceneGraphObject() instanceof Node;
            
            final Node node = (Node) fxomObject.getSceneGraphObject();
            this.columnIndex = GridPane.getColumnIndex(node);
            this.rowIndex = GridPane.getRowIndex(node);
            this.columnSpan = GridPane.getColumnSpan(node);
            this.rowSpan = GridPane.getRowSpan(node);
            this.vgrow = GridPane.getVgrow(node);
            this.hgrow = GridPane.getHgrow(node);
            this.valignment = GridPane.getValignment(node);
            this.halignment = GridPane.getHalignment(node);
        }

        public GridSnapshotItem(FXOMObject fxomObject, int columnIndex, int rowIndex) {
            assert fxomObject != null;
            assert fxomObject.getSceneGraphObject() instanceof Node;
            assert columnIndex >= 0;
            assert rowIndex >= 0;
            
            this.columnIndex = columnIndex;
            this.rowIndex = rowIndex;
            this.columnSpan = null;
            this.rowSpan = null;
            this.vgrow = null;
            this.hgrow = null;
            this.valignment = null;
            this.halignment = null;
        }

        public int getColumnIndex() {
            return (columnIndex == null) ? 0 : columnIndex;
        }

        public int getRowIndex() {
            return (rowIndex == null) ? 0 : rowIndex;
        }

        public int getColumnSpan() {
            return (columnSpan == null) ? 1 : columnSpan;
        }

        public int getRowSpan() {
            return (rowSpan == null) ? 1 : rowSpan;
        }

        public Priority getVgrow() {
            return vgrow;
        }

        public Priority getHgrow() {
            return hgrow;
        }

        public VPos getValignment() {
            return valignment;
        }

        public HPos getHalignment() {
            return halignment;
        }
        
        public GridBounds getBounds() {
            final int actualColumnIndex = (columnIndex == null) ? 0 : columnIndex;
            final int actualRowIndex = (rowIndex == null) ? 0 : rowIndex;
            
            final int actualColumnSpan;
            if ((columnSpan == null) || (columnSpan == GridPane.REMAINING)) {
                actualColumnSpan = 1;
            } else {
                actualColumnSpan = columnSpan;
            }
            final int actualRowSpan;
            if ((rowSpan == null) || (rowSpan == GridPane.REMAINING)) {
                actualRowSpan = 1;
            } else {
                actualRowSpan = rowSpan;
            }
            
            return new GridBounds(actualColumnIndex, actualRowIndex, actualColumnSpan, actualRowSpan);
        }
}
