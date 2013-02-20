/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package javafx.scene;

import com.sun.javafx.collections.TrackableObservableList;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.BoxBounds;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.jmx.MXNodeAlgorithm;
import com.sun.javafx.jmx.MXNodeAlgorithmContext;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.sg.PGLightBase;
import com.sun.javafx.tk.Toolkit;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;

/**
 * The {@code LightBase} class provides definitions of common properties for
 * objects that represent a form of Light source.  These properties
 * include:
 * <ul>
 * <li>The color that defines color of the light source.
 * </ul>
 *
 * @since JavaFX 8
 */
public abstract class LightBase extends Node {
    /*
     *    A Light source is a Node
          LightBase is an abstract base class for other Light classes
          Support AmbientLight, PointLight and DirectionalLight.
          No plan to add SpotLight in FX8
          No plan to include attenuation in light computation in FX8
          Scoping
            Spacial vs. hierarchical (or explicit) scoping
            Spacial is hard to do it right
            Explicit is the preferred approach
            Add a default light when no light is specified in a 3D scene (principle of least surprise)
 
     */
    
    private Affine3D localToSceneTx = new Affine3D();

    /**
     * Creates a new instance of {@code LightBase} class with a default Color.WHITE light source.
     */
    protected LightBase() {
        this(Color.WHITE);
    }

    /**
     * Creates a new instance of {@code LightBase} class using the specified color.
     * 
     * @param color the color of the light source
     */
    protected LightBase(Color color) {
        setColor(color);
        this.localToSceneTransformProperty().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                impl_markDirty(DirtyBits.NODE_LIGHT_TRANSFORM);
            }
        });
    }
    
    /**
     * Specifies the color of light source.
     *
     * @defaultValue null
     */
    private ObjectProperty<Color> color;

    public final void setColor(Color value) {
        colorProperty().set(value);
    }

    public final Color getColor() {
        return color == null ? null : color.get();
    }

    public final ObjectProperty<Color> colorProperty() {
        if (color == null) {
            color = new SimpleObjectProperty<Color>(LightBase.this, "color") {
                @Override
                protected void invalidated() {
                    impl_markDirty(DirtyBits.NODE_LIGHT);
                }                
            };
        }
        return color;
    }

    /**
     * Defines the light on or off.
     *
     * @defaultValue true
     */
    private BooleanProperty lightOn;

    public final void setLightOn(boolean value) {
        lightOnProperty().set(value);
    }

    public final boolean isLightOn() {
        return lightOn == null ? true : lightOn.get();
    }

    public final BooleanProperty lightOnProperty() {
        if (lightOn == null) {
            lightOn = new SimpleBooleanProperty(LightBase.this, "lightOn", true) {
                @Override
                protected void invalidated() {
                    impl_markDirty(DirtyBits.NODE_LIGHT);
                }
            };
        }
        return lightOn;
    }

    private ObservableList<Node> scope;
    
    /**
     * Gets the list of nodes that specifies the
     * hierarchical scope of this Light. If the scope list is empty, 
     * the Light node has universe scope: all nodes under it's scene
     * are affected by it. If the scope list is non-empty, only those
     * 3D Shape nodes in the scope list and under the Group nodes in the
     * scope list are affected by this Light node.
     */
    public ObservableList<Node> getScope() {
        if (scope == null) {
            scope = new TrackableObservableList<Node>() {

                @Override
                protected void onChanged(Change<Node> c) {
                    while (c.next()) {
                        for (Node node : c.getRemoved()) {
//                            node.impl_remove(Node.this);
                        }
                        for (Node node : c.getAddedSubList()) {
//                            node.impl_add(LightBase.this);
                        }
                    }
                }
            };
        }

        return scope;
    }
    
    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public void impl_updatePG() {
        super.impl_updatePG();
        PGLightBase pgLightBase = (PGLightBase) impl_getPGNode();
        if (impl_isDirty(DirtyBits.NODE_LIGHT)) {
            pgLightBase.setColor((getColor() == null) ? null
                    : Toolkit.getPaintAccessor().getPlatformPaint(getColor()));
            //TODO: 3D - Handle light on/off
        }
        if (impl_isDirty(DirtyBits.NODE_LIGHT_TRANSFORM)) {
            localToSceneTx.setToIdentity();
            getLocalToSceneTransform().impl_apply(localToSceneTx);
            // TODO: 3D - For now, we are treating the scene as world. This may need to change
            // for the fixed eye position case.
            pgLightBase.setWorldTransform(localToSceneTx);
        }
    }

     /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public BaseBounds impl_computeGeomBounds(BaseBounds bounds, BaseTransform tx) {
        // TODO: 3D - Check is this the right default
        return new BoxBounds();
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    protected boolean impl_computeContains(double localX, double localY) {
        // TODO: 3D - Check is this the right default
        return false;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public Object impl_processMXNode(MXNodeAlgorithm alg, MXNodeAlgorithmContext ctx) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
