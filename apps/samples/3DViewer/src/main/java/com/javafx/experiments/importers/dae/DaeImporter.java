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
package com.javafx.experiments.importers.dae;

import com.javafx.experiments.importers.Importer;
import javafx.geometry.Point3D;
import javafx.scene.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.*;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import com.javafx.experiments.shape3d.PolygonMesh;
import com.javafx.experiments.shape3d.PolygonMeshView;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.*;

/**
 * Loader for ".dae" 3D files
 *
 * Notes:
 *
 *  - Assume Y is up for now
 *  - Assume 1 Unit = 1 FX Unit
 */
@SuppressWarnings("UnusedDeclaration")
public class DaeImporter extends Importer {
    private Group rootNode = new Group();
    private Camera firstCamera = null;
    private double firstCameraAspectRatio = 4/3;
    private Map<String,Camera> cameras = new HashMap<>();
    private Map<String,Object> meshes = new HashMap<>();
    private  boolean createPolyMesh;

    {
        // CHANGE FOR Y_UP
        rootNode.getTransforms().add(new Rotate(180,0,0,0,Rotate.X_AXIS));
    }

    public DaeImporter() {
    }
    public DaeImporter(String url, boolean createPolyMesh) {
        load(url, createPolyMesh);
    }

    public DaeImporter(File file, boolean createPolyMesh) {
        this.createPolyMesh = createPolyMesh;
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(file, new DaeSaxParser());
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
    }

    public DaeImporter(InputStream in, boolean createPolyMesh) {
        this.createPolyMesh = createPolyMesh;
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(in, new DaeSaxParser());
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
    }

    public Scene createScene(int width) {
        Scene scene = new Scene(rootNode,width,(int)(width/firstCameraAspectRatio),true);
        if (firstCamera!=null) scene.setCamera(firstCamera);
        scene.setFill(Color.BEIGE);
        return scene;
    }

    public Camera getFirstCamera() {
        return firstCamera;
    }

    @Override
    public Group getRoot() {
        return rootNode;
    }

    @Override
    public void load(String url, boolean createPolygonMesh) {
    this.createPolyMesh = createPolygonMesh;
        long START = System.currentTimeMillis();
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(url, new DaeSaxParser());
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        long END = System.currentTimeMillis();
        System.out.println("IMPORTED ["+url+"] in  "+((END-START))+"ms");
    }

    @Override
    public boolean isSupported(String extension) {
        return extension != null && extension.equals("dae");
    }
    private static enum State {
        UNKNOWN,camera,visual_scene,node,aspect_ratio,xfov,yfov,znear,zfar,instance_camera,instance_geometry,
        translate,rotate,scale,matrix,float_array,polygons,input,p,vertices,authoring_tool,polylist,vcount}

    private static State state(String name) {
        try {
            return State.valueOf(name);
        } catch (Exception e) {
            return State.UNKNOWN;
        }
    }

    private class DaeSaxParser extends DefaultHandler {
        private State state = State.UNKNOWN;
        private String authoringTool;
        private LinkedList<DaeNode> nodes = new LinkedList<>();
        private StringBuilder charBuf;
        private Map<String,String> currentId = new HashMap<>();
        private Map<String,float[]> floatArrays = new HashMap<>();
        private Map<String,Input> inputs = new HashMap<>();
        private int[] vCounts;
        private List<int[]> pLists = new ArrayList<>();
        private List<Transform> currentTransforms;
        private Double xfov,yfov,znear,zfar,aspect_ratio;

        @Override public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            state = state(qName);
            currentId.put(qName,attributes.getValue("id"));
            charBuf = new StringBuilder();
            switch(state) {
                case camera:
                    aspect_ratio = xfov = yfov = znear = zfar = null;
                    break;
                case visual_scene:
                    rootNode.setId(attributes.getValue("name"));
                    DaeNode rootDaeNode = new DaeNode(attributes.getValue("id"),attributes.getValue("name"));
                    rootDaeNode.group = rootNode;
                    nodes.push(rootDaeNode);
                    break;
                case node:
                    currentTransforms = new ArrayList<>();
                    nodes.push(new DaeNode(attributes.getValue("id"),attributes.getValue("name")));
                    break;
                case instance_camera:
                    nodes.peek().instance_camera = cameras.get(attributes.getValue("url").substring(1));
                    break;
                case instance_geometry:
                    nodes.peek().instance_geometry = meshes.get(attributes.getValue("url").substring(1));
                    break;
                case polygons:
                case polylist:
                    inputs.clear();
                    pLists.clear();
                    break;
                case input:
                    Input input = new Input(
                            attributes.getValue("offset")!=null? Integer.parseInt(attributes.getValue("offset")): 0,
                            attributes.getValue("semantic"),
                            attributes.getValue("source"));
                    inputs.put(input.semantic,input);
                    break;
            }
        }

        @Override public void endElement(String uri, String localName, String qName) throws SAXException {
            switch(state(qName)) {
                case node:
                    DaeNode thisNode = nodes.pop();
                    if (thisNode.isCamera()) { // IS CAMERA
                        thisNode.instance_camera.setId(thisNode.name);
                        thisNode.instance_camera.getTransforms().addAll(currentTransforms);
                        nodes.peek().getGroup().getChildren().add(thisNode.instance_camera);
                    } else if (thisNode.isMesh()) { // IS MESH VIEW
                        Node meshView;
                        if (thisNode.instance_geometry instanceof PolygonMesh) {
                            meshView = new PolygonMeshView((PolygonMesh)thisNode.instance_geometry);
                        } else {
                            meshView = new MeshView((TriangleMesh)thisNode.instance_geometry);
                        }
                        meshView.setId(thisNode.name);
                        meshView.getTransforms().addAll(currentTransforms);
                        nodes.peek().getGroup().getChildren().add(meshView);
                    } else { // IS GROUP
                        Group group = thisNode.getGroup();
                        group.setId(thisNode.name);
                        group.getTransforms().addAll(currentTransforms);
                        nodes.peek().getGroup().getChildren().add(group);
                    }
                    break;
                case aspect_ratio:
                    aspect_ratio = Double.parseDouble(charBuf.toString().trim());
                    // workaround for Cheetah3D which seems to have it backwards
                    if ("Cheetah3D".equals(authoringTool)) aspect_ratio = 1/aspect_ratio;
                    break;
                case xfov:
                    yfov = Double.parseDouble(charBuf.toString().trim());
                    break;
                case yfov:
                    yfov = Double.parseDouble(charBuf.toString().trim());
                    break;
                case znear:
                    znear = Double.parseDouble(charBuf.toString().trim());
                    break;
                case zfar:
                    zfar = Double.parseDouble(charBuf.toString().trim());
                    break;
                case camera:
                    PerspectiveCamera camera = new PerspectiveCamera(true);
                    if (yfov != null) {
                        camera.setVerticalFieldOfView(true);
                        camera.setFieldOfView(yfov);
                    } else if (xfov != null) {
                        camera.setVerticalFieldOfView(false);
                        camera.setFieldOfView(xfov);
                    }
                    if (znear != null) camera.setNearClip(znear);
                    if (zfar != null) camera.setFarClip(zfar);
                    cameras.put(currentId.get("camera"), camera);
                    if (firstCamera==null) {
                        firstCamera = camera;
                        if (aspect_ratio != null) firstCameraAspectRatio = aspect_ratio;
                    }
                    break;
                case translate:
                    String[] tv = charBuf.toString().trim().split("\\s+");
                    currentTransforms.add(new Translate(
                        Double.parseDouble(tv[0].trim()),
                        Double.parseDouble(tv[1].trim()),
                        Double.parseDouble(tv[2].trim())
                    ));
                    break;
                case rotate:
                    String[] rv = charBuf.toString().trim().split("\\s+");
                    currentTransforms.add(new Rotate(
                        Double.parseDouble(rv[3].trim()),
                        0,0,0,
                        new Point3D(
                            Double.parseDouble(rv[0].trim()),
                            Double.parseDouble(rv[1].trim()),
                            Double.parseDouble(rv[2].trim())
                        )
                    ));
                    break;
                case scale:
                    String[] sv = charBuf.toString().trim().split("\\s+");
                    currentTransforms.add(new Scale(
                        Double.parseDouble(sv[0].trim()),
                        Double.parseDouble(sv[1].trim()),
                        Double.parseDouble(sv[2].trim()),
                        0,0,0
                    ));
                    break;
                case matrix:
                    String[] mv = charBuf.toString().trim().split("\\s+");
                    currentTransforms.add(new Affine(
                        Double.parseDouble(mv[0].trim()), // mxx
                        Double.parseDouble(mv[1].trim()), // mxy
                        Double.parseDouble(mv[2].trim()), // mxz
                        Double.parseDouble(mv[3].trim()), // tx
                        Double.parseDouble(mv[4].trim()), // myx
                        Double.parseDouble(mv[5].trim()), // myy
                        Double.parseDouble(mv[6].trim()), // myz
                        Double.parseDouble(mv[7].trim()), // ty
                        Double.parseDouble(mv[8].trim()), // mzx
                        Double.parseDouble(mv[9].trim()), // mzy
                        Double.parseDouble(mv[10].trim()), // mzz
                        Double.parseDouble(mv[11].trim()) // tz
                    ));
                    break;
                case float_array:
                    String[] numbers = charBuf.toString().trim().split("\\s+");
                    float[] array = new float[numbers.length];
                    for(int i=0;i<numbers.length;i++) {
                        array[i] = Float.parseFloat(numbers[i].trim());
                    }
                    floatArrays.put(currentId.get("source"), array);
                    break;
                case p:
                    numbers = charBuf.toString().trim().split("\\s+");
                    int[] iArray = new int[numbers.length];
                    for(int i=0;i<numbers.length;i++) {
                        iArray[i] = Integer.parseInt(numbers[i].trim());
                    }
                    pLists.add(iArray);
                    break;
                case polygons:
                    // create mesh put in map
                    if(createPolyMesh) {
                        Input vertexInput = inputs.get("VERTEX");
                        Input texInput = inputs.get("TEXCOORD");
                        float[] points = floatArrays.get(vertexInput.source.substring(1));
                        float[] texCoords = floatArrays.get(texInput.source.substring(1));
                        int[][] faces = new int[pLists.size()][];
                        for(int f=0;f<faces.length;f++) {
                            faces[f] = pLists.get(f);
                        }
                        PolygonMesh mesh = new PolygonMesh(points,texCoords,faces);
                        meshes.put(currentId.get("geometry"),mesh);
                    } else {
                        TriangleMesh mesh = new TriangleMesh();
                        meshes.put(currentId.get("geometry"),mesh);
                        throw new UnsupportedOperationException("Need to implement TriangleMesh creation");
                    }
                    break;
                case vertices:
                    // put vertex float into map again with new ID
                    String sourceId = inputs.get("POSITION").source.substring(1);
                    float[] points = floatArrays.get(sourceId);
                    floatArrays.put(
                            currentId.get("vertices"),
                            points);
                    break;
                case authoring_tool:
                    authoringTool = charBuf.toString().trim();
                    break;
                case vcount:
                    numbers = charBuf.toString().trim().split("\\s+");
                    vCounts = new int[numbers.length];
                    for(int i=0;i<numbers.length;i++) {
                        vCounts[i] = Integer.parseInt(numbers[i].trim());
                    }
                    break;
                case polylist:
                    // create mesh put in map
                    if(createPolyMesh) {
                        int faceStep = 1;
                        Input vertexInput = inputs.get("VERTEX");
                        if (vertexInput!=null && (vertexInput.offset+1) > faceStep) faceStep = vertexInput.offset+1;
                        Input texInput = inputs.get("TEXCOORD");
                        if (texInput!=null && (texInput.offset+1) > faceStep) faceStep = texInput.offset+1;
                        Input normalInput = inputs.get("NORMAL");
                        if (normalInput!=null && (normalInput.offset+1) > faceStep) faceStep = normalInput.offset+1;
                        points = floatArrays.get(vertexInput.source.substring(1));
                        float[] texCoords;
                        if (texInput==null) {
                            texCoords = new float[]{0,0};
                        } else {
                            texCoords = floatArrays.get(texInput.source.substring(1));
                        }
                        int[][] faces = new int[vCounts.length][];
                        int[] p = pLists.get(0);
                        int faceIndex = 0;
                        for(int f=0;f<faces.length;f++) {
                            final int numOfVertex = vCounts[f];
                            final int[] face = new int[numOfVertex*2];
                            for(int v=0;v<numOfVertex;v++) {
                                final int vertexIndex =faceIndex+(v*faceStep);
                                face[v*2] = p[vertexIndex+vertexInput.offset];
                                face[(v*2)+1] = (texInput==null) ? 0 : p[vertexIndex+texInput.offset];
                            }
                            faces[f] = face;
                            faceIndex += numOfVertex*faceStep;
                        }
                        PolygonMesh mesh = new PolygonMesh(points,texCoords,faces);
                        meshes.put(currentId.get("geometry"),mesh);
                    } else {
                        TriangleMesh mesh = new TriangleMesh();
                        meshes.put(currentId.get("geometry"),mesh);
                        throw new UnsupportedOperationException("Need to implement TriangleMesh creation");
                    }
                    break;
            }
        }

        @Override public void characters(char[] ch, int start, int length) throws SAXException {
            charBuf.append(ch,start,length);
        }
    }

    private static final class Input {
        public final int offset;
        public final String semantic;
        public final String source;

        private Input(int offset, String semantic, String source) {
            this.offset = offset;
            this.semantic = semantic;
            this.source = source;
        }

        @Override public String toString() {
            return "Input{" +
                    "offset=" + offset +
                    ", semantic='" + semantic + '\'' +
                    ", source='" + source + '\'' +
                    '}';
        }
    }

    /** So far a node can be one of camera, geometry or group */
    private static final class DaeNode {
        private final String id;
        private final String name;
        private Camera instance_camera;
        private Object instance_geometry;
        private Group group;

        private DaeNode(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public boolean isCamera() {
            return instance_camera !=null;
        }

        public boolean isMesh() {
            return instance_geometry !=null;
        }

        public boolean isGroup() {
            return instance_camera == null && instance_geometry == null;
        }

        public Group getGroup() {
            if(group ==null) group = new Group();
            return group;
        }

        @Override public String toString() {
            return "DaeNode{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", instance_camera=" + instance_camera +
                    ", instance_geometry=" + instance_geometry +
                    ", group=" + group +
                    '}';
        }
    }

}
