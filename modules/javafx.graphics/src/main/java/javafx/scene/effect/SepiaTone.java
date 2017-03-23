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

import com.sun.javafx.util.Utils;
import com.sun.javafx.effect.EffectDirtyBits;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.BoundsAccessor;


/**
 * A filter that produces a sepia tone effect, similar to antique photographs.
 *
 * <p>
 * Example:
 * <pre>{@code
 * SepiaTone sepiaTone = new SepiaTone();
 * sepiaTone.setLevel(0.7);
 *
 * Image image = new Image("boat.jpg");
 * ImageView imageView = new ImageView(image);
 * imageView.setFitWidth(200);
 * imageView.setPreserveRatio(true);
 * imageView.setEffect(sepiaTone);
 * }</pre>
 * <p> The code above applied on this image: </p>
 * <p>
 * <img src="doc-files/photo.png" alt="A photo">
 * </p>
 * <p> produces the following: </p>
 * <p>
 * <img src="doc-files/sepiatone.png" alt="The visual effect of SepiaTone on photo">
 * </p>
 * @since JavaFX 2.0
 */
public class SepiaTone extends Effect {
    /**
     * Creates a new instance of SepiaTone with default parameters.
     */
    public SepiaTone() {}

    /**
     * Creates a new instance of SepiaTone with the specified level.
     * @param level the level value, which controls the intensity of the effect
     * @since JavaFX 2.1
     */
    public SepiaTone(double level) {
        setLevel(level);
    }

    @Override
    com.sun.scenario.effect.SepiaTone createPeer() {
        return new com.sun.scenario.effect.SepiaTone();
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
     * The level value, which controls the intensity of the sepia effect.
     * <pre>
     *       Min: 0.0f
     *       Max: 1.0f
     *   Default: 1.0f
     *  Identity: 0.0f
     * </pre>
     * @defaultValue 1.0f
     */
    private DoubleProperty level;


    public final void setLevel(double value) {
        levelProperty().set(value);
    }

    public final double getLevel() {
        return level == null ? 1 : level.get();
    }

    public final DoubleProperty levelProperty() {
        if (level == null) {
            level = new DoublePropertyBase(1) {

                @Override
                public void invalidated() {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
                }

                @Override
                public Object getBean() {
                    return SepiaTone.this;
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

        com.sun.scenario.effect.SepiaTone peer =
                (com.sun.scenario.effect.SepiaTone) getPeer();
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
        SepiaTone st = new SepiaTone(this.getLevel());
        st.setInput(this.getInput());
        return st;

    }
}
