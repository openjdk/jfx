package com.javafx.experiments.shape3d.symbolic;

import com.javafx.experiments.shape3d.PolygonMesh;

public class OriginalPointArray extends SymbolicPointArray {
    PolygonMesh mesh;

    public OriginalPointArray(PolygonMesh mesh) {
        super(new float[mesh.getPoints().size()]);
        this.mesh = mesh;
    }

    @Override
    public void update() {
        mesh.getPoints().copyTo(0, data, 0, data.length);
    }
}
