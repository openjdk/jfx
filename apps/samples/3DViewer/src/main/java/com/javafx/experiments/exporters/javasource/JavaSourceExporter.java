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
package com.javafx.experiments.exporters.javasource;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.value.WritableValue;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import com.javafx.experiments.utils3d.animation.TickCalculation;
import com.javafx.experiments.utils3d.animation.NumberTangentInterpolator;
import com.javafx.experiments.utils3d.animation.SplineInterpolator;

/**
 * A exporter for 3D Models and animations that creates a Java Source file.
 */
public class JavaSourceExporter {
    private int nodeCount = 0;
    private int materialCount = 0;
    private int meshCount = 0;
    private int meshViewCount = 0;
    private int methodCount = 1;
    private int translateCount = 0;
    private int rotateCount = 0;
    private int scaleCount = 0;
    private Map<WritableValue,String> writableVarMap = new HashMap<>();
    private StringBuilder nodeCode = new StringBuilder();
    private StringBuilder timelineCode = new StringBuilder();
    private final boolean hasTimeline;
    private final String baseUrl;
    private final String className;
    private final String packageName;
    private final File outputFile;

    public JavaSourceExporter(String baseUrl, Node rootNode, Timeline timeline, File outputFile) {
        this(baseUrl, rootNode, timeline,computePackageName(baseUrl), outputFile);
    }

    public JavaSourceExporter(String baseUrl, Node rootNode, Timeline timeline, String packageName, File outputFile) {
        this.baseUrl =
                (baseUrl.charAt(baseUrl.length()-1) == '/') ?
                        baseUrl.replaceAll("/+","/") :
                        baseUrl.replaceAll("/+","/") + '/';
        this.hasTimeline = timeline != null;
        this.className = outputFile.getName().substring(0,outputFile.getName().lastIndexOf('.'));
        this.packageName = packageName;
        this.outputFile = outputFile;
        process("        ",rootNode);
        if (hasTimeline) process("        ",timeline);
    }

    private static String computePackageName(String baseUrl) {
        // remove protocol from baseUrl
        System.out.println("JavaSourceExporter.computePackageName   baseUrl = " + baseUrl);
        baseUrl = baseUrl.replaceAll("^[a-z]+:/+","/");
        System.out.println("baseUrl = " + baseUrl);
        // try and work out package name from file
        StringBuilder packageName = new StringBuilder();
        String[] pathSegments = baseUrl.split(File.separatorChar == '\\'?"\\\\":"/");
        System.out.println("pathSegments = " + Arrays.toString(pathSegments));
        loop: for (int i = pathSegments.length-1; i >= 0; i -- ) {
            switch (pathSegments[i]){
                case "com":
                case "org":
                case "net":
                    packageName.insert(0,pathSegments[i]);
                    break loop;
                case "src":
                    packageName.deleteCharAt(0);
                    break loop;
                case "main":
                    if ("src".equals(pathSegments[i-1])) {
                        packageName.deleteCharAt(0);
                        break loop;
                    }
                default:
                    packageName.insert(0,'.'+pathSegments[i]);
                    break;
            }
            // if we get all way to root of file system then we have failed
            if (i==0) packageName = null;
        }
        System.out.println(" packageName = " + packageName);
        return packageName==null?null:packageName.toString();
    }

    public void export() {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(outputFile));
//            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(System.out));
            StringBuilder nodeVars = new StringBuilder();
            nodeVars.append("    private static Node ");
            for (int i=0; i<nodeCount; i++) {
                if (i!=0) nodeVars.append(',');
                nodeVars.append("NODE_"+i);
            }
            nodeVars.append(";\n");
            nodeVars.append("    private static TriangleMesh ");
            for (int i=0; i<meshCount; i++) {
                if (i!=0) nodeVars.append(',');
                nodeVars.append("MESH_"+i);
            }
            nodeVars.append(";\n");
            if (translateCount > 0) {
                nodeVars.append("    private static Translate ");
                for (int i=0; i<translateCount; i++) {
                    if (i!=0) nodeVars.append(',');
                    nodeVars.append("TRANS_"+i);
                }
                nodeVars.append(";\n");
            }
            if (rotateCount > 0) {
                nodeVars.append("    private static Rotate ");
                for (int i=0; i<rotateCount; i++) {
                    if (i!=0) nodeVars.append(',');
                    nodeVars.append("ROT_"+i);
                }
                nodeVars.append(";\n");
            }
            if (scaleCount > 0) {
                nodeVars.append("    private static Scale ");
                for (int i=0; i<scaleCount; i++) {
                    if (i!=0) nodeVars.append(',');
                    nodeVars.append("SCALE_"+i);
                }
                nodeVars.append(";\n");
            }
            nodeVars.append("    public static final MeshView[] MESHVIEWS = new MeshView[]{ ");
            for (int i=0; i<meshViewCount; i++) {
                if (i!=0) nodeVars.append(',');
                nodeVars.append("new MeshView()");
            }
            nodeVars.append("};\n");
//            nodeVars.append("    public static final MeshView ");
//            for (int i=0; i<meshViewCount; i++) {
//                if (i!=0) nodeVars.append(',');
//                nodeVars.append("MESHVIEW_"+i+" = new MeshView()");
//            }
//            nodeVars.append(";\n");

            nodeVars.append("    public static final Node ROOT;\n");
            nodeVars.append("    public static final Map<String,MeshView> MESHVIEW_MAP;\n");
            if (hasTimeline) {
                nodeVars.append("    public static final Timeline TIMELINE = new Timeline();\n");
            }
            StringBuilder methodCode = new StringBuilder();
            for (int m=0; m< methodCount;m++) {
                methodCode.append("        method"+m+"();\n");
            }

            if (packageName != null) out.write(
                    "package "+packageName+";\n\n");
            out.write(
                    "import java.util.*;\n" +
                    "import javafx.util.Duration;\n" +
                    "import javafx.animation.*;\n" +
                    "import javafx.scene.*;\n" +
                    "import javafx.scene.paint.*;\n" +
                    "import javafx.scene.image.*;\n" +
                    "import javafx.scene.shape.*;\n" +
                    "import javafx.scene.transform.*;\n\n" +
                    "public class "+className+" {\n" +
                    nodeVars +
                    "    // ======== NODE CODE ===============\n" +
                    "    private static void method0(){\n" +
                    nodeCode +
                    "    }\n" +
                    "    static {\n" +
                    methodCode +
                    "        // ======== TIMELINE CODE ===============\n" +
                    timelineCode +
                    "        // ======== SET PUBLIC VARS ===============\n" +
                    "        Map<String,MeshView> meshViewMap = new HashMap<String,MeshView>();\n" +
                    "        for (MeshView meshView: MESHVIEWS) meshViewMap.put(meshView.getId(),meshView);\n" +
                    "        MESHVIEW_MAP = Collections.unmodifiableMap(meshViewMap);\n" +
                    "        ROOT = NODE_0;\n" +
                    "    }\n" +
                    "}\n");

            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int process(String indent, Node node) {
        if (node instanceof MeshView) {
            return process(indent,(MeshView)node);
        } else if (node instanceof Group) {
            return process(indent,(Group)node);
        } else {
            throw new UnsupportedOperationException("Found unknown node type: "+node.getClass().getName());
        }
    }

    private int process(String indent, MeshView node) {
        final int index = nodeCount ++;
        final String varName = "NODE_"+index;
        final int meshViewIndex = meshViewCount ++;
        final String meshViewVarName = "MESHVIEWS["+meshViewIndex+"]";
//        nodeCode.append(indent+meshViewVarName+" = new MeshView();\n");
        nodeCode.append(indent+varName+" = "+meshViewVarName+";\n");
        if (node.getId() != null) nodeCode.append(indent+meshViewVarName+".setId(\""+node.getId()+"\");\n");
        nodeCode.append(indent+meshViewVarName+".setCullFace(CullFace."+node.getCullFace().name()+");\n");
        processNodeTransforms(indent,meshViewVarName,node);
        process(indent,meshViewVarName,(PhongMaterial)node.getMaterial());
        process(indent,meshViewVarName,(TriangleMesh)node.getMesh());
        return index;
    }

    private void processNodeTransforms(String indent, String varName, Node node) {
        if (node.getTranslateX() != 0) nodeCode.append(indent+varName+".setTranslateX("+node.getTranslateX()+");\n");
        if (node.getTranslateY() != 0) nodeCode.append(indent+varName+".setTranslateY("+node.getTranslateY()+");\n");
        if (node.getTranslateZ() != 0) nodeCode.append(indent+varName+".setTranslateZ("+node.getTranslateZ()+");\n");
        if (node.getRotate() != 0) nodeCode.append(indent+varName+".setRotate("+node.getRotate()+");\n");
        if (!node.getTransforms().isEmpty()) {
            nodeCode.append(indent+varName+".getTransforms().addAll(\n");
            for (int i=0; i< node.getTransforms().size(); i++) {
                if (i!=0) nodeCode.append(",\n");
                Transform transform = node.getTransforms().get(i);
                if (transform instanceof Translate) {
                    Translate t = (Translate)transform;
                    nodeCode.append(indent+"    "+storeTransform(t)+" = new Translate("+t.getX()+","+t.getY()+","+t.getZ()+")");
                } else if (transform instanceof Scale) {
                    Scale s = (Scale)transform;
                    nodeCode.append(indent+"    "+storeTransform(s)+" = new Scale("+s.getX()+","+s.getY()+","+s.getZ()+","+s.getPivotX()+","+s.getPivotY()+","+s.getPivotZ()+")");
                } else if (transform instanceof Rotate) {
                    Rotate r = (Rotate)transform;
                    nodeCode.append(indent+"    "+storeTransform(r)+" = new Rotate("+r.getAngle()+","+r.getPivotX()+","+r.getPivotY()+","+r.getPivotZ()+",");
                    if (r.getAxis() == Rotate.X_AXIS) {
                        nodeCode.append("Rotate.X_AXIS");
                    } else if (r.getAxis() == Rotate.Y_AXIS) {
                        nodeCode.append("Rotate.Y_AXIS");
                    } else if (r.getAxis() == Rotate.Z_AXIS) {
                        nodeCode.append("Rotate.Z_AXIS");
                    }
                    nodeCode.append(")");
                } else {
                    throw new UnsupportedOperationException("Unknown Transform Type: "+transform.getClass());
                }

            }
            nodeCode.append("\n"+indent+");\n");
        }
    }

    private String storeTransform(Transform transform) {
        String varName;
        if (transform instanceof Translate) {
            final int index = translateCount ++;
            varName = "TRANS_"+index;
            Translate t = (Translate)transform;
            writableVarMap.put(t.xProperty(),varName+".xProperty()");
            writableVarMap.put(t.yProperty(),varName+".yProperty()");
            writableVarMap.put(t.zProperty(),varName+".zProperty()");
        } else if (transform instanceof Scale) {
            final int index = scaleCount ++;
            varName = "SCALE_"+index;
            Scale s = (Scale)transform;
            writableVarMap.put(s.xProperty(),varName+".xProperty()");
            writableVarMap.put(s.yProperty(),varName+".yProperty()");
            writableVarMap.put(s.zProperty(),varName+".zProperty()");
            writableVarMap.put(s.pivotXProperty(),varName+".pivotXProperty()");
            writableVarMap.put(s.pivotYProperty(),varName+".pivotYProperty()");
            writableVarMap.put(s.pivotZProperty(),varName+".pivotZProperty()");
        } else if (transform instanceof Rotate) {
            final int index = rotateCount ++;
            varName = "ROT_"+index;
            Rotate r = (Rotate)transform;
            writableVarMap.put(r.angleProperty(),varName+".angleProperty()");
        } else {
            throw new UnsupportedOperationException("Unknown Transform Type: "+transform.getClass());
        }
        return varName;
    }

    private int process(String indent, Group node) {
        final int index = nodeCount ++;
        final String varName = "NODE_"+index;
        List<Integer> childIndex = new ArrayList<>();
        for (int i = 0; i < node.getChildren().size(); i++) {
            Node child = node.getChildren().get(i);
            childIndex.add(
                    process(indent,child));
        }
        nodeCode.append(indent+varName+" = new Group(");
        for (int i = 0; i < childIndex.size(); i++) {
            if (i!=0) nodeCode.append(',');
            nodeCode.append("NODE_"+childIndex.get(i));
        }
        nodeCode.append(");\n");
        processNodeTransforms(indent, varName, node);
        nodeCode.append("    }\n    private static void method" + (methodCount++) + "(){\n");
        return index;
    }

    private void process(String indent, String varName, TriangleMesh mesh) {
        final int index = meshCount ++;
        final String meshName = "MESH_"+index;

        nodeCode.append(indent+meshName+" = new TriangleMesh();\n");
        nodeCode.append(indent+varName+".setMesh("+meshName+");\n");

        nodeCode.append(indent+meshName+".getPoints().ensureCapacity("+mesh.getPoints().size()+");\n");
        nodeCode.append(indent + meshName + ".getPoints().addAll(");
        for (int i = 0; i < mesh.getPoints().size(); i++) {
            if (i!=0) nodeCode.append(',');
            nodeCode.append(mesh.getPoints().get(i)+"f");
        }
        nodeCode.append(");\n");
        nodeCode.append("    }\n    private static void method" + (methodCount++) + "(){\n");
        nodeCode.append(indent+meshName+".getTexCoords().ensureCapacity("+mesh.getTexCoords().size()+");\n");
        nodeCode.append(indent + meshName + ".getTexCoords().addAll(");
        for (int i = 0; i < mesh.getTexCoords().size(); i++) {
            if (i!=0) nodeCode.append(',');
            nodeCode.append(mesh.getTexCoords().get(i)+"f");
        }
        nodeCode.append(");\n");
        nodeCode.append("    }\n    private static void method" + (methodCount++) + "(){\n");
        nodeCode.append(indent+meshName+".getFaces().ensureCapacity("+mesh.getFaces().size()+");\n");
        nodeCode.append(indent + meshName + ".getFaces().addAll(");
        for (int i = 0; i < mesh.getFaces().size(); i++) {
            if ((i%5000) == 0 && i > 0) {
                nodeCode.append(");\n");
                nodeCode.append("    }\n    private static void method" + (methodCount++) + "(){\n");
                nodeCode.append(indent+meshName+".getFaces().addAll(");
            } else if (i!=0) {
                nodeCode.append(',');
            }
            nodeCode.append(mesh.getFaces().get(i));
        }
        nodeCode.append(");\n");
        nodeCode.append("    }\n    private static void method" + (methodCount++) + "(){\n");
        nodeCode.append(indent+meshName+".getFaceSmoothingGroups().ensureCapacity("+mesh.getFaceSmoothingGroups().size()+");\n");
        nodeCode.append(indent+meshName+".getFaceSmoothingGroups().addAll(");
        for (int i = 0; i < mesh.getFaceSmoothingGroups().size(); i++) {
            if (i!=0) nodeCode.append(',');
            nodeCode.append(mesh.getFaceSmoothingGroups().get(i));
        }
        nodeCode.append(");\n");

//        nodeCode.append(indent+varName+".setMesh("+process((TriangleMesh)node.getMesh())+");\n");
    }

    private void process(String indent, String varName, PhongMaterial material) {
        final int index = materialCount ++;
        final String materialName = "MATERIAL_"+index;

        nodeCode.append(indent + "PhongMaterial " + materialName + " = new PhongMaterial();\n");
        nodeCode.append(indent + materialName + ".setDiffuseColor(" + toCode(material.getDiffuseColor()) + ");\n");
        String specColor = toCode(material.getSpecularColor());
        if (specColor!=null) nodeCode.append(indent + materialName + ".setSpecularColor(" + specColor + ");\n");
        nodeCode.append(indent + materialName + ".setSpecularPower("+material.getSpecularPower()+");\n");
        if (material.getDiffuseMap() != null) {
            nodeCode.append(indent + "try {\n");
            nodeCode.append(indent + "    " + materialName + ".setDiffuseMap("+toString(material.getDiffuseMap())+");\n");
            nodeCode.append(indent + "} catch (NullPointerException npe) {\n");
            nodeCode.append(indent + "    System.err.println(\"Could not load texture resource ["+material.getDiffuseMap().impl_getUrl()+"]\");\n");
            nodeCode.append(indent + "}\n");
        }
        if (material.getBumpMap() != null) {
            nodeCode.append(indent + materialName + ".setBumpMap("+toString(material.getBumpMap())+");\n");
        }
        if (material.getSpecularMap() != null) {
            nodeCode.append(indent + materialName + ".setSpecularMap()("+toString(material.getSpecularMap())+");\n");
        }
        if (material.getSelfIlluminationMap() != null) {
            nodeCode.append(indent + materialName + ".setSelfIlluminationMap()("+toString(material.getSelfIlluminationMap())+");\n");
        }
        nodeCode.append(indent+varName+".setMaterial("+materialName+");\n");
    }

    private String toString(Image image) {
        String url = image.impl_getUrl();
        if (url.startsWith(baseUrl)) {
            return  "new Image("+className+".class.getResource(\""+url.substring(baseUrl.length())+"\").toExternalForm())";
        } else {
            return "new Image(\""+url+"\")";
        }
    }

    private void process(String indent, Timeline timeline) {
        int count = 0;
        for (KeyFrame keyFrame: timeline.getKeyFrames()) {
            if (keyFrame.getValues().isEmpty()) continue;
            nodeCode.append(indent+"TIMELINE.getKeyFrames().add(new KeyFrame(Duration.millis("+keyFrame.getTime().toMillis()+"d),\n");
            boolean firstKeyValue = true;
            for (KeyValue keyValue: keyFrame.getValues()) {
                if (firstKeyValue) {
                    firstKeyValue = false;
                } else {
                    nodeCode.append(",\n");
                }
                String var = writableVarMap.get(keyValue.getTarget());
                if (var == null) System.err.println("Failed to find writable value in map for : "+keyValue.getTarget());
                nodeCode.append(indent+"    new KeyValue("+var+","+keyValue.getEndValue()+","+toString(keyValue.getInterpolator())+")");
            }
            nodeCode.append("\n"+indent+"));\n");

            if (count > 0 && ((count % 10) == 0)) {
                nodeCode.append("    }\n    private static void method" + (methodCount++) + "(){\n");
            }
            count ++;
        }
    }

    private String toString(Interpolator interpolator) {
//        if (interpolator == Interpolator.DISCRETE || true) {
        if (interpolator == Interpolator.DISCRETE) {
            return "Interpolator.DISCRETE";
        } else if (interpolator == Interpolator.EASE_BOTH) {
            return "Interpolator.EASE_BOTH";
        } else if (interpolator == Interpolator.EASE_IN) {
            return "Interpolator.EASE_IN";
        } else if (interpolator == Interpolator.EASE_OUT) {
            return "Interpolator.EASE_OUT";
        } else if (interpolator == Interpolator.LINEAR) {
            return "Interpolator.LINEAR";
        } else if (interpolator instanceof SplineInterpolator) {
            SplineInterpolator si = (SplineInterpolator)interpolator;
            return "Interpolator.SPLINE("+si.getX1()+"d,"+si.getY1()+"d,"+si.getX2()+"d,"+si.getY2()+"d)";
        } else if (interpolator instanceof NumberTangentInterpolator) {
            NumberTangentInterpolator ti = (NumberTangentInterpolator)interpolator;
            return "Interpolator.TANGENT(Duration.millis("+ TickCalculation.toMillis((long)ti.getInTicks())+"d),"+ti.getInValue()
                    +"d,Duration.millis("+TickCalculation.toMillis((long)ti.getInTicks())+"d),"+ti.getOutValue()+"d)";
        } else {
            throw new UnsupportedOperationException("Unknown Interpolator type: "+interpolator.getClass());
        }
    }

    private String toCode(Color color) {
        return color == null ? null : "new Color("+color.getRed()+","+color.getGreen()+","+color.getBlue()+","+color.getOpacity()+")";
    }
}
