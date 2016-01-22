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

import java.util.*;
import com.javafx.experiments.importers.maya.types.*;
import com.javafx.experiments.importers.maya.values.*;
import com.javafx.experiments.importers.maya.values.impl.*;
import java.io.*;

class nxRigidBody extends MNodeType {
    public nxRigidBody(final MEnv env) {
        super(env, "nxRigidBody");
        addAttribute(new MAttribute(env, "initialPosition", "ip",
                                    env.findDataType("float3")));
        addAttribute(new MAttribute(env, "initialOrientation", "ior",
                                    env.findDataType("float3")));
        addAttribute(new MAttribute(env, "initialVelocity", "iv",
                                    env.findDataType("float3")));
        addAttribute(new MAttribute(env, "initialSpin", "is",
                                    env.findDataType("float3")));
        addAttribute(new MAttribute(env, "staticFriction", "sf", env.findDataType("double")));
        addAttribute(new MAttribute(env, "dynamicFriction", "df", env.findDataType("double")));
        addAttribute(new MAttribute(env, "centerOfMass", "com",
                                    env.findDataType("float3")));
        /*
        addAttribute(new MAttribute(env, "inertiaTensor", "inert",
                                    env.findDataType("float3")));
        */
        addAttribute(new MAttribute(env, "mass", "mas",
                                    env.findDataType("double")));
        addAttribute(new MAttribute(env, "density", "den",
                                    env.findDataType("double")));
        addAttribute(new MAttribute(env, "shape", "sha",
                                    new MArrayType(env, env.findDataType("Message"))));
        addAttribute(new MAttribute(env, "active", "act",
                                    env.findDataType("bool")));
        addAttribute(new MAttribute(env, "bounciness", "b",
                                    env.findDataType("double")));
        addAttribute(new MAttribute(env, "damping", "dp",
                                    env.findDataType("double")));
        addAttribute(new MAttribute(env, "angularDamping", "adp",
                                    env.findDataType("double")));
        addAttribute(new MAttribute(env, "sleepingThreshold", "sthrd",
                                    env.findDataType("double")));

        // joint orient
        addAttribute(new MAttribute(env, "jointOrient", "jo", env.findDataType("double3")));

        // joint orient
        addAlias("jox", "jo.x");
        addAlias("joy", "jo.y");
        addAlias("joz", "jo.z");

        addAlias("ipx", "ip.x");
        addAlias("ipy", "ip.y");
        addAlias("ipz", "ip.z");

        addAlias("iox", "ior.x");
        addAlias("ioy", "ior.y");
        addAlias("ioz", "ior.z");

        addAlias("comx", "com.x");
        addAlias("comy", "com.y");
        addAlias("comz", "com.z");

        addAlias("ivx", "iv.x");
        addAlias("ivy", "iv.y");
        addAlias("ivz", "iv.z");

        addAlias("isx", "is.x");
        addAlias("isy", "is.y");
        addAlias("isz", "is.z");
    }

    protected void initNode(MNode result) {
        MFloat f  = (MFloat)result.getAttr("mas");
        f.set(1);
        MBool b = (MBool)result.getAttr("act");
        b.set(true);
        f = (MFloat)result.getAttr("b");
        f.set(.6f);
        MFloat staticFriction = (MFloat)result.getAttr("sf");
        staticFriction.set(.2f);
        MFloat dynamicFriction = (MFloat)result.getAttr("df");
        dynamicFriction.set(.2f);
    }
}



// disney maya bullet plugin

class dRigidBody extends MNodeType {
    public dRigidBody(final MEnv env) {
        super(env, "dRigidBody");
        addAttribute(new MAttribute(env, "inCollisionShape", "incs",
                                    env.findDataType("Message")));
        addAttribute(new MAttribute(env, "solver", "solv",
                                    env.findDataType("Message")));
        addAttribute(new MAttribute(env, "restitution", "rst",
                                    env.findDataType("double")));
        addAttribute(new MAttribute(env, "friction", "fc",
                                    env.findDataType("double")));
        addAttribute(new MAttribute(env, "linearDamping", "ld",
                                    env.findDataType("double")));
        addAttribute(new MAttribute(env, "angularDamping", "ad",
                                    env.findDataType("double")));
        addAttribute(new MAttribute(env, "initialPosition", "inpo",
                                    env.findDataType("float3")));
        addAttribute(new MAttribute(env, "initialRotation", "inro",
                                    env.findDataType("float3")));
        addAttribute(new MAttribute(env, "initialVelocity", "inve",
                                    env.findDataType("float3")));
        addAttribute(new MAttribute(env, "initialSpin", "insp",
                                    env.findDataType("float3")));
        addAttribute(new MAttribute(env, "mass", "ma",
                                    env.findDataType("double")));
    }

    protected void initNode(MNode result) {
        MFloat f  = (MFloat)result.getAttr("rst");
        f.set(.1f);
        f  = (MFloat)result.getAttr("fc");
        f.set(0.5f);
        f  = (MFloat)result.getAttr("ld");
        f.set(0.3f);
        f  = (MFloat)result.getAttr("ad");
        f.set(0.3f);
        f  = (MFloat)result.getAttr("ma");
        f.set(1.0f);
    }
}

class dCollisionShape extends MNodeType {
    public dCollisionShape(final MEnv env) {
        super(env, "dCollisionShape");
        addAttribute(new MAttribute(env, "type", "tp",
                                    env.findDataType("enum")));
        addAttribute(new MAttribute(env, "scale", "sc",
                                    env.findDataType("float3")));
        addAttribute(new MAttribute(env, "outCollisionShape", "oucs",
                                    env.findDataType("Message")));
        addAttribute(new MAttribute(env, "inShape", "insh",
                                    env.findDataType("Message")));
    }
    protected void initNode(MNode result) {
        MFloat3 f  = (MFloat3)result.getAttr("sc");
        f.set(1f, 1f, 1f);
    }
}

class dHingeConstraint extends MNodeType {
    public dHingeConstraint(final MEnv env) {
        super(env, "dHingeConstraint");
        addAttribute(new MAttribute(env, "inRigidBodyA", "inrba",
                                    env.findDataType("Message")));
        addAttribute(new MAttribute(env, "inRigidBodyB", "inrbb",
                                    env.findDataType("Message")));
        addAttribute(new MAttribute(env, "damping", "dmp",
                                    env.findDataType("double")));
        addAttribute(new MAttribute(env, "lowerLimit", "llmt",
                                    env.findDataType("double")));
        addAttribute(new MAttribute(env, "upperLimit", "ulmt",
                                    env.findDataType("double")));
        addAttribute(new MAttribute(env, "limitSoftness", "lmSo",
                                    env.findDataType("double")));
        addAttribute(new MAttribute(env, "biasFactor", "biFa",
                                    env.findDataType("double")));
        addAttribute(new MAttribute(env, "relaxationFactor", "reFa",
                                    env.findDataType("double")));
        addAttribute(new MAttribute(env, "rotationInA", "hgRotA",
                                    env.findDataType("float3")));
        addAttribute(new MAttribute(env, "rotationInB", "hgRotB",
                                    env.findDataType("float3")));
        addAttribute(new MAttribute(env, "pivotInA", "pivinA",
                                    env.findDataType("float3")));
        addAttribute(new MAttribute(env, "pivotInB", "pivinB",
                                    env.findDataType("float3")));
        addAttribute(new MAttribute(env, "enableAngularMotor", "enAM",
                                    env.findDataType("bool")));
        addAttribute(new MAttribute(env, "motorTargetVelocity", "mTV",
                                    env.findDataType("double")));
        addAttribute(new MAttribute(env, "maxMotorImpulse", "mMI",
                                    env.findDataType("double")));
    }

    protected void initNode(MNode result) {
        MFloat f  = (MFloat)result.getAttr("reFa");
        f.set(1.0f);
        f  = (MFloat)result.getAttr("dmp");
        f.set(1.0f);
        f  = (MFloat)result.getAttr("llmt");
        f.set(1.0f);
        f  = (MFloat)result.getAttr("ulmt");
        f.set(-1.0f);
        f  = (MFloat)result.getAttr("reFa");
        f.set(1.0f);
        f  = (MFloat)result.getAttr("lmSo");
        f.set(.9f);
        f  = (MFloat)result.getAttr("biFa");
        f.set(.3f);
        f  = (MFloat)result.getAttr("mMI");
        f.set(1.0f);
    }
}

class dNailConstraint extends MNodeType {
    public dNailConstraint(final MEnv env) {
        super(env, "dNailConstraint");
        addAttribute(new MAttribute(env, "inRigidBodyA", "inrbA",
                                    env.findDataType("Message")));
        addAttribute(new MAttribute(env, "inRigidBodyB", "inrbB",
                                    env.findDataType("Message")));
        addAttribute(new MAttribute(env, "damping", "dmp",
                                    env.findDataType("double")));
        addAttribute(new MAttribute(env, "pivotInA", "pivA",
                                    env.findDataType("float3")));
        addAttribute(new MAttribute(env, "pivotInB", "pivB",
                                    env.findDataType("float3")));
    }

    protected void initNode(MNode result) {
        MFloat f  = (MFloat)result.getAttr("dmp");
        f.set(1.0f);
    }
}



class characterType extends MNodeType {
    public characterType(final MEnv env) {
        super(env, "character");
        addSuperType(env.findNodeType("objectSet"));
        // @TODO attributes
        addAttribute(new MAttribute(env, "attributeAliasList", "aal",
                                    env.findDataType("AttributeAlias")));
        addAttribute(new MAttribute(env, "linearValues", "lv",
                                    env.findDataType("float[]")));
        addAttribute(new MAttribute(env, "unitlessValues", "uv",
                                    env.findDataType("float[]")));
        addAttribute(new MAttribute(env, "angularValues", "av",
                                    env.findDataType("float[]")));
        addAttribute(new MAttribute(env, "timeValues", "tv",
                                    env.findDataType("float[]")));
        addAttribute(new MAttribute(env, "unitlessClipValues", "uc",
                                    env.findDataType("float[]")));
        addAttribute(new MAttribute(env, "animationMapping", "am",
                                    env.findDataType("characterMapping")));
        addAttribute(new MAttribute(env, "referenceMapping", "rm",
                                    env.findDataType("characterMapping")));
        addAttribute(new MAttribute(env, "clipIndexMap", "cim",
                                    env.findDataType("int[]")));
        addAttribute(new MAttribute(env, "timelineClipStart", "tcs",
                                    env.findDataType("time")));
        addAttribute(new MAttribute(env, "timelineClipEnd", "tce",
                                    env.findDataType("time")));

    }
}

class animClipType extends MNodeType {
    public animClipType(final MEnv env) {
        super(env, "animClip");
        addSuperType(env.findNodeType("dependNode"));
        addAttribute(new MAttribute(env, "sourceStart", "ss",
                                    env.findDataType("time")));

        addAttribute(new MAttribute(env, "sourceEnd", "se",
                                    env.findDataType("time")));
        addAttribute(new MAttribute(env, "clipInstance", "ci",
                                    env.findDataType("bool")));
        addAttribute(new MAttribute(env, "startFrame", "sf",
                                    env.findDataType("time")));
        addAttribute(new MAttribute(env, "enabled", "ea",
                                    env.findDataType("bool")));
        addAttribute(new MAttribute(env, "postCycle", "ca",
                                    env.findDataType("double")));
        addAttribute(new MAttribute(env, "scale", "sc",
                                    env.findDataType("double")));
    }
    protected void initNode(MNode result) {
        MFloat f  = (MFloat)result.getAttr("sc");
        f.set(1);
    }

}

class clipSchedulerType extends MNodeType {
    public clipSchedulerType(final MEnv env) {
        super(env, "clipScheduler");
        addSuperType(env.findNodeType("dependNode"));
        addAttribute(new MAttribute(env, "clip", "cl",
                                    new MArrayType(env, env.findDataType("Message"))));
        addAttribute(new MAttribute(env, "start", "st",
                                    env.findDataType("float[]")));
        addAttribute(new MAttribute(env, "sourceStart", "ss",
                                    env.findDataType("float[]")));
        addAttribute(new MAttribute(env, "sourceEnd", "se",
                                    env.findDataType("float[]")));
        addAttribute(new MAttribute(env, "scale", "sc",
                                    env.findDataType("float[]")));
        addAttribute(new MAttribute(env, "hold", "h",
                                    env.findDataType("float[]")));
        addAttribute(new MAttribute(env, "weightStyle", "ws",
                                    env.findDataType("int[]")));
        addAttribute(new MAttribute(env, "preCycle", "cb",
                                    env.findDataType("float[]")));
        addAttribute(new MAttribute(env, "postCycle", "ca",
                                    env.findDataType("float[]")));
        addAttribute(new MAttribute(env, "track", "tr",
                                    env.findDataType("int[]")));
        addAttribute(new MAttribute(env, "trackState", "ts",
                                    env.findDataType("int[]")));
        addAttribute(new MAttribute(env, "numTracks", "nt",
                                    env.findDataType("int")));

    }
}


class clipLibraryType extends MNodeType {
    public clipLibraryType(final MEnv env) {
        super(env, "clipLibrary");
        addSuperType(env.findNodeType("dependNode"));
        addAttribute(new MAttribute(env, "clipEvalList", "cel",
                                    new MArrayType(env, new MCompoundType(env,
                                                                     "clipEvalList.cel") {
                                            {
                                                addField("cev",
                                                         new MArrayType(env,
                                                                        new MCompoundType(env,
                                                                                          "clipEvalList.cel.cev") {
                                                                            {
                                                                                addField("cevr", // clipEval_Raw
                                                                                         env.findDataType("function"),
                                                                                         null);
                                                                            }
                                                                        }),
                                                         null);
                                            }
                                        })));
        addAttribute(new MAttribute(env, "sourceClip", "sc",
                                    new MArrayType(env,
                                                   env.findDataType("Message"))));

        addAttribute(new MAttribute(env, "clip", "cl",
                                    new MArrayType(env,
                                                   env.findDataType("Message"))));
        addAttribute(new MAttribute(env, "characterData", "cd",
                                    new MCompoundType(env, "cd") {
                                        {
                                            addField("cm",
                                                     env.findDataType("characterMapping"), null);
                                            addField("cim",
                                                     env.findDataType("int[]"), null);
                                        }
                                    }));


    }
}

public class MEnv {

    float  playbackStart;
    float playbackEnd;

    public void setPlaybackRange(float min, float max) {
        playbackStart = min;
        playbackEnd = max;
    }

    public float getPlaybackStart() {
        return playbackStart;
    }

    public float getPlaybackEnd() {
        return playbackEnd;
    }

    public static class Visitor {
        public void visit(MObject obj) {
            obj.accept(this);
        }
        public void visit(MData data) {
        }

        public void visitEnv(MEnv env) {
        }

        public void visitNodeType(MNodeType type) {
        }

        public void visitNode(MNode node) {
        }

        public void visitDataType(MDataType type) {
        }

        public void visitAttribute(MAttribute attr) {
        }

        public void visitNodeAttribute(MNode node, MAttribute attr, MData value) {
        }
    }

    public void accept(Visitor visitor) {
        visitor.visitEnv(this);
        for (MDataType t : dataTypes.values()) {
            if (t != null) {
                t.accept(visitor);
            }
        }
        for (MNodeType t : nodeTypes.values()) {
            t.accept(visitor);
        }
        for (MNode t : nodes) {
            t.accept(visitor);
        }
    }

    public void dump(final PrintStream ps) {
        Visitor v = new Visitor() {
                public void visitEnv(MEnv env) {
                    ps.println("MEnv:");
                }

                public void visitNodeType(MNodeType type) {
                    ps.println("Node Type: "+ type.getName());
                }

                public void visitNode(MNode node) {
                    ps.println("Node: "+ node.getName());
                }

                public void visitDataType(MDataType type) {
                    ps.println("Data type: "+ type.getName());
                }

                public void visitAttribute(MAttribute attr) {
                    ps.println("Attribute : "+ attr);
                }

                public void visitNodeAttribute(MNode node, MAttribute attr, MData value) {
                    ps.println("Attribute value: " + node.getName() +"."+attr.getShortName() + " = "+ value);
                }
            };
        accept(v);
    }


    private Set<MNode> nodes = new LinkedHashSet();
    private Map<String, MNodeType> nodeTypes = new HashMap();
    private Map<String, MDataType> dataTypes = new HashMap();

    public Collection<MNode> getNodes() {
        return nodes;
    }

    public MData createData(String dataType) {
        MDataType type = dataTypes.get(dataType);
        if (type == null) {
            //            System.out.println("WARNING: data type not found: " + dataType);
            return null;
        }
        return type.createData();
    }

    public MNode createNode(String typeName, String name) {
        return createNode(findNodeType(typeName), name);
    }

    public MNode createNode(MNodeType type, String name) {
        if (type == null) {
            return null;
        }
        return type.createNode(name);
    }

    public MNode findNode(String name) {
        if (name.indexOf('|') >= 0) {
            String[] names = name.split("\\|");
            MNode result = null;
            for (String n : names) {
                if (n != null && n.length() > 0) {
                    if (result == null) {
                        result = findNode(n);
                    } else {
                        result = result.getChildNode(n);
                    }
                    if (result == null) {
                        break;
                    }
                }
            }
            return result;
        }
        MNode result = null;
        //        System.out.println("find NODE... " + name);
        for (MNode n: nodes) {
            if (n.getName().equals(name)) {
                //                System.out.println("find node: " + name + " : " + n.getName() + " full: " + n.getFullName());
                if (result != null) {
                    if (n.getParentNodes().size() == 0) {
                        result = n;
                    }
                } else {
                    result = n;
                }
            }
        }
        return result;
    }

    //    public abstract MData  parseData(MDataType type, String text);

    public void aliasDataType(String alias, String type) {
        dataTypes.put(alias, dataTypes.get(type));
    }

    public void addDataType(MDataType dataType) {
        dataTypes.put(dataType.getName(), dataType);
    }
    public void addNodeType(MNodeType nodeType) {
        nodeTypes.put(nodeType.getName(), nodeType);
    }

    public MDataType findDataType(String name) {
        return dataTypes.get(name);
    }

    public MNodeType findNodeType(String name) {
        MNodeType ret =  nodeTypes.get(name);
        if (ret == null) {
            //System.out.println("nodeTypes =" + nodeTypes);
            //throw new Error("Node Type Not Found: "+ name);
//            System.out.println("node type not found: " +name);
            return null;
        }
        return ret;
    }

    public void addNode(MNode node) {
        nodes.add(node);
    }

    static class TransformNodeType extends MNodeType {
        protected void initNode(MNode result) {
            MFloat3 s = (MFloat3)result.getAttr("s");
            s.set(1, 1, 1);
        }
        TransformNodeType(MEnv env) {
            super(env, "transform");
            MDataType t = env.findDataType("double3");
            addSuperType(env.findNodeType("geometryShape"));
            addAttribute(new MAttribute(env, "translate", "t", t));
            addAttribute(new MAttribute(env, "rotate", "r", t));
            addAttribute(new MAttribute(env, "scale", "s", t));
            addAttribute(new MAttribute(env, "rotateAxis", "ra", t));
            addAttribute(new MAttribute(env, "rotatePivot", "rp", t));
            addAttribute(new MAttribute(env, "rotatePivotTranslate", "rpt", t));
            addAttribute(new MAttribute(env, "scalePivot", "sp", t));
            addAttribute(new MAttribute(env, "scalePivotTranslate", "spt", t));
            addAttribute(new MAttribute(env, "translateMinusRotatePivot", "tmrp", t));
            addAttribute(new MAttribute(env, "worldMatrix", "wm", env.findDataType("matrix")));
            String[] alias = new String[]  {
                "t", "r", "s", "rp", "rpt", "sp", "spt", "ra", "tmrp",
            };
            String[] comps = new String[] { "x", "y", "z" };
            for (int i = 0; i < alias.length; i++) {
                for (int j = 0; j < 3; j++) {
                    addAlias(alias[i]+ comps[j], alias[i] + "["+j+"]");
                }
            }
        }
    }

    static class DynBase extends MNodeType {
        DynBase(MEnv env) {
            super(env, "dynBase");
            addSuperType(env.findNodeType("transform"));
        }
    }

    static class Field extends MNodeType {
        Field(final MEnv env) {
            super(env, "field");
            addSuperType(env.findNodeType("dynBase"));
            addAttribute(new MAttribute(env, "magnitude", "mag",
                                        env.findDataType("double")));
            addAttribute(new MAttribute(env, "attenuation", "att",
                                        env.findDataType("double")));

            addAttribute(new MAttribute(env, "maxDistance", "com/javafx/importers/max",
                                        env.findDataType("double")));
            addAttribute(new MAttribute(env, "falloffCurve", "fc",
                                        new MArrayType(env,
                                                   new MCompoundType(env,
                                                                     "field.fc") {
                                                       {
                                                           addField("fcp",
                                                                    env.findDataType("float"),
                                                                    null);

                                                           addField("fcfv",
                                                                    env.findDataType("float"),
                                                                    null);

                                                           addField("fci",
                                                                    env.findDataType("enum"),
                                                                    null);

                                                       }
                                                   })));
            addAttribute(new MAttribute(env, "inputData", "ind",
                                        new MArrayType(env,
                                                   new MCompoundType(env,
                                                                    "field.ind") {
                                                       {
                                                           addField("inp",
                                                                    env.findDataType("float3[]"),
                                                                    null);

                                                           addField("inv",
                                                                    env.findDataType("float3[]"),
                                                                    null);

                                                           addField("im",
                                                                    env.findDataType("float[]"),
                                                                    null);

                                                           addField("dt",
                                                                    env.findDataType("double"),
                                                                    null);

                                                       }
                                                   })));

            addAttribute(new MAttribute(env, "outputForce", "of",
                                        env.findDataType("float3[]")));
        }

        protected void initNode(MNode result) {
            MFloat f  = (MFloat)result.getAttr("mag");
            f.set(1);
            f = (MFloat)result.getAttr("com/javafx/importers/max");
            f.set(-1);
        }
    }

    static class RadialField extends MNodeType {
        RadialField(final MEnv env) {
            super(env, "radialField");
            addSuperType(env.findNodeType("field"));
            addAttribute(new MAttribute(env, "radialType", "typ",
                                        env.findDataType("double")));
        }
    }

    static class VortexField extends MNodeType {
        VortexField(final MEnv env) {
            super(env, "vortexField");
            addSuperType(env.findNodeType("field"));
            addAttribute(new MAttribute(env, "axis", "ax",
                                        env.findDataType("float3")));
            addAlias("axx", "ax.x");
            addAlias("axy", "ax.y");
            addAlias("axz", "ax.z");
        }
    }

    static class GravityField extends MNodeType {
        GravityField(final MEnv env) {
            super(env, "gravityField");
            addSuperType(env.findNodeType("field"));
            addAttribute(new MAttribute(env, "direction", "d",
                                        env.findDataType("float3")));
            addAlias("dx", "d.x");
            addAlias("dy", "d.y");
            addAlias("dz", "d.z");
        }
    }

    static class PointEmitter extends MNodeType {
        PointEmitter(MEnv env) {
            super(env, "pointEmitter");
            addSuperType(env.findNodeType("dynBase"));
            addAttribute(new MAttribute(env, "emitterType", "emt", env.findDataType("enum")));
            addAttribute(new MAttribute(env, "rate", "rat", env.findDataType("double")));
            addAttribute(new MAttribute(env, "speed", "spd", env.findDataType("double")));
            addAttribute(new MAttribute(env, "speedRandom", "srnd", env.findDataType("double")));
            addAttribute(new MAttribute(env, "direction", "d", env.findDataType("double3")));
            addAlias("dx", "d.x");
            addAlias("dy", "d.y");
            addAlias("dz", "d.z");
            addAttribute(new MAttribute(env, "particleColor", "pc", env.findDataType("double3")));
            addAlias("pcr", "pc.x");
            addAlias("pcg", "pc.y");
            addAlias("pcb", "pc.z");
            addAttribute(new MAttribute(env, "output", "ot",
                                        new MArrayType(env, env.findDataType("Message"))));
            addAttribute(new MAttribute(env, "spread", "spr", env.findDataType("double")));
        }

        protected void initNode(MNode result) {
            MInt e = (MInt)result.getAttr("emt");
            e.set(1);
            MFloat d = (MFloat)result.getAttr("rat");
            d.set(100);
            MFloat3 d3 = (MFloat3)result.getAttr("d");
            d3.set(1, 0, 0);
            MFloat3 pc = (MFloat3)result.getAttr("pc");
            pc.set(.5f, .5f, .5f);
            MFloat spd = (MFloat)result.getAttr("spd");
            spd.set(1);
        }
    }

    static class Constraint extends MNodeType {
        Constraint(MEnv env) {
            super(env, "constraint");
            addSuperType(env.findNodeType("transform"));
            addAttribute(new MAttribute(env, "enableRestPosition", "erp", env.findDataType("bool")));
        }
    }

    static class PointConstraint extends MNodeType {
        PointConstraint(MEnv env) {
            super(env, "pointConstraint");
            addSuperType(env.findNodeType("constraint"));
            addAttribute(new MAttribute(env, "constraintRotatePivot", "crp", env.findDataType("double3")));
            addAttribute(new MAttribute(env, "constraintRotateTranslate", "crt", env.findDataType("double3")));
            addAttribute(new MAttribute(env, "constraintTranslate", "ct", env.findDataType("double3")));

            addAlias("crpx", "crp.x");
            addAlias("crpy", "crp.y");
            addAlias("crpz", "crp.z");

            addAlias("crtx", "crt.x");
            addAlias("crty", "crt.y");
            addAlias("crtz", "crt.z");

            addAlias("ctx", "ct.x");
            addAlias("cty", "ct.y");
            addAlias("ctz", "ct.z");

            addAttribute(new MAttribute(env, "restTranslate", "rst", env.findDataType("double3")));
            addAlias("rstx", "rst.x");
            addAlias("rsty", "rst.y");
            addAlias("rstz", "rst.z");
        }
    }

    static class ParentConstraint extends MNodeType {
        ParentConstraint(final MEnv env) {
            super(env, "parentConstraint");
            addSuperType(env.findNodeType("constraint"));
            addAttribute(new MAttribute(env, "target", "tg",
                                        new MArrayType(env, new MCompoundType(env, "parentConstraint.tg") {
                                                {
                                                    addField("tt", env.findDataType("double3"), null);
                                                    addField("trp", env.findDataType("double3"), null);
                                                    addField("trt", env.findDataType("double3"), null);
                                                    addField("tpm", env.findDataType("matrix"), null);
                                                    addField("tw", env.findDataType("double"), null);
                                                }
                                            })));
            addAttribute(new MAttribute(env, "restRotate", "rsrr", env.findDataType("double3")));
            addAlias("rsrrx", "rsrr.x");
            addAlias("rsrry", "rsrr.y");
            addAlias("rsrrz", "rsrr.z");

            addAttribute(new MAttribute(env, "offset", "o", env.findDataType("double3")));
            addAlias("ox", "o.x");
            addAlias("oy", "o.y");
            addAlias("oz", "o.z");
            addAttribute(new MAttribute(env, "restTranslate", "rst", env.findDataType("double3")));
            addAlias("rstx", "rst.x");
            addAlias("rsty", "rst.y");
            addAlias("rstz", "rst.z");
        }
    }

    static class OrientConstraint extends MNodeType {
        OrientConstraint(final MEnv env) {
            super(env, "orientConstraint");
            addSuperType(env.findNodeType("constraint"));
            addAttribute(new MAttribute(env, "target", "tg",
                                        new MArrayType(env, new MCompoundType(env, "orientConstraint.tg") {
                                                {
                                                    addField("tt", env.findDataType("double3"), null);
                                                    addField("trp", env.findDataType("double3"), null);
                                                    addField("trt", env.findDataType("double3"), null);
                                                    addField("tpm", env.findDataType("matrix"), null);
                                                    addField("tw", env.findDataType("double"), null);
                                                }
                                            })));
        }
    }


    static class AimConstraint extends MNodeType {
        AimConstraint(final MEnv env) {
            super(env, "aimConstraint");
            addSuperType(env.findNodeType("constraint"));
            addAttribute(new MAttribute(env, "target", "tg",
                                        new MArrayType(env, new MCompoundType(env, "aimConstraint.tg") {
                                                {
                                                    addField("tt", env.findDataType("double3"), null);
                                                    addField("trp", env.findDataType("double3"), null);
                                                    addField("trt", env.findDataType("double3"), null);
                                                    addField("tpm", env.findDataType("matrix"), null);
                                                    addField("tw", env.findDataType("double"), null);
                                                }
                                            })));
            addAttribute(new MAttribute(env, "aimVector", "a", env.findDataType("double3")));
            addAlias("ax", "a.x");
            addAlias("ay", "a.y");
            addAlias("az", "a.z");
            addAttribute(new MAttribute(env, "upVector", "u", env.findDataType("double3")));
            addAlias("ux", "u.x");
            addAlias("uy", "u.y");
            addAlias("uz", "u.z");
            addAttribute(new MAttribute(env, "worldUpVector", "wu", env.findDataType("double3")));
            addAlias("wux", "wu.x");
            addAlias("wuy", "wu.y");
            addAlias("wuz", "wu.z");

            addAttribute(new MAttribute(env, "restRotate", "rsrr", env.findDataType("double3")));
            addAlias("rsrrx", "rsrr.x");
            addAlias("rsrry", "rsrr.y");
            addAlias("rsrrz", "rsrr.z");

            addAttribute(new MAttribute(env, "offset", "o", env.findDataType("double3")));
            addAlias("ox", "o.x");
            addAlias("oy", "o.y");
            addAlias("oz", "o.z");
        }

        protected void initNode(MNode result) {
            MFloat3 v = (MFloat3)result.getAttr("a");
            v.set(1, 0, 0);
            v = (MFloat3)result.getAttr("u");
            v.set(0, 1, 0);
            v = (MFloat3)result.getAttr("wu");
            v.set(0, 1, 0);
        }
    }

    static class PoleVectorConstraint extends MNodeType {
        PoleVectorConstraint(final MEnv env) {
            super(env, "poleVectorConstraint");
            addSuperType(env.findNodeType("pointConstraint"));
            addAttribute(new MAttribute(env, "pivotSpace", "ps", env.findDataType("matrix")));
            addAttribute(new MAttribute(env, "target", "tg",
                                        new MCompoundType(env, "poleVectorConstraint.tg") {
                                            {
                                                addField("tt", env.findDataType("double3"), null);
                                                addField("trp", env.findDataType("double3"), null);
                                                addField("trt", env.findDataType("double3"), null);
                                                addField("tpm", env.findDataType("matrix"), null);
                                                addField("tw", env.findDataType("double"), null);
                                            }
                                        }));
        }
    }


    static class IKHandle extends MNodeType {
        IKHandle(MEnv env) {
            super(env, "ikHandle");
            addSuperType(env.findNodeType("transform"));
            addAttribute(new MAttribute(env, "startJoint", "hsj", env.findDataType("Message")));
            addAttribute(new MAttribute(env, "endEffector", "hee", env.findDataType("Message")));
            addAttribute(new MAttribute(env, "weight", "hw", env.findDataType("double")));
            addAttribute(new MAttribute(env, "poleVector", "pv", env.findDataType("double3")));
            addAttribute(new MAttribute(env, "roll", "rol", env.findDataType("double")));
            addAttribute(new MAttribute(env, "twist", "twi", env.findDataType("double")));
            addAlias("pvx", "pv.x");
            addAlias("pvy", "pv.y");
            addAlias("pvz", "pv.z");
        }
        protected void initNode(MNode result) {
            MFloat3 pv = (MFloat3)result.getAttr("pv");
            pv.set(0, 0, 1);
        }
    }

    static class IKEffectorType extends MNodeType {
        IKEffectorType(MEnv env) {
            super(env, "ikEffector");
            addSuperType(env.findNodeType("transform"));
            addAttribute(new MAttribute(env, "handlePath", "hp", env.findDataType("Message")));
        }
    }

    static class JointNodeType extends MNodeType {
        JointNodeType(MEnv env) {
            super(env, "joint");
            addSuperType(env.findNodeType("transform"));
            addAttribute(new MAttribute(env, "bindPose", "bps", env.findDataType("matrix")));
            addAttribute(new MAttribute(env, "jointOrient", "jo", env.findDataType("double3")));
            addAttribute(new MAttribute(env, "jointOrientType", "jot", env.findDataType("string")));
            addAttribute(new MAttribute(env, "inverseParentScale", "is", env.findDataType("double3")));
            addAttribute(new MAttribute(env, "preferredAngle", "pa", env.findDataType("double3")));
            addAlias("pax", "pa.x");
            addAlias("pay", "pa.y");
            addAlias("paz", "pa.z");
            addAttribute(new MAttribute(env, "segmentScaleCompensate", "ssc", env.findDataType("bool")));
        }
        protected void initNode(MNode result) {
            MString jot = (MString)result.getAttr("jot");
            jot.set("xyz");
            MFloat3 is = (MFloat3)result.getAttr("is");
            is.set(1, 1, 1);
            MBool ssc = (MBool)result.getAttr("ssc");
            ssc.set(true);
        }
    }

    static class GeometryFilterNodeType extends MNodeType {
        GeometryFilterNodeType(final MEnv env) {
            super(env, "geometryFilter");
            addSuperType(env.findNodeType("dependNode"));
            addAttribute(new MAttribute(env, "outputGeometry", "og",
                                        new MArrayType(env, env.findDataType("Message"))));;
            addAttribute(new MAttribute(env, "input", "ip",
                                        new MArrayType(env, new MCompoundType(env,
                                                                              "geometryFilter.ip") {
                                                {
                                                    addField("ig", env.findDataType("Message"), null);
                                                    addField("gi", env.findDataType("int"), null);
                                                }
                                            })));

        }
    }

    static class TweakNodeType extends MNodeType {
        TweakNodeType(final MEnv env) {
            super(env, "tweak");
            addSuperType(env.findNodeType("geometryFilter"));
        }
    }

    static class BlendShapeNodeType extends MNodeType {
        BlendShapeNodeType(final MEnv env) {
            super(env, "blendShape");
            addSuperType(env.findNodeType("geometryFilter"));
            addAttribute(new MAttribute(env, "weight", "w",
                                        env.findDataType("float[]")));
            addAttribute(new MAttribute(env, "inputTarget",
                                        "it",
                                        new MArrayType(env,
                                                       new MCompoundType(env,
                                                                         "blendShape.it") {
                                                {

                                                    addField("itg",
                                                             new MArrayType(env,  new MCompoundType(env, "blendShape.it.itg") {
                                                                     {
                                                                         addField("iti", new MArrayType(env, new MCompoundType(env, "blendShape.it.itg.iti") {
                                                                                 {
                                                                                     addField("itg", env.findDataType("geometry"), null);
                                                                                     addField("ipt", env.findDataType("pointArray"), null);
                                                                                     addField("ict", env.findDataType("componentList"), null);
                                                                                 }
                                                                             }), null);
                                                                         addField("tw", env.findDataType("float[]"), null);
                                                                     }
                                                                 }),
                                                             null);
                                                }
                                            })));

        }
    }

    static class SkinClusterNodeType extends MNodeType {
        SkinClusterNodeType(final MEnv env) {
            super(env, "skinCluster");
            addSuperType(env.findNodeType("geometryFilter"));
            addAttribute(new MAttribute(env, "weightList", "wl",
                                        new MArrayType(env, new MCompoundType(env, "skinCluster.wl") {
                                                {
                                                    addField("w", env.findDataType("double[]"),
                                                             env.createData("double[]"));
                                                }
                                            })));
            addAttribute(new MAttribute(env, "bindPreMatrix",
                                        "pm", new MArrayType(env, env.findDataType("matrix"))));
            addAttribute(new MAttribute(env, "geomMatrix",
                                        "gm", env.findDataType("matrix")));
            addAttribute(new MAttribute(env, "matrix",
                                        "ma", new MArrayType(env, env.findDataType("matrix"))));
        }
    }


    static class MaterialInfoNodeType extends MNodeType {
        MaterialInfoNodeType(MEnv env) {
            super(env, "materialInfo");
            addSuperType(env.findNodeType("dependNode"));
            addAttribute(new MAttribute(env, "shadingGroup", "sg",
                                        env.findDataType("Message")));
            addAttribute(new MAttribute(env, "material", "m",
                                        env.findDataType("Message")));
        }
    }

    static class EntityType extends MNodeType {
        EntityType(MEnv env) {
            super(env, "entity");
            addSuperType(env.findNodeType("dependNode"));
            addAttribute(new MAttribute(env, "dagSetMembers", "dsm",
                                        env.findDataType("Message")));
        }
    }

    static class DagPoseType extends MNodeType {
        DagPoseType(MEnv env) {
            super(env, "dagPose");
            addSuperType(env.findNodeType("dependNode"));
            addAttribute(new MAttribute(env, "worldMatrix", "wm", env.findDataType("matrix")));

        }
    }

    static class ObjectSetType extends MNodeType {
        ObjectSetType(MEnv env) {
            super(env, "objectSet");
            addSuperType(env.findNodeType("entity"));
            /*
            addAttribute(new MAttribute(env, "dnSetMembers", "dnsm",
                                        env.findDataType("genericTypedData")));
            */
        }
    }

    static class CharacterType extends MNodeType {
        CharacterType(MEnv env) {
            super(env, "character");
            addSuperType(env.findNodeType("objectSet"));
            addAttribute(new MAttribute(env, "unitlessValues", "uv",
                                        env.findDataType("float[]")));
            addAttribute(new MAttribute(env, "linearValues", "lv",
                                        env.findDataType("float[]")));
            addAttribute(new MAttribute(env, "angularValues", "av",
                                        env.findDataType("float[]")));
            addAttribute(new MAttribute(env, "timeValues", "tv",
                                        env.findDataType("float[]")));
            addAttribute(new MAttribute(env, "animationMapping", "am",
                                        new MArrayType(env,  env.findDataType("string"))));
        }
    }


    static class GroupIdType extends MNodeType {
        GroupIdType(MEnv env) {
            super(env, "groupId");
            addSuperType(env.findNodeType("dependNode"));
            addAttribute(new MAttribute(env, "groupId", "id",
                                        env.findDataType("int")));
        }
    }

    static class GroupPartsType extends MNodeType {
        GroupPartsType(MEnv env) {
            super(env, "groupParts");
            addSuperType(env.findNodeType("dependNode"));
            addAttribute(new MAttribute(env, "inputGeometry", "ig",
                                        env.findDataType("Message")));
            addAttribute(new MAttribute(env, "inputComponents", "ic",
                                        env.findDataType("componentList")));
            addAttribute(new MAttribute(env, "groupId", "gi",
                                        env.findDataType("int")));
        }
    }

    static class ShadingEngineNodeType extends MNodeType {
        ShadingEngineNodeType(MEnv env) {
            super(env, "shadingEngine");
            addSuperType(env.findNodeType("objectSet"));
            addAttribute(new MAttribute(env, "surfaceShader", "ss",
                                        env.findDataType("Message")));
        }
    }

    static class LightLinkerNodeType extends MNodeType {
        LightLinkerNodeType(MEnv env) {
            super(env, "lightLinker");
            addSuperType(env.findNodeType("dependNode"));
        }
    }

    static class DependNodeType extends MNodeType {
        DependNodeType(MEnv env) {
            super(env, "dependNode");
            addAttribute(new MAttribute(env, "message", "msg",
                                        env.findDataType("Message")));
        }
    }

    static class AddDoubleLinearType extends MNodeType {
        AddDoubleLinearType(MEnv env) {
            super(env, "addDoubleLinear");
            addSuperType(env.findNodeType("dependNode"));
            addAttribute(new MAttribute(env, "input1", "i1",
                                        env.findDataType("double")));
            addAttribute(new MAttribute(env, "input2", "i2",
                                        env.findDataType("double")));
            addAttribute(new MAttribute(env, "output", "o",
                                        env.findDataType("double")));
        }
    }

    static class MotionPathType extends MNodeType {
        MotionPathType(final MEnv env) {
            super(env, "motionPath");
            addSuperType(env.findNodeType("dependNode"));
            addAttribute(new MAttribute(env, "uValue", "u",
                                        env.findDataType("double")));
            addAttribute(new MAttribute(env, "frontTwist", "ft",
                                        env.findDataType("double")));
            addAttribute(new MAttribute(env, "upTwist", "ut",
                                        env.findDataType("double")));
            addAttribute(new MAttribute(env, "sideTwist", "st",
                                        env.findDataType("double")));
            addAttribute(new MAttribute(env, "allCoordinates", "ac",
                                        env.findDataType("float3")));
            addAlias("ac.x", "xc");
            addAlias("ac.y", "yc");
            addAlias("ac.z", "zc");
            addAttribute(new MAttribute(env, "rotate", "r",
                                        env.findDataType("float3")));
            addAlias("r.x", "rx");
            addAlias("r.y", "ry");
            addAlias("r.z", "rz");
            addAttribute(new MAttribute(env, "rotateOrder", "ro",
                                        env.findDataType("enum")));
            addAttribute(new MAttribute(env, "geometryPath", "gp",
                                        env.findDataType("Message")));
            addAttribute(new MAttribute(env, "follow", "f",
                                        env.findDataType("bool")));
            addAttribute(new MAttribute(env, "inverseUp", "ip",
                                        env.findDataType("bool")));
            addAttribute(new MAttribute(env, "inverseFront", "if",
                                        env.findDataType("bool")));
            addAttribute(new MAttribute(env, "frontAxis", "fa",
                                        env.findDataType("enum")));
            addAttribute(new MAttribute(env, "upAxis", "ua",
                                        env.findDataType("enum")));
            addAttribute(new MAttribute(env, "worldUpVector", "wu",
                                        env.findDataType("float3")));
            addAlias("wu.x", "wux");
            addAlias("wu.y", "wuy");
            addAlias("wu.z", "wuz");
            addAttribute(new MAttribute(env, "worldUpType", "wut",
                                        env.findDataType("enum")));
            addAttribute(new MAttribute(env, "worldUpMatrix", "wum",
                                        env.findDataType("matrix")));
            addAttribute(new MAttribute(env, "orientMatrix", "om",
                                        env.findDataType("matrix")));
            addAttribute(new MAttribute(env, "bank", "b",
                                        env.findDataType("bool")));
            addAttribute(new MAttribute(env, "bankScale", "bs",
                                        env.findDataType("double")));
            addAttribute(new MAttribute(env, "bankLimit", "bl",
                                        env.findDataType("double")));
            addAttribute(new MAttribute(env, "fractionMode", "fm",
                                        env.findDataType("bool")));
        }
        protected void initNode(MNode result) {
            MFloat w = (MFloat)result.getAttr("bl");
            w.set(90);
            w = (MFloat)result.getAttr("bs");
            w.set(1);
            MFloat3 wu = (MFloat3)result.getAttr("wu");
            wu.set(0, 1, 0);
            MInt e = (MInt)result.getAttr("wut");
            e.set(3);
            e = (MInt)result.getAttr("ua");
            e.set(2);
            e = (MInt)result.getAttr("fa");
            e.set(1);
        }
    }

    static class UvChooser extends MNodeType {
        UvChooser(final MEnv env) {
            super(env, "uvChooser");
            addSuperType(env.findNodeType("dependNode"));
            addAttribute(new MAttribute(env, "VertexUvOne", "vt1",
                                        env.findDataType("float2")));
            addAlias("vt1.x", "t1u");
            addAlias("vt1.y", "t1v");
            addAttribute(new MAttribute(env, "VertexUvTwo", "vt2",
                                        env.findDataType("float2")));
            addAlias("vt2.x", "t2u");
            addAlias("vt2.y", "t2v");
            addAttribute(new MAttribute(env, "VertexUvThree", "vt3",
                                        env.findDataType("float2")));
            addAlias("vt3.x", "t3u");
            addAlias("vt3.y", "t3v");

            addAttribute(new MAttribute(env, "outVertexUvOne", "ov1",
                                        env.findDataType("float2")));
            addAlias("ov1.x", "o1u");
            addAlias("ov1.y", "o1v");
            addAttribute(new MAttribute(env, "outVertexUvTwo", "ov2",
                                        env.findDataType("float2")));
            addAlias("ov2.x", "o2u");
            addAlias("ov2.y", "o2v");
            addAttribute(new MAttribute(env, "outVertexUvThree", "ov3",
                                        env.findDataType("float2")));

            addAlias("ov3.x", "o3u");
            addAlias("ov3.y", "o3v");
            addAttribute(new MAttribute(env, "outUv", "ouv",
                                        env.findDataType("float2")));
            addAlias("ouv.x", "ou");
            addAlias("ouv.y", "ov");
            addAttribute(new MAttribute(env, "uvSets", "uvs",
                                        new MArrayType(env, env.findDataType("string"))));
        }
    }

    static class Place2dTexture extends MNodeType {
        Place2dTexture(final MEnv env) {
            super(env, "place2dTexture");
            addSuperType(env.findNodeType("dependNode"));
            addAttribute(new MAttribute(env, "VertexUvOne", "vt1",
                                        env.findDataType("float2")));
            addAlias("vt1.x", "t1u");
            addAlias("vt1.y", "t1v");
            addAttribute(new MAttribute(env, "VertexUvTwo", "vt2",
                                        env.findDataType("float2")));
            addAlias("vt2.x", "t2u");
            addAlias("vt2.y", "t2v");
            addAttribute(new MAttribute(env, "VertexUvThree", "vt3",
                                        env.findDataType("float2")));
            addAlias("vt3.x", "t3u");
            addAlias("vt3.y", "t3v");

            addAttribute(new MAttribute(env, "uvCoord", "uv",
                                        env.findDataType("float2")));
            addAlias("uv.x", "u");
            addAlias("uv.y", "v");
        }
    }

    static class LayeredTexture extends MNodeType {
        LayeredTexture(final MEnv env) {
            super(env, "layeredTexture");
            addSuperType(env.findNodeType("dependNode"));
            addAttribute(new MAttribute(env, "outColor", "oc",
                                        env.findDataType("float3")));
            addAlias("oc.x", "ocr");
            addAlias("oc.y", "ocg");
            addAlias("oc.z", "ocb");
            addAttribute(new MAttribute(env, "outAlpha", "oa",
                                        env.findDataType("float")));
            addAttribute(new MAttribute(env, "outTransparency", "ot",
                                        env.findDataType("float3")));
            addAlias("ot.x", "otr");
            addAlias("ot.y", "otg");
            addAlias("ot.z", "otb");
            addAttribute(new MAttribute(env, "inputs", "cs",
                                        new MArrayType(env,
                                                new MCompoundType(env,
                                                                     "layeredTexture.cs") {
                                                       {
                                                           addField("c",
                                                                    new MCompoundType(env, "layeredTexture.cs.c") {
                                                                        {
                                                                            addField("cr", env.findDataType("float"), null);
                                                                            addField("cg", env.findDataType("float"), null);
                                                                            addField("cb", env.findDataType("float"), null);
                                                                        }
                                                                    },
                                                                    null);

                                                           addField("a",
                                                                    env.findDataType("float"),
                                                                    null);
                                                           addField("bm",
                                                                    env.findDataType("enum"),
                                                                    null);
                                                       }
                                                   })));
        }
    }

    static class ChoiceNodeType extends MNodeType {
        ChoiceNodeType(MEnv env) {
            super(env, "choice");
            addSuperType(env.findNodeType("dependNode"));
            addAttribute(new MAttribute(env, "selector", "s",
                                        env.findDataType("integer")));
            addAttribute(new MAttribute(env, "input", "i",
                                        new MArrayType(env, env.findDataType("Message"))));
            addAttribute(new MAttribute(env, "output", "o",
                                        env.findDataType("Message")));
        }
    }

    static class AbstractBaseCreateType extends MNodeType {
        AbstractBaseCreateType(MEnv env) {
            super(env, "abstractBaseCreate");
            addSuperType(env.findNodeType("dependNode"));
        }
    }

    static class TransformGeometryType extends MNodeType {
        TransformGeometryType(MEnv env) {
            super(env, "transformGeometry");
            addSuperType(env.findNodeType("abstractBaseCreate"));
            addAttribute(new MAttribute(env, "inputGeometry", "ig",
                                        env.findDataType("Message")));
            addAttribute(new MAttribute(env, "transform", "txf",
                                        env.findDataType("matrix")));
            addAttribute(new MAttribute(env, "invertTransform", "itf",
                                        env.findDataType("bool")));
            addAttribute(new MAttribute(env, "freezeNormals", "fn",
                                        env.findDataType("bool")));
            addAttribute(new MAttribute(env, "outputGeometry", "og",
                                        env.findDataType("Message")));
        }
    }


    static class PolyBaseType extends MNodeType {
        PolyBaseType(MEnv env) {
            super(env, "polyBase");
            addSuperType(env.findNodeType("dependNode"));
            addAttribute(new MAttribute(env, "output", "out",
                                        env.findDataType("Message")));
        }
    }


    static class PolyModifierType extends MNodeType {
        PolyModifierType(MEnv env) {
            super(env, "polyModifier");
            addSuperType(env.findNodeType("polyBase"));
            addAttribute(new MAttribute(env, "inputPolymesh", "ip",
                                        env.findDataType("Message")));
            addAttribute(new MAttribute(env, "inputComponents", "ics",
                                        env.findDataType("componentList")));
        }
    }

    static class PolyNormalType extends MNodeType {
        PolyNormalType(MEnv env) {
            super(env, "polyNormal");
            addSuperType(env.findNodeType("polyModifier"));
            addAttribute(new MAttribute(env, "normalMode", "nm",
                                        env.findDataType("enum")));
            addAttribute(new MAttribute(env, "userNormalMode", "unm",
                                        env.findDataType("bool")));
        }

        protected void initNode(MNode result) {
            MBool f  = (MBool)result.getAttr("unm");
            f.set(true);
        }
    }


    static class PolyCreatorType extends MNodeType {
        PolyCreatorType(MEnv env) {
            super(env, "polyCreator");
            addSuperType(env.findNodeType("polyBase"));
        }
    }

    static class PolyPrimitiveType extends MNodeType {
        PolyPrimitiveType(MEnv env) {
            super(env, "polyPrimitive");
            addSuperType(env.findNodeType("polyCreator"));
        }
    }

    static class PolyCubeType extends MNodeType {
        PolyCubeType(MEnv env) {
            super(env, "polyCube");
            addSuperType(env.findNodeType("polyPrimitive"));
            addAttribute(new MAttribute(env, "width", "w",
                                        env.findDataType("float")));
            addAttribute(new MAttribute(env, "height", "h",
                                        env.findDataType("float")));
            addAttribute(new MAttribute(env, "depth", "d",
                                        env.findDataType("float")));
        }
        protected void initNode(MNode result) {
            MFloat w = (MFloat)result.getAttr("w");
            MFloat h = (MFloat)result.getAttr("h");
            MFloat d = (MFloat)result.getAttr("d");
            w.set(1);
            h.set(1);
            d.set(1);
        }
    }

    static class PolyCylinderType extends MNodeType {
        PolyCylinderType(MEnv env) {
            super(env, "polyCylinder");
            addSuperType(env.findNodeType("polyPrimitive"));
            addAttribute(new MAttribute(env, "radius", "r",
                                        env.findDataType("float")));
            addAttribute(new MAttribute(env, "height", "h",
                                        env.findDataType("float")));

        }
        protected void initNode(MNode result) {
            MFloat r = (MFloat)result.getAttr("r");
            MFloat h = (MFloat)result.getAttr("h");
            r.set(1);
            h.set(2);
        }
    }

    static class PolySphereType extends MNodeType {
        PolySphereType(MEnv env) {
            super(env, "polySphere");
            addSuperType(env.findNodeType("polyPrimitive"));
            addAttribute(new MAttribute(env, "radius", "r",
                                        env.findDataType("float")));
        }
        protected void initNode(MNode result) {
            MFloat r = (MFloat)result.getAttr("r");
            r.set(1);
        }
    }

    static class SurfaceShaderType extends MNodeType {
        SurfaceShaderType(MEnv env) {
            super(env, "surfaceShader");
            addSuperType(env.findNodeType("dependNode"));
            addAttribute(new MAttribute(env, "color", "c",
                                        env.findDataType("float3")));
            addAlias("cr", "c.x");
            addAlias("cg", "c.y");
            addAlias("cb", "c.z");
            addAttribute(new MAttribute(env, "outColor", "oc",
                                        env.findDataType("float3")));
            addAttribute(new MAttribute(env, "outTransparency", "ot",
                                        env.findDataType("float3")));
            addAttribute(new MAttribute(env, "outGlowColor", "ogc",
                                        env.findDataType("float3")));
            addAttribute(new MAttribute(env, "outMatteOpacity", "omo",
                                        env.findDataType("float3")));
            addAttribute(new MAttribute(env, "diffuse", "dc",
                                        env.findDataType("float")));
            addAttribute(new MAttribute(env, "ambientColor", "ambc",
                                        env.findDataType("float3")));
            addAlias("acr", "ambc.x");
            addAlias("acg", "ambc.y");
            addAlias("acb", "ambc.z");
            addAttribute(new MAttribute(env, "incandescence", "ic",
                                        env.findDataType("float3")));
            addAlias("ir", "ic.x");
            addAlias("ig", "ic.y");
            addAlias("ib", "ic.z");
            addAttribute(new MAttribute(env, "transparency", "it",
                                        env.findDataType("float3")));
            addAlias("itr", "it.x");
            addAlias("itg", "it.y");
            addAlias("itb", "it.z");
            addAttribute(new MAttribute(env, "normalCamera", "n",
                                        env.findDataType("float3")));
            addAlias("nx", "n.x");
            addAlias("ny", "n.y");
            addAlias("nz", "n.z");
            addAttribute(new MAttribute(env, "transparency", "it",
                                        env.findDataType("float3")));
            addAlias("itr", "it.x");
            addAlias("itg", "it.y");
            addAlias("itb", "it.z");
        }

        protected void initNode(MNode node) {
            MFloat dc = (MFloat)node.getAttr("dc");
            dc.set(.8f);
            //            MFloat3 ambc = (MFloat3)node.getAttr("ambc");
            //            ambc.set(.2f, .2f, .2f);
            MFloat3 c = (MFloat3)node.getAttr("c");
            c.set(.5f, .5f, .5f);
            //            MFloat3 ic = (MFloat3)node.getAttr("ic");

        }
    }

    static class LambertType extends MNodeType {
        LambertType(MEnv env) {
            super(env, "lambert");
            addSuperType(env.findNodeType("dependNode"));
            addAttribute(new MAttribute(env, "color", "c",
                                        env.findDataType("float3")));
            addAlias("cr", "c.x");
            addAlias("cg", "c.y");
            addAlias("cb", "c.z");
            addAttribute(new MAttribute(env, "outColor", "oc",
                                        env.findDataType("float3")));
            addAttribute(new MAttribute(env, "outTransparency", "ot",
                                        env.findDataType("float3")));
            addAttribute(new MAttribute(env, "outGlowColor", "ogc",
                                        env.findDataType("float3")));
            addAttribute(new MAttribute(env, "outMatteOpacity", "omo",
                                        env.findDataType("float3")));
            addAttribute(new MAttribute(env, "diffuse", "dc",
                                        env.findDataType("float")));
            addAttribute(new MAttribute(env, "ambientColor", "ambc",
                                        env.findDataType("float3")));
            addAlias("acr", "ambc.x");
            addAlias("acg", "ambc.y");
            addAlias("acb", "ambc.z");
            addAttribute(new MAttribute(env, "incandescence", "ic",
                                        env.findDataType("float3")));
            addAlias("ir", "ic.x");
            addAlias("ig", "ic.y");
            addAlias("ib", "ic.z");
            addAttribute(new MAttribute(env, "transparency", "it",
                                        env.findDataType("float3")));
            addAlias("itr", "it.x");
            addAlias("itg", "it.y");
            addAlias("itb", "it.z");
            addAttribute(new MAttribute(env, "normalCamera", "n",
                                        env.findDataType("float3")));
            addAlias("nx", "n.x");
            addAlias("ny", "n.y");
            addAlias("nz", "n.z");
            addAttribute(new MAttribute(env, "transparency", "it",
                                        env.findDataType("float3")));
            addAlias("itr", "it.x");
            addAlias("itg", "it.y");
            addAlias("itb", "it.z");
        }

        protected void initNode(MNode node) {
            MFloat dc = (MFloat)node.getAttr("dc");
            dc.set(.8f);
            //            MFloat3 ambc = (MFloat3)node.getAttr("ambc");
            //            ambc.set(.2f, .2f, .2f);
            MFloat3 c = (MFloat3)node.getAttr("c");
            c.set(.5f, .5f, .5f);
            //            MFloat3 ic = (MFloat3)node.getAttr("ic");

        }
    }

    static class ReflectType extends MNodeType {
        ReflectType(MEnv env) {
            super(env, "reflect");
            addSuperType(env.findNodeType("lambert"));
            addAttribute(new MAttribute(env, "specularColor", "sc",
                                        env.findDataType("float3")));
            addAlias("sr", "sc.x");
            addAlias("sg", "sc.y");
            addAlias("sb", "sc.z");
            addAttribute(new MAttribute(env, "reflectivity", "rfl",
                                        env.findDataType("float")));
        }

        protected void initNode(MNode result) {
            MFloat3 sc = (MFloat3)result.getAttr("sc");
            sc.set(.5f, .5f, .5f);
            MFloat rfl = (MFloat)result.getAttr("rfl");
            rfl.set(.5f);
        }
    }

    static class PhongNodeType extends MNodeType {
        PhongNodeType(MEnv env) {
            super(env, "phong");
            addSuperType(env.findNodeType("reflect"));
            addAttribute(new MAttribute(env,
                                        "cosinePower",
                                        "cp",
                                        env.findDataType("float")));
        }

        protected void initNode(MNode result) {
            MFloat cp = (MFloat)result.getAttr("cp");
            cp.set(20f);
        }
    }

    static class PhongENodeType extends MNodeType {
        PhongENodeType(MEnv env) {
            super(env, "phongE");
            addSuperType(env.findNodeType("reflect"));
            addAttribute(new MAttribute(env,
                                        "cosinePower",
                                        "cp",
                                        env.findDataType("float")));
        }

        protected void initNode(MNode result) {
            MFloat cp = (MFloat)result.getAttr("cp");
            cp.set(20f);
        }
    }

    static class BlinnNodeType extends MNodeType {
        BlinnNodeType(MEnv env) {
            super(env, "blinn");
            addSuperType(env.findNodeType("reflect"));
            addAttribute(new MAttribute(env,
                                        "eccentricity",
                                        "ec",
                                        env.findDataType("float")));
        }

        protected void initNode(MNode result) {
            MFloat ec = (MFloat)result.getAttr("ec");
            ec.set(.3f);
        }
    }

    static class Bump2dNodeType extends MNodeType {
        Bump2dNodeType(MEnv env) {
            super(env, "bump2d");
            addSuperType(env.findNodeType("dependNode"));
            addAttribute(new MAttribute(env, "bumpValue", "bv",
                                        env.findDataType("float")));
            addAttribute(new MAttribute(env, "outNormal", "o",
                                        env.findDataType("float3")));
        }
    }

    static class Texture2DType extends MNodeType {
        Texture2DType(MEnv env) {
            super(env, "texture2d");
            addSuperType(env.findNodeType("dependNode"));
            addAttribute(new MAttribute(env, "outColor", "oc",
                                        env.findDataType("float3")));
            addAttribute(new MAttribute(env, "outAlpha", "oa",
                                        env.findDataType("float")));
        }
    }

    static class FileType extends MNodeType {
        FileType(MEnv env) {
            super(env, "file");
            addSuperType(env.findNodeType("texture2d"));
            addAttribute(new MAttribute(env, "fileTextureName", "ftn",
                                        env.findDataType("string")));
            addAttribute(new MAttribute(env, "outTransparency", "ot",
                                        env.findDataType("float3")));
            addAlias("otr", "ot.x");
            addAlias("otg", "ot.y");
            addAlias("otb", "ot.z");
        }
    }

    static class PsdFileTex extends MNodeType {
        PsdFileTex(MEnv env) {
            super(env, "psdFileTex");
            addSuperType(env.findNodeType("file"));
            addAttribute(new MAttribute(env, "layerSetName", "lsn",
                                        env.findDataType("string")));
            addAttribute(new MAttribute(env, "layerSets", "lys",
                                        env.findDataType("string[]")));
            addAttribute(new MAttribute(env, "alpha", "alp",
                                        env.findDataType("string")));
            addAttribute(new MAttribute(env, "alphaList", "als",
                                        env.findDataType("string[]")));
        }
    }

    static class DagNodeType extends MNodeType {
        static class iogType extends MArrayType {
            static class ogType extends MCompoundType {
                public ogType(MEnv env) {
                    super(env, "dagNode.iog.og");
                    addField("gcl", env.findDataType("componentList"), env.createData("componentList"));
                    addField("gid", env.findDataType("int"), env.createData("int"));
                    addField("gco", env.findDataType("short"), env.createData("short"));
                }
            }

            static class ogArrayType extends MArrayType {
                public ogArrayType(MEnv env) {
                    super(env, new ogType(env));
                }
            }

            static class ogsType extends MCompoundType {
                public ogsType(MEnv env) {
                    super(env, "dagNode.iog.og[]");
                    addField("og", new ogArrayType(env), null);
                }
            }


            public iogType(MEnv env) {
                super(env, new ogsType(env));
            }
        }
        DagNodeType(MEnv env) {
            super(env, "dagNode");
            addAttribute(new MAttribute(env, "instObjGroups", "iog",
                                        new iogType(env)));
            addAttribute(new MAttribute(env, "parentMatrix", "pm", env.findDataType("matrix")));
            addAttribute(new MAttribute(env, "parentInverseMatrix", "pim", env.findDataType("matrix")));
            addAttribute(new MAttribute(env, "visibility", "v", env.findDataType("bool")));
            addAttribute(new MAttribute(env, "intermediateObject", "io", env.findDataType("bool")));
        }

        protected void initNode(MNode result) {
            MBool v = (MBool) result.getAttr("v");
            v.set(true);
        }
    }

    //----------------------------------------------------------------------
    // Utility node types
    //

    static class UnitConversionType extends MNodeType {
        UnitConversionType(MEnv env) {
            super(env, "unitConversion");
            addSuperType(env.findNodeType("dependNode"));
            addAttribute(new MAttribute(env, "input", "i", env.findDataType("Message")));
            addAttribute(new MAttribute(env, "output", "o", env.findDataType("Message")));
            addAttribute(new MAttribute(env, "conversionFactor", "cf", env.findDataType("double")));
        }
        protected void initNode(MNode result) {
            MFloat cf = (MFloat)result.getAttr("cf");
            cf.set(1f);
        }
    }


    static class BlendType extends MNodeType {
        BlendType(MEnv env) {
            super(env, "blend");
            addSuperType(env.findNodeType("dependNode"));
            addAttribute(new MAttribute(env, "input", "i", env.findDataType("double[]")));
            addAttribute(new MAttribute(env, "output", "o", env.findDataType("double")));
            addAttribute(new MAttribute(env, "current", "c", env.findDataType("integer")));
        }
    }

    static class BlendColorsType extends MNodeType {
        BlendColorsType(MEnv env) {
            super(env, "blendColors");
            addSuperType(env.findNodeType("dependNode"));
            addAttribute(new MAttribute(env, "blender", "b", env.findDataType("double")));
            addAttribute(new MAttribute(env, "color1", "c1", env.findDataType("float3")));
            addAlias("color1R", "c1.x");
            addAlias("color1G", "c1.y");
            addAlias("color1B", "c1.z");
            addAlias("c1r", "c1.x");
            addAlias("c1g", "c1.y");
            addAlias("c1b", "c1.z");
            addAttribute(new MAttribute(env, "color2", "c2", env.findDataType("float3")));
            addAlias("color2R", "c2.x");
            addAlias("color2G", "c2.y");
            addAlias("color2B", "c2.z");
            addAlias("c2r", "c2.x");
            addAlias("c2g", "c2.y");
            addAlias("c2b", "c2.z");
            addAttribute(new MAttribute(env, "output", "op", env.findDataType("float3")));
            addAlias("outputR", "op.x");
            addAlias("outputG", "op.y");
            addAlias("outputB", "op.z");
            addAlias("opr", "op.x");
            addAlias("opg", "op.y");
            addAlias("opb", "op.z");
        }
        protected void initNode(MNode result) {
            MFloat b = (MFloat)result.getAttr("b");
            b.set(0.5f);
            MFloat3 c1 = (MFloat3) result.getAttr("c1");
            c1.set(1, 0, 0);
            MFloat3 c2 = (MFloat3) result.getAttr("c2");
            c2.set(0, 0, 1);
        }
    }

    static class BlendWeightedType extends MNodeType {
        BlendWeightedType(MEnv env) {
            super(env, "blendWeighted");
            addSuperType(env.findNodeType("blend"));
            addAttribute(new MAttribute(env, "weight", "w", env.findDataType("float[]")));
        }
    }

    static class MultiplyDivideType extends MNodeType {
        MultiplyDivideType(MEnv env) {
            super(env, "multiplyDivide");
            addSuperType(env.findNodeType("dependNode"));
            /*
Operation controls the operation performed by this node. The settings are:
No operation: Output is set to equal Input 1. Input 2 is completely ignored.
Multiply: Output is set to equal Input 1 times Input 2.

Divide: Output is set to equal Input 1 divided by Input 2.

Power: Output is set to equal Input 1 raised to the power of Input 2.

Tip: To calculate the square root of Input 1, set Operation to Power, and set Input 2 to 0.5
             */
            addAttribute(new MAttribute(env, "operation", "op", env.findDataType("enum")));
            addAttribute(new MAttribute(env, "input1", "i1", env.findDataType("float3")));
            addAlias("i1x", "i1.x");
            addAlias("i1y", "i1.y");
            addAlias("i1z", "i1.z");
            addAttribute(new MAttribute(env, "input2", "i2", env.findDataType("float3")));
            addAlias("i2x", "i2.x");
            addAlias("i2y", "i2.y");
            addAlias("i2z", "i2.z");
            addAttribute(new MAttribute(env, "output", "o", env.findDataType("float3")));
            addAlias("ox", "o.x");
            addAlias("oy", "o.y");
            addAlias("oz", "o.z");

        }
        protected void initNode(MNode result) {
            MInt op = (MInt)result.getAttr("op");
            op.set(1);
        }
    }

    static class PlusMinusAverageType extends MNodeType {
        PlusMinusAverageType(MEnv env) {
            super(env, "plusMinusAverage");
            addSuperType(env.findNodeType("dependNode"));
            /*
Operation controls the mathematical operation done by this node. It has four possible values:
No operation: The first input is copied to the output. All other inputs are ignored.

Sum: All of the inputs are added together, and the output is set to their sum.

Subtract: The output is set to the first input, minus all the other inputs.

Average: The output is set to the sum of all the inputs, divided by the number of inputs.
             */
            addAttribute(new MAttribute(env, "operation", "op", env.findDataType("enum")));
            addAttribute(new MAttribute(env, "input1D", "i1", env.findDataType("float[]")));
            addAttribute(new MAttribute(env, "input2D", "i2", env.findDataType("float2[]")));
            addAttribute(new MAttribute(env, "input3D", "i3", env.findDataType("float3[]")));
            addAlias("i2x", "i2.x");
            addAlias("i2y", "i2.y");

            addAlias("i3x", "i3.x");
            addAlias("i3y", "i3.y");
            addAlias("i3z", "i3.z");

            addAttribute(new MAttribute(env, "output1D", "o1", env.findDataType("float")));
            addAttribute(new MAttribute(env, "output2D", "o2", env.findDataType("float2")));
            addAttribute(new MAttribute(env, "output3D", "o3", env.findDataType("float3")));
            addAlias("o2x", "o2.x");
            addAlias("o2y", "o2.y");

            addAlias("o3x", "o3.x");
            addAlias("o3y", "o3.y");
            addAlias("o3z", "o3.z");


        }
        protected void initNode(MNode result) {
            MInt op = (MInt)result.getAttr("op");
            op.set(1);
        }
    }

    static class ReverseType extends MNodeType {
        ReverseType(MEnv env) {
            super(env, "reverse");
            addSuperType(env.findNodeType("dependNode"));
            addAttribute(new MAttribute(env, "input", "i", env.findDataType("float3")));
            addAlias("ix", "i.x");
            addAlias("iy", "i.y");
            addAlias("iz", "i.z");
            addAttribute(new MAttribute(env, "output", "o", env.findDataType("float3")));
            addAlias("ox", "o.x");
            addAlias("oy", "o.y");
            addAlias("oz", "o.z");

        }
    }

    static class ClampType extends MNodeType {
        ClampType(MEnv env) {
            super(env, "clamp");
            addSuperType(env.findNodeType("dependNode"));
            addAttribute(new MAttribute(env, "min", "mn", env.findDataType("float3")));
            addAlias("mnr", "mn.x");
            addAlias("mng", "mn.y");
            addAlias("mnb", "mn.z");
            addAttribute(new MAttribute(env, "com/javafx/importers/max", "mx", env.findDataType("float3")));
            addAlias("mxr", "mx.x");
            addAlias("mxg", "mx.y");
            addAlias("mxb", "mx.z");
            addAttribute(new MAttribute(env, "input", "ip", env.findDataType("float3")));
            addAlias("ipr", "ip.x");
            addAlias("ipg", "ip.y");
            addAlias("ipb", "ip.z");
            addAttribute(new MAttribute(env, "output", "op", env.findDataType("float3")));
            addAlias("opr", "op.x");
            addAlias("opg", "op.y");
            addAlias("opb", "op.z");
        }
    }


    static class ConditionType extends MNodeType {
        ConditionType(MEnv env) {
            super(env, "condition");
            addSuperType(env.findNodeType("dependNode"));
            /*
               Operation is the test that is performed between the First Term and Second Term attributes. If the test is true, Out Color is set to ColorIfTrue. If it is false, Out Color is set to ColorIfFalse.
The possible operations are:

Equal
Not Equal
Greater Than
Greater Or Equal
Less Than
Less or Equal */
            addAttribute(new MAttribute(env, "operation", "op", env.findDataType("enum")));
            addAttribute(new MAttribute(env, "firstTerm", "ft", env.findDataType("float")));
            addAttribute(new MAttribute(env, "secondTerm", "st", env.findDataType("float")));
            addAttribute(new MAttribute(env, "colorIfTrue", "ct", env.findDataType("float3")));
            addAttribute(new MAttribute(env, "colorIfFalse", "cf", env.findDataType("float3")));
            addAttribute(new MAttribute(env, "outColor", "oc", env.findDataType("float3")));
            addAlias("ctr", "ct.x");
            addAlias("ctg", "ct.y");
            addAlias("ctb", "ct.z");
            addAlias("cfr", "cf.x");
            addAlias("cfg", "cf.y");
            addAlias("cfb", "cf.z");
            addAlias("ocr", "oc.x");
            addAlias("ocg", "oc.y");
            addAlias("ocb", "oc.z");
        }
        protected void initNode(MNode result) {
            MFloat3 cf = (MFloat3)result.getAttr("cf");
            cf.set(1, 1, 1);
        }
    }

    //
    // End utility node types
    //----------------------------------------------------------------------

    static class ShapeType extends MNodeType {
        ShapeType(MEnv env) {
            super(env, "shape");
            addSuperType(env.findNodeType("dagNode"));
        }
    }

    static class LightType extends MNodeType {
        LightType(MEnv env) {
            super(env, "light");
            addSuperType(env.findNodeType("shape"));
            addAttribute(new MAttribute(env, "color", "cl",
                                        env.findDataType("float3")));
            addAlias("cr", "cl.x");
            addAlias("cg", "cl.y");
            addAlias("cb", "cl.z");
            addAttribute(new MAttribute(env, "intensity", "in",
                                        env.findDataType("float")));
            addAttribute(new MAttribute(env, "li", "li",
                                        env.findDataType("float3"))); // ???
        }

        protected void initNode(MNode result) {
            MFloat3 cl = (MFloat3)result.getAttr("cl");
            cl.set(1, 1, 1);
            MFloat in = (MFloat)result.getAttr("in");
            in.set(1f);
        }
    }

    static class RenderLightType extends MNodeType {
        RenderLightType(MEnv env) {
            super(env, "renderLight");
            addSuperType(env.findNodeType("light"));
        }
    }

    static class NonAmbientLightShapeNodeType extends MNodeType {
        NonAmbientLightShapeNodeType(MEnv env) {
            super(env, "nonAmbientLightShapeNode");
            addSuperType(env.findNodeType("renderLight"));
            addAttribute(new MAttribute(env, "decayRate", "de",
                                        env.findDataType("enum")));
        }
    }

    static class NonExtendedLightShapeNodeType extends MNodeType {
        NonExtendedLightShapeNodeType(MEnv env) {
            super(env, "nonExtendedLightShapeNode");
            addSuperType(env.findNodeType("nonAmbientLightShapeNode"));
        }
    }

    static class PointLightType extends MNodeType {
        PointLightType(MEnv env) {
            super(env, "pointLight");
            addSuperType(env.findNodeType("nonExtendedLightShapeNode"));
        }
    }

    static class DirectionalLightType extends MNodeType {
        DirectionalLightType(MEnv env) {
            super(env, "directionalLight");
            addSuperType(env.findNodeType("nonExtendedLightShapeNode"));
        }
    }

    static class SpotLightType extends MNodeType {
        SpotLightType(MEnv env) {
            super(env, "spotLight");
            addSuperType(env.findNodeType("nonExtendedLightShapeNode"));
            addAttribute(new MAttribute(env, "coneAngle", "ca",
                                        env.findDataType("double")));
            addAttribute(new MAttribute(env, "penumbraAngle", "pa",
                                        env.findDataType("double")));
            addAttribute(new MAttribute(env, "dropoff", "dro",
                                        env.findDataType("double")));
        }
        protected void initNode(MNode result) {
            MFloat ca = (MFloat)result.getAttr("ca");
            ca.set(40);
        }
    }

    static class CameraType extends MNodeType {
        CameraType(MEnv env) {
            super(env, "camera");
            addSuperType(env.findNodeType("shape"));
            addAttribute(new MAttribute(env, "renderable", "rnd",
                                        env.findDataType("bool")));
            addAttribute(new MAttribute(env, "cameraAperture", "cap",
                                        env.findDataType("double2")));
            addAlias("horizontalFilmAperture", "cap.x");
            addAlias("hfa", "cap.x");
            addAlias("verticalFilmAperture", "cap.y");
            addAlias("vfa", "cap.y");
            // 0 = fill, 1 = horizontal fit, 2 = vertical fit, 3 = overscan fit
            addAttribute(new MAttribute(env, "filmFit", "ff",
                                        env.findDataType("int")));
            addAttribute(new MAttribute(env, "imageName", "imn",
                                        env.findDataType("string")));
            addAttribute(new MAttribute(env, "depthName", "den",
                                        env.findDataType("string")));
            addAttribute(new MAttribute(env, "maskName", "man",
                                        env.findDataType("string")));
            // In centimeters
            addAttribute(new MAttribute(env, "centerOfInterest", "coi",
                                        env.findDataType("double")));
            // In millimeters
            addAttribute(new MAttribute(env, "focalLength", "fl",
                                        env.findDataType("double")));
            addAttribute(new MAttribute(env, "lensSqueezeRatio", "lsr",
                                        env.findDataType("double")));
            // In centimeters
            addAttribute(new MAttribute(env, "orthographicWidth", "ow",
                                        env.findDataType("double")));
            addAttribute(new MAttribute(env, "orthographic", "o",
                                        env.findDataType("bool")));

            // In centimeters
            addAttribute(new MAttribute(env, "farClipPlane", "fcp",
                                        env.findDataType("double")));
            addAttribute(new MAttribute(env, "nearClipPlane", "ncp",
                                        env.findDataType("double")));
        }

        protected void initNode(MNode result) {
            MBool renderable = (MBool)result.getAttr("rnd");
            renderable.set(true);
            MInt filmFit = (MInt) result.getAttr("ff");
            filmFit.set(1); // horizontal fit
            MFloat centerOfInterest = (MFloat) result.getAttr("coi");
            centerOfInterest.set(5.0f);
            MFloat focalLength = (MFloat) result.getAttr("fl");
            focalLength.set(35.0f);
            MFloat orthographicWidth = (MFloat) result.getAttr("ow");
            orthographicWidth.set(10.0f);
            MFloat2 cameraAperture = (MFloat2) result.getAttr("cap");
            cameraAperture.set(3.6f, 2.4f);
            MFloat lensSqueezeRatio = (MFloat) result.getAttr("lsr");
            lensSqueezeRatio.set(1.0f);
            MFloat f = (MFloat)result.getAttr("fcp");
            f.set(1000);
            f = (MFloat)result.getAttr("ncp");
            f.set(.1f);
        }
    }

    static class RigidConstraint extends MNodeType {
        RigidConstraint(MEnv env) {
            super(env, "rigidConstraint");
            addSuperType(env.findNodeType("transform"));

            addAttribute(new MAttribute(env, "rigidBody1", "rb1", new MArrayType(env, env.findDataType("Message"))));
            addAttribute(new MAttribute(env, "rigidBody2", "rb2", new MArrayType(env, env.findDataType("Message"))));

            addAttribute(new MAttribute(env, "constraintType", "typ", env.findDataType("enum")));
            addAttribute(new MAttribute(env, "initialPosition", "ip", env.findDataType("double3")));
            addAlias("ipx", "ip.x");
            addAlias("ipy", "ip.y");
            addAlias("ipz", "ip.z");
            addAttribute(new MAttribute(env, "initialOrientation", "ino", env.findDataType("double3")));
            addAlias("iox", "inp.x");
            addAlias("ioy", "inp.y");
            addAlias("ioz", "inp.z");
            addAttribute(new MAttribute(env, "springDamping", "dmp", env.findDataType("double")));
            addAttribute(new MAttribute(env, "springStiffness", "sst", env.findDataType("double")));
            addAttribute(new MAttribute(env, "springRestLength", "srl", env.findDataType("double")));
            addAttribute(new MAttribute(env, "choice", "chc", env.findDataType("integer")));
        }

        protected void initNode(MNode result) {
            MInt typ = (MInt)result.getAttr("typ");
            typ.set(1);
            MFloat sst = (MFloat)result.getAttr("sst");
            sst.set(5);
            MFloat dmp = (MFloat)result.getAttr("dmp");
            dmp.set(.1f);
            MFloat srl = (MFloat)result.getAttr("srl");
            srl.set(1);
        }
    }


    static class RigidBodyType extends MNodeType {
        RigidBodyType(final MEnv env) {
            super(env, "rigidBody");
            addSuperType(env.findNodeType("shape"));
            addAttribute(new MAttribute(env, "inputGeometryMsg", "igm", new MArrayType(env, env.findDataType("Message"))));
            addAttribute(new MAttribute(env, "initialPosition", "ip", env.findDataType("double3")));
            addAlias("ipx", "ip.x");
            addAlias("ipy", "ip.y");
            addAlias("ipz", "ip.z");
            addAttribute(new MAttribute(env, "mass", "mas", env.findDataType("double")));
            addAttribute(new MAttribute(env, "bounciness", "b", env.findDataType("double")));
            addAttribute(new MAttribute(env, "staticFriction", "sf", env.findDataType("double")));
            addAttribute(new MAttribute(env, "dynamicFriction", "df", env.findDataType("double")));
            addAttribute(new MAttribute(env, "centerOfMass", "com", env.findDataType("double3")));
            addAttribute(new MAttribute(env, "standin", "si", env.findDataType("enum")));
            addAlias("cmx", "com.x");
            addAlias("cmy", "com.y");
            addAlias("cmz", "com.z");
            addAttribute(new MAttribute(env, "initialVelocity", "iv", env.findDataType("double3")));
            addAlias("ivx", "iv.x");
            addAlias("ivy", "iv.y");
            addAlias("ivz", "iv.z");
            addAttribute(new MAttribute(env, "initialSpin", "is", env.findDataType("double3")));
            addAlias("is.x", "is.x");
            addAlias("is.y", "is.y");
            addAlias("is.z", "is.z");
            addAttribute(new MAttribute(env, "initialOrientation", "ior", env.findDataType("double3")));
            addAlias("iox", "ior.x");
            addAlias("ioy", "ior.y");
            addAlias("ioz", "ior.z");
            addAttribute(new MAttribute(env, "isKinematic", "kin", env.findDataType("bool")));
            addAttribute(new MAttribute(env, "active", "act", env.findDataType("bool")));
            addAttribute(new MAttribute(env, "fieldData", "fld",
                                        new MCompoundType(env, "rigidBody.fild") {
                                            {
                                                addField("fdp",
                                                         env.findDataType("float3[]"),
                                                         null);
                                                addField("fdv",
                                                         env.findDataType("float3[]"),
                                                         null);
                                                addField("fdm",
                                                         env.findDataType("float[]"),
                                                         null);
                                                addField("dt",
                                                         env.findDataType("time"),
                                                         null);
                                            }
                                        }));;

            addAttribute(new MAttribute(env, "inputForce", "ifr",
                                        env.findDataType("float3[]")));
        }


        protected void initNode(MNode result) {
            MBool active = (MBool)result.getAttr("active");
            active.set(true);
            MFloat mass = (MFloat)result.getAttr("mas");
            mass.set(1f);
            MFloat staticFriction = (MFloat)result.getAttr("sf");
            staticFriction.set(.2f);
            MFloat dynamicFriction = (MFloat)result.getAttr("df");
            dynamicFriction.set(.2f);
            MFloat bounciness = (MFloat)result.getAttr("b");
            bounciness.set(.6f);
        }
    }


    static class GeometryShapeType extends MNodeType {
        GeometryShapeType(MEnv env) {
            super(env, "geometryShape");
            addSuperType(env.findNodeType("shape"));
        }
    }

    static class DeformableShapeType extends MNodeType {
        DeformableShapeType(MEnv env) {
            super(env, "deformableShape");
            addSuperType(env.findNodeType("geometryShape"));
        }
    }

    static class ParticleType extends MNodeType {
        ParticleType(MEnv env) {
            super(env, "particle");
            addSuperType(env.findNodeType("deformableShape"));
            addAttribute(new MAttribute(env, "newParticles", "npt",
                                        new MArrayType(env, env.findDataType("Message"))));
            addAttribute(new MAttribute(env, "lifeSpanMode", "lfm", env.findDataType("enum")));
            addAttribute(new MAttribute(env, "lifeSpanRandom", "lfr", env.findDataType("double")));
            addAttribute(new MAttribute(env, "emissionToWorld", "eiw", env.findDataType("bool")));
            addAttribute(new MAttribute(env, "inputForce", "ifc", env.findDataType("float3[]")));

        }

        protected void initNode(MNode result) {
            MBool eiw = (MBool)result.getAttr("eiw");
            eiw.set(true);
        }
    }

    static class ControlPointType extends MNodeType {
        static class UVSet extends MArrayType {
            static class UVSetElement extends MCompoundType {
                public UVSetElement(MEnv env) {
                    super(env, "controlPoint.uvSet");
                    addField("uvsn", env.findDataType("string"), env.createData("string"));
                    addField("uvsp", env.findDataType("float2[]"), env.createData("float2[]"));
                    getFields().put("uvSetName", getField("uvsn"));
                    getFields().put("uvSetPoints", getField("uvsp"));
                }
            }
            public UVSet(MEnv env) {
                super(env, new UVSetElement(env));
            }
        }
        ControlPointType(MEnv env) {
            super(env, "controlPoint");
            addSuperType(env.findNodeType("deformableShape"));
            addAttribute(new MAttribute(env,
                                        "uvSet",
                                        "uvst",
                                        new UVSet(env)));
            addAttribute(new MAttribute(env,
                                        "currentUVSet",
                                        "cuvs",
                                        env.findDataType("string")));
            addAttribute(new MAttribute(env, "tweakLocation", "twl",
                                        env.findDataType("Message")));

        }
    }

    static class CurveShapeType extends MNodeType {
        CurveShapeType(MEnv env) {
            super(env, "curveShape");
            addSuperType(env.findNodeType("controlPoint"));
        }
    }

    static class NurbsCurveType extends MNodeType {
        NurbsCurveType(MEnv env) {
            super(env, "nurbsCurve");
            addSuperType(env.findNodeType("curveShape"));
            addAttribute(new MAttribute(env, "cached", "cc", env.findDataType("nurbsCurve")));
            addAttribute(new MAttribute(env, "worldSpace", "ws",
                                        new MArrayType(env, env.findDataType("nurbsCurve"))));
            addAttribute(new MAttribute(env, "local", "l",
                                        env.findDataType("nurbsCurve")));
        }
    }

    static class SurfaceShapeType extends MNodeType {
        SurfaceShapeType(MEnv env) {
            super(env, "surfaceShape");
            addSuperType(env.findNodeType("controlPoint"));
        }
    }

    static class MeshNodeType extends MNodeType {
        MeshNodeType(MEnv env) {
            super(env, "mesh");
            addSuperType(env.findNodeType("surfaceShape"));
            addAttribute(new MAttribute(env, "vrts", "vt",
                                        env.findDataType("float3[]")));
            addAttribute(new MAttribute(env, "pnts", "pt",
                                        env.findDataType("float3[]")));
            addAttribute(new MAttribute(env, "face", "fc",
                                        env.findDataType("polyFace")));
            addAttribute(new MAttribute(env, "edge", "ed",
                                        env.findDataType("int3[]")));
            addAttribute(new MAttribute(env, "normals", "n",
                                        env.findDataType("float3[]")));
            addAttribute(new MAttribute(env, "inMesh", "i",
                                        env.findDataType("Message")));
            addAttribute(new MAttribute(env, "outMesh", "o",
                                        env.findDataType("Message")));
        }
    }

    static class animCurve extends MNodeType {
        static class ktv extends MCompoundType {
            public ktv(MEnv env) {
                super(env, "animCurve.ktv");
                addField("kt", env.findDataType("time"), env.createData("time"));
                addField("kv", env.findDataType("double"), env.createData("double"));
            }
        }
        animCurve(MEnv env) {
            super(env, "animCurve");
            addSuperType(env.findNodeType("dependNode"));
            addAttribute(new MAttribute(env, "tangentType", "tan", env.findDataType("enum")));
            addAttribute(new MAttribute(env, "keyTimeValue", "ktv",
                                        new MArrayType(env, new ktv(env))));
            addAttribute(new MAttribute(env, "keyTanInX", "kix",
                                        env.findDataType("double[]")));
            addAttribute(new MAttribute(env, "keyTanInY", "kiy",
                                        env.findDataType("double[]")));
            addAttribute(new MAttribute(env, "keyTanOutX", "kox",
                                        env.findDataType("double[]")));
            addAttribute(new MAttribute(env, "keyTanOutY", "koy",
                                        env.findDataType("double[]")));
            addAttribute(new MAttribute(env, "keyTanInType", "kit",
                                        env.findDataType("enum[]")));
            addAttribute(new MAttribute(env, "keyTanOutType", "kot",
                                        env.findDataType("enum[]")));
            addAttribute(new MAttribute(env, "apply", "a",
                                        env.findDataType("function")));
        }
    }

    static class animCurveTA extends MNodeType {
        animCurveTA(MEnv env) {
            super(env, "animCurveTA");
            addSuperType(env.findNodeType("animCurve"));
            addAttribute(new MAttribute(env, "input", "i", env.findDataType("time")));
            addAttribute(new MAttribute(env, "output", "o", env.findDataType("double")));
        }
    }

    static class animCurveTL extends MNodeType {
        animCurveTL(MEnv env) {
            super(env, "animCurveTL");
            addSuperType(env.findNodeType("animCurve"));
            addAttribute(new MAttribute(env, "input", "i", env.findDataType("time")));
            addAttribute(new MAttribute(env, "output", "o", env.findDataType("double")));
        }
    }

    static class animCurveTT extends MNodeType {
        animCurveTT(MEnv env) {
            super(env, "animCurveTT");
            addSuperType(env.findNodeType("animCurve"));
            addAttribute(new MAttribute(env, "input", "i", env.findDataType("time")));
            addAttribute(new MAttribute(env, "output", "o", env.findDataType("time")));
        }
    }

    static class animCurveTU extends MNodeType {
        animCurveTU(MEnv env) {
            super(env, "animCurveTU");
            addSuperType(env.findNodeType("animCurve"));
            addAttribute(new MAttribute(env, "input", "i", env.findDataType("time")));
            addAttribute(new MAttribute(env, "output", "o", env.findDataType("double")));
        }
    }

    static class animCurveUA extends MNodeType {
        animCurveUA(MEnv env) {
            super(env, "animCurveUA");
            addSuperType(env.findNodeType("animCurve"));
            addAttribute(new MAttribute(env, "input", "i", env.findDataType("double")));
            addAttribute(new MAttribute(env, "output", "o", env.findDataType("double")));
        }
    }

    static class animCurveUL extends MNodeType {
        animCurveUL(MEnv env) {
            super(env, "animCurveUL");
            addSuperType(env.findNodeType("animCurve"));
            addAttribute(new MAttribute(env, "input", "i", env.findDataType("double")));
            addAttribute(new MAttribute(env, "output", "o", env.findDataType("double")));
        }
    }

    static class animCurveUT extends MNodeType {
        animCurveUT(MEnv env) {
            super(env, "animCurveUT");
            addSuperType(env.findNodeType("animCurve"));
            addAttribute(new MAttribute(env, "input", "i", env.findDataType("double")));
            addAttribute(new MAttribute(env, "output", "o", env.findDataType("time")));
        }
    }

    static class animCurveUU extends MNodeType {
        animCurveUU(MEnv env) {
            super(env, "animCurveUU");
            addSuperType(env.findNodeType("animCurve"));
            addAttribute(new MAttribute(env, "input", "i", env.findDataType("double")));
            addAttribute(new MAttribute(env, "output", "o", env.findDataType("double")));
        }
    }

    static class hwShaderType extends MNodeType {
        hwShaderType(MEnv env) {
            super(env, "hwShader");
            addSuperType(env.findNodeType("dependNode"));
            addAttribute(new MAttribute(env, "outColor", "oc",
                                        env.findDataType("float3")));
            addAttribute(new MAttribute(env, "shader", "s",
                                        env.findDataType("string")));
        }
    }

    static class cgfxShaderType extends MNodeType {
        cgfxShaderType(MEnv env) {
            super(env, "cgfxShader");
            addSuperType(env.findNodeType("hwShader"));
            addAttribute(new MAttribute(env, "shader", "s",
                                        env.findDataType("string")));
            addAttribute(new MAttribute(env, "technique", "t",
                                        env.findDataType("string")));
            addAttribute(new MAttribute(env, "vas", "vas",
                                        env.findDataType("stringArray")));
            addAttribute(new MAttribute(env, "val", "val",
                                        env.findDataType("stringArray")));
        }
    }

    static class cgfxVectorType extends MNodeType {
        cgfxVectorType(MEnv env) {
            super(env, "cgfxVector"); // ?
            addSuperType(env.findNodeType("dependNode"));
            addAttribute(new MAttribute(env, "matrix", "m",
                                        env.findDataType("matrix")));
            addAttribute(new MAttribute(env, "worldVector", "wv",
                                        env.findDataType("float3")));
        }
    }


    public MEnv() {
        // DATA TYPES
        addDataType(new MCharacterMappingType(this));
        addDataType(new MAttributeAliasType(this));
        addDataType(new MBoolType(this));
        addDataType(new MIntType(this));
        addDataType(new MIntArrayType(this));
        addDataType(new MStringType(this));
        addDataType(new MFloatType(this));
        addDataType(new MFloat2Type(this));
        addDataType(new MFloat3Type(this));
        addDataType(new MFloatArrayType(this));
        addDataType(new MFloat2ArrayType(this));
        addDataType(new MFloat3ArrayType(this));
        addDataType(new MInt3ArrayType(this));
        addDataType(new MPointerType(this));
        addDataType(new MPolyFaceType(this));
        addDataType(new MMatrixType(this));
        addDataType(new MComponentListType(this));
        addDataType(new MNurbsCurveType(this));


        aliasDataType("short", "int");
        aliasDataType("long", "int");
        aliasDataType("double", "float");
        aliasDataType("double[]", "float[]");
        aliasDataType("time", "float");
        aliasDataType("enum", "int");
        aliasDataType("long[]", "int[]");
        aliasDataType("enum[]", "int[]");
        aliasDataType("long3", "int3");
        aliasDataType("long3[]", "int3[]");
        aliasDataType("double2", "float2");
        aliasDataType("double3", "float3");
        aliasDataType("double3[]", "float3[]");
        aliasDataType("function", "int");


        addDataType(new MArrayType(this, findDataType("string")));
        aliasDataType("stringArray", "string[]");


        // NODE TYPES
        addNodeType(new DagNodeType(this));
        addNodeType(new ShapeType(this));
        addNodeType(new GeometryShapeType(this));
        addNodeType(new DeformableShapeType(this));
        addNodeType(new ParticleType(this));
        addNodeType(new ControlPointType(this));
        addNodeType(new CurveShapeType(this));
        addNodeType(new NurbsCurveType(this));
        addNodeType(new SurfaceShapeType(this));
        addNodeType(new MeshNodeType(this));
        addNodeType(new RigidBodyType(this));

        addNodeType(new TransformNodeType(this));

        addNodeType(new DynBase(this));
        addNodeType(new Field(this));
        addNodeType(new RadialField(this));
        addNodeType(new VortexField(this));
        addNodeType(new GravityField(this));
        addNodeType(new PointEmitter(this));
        addNodeType(new RigidConstraint(this));

        addNodeType(new Constraint(this));
        addNodeType(new PointConstraint(this));
        addNodeType(new ParentConstraint(this));
        addNodeType(new OrientConstraint(this));
        addNodeType(new AimConstraint(this));
        addNodeType(new PoleVectorConstraint(this));
        addNodeType(new IKHandle(this));
        addNodeType(new IKEffectorType(this));

        addNodeType(new DependNodeType(this));
        addNodeType(new DagPoseType(this));
        addNodeType(new AbstractBaseCreateType(this));
        addNodeType(new TransformGeometryType(this));
        addNodeType(new PolyBaseType(this));
        addNodeType(new PolyModifierType(this));
        addNodeType(new PolyNormalType(this));
        addNodeType(new AddDoubleLinearType(this));
        addNodeType(new MotionPathType(this));
        addNodeType(new LayeredTexture(this));
        addNodeType(new UvChooser(this));
        addNodeType(new Place2dTexture(this));
        addNodeType(new ChoiceNodeType(this));
        addNodeType(new PolyBaseType(this));
        addNodeType(new PolyCreatorType(this));
        addNodeType(new PolyPrimitiveType(this));
        addNodeType(new PolyCubeType(this));
        addNodeType(new PolyCylinderType(this));
        addNodeType(new PolySphereType(this));


        addNodeType(new Bump2dNodeType(this));
        addNodeType(new JointNodeType(this));
        addNodeType(new GeometryFilterNodeType(this));
        addNodeType(new SkinClusterNodeType(this));
        addNodeType(new BlendShapeNodeType(this));
        addNodeType(new TweakNodeType(this));
        addNodeType(new GroupIdType(this));
        addNodeType(new GroupPartsType(this));
        addNodeType(new animCurve(this));
        addNodeType(new animCurveTA(this));
        addNodeType(new animCurveTL(this));
        addNodeType(new animCurveTU(this));
        addNodeType(new animCurveUA(this));
        addNodeType(new animCurveUL(this));
        addNodeType(new animCurveUT(this));
        addNodeType(new animCurveUU(this));
        addNodeType(new SurfaceShaderType(this));
        addNodeType(new LambertType(this));
        addNodeType(new ReflectType(this));
        addNodeType(new PhongNodeType(this));
        addNodeType(new PhongENodeType(this));
        addNodeType(new BlinnNodeType(this));
        addNodeType(new Texture2DType(this));
        addNodeType(new FileType(this));
        addNodeType(new PsdFileTex(this));

        addNodeType(new EntityType(this));
        addNodeType(new ObjectSetType(this));
        addNodeType(new ShadingEngineNodeType(this));
        addNodeType(new LightLinkerNodeType(this));
        addNodeType(new MaterialInfoNodeType(this));

        addNodeType(new ConditionType(this));
        addNodeType(new MultiplyDivideType(this));
        addNodeType(new PlusMinusAverageType(this));
        addNodeType(new ReverseType(this));
        addNodeType(new UnitConversionType(this));
        addNodeType(new BlendType(this));
        addNodeType(new BlendColorsType(this));
        addNodeType(new BlendWeightedType(this));
        //        addNodeType(new CharacterType(this));
        addNodeType(new ClampType(this));

        addNodeType(new LightType(this));
        addNodeType(new RenderLightType(this));
        addNodeType(new NonAmbientLightShapeNodeType(this));
        addNodeType(new NonExtendedLightShapeNodeType(this));
        addNodeType(new PointLightType(this));
        addNodeType(new DirectionalLightType(this));
        addNodeType(new SpotLightType(this));

        addNodeType(new CameraType(this));

        addNodeType(new hwShaderType(this));
        addNodeType(new cgfxShaderType(this));
        addNodeType(new cgfxVectorType(this));
        addNodeType(new characterType(this));
        addNodeType(new animClipType(this));
        addNodeType(new clipSchedulerType(this));
        addNodeType(new clipLibraryType(this));

        // bullet physics nodes
        addNodeType(new dRigidBody(this));
        addNodeType(new dCollisionShape(this));
        addNodeType(new dHingeConstraint(this));
        addNodeType(new dNailConstraint(this));

        // nvidia physx physics nodes
        addNodeType(new nxRigidBody(this));
    }

    Map<MNode, Set<MConnection> > connections = new HashMap();
    Map<MNode, Set<MConnection> > invConnections = new HashMap();

    public void connectAttr(String src,  String target) {
        MPath srcPath = new MPath(this, src);
        MNode srcNode = srcPath.getTargetNode();
        MPath targetPath = new MPath(this, target);
        MNode targetNode = targetPath.getTargetNode();
        // System.err.println("connectAttr " + src + " to " + target + " => " + srcPath + " to " + targetPath);
        if (srcNode == null || targetNode == null) {
            // System.err.println("  srcNode = " + srcNode + " targetNode = " + targetNode);
            return;
        }
        // System.err.println("connectAttr " + srcPath + " to " + targetPath);
        MConnection conn = new MConnection(srcPath, targetPath);
        Set<MConnection> set = connections.get(srcNode);
        if (set == null) {
            set = new HashSet();
            connections.put(srcNode, set);
        }
        set.add(conn);

        set = invConnections.get(targetNode);
        if (set == null) {
            set = new HashSet();
            invConnections.put(targetNode, set);
        }
        set.add(conn);
    }

    public Set<MConnection> getConnectionsTo(String str) {
        return getConnectionsTo(new MPath(this, str));
    }

    public Set<MConnection> getConnectionsTo(String str, boolean checkSubPaths) {
        return getConnectionsTo(new MPath(this, str), checkSubPaths);
    }

    public Set<MConnection> getConnectionsTo(MPath path) {
        return getConnectionsTo(path, true);
    }

    public Set<MConnection> getConnectionsTo(MPath path, boolean checkSubPaths) {

        Set<MConnection> result = new HashSet();
        getConnectionsTo(path, checkSubPaths, result);
        return result;
    }

    public Set<MConnection> getConnectionsTo(MPath path, boolean checkSubPaths, Set<MConnection> result) {
        Set<MConnection> conns = invConnections.get(path.getTargetNode());
        // System.err.println("Looking up connections to: " + path);
        if (conns != null) {
            for (MConnection conn : conns) {
                // System.err.println("  Checking " + conn.getTargetPath());
                if (checkSubPaths) {
                    if (path.isPrefixOf(conn.getTargetPath())) {
                        // System.err.println("   [adding]");
                        result.add(conn);
                    }
                } else {
                    if (path.equals(conn.getTargetPath())) {
                        // System.err.println("   [adding]");
                        result.add(conn);
                    }
                }
            }
        }
        return result;
    }

    public Set<MPath> getPathsConnectingTo(String str) {
        return getPathsConnectingTo(new MPath(this, str));
    }

    public Set<MPath> getPathsConnectingTo(String str, boolean checkSubPaths) {
        return getPathsConnectingTo(new MPath(this, str), checkSubPaths);
    }

    public Set<MPath> getPathsConnectingTo(MPath path) {
        return getPathsConnectingTo(path, true);
    }

    public Set<MPath> getPathsConnectingTo(MPath path, boolean checkSubPaths) {
        Set<MPath> result = new HashSet();
        Set<MConnection> conns = getConnectionsTo(path, checkSubPaths);
        for (MConnection conn : conns) {
            result.add(conn.getSourcePath());
        }
        return result;
    }

    public Set<MConnection> getConnectionsFrom(String str) {
        return getConnectionsFrom(new MPath(this, str));
    }

    public Set<MConnection> getConnectionsFrom(String str, boolean checkSubPaths) {
        return getConnectionsFrom(new MPath(this, str), checkSubPaths);
    }

    public Set<MConnection> getConnectionsFrom(MPath path) {
        return getConnectionsFrom(path, true);
    }

    public Set<MConnection> getConnectionsFrom(MPath path, boolean checkSubPaths) {
        Set<MConnection> result = new HashSet();
        Set<MConnection> conns = connections.get(path.getTargetNode());
        if (conns != null) {
            for (MConnection conn : conns) {
                if (checkSubPaths) {
                    if (path.isPrefixOf(conn.getSourcePath())) {
                        result.add(conn);
                    }
                } else {
                    if (path.equals(conn.getSourcePath())) {
                        result.add(conn);
                    }
                }
            }
        }
        return result;
    }

    public Set<MPath> getPathsConnectingFrom(String str) {
        return getPathsConnectingFrom(new MPath(this, str));
    }

    public Set<MPath> getPathsConnectingFrom(String str, boolean checkSubPaths) {
        return getPathsConnectingFrom(new MPath(this, str), checkSubPaths);
    }

    public Set<MPath> getPathsConnectingFrom(MPath path) {
        return getPathsConnectingFrom(path, true);
    }

    public Set<MPath> getPathsConnectingFrom(MPath path, boolean checkSubPaths) {
        Set<MPath> result = new HashSet();
        Set<MConnection> conns = getConnectionsFrom(path, checkSubPaths);
        for (MConnection conn : conns) {
            result.add(conn.getTargetPath());
        }
        return result;
    }

    public Set<MConnection> getIncomingConnections(MNode node) {
        return invConnections.get(node);
    }

    public Set<MConnection> getOutgoingConnections(MNode node) {
        return connections.get(node);
    }

    public MPointer createPointer(MPath path) {
        MPointer ptr = new MPointerImpl((MPointerType)findDataType("Message"));
        ptr.setTarget(path);
        return ptr;
    }
}
