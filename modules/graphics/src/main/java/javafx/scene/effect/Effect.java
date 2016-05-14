/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.Observable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Node;

import com.sun.javafx.effect.EffectDirtyBits;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.BoundsAccessor;
import com.sun.scenario.effect.EffectHelper;

/**
 * The abstract base class for all effect implementations.
 * An effect is a graphical algorithm that produces an image, typically
 * as a modification of a source image.
 * An effect can be associated with a scene graph {@code Node} by setting
 * the {@link javafx.scene.Node#effectProperty Node.effect} attribute.
 * Some effects change the color properties of the source pixels
 * (such as {@link ColorAdjust}),
 * others combine multiple images together (such as {@link Blend}),
 * while still others warp or move the pixels of the source image around
 * (such as {@link DisplacementMap} or {@link PerspectiveTransform}).
 * All effects have at least one input defined and the input can be set
 * to another effect to chain the effects together and combine their
 * results, or it can be left unspecified in which case the effect will
 * operate on a graphical rendering of the node it is attached to.
 * <p>
 * Note: this is a conditional feature. See
 * {@link javafx.application.ConditionalFeature#EFFECT ConditionalFeature.EFFECT}
 * for more information.
 * @since JavaFX 2.0
 */
public abstract class Effect {
    static {
        // This is used by classes in different packages to get access to
        // private and package private methods.
        EffectHelper.setEffectAccessor(new EffectHelper.EffectAccessor() {

            @Override
            public com.sun.scenario.effect.Effect getPeer(Effect effect) {
                return effect.getPeer();
            }

            @Override
            public void sync(Effect effect) {
                effect.sync();
            }

            @Override
            public IntegerProperty effectDirtyProperty(Effect effect) {
                return effect.effectDirtyProperty();
            }

            @Override
            public boolean isEffectDirty(Effect effect) {
                return effect.isEffectDirty();
            }

            @Override
            public BaseBounds getBounds(Effect effect, BaseBounds bounds,
                    BaseTransform tx, Node node, BoundsAccessor boundsAccessor) {
                return effect.getBounds(bounds, tx, node, boundsAccessor);
            }

            @Override
            public Effect copy(Effect effect) {
                return effect.copy();
            }

            @Override
            public com.sun.scenario.effect.Blend.Mode getToolkitBlendMode(BlendMode mode) {
                return Blend.getToolkitMode(mode);
            }
        });
    }

    /**
     * Creates a new Effect.
     */
    protected Effect() {
       markDirty(EffectDirtyBits.EFFECT_DIRTY);
    }

    void effectBoundsChanged() {
        toggleDirty(EffectDirtyBits.BOUNDS_CHANGED);
    }

    private com.sun.scenario.effect.Effect peer;
    abstract com.sun.scenario.effect.Effect createPeer();

    com.sun.scenario.effect.Effect getPeer() {
        if (peer == null) {
            peer = createPeer();
        }
        return peer;
    }

    // effect is marked dirty in the constructor, so we don't need to be lazy here
    private IntegerProperty effectDirty =
            new SimpleIntegerProperty(this, "effectDirty");

    private void setEffectDirty(int value) {
        effectDirtyProperty().set(value);
    }

    private final IntegerProperty effectDirtyProperty() {
        return effectDirty;
    }

    private final boolean isEffectDirty() {
        return isEffectDirty(EffectDirtyBits.EFFECT_DIRTY);
    }

    /**
     * Set the specified dirty bit
     */
    final void markDirty(EffectDirtyBits dirtyBit) {
        setEffectDirty(effectDirty.get() | dirtyBit.getMask());
    }

    /**
     * Toggle the specified dirty bit
     */
    private void toggleDirty(EffectDirtyBits dirtyBit) {
        setEffectDirty(effectDirty.get() ^ dirtyBit.getMask());
    }

    /**
     * Test the specified dirty bit
     */
    private boolean isEffectDirty(EffectDirtyBits dirtyBit) {
        return ((effectDirty.get() & dirtyBit.getMask()) != 0);
    }

    /**
     * Clear the specified dirty bit
     */
    private void clearEffectDirty(EffectDirtyBits dirtyBit) {
        setEffectDirty(effectDirty.get() & ~dirtyBit.getMask());
    }

    final void sync() {
        if (isEffectDirty(EffectDirtyBits.EFFECT_DIRTY)) {
            update();
            clearEffectDirty(EffectDirtyBits.EFFECT_DIRTY);
        }
    }

    abstract void update();

    abstract boolean checkChainContains(Effect e);

    boolean containsCycles(Effect value) {
        if (value != null
                && (value == this || value.checkChainContains(this))) {
            return true;
        }
        return false;
    }

    class EffectInputChangeListener extends EffectChangeListener {
        private int oldBits;

        public void register(Effect value) {
            super.register(value == null? null: value.effectDirtyProperty());
            if (value != null) {
                oldBits = value.effectDirtyProperty().get();
            }
        }

        @Override
        public void invalidated(Observable valueModel) {
            int newBits = ((IntegerProperty)valueModel).get();
            int dirtyBits = newBits ^ oldBits;
            oldBits = newBits;
            if (EffectDirtyBits.isSet(dirtyBits, EffectDirtyBits.EFFECT_DIRTY)
                && EffectDirtyBits.isSet(newBits, EffectDirtyBits.EFFECT_DIRTY)) {
                    markDirty(EffectDirtyBits.EFFECT_DIRTY);
            }
            if (EffectDirtyBits.isSet(dirtyBits, EffectDirtyBits.BOUNDS_CHANGED)) {
                toggleDirty(EffectDirtyBits.BOUNDS_CHANGED);
            }
        }
    }

    class EffectInputProperty extends ObjectPropertyBase<Effect> {
        private final String propertyName;

        private Effect validInput = null;

        private final EffectInputChangeListener effectChangeListener =
                new EffectInputChangeListener();

        public EffectInputProperty(final String propertyName) {
            this.propertyName = propertyName;
        }

        @Override
        public void invalidated() {
            final Effect newInput = super.get();
            if (containsCycles(newInput)) {
                if (isBound()) {
                    unbind();
                    set(validInput);
                    throw new IllegalArgumentException("Cycle in effect chain "
                            + "detected, binding was set to incorrect value, "
                            + "unbinding the input property");
                } else {
                    set(validInput);
                    throw new IllegalArgumentException("Cycle in effect chain detected");
                }
            }
            validInput = newInput;
            effectChangeListener.register(newInput);
            markDirty(EffectDirtyBits.EFFECT_DIRTY);

            // we toggle dirty flag for bounds on this effect to notify
            // "consumers" of this effect that bounds have changed
            //
            // bounds of this effect might change
            // even if bounds of chained effect are not dirty
            effectBoundsChanged();
        }

        @Override
        public Object getBean() {
            return Effect.this;
        }

        @Override
        public String getName() {
            return propertyName;
        }
    }

   /**
    * Returns bounds of given node with applied effect.
    *
    * We *never* pass null in as a bounds. This method will
    * NOT take a null bounds object. The returned value may be
    * the same bounds object passed in, or it may be a new object.
    */
    abstract BaseBounds getBounds(BaseBounds bounds,
                                  BaseTransform tx,
                                  Node node,
                                  BoundsAccessor boundsAccessor);

    abstract Effect copy();

    static BaseBounds transformBounds(BaseTransform tx, BaseBounds r) {
        if (tx == null || tx.isIdentity()) {
            return r;
        }
        BaseBounds ret = new RectBounds();
        ret = tx.transform(r, ret);
        return ret;
    }

    // utility method used in calculation of bounds in BoxBlur and DropShadow effects
    static int getKernelSize(float fsize, int iterations) {
        int ksize = (int) Math.ceil(fsize);
        if (ksize < 1) ksize = 1;
        ksize = (ksize-1) * iterations + 1;
        ksize |= 1;
        return ksize / 2;
    }

    // utility method used for calculation of bounds in Shadow and DropShadow effects
    static BaseBounds getShadowBounds(BaseBounds bounds,
                                      BaseTransform tx,
                                      float width,
                                      float height,
                                      BlurType blurType) {
        int hgrow = 0;
        int vgrow = 0;

        switch (blurType) {
            case GAUSSIAN:
                float hradius = width < 1.0f ? 0.0f : ((width - 1.0f) / 2.0f);
                float vradius = height < 1.0f ? 0.0f : ((height - 1.0f) / 2.0f);
                hgrow = (int) Math.ceil(hradius);
                vgrow = (int) Math.ceil(vradius);
                break;
            case ONE_PASS_BOX:
                hgrow = getKernelSize(Math.round(width/3.0f), 1);
                vgrow = getKernelSize(Math.round(height/3.0f), 1);
                break;
            case TWO_PASS_BOX:
                hgrow = getKernelSize(Math.round(width/3.0f), 2);
                vgrow = getKernelSize(Math.round(height/3.0f), 2);
                break;
            case THREE_PASS_BOX:
                hgrow = getKernelSize(Math.round(width/3.0f), 3);
                vgrow = getKernelSize(Math.round(height/3.0f), 3);
                break;
        }

        bounds = bounds.deriveWithPadding(hgrow, vgrow, 0);

        return transformBounds(tx, bounds);
    }

    // Returns input bounds for an effect. These are either bounds of input effect or
    // geometric bounds of the node on which the effect calling this method is applied.
    static BaseBounds getInputBounds(BaseBounds bounds,
                                     BaseTransform tx,
                                     Node node,
                                     BoundsAccessor boundsAccessor,
                                     Effect input) {
        if (input != null) {
            bounds = input.getBounds(bounds, tx, node, boundsAccessor);
        } else {
            bounds = boundsAccessor.getGeomBounds(bounds, tx, node);
        }

        return bounds;
    }
}
