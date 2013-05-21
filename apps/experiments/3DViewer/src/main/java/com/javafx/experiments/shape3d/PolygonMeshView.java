package com.javafx.experiments.shape3d;

import java.util.Arrays;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
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
    public void setMesh(PolygonMesh mesh) { this.mesh.set(mesh); }
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
    private SimpleIntegerProperty subdivision = new SimpleIntegerProperty(0) {
        @Override protected void invalidated() {
            updateSubdivision();
        }
    };
    public int getSubdivision() { return subdivision.get(); }
    public SimpleIntegerProperty subdivisionProperty() { return subdivision; }
    public void setSubdivision(int subdivision) { this.subdivision.set(subdivision); }

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
        final int iterations = subdivision.get();
        if (iterations == 0) {
            subdividedMesh = null;
        } else {
            subdividedMesh = getMesh();
            for (int i=0; i<iterations; i++) {
                subdividedMesh = Subdivision.subdivide(subdividedMesh);
            }
        }
        updateMesh();
    }

    private void updateMesh() {
        PolygonMesh pmesh = subdividedMesh != null ? subdividedMesh : getMesh();
        if (pmesh == null || pmesh.points == null || pmesh.faces == null || pmesh.texCoords == null) {
            meshView.setMesh(null);
            return;
        }
        final boolean isWireframe = getDrawMode() == DrawMode.LINE;
        if (DEBUG) System.out.println("UPDATE MESH -- "+(isWireframe?"WIREFRAME":"SOLID"));
        final TriangleMesh triangleMesh = new TriangleMesh();
        // get triangle mesh points
        final ObservableFloatArray points =  triangleMesh.getPoints();
        // copy over points
        final int numOfPoints = pmesh.points.length/TriangleMesh.NUM_COMPONENTS_PER_POINT;
        if (DEBUG) System.out.println("numOfPoints = " + numOfPoints);
        points.addAll(pmesh.points);

        if(isWireframe) {
            // create faces and add point for each edge
            ObservableIntegerArray faces = triangleMesh.getFaces();
            for(int[] face: pmesh.faces) {
                if (DEBUG) System.out.println("face.length = " + (face.length/2)+"  -- "+Arrays.toString(face));
                int lastPointIndex = face[face.length-2];
                if (DEBUG) System.out.println("    lastPointIndex = " + lastPointIndex);
                for (int p=0;p<face.length;p+=2) {
                    int pointIndex = face[p];
                    if (DEBUG) System.out.println("        connecting point["+lastPointIndex+"] to point[" + pointIndex+"]");
                    faces.addAll(lastPointIndex,0,pointIndex,0,points.size()/TriangleMesh.NUM_COMPONENTS_PER_POINT,0);
                    final int numOfPoints2 = points.size()/TriangleMesh.NUM_COMPONENTS_PER_POINT;
                    if (DEBUG) System.out.println("            numOfPoints = " + numOfPoints2);
                    // get start and end point
                    final float x1 = points.get(lastPointIndex*TriangleMesh.NUM_COMPONENTS_PER_POINT);
                    final float y1 = points.get((lastPointIndex*TriangleMesh.NUM_COMPONENTS_PER_POINT)+1);
                    final float z1 = points.get((lastPointIndex*TriangleMesh.NUM_COMPONENTS_PER_POINT)+2);
                    final float x2 = points.get(pointIndex*TriangleMesh.NUM_COMPONENTS_PER_POINT);
                    final float y2 = points.get((pointIndex*TriangleMesh.NUM_COMPONENTS_PER_POINT)+1);
                    final float z2 = points.get((pointIndex*TriangleMesh.NUM_COMPONENTS_PER_POINT)+2);
                    final float distance = Math.abs(distanceBetweenPoints(x1,y1,z1,x2,y2,z2));
                    final float offset = distance/1000;
                    // add new point
                    points.addAll(
                            points.get(pointIndex*TriangleMesh.NUM_COMPONENTS_PER_POINT)+offset,
                            points.get((pointIndex*TriangleMesh.NUM_COMPONENTS_PER_POINT)+1)+offset,
                            points.get((pointIndex*TriangleMesh.NUM_COMPONENTS_PER_POINT)+2)+offset);
                    if (DEBUG) System.out.println("            faces.size() = " + faces.size());
                    lastPointIndex = pointIndex;
                }
            }
            final int numOfFacesBefore = pmesh.faces.length;
            if (DEBUG) System.out.println("numOfFacesBefore = " + numOfFacesBefore);
            final int numOfFacesAfter = faces.size()/TriangleMesh.NUM_COMPONENTS_PER_FACE;
            if (DEBUG) System.out.println("numOfFacesAfter = " + numOfFacesAfter);

            // set simple texCoords for wireframe
            triangleMesh.getTexCoords().addAll(0,0);
        } else {
            // create faces and break into triangles
            ObservableIntegerArray faces = triangleMesh.getFaces();
            for(int[] face: pmesh.faces) {
                if (DEBUG) System.out.println("face.length = " + face.length+"  -- "+Arrays.toString(face));
                int firstPointIndex = face[0];
                int firstTexIndex = face[1];
                int lastPointIndex = face[2];
                int lastTexIndex = face[3];
                for (int p=4;p<face.length;p+=2) {
                    int pointIndex = face[p];
                    int texIndex = face[p+1];
                    faces.addAll(
                            firstPointIndex,
                            firstTexIndex,
                            lastPointIndex,
                            lastTexIndex,
                            pointIndex,
                            texIndex);
//                    if (DEBUG) System.out.println("        faces.size() = " + faces.size());
                    lastPointIndex = pointIndex;
                    lastTexIndex = texIndex;
                }
            }
            final int numOfFacesBefore = pmesh.faces.length;
            if (DEBUG) System.out.println("numOfFacesBefore = " + numOfFacesBefore);
            final int numOfFacesAfter = faces.size()/TriangleMesh.NUM_COMPONENTS_PER_FACE;
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
