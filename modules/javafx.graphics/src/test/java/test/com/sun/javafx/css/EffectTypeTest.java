/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.css;

import com.sun.javafx.css.ParsedValueImpl;
import javafx.css.ParsedValue;
import javafx.css.Size;
import javafx.css.SizeUnits;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.effect.InnerShadow;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import javafx.css.converter.EffectConverter;
import javafx.css.converter.DeriveColorConverter;


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
                new ParsedValueImpl<Color,Color>(is.getColor(),null),
                new ParsedValueImpl<Size,Size>(makeSize(0.5f),null)
            };
            colorVal = new ParsedValueImpl<>(values, DeriveColorConverter.getInstance());
        } else {
            colorVal = new ParsedValueImpl<>(is.getColor(),null);
        }

        BlurType blurType = is.getBlurType();

        ParsedValue[] vals = new ParsedValue[] {
            new ParsedValueImpl<BlurType,BlurType>(blurType, null),
            colorVal,
            new ParsedValueImpl<Size,Size>(radius, null),
            new ParsedValueImpl<Size,Size>(choke, null),
            new ParsedValueImpl<Size,Size>(offsetX, null),
            new ParsedValueImpl<Size,Size>(offsetY, null)
        };

        return new ParsedValueImpl<>(vals, EffectConverter.InnerShadowConverter.getInstance());
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
                new ParsedValueImpl<Color,Color>(ds.getColor(),null),
                new ParsedValueImpl<Size,Size>(makeSize(0.5f),null)
            };
            colorVal = new ParsedValueImpl<>(values, DeriveColorConverter.getInstance());
        } else {
            colorVal = new ParsedValueImpl<>(ds.getColor(),null);
        }

        BlurType blurType = ds.getBlurType();

        ParsedValue[] vals = new ParsedValue[] {
            new ParsedValueImpl<BlurType,BlurType>(blurType, null),
            colorVal,
            new ParsedValueImpl<Size,Size>(radius, null),
            new ParsedValueImpl<Size,Size>(spread, null),
            new ParsedValueImpl<Size,Size>(offsetX, null),
            new ParsedValueImpl<Size,Size>(offsetY, null)
        };

        return new ParsedValueImpl<>(vals, EffectConverter.DropShadowConverter.getInstance());
    }

    void checkColor(Color c1, Color c2, String msg) {
        assertEquals(c1.getRed(), c2.getRed(), 0.001, msg + ".red");
        assertEquals(c1.getBlue(), c2.getBlue(), 0.001, msg + ".blue");
        assertEquals(c1.getGreen(), c2.getGreen(), 0.001, msg + ".green");
        assertEquals(c1.getOpacity(), c2.getOpacity(), 0.001, msg + ".opacity");
    }

    void checkInnerShadow(String msg, InnerShadow o1, InnerShadow o2) {
        assertEquals(o1.getOffsetX(), o2.getOffsetX(), 0.001, msg + "innershadow.offsetX");
        assertEquals(o1.getOffsetY(), o2.getOffsetY(), 0.001, msg + "innershadow.offsety");
        assertEquals(o1.getChoke(), o2.getChoke(), 0.001, msg + "innershadow.choke");
        assertEquals(o1.getRadius(), o2.getRadius(), 0.001, msg + "innershadow.radius");
        checkColor(o1.getColor(), o2.getColor(), msg + "innershadow");
        assertEquals(o1.getBlurType(), o2.getBlurType(), msg + "innershadow.blurType");
    }

    void checkDropShadow(String msg, DropShadow o1, DropShadow o2) {
        assertEquals(o1.getOffsetX(), o2.getOffsetX(), 0.001, msg + "DropShadow.offsetX");
        assertEquals(o1.getOffsetY(), o2.getOffsetY(), 0.001, msg + "DropShadow.offsety");
        assertEquals(o1.getSpread(), o2.getSpread(), 0.001, msg + "DropShadow.spread");
        assertEquals(o1.getRadius(), o2.getRadius(), 0.001, msg + "DropShadow.radius");
        checkColor(o1.getColor(), o2.getColor(), msg + "DropShadow");
        assertEquals(o1.getBlurType(), o2.getBlurType(), msg + "DropShadow.blurType");
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
        is.setColor(com.sun.javafx.util.Utils.deriveColor(is.getColor(), 0.5f));
        checkInnerShadow("convert[2] ", is, (InnerShadow)result);

        DropShadow ds = getDropShadow();
        value = getDropShadowValue(ds, false);
        result = value.convert(font);
        checkDropShadow("convert[3] ", ds, (DropShadow)result);

        // Test with derived colors
        value = getDropShadowValue(ds, true);
        result = value.convert(font);
        // derived color is 50% of is.getColor()
        ds.setColor(com.sun.javafx.util.Utils.deriveColor(ds.getColor(), 0.5f));
        checkDropShadow("convert[4] ", ds, (DropShadow)result);

    }

}
