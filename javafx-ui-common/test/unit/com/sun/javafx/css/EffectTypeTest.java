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

package com.sun.javafx.css;

import static org.junit.Assert.assertEquals;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.effect.InnerShadow;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import org.junit.Test;

import com.sun.javafx.css.converters.EffectConverter;
import com.sun.javafx.css.parser.DeriveColorConverter;


public class EffectTypeTest {

    public EffectTypeTest() {
    }

    Size makeSize(float f) {
        return new Size(f, SizeUnits.PX);
    }

    InnerShadow getInnerShadow() {
        return new InnerShadow();
    }

    ParsedValue<ParsedValue[], Effect> getInnerShadowValue(InnerShadow is, boolean colorIsDerived) {
        Size offsetX = makeSize((float) is.getOffsetX());
        Size offsetY = makeSize((float) is.getOffsetX());
        Size choke = makeSize((float) is.getChoke());
        Size radius = makeSize((float) is.getRadius());

        ParsedValue<?,Color> colorVal;

        if (colorIsDerived) {
            ParsedValue[] values = new ParsedValue[] {
                new ParsedValue<Color,Color>(is.getColor(),null),
                new ParsedValue<Size,Size>(makeSize(0.5f),null)
            };
            colorVal = new ParsedValue<ParsedValue[],Color>(values, DeriveColorConverter.getInstance());
        } else {
            colorVal = new ParsedValue<Color,Color>(is.getColor(),null);
        }

        BlurType blurType = is.getBlurType();

        ParsedValue[] vals = new ParsedValue[] {
            new ParsedValue<BlurType,BlurType>(blurType, null),
            colorVal,
            new ParsedValue<Size,Size>(radius, null),
            new ParsedValue<Size,Size>(choke, null),
            new ParsedValue<Size,Size>(offsetX, null),
            new ParsedValue<Size,Size>(offsetY, null)
        };

        return new ParsedValue<ParsedValue[],Effect>(vals, EffectConverter.InnerShadowConverter.getInstance());
    }

    DropShadow getDropShadow() {
        return new DropShadow();
    }

    ParsedValue<ParsedValue[], Effect> getDropShadowValue(DropShadow ds, boolean colorIsDerived) {
        Size offsetX = makeSize((float) ds.getOffsetX());
        Size offsetY = makeSize((float) ds.getOffsetX());
        Size spread = makeSize((float) ds.getSpread());
        Size radius = makeSize((float) ds.getRadius());

        ParsedValue<?,Color> colorVal;

        if (colorIsDerived) {
            ParsedValue[] values = new ParsedValue[] {
                new ParsedValue<Color,Color>(ds.getColor(),null),
                new ParsedValue<Size,Size>(makeSize(0.5f),null)
            };
            colorVal = new ParsedValue<ParsedValue[],Color>(values, DeriveColorConverter.getInstance());
        } else {
            colorVal = new ParsedValue<Color,Color>(ds.getColor(),null);
        }

        BlurType blurType = ds.getBlurType();

        ParsedValue[] vals = new ParsedValue[] {
            new ParsedValue<BlurType,BlurType>(blurType, null),
            colorVal,
            new ParsedValue<Size,Size>(radius, null),
            new ParsedValue<Size,Size>(spread, null),
            new ParsedValue<Size,Size>(offsetX, null),
            new ParsedValue<Size,Size>(offsetY, null)
        };

        return new ParsedValue<ParsedValue[],Effect>(vals, EffectConverter.DropShadowConverter.getInstance());
    }

    void checkColor(String msg, Color c1, Color c2) {
        assertEquals(msg + ".red", c1.getRed(), c2.getRed(), 0.001);
        assertEquals(msg + ".blue", c1.getBlue(), c2.getBlue(), 0.001);
        assertEquals(msg + ".green", c1.getGreen(), c2.getGreen(), 0.001);
        assertEquals(msg + ".opacity", c1.getOpacity(), c2.getOpacity(), 0.001);
    }

    void checkInnerShadow(String msg, InnerShadow o1, InnerShadow o2) {
        assertEquals(msg + "innershadow.offsetX", o1.getOffsetX(), o2.getOffsetX(), 0.001);
        assertEquals(msg + "innershadow.offsety", o1.getOffsetY(), o2.getOffsetY(), 0.001);
        assertEquals(msg + "innershadow.choke", o1.getChoke(), o2.getChoke(), 0.001);
        assertEquals(msg + "innershadow.radius", o1.getRadius(), o2.getRadius(), 0.001);
        checkColor(msg + "innershadow", o1.getColor(), o2.getColor());
        assertEquals(msg + "innershadow.blurType", o1.getBlurType(), o2.getBlurType());
    }

    void checkDropShadow(String msg, DropShadow o1, DropShadow o2) {
        assertEquals(msg + "DropShadow.offsetX", o1.getOffsetX(), o2.getOffsetX(), 0.001);
        assertEquals(msg + "DropShadow.offsety", o1.getOffsetY(), o2.getOffsetY(), 0.001);
        assertEquals(msg + "DropShadow.spread", o1.getSpread(), o2.getSpread(), 0.001);
        assertEquals(msg + "DropShadow.radius", o1.getRadius(), o2.getRadius(), 0.001);
        checkColor(msg + "DropShadow", o1.getColor(), o2.getColor());
        assertEquals(msg + "DropShadow.blurType", o1.getBlurType(), o2.getBlurType());
    }

    /**
     * Test of convert method, of class EffectType.
     */
    @Test
    public void testConvert() {
        //System.out.println("convert");
        InnerShadow is = getInnerShadow();
        Font font = null;
        ParsedValue<ParsedValue[], Effect> value = getInnerShadowValue(is, false);
        Effect result = value.convert(font);
        checkInnerShadow("convert[1] ",  is, (InnerShadow)result);

        // Test with derived colors
        value = getInnerShadowValue(is, true);
        result = value.convert(font);
        // derived color is 50% of is.getColor()
        is.setColor(com.sun.javafx.Utils.deriveColor(is.getColor(), 0.5f));
        checkInnerShadow("convert[2] ", is, (InnerShadow)result);

        DropShadow ds = getDropShadow();
        value = getDropShadowValue(ds, false);
        result = value.convert(font);
        checkDropShadow("convert[3] ", ds, (DropShadow)result);

        // Test with derived colors
        value = getDropShadowValue(ds, true);
        result = value.convert(font);
        // derived color is 50% of is.getColor()
        ds.setColor(com.sun.javafx.Utils.deriveColor(ds.getColor(), 0.5f));
        checkDropShadow("convert[4] ", ds, (DropShadow)result);

    }

}
