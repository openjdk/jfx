/*
 * Copyright (c) 2013, 2022, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.scene.LightBaseHelper;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.transform.TransformHelper;
import com.sun.javafx.sg.prism.NGLightBase;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.tk.Toolkit;

import java.util.List;
import java.util.stream.Collectors;

import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape3D;
import com.sun.javafx.logging.PlatformLogger;

/**
 * The {@code LightBase} class is the base class for all objects that represent a form of light source. All light types
 * share the following common properties that are defined in this class:
 * <ul>
 *   <li>{@code color} - the color of the light source</li>
 *   <li>{@code scope} - a list of nodes the light source affects</li>
 *   <li>{@code exlusionScope} - a list of nodes the light source does not affect</li>
 * </ul>
 *
 * In addition to these, each light type supports a different set of properties, summarized in the following table:
 *
 * <table border="1">
 *   <caption>Summary of Light types by attributes</caption>
 *   <tr>
 *     <th scope="col">Light type</th>
 *     <th scope="col">Rotation and Direction</th>
 *     <th scope="col">Location and Attenuation</th>
 *   </tr>
 *   <tr>
 *     <th scope="row">{@link AmbientLight}</th>
 *     <td style="text-align:center">&#10007;</td>
 *     <td style="text-align:center">&#10007;</td>
 *   </tr>
 *   <tr>
 *     <th scope="row">{@link DirectionalLight}</th>
 *     <td style="text-align:center">&#10003;</td>
 *     <td style="text-align:center">&#10007;</td>
 *   </tr>
 *   <tr>
 *     <th scope="row">{@link PointLight}</th>
 *     <td style="text-align:center">&#10007;</td>
 *     <td style="text-align:center">&#10003;</td>
 *   </tr>
 *   <tr>
 *     <th scope="row">{@link SpotLight}*</th>
 *     <td style="text-align:center">&#10003;</td>
 *     <td style="text-align:center">&#10003;</td>
 *   </tr>
 * </table>
 * * Supports spotlight attenuation factors as described in its class docs.
 *
 * <p>
 * An application cannot add its own light types. Extending {@code LightBase} directly may lead to an
 * {@code UnsupportedOperationException} being thrown.
 * <p>
 * All light types are not affected by scaling and shearing transforms.
 *
 * <h2><a id="Color">Color</a></h2>
 * A light source produces a light of a single color. The appearance of an object illuminated by the light is a result
 * of the color of the light and the material the object is made of. For example, for a blue light, a white object will
 * appear blue, a black object will appear black, and a (255, 0, 255)-colored object will appear blue, as the light
 * "selects" only the blue component of the material color. The mathematical definition of the material-light
 * interaction is available in {@link javafx.scene.paint.PhongMaterial PhongMaterial}.
 * <p>
 * See also the implementation notes section.
 *
 * <h2><a id="Scopes">Scopes</a></h2>
 * A light has a {@code scope} list and an {@code exclusionScope} list that define which nodes are illuminated by it and
 * which aren't. A node can exist in only one of the lists: if it is added to one, it is silently removed from the
 * other. If a node does not exist in any list, it inherits its affected state from its parent, recursively. An
 * exception to this is that a light with an empty {@code scope} affects all nodes in its scene/subscene implicitly
 * (except for those in its {@code exlusionScope}) as if the root of the scene is in the {@code scope}.
 * <p>
 * The {@code exlusionScope} is useful only for nodes that would otherwise be in scope of the light. Excluding a node is
 * a convenient alternative to traversing the scenegraph hierarchy and adding all of the other nodes to the light's
 * scope. Instead, the scope can remain wide, and specific nodes can be excluded with the exclusion scope.
 *
 * <h2><a id="Direction">Direction</a></h2>
 * The direction of the light is defined by the {@code direction} vector property of the light. The light's
 * direction can be rotated by setting a rotation transform on the light. For example, if the direction vector is
 * {@code (1, 1, 1)} and the light is not rotated, it will point in the {@code (1, 1, 1)} direction, and if the light is
 * rotated 90 degrees on the y axis, it will point in the {@code (1, 1, -1)} direction.
 * <p>
 * Light types that do not have a direction are not affected by rotation transforms.
 *
 * <h2><a id="Attenuation">Distance Attenuation</a></h2>
 * Any pixel within the range of the light will be illuminated by it (unless it belongs to a {@code Shape3D} outside of
 * its <a href="#Scopes">scope</a>). The distance is measured from the light's position to the pixel's position, and is
 * checked to be within the maximum range of the light, as defined by its {@code maxRange} property. The light's
 * position can be changed by setting a translation transform on the light.
 * <p>
 * The light's intensity can be set to change over distance by attenuating it. The attenuation formula
 * <p>
 * {@code attn = 1 / (ca + la * dist + qa * dist^2)}
 * <p>
 * defines 3 coefficients: {@code ca}, {@code la}, and {@code qa}, which control the constant, linear, and quadratic
 * behaviors of intensity falloff over distance, respectively. The effective color of the light at a given point in
 * space is {@code color * attn}. It is possible, albeit unrealistic, to specify negative values to attenuation
 * coefficients. This allows the resulting attenuation factor to be negative, which results in the light's color being
 * subtracted from the material color instead of added to it, thus creating a "shadow caster".
 * <p>
 * For a realistic effect, {@code maxRange} should be set to a distance at which the attenuation is close to 0, as this
 * will give a soft cutoff.
 * <p>
 * Light types that are not distance-attenuated are not affected by translation transforms. For these types, a decrease
 * in intensity can be achieved by using a darker color.
 *
 * <p>
 * <b>Note</b>: this is a conditional feature. See
 * {@link javafx.application.ConditionalFeature#SCENE3D ConditionalFeature.SCENE3D} for more information.
 *
 * @implNote
 * The following applies to the {@code color} property of the light:
 * <ol>
 *   <li> A black colored light is ignored since its contribution is 0.
 *   <li> The transparency (alpha) component of a light is ignored.
 * </ol>
 * These behaviors are not specified and could change in the future.
 *
 * @since JavaFX 8.0
 */
public abstract class LightBase extends Node {
    static {
         // This is used by classes in different packages to get access to
         // private and package private methods.
        LightBaseHelper.setLightBaseAccessor(new LightBaseHelper.LightBaseAccessor() {
            @Override
            public void doMarkDirty(Node node, DirtyBits dirtyBit) {
                ((LightBase) node).doMarkDirty(dirtyBit);
            }

            @Override
            public void doUpdatePeer(Node node) {
                ((LightBase) node).doUpdatePeer();
            }

            @Override
            public BaseBounds doComputeGeomBounds(Node node,
                    BaseBounds bounds, BaseTransform tx) {
                return ((LightBase) node).doComputeGeomBounds(bounds, tx);
            }

            @Override
            public boolean doComputeContains(Node node, double localX, double localY) {
                return ((LightBase) node).doComputeContains(localX, localY);
            }
        });
    }

    private Affine3D localToSceneTx = new Affine3D();

    {
        // To initialize the class helper at the beginning of each constructor of this class
        LightBaseHelper.initHelper(this);
    }

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
        if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
            String logname = LightBase.class.getName();
            PlatformLogger.getLogger(logname).warning("System can't support "
                                                      + "ConditionalFeature.SCENE3D");
        }

        setColor(color);
        this.localToSceneTransformProperty().addListener(observable ->
                NodeHelper.markDirty(this, DirtyBits.NODE_LIGHT_TRANSFORM));
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
                    NodeHelper.markDirty(LightBase.this, DirtyBits.NODE_LIGHT);
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
                    NodeHelper.markDirty(LightBase.this, DirtyBits.NODE_LIGHT);
                }
            };
        }
        return lightOn;
    }

    private ObservableList<Node> scope;

    /**
     * Gets the list of nodes that specifies the hierarchical scope of this light. Any {@code Shape3D}s in this list or
     * under a {@code Parent} in this list are affected by this light, unless a closer parent exists in the
     * {@code exclusionScope} list. If the list is empty, all nodes under the light's scene/subscene are affected by it
     * (unless they are in the {@code exclusionScope}).
     *
     * @return the list of nodes that specifies the hierarchical scope of this light
     * @see #getExclusionScope
     */
    public ObservableList<Node> getScope() {
        if (scope == null) {
            scope = new TrackableObservableList<>() {

                @Override
                protected void onChanged(Change<Node> c) {
                    doOnChanged(c, exclusionScope);
                }
            };
        }
        return scope;
    }

    private ObservableList<Node> exclusionScope;

    /**
     * Gets the list of nodes that specifies the hierarchical exclusion scope of this light. Any {@code Shape3D}s in
     * this list or under a {@code Parent} in this list are not affected by this light, unless a closer parent exists in
     * the {@code scope} list. <br>
     * This is a convenience list for excluding nodes that would otherwise be in scope of the light.
     *
     * @return the list of nodes that specifies the hierarchical exclusion scope of this light
     * @see #getScope
     * @since 13
     */
    public ObservableList<Node> getExclusionScope() {
        if (exclusionScope == null) {
            exclusionScope = new TrackableObservableList<>() {

                @Override
                protected void onChanged(Change<Node> c) {
                    doOnChanged(c, scope);
                }
            };
        }
        return exclusionScope;
    }

    private void doOnChanged(Change<Node> c, ObservableList<Node> otherScope) {
        NodeHelper.markDirty(this, DirtyBits.NODE_LIGHT_SCOPE);
        while (c.next()) {
            c.getRemoved().forEach(this::markChildrenDirty);
            c.getAddedSubList().forEach(node -> {
                if (otherScope != null && otherScope.remove(node)) {
                    return; // the other list will take care of the change
                }
                markChildrenDirty(node);
            });
        }
    }

    @Override
    void scenesChanged(final Scene newScene, final SubScene newSubScene,
                       final Scene oldScene, final SubScene oldSubScene) {
        // This light is owned by the Scene/SubScene, and thus must change
        // accordingly. Note lights can owned by either a Scene or SubScene,
        // but not both.
        if (oldSubScene != null) {
            oldSubScene.removeLight(this);
        } else if (oldScene != null) {
            oldScene.removeLight(this);
        }
        if (newSubScene != null) {
            newSubScene.addLight(this);
        } else if (newScene != null) {
            newScene.addLight(this);
        }
    }

    /**
     * For use by implementing subclasses. Treat as protected.
     *
     * Creates and returns a SimpleDoubleProperty with an invalidation scheme.
     */
    DoubleProperty getLightDoubleProperty(String name, double initialValue) {
        return new SimpleDoubleProperty(this, name, initialValue) {
            @Override
            protected void invalidated() {
                NodeHelper.markDirty(LightBase.this, DirtyBits.NODE_LIGHT);
            }
        };
    }

    private void markOwnerDirty() {
        // if the light is part of the scene/subScene, we will need to notify
        // the owner to mark the entire scene/subScene dirty.
        SubScene subScene = getSubScene();
        if (subScene != null) {
            subScene.markContentDirty();
        } else {
            Scene scene = getScene();
            if (scene != null) {
                scene.setNeedsRepaint();
            }
        }
    }

    /**
     * Marks dirty all the 3D shapes that had their scoped/excluded state change. The method recursively traverses the
     * given node's graph top-down to find all the leaves (3D shapes). Nodes that are not contained in one of the scope
     * lists inherit their parent's scope, and nodes that are contained in one of the lists override their parent's
     * state. For this reason, when traversing the graph, if a node that is contained in a list is reached, its branch
     * is skipped.
     *
     * @param node the node that was added/removed from a scope
     */
    private void markChildrenDirty(Node node) {
        if (node instanceof Shape3D) {
            // Dirty using a lightweight DirtyBits.NODE_DRAWMODE bit
            NodeHelper.markDirty(((Shape3D) node), DirtyBits.NODE_DRAWMODE);
        } else if (node instanceof Parent) {
            for (Node child : ((Parent) node).getChildren()) {
                if ((scope != null && getScope().contains(child)) ||
                        (exclusionScope != null && getExclusionScope().contains(child))) {
                    continue; // child overrides parent, no need to propagate the change
                }
                markChildrenDirty(child);
            }
        }
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private void doMarkDirty(DirtyBits dirtyBit) {
        if ((scope == null) || getScope().isEmpty()) {
            // This light affects the entire scene/subScene
            markOwnerDirty();
        } else if (dirtyBit != DirtyBits.NODE_LIGHT_SCOPE) {
            // Skip NODE_LIGHT_SCOPE dirty since it is processed on scope change.
            getScope().forEach(this::markChildrenDirty);
        }
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private void doUpdatePeer() {
        NGLightBase peer = getPeer();
        if (isDirty(DirtyBits.NODE_LIGHT)) {
            peer.setColor((getColor() == null) ?
                    Toolkit.getPaintAccessor().getPlatformPaint(Color.WHITE)
                    : Toolkit.getPaintAccessor().getPlatformPaint(getColor()));
            peer.setLightOn(isLightOn());
        }

        if (isDirty(DirtyBits.NODE_LIGHT_SCOPE)) {
            if (scope != null) {
                if (getScope().isEmpty()) {
                    peer.setScope(List.of());
                } else {
                    peer.setScope(getScope().stream().map(n -> n.<NGNode>getPeer()).collect(Collectors.toList()));
                }
            }
            if (exclusionScope != null) {
                if (getExclusionScope().isEmpty()) {
                    peer.setExclusionScope(List.of());
                } else {
                    peer.setExclusionScope(getExclusionScope().stream().map(n -> n.<NGNode>getPeer()).collect(Collectors.toList()));
                }
            }
        }

        if (isDirty(DirtyBits.NODE_LIGHT_TRANSFORM)) {
            localToSceneTx.setToIdentity();
            TransformHelper.apply(getLocalToSceneTransform(), localToSceneTx);
            // TODO: 3D - For now, we are treating the scene as world. This may need to change
            // for the fixed eye position case.
            peer.setWorldTransform(localToSceneTx);
        }
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private BaseBounds doComputeGeomBounds(BaseBounds bounds, BaseTransform tx) {
        // TODO: 3D - Check is this the right default
        return new BoxBounds();
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private boolean doComputeContains(double localX, double localY) {
        // TODO: 3D - Check is this the right default
        return false;
    }

}
