/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.effect;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.paint.Color;

import com.sun.javafx.util.Utils;
import com.sun.javafx.tk.Toolkit;

/**
 * The abstract base class for all light implementations.
 * @since JavaFX 2.0
 */
public abstract class Light {

    /**
     * Creates a new Light.
     */
    protected Light() {
        markDirty();
    }

    abstract com.sun.scenario.effect.light.Light createPeer();
    private com.sun.scenario.effect.light.Light peer;

    com.sun.scenario.effect.light.Light getPeer() {
        if (peer == null) {
            peer = createPeer();
        }
        return peer;
    }
    /**
     * The color of the light source.
     * <pre>
     *       Min: n/a
     *       Max: n/a
     *   Default: Color.WHITE
     *  Identity: n/a
     * </pre>
     * @defaultValue WHITE
     */
    private ObjectProperty<Color> color;

    public final void setColor(Color value) {
        colorProperty().set(value);
    }

    public final Color getColor() {
        return color == null ? Color.WHITE : color.get();
    }

    public final ObjectProperty<Color> colorProperty() {
        if (color == null) {
            color = new ObjectPropertyBase<Color>(Color.WHITE) {

                @Override
                public void invalidated() {
                    markDirty();
                }

                @Override
                public Object getBean() {
                    return Light.this;
                }

                @Override
                public String getName() {
                    return "color";
                }
            };
        }
        return color;
    }

    void sync() {
        if (isEffectDirty()) {
            update();
            clearDirty();
        }
    }

   private Color getColorInternal() {
        Color c = getColor();
        return c == null ? Color.WHITE : c;
    }

    void update() {
        getPeer().setColor(Toolkit.getToolkit().toColor4f(getColorInternal()));
    }

    private BooleanProperty effectDirty;

    private void setEffectDirty(boolean value) {
        effectDirtyProperty().set(value);
    }

    final BooleanProperty effectDirtyProperty() {
        if (effectDirty == null) {
            effectDirty = new SimpleBooleanProperty(this, "effectDirty");
        }
        return effectDirty;
    }

    boolean isEffectDirty() {
        return effectDirty == null ? false : effectDirty.get();
    }

    final void markDirty() {
        setEffectDirty(true);
    }

    final void clearDirty() {
        setEffectDirty(false);
    }

    /**
     * Represents a distant light source.
     *
     * <p>
     * Example:
     * <pre>{@code
     * Light.Distant light = new Light.Distant();
     * light.setAzimuth(45.0);
     * light.setElevation(30.0);
     *
     * Lighting lighting = new Lighting();
     * lighting.setLight(light);
     * lighting.setSurfaceScale(5.0);
     *
     * Text text = new Text();
     * text.setText("Distant");
     * text.setFill(Color.STEELBLUE);
     * text.setFont(Font.font("null", FontWeight.BOLD, 80));
     * text.setX(10.0f);
     * text.setY(10.0f);
     * text.setTextOrigin(VPos.TOP);
     * text.setEffect(lighting);
     *
     * Rectangle rect = new Rectangle(300,150);
     * rect.setFill(Color.ALICEBLUE);
     * rect.setEffect(lighting);
     * }</pre>
     *
     * <p> The code above produces the following: </p>
     * <p> <img src="doc-files/lightdistant.png" alt="The visual effect of distant
     * Light on text"> </p>
     * @since JavaFX 2.0
     */
    public static class Distant extends Light {
       /**
        * Creates a new instance of Distant light with default parameters.
        */
        public Distant() {}

       /**
        * Creates a new instance of Distant light with the specified azimuth,
        * elevation, and color.
        * @param azimuth the azimuth of the light
        * @param elevation the elevation of the light
        * @param color the color of the light
        * @since JavaFX 2.1
        */
        public Distant(double azimuth, double elevation, Color color) {
           setAzimuth(azimuth);
           setElevation(elevation);
           setColor(color);
        }

        @Override
        com.sun.scenario.effect.light.DistantLight createPeer() {
            return new com.sun.scenario.effect.light.DistantLight();
        }
        /**
         * The azimuth of the light.  The azimuth is the direction angle
         * for the light source on the XY plane, in degrees.
         * <pre>
         *       Min:  n/a
         *       Max:  n/a
         *   Default: 45.0
         *  Identity:  n/a
         * </pre>
         * @defaultValue 45.0
         */
        private DoubleProperty azimuth;

        public final void setAzimuth(double value) {
            azimuthProperty().set(value);
        }

        public final double getAzimuth() {
            return azimuth == null ? 45 : azimuth.get();
        }

        public final DoubleProperty azimuthProperty() {
            if (azimuth == null) {
                azimuth = new DoublePropertyBase(45) {

                    @Override
                    public void invalidated() {
                        markDirty();
                    }

                    @Override
                    public Object getBean() {
                        return Distant.this;
                    }

                    @Override
                    public String getName() {
                        return "azimuth";
                    }
                };
            }
            return azimuth;
        }
        /**
         * The elevation of the light.  The elevation is the
         * direction angle for the light source on the YZ plane, in degrees.
         * <pre>
         *       Min:  n/a
         *       Max:  n/a
         *   Default: 45.0
         *  Identity:  n/a
         * </pre>
         * @defaultValue 45.0
         */
        private DoubleProperty elevation;

        public final void setElevation(double value) {
            elevationProperty().set(value);
        }

        public final double getElevation() {
            return elevation == null ? 45 : elevation.get();
        }

        public final DoubleProperty elevationProperty() {
            if (elevation == null) {
                elevation = new DoublePropertyBase(45) {

                    @Override
                    public void invalidated() {
                        markDirty();
                    }

                    @Override
                    public Object getBean() {
                        return Distant.this;
                    }

                    @Override
                    public String getName() {
                        return "elevation";
                    }
                };
            }
            return elevation;
        }

        @Override
        void update() {
            super.update();
            com.sun.scenario.effect.light.DistantLight peer =
                    (com.sun.scenario.effect.light.DistantLight) getPeer();
            peer.setAzimuth((float) getAzimuth());
            peer.setElevation((float) getElevation());
        }
    }

    /**
     * Represents a light source at a given position in 3D space.
     *
     * <p>
     * Example:
     * <pre>{@code
     * Light.Point light = new Light.Point();
     * light.setX(100);
     * light.setY(100);
     * light.setZ(50);
     *
     * Lighting lighting = new Lighting();
     * lighting.setLight(light);
     * lighting.setSurfaceScale(5.0);
     *
     * Text text = new Text();
     * text.setText("Point");
     * text.setFill(Color.STEELBLUE);
     * text.setFont(Font.font(null, FontWeight.BOLD, 80));
     * text.setX(10.0);
     * text.setY(10.0);
     * text.setTextOrigin(VPos.TOP);
     *
     * Rectangle rect = new Rectangle(250, 150);
     * rect.setFill(Color.ALICEBLUE);
     * rect.setEffect(lighting);
     * text.setEffect(lighting);
     * }</pre>
     *
     * <p> The code above produces the following: </p>
     * <p> <img src="doc-files/lightpoint.png" alt="The visual effect of point
     * Light on text"> </p>
     * @since JavaFX 2.0
     */
    public static class Point extends Light {
       /**
        * Creates a new instance of Point light with default parameters.
        */
        public Point() {}

       /**
        * Creates a new instance of Point light with the specified x, y, x, and
        * color.
        * @param x the x coordinate of the light position
        * @param y the y coordinate of the light position
        * @param z the z coordinate of the light position
        * @param color the color of the light
        * @since JavaFX 2.1
        */
        public Point(double x, double y, double z, Color color) {
           setX(x);
           setY(y);
           setZ(z);
           setColor(color);
        }

        @Override
        com.sun.scenario.effect.light.PointLight createPeer() {
            return new com.sun.scenario.effect.light.PointLight();
        }
        /**
         * The x coordinate of the light position.
         * <pre>
         *       Min: n/a
         *       Max: n/a
         *   Default: 0.0
         *  Identity: n/a
         * </pre>
         * @defaultValue 0.0
         */
        private DoubleProperty x;

        public final void setX(double value) {
            xProperty().set(value);
        }

        public final double getX() {
            return x == null ? 0 : x.get();
        }

        public final DoubleProperty xProperty() {
            if (x == null) {
                x = new DoublePropertyBase() {

                    @Override
                    public void invalidated() {
                        markDirty();
                    }

                    @Override
                    public Object getBean() {
                        return Point.this;
                    }

                    @Override
                    public String getName() {
                        return "x";
                    }
                };
            }
            return x;
        }
        /**
         * The y coordinate of the light position.
         * <pre>
         *       Min: n/a
         *       Max: n/a
         *   Default: 0.0
         *  Identity: n/a
         * </pre>
         * @defaultValue 0.0
         */
        private DoubleProperty y;

        public final void setY(double value) {
            yProperty().set(value);
        }

        public final double getY() {
            return y == null ? 0 : y.get();
        }

        public final DoubleProperty yProperty() {
            if (y == null) {
                y = new DoublePropertyBase() {

                    @Override
                    public void invalidated() {
                        markDirty();
                    }

                    @Override
                    public Object getBean() {
                        return Point.this;
                    }

                    @Override
                    public String getName() {
                        return "y";
                    }
                };
            }
            return y;
        }
        /**
         * The z coordinate of the light position.
         * <pre>
         *       Min: n/a
         *       Max: n/a
         *   Default: 0.0
         *  Identity: n/a
         * </pre>
         * @defaultValue 0.0
         */
        private DoubleProperty z;

        public final void setZ(double value) {
            zProperty().set(value);
        }

        public final double getZ() {
            return z == null ? 0 : z.get();
        }

        public final DoubleProperty zProperty() {
            if (z == null) {
                z = new DoublePropertyBase() {

                    @Override
                    public void invalidated() {
                        markDirty();
                    }

                    @Override
                    public Object getBean() {
                        return Point.this;
                    }

                    @Override
                    public String getName() {
                        return "z";
                    }
                };
            }
            return z;
        }

        @Override
        void update() {
            super.update();
            com.sun.scenario.effect.light.PointLight peer =
                    (com.sun.scenario.effect.light.PointLight) getPeer();
            peer.setX((float) getX());
            peer.setY((float) getY());
            peer.setZ((float) getZ());
        }
    }

    /**
     * Represents a spot light source at a given position in 3D space, with
     * configurable direction and focus.
     *
     * <p>
     * Example:
     * <pre>{@code
     * Light.Spot light = new Light.Spot();
     * light.setX(150);
     * light.setY(100);
     * light.setZ(80);
     * light.setPointsAtX(0);
     * light.setPointsAtY(0);
     * light.setPointsAtZ(-50);
     * light.setSpecularExponent(2);
     *
     * Lighting lighting = new Lighting();
     * lighting.setLight(light);
     * lighting.setSurfaceScale(5.0);
     *
     * Text text = new Text();
     * text.setText("Spot");
     * text.setFill(Color.STEELBLUE);
     * text.setFont(Font.font(null, FontWeight.BOLD, 80));
     * text.setX(10.0);
     * text.setY(10.0);
     * text.setTextOrigin(VPos.TOP);
     * text.setEffect(lighting);
     *
     * Rectangle rect = new Rectangle(200, 150);
     * rect.setFill(Color.ALICEBLUE);
     * rect.setEffect(lighting);
     * }</pre>
     *
     * <p> The code above produces the following: </p>
     * <p> <img src="doc-files/lightspot.png" alt="The visual effect of spot Light
     * on text"> </p>
     *
     * @since JavaFX 2.0
     */
    public static class Spot extends Light.Point {
       /**
        * Creates a new instance of Spot light with default parameters.
        */
        public Spot() {}

       /**
        * Creates a new instance of Spot light with the specified x, y, z,
        * specularExponent, and color.
        * @param x the x coordinate of the light position
        * @param y the y coordinate of the light position
        * @param z the z coordinate of the light position
        * @param specularExponent the specular exponent, which controls the
        * focus of the light source
        * @param color the color of the light
        * @since JavaFX 2.1
        */
        public Spot(double x, double y, double z, double specularExponent, Color color) {
           setX(x);
           setY(y);
           setZ(z);
           setSpecularExponent(specularExponent);
           setColor(color);
        }

        @Override
        com.sun.scenario.effect.light.SpotLight createPeer() {
            return new com.sun.scenario.effect.light.SpotLight();
        }
        /**
         * The x coordinate of the direction vector for this light.
         * <pre>
         *       Min: n/a
         *       Max: n/a
         *   Default: 0.0
         *  Identity: n/a
         * </pre>
         * @defaultValue 0.0
         */
        private DoubleProperty pointsAtX;

        public final void setPointsAtX(double value) {
            pointsAtXProperty().set(value);
        }

        public final double getPointsAtX() {
            return pointsAtX == null ? 0 : pointsAtX.get();
        }

        public final DoubleProperty pointsAtXProperty() {
            if (pointsAtX == null) {
                pointsAtX = new DoublePropertyBase() {

                    @Override
                    public void invalidated() {
                        markDirty();
                    }

                    @Override
                    public Object getBean() {
                        return Spot.this;
                    }

                    @Override
                    public String getName() {
                        return "pointsAtX";
                    }
                };
            }
            return pointsAtX;
        }
        /**
         * The y coordinate of the direction vector for this light.
         * <pre>
         *       Min: n/a
         *       Max: n/a
         *   Default: 0.0
         *  Identity: n/a
         * </pre>
         * @defaultValue 0.0
         */
        private DoubleProperty pointsAtY;

        public final void setPointsAtY(double value) {
            pointsAtYProperty().set(value);
        }

        public final double getPointsAtY() {
            return pointsAtY == null ? 0 : pointsAtY.get();
        }

        public final DoubleProperty pointsAtYProperty() {
            if (pointsAtY == null) {
                pointsAtY = new DoublePropertyBase() {

                    @Override
                    public void invalidated() {
                        markDirty();
                    }

                    @Override
                    public Object getBean() {
                        return Spot.this;
                    }

                    @Override
                    public String getName() {
                        return "pointsAtY";
                    }
                };
            }
            return pointsAtY;
        }
        /**
         * The z coordinate of the direction vector for this light.
         * <pre>
         *       Min: n/a
         *       Max: n/a
         *   Default: 0.0
         *  Identity: n/a
         * </pre>
         * @defaultValue 0.0
         */
        private DoubleProperty pointsAtZ;

        public final void setPointsAtZ(double value) {
            pointsAtZProperty().set(value);
        }

        public final double getPointsAtZ() {
            return pointsAtZ == null ? 0 : pointsAtZ.get();
        }

        public final DoubleProperty pointsAtZProperty() {
            if (pointsAtZ == null) {
                pointsAtZ = new DoublePropertyBase() {

                    @Override
                    public void invalidated() {
                        markDirty();
                    }

                    @Override
                    public Object getBean() {
                        return Spot.this;
                    }

                    @Override
                    public String getName() {
                        return "pointsAtZ";
                    }
                };
            }
            return pointsAtZ;
        }
        /**
         * The specular exponent, which controls the focus of this
         * light source.
         * <pre>
         *       Min: 0.0
         *       Max: 4.0
         *   Default: 1.0
         *  Identity: 1.0
         * </pre>
         * @defaultValue 1.0
         */
        private DoubleProperty specularExponent;

        public final void setSpecularExponent(double value) {
            specularExponentProperty().set(value);
        }

        public final double getSpecularExponent() {
            return specularExponent == null ? 1 : specularExponent.get();
        }

        public final DoubleProperty specularExponentProperty() {
            if (specularExponent == null) {
                specularExponent = new DoublePropertyBase(1) {

                    @Override
                    public void invalidated() {
                        markDirty();
                    }

                    @Override
                    public Object getBean() {
                        return Spot.this;
                    }

                    @Override
                    public String getName() {
                        return "specularExponent";
                    }
                };
            }
            return specularExponent;
        }

        @Override
        void update() {
            super.update();
            com.sun.scenario.effect.light.SpotLight peer =
                    (com.sun.scenario.effect.light.SpotLight) getPeer();
            peer.setPointsAtX((float) getPointsAtX());
            peer.setPointsAtY((float) getPointsAtY());
            peer.setPointsAtZ((float) getPointsAtZ());
            peer.setSpecularExponent((float) Utils.clamp(0, getSpecularExponent(), 4));
        }
    }
}
