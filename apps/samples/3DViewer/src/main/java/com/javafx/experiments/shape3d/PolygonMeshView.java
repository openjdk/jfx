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
package com.javafx.experiments.shape3d;

import java.util.Arrays;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ArrayChangeListener;
import javafx.collections.ObservableFloatArray;
import javafx.scene.Parent;
import javafx.scene.paint.Material;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import static javafx.scene.shape.TriangleMesh.*;
import static com.javafx.experiments.shape3d.SubdivisionMesh.*;

/**
 * A MeshView node for Polygon Meshes
 */
public class PolygonMeshView extends Parent {
    private static final boolean DEBUG = false;
    private final MeshView meshView = new MeshView();

    private TriangleMesh triangleMesh = new TriangleMesh();

    // this is null if no subdivision is happening (i.e. subdivisionLevel = 0);
    private SubdivisionMesh subdivisionMesh;

    private final ArrayChangeListener<ObservableFloatArray> meshPointsListener = (t, bln, i, i1) -> {
        pointsDirty = true;
        updateMesh();
    };
    private final ArrayChangeListener<ObservableFloatArray> meshTexCoordListener = (t, bln, i, i1) -> {
        texCoordsDirty = true;
        updateMesh();
    };

    private boolean pointsDirty = true;
    private boolean pointsSizeDirty = true;
    private boolean texCoordsDirty = true;
    private boolean facesDirty = true;

    // =========================================================================
    // PROPERTIES

    /**
     * Specifies the 3D mesh data of this {@code MeshView}.
     *
     * @defaultValue null
     */
    private ObjectProperty<PolygonMesh> meshProperty;
    public PolygonMesh getMesh() { return meshProperty().get(); }
    public void setMesh(PolygonMesh mesh) { meshProperty().set(mesh);}
    public ObjectProperty<PolygonMesh> meshProperty() {
        if (meshProperty == null) {
            meshProperty = new SimpleObjectProperty<PolygonMesh>();
            meshProperty.addListener((observable, oldValue, newValue) -> {
                if (oldValue != null) {
                    oldValue.getPoints().removeListener(meshPointsListener);
                    oldValue.getPoints().removeListener(meshTexCoordListener);
                }

                meshProperty.set(newValue);

                pointsDirty = pointsSizeDirty = texCoordsDirty = facesDirty = true;
                updateMesh();

                if (newValue != null) {
                    newValue.getPoints().addListener(meshPointsListener);
                    newValue.getTexCoords().addListener(meshTexCoordListener);
                }
            });
        }
        return meshProperty;
    }

    /**
     * Defines the drawMode this {@code Shape3D}.
     *
     * @defaultValue DrawMode.FILL
     */
    private ObjectProperty<DrawMode> drawMode;
    public final void setDrawMode(DrawMode value) { drawModeProperty().set(value); }
    public final DrawMode getDrawMode() { return drawMode == null ? DrawMode.FILL : drawMode.get(); }
    public final ObjectProperty<DrawMode> drawModeProperty() {
        if (drawMode == null) {
            drawMode = new SimpleObjectProperty<DrawMode>(PolygonMeshView.this, "drawMode", DrawMode.FILL) {
                @Override protected void invalidated() {
                    meshView.setDrawMode(get());
                    pointsDirty = pointsSizeDirty = texCoordsDirty = facesDirty = true;
                    updateMesh();
                }
            };
        }
        return drawMode;
    }

    /**
     * Defines the drawMode this {@code Shape3D}.
     *
     * @defaultValue CullFace.BACK
     */
    private ObjectProperty<CullFace> cullFace;
    public final void setCullFace(CullFace value) { cullFaceProperty().set(value); }
    public final CullFace getCullFace() { return cullFace == null ? CullFace.BACK : cullFace.get(); }
    public final ObjectProperty<CullFace> cullFaceProperty() {
        if (cullFace == null) {
            cullFace = new SimpleObjectProperty<CullFace>(PolygonMeshView.this, "cullFace", CullFace.BACK) {
                @Override protected void invalidated() {
                    meshView.setCullFace(get());
                }
            };
        }
        return cullFace;
    }

    /**
     * Defines the material this {@code Shape3D}.
     * The default material is null. If {@code Material} is null, a PhongMaterial
     * with a diffuse color of Color.LIGHTGRAY is used for rendering.
     *
     * @defaultValue null
     */
    private ObjectProperty<Material> materialProperty = new SimpleObjectProperty<Material>();
    public Material getMaterial() { return materialProperty.get(); }
    public void setMaterial(Material material) { materialProperty.set(material); }
    public ObjectProperty<Material> materialProperty() { return materialProperty; }

    /**
     * Number of iterations of Catmull Clark subdivision to apply to the mesh
     *
     * @defaultValue 0
     */
    private SimpleIntegerProperty subdivisionLevelProperty;
    public void setSubdivisionLevel(int subdivisionLevel) { subdivisionLevelProperty().set(subdivisionLevel); }
    public int getSubdivisionLevel() { return subdivisionLevelProperty == null ? 0 : subdivisionLevelProperty.get(); }
    public SimpleIntegerProperty subdivisionLevelProperty() {
        if (subdivisionLevelProperty == null) {
            subdivisionLevelProperty = new SimpleIntegerProperty(getSubdivisionLevel()) {
                @Override protected void invalidated() {
                    // create SubdivisionMesh if subdivisionLevel is greater than 0
                    if ((getSubdivisionLevel() > 0) && (subdivisionMesh == null)) {
                        subdivisionMesh = new SubdivisionMesh(getMesh(), getSubdivisionLevel(), getBoundaryMode(), getMapBorderMode());
                        subdivisionMesh.getOriginalMesh().getPoints().addListener((t, bln, i, i1) -> subdivisionMesh.update());
                        setMesh(subdivisionMesh);
                    }
                    if (subdivisionMesh != null) {
                        subdivisionMesh.setSubdivisionLevel(getSubdivisionLevel());
                        subdivisionMesh.update();
                    }
                    pointsDirty = pointsSizeDirty = texCoordsDirty = facesDirty = true;
                    updateMesh();
                }
            };
        }
        return subdivisionLevelProperty;
    }

    /**
     * Texture mapping boundary rule for Catmull Clark subdivision applied to the mesh
     *
     * @defaultValue BoundaryMode.CREASE_EDGES
     */
    private SimpleObjectProperty<BoundaryMode> boundaryMode;
    public void setBoundaryMode(BoundaryMode boundaryMode) { boundaryModeProperty().set(boundaryMode); }
    public BoundaryMode getBoundaryMode() { return boundaryMode == null ? BoundaryMode.CREASE_EDGES : boundaryMode.get(); }
    public SimpleObjectProperty<BoundaryMode> boundaryModeProperty() {
        if (boundaryMode == null) {
            boundaryMode = new SimpleObjectProperty<BoundaryMode>(getBoundaryMode()) {
                @Override protected void invalidated() {
                    if (subdivisionMesh != null) {
                        subdivisionMesh.setBoundaryMode(getBoundaryMode());
                        subdivisionMesh.update();
                    }
                    pointsDirty = true;
                    updateMesh();
                }
            };
        }
        return boundaryMode;
    }

    /**
     * Texture mapping smoothness option for Catmull Clark subdivision applied to the mesh
     *
     * @defaultValue MapBorderMode.NOT_SMOOTH
     */
    private SimpleObjectProperty<MapBorderMode> mapBorderMode;
    public void setMapBorderMode(MapBorderMode mapBorderMode) { mapBorderModeProperty().set(mapBorderMode); }
    public MapBorderMode getMapBorderMode() { return mapBorderMode == null ? MapBorderMode.NOT_SMOOTH : mapBorderMode.get(); }
    public SimpleObjectProperty<MapBorderMode> mapBorderModeProperty() {
        if (mapBorderMode == null) {
            mapBorderMode = new SimpleObjectProperty<MapBorderMode>(getMapBorderMode()) {
                @Override protected void invalidated() {
                    if (subdivisionMesh != null) {
                        subdivisionMesh.setMapBorderMode(getMapBorderMode());
                        subdivisionMesh.update();
                    }
                    texCoordsDirty = true;
                    updateMesh();
                }
            };
        }
        return mapBorderMode;
    }

    // =========================================================================
    // CONSTRUCTORS

    public PolygonMeshView() {
        meshView.materialProperty().bind(materialProperty());
        getChildren().add(meshView);
    }

    public PolygonMeshView(PolygonMesh mesh) {
        this();
        setMesh(mesh);
    }

    // =========================================================================
    // PRIVATE METHODS

    private void updateMesh() {
        PolygonMesh pmesh = getMesh();
        if (pmesh == null || pmesh.faces == null) {
            triangleMesh = new TriangleMesh();
            meshView.setMesh(triangleMesh);
            return;
        }

        final int pointElementSize = triangleMesh.getPointElementSize();
        final int faceElementSize = triangleMesh.getFaceElementSize();
        final boolean isWireframe = getDrawMode() == DrawMode.LINE;
        if (DEBUG) System.out.println("UPDATE MESH -- "+(isWireframe?"WIREFRAME":"SOLID"));
        final int numOfPoints = pmesh.getPoints().size() / pointElementSize;
        if (DEBUG) System.out.println("numOfPoints = " + numOfPoints);

        if(isWireframe) {
            // The current triangleMesh implementation gives buggy behavior when the size of faces are shrunken
            // Create a new TriangleMesh as a work around
            // [JIRA] (RT-31178)
            if (texCoordsDirty || facesDirty || pointsSizeDirty) {
                triangleMesh = new TriangleMesh();
                pointsDirty = pointsSizeDirty = texCoordsDirty = facesDirty = true; // to fill in the new triangle mesh
            }
            if (facesDirty) {
                facesDirty = false;
                // create faces for each edge
                int [] facesArray = new int [pmesh.getNumEdgesInFaces() * faceElementSize];
                int facesInd = 0;
                int pointsInd = pmesh.getPoints().size();
                for(int[] face: pmesh.faces) {
                    if (DEBUG) System.out.println("face.length = " + (face.length/2)+"  -- "+Arrays.toString(face));
                    int lastPointIndex = face[face.length-2];
                    if (DEBUG) System.out.println("    lastPointIndex = " + lastPointIndex);
                    for (int p=0;p<face.length;p+=2) {
                        int pointIndex = face[p];
                        if (DEBUG) System.out.println("        connecting point["+lastPointIndex+"] to point[" + pointIndex+"]");
                        facesArray[facesInd++] = lastPointIndex;
                        facesArray[facesInd++] = 0;
                        facesArray[facesInd++] = pointIndex;
                        facesArray[facesInd++] = 0;
                        facesArray[facesInd++] = pointsInd / pointElementSize;
                        facesArray[facesInd++] = 0;
                        if (DEBUG) System.out.println("            facesInd = " + facesInd);
                        pointsInd += pointElementSize;
                        lastPointIndex = pointIndex;
                    }
                }
                triangleMesh.getFaces().setAll(facesArray);
                triangleMesh.getFaceSmoothingGroups().clear();
            }
            if (texCoordsDirty) {
                texCoordsDirty = false;
                // set simple texCoords for wireframe
                triangleMesh.getTexCoords().setAll(0,0);
            }
            if (pointsDirty) {
                pointsDirty = false;
                // create points and copy over points to the first part of the array
                float [] pointsArray = new float [pmesh.getPoints().size() + pmesh.getNumEdgesInFaces()*3];
                pmesh.getPoints().copyTo(0, pointsArray, 0, pmesh.getPoints().size());

                // add point for each edge
                int pointsInd = pmesh.getPoints().size();
                for(int[] face: pmesh.faces) {
                    int lastPointIndex = face[face.length-2];
                    for (int p=0;p<face.length;p+=2) {
                        int pointIndex = face[p];
                        // get start and end point
                        final float x1 = pointsArray[lastPointIndex * pointElementSize];
                        final float y1 = pointsArray[lastPointIndex * pointElementSize + 1];
                        final float z1 = pointsArray[lastPointIndex * pointElementSize + 2];
                        final float x2 = pointsArray[pointIndex * pointElementSize];
                        final float y2 = pointsArray[pointIndex * pointElementSize + 1];
                        final float z2 = pointsArray[pointIndex * pointElementSize + 2];
                        final float distance = Math.abs(distanceBetweenPoints(x1,y1,z1,x2,y2,z2));
                        final float offset = distance/1000;
                        // add new point
                        pointsArray[pointsInd++] = x2 + offset;
                        pointsArray[pointsInd++] = y2 + offset;
                        pointsArray[pointsInd++] = z2 + offset;
                        lastPointIndex = pointIndex;
                    }
                }
                triangleMesh.getPoints().setAll(pointsArray);
            }
        } else {
            // The current triangleMesh implementation gives buggy behavior when the size of faces are shrunken
            // Create a new TriangleMesh as a work around
            // [JIRA] (RT-31178)
            if (texCoordsDirty || facesDirty || pointsSizeDirty) {
                triangleMesh = new TriangleMesh();
                pointsDirty = pointsSizeDirty = texCoordsDirty = facesDirty = true; // to fill in the new triangle mesh
            }
            if (facesDirty) {
                facesDirty = false;
                // create faces and break into triangles
                final int numOfFacesBefore = pmesh.faces.length;
                final int numOfFacesAfter = pmesh.getNumEdgesInFaces() - 2*numOfFacesBefore;
                int [] facesArray = new int [numOfFacesAfter * faceElementSize];
                int [] smoothingGroupsArray = new int [numOfFacesAfter];
                int facesInd = 0;
                for(int f = 0; f < pmesh.faces.length; f++) {
                    int[] face = pmesh.faces[f];
                    int currentSmoothGroup = pmesh.getFaceSmoothingGroups().get(f);
                    if (DEBUG) System.out.println("face.length = " + face.length+"  -- "+Arrays.toString(face));
                    int firstPointIndex = face[0];
                    int firstTexIndex = face[1];
                    int lastPointIndex = face[2];
                    int lastTexIndex = face[3];
                    for (int p=4;p<face.length;p+=2) {
                        int pointIndex = face[p];
                        int texIndex = face[p+1];
                        facesArray[facesInd * faceElementSize] = firstPointIndex;
                        facesArray[facesInd * faceElementSize + 1] = firstTexIndex;
                        facesArray[facesInd * faceElementSize + 2] = lastPointIndex;
                        facesArray[facesInd * faceElementSize + 3] = lastTexIndex;
                        facesArray[facesInd * faceElementSize + 4] = pointIndex;
                        facesArray[facesInd * faceElementSize + 5] = texIndex;
                        smoothingGroupsArray[facesInd] = currentSmoothGroup;
                        facesInd++;
                        lastPointIndex = pointIndex;
                        lastTexIndex = texIndex;
                    }
                }
                triangleMesh.getFaces().setAll(facesArray);
                triangleMesh.getFaceSmoothingGroups().setAll(smoothingGroupsArray);
            }
            if (texCoordsDirty) {
                texCoordsDirty = false;
                triangleMesh.getTexCoords().setAll(pmesh.getTexCoords());
            }
            if (pointsDirty) {
                pointsDirty = false;
                triangleMesh.getPoints().setAll(pmesh.getPoints());
            }
        }

        if (DEBUG) System.out.println("CREATING TRIANGLE MESH");
        if (DEBUG) System.out.println("    points    = "+Arrays.toString(((TriangleMesh) meshView.getMesh()).getPoints().toArray(null)));
        if (DEBUG) System.out.println("    texCoords = "+Arrays.toString(((TriangleMesh) meshView.getMesh()).getTexCoords().toArray(null)));
        if (DEBUG) System.out.println("    faces     = "+Arrays.toString(((TriangleMesh) meshView.getMesh()).getFaces().toArray(null)));

        if (meshView.getMesh() != triangleMesh) {
            meshView.setMesh(triangleMesh);
        }
        pointsDirty = pointsSizeDirty = texCoordsDirty = facesDirty = false;
    }

    private float distanceBetweenPoints(float x1, float y1, float z1, float x2, float y2, float z2) {
        return (float)Math.sqrt(
                Math.pow(z2 - z1,2) +
                Math.pow(x2 - x1,2) +
                Math.pow(y2 - y1,2));
    }
}
