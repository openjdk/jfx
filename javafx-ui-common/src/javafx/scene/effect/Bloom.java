/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.Utils;
import com.sun.javafx.effect.EffectDirtyBits;
import com.sun.javafx.effect.EffectUtils;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.BoundsAccessor;


/**
 * A high-level effect that makes brighter portions of the input image
 * appear to glow, based on a configurable threshold.
 *
<PRE>
import javafx.scene.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.scene.paint.*;
import javafx.scene.effect.*;

Group g = new Group();

Rectangle r = new Rectangle();
r.setX(10);
r.setY(10);
r.setWidth(160);
r.setHeight(80);
r.setFill(Color.DARKBLUE);

Text t = new Text();
t.setText("Bloom!");
t.setFill(Color.YELLOW);
t.setFont(Font.font(null, FontWeight.BOLD, 36));
t.setX(25);
t.setY(65);

g.setCache(true);
g.setEffect(new Bloom());
g.getChildren().add(r);
g.getChildren().add(t);
</PRE>
 */
public class Bloom extends Effect {
    /**
     * Creates a new instance of Bloom with default parameters.
     */
    public Bloom() {}

    /**
     * Creates a new instance of Bloom with the specified threshold.
     * @param threshold the threshold value for the bloom effect
     */
    public Bloom(double threshold) {
        setThreshold(threshold);
    }

    @Override
    com.sun.scenario.effect.Bloom impl_createImpl() {
        return new com.sun.scenario.effect.Bloom();
    };
    /**
     * The input for this {@code Effect}.
     * If set to {@code null}, or left unspecified, a graphical image of
     * the {@code Node} to which the {@code Effect} is attached will be
     * used as the input.
     * @defaultvalue null
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
    boolean impl_checkChainContains(Effect e) {
        Effect localInput = getInput();
        if (localInput == null)
            return false;
        if (localInput == e)
            return true;
        return localInput.impl_checkChainContains(e);
    }

    /**
     * The threshold value controls the minimum luminosity value of
     * the pixels that will be made to glow.
     * <pre>
     *       Min: 0.0
     *       Max: 1.0
     *   Default: 0.3
     *  Identity: n/a
     * </pre>
     * @defaultvalue 0.3
     */
    private DoubleProperty threshold;


    public final void setThreshold(double value) {
        thresholdProperty().set(value);
    }

    public final double getThreshold() {
        return threshold == null ? 0.3 : threshold.get();
    }

    public final DoubleProperty thresholdProperty() {
        if (threshold == null) {
            threshold = new DoublePropertyBase(0.3) {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                }

                @Override
                public Object getBean() {
                    return Bloom.this;
                }

                @Override
                public String getName() {
                    return "threshold";
                }
            };
        }
        return threshold;
    }

    @Override
    void impl_update() {
        Effect localInput = getInput();
        if (localInput != null) {
            localInput.impl_sync();
        }

        com.sun.scenario.effect.Bloom peer =
                (com.sun.scenario.effect.Bloom) impl_getImpl();
        peer.setInput(localInput == null ? null : localInput.impl_getImpl());
        peer.setThreshold((float)Utils.clamp(0, getThreshold(), 1));
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public BaseBounds impl_getBounds(BaseBounds bounds,
                                     BaseTransform tx,
                                     Node node,
                                     BoundsAccessor boundsAccessor) {
        return EffectUtils.getInputBounds(bounds, tx,
                                          node, boundsAccessor,
                                          getInput());
    }
}
