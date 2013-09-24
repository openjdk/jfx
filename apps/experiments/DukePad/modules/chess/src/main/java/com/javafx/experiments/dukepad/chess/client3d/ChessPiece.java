/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.javafx.experiments.dukepad.chess.client3d;


import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;


/**
 *
 */
public class ChessPiece extends Group {

    public static enum Side {
        BLACK, WHITE
    }

    public static enum Role {
        PAWN, ROOK, KNIGHT, BISHOP, QUEEN, KING
    }

    private static ChessPieceFactory chessPieceFactory;

    public static ChessPieceFactory getChessPieceFactory() {
        if (chessPieceFactory == null) {
            chessPieceFactory = new ChessPieceFactory();
        }
        return chessPieceFactory;
    }

    public static class ChessPieceFactory {

        private final PhongMaterial blackMaterial;
        private final PhongMaterial whiteMaterial;

        {
            blackMaterial = new PhongMaterial(Color.web("606060"));
            blackMaterial.setSpecularColor(Color.web("808080"));
            blackMaterial.setSpecularPower(18);
            whiteMaterial = new PhongMaterial(Color.WHITE);
            whiteMaterial.setSpecularColor(Color.web("E0E0E0"));
            whiteMaterial.setSpecularPower(18);
        }

        public final String hat(Role role) {
            return "hat_" + role.name().toLowerCase();
        }

        public ChessPiece createPiece(Side side, Role role) {
            ChessPiece duke = new ChessPiece();
            for (String key : Duke3DModel.MESHVIEW_MAP.keySet()) {
                if (key != null && key.contains("hat") && !key.contains(hat(role))) {
                    continue;
                }
                MeshView original = Duke3DModel.MESHVIEW_MAP.get(key);
                MeshView meshView = new MeshView(original.getMesh());
                if (side == Side.BLACK) {
                    meshView.setMaterial(blackMaterial);
                } else {
                    meshView.setMaterial(whiteMaterial);
                }
                duke.getChildren().add(meshView);
            }
            return duke;
        }
    }
}
