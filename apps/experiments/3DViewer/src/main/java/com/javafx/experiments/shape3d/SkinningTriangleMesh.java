
package com.javafx.experiments.shape3d;

import com.javafx.experiments.importers.maya.Joint;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.ObservableFloatArray;
import javafx.geometry.Point3D;
import javafx.scene.shape.TriangleMesh;
import static javafx.scene.shape.TriangleMesh.NUM_COMPONENTS_PER_POINT;
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.scene.transform.Transform;

/**
 * TriangleMesh that updates itself when the joint transforms are updated.
 * Assumes that the dimensions of weights is nJoints x nPoints
 */
public class SkinningTriangleMesh extends TriangleMesh {
    private final Point3D[][] relativePoints; // nPoints x nJoints
    private final float[][] weights; // nPoints x nJoints
    private final List<Integer>[] weightIndices;
    private final List<Joint> joints;
    private final int nPoints;
    private final int nJoints;
    private Transform meshInverseTransform;

    public SkinningTriangleMesh(TriangleMesh mesh, Transform meshTransform, float[][] weights, Affine[] bindTransforms, List<Joint> joints) {
        this.getPoints().setAll(mesh.getPoints());
        this.getTexCoords().setAll(mesh.getTexCoords());
        this.getFaces().setAll(mesh.getFaces());
        this.getFaceSmoothingGroups().setAll(mesh.getFaceSmoothingGroups());
        
        this.weights = weights;
        this.joints = joints;
        
        nJoints = joints.size();
        nPoints = getPoints().size()/NUM_COMPONENTS_PER_POINT;
        
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
        ObservableFloatArray points = getPoints();

        Transform[] preJointTransforms = new Transform[nJoints];
        for (int j = 0; j < nJoints; j++) {
            preJointTransforms[j] = meshInverseTransform.createConcatenation(joints.get(j).getLocalToSceneTransform());
        }

        float [] weightedPointArray = new float [3];
        for (int i = 0; i < nPoints; i++) {
            if (!weightIndices[i].isEmpty()) {
                Point3D weightedPoint = new Point3D(0,0,0);
                for (Integer j : weightIndices[i]) {
                    Point3D absolutePoint = preJointTransforms[j].transform(relativePoints[i][j]);
                    weightedPoint = weightedPoint.add(absolutePoint.multiply(weights[i][j]));
                }
                weightedPointArray[0] = (float) weightedPoint.getX();
                weightedPointArray[1] = (float) weightedPoint.getY();
                weightedPointArray[2] = (float) weightedPoint.getZ();
                points.set(3*i, weightedPointArray, 0, 3);
            }
        }
        
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
//                points.set(3*i, new float [] {(float) weightedPoint.getX(), (float) weightedPoint.getY(), (float) weightedPoint.getZ()}, 0, 3);
//            }
//        }
        
    }
}
