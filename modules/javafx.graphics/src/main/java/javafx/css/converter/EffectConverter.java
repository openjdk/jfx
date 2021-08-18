/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javafx.css.converter;

import javafx.css.Size;
import javafx.css.ParsedValue;
import javafx.css.StyleConverter;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.effect.InnerShadow;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.Map;

/**
 * Converter to convert a string representation of an {@code Effect} to an {@code Effect}.
 * @since 9
 */
public class EffectConverter extends StyleConverter<ParsedValue[], Effect> {

    // lazy, thread-safe instatiation
    private static class Holder {
        static final EffectConverter EFFECT_CONVERTER =
                new EffectConverter();
        static final DropShadowConverter DROP_SHADOW_INSTANCE =
                new DropShadowConverter();
        static final InnerShadowConverter INNER_SHADOW_INSTANCE =
                new InnerShadowConverter();
    }

    /**
     * Gets the {@code EffectConverter} instance.
     * @return the {@code EffectConverter} instance
     */
    public static StyleConverter<ParsedValue[], Effect> getInstance() {
        return Holder.EFFECT_CONVERTER;
    }

    @Override
    public Effect convert(ParsedValue<ParsedValue[], Effect> value, Font font) {
        throw new IllegalArgumentException("Parsed value is not an Effect");
    }

    /**
     * Constructs an EffectConverter.
     */
    protected EffectConverter() {
        super();
    }

    @Override
    public String toString() {
        return "EffectConverter";
    }

    /**
     * Converter to convert a {@code DropShadow} effect.
     * @since 9
     */
    public static final class DropShadowConverter extends EffectConverter {

        /**
         * Gets the {@code DropShadowConverter} instance.
         * @return the {@code DropShadowConverter} instance
         */
        public static DropShadowConverter getInstance() {
            return Holder.DROP_SHADOW_INSTANCE;
        }

        private DropShadowConverter() {
            super();
        }

        // dropshadow( <blur-type> , <color> , <number> , <number> , <number> , <number> )
        // <blur-type> = [      gaussian | one-pass-box | three-pass-box | two-pass-box ]
        // <color> The shadow Color.
        // <number> The radius of the shadow blur kernel. In the range
        //          [0.0 ... 127.0], typical value 10.
        // <number> he spread of the shadow. The spread is the portion of
        //          the radius where the contribution of the source material
        //          will be 100%. The remaining portion of the radius will
        //          have a contribution controlled by the blur kernel. A
        //          spread of 0.0 will result in a distribution of the shadow
        //          determined entirely by the blur algorithm. A spread of
        //          1.0 will result in a solid growth outward of the source
        //          material opacity to the limit of the radius with a very
        //          sharp cutoff to transparency at the radius. In the range
        //          [0.0 ... 1.0] a value of 0.0 is no spread.
        // <number> The shadow offset in the x direction, in pixels.
        // <number> The shadow offset in the y direction, in pixels.
        @Override
        public Effect convert(ParsedValue<ParsedValue[], Effect> value, Font font) {

            Effect effect = super.getCachedValue(value);
            if (effect != null) return effect;

            final ParsedValue[] values = value.getValue();
            final BlurType blurType = (BlurType) values[0].convert(font);
            final Color color = (Color) values[1].convert(font);
            final Double radius = ((Size) values[2].convert(font)).pixels(font);
            final Double spread = ((Size) values[3].convert(font)).pixels(font);
            final Double offsetX = ((Size) values[4].convert(font)).pixels(font);
            final Double offsetY = ((Size) values[5].convert(font)).pixels(font);
            DropShadow dropShadow = new DropShadow();
            if (blurType != null) {
                dropShadow.setBlurType(blurType);
            }
            if (color != null) {
                dropShadow.setColor(color);
            }
            if (spread != null) {
                dropShadow.setSpread(spread);
            }
            if (radius != null) {
                dropShadow.setRadius(radius);
            }
            if (offsetX != null) {
                dropShadow.setOffsetX(offsetX);
            }
            if (offsetY != null) {
                dropShadow.setOffsetY(offsetY);
            }

            super.cacheValue(value, dropShadow);

            return dropShadow;
        }

        @Override
        public String toString() {
            return "DropShadowConverter";
        }
    }

    /**
     * Converter to convert an {@code InnerShadow} effect.
     * @since 9
     */
    public static final class InnerShadowConverter extends EffectConverter {

        /**
         * Gets the {@code InnerShadowConverter} instance.
         * @return the {@code InnerShadowConverter} instance
         */
        public static InnerShadowConverter getInstance() {
            return Holder.INNER_SHADOW_INSTANCE;
        }

        private InnerShadowConverter() {
            super();
        }

        // innershadow( <blur-type> , <color> , <number> , <number> , <number> , <number> )
        // <blur-type> = [      gaussian | one-pass-box | three-pass-box | two-pass-box ]
        // <color> The shadow Color.
        // <number> The radius of the shadow blur kernel. In the range
        //          [0.0 ... 127.0], typical value 10.
        // <number> The choke of the shadow. The choke is the portion of the
        //          radius where the contribution of the source material will
        //          be 100%. The remaining portion of the radius will have a
        //          contribution controlled by the blur kernel. A choke of 0.0
        //          will result in a distribution of the shadow determined
        //          entirely by the blur algorithm. A choke of 1.0 will result
        //          in a solid growth inward of the shadow from the edges to
        //          the limit of the radius with a very sharp cutoff to
        //          transparency inside the radius. In the range [0.0 ... 1.0]
        //          a value of 0.0 is no spread.
        // <number> The shadow offset in the x direction, in pixels.
        // <number> The shadow offset in the y direction, in pixels.
        @Override
        public Effect convert(ParsedValue<ParsedValue[], Effect> value, Font font) {

            Effect effect = super.getCachedValue(value);
            if (effect != null) return effect;

            final ParsedValue[] values = value.getValue();
            final BlurType blurType = (BlurType) values[0].convert(font);
            final Color color = (Color) values[1].convert(font);
            final Double radius = ((Size) values[2].convert(font)).pixels(font);
            final Double choke = ((Size) values[3].convert(font)).pixels(font);
            final Double offsetX = ((Size) values[4].convert(font)).pixels(font);
            final Double offsetY = ((Size) values[5].convert(font)).pixels(font);
            InnerShadow innerShadow = new InnerShadow();
            if (blurType != null) {
                innerShadow.setBlurType(blurType);
            }
            if (color != null) {
                innerShadow.setColor(color);
            }
            if (radius != null) {
                innerShadow.setRadius(radius);
            }
            if (choke != null) {
                innerShadow.setChoke(choke);
            }
            if (offsetX != null) {
                innerShadow.setOffsetX(offsetX);
            }
            if (offsetY != null) {
                innerShadow.setOffsetY(offsetY);
            }

            super.cacheValue(value, innerShadow);

            return innerShadow;
        }

        @Override
        public String toString() {
            return "InnerShadowConverter";
        }
    }

    private static Map<ParsedValue<ParsedValue[], Effect>, Effect> cache;

    /**
     * Clears the InnerShadowConverter cache.
     */
    public static void clearCache() { if (cache != null) cache.clear(); }

}

