/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates.
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
package com.javafx.experiments.importers.max;

import com.javafx.experiments.importers.Importer;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PointLight;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Mesh;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.scene.transform.Transform;

/** Max ASCII file loader */
public class MaxLoader extends Importer {
    private static Mesh createMaxMesh(MaxData.GeomNode maxNode, Transform tm) {
        Transform tmr = null;
        try {
            tmr = tm != null ? tm.createInverse() : null;
        } catch (NonInvertibleTransformException ex) {
            throw new RuntimeException(ex);
        }
        float vts[] = new float[maxNode.mesh.nPoints * 3];
        if (tmr != null) {
            for (int i = 0; i < maxNode.mesh.nPoints; i++) {
                Point3D pt = tmr.transform(
                        maxNode.mesh.points[i * 3],
                        maxNode.mesh.points[i * 3 + 1],
                        maxNode.mesh.points[i * 3 + 2]);
                vts[i * 3] = (float) pt.getX();
                vts[i * 3 + 1] = (float) pt.getY();
                vts[i * 3 + 2] = (float) pt.getZ();
            }
        } else {
            for (int i = 0; i < maxNode.mesh.nPoints; i++) {
                vts[i * 3] = maxNode.mesh.points[i * 3];
                vts[i * 3 + 1] = maxNode.mesh.points[i * 3 + 1];
                vts[i * 3 + 2] = maxNode.mesh.points[i * 3 + 2];
            }
        }

        if ((maxNode.mesh.mapping != null) &&
                (maxNode.mesh.mapping[0].faces.length != maxNode.mesh.nFaces)) {
            //throw RuntimeException;
        }

        MaxData.MappingChannel mapping = maxNode.mesh.mapping[0];

        float uvs[] = new float[mapping.ntPoints * 2];
        for (int i = 0; i < mapping.ntPoints; i++) {
            uvs[i * 2] = mapping.tPoints[i * 2];
            uvs[i * 2 + 1] = 1.0f - mapping.tPoints[i * 2 + 1];
        }

        int faces[] = new int[maxNode.mesh.nFaces * 6];
        int sg[] = new int[maxNode.mesh.nFaces];
        for (int i = 0; i < maxNode.mesh.nFaces; i++) {
            int[] f = maxNode.mesh.faces;
            int[] mf = mapping.faces;
            faces[i * 6] = f[i * 4];
            faces[i * 6 + 1] = mf[i * 3];
            faces[i * 6 + 2] = f[i * 4 + 1];
            faces[i * 6 + 3] = mf[i * 3 + 1];
            faces[i * 6 + 4] = f[i * 4 + 2];
            faces[i * 6 + 5] = mf[i * 3 + 2];
            sg[i] = f[i * 4 + 3];
        }

        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().setAll(vts);
        mesh.getTexCoords().setAll(uvs);
        mesh.getFaces().setAll(faces);
        mesh.getFaceSmoothingGroups().setAll(sg);
        return mesh;
    }

    private static Color color(Point3D v) {
        return Color.color(v.getX(), v.getY(), v.getZ());
    }

    private static Transform loadNodeTM(MaxData.NodeTM tm) {
        return new Affine(
                tm.tm[0].getX(), tm.tm[1].getX(), tm.tm[2].getX(), tm.pos.getX(),
                tm.tm[0].getY(), tm.tm[1].getY(), tm.tm[2].getY(), tm.pos.getY(),
                tm.tm[0].getZ(), tm.tm[1].getZ(), tm.tm[2].getZ(), tm.pos.getZ());

    }

    private Material[] materials;

    private MeshView loadMaxMeshView(MaxData.GeomNode maxNode, MaxData maxData, Transform tm) {
        Mesh mesh = createMaxMesh(maxNode, tm);
        MeshView meshView = new MeshView();
        meshView.setMesh(mesh);
        meshView.setMaterial(materials[maxNode.materialRef]);
        // Color amb = color(maxData.materials[maxNode.materialRef].ambientColor);
        //meshView.setAmbient(amb);

        meshView.setUserData(maxNode.name);
        // meshView.setWireframe(true);
        return meshView;
    }

    public static String appendSuffix(String fileName, String suffix) {
        int dot = fileName.lastIndexOf('.');
        String ext = fileName.substring(dot, fileName.length());
        String name = fileName.substring(0, dot);
        String res = name + suffix + ext;
        return res;
    }

    public static String getSpecularTextureName(String diffName) {
        return appendSuffix(diffName, "_sp");
    }

    public static String getBumpTextureName(String diffName) {
        return appendSuffix(diffName, "_bp");
    }

    static Image loadImage(String fullName) {
        Image img = null;
        try {
            File f = new File(fullName);
            if (f.exists()) {
                String url = f.toURL().toString();
                img = new Image(url);
            } else {
                System.out.println("Texture file does not exist: " + fullName);
            }
        } catch (Exception ex) {
            System.out.println("Failed to load:" + fullName);
            ex.printStackTrace(System.out);
        }
        return img;
    }

    private void loadMaxMaterials(MaxData.Material mtls[], String dir) {
        materials = new Material[mtls.length];
        for (int i = 0; i < mtls.length; i++) {
            MaxData.Material m = mtls[i];
            PhongMaterial mtl = new PhongMaterial(
                    Color.color(
                            m.diffuseColor.getX(), m.diffuseColor.getY(), m.diffuseColor.getZ()));

            String fullName = dir + File.separatorChar + m.diffuseMap;
            Image diffuseMap = loadImage(fullName);
            Image specularMap = loadImage(getSpecularTextureName(fullName));
            Image bumpMap = loadImage(getBumpTextureName(fullName)); ;

            mtl.setDiffuseMap(diffuseMap);
            mtl.setSpecularMap(specularMap);
            mtl.setBumpMap(bumpMap);
            mtl.setDiffuseColor(Color.WHITE);
            materials[i] = mtl;
        }
    }

    private void loadMaxMaterialsUrl(MaxData.Material mtls[], String baseURl) {
        materials = new Material[mtls.length];
        for (int i = 0; i < mtls.length; i++) {
            MaxData.Material m = mtls[i];
            PhongMaterial mtl = new PhongMaterial(
                    Color.color(
                            m.diffuseColor.getX(), m.diffuseColor.getY(), m.diffuseColor.getZ()));

            String fullName = baseURl + m.diffuseMap;
            Image diffuseMap = new Image(fullName);
            Image specularMap = new Image(getSpecularTextureName(fullName));
            Image bumpMap = new Image(getBumpTextureName(fullName)); ;

            mtl.setDiffuseMap(diffuseMap);
            mtl.setSpecularMap(specularMap);
            mtl.setBumpMap(bumpMap);
            mtl.setDiffuseColor(Color.WHITE);
            materials[i] = mtl;
        }
    }

    PointLight loadLight(MaxData.LightNode ln, Transform ntm) {
        PointLight l = new PointLight();
        if (ntm != null) {
            l.setTranslateX(ntm.getTx());
            l.setTranslateY(ntm.getTy());
            l.setTranslateZ(ntm.getTz());
        }
        l.setColor(Color.color(ln.r, ln.g, ln.b));
        //        l.setStrength(ln.intensity);     // TODO
        return l;
    }

    private Node loadMaxNode(MaxData.Node maxNode, MaxData maxData) {
        Transform ntm = loadNodeTM(maxNode.nodeTM);

        //        Translate tm = new Translate(ntm.getTx(), ntm.getTy(), ntm.getTz());
        Group group = null;
        if (maxNode.children != null) {
            group = new Group();

            //            group.getTransforms().add(tm);
            for (MaxData.Node maxChild : maxNode.children) {
                Node child = loadMaxNode(maxChild, maxData);
                if (child != null) {
                    group.getChildren().add(child);
                }
            }
        }

        Node node = null;

        if (maxNode instanceof MaxData.GeomNode) {
            MaxData.GeomNode geomNode = (MaxData.GeomNode) maxNode;
            node = loadMaxMeshView(geomNode, maxData, null /* tm */);
        }

        if ((maxNode instanceof MaxData.LightNode)) {
            node = loadLight((MaxData.LightNode) maxNode, ntm);
        }

        if (group != null && node != null) {
            //          meshView.getTransforms().add(tm);
            group.getChildren().add(node);
        }

        return group != null ? group : node;
    }

    public static class MaxScene extends Group {
        public final Group geometry;
        public final Group lights;

        public MaxScene(Group geometry, Group lights) {
            this.geometry = geometry;
            this.lights = lights;
            getChildren().addAll(geometry, lights);
        }
    }

    public MaxScene loadMaxFile(File file) {
        MaxData maxData = null;
        try {
            maxData = new MaxAseParser(file.getPath()).data;
        } catch (FileNotFoundException ex) {
        } catch (IOException ex) {
        }

        String dir = file.getParent();
        System.out.println(dir);

        loadMaxMaterials(maxData.materials, dir);

        Group root = new Group();
        Group lroot = new Group();
        for (Map.Entry<String, MaxData.Node> n : maxData.roots.entrySet()) {
            Node node = loadMaxNode(n.getValue(), maxData);

            if (node instanceof PointLight) { // or LightBase ?
                lroot.getChildren().add(node);
            } else if (node != null) {
                root.getChildren().add(node);
            }
        }

        return new MaxScene(root, lroot);
    }

    public MaxScene loadMaxUrl(String fileUrl) throws IOException{
        MaxData maxData = new MaxAseParser(new URL(fileUrl).openStream()).data;

        String baseUrl = fileUrl.substring(0,fileUrl.lastIndexOf('/')+1);

        loadMaxMaterialsUrl(maxData.materials, baseUrl);

        Group root = new Group();
        Group lroot = new Group();
        for (Map.Entry<String, MaxData.Node> n : maxData.roots.entrySet()) {
            Node node = loadMaxNode(n.getValue(), maxData);

            if (node instanceof PointLight) { // or LightBase ?
                lroot.getChildren().add(node);
            } else if (node != null) {
                root.getChildren().add(node);
            }
        }

        return new MaxScene(root, lroot);
    }

    private MaxScene root;

    @Override
    public void load(String fileUrl, boolean asPolygonMesh) throws IOException {
        loadMaxUrl(fileUrl);
        if (asPolygonMesh) {
            throw new RuntimeException("Polygon Mesh is not supported");
        } else {
            root = loadMaxUrl(fileUrl);
        }
    }

    @Override
    public Group getRoot() {
        return root;
    }

    @Override
    public boolean isSupported(String extension) {
        return extension != null && extension.equals("ase");
    }

}
