/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.BoundsAccessor;

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
 */
public abstract class Effect {
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
     abstract com.sun.scenario.effect.Effect impl_createImpl();

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public com.sun.scenario.effect.Effect impl_getImpl() {
        if (peer == null) {
            peer = impl_createImpl();
        }
        return peer;
    }

    // effect is marked dirty in the constructor, so we don't need to be lazy here
    private IntegerProperty effectDirty =
            new SimpleIntegerProperty(this, "effectDirty");

    private void setEffectDirty(int value) {
        impl_effectDirtyProperty().set(value);
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public final IntegerProperty impl_effectDirtyProperty() {
        return effectDirty;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public final boolean impl_isEffectDirty() {
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

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public final void impl_sync() {
        if (isEffectDirty(EffectDirtyBits.EFFECT_DIRTY)) {
            impl_update();
            clearEffectDirty(EffectDirtyBits.EFFECT_DIRTY);
        }
    }

    abstract void impl_update();

    abstract boolean impl_checkChainContains(Effect e);

    boolean impl_containsCycles(Effect value) {
        if (value != null
                && (value == this || value.impl_checkChainContains(this))) {
            return true;
        }
        return false;
    }

    class EffectInputChangeListener extends EffectChangeListener {
        private int oldBits;

        public void register(Effect value) {
            super.register(value == null? null: value.impl_effectDirtyProperty());
            if (value != null) {
                oldBits = value.impl_effectDirtyProperty().get();
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
            if (impl_containsCycles(newInput)) {
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
    *
    * @treatAsPrivate implementation detail
    * @deprecated This is an internal API that is not intended for use and will be removed in the next version
    */
    @Deprecated
    public abstract BaseBounds impl_getBounds(BaseBounds bounds,
                                              BaseTransform tx,
                                              Node node,
                                              BoundsAccessor boundsAccessor);
    /**
     * 
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public abstract Effect impl_copy();
    
}
