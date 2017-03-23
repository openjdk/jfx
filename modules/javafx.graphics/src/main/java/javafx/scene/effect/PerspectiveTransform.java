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

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;

import com.sun.javafx.effect.EffectDirtyBits;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.BoundsAccessor;


/**
 * An effect that provides non-affine transformation of the input content.
 * Most typically {@code PerspectiveTransform} is used to provide a "faux"
 * three-dimensional effect for otherwise two-dimensional content.
 * <p>
 * A perspective transformation is capable of mapping an arbitrary
 * quadrilateral into another arbitrary quadrilateral, while preserving
 * the straightness of lines.  Unlike an affine transformation, the
 * parallelism of lines in the source is not necessarily preserved in the
 * output.
 * <p>
 * Note that this effect does not adjust the coordinates of input events
 * or any methods that measure containment on a {@code Node}.
 * The results of mouse picking and the containment methods are undefined
 * when a {@code Node} has a {@code PerspectiveTransform} effect in place.
 *
 * <p>
 * Example:
 * <pre>{@code
 * PerspectiveTransform perspectiveTrasform = new PerspectiveTransform();
 * perspectiveTrasform.setUlx(10.0);
 * perspectiveTrasform.setUly(10.0);
 * perspectiveTrasform.setUrx(310.0);
 * perspectiveTrasform.setUry(40.0);
 * perspectiveTrasform.setLrx(310.0);
 * perspectiveTrasform.setLry(60.0);
 * perspectiveTrasform.setLlx(10.0);
 * perspectiveTrasform.setLly(90.0);
 *
 * Group g = new Group();
 * g.setEffect(perspectiveTrasform);
 * g.setCache(true);
 *
 * Rectangle rect = new Rectangle();
 * rect.setX(10.0);
 * rect.setY(10.0);
 * rect.setWidth(280.0);
 * rect.setHeight(80.0);
 * rect.setFill(Color.web("0x3b596d"));
 *
 * Text text = new Text();
 * text.setX(20.0);
 * text.setY(65.0);
 * text.setText("Perspective");
 * text.setFill(Color.ALICEBLUE);
 * text.setFont(Font.font(null, FontWeight.BOLD, 36));
 *
 * g.getChildren().addAll(rect, text);
 * }</pre>
 * <p> The code above produces the following: </p>
 * <p>
 * <img src="doc-files/perspectivetransform.png" alt="The visual effect of
 * PerspectiveTransform on text">
 * </p>
 * @since JavaFX 2.0
 */
public class PerspectiveTransform extends Effect {
    /**
     * Creates a new instance of PerspectiveTransform with default parameters.
     */
    public PerspectiveTransform() {}

    /**
     * Creates a new instance of PerspectiveTransform with the specified ulx,
     * uly, urx, ury, lrx, lry, llx, and lly.
     * @param ulx the x coordinate of upper left corner
     * @param uly the y coordinate of upper left corner
     * @param urx the x coordinate of upper right corner
     * @param ury the y coordinate of upper right corner
     * @param lrx the x coordinate of lower right corner
     * @param lry the y coordinate of lower right corner
     * @param llx the x coordinate of lower left corner
     * @param lly the y coordinate of lower left corner
     * @since JavaFX 2.1
     */
    public PerspectiveTransform(double ulx, double uly,
                                double urx, double ury,
                                double lrx, double lry,
                                double llx, double lly) {
        setUlx(ulx); setUly(uly);
        setUrx(urx); setUry(ury);
        setLlx(llx); setLly(lly);
        setLrx(lrx); setLry(lry);
    }

    private void updateXform() {
        ((com.sun.scenario.effect.PerspectiveTransform) getPeer()).setQuadMapping(
                             (float)getUlx(), (float)getUly(),
                             (float)getUrx(), (float)getUry(),
                             (float)getLrx(), (float)getLry(),
                             (float)getLlx(), (float)getLly());
    }

    @Override
    com.sun.scenario.effect.PerspectiveTransform createPeer() {
        return new com.sun.scenario.effect.PerspectiveTransform();
    };
    /**
     * The input for this {@code Effect}.
     * If set to {@code null}, or left unspecified, a graphical image of
     * the {@code Node} to which the {@code Effect} is attached will be
     * used as the input.
     * @defaultValue null
     */
    private ObjectProperty<Effect> input;


    public final void setInput(Effect value) {
        inputProperty().set(value);
    }

    public final Effect getInput() {
        return input == null ? null : input.get();
    }

    public final ObjectProperty<Effect> inputProperty() {
        if (input == null) {
            input = new EffectInputProperty("input");
        }
        return input;
    }

    @Override
    boolean checkChainContains(Effect e) {
        Effect localInput = getInput();
        if (localInput == null)
            return false;
        if (localInput == e)
            return true;
        return localInput.checkChainContains(e);
    }

    /**
     * The x coordinate of the output location onto which the upper left
     * corner of the source is mapped.
     * @defaultValue 0.0
     */
    private DoubleProperty ulx;


    public final void setUlx(double value) {
        ulxProperty().set(value);
    }

    public final double getUlx() {
        return ulx == null ? 0 : ulx.get();
    }

    public final DoubleProperty ulxProperty() {
        if (ulx == null) {
            ulx = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                    effectBoundsChanged();
                }

                @Override
                public Object getBean() {
                    return PerspectiveTransform.this;
                }

                @Override
                public String getName() {
                    return "ulx";
                }
            };
        }
        return ulx;
    }

    /**
     * The y coordinate of the output location onto which the upper left
     * corner of the source is mapped.
     * @defaultValue 0.0
     */
    private DoubleProperty uly;


    public final void setUly(double value) {
        ulyProperty().set(value);
    }

    public final double getUly() {
        return uly == null ? 0 : uly.get();
    }

    public final DoubleProperty ulyProperty() {
        if (uly == null) {
            uly = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                    effectBoundsChanged();
                }

                @Override
                public Object getBean() {
                    return PerspectiveTransform.this;
                }

                @Override
                public String getName() {
                    return "uly";
                }
            };
        }
        return uly;
    }

    /**
     * The x coordinate of the output location onto which the upper right
     * corner of the source is mapped.
     * @defaultValue 0.0
     */
    private DoubleProperty urx;


    public final void setUrx(double value) {
        urxProperty().set(value);
    }

    public final double getUrx() {
        return urx == null ? 0 : urx.get();
    }

    public final DoubleProperty urxProperty() {
        if (urx == null) {
            urx = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                    effectBoundsChanged();
                }

                @Override
                public Object getBean() {
                    return PerspectiveTransform.this;
                }

                @Override
                public String getName() {
                    return "urx";
                }
            };
        }
        return urx;
    }

    /**
     * The y coordinate of the output location onto which the upper right
     * corner of the source is mapped.
     * @defaultValue 0.0
     */
    private DoubleProperty ury;


    public final void setUry(double value) {
        uryProperty().set(value);
    }

    public final double getUry() {
        return ury == null ? 0 : ury.get();
    }

    public final DoubleProperty uryProperty() {
        if (ury == null) {
            ury = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                    effectBoundsChanged();
                }

                @Override
                public Object getBean() {
                    return PerspectiveTransform.this;
                }

                @Override
                public String getName() {
                    return "ury";
                }
            };
        }
        return ury;
    }

    /**
     * The x coordinate of the output location onto which the lower right
     * corner of the source is mapped.
     * @defaultValue 0.0
     */
    private DoubleProperty lrx;


    public final void setLrx(double value) {
        lrxProperty().set(value);
    }

    public final double getLrx() {
        return lrx == null ? 0 : lrx.get();
    }

    public final DoubleProperty lrxProperty() {
        if (lrx == null) {
            lrx = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                    effectBoundsChanged();
                }

                @Override
                public Object getBean() {
                    return PerspectiveTransform.this;
                }

                @Override
                public String getName() {
                    return "lrx";
                }
            };
        }
        return lrx;
    }

    /**
     * The y coordinate of the output location onto which the lower right
     * corner of the source is mapped.
     * @defaultValue 0.0
     */
    private DoubleProperty lry;


    public final void setLry(double value) {
        lryProperty().set(value);
    }

    public final double getLry() {
        return lry == null ? 0 : lry.get();
    }

    public final DoubleProperty lryProperty() {
        if (lry == null) {
            lry = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                    effectBoundsChanged();
                }

                @Override
                public Object getBean() {
                    return PerspectiveTransform.this;
                }

                @Override
                public String getName() {
                    return "lry";
                }
            };
        }
        return lry;
    }

    /**
     * The x coordinate of the output location onto which the lower left
     * corner of the source is mapped.
     * @defaultValue 0.0
     */
    private DoubleProperty llx;


    public final void setLlx(double value) {
        llxProperty().set(value);
    }

    public final double getLlx() {
        return llx == null ? 0 : llx.get();
    }

    public final DoubleProperty llxProperty() {
        if (llx == null) {
            llx = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                    effectBoundsChanged();
                }

                @Override
                public Object getBean() {
                    return PerspectiveTransform.this;
                }

                @Override
                public String getName() {
                    return "llx";
                }
            };
        }
        return llx;
    }

    /**
     * The y coordinate of the output location onto which the lower left
     * corner of the source is mapped.
     * @defaultValue 0.0
     */
    private DoubleProperty lly;


    public final void setLly(double value) {
        llyProperty().set(value);
    }

    public final double getLly() {
        return lly == null ? 0 : lly.get();
    }

    public final DoubleProperty llyProperty() {
        if (lly == null) {
            lly = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                    effectBoundsChanged();
                }

                @Override
                public Object getBean() {
                    return PerspectiveTransform.this;
                }

                @Override
                public String getName() {
                    return "lly";
                }
            };
        }
        return lly;
    }

    @Override
    void update() {
        Effect localInput = getInput();
        if (localInput != null) {
            localInput.sync();
        }

        ((com.sun.scenario.effect.PerspectiveTransform)getPeer())
            .setInput(localInput == null ? null : localInput.getPeer());
        updateXform();
    }

    private float devcoords[] = new float[8];

    @Override
    BaseBounds getBounds(BaseBounds bounds,
                         BaseTransform tx,
                         Node node,
                         BoundsAccessor boundsAccessor) {
        setupDevCoords(tx);

        float minx, miny, maxx, maxy;
        minx = maxx = devcoords[0];
        miny = maxy = devcoords[1];
        for (int i = 2; i < devcoords.length; i += 2) {
            if (minx > devcoords[i]) minx = devcoords[i];
            else if (maxx < devcoords[i]) maxx = devcoords[i];
            if (miny > devcoords[i+1]) miny = devcoords[i+1];
            else if (maxy < devcoords[i+1]) maxy = devcoords[i+1];
        }

        return new RectBounds(minx, miny, maxx, maxy);
    }

    private void setupDevCoords(BaseTransform transform) {
        devcoords[0] = (float)getUlx();
        devcoords[1] = (float)getUly();
        devcoords[2] = (float)getUrx();
        devcoords[3] = (float)getUry();
        devcoords[4] = (float)getLrx();
        devcoords[5] = (float)getLry();
        devcoords[6] = (float)getLlx();
        devcoords[7] = (float)getLly();
        transform.transform(devcoords, 0, devcoords, 0, 4);
    }

    @Override
    Effect copy() {
        return new PerspectiveTransform(this.getUlx(), this.getUly(),
                this.getUrx(), this.getUry(), this.getLrx(), this.getLry(),
                this.getLlx(), this.getLly());
    }
}
