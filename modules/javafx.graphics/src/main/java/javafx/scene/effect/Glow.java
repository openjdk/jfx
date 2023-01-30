/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.util.Utils;
import com.sun.javafx.effect.EffectDirtyBits;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.BoundsAccessor;


/**
 * A high-level effect that makes the input image appear to glow,
 * based on a configurable threshold.
 *
 * <p>
 * Example:
 * <pre>{@code
 * Image image = new Image("boat.jpg");
 * ImageView imageView = new ImageView(image);
 * imageView.setFitWidth(200);
 * imageView.setPreserveRatio(true);
 *
 * imageView.setEffect(new Glow(0.8));
 * }</pre>
 * <p> The code above applied on this image: </p>
 * <p>
 * <img src="doc-files/photo.png" alt="A photo">
 * </p>
 * <p> produces the following: </p>
 * <p>
 * <img src="doc-files/glow.png" alt="The visual effect of Glow on photo">
 * </p>
 * @since JavaFX 2.0
 */
public class Glow extends Effect {
    /**
     * Creates a new instance of Glow with default parameters.
     */
    public Glow() {}

    /**
     * Creates a new instance of Glow with specified level.
     * @param level the level value, which controls the intensity
     * of the glow effect
     */
    public Glow(double level) {
        setLevel(level);
    }

    @Override
    com.sun.scenario.effect.Glow createPeer() {
        return new com.sun.scenario.effect.Glow();
    }
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
     * The level value, which controls the intensity of the glow effect.
     * <pre>
     *       Min: 0.0
     *       Max: 1.0
     *   Default: 0.3
     *  Identity: 0.0
     * </pre>
     * @defaultValue 0.3
     */
    private DoubleProperty level;


    public final void setLevel(double value) {
        levelProperty().set(value);
    }

    public final double getLevel() {
        return level == null ? 0.3 : level.get();
    }

    public final DoubleProperty levelProperty() {
        if (level == null) {
            level = new DoublePropertyBase(0.3) {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                }

                @Override
                public Object getBean() {
                    return Glow.this;
                }

                @Override
                public String getName() {
                    return "level";
                }
            };
        }
        return level;
    }

    @Override
    void update() {
        Effect localInput = getInput();
        if (localInput != null) {
            localInput.sync();
        }

        com.sun.scenario.effect.Glow peer =
                (com.sun.scenario.effect.Glow) getPeer();
        peer.setInput(localInput == null ? null : localInput.getPeer());
        peer.setLevel((float)Utils.clamp(0, getLevel(), 1));
    }

    @Override
    BaseBounds getBounds(BaseBounds bounds,
                         BaseTransform tx,
                         Node node,
                         BoundsAccessor boundsAccessor) {
        return getInputBounds(bounds, tx, node, boundsAccessor, getInput());
    }

    @Override
    Effect copy() {
        return new Glow(this.getLevel());
    }
}
