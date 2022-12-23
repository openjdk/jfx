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

package javafx.scene.paint;

import com.sun.javafx.beans.event.AbstractNotifyListener;
import com.sun.javafx.scene.paint.MaterialHelper;
import com.sun.javafx.sg.prism.NGPhongMaterial;
import com.sun.javafx.tk.Toolkit;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.AmbientLight;
import javafx.scene.PointLight;
import javafx.scene.image.Image;

/**
 * The {@code PhongMaterial} class provides definitions of properties that
 * represent a Phong shaded material. It describes the interaction of
 * light with the surface of the {@code Mesh} it is applied to. The {@code PhongMaterial}
 * reflects light in terms of a diffuse and specular component together with
 * an ambient and a self illumination term. The color of a point on a geometric
 * surface is mathematical function of these four components.
 * <p>
 * The color is computed by the following equation:
 *
 * <pre>{@code
 * for each ambient light source i {
 *     ambient += lightColor[i]
 * }
 *
 * for each point light source i {
 *     diffuse += (L[i] . N) * lightColor[i]
 *     specular += ((R[i] . V) ^ (specularPower * intensity(specularMap))) * lightColor[i]
 * }
 *
 * color = (ambient + diffuse) * diffuseColor * diffuseMap
 *             + specular * specularColor * specularMap
 *             + selfIlluminationMap
 * }</pre>
 * where
 * {@code lightColor[i]} is the color of light source i,<br>
 * {@code L[i]} is the vector from the surface to light source i,<br>
 * {@code N} is the normal vector (taking into the account the bumpMap if present),<br>
 * {@code R[i]} is the normalized reflection vector for L[i] about the surface normal,<br>
 * and {@code V} is the normalized view vector.
 *
 * @see AmbientLight
 * @see PointLight
 * @since JavaFX 8.0
 */
public class PhongMaterial extends Material {

    private boolean diffuseColorDirty = true;
    private boolean specularColorDirty = true;
    private boolean specularPowerDirty = true;
    private boolean diffuseMapDirty = true;
    private boolean specularMapDirty = true;
    private boolean bumpMapDirty = true;
    private boolean selfIlluminationMapDirty = true;

    /**
     * Creates a new instance of {@code PhongMaterial} class with a default
     * Color.WHITE {@code diffuseColor} property.
     */
    public PhongMaterial() {
        setDiffuseColor(Color.WHITE);
    }

    /**
     * Creates a new instance of {@code PhongMaterial} class using the specified
     * color for its {@code diffuseColor} property.
     *
     * @param diffuseColor the color of the diffuseColor property
     */
    public PhongMaterial(Color diffuseColor) {
        setDiffuseColor(diffuseColor);
    }

    /**
     * Creates a new instance of {@code PhongMaterial} class using the specified
     * colors and images for its {@code diffuseColor} properties.
     *
     * @param diffuseColor the color of the diffuseColor property
     * @param diffuseMap the image of the diffuseMap property
     * @param specularMap the image of the specularMap property
     * @param bumpMap the image of the bumpMap property
     * @param selfIlluminationMap the image of the selfIlluminationMap property
     *
     */
    public PhongMaterial(Color diffuseColor, Image diffuseMap,
            Image specularMap, Image bumpMap, Image selfIlluminationMap) {
        setDiffuseColor(diffuseColor);
        setDiffuseMap(diffuseMap);
        setSpecularMap(specularMap);
        setBumpMap(bumpMap);
        setSelfIlluminationMap(selfIlluminationMap);
    }

    /**
     * The diffuse color of this {@code PhongMaterial}.
     *
     * @defaultValue Color.WHITE
     */
    private ObjectProperty<Color> diffuseColor;

    public final void setDiffuseColor(Color value) {
        diffuseColorProperty().set(value);
    }

    public final Color getDiffuseColor() {
        return diffuseColor == null ? null : diffuseColor.get();
    }

    public final ObjectProperty<Color> diffuseColorProperty() {
        if (diffuseColor == null) {
            diffuseColor = new SimpleObjectProperty<>(PhongMaterial.this,
                    "diffuseColor") {
                @Override
                protected void invalidated() {
                    diffuseColorDirty = true;
                    setDirty(true);
                }
            };
        }
        return diffuseColor;
    }

    /**
     * The specular color of this {@code PhongMaterial}.
     *
     * @defaultValue null
     */
    private ObjectProperty<Color> specularColor;

    public final void setSpecularColor(Color value) {
        specularColorProperty().set(value);
    }

    public final Color getSpecularColor() {
        return specularColor == null ? null : specularColor.get();
    }

    public final ObjectProperty<Color> specularColorProperty() {
        if (specularColor == null) {
            specularColor = new SimpleObjectProperty<>(PhongMaterial.this,
                    "specularColor") {
                @Override
                protected void invalidated() {
                    specularColorDirty = true;
                    setDirty(true);
                }
            };
        }
        return specularColor;
    }

    /**
     * The specular power of this {@code PhongMaterial}.
     *
     * @defaultValue 32.0
     */
    private DoubleProperty specularPower;

    public final void setSpecularPower(double value) {
        specularPowerProperty().set(value);
    }

    public final double getSpecularPower() {
        return specularPower == null ? 32 : specularPower.get();
    }

    public final DoubleProperty specularPowerProperty() {
        if (specularPower == null) {
            specularPower = new SimpleDoubleProperty(PhongMaterial.this,
                    "specularPower", 32.0) {
                @Override
                public void invalidated() {
                    specularPowerDirty = true;
                    setDirty(true);
                }
            };
        }
        return specularPower;
    }

    private final AbstractNotifyListener platformImageChangeListener = new AbstractNotifyListener() {
        @Override
        public void invalidated(Observable valueModel) {
            if (oldDiffuseMap != null
                    && valueModel == Toolkit.getImageAccessor().getImageProperty(oldDiffuseMap)) {
                diffuseMapDirty = true;
            } else if (oldSpecularMap != null
                    && valueModel == Toolkit.getImageAccessor().getImageProperty(oldSpecularMap)) {
                specularMapDirty = true;
            } else if (oldBumpMap != null
                    && valueModel == Toolkit.getImageAccessor().getImageProperty(oldBumpMap)) {
                bumpMapDirty = true;
            } else if (oldSelfIlluminationMap != null
                    && valueModel == Toolkit.getImageAccessor().getImageProperty(oldSelfIlluminationMap)) {
                selfIlluminationMapDirty = true;
            }
            setDirty(true);
        }
    };

    /**
     * The diffuse map of this {@code PhongMaterial}.
     *
     * @defaultValue null
     */
    // TODO: 3D - Texture or Image? For Media it might be better to have it as a Texture
    private ObjectProperty<Image> diffuseMap;

    public final void setDiffuseMap(Image value) {
        diffuseMapProperty().set(value);
    }

    public final Image getDiffuseMap() {
        return diffuseMap == null ? null : diffuseMap.get();
    }

    private Image oldDiffuseMap;
    public final ObjectProperty<Image> diffuseMapProperty() {
        if (diffuseMap == null) {
            diffuseMap = new SimpleObjectProperty<>(PhongMaterial.this,
                    "diffuseMap") {

                private boolean needsListeners = false;

                @Override
                public void invalidated() {
                    Image _image = get();

                    if (needsListeners) {
                        Toolkit.getImageAccessor().getImageProperty(oldDiffuseMap).
                                removeListener(platformImageChangeListener.getWeakListener());
                    }

                    needsListeners = _image != null && (Toolkit.getImageAccessor().isAnimation(_image)
                            || _image.getProgress() < 1);

                    if (needsListeners) {
                        Toolkit.getImageAccessor().getImageProperty(_image).
                                addListener(platformImageChangeListener.getWeakListener());
                    }
                    oldDiffuseMap = _image;
                    diffuseMapDirty = true;
                    setDirty(true);
                }
            };
        }
        return diffuseMap;
    }

    /**
     * The specular map of this {@code PhongMaterial}.
     *
     * @defaultValue null
     */
    // TODO: 3D - Texture or Image? For Media it might be better to have it as a Texture
    private ObjectProperty<Image> specularMap;

    public final void setSpecularMap(Image value) {
        specularMapProperty().set(value);
    }

    public final Image getSpecularMap() {
        return specularMap == null ? null : specularMap.get();
    }

    private Image oldSpecularMap;
    public final ObjectProperty<Image> specularMapProperty() {
        if (specularMap == null) {
            specularMap = new SimpleObjectProperty<>(PhongMaterial.this,
                    "specularMap") {

                private boolean needsListeners = false;

                @Override
                public void invalidated() {
                    Image _image = get();

                    if (needsListeners) {
                        Toolkit.getImageAccessor().getImageProperty(oldSpecularMap).
                                removeListener(platformImageChangeListener.getWeakListener());
                    }

                    needsListeners = _image != null && (Toolkit.getImageAccessor().isAnimation(_image)
                            || _image.getProgress() < 1);

                    if (needsListeners) {
                        Toolkit.getImageAccessor().getImageProperty(_image).
                                addListener(platformImageChangeListener.getWeakListener());
                    }

                    oldSpecularMap = _image;
                    specularMapDirty = true;
                    setDirty(true);
                }
            };
        }
        return specularMap;
    }

    /**
     * The bump map of this {@code PhongMaterial}, which is a normal map stored
     * as a RGB {@link Image}.
     *
     * @defaultValue null
     */
    // TODO: 3D - Texture or Image? For Media it might be better to have it as a Texture
    private ObjectProperty<Image> bumpMap;

    public final void setBumpMap(Image value) {
        bumpMapProperty().set(value);
    }

    public final Image getBumpMap() {
        return bumpMap == null ? null : bumpMap.get();
    }

    private Image oldBumpMap;
    public final ObjectProperty<Image> bumpMapProperty() {
        if (bumpMap == null) {
            bumpMap = new SimpleObjectProperty<>(PhongMaterial.this,
                    "bumpMap") {

                private boolean needsListeners = false;

                @Override
                public void invalidated() {
                    Image _image = get();

                    if (needsListeners) {
                        Toolkit.getImageAccessor().getImageProperty(oldBumpMap).
                                removeListener(platformImageChangeListener.getWeakListener());
                    }

                    needsListeners = _image != null && (Toolkit.getImageAccessor().isAnimation(_image)
                            || _image.getProgress() < 1);

                    if (needsListeners) {
                        Toolkit.getImageAccessor().getImageProperty(_image).
                                addListener(platformImageChangeListener.getWeakListener());
                    }

                    oldBumpMap = _image;
                    bumpMapDirty = true;
                    setDirty(true);
                }
            };
        }
        return bumpMap;
    }

    /**
     * The self illumination map of this {@code PhongMaterial}.
     *
     * @defaultValue null
     */
    // TODO: 3D - Texture or Image? For Media it might be better to have it as a Texture
    private ObjectProperty<Image> selfIlluminationMap;

    public final void setSelfIlluminationMap(Image value) {
        selfIlluminationMapProperty().set(value);
    }

    public final Image getSelfIlluminationMap() {
        return selfIlluminationMap == null ? null : selfIlluminationMap.get();
    }

    private Image oldSelfIlluminationMap;
    public final ObjectProperty<Image> selfIlluminationMapProperty() {
        if (selfIlluminationMap == null) {
            selfIlluminationMap = new SimpleObjectProperty<>(PhongMaterial.this,
                    "selfIlluminationMap") {

                private boolean needsListeners = false;

                @Override
                public void invalidated() {
                    Image _image = get();

                    if (needsListeners) {
                        Toolkit.getImageAccessor().getImageProperty(oldSelfIlluminationMap).
                                removeListener(platformImageChangeListener.getWeakListener());
                    }

                    needsListeners = _image != null && (Toolkit.getImageAccessor().isAnimation(_image)
                            || _image.getProgress() < 1);

                    if (needsListeners) {
                        Toolkit.getImageAccessor().getImageProperty(_image).
                                addListener(platformImageChangeListener.getWeakListener());
                    }

                    oldSelfIlluminationMap = _image;
                    selfIlluminationMapDirty = true;
                    setDirty(true);
                }
            };
        }
        return selfIlluminationMap;
    }

    @Override
    void setDirty(boolean value) {
        super.setDirty(value);
        if (!value) {
            diffuseColorDirty = false;
            specularColorDirty = false;
            specularPowerDirty = false;
            diffuseMapDirty = false;
            specularMapDirty = false;
            bumpMapDirty = false;
            selfIlluminationMapDirty = false;
        }
    }

    /** The peer node created by the graphics Toolkit/Pipeline implementation */
    private NGPhongMaterial peer;

    @Override
    NGPhongMaterial getNGMaterial() {
        if (peer == null) {
            peer = new NGPhongMaterial();
        }
        return peer;
    }

    @Override
    void updatePG(){
        if (!isDirty()) {
            return;
        }

        final NGPhongMaterial pMaterial = MaterialHelper.getNGMaterial(this);
        if (diffuseColorDirty) {
            pMaterial.setDiffuseColor(getDiffuseColor() == null ? null
                    : Toolkit.getPaintAccessor().getPlatformPaint(getDiffuseColor()));
        }
        if (specularColorDirty) {
            pMaterial.setSpecularColor(getSpecularColor() == null ? null
                    : Toolkit.getPaintAccessor().getPlatformPaint(getSpecularColor()));
        }
        if (specularPowerDirty) {
            pMaterial.setSpecularPower((float)getSpecularPower());
        }
        if (diffuseMapDirty) {
            pMaterial.setDiffuseMap(getDiffuseMap()
                    == null ? null : Toolkit.getImageAccessor().getPlatformImage(getDiffuseMap()));
        }
        if (specularMapDirty) {
            pMaterial.setSpecularMap(getSpecularMap()
                    == null ? null : Toolkit.getImageAccessor().getPlatformImage(getSpecularMap()));
        }
        if (bumpMapDirty) {
            pMaterial.setBumpMap(getBumpMap()
                    == null ? null : Toolkit.getImageAccessor().getPlatformImage(getBumpMap()));
        }
        if (selfIlluminationMapDirty) {
            pMaterial.setSelfIllumMap(getSelfIlluminationMap()
                    == null ? null : Toolkit.getImageAccessor().getPlatformImage(getSelfIlluminationMap()));
        }

        setDirty(false);
    }

    @Override public String toString() {
        return "PhongMaterial[" + "diffuseColor=" + getDiffuseColor() +
                ", specularColor=" + getSpecularColor() +
                ", specularPower=" + getSpecularPower() +
                ", diffuseMap=" + getDiffuseMap() +
                ", specularMap=" + getSpecularMap() +
                ", bumpMap=" + getBumpMap() +
                ", selfIlluminationMap=" + getSelfIlluminationMap() + "]";
    }

}
