
package com.javafx.experiments.shape3d;

import com.javafx.experiments.importers.maya.Joint;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.ObservableFloatArray;
import javafx.geometry.Point3D;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.scene.transform.Transform;

/**
 * PolygonMesh that updates itself when the joint transforms are updated.
 * Assumes that the dimensions of weights is nJoints x nPoints
 */
public class SkinningMesh extends PolygonMesh {
    private final Point3D[][] relativePoints; // nPoints x nJoints
    private final float[][] weights; // nPoints x nJoints
    private final List<Integer>[] weightIndices;
    private final List<Joint> joints;
    private final int nPoints;
    private final int nJoints;
    private Transform meshInverseTransform;

    public SkinningMesh(PolygonMesh mesh, Transform meshTransform, float[][] weights, Affine[] bindTransforms, List<Joint> joints) {
        this.getPoints().addAll(mesh.getPoints());
        this.getTexCoords().addAll(mesh.getTexCoords());
        this.faces = mesh.faces;
        
        this.weights = weights;
        this.joints = joints;
        
        nJoints = joints.size();
        nPoints = getPoints().size()/ TriangleMesh.NUM_COMPONENTS_PER_POINT;
        
        try {
            meshInverseTransform = meshTransform.createInverse();
        } catch (NonInvertibleTransformException ex) {
            System.err.println("Caught NonInvertibleTransformException: " + ex.getMessage());
        }
        
        weightIndices = new List[nPoints];
        for (int i = 0; i < nPoints; i++) {
            weightIndices[i] = new ArrayList<Integer>();
            for (int j = 0; j < nJoints; j++) {
                if (weights[i][j] != 0.0f) {
                    weightIndices[i].add(new Integer(j));
                }
            }
        }
        
        ObservableFloatArray points = getPoints();
        relativePoints = new Point3D[nPoints][nJoints];
        for (int j = 0; j < nJoints; j++) {
            Transform postBindTransform = bindTransforms[j].createConcatenation(meshTransform);
            for (int i = 0; i < nPoints; i++) {
                relativePoints[i][j] = postBindTransform.transform(points.get(3*i), points.get(3*i+1), points.get(3*i+2));
            }
        }
    }
    
    public void update() {
        Transform[] preJointTransforms = new Transform[nJoints];
        for (int j = 0; j < nJoints; j++) {
            preJointTransforms[j] = meshInverseTransform.createConcatenation(joints.get(j).getLocalToSceneTransform());
        }

        float[] points = new float [getPoints().size()];
        
        for (int i = 0; i < nPoints; i++) {
            if (!weightIndices[i].isEmpty()) {
                Point3D weightedPoint = new Point3D(0,0,0);
                for (Integer j : weightIndices[i]) {
                    Point3D absolutePoint = preJointTransforms[j].transform(relativePoints[i][j]);
                    weightedPoint = weightedPoint.add(absolutePoint.multiply(weights[i][j]));
                }
                points[3*i] = (float) weightedPoint.getX();
                points[3*i+1] = (float) weightedPoint.getY();
                points[3*i+2] = (float) weightedPoint.getZ();
            }
        }
        getPoints().set(0, points, 0, points.length);
        
//        // The following loop is equivalent to the one above, the difference
//        // being that this one is more straight-forward (it checks and skips
//        // the zero weights).
//        for (int i = 0; i < nPoints; i++) {
//            Point3D weightedPoint = new Point3D(0,0,0);
//            boolean isVertexInfluenced = false;
//            for (int j = 0; j < nJoints; j++) {
//                if (weights[i][j] != 0.0f) {
//                    isVertexInfluenced = true;
//                    Point3D absolutePoint = preJointTransforms[j].transform(relativePoints[i][j]);
//                    weightedPoint = weightedPoint.add(absolutePoint.multiply(weights[i][j]));
//                }
//            }
//            if (isVertexInfluenced) {
//                points[3*i] = (float) weightedPoint.getX();
//                points[3*i+1] = (float) weightedPoint.getY();
//                points[3*i+2] = (float) weightedPoint.getZ();
//            }
//        }
//        getPoints().set(0, points, 0, points.length);
        
    }
}
