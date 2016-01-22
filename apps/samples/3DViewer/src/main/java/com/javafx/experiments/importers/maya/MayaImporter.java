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
package com.javafx.experiments.importers.maya;

import com.javafx.experiments.importers.Importer;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.DepthTest;
import javafx.scene.Node;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
 * MayaImporter
 * <p/>
 * MayaImporter.getRoot() returns a JavaFX node hierarchy MayaImporter.getTimeline() returns a JavaFX timeline
 */
public class MayaImporter extends Importer {
    public static final boolean DEBUG = Loader.DEBUG;
    public static final boolean WARN = Loader.WARN;

    // NO_JOINTS
    // [Note to Alex]: I've re-enabled joints, but lets not use rootCharacter [John]
    // javafx.scene.shape3d.Character rootCharacter = new javafx.scene.shape3d.Character();
    MayaGroup root = new MayaGroup();
    Timeline timeline;
    Set<Node> meshParents = new HashSet();

    // NO_JOINTS
    // [Note to Alex]: I've re-enabled joints, but lets not use rootCharacter [John]
    // public javafx.scene.shape3d.Character getRootCharacter() {
    //        return rootCharacter;
    // }

    @Override
    public MayaGroup getRoot() {
        return root;
    }

    //=========================================================================
    // MayaImporter.getTimeline
    //-------------------------------------------------------------------------
    // MayaImporter.getTimeline() returns a JavaFX timeline
    // (javafx.animation.Timeline)
    //=========================================================================
    @Override
    public Timeline getTimeline() {
        return timeline;
    }

    //=========================================================================
    // MayaImporter.getMeshParents
    //=========================================================================
    public Set<Node> getMeshParents() {
        return meshParents;
    }

    //=========================================================================
    // MayaImporter.load
    //=========================================================================
    @Override
    public void load(String url, boolean asPolygonMesh) {
        try {
            Loader loader = new Loader();
            loader.load(new java.net.URL(url), asPolygonMesh);

            // This root is not automatically added to the scene.
            // It needs to be added by the user of MayaImporter.
            //            root = new Xform();

            // Add top level nodes to the root
            int nodeCount = 0;
            for (Node n : loader.loaded.values()) {
                if (n != null) {
                    // Only add a node if it has no parents, ie. top level node
                    if (n.getParent() == null) {
                        if (Loader.DEBUG) {
                            System.out.println("Adding top level node " + n.getId() + " to root!");
                        }
                        n.setDepthTest(DepthTest.ENABLE);
                        if (!(n instanceof MeshView) || ((TriangleMesh)((MeshView)n).getMesh()).getPoints().size() > 0) {
                            root.getChildren().add(n);
                        }
                    }
                    nodeCount++;
                }
            }
            // [Note to Alex]: I've re-enabled joints, but lets not use rootCharacter [John]
            // rootCharacter.setRootJoint(loader.rootJoint);
            if (Loader.DEBUG) { System.out.println("There are " + nodeCount + " nodes."); }

            // if meshes were not loaded in the code above
            // (which they now are) one would need to
            // set meshParents from the loader
            // meshParents.addAll(loader.meshParents.keySet());
            // this is not necessary at the moment

            timeline = new Timeline();
            int count = 0;

            // Add all the keyframes to the timeline from loader.keyFrameMap
            for (final Map.Entry<Float, List<KeyValue>> e : loader.keyFrameMap.entrySet()) {
                // if (DEBUG) System.out.println("key frame at : "+ e.getKey());
                timeline.getKeyFrames().add
                        (
                                new KeyFrame(
                                        javafx.util.Duration.millis(e.getKey() * 1000f),
                                        (KeyValue[]) e.getValue().toArray(new KeyValue[e.getValue().size()])));
                count++;
            }

            if (Loader.DEBUG) { System.out.println("Loaded " + count + " key frames."); }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isSupported(String extension) {
        return extension != null && extension.equals("ma");
    }
}
