
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
 */package com.javafx.experiments.shape3d;

import com.javafx.experiments.shape3d.symbolic.SymbolicPolygonMesh;
import com.javafx.experiments.shape3d.symbolic.SymbolicSubdivisionBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Catmull Clark subdivision surface polygon mesh
 */
public class SubdivisionMesh extends PolygonMesh {
    private final PolygonMesh originalMesh;
    private int subdivisionLevel;
    private BoundaryMode boundaryMode;
    private MapBorderMode mapBorderMode;
    private final List<SymbolicPolygonMesh> symbolicMeshes;

    private boolean pointValuesDirty;
    private boolean meshDirty;
    private boolean subdivisionLevelDirty;

    /**
     * Describes whether the edges and points at the boundary are treated as creases
     */
    public enum BoundaryMode {
        /**
         * Only edges at the boundary are treated as creases
         */
        CREASE_EDGES,
        /**
         * Edges and points at the boundary are treated as creases
         */
        CREASE_ALL
    }

    /**
     * Describes how the new texture coordinate for the control point is defined
     */
    public enum MapBorderMode {
        /**
         * Jeeps the same uvs for all control points
         */
        NOT_SMOOTH,
        /**
         * Smooths uvs of points at corners
         */
        SMOOTH_INTERNAL,
        /**
         * Smooths uvs of points at boundaries and original control points (and creases [in the future when creases are defined])
         */
        SMOOTH_ALL
    }

    public SubdivisionMesh(PolygonMesh originalMesh, int subdivisionLevel, BoundaryMode boundaryMode, MapBorderMode mapBorderMode) {
        this.originalMesh = originalMesh;
        setSubdivisionLevelForced(subdivisionLevel);
        setBoundaryModeForced(boundaryMode);
        setMapBorderModeForced(mapBorderMode);

        symbolicMeshes = new ArrayList<>(4); // the polymesh is usually subdivided up to 3 times

        originalMesh.getPoints().addListener((observableArray, sizeChanged, from, to) -> {
            if (sizeChanged) {
                meshDirty = true;
            } else {
                pointValuesDirty = true;
            }
        });
        originalMesh.getTexCoords().addListener((observableArray, sizeChanged, from, to) -> meshDirty = true);
    }

    /**
     * Updates the variables of the underlying polygon mesh.
     * It only updates the fields that need to be updated.
     */
    public void update() {
        if (meshDirty) {
            symbolicMeshes.clear();
            symbolicMeshes.add(new SymbolicPolygonMesh(originalMesh));
            pointValuesDirty = true;
            subdivisionLevelDirty = true;
        }

        while (subdivisionLevel >= symbolicMeshes.size()) {
            symbolicMeshes.add(SymbolicSubdivisionBuilder.subdivide(symbolicMeshes.get(symbolicMeshes.size()-1), boundaryMode, mapBorderMode));
            pointValuesDirty = true;
            subdivisionLevelDirty = true;
        }

        if (pointValuesDirty) {
            for (int i = 0; i <= subdivisionLevel; i++) {
                SymbolicPolygonMesh symbolicMesh = symbolicMeshes.get(i);
                symbolicMesh.points.update();
            }
        }

        if (pointValuesDirty || subdivisionLevelDirty) {
            getPoints().setAll(symbolicMeshes.get(subdivisionLevel).points.data);
        }

        if (subdivisionLevelDirty) {
            faces = symbolicMeshes.get(subdivisionLevel).faces;
            numEdgesInFaces = -1;
            getFaceSmoothingGroups().setAll(symbolicMeshes.get(subdivisionLevel).faceSmoothingGroups);
            getTexCoords().setAll(symbolicMeshes.get(subdivisionLevel).texCoords);
        }

        meshDirty = false;
        pointValuesDirty = false;
        subdivisionLevelDirty = false;
    }

    private void setSubdivisionLevelForced(int subdivisionLevel) {
        this.subdivisionLevel = subdivisionLevel;
        subdivisionLevelDirty = true;
    }

    private void setBoundaryModeForced(SubdivisionMesh.BoundaryMode boundaryMode) {
        this.boundaryMode = boundaryMode;
        meshDirty = true;
    }

    private void setMapBorderModeForced(SubdivisionMesh.MapBorderMode mapBorderMode) {
        this.mapBorderMode = mapBorderMode;
        meshDirty = true;
    }

    public PolygonMesh getOriginalMesh() {
        return originalMesh;
    }

    public int getSubdivisionLevel() {
        return subdivisionLevel;
    }

    public void setSubdivisionLevel(int subdivisionLevel) {
        if (subdivisionLevel != this.subdivisionLevel) {
            setSubdivisionLevelForced(subdivisionLevel);
        }
    }

    public SubdivisionMesh.BoundaryMode getBoundaryMode() {
        return boundaryMode;
    }

    public void setBoundaryMode(SubdivisionMesh.BoundaryMode boundaryMode) {
        if (boundaryMode != this.boundaryMode) {
            setBoundaryModeForced(boundaryMode);
        }
    }

    public SubdivisionMesh.MapBorderMode getMapBorderMode() {
        return mapBorderMode;
    }

    public void setMapBorderMode(SubdivisionMesh.MapBorderMode mapBorderMode) {
        if (mapBorderMode != this.mapBorderMode) {
            setMapBorderModeForced(mapBorderMode);
        }
    }
}
