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

package test.javafx.css;

import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import com.sun.javafx.css.ParsedValueImpl;
import javafx.css.CssParser;
import javafx.css.CssParserShim;
import javafx.css.ParsedValue;
import javafx.css.Size;
import javafx.css.SizeUnits;
import javafx.css.Stylesheet;
import javafx.css.converter.PaintConverter;
import javafx.css.converter.StopConverter;
import org.junit.Test;
import static org.junit.Assert.assertEquals;


public class PaintTypeTest {

    public PaintTypeTest() {
    }

    final Stop[] stops = new Stop[] {
        new Stop(0.0f, Color.WHITE),
        new Stop(1.0f, Color.BLACK)
    };

    final Paint[] paints = new Paint[] {
        Color.rgb(0, 128, 255),
        new LinearGradient(0.0f, 0.0f, 1.0f, 1.0f, true, CycleMethod.NO_CYCLE, stops),
        new RadialGradient(225, 0.28, 1f, 1f, 5.0f, false, CycleMethod.NO_CYCLE, stops)
    };

    ParsedValue<?,Size> sizeVal(float value) {
        return new ParsedValueImpl<>(new Size(value*100, SizeUnits.PERCENT), null);
    }

    ParsedValue<ParsedValue[],Stop> stopValue(Stop stop) {
        ParsedValue<?,Size> offset = sizeVal((float)stop.getOffset());
        ParsedValue<Color,Color> color = new ParsedValueImpl<>(stop.getColor(), null);
        ParsedValue[] values = new ParsedValue[] { offset, color };
        return new ParsedValueImpl<>(values, StopConverter.getInstance());
    }

    ParsedValue<ParsedValue[],Paint> linearGradientValues(LinearGradient lg) {
        ParsedValue[] values = new ParsedValue[7];
        int v = 0;
        values[v++] = sizeVal((float) lg.getStartX());
        values[v++] = sizeVal((float) lg.getStartY());
        values[v++] = sizeVal((float) lg.getEndX());
        values[v++] = sizeVal((float) lg.getEndY());
        values[v++] = new ParsedValueImpl<CycleMethod,CycleMethod>(lg.getCycleMethod(),null);
        values[v++] = stopValue(stops[0]);
        values[v++] = stopValue(stops[1]);
        return new ParsedValueImpl<>(values, PaintConverter.LinearGradientConverter.getInstance());
    }

    ParsedValue<ParsedValue[],Paint> radialGradientValues(RadialGradient rg) {
        ParsedValue[] values = new ParsedValue[8];
        int v = 0;
        values[v++] = new ParsedValueImpl<Size,Size>(new Size(rg.getFocusAngle(), SizeUnits.PX), null);
        values[v++] = new ParsedValueImpl<Size,Size>(new Size(rg.getFocusDistance(), SizeUnits.PX), null);
        values[v++] = sizeVal((float) rg.getCenterX());
        values[v++] = sizeVal((float) rg.getCenterY());
        values[v++] = new ParsedValueImpl<Size,Size>(new Size(rg.getRadius(), SizeUnits.PX), null);
        values[v++] = new ParsedValueImpl<CycleMethod,CycleMethod>(rg.getCycleMethod(),null);
        values[v++] = stopValue(stops[0]);
        values[v++] = stopValue(stops[1]);
        return new ParsedValueImpl<>(values, PaintConverter.RadialGradientConverter.getInstance());
    }

    final ParsedValue[] paintValues = new ParsedValue[] {
        new ParsedValueImpl<Paint,Paint>(paints[0], null),
        linearGradientValues((LinearGradient)paints[1]),
        radialGradientValues((RadialGradient)paints[2])
    };

    /**
     * Paint is layered (one value per layer).  &lt;paint&gt; [, &lt;paint&gt;]*.
     * In the convert function, the ParsedValue is a ParsedValue for each layer.
     * That is, value.getValue() returns a ParsedValue[].
     */
    ParsedValue<ParsedValue<?,Paint>[],Paint[]> getValue(int nLayers) {
        ParsedValue<Paint,Paint>[] layers = new ParsedValue[nLayers];
        for (int l=0; l<nLayers; l++) {
            layers[l] = paintValues[l % paintValues.length];
        }
        return new ParsedValueImpl<>(layers, PaintConverter.SequenceConverter.getInstance());
    }
    /**
     * Test of convert method, of class PaintType.
     */
    @Test
    public void testConvert() {
        //System.out.println("convert");
        int nValues = paints.length;
        ParsedValue<ParsedValue<?,Paint>[],Paint[]> value = getValue(nValues);
        Font font = null;
        Paint[] result = value.convert(font);
        assertEquals(nValues, result.length);
        for(int r=0; r<result.length; r++) {
            Paint expResult = paints[r % paints.length];
            assertEquals(expResult, result[r]);
        }
    }

    String[] css;

    Paint[][] expResults;

    // values for top, right, bottom, left
    final String[][][] svals = new String[][][] {
        { {"#ff0000"} },
        { {"#ff0000"}, {"#00ff00"}, {"#0000ff"}, {"#ffffff"} }
    };

    public void setup() {
        css = new String[svals.length];
        expResults = new Paint[svals.length][];
        for (int i=0; i<svals.length; i++) {
            StringBuilder sbuf = new StringBuilder();
            expResults[i] = new Paint[svals[i].length];
            for (int j=0; j<svals[i].length; j++) {
                for (int k=0; k<svals[i][j].length; k++) {
                    expResults[i][j] = Color.web(svals[i][j][k]);
                    sbuf.append(svals[i][j][k]);
                    if (k+1 < svals[i][j].length) sbuf.append(' ');
                }

                if (j+1 < svals[i].length) sbuf.append(", ");
            }
            css[i] = sbuf.toString();
        }
    }

    @Test
    public void testPaintTypeWithCSS() {
        setup();

        for (int i=0; i<css.length; i++) {

            Stylesheet stylesheet =
                new CssParser().parse("* { -fx-border-color: " + css[i] + "; }");

            ParsedValue value = TypeTest.getValueFor(stylesheet, "-fx-border-color");

            Paint[][] paints = (Paint[][])value.convert(Font.getDefault());

            //assertEquals(expResults[i].length,paints.length);

            for(int j=0; j<paints.length; j++) {
                String msg = Integer.toString(i) + "." + Integer.toString(j);
                assertEquals(msg, expResults[i][j], paints[j][0]);
            }
        }

    }

    @Test
    public void testParseRadialGradient() {

        // <radial-gradient> = radial-gradient(
        //        [ focus-angle <angle>, ]?
        //        [ focus-distance <percentage>, ]?
        //        [ center <point>, ]?
        //        radius <length>,
        //        [ [ repeat | reflect ] ,]?
        //        <color-stop>[, <color-stop>]+ )
        ParsedValue value = new CssParserShim().parseExpr("-fx-background-color",
                "radial-gradient(focus-angle 90deg, focus-distance 50%, radius 50, red, green, blue)");
        RadialGradient result = (RadialGradient)((Paint[])value.convert(null))[0];
        RadialGradient expected = new RadialGradient(90, .5, 0, 0, 50,
                false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.RED),
                new Stop(.5, Color.GREEN),
                new Stop(1.0,Color.BLUE));
        assertEquals(expected,result);

        value = new CssParserShim().parseExpr("-fx-background-color",
                "radial-gradient(focus-angle 1.5708rad, focus-distance 50%, radius 50, red, green, blue)");
        result = (RadialGradient)((Paint[])value.convert(null))[0];
        assertEquals(expected,result);

        value = new CssParserShim().parseExpr("-fx-background-color",
                "radial-gradient(center 0% 10%, radius 50%, reflect, red, green, blue)");
        result = (RadialGradient)((Paint[])value.convert(null))[0];
        expected = new RadialGradient(0, 0, 0, .1, .5,
                true, CycleMethod.REFLECT,
                new Stop(0, Color.RED),
                new Stop(.5, Color.GREEN),
                new Stop(1.0,Color.BLUE));
        assertEquals(expected,result);
    }

    @Test
    public void testParseLinearGradient() {

        // <linear-gradient> = linear-gradient(
        //        [ [from <point> to <point>] | [ to <side-or-corner> ] ] ,]? [ [ repeat | reflect ] ,]?
        //        <color-stop>[, <color-stop>]+
        // )
        //
        ParsedValue value = new CssParserShim().parseExpr("-fx-background-color",
                "linear-gradient(to top, red, green, blue)");
        LinearGradient result = (LinearGradient)((Paint[])value.convert(null))[0];
        LinearGradient expected = new LinearGradient(0, 1, 0, 0,
                true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.RED),
                new Stop(.5, Color.GREEN),
                new Stop(1.0, Color.BLUE));
        assertEquals(expected,result);

        value = new CssParserShim().parseExpr("-fx-background-color",
                "linear-gradient(to bottom, red, green, blue)");
        result = (LinearGradient)((Paint[])value.convert(null))[0];
        expected = new LinearGradient(0, 0, 0, 1,
                true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.RED),
                new Stop(.5, Color.GREEN),
                new Stop(1.0, Color.BLUE));
        assertEquals(expected,result);

        value = new CssParserShim().parseExpr("-fx-background-color",
                "linear-gradient(to left, red, green, blue)");
        result = (LinearGradient)((Paint[])value.convert(null))[0];
        expected = new LinearGradient(1, 0, 0, 0,
                true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.RED),
                new Stop(.5, Color.GREEN),
                new Stop(1.0, Color.BLUE));
        assertEquals(expected,result);

        value = new CssParserShim().parseExpr("-fx-background-color",
                "linear-gradient(to right, red, green, blue)");
        result = (LinearGradient)((Paint[])value.convert(null))[0];
        expected = new LinearGradient(0, 0, 1, 0,
                true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.RED),
                new Stop(.5, Color.GREEN),
                new Stop(1.0, Color.BLUE));
        assertEquals(expected,result);

        value = new CssParserShim().parseExpr("-fx-background-color",
                "linear-gradient(to bottom left, red, green, blue)");
        result = (LinearGradient)((Paint[])value.convert(null))[0];
        expected = new LinearGradient(1, 0, 0, 1,
                true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.RED),
                new Stop(.5, Color.GREEN),
                new Stop(1.0, Color.BLUE));
        assertEquals(expected,result);

        value = new CssParserShim().parseExpr("-fx-background-color",
                "linear-gradient(to bottom right, red, green, blue)");
        result = (LinearGradient)((Paint[])value.convert(null))[0];
        expected = new LinearGradient(0, 0, 1, 1,
                true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.RED),
                new Stop(.5, Color.GREEN),
                new Stop(1.0, Color.BLUE));
        assertEquals(expected,result);

        value = new CssParserShim().parseExpr("-fx-background-color",
                "linear-gradient(to top left, red, green, blue)");
        result = (LinearGradient)((Paint[])value.convert(null))[0];
        expected = new LinearGradient(1, 1, 0, 0,
                true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.RED),
                new Stop(.5, Color.GREEN),
                new Stop(1.0, Color.BLUE));
        assertEquals(expected,result);

        value = new CssParserShim().parseExpr("-fx-background-color",
                "linear-gradient(to top right, red, green, blue)");
        result = (LinearGradient)((Paint[])value.convert(null))[0];
        expected = new LinearGradient(0, 1, 1, 0,
                true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.RED),
                new Stop(.5, Color.GREEN),
                new Stop(1.0, Color.BLUE));
        assertEquals(expected,result);

        value = new CssParserShim().parseExpr("-fx-background-color",
                "linear-gradient(from 10% 10% to 90% 90%, reflect, red, green, blue)");
        result = (LinearGradient)((Paint[])value.convert(null))[0];
        expected = new LinearGradient(.1, .1, .9, .9,
                true, CycleMethod.REFLECT,
                new Stop(0, Color.RED),
                new Stop(.5, Color.GREEN),
                new Stop(1.0, Color.BLUE));
        assertEquals(expected,result);
  }

}
