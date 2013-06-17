package com.javafx.experiments.shape3d;

import java.util.Arrays;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ArrayChangeListener;
import javafx.collections.ObservableFloatArray;
import javafx.collections.ObservableIntegerArray;
import javafx.scene.Parent;
import javafx.scene.paint.Material;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
 * A MeshView node for Polygon Meshes
 */
public class PolygonMeshView extends Parent {
    private static final boolean DEBUG = false;
    private final MeshView meshView = new MeshView();
    private PolygonMesh subdividedMesh = null;
    // TODO keep only one TriangleMesh around

    // =========================================================================
    // PROPERTIES

    /**
     * Specifies the 3D mesh data of this {@code MeshView}.
     *
     * @defaultValue null
     */
    private ObjectProperty<PolygonMesh> mesh = new SimpleObjectProperty<PolygonMesh>(){
        @Override protected void invalidated() {
            updateSubdivision();
        }
    };
    public PolygonMesh getMesh() { return mesh.get(); }
    public void setMesh(PolygonMesh mesh) { 
        this.mesh.set(mesh);
        mesh.getPoints().addListener(new ArrayChangeListener<ObservableFloatArray>() {
            @Override
            public void onChanged(ObservableFloatArray t, boolean bln, int i, int i1) {
                // TODO don't update the whole mesh, only update the parts affected by points
                updateMesh();
            }
        });
    }
    
    public ObjectProperty<PolygonMesh> meshProperty() { return mesh; }
    
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
                    updateMesh();
                    meshView.setDrawMode(get());
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
    private SimpleIntegerProperty subdivisionLevel = new SimpleIntegerProperty(0) {
        @Override protected void invalidated() {
            updateSubdivision();
        }
    };
    public int getSubdivisionLevel() { return subdivisionLevel.get(); }
    public SimpleIntegerProperty subdivisionLevelProperty() { return subdivisionLevel; }
    public void setSubdivisionLevel(int subdivisionLevel) { this.subdivisionLevel.set(subdivisionLevel); }
    
    /**
     * Texture mapping boundary rule for Catmull Clark subdivision applied to the mesh
     *
     * @defaultValue BoundaryMode.CREASE_EDGES
     */
    private SimpleObjectProperty<SubDivision.BoundaryMode> boundaryMode = new SimpleObjectProperty<SubDivision.BoundaryMode>(SubDivision.BoundaryMode.CREASE_EDGES) {
        @Override protected void invalidated() {
            updateSubdivision();
        }
    };
    public SubDivision.BoundaryMode getBoundaryMode() { return boundaryMode.get(); }
    public SimpleObjectProperty<SubDivision.BoundaryMode> boundaryModeProperty() { return boundaryMode; }
    public void setBoundaryMode(SubDivision.BoundaryMode boundaryMode) { this.boundaryMode.set(boundaryMode); }
    
    /**
     * Texture mapping smoothness option for Catmull Clark subdivision applied to the mesh
     *
     * @defaultValue MapBorderMode.NOT_SMOOTH
     */
    private SimpleObjectProperty<SubDivision.MapBorderMode> mapBorderMode = new SimpleObjectProperty<SubDivision.MapBorderMode>(SubDivision.MapBorderMode.NOT_SMOOTH) {
        @Override protected void invalidated() {
            updateSubdivision();
        }
    };
    public SubDivision.MapBorderMode getMapBorderMode() { return mapBorderMode.get(); }
    public SimpleObjectProperty<SubDivision.MapBorderMode> mapBorderModeProperty() { return mapBorderMode; }
    public void setMapBorderMode(SubDivision.MapBorderMode mapBorderMode) { this.mapBorderMode.set(mapBorderMode); }

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

    private void updateSubdivision() {
        final int iterations = subdivisionLevel.get();
        if (iterations == 0) {
            subdividedMesh = null;
        } else {
            subdividedMesh = getMesh();
            for (int i=0; i<iterations; i++) {
                subdividedMesh = SubDivision.subdivide(subdividedMesh, boundaryMode.get(), mapBorderMode.get());
            }
        }
        updateMesh();
    }

    private void updateMesh() {
        PolygonMesh pmesh = subdividedMesh != null ? subdividedMesh : getMesh();
        if (pmesh == null || pmesh.faces == null || pmesh.texCoords == null) {
            meshView.setMesh(null);
            return;
        }
        final boolean isWireframe = getDrawMode() == DrawMode.LINE;
        if (DEBUG) System.out.println("UPDATE MESH -- "+(isWireframe?"WIREFRAME":"SOLID"));
        final TriangleMesh triangleMesh = new TriangleMesh();
        final int numOfPoints = pmesh.getPoints().size()/TriangleMesh.NUM_COMPONENTS_PER_POINT;
        if (DEBUG) System.out.println("numOfPoints = " + numOfPoints);
        
        if(isWireframe) {
            // create points and copy over points to the first part of the array
            float [] pointsArray = new float [pmesh.getPoints().size() + pmesh.getNumEdgesInFaces()*3];
            System.arraycopy(pmesh.getPoints().toArray(null), 0, pointsArray, 0, pmesh.getPoints().size());
            int pointsInd = pmesh.getPoints().size();

            // create faces and add point for each edge
            final int numOfFacesBefore = pmesh.faces.length;
            final int numOfFacesAfter = pmesh.getNumEdgesInFaces();
            int [] facesArray = new int [numOfFacesAfter * TriangleMesh.NUM_COMPONENTS_PER_FACE];
            int facesInd = 0;
            
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
                    facesArray[facesInd++] = pointsInd/TriangleMesh.NUM_COMPONENTS_PER_POINT;
                    facesArray[facesInd++] = 0;
                    final int numOfPoints2 = pointsInd/TriangleMesh.NUM_COMPONENTS_PER_POINT;
                    if (DEBUG) System.out.println("            numOfPoints = " + numOfPoints2);
                    // get start and end point
                    final float x1 = pointsArray[lastPointIndex*TriangleMesh.NUM_COMPONENTS_PER_POINT];
                    final float y1 = pointsArray[lastPointIndex*TriangleMesh.NUM_COMPONENTS_PER_POINT+1];
                    final float z1 = pointsArray[lastPointIndex*TriangleMesh.NUM_COMPONENTS_PER_POINT+2];
                    final float x2 = pointsArray[pointIndex*TriangleMesh.NUM_COMPONENTS_PER_POINT];
                    final float y2 = pointsArray[pointIndex*TriangleMesh.NUM_COMPONENTS_PER_POINT+1];
                    final float z2 = pointsArray[pointIndex*TriangleMesh.NUM_COMPONENTS_PER_POINT+2];
                    final float distance = Math.abs(distanceBetweenPoints(x1,y1,z1,x2,y2,z2));
                    final float offset = distance/1000;
                    // add new point
                    pointsArray[pointsInd++] = x2 + offset;
                    pointsArray[pointsInd++] = y2 + offset;
                    pointsArray[pointsInd++] = z2 + offset;
                    if (DEBUG) System.out.println("            facesInd = " + facesInd);
                    lastPointIndex = pointIndex;
                }
            }
            triangleMesh.getPoints().addAll(pointsArray);
            triangleMesh.getFaces().addAll(facesArray);
            if (DEBUG) System.out.println("numOfFacesBefore = " + numOfFacesBefore);
            if (DEBUG) System.out.println("numOfFacesAfter = " + numOfFacesAfter);
            // set simple texCoords for wireframe
            triangleMesh.getTexCoords().addAll(0,0);
        } else {
            // copy over points
            triangleMesh.getPoints().addAll(pmesh.getPoints());
        
            // create faces and break into triangles
            final int numOfFacesBefore = pmesh.faces.length;
            final int numOfFacesAfter = pmesh.getNumEdgesInFaces() - 2*numOfFacesBefore;
            int [] facesArray = new int [numOfFacesAfter * TriangleMesh.NUM_COMPONENTS_PER_FACE];
            int facesInd = 0;
            
            for(int[] face: pmesh.faces) {
                if (DEBUG) System.out.println("face.length = " + face.length+"  -- "+Arrays.toString(face));
                int firstPointIndex = face[0];
                int firstTexIndex = face[1];
                int lastPointIndex = face[2];
                int lastTexIndex = face[3];
                for (int p=4;p<face.length;p+=2) {
                    int pointIndex = face[p];
                    int texIndex = face[p+1];
                    facesArray[facesInd++] = firstPointIndex;
                    facesArray[facesInd++] = firstTexIndex;
                    facesArray[facesInd++] = lastPointIndex;
                    facesArray[facesInd++] = lastTexIndex;
                    facesArray[facesInd++] = pointIndex;
                    facesArray[facesInd++] = texIndex;
                    lastPointIndex = pointIndex;
                    lastTexIndex = texIndex;
                }
            }
            triangleMesh.getFaces().addAll(facesArray);
            if (DEBUG) System.out.println("numOfFacesBefore = " + numOfFacesBefore);
            if (DEBUG) System.out.println("numOfFacesAfter = " + numOfFacesAfter);
            // copy over texCoords
            triangleMesh.getTexCoords().addAll(pmesh.texCoords);
        }
        if (DEBUG) System.out.println("CREATING TRIANGLE MESH");
        if (DEBUG) System.out.println("    points    = "+Arrays.toString(triangleMesh.getPoints().toArray(null)));
        if (DEBUG) System.out.println("    texCoords = "+Arrays.toString(triangleMesh.getTexCoords().toArray(null)));
        if (DEBUG) System.out.println("    faces     = "+Arrays.toString(triangleMesh.getFaces().toArray(null)));
        meshView.setMesh(triangleMesh);
    }

    private float distanceBetweenPoints(float x1, float y1, float z1, float x2, float y2, float z2) {
        return (float)Math.sqrt(
                Math.pow(z2 - z1,2) +
                Math.pow(x2 - x1,2) +
                Math.pow(y2 - y1,2));
    }
}
