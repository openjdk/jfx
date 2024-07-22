/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.scene.layout;

import org.junit.Test;

public class RegionTest {

    public RegionTest() {
    }

    @Test public void dummy() { }
//    CSSProperty getCssMetaDataByName(String name, List<CSSProperty> keys) {
//        CSSProperty keyForName = null;
//        for (CSSProperty k : keys) {
//            if (k.getProperty().equals(name)) {
//                keyForName = k;
//                break;
//            }
//        }
//        assertNotNull(keyForName);
//        return keyForName;
//    }
//
//    ParsedValue getValueFor(Stylesheet stylesheet, String property ) {
//        for (Rule rule : stylesheet.getRules()) {
//            for (Declaration decl : rule.getDeclarations()) {
//                if (property.equals(decl.getProperty())) {
//                    return decl.getParsedValue();
//                }
//            }
//        }
//        fail("getValueFor " + property);
//        return null;
//    }
//
//     @Test
//     public void testConvertCSSPropertysToBackgroundFills() {
//
//         Paint[] expectedColors = new Paint[] {
//            Color.web("#ff0000"),
//            Color.web("#00ff00"),
//            Color.web("#0000ff")
//         };
//
//        Insets[] expectedInsets = {
//            new Insets(-1,-1,-1,-1),
//            new Insets(0,0,0,0),
//            new Insets(1,1,1,1),
//        };
//
//        Insets[] expectedRadii = {
//            new Insets(-1,-1,-1,-1),
//            new Insets(0,0,0,0),
//            new Insets(1,1,1,1),
//        };
//
//
//        Stylesheet stylesheet = new CssParser().parse(
//                    "* { " +
//                        "-fx-background-color: #ff0000, #00ff00, #0000ff;" +
//                        "-fx-background-radius: -1, 0, 1; " +
//                        "-fx-background-insets: -1, 0, 1; " +
//                    "}");
//
//        Map<CSSProperty,Object> keyValues = new HashMap<CSSProperty,Object>();
//
//        ParsedValue cssColorValue = getValueFor(stylesheet, "-fx-background-color");
//        CSSProperty cssColorProperty = getCssMetaDataByName("-fx-background-color", BackgroundFill.getClassCssMetaData());
//        Object value = cssColorProperty.getConverter().convert(cssColorValue,Font.getDefault());
//        assertTrue(value instanceof Paint[]);
//        Paint[] cssPaints = (Paint[])value;
//        keyValues.put(cssColorProperty, cssPaints);
//
//        ParsedValue cssRadiusValue = getValueFor(stylesheet, "-fx-background-radius");
//        CSSProperty cssRadiusProperty = getCssMetaDataByName("-fx-background-radius", BackgroundFill.getClassCssMetaData());
//        value = cssRadiusProperty.getConverter().convert(cssRadiusValue,Font.getDefault());
//        assertTrue(value instanceof Insets[]);
//        keyValues.put(cssRadiusProperty, (Insets[])value);
//
//        ParsedValue cssInsetsValue = getValueFor(stylesheet, "-fx-background-insets");
//        CSSProperty cssInsetsProperty = getCssMetaDataByName("-fx-background-insets", BackgroundFill.getClassCssMetaData());
//        value = cssInsetsProperty.getConverter().convert(cssInsetsValue,Font.getDefault());
//        assertTrue(value instanceof Insets[]);
//        keyValues.put(cssInsetsProperty, (Insets[])value);
//
//
//        List<BackgroundFill> fills = BackgroundFillConverter.BACKGROUND_FILLS_CONVERTER.convert(keyValues);
//
//        assertEquals(cssPaints.length, fills.size());
//
//        for (int f=0; f<fills.size(); f++) {
//            BackgroundFill cssFill = fills.get(f);
//
//            assertEquals(expectedColors[f], cssFill.getFill());
//            assertEquals(expectedInsets[f], cssFill.getOffsets());
//            assertEquals(expectedRadii[f].getTop(), cssFill.getTopLeftCornerRadius(), 0.01);
//            assertEquals(expectedRadii[f].getRight(), cssFill.getTopRightCornerRadius(), 0.01);
//            assertEquals(expectedRadii[f].getBottom(), cssFill.getBottomRightCornerRadius(), 0.01);
//            assertEquals(expectedRadii[f].getLeft(), cssFill.getBottomLeftCornerRadius(), 0.01);
//        }
//
//        try {
//            CSSProperty styleable = getCssMetaDataByName("-fx-background-fills", Region.getClassCssMetaData());
//            styleable.set(new Region(), fills);
//        } catch (Exception e) {
//            fail(e.toString());
//        }
//     }
//
//     @Test
//     public void testConvertCSSPropertysToStrokeBorders() {
//
//        Paint[][] expectedBorderColors = {
//            {Color.RED, Color.RED, Color.RED, Color.RED},
//            {Color.YELLOW, Color.BLUE, Color.GREEN, Color.RED},
//            {Color.web("#d0d0d0"), Color.web("#0d0d0d"), Color.web("#d0d0d0"), Color.web("#0d0d0d") }
//        };
//
//        Insets[] expectedBorderWidths = {
//            new Insets(1,2,3,4),
//            new Insets(1,2,3,4),
//            new Insets(1,2,3,4)
//        };
//
//        Insets[] expectedBorderRadii = {
//            new Insets(5,5,5,5),
//            new Insets(1,2,1,2),
//            new Insets(1,2,1,2)
//        };
//
//        Insets[] expectedBorderInsets = {
//            new Insets(-1,-1,-1,-1),
//            new Insets(0,0,0,0),
//            new Insets(1,1,1,1),
//        };
//
//        BorderStyle dashed = new BorderStyle(
//                StrokeType.CENTERED,
//                StrokeLineJoin.MITER,
//                StrokeLineCap.BUTT,
//                10.0,
//                0.0,
//                new double[] { 5.0, 3.0 }
//        );
//
//        BorderStyle dotted = new BorderStyle(
//                StrokeType.CENTERED,
//                StrokeLineJoin.MITER,
//                StrokeLineCap.BUTT,
//                10.0,
//                0.0,
//                new double[] { 1.0f, 3.0f }
//        );
//
//        BorderStyle[][] expectedBorderStyles = {
//            {dashed, dotted, BorderStyle.SOLID, BorderStyle.NONE},
//            {dashed, dotted, BorderStyle.SOLID, BorderStyle.NONE},
//            {dashed, dotted, BorderStyle.SOLID, BorderStyle.NONE},
//        };
//
//        Stylesheet stylesheet = new CssParser().parse(
//                    "* { " +
//                        "-fx-border-color: red, " +
//                            "yellow blue green red," +
//                            "#d0d0d0 #0d0d0d;" +
//                        "-fx-border-width: 1 2 3 4; " +
//                        "-fx-border-radius: 5, 1 2; " +
//                        "-fx-border-insets: -1, 0, 1;" +
//                        "-fx-border-style: dashed dotted solid none;" +
//                    "}");
//
//        Map<CSSProperty,Object> keyValues = new HashMap<CSSProperty,Object>();
//        ParsedValue cssColorValue = getValueFor(stylesheet, "-fx-border-color");
//        CSSProperty cssColorProperty = getCssMetaDataByName("-fx-border-color", StrokeBorder.getClassCssMetaData());
//        Object value = cssColorProperty.getConverter().convert(cssColorValue,Font.getDefault());
//        assertTrue(value instanceof Paint[][]);
//        Paint[][] cssPaints = (Paint[][])value;
//        keyValues.put(cssColorProperty, cssPaints);
//
//        ParsedValue cssInsetsValue = getValueFor(stylesheet, "-fx-border-insets");
//        CSSProperty cssInsetsProperty = getCssMetaDataByName("-fx-border-insets", StrokeBorder.getClassCssMetaData());
//        value = cssInsetsProperty.getConverter().convert(cssInsetsValue,Font.getDefault());
//        assertTrue(value instanceof Insets[]);
//        Insets[] cssInsets = (Insets[])value;
//        keyValues.put(cssInsetsProperty, cssInsets);
//
//        ParsedValue cssRadiusValue = getValueFor(stylesheet, "-fx-border-radius");
//        CSSProperty cssRadiusProperty = getCssMetaDataByName("-fx-border-radius", StrokeBorder.getClassCssMetaData());
//        value = cssRadiusProperty.getConverter().convert(cssRadiusValue,Font.getDefault());
//        assertTrue(value instanceof Margins[]);
//        Margins[] cssRadii = (Margins[])value;
//        keyValues.put(cssRadiusProperty, cssRadii);
//
//        ParsedValue cssWidthValue = getValueFor(stylesheet, "-fx-border-width");
//        CSSProperty cssWidthProperty = getCssMetaDataByName("-fx-border-width", StrokeBorder.getClassCssMetaData());
//        value = cssWidthProperty.getConverter().convert(cssWidthValue,Font.getDefault());
//        assertTrue(value instanceof Margins[]);
//        Margins[] cssWidth = (Margins[])value;
//        keyValues.put(cssWidthProperty, cssWidth);
//
//        ParsedValue cssStyleValue = getValueFor(stylesheet, "-fx-border-style");
//        CSSProperty cssStyleProperty = getCssMetaDataByName("-fx-border-style", StrokeBorder.getClassCssMetaData());
//        value = cssStyleProperty.getConverter().convert(cssStyleValue,Font.getDefault());
//        assertTrue(value instanceof BorderStyle[][]);
//        BorderStyle[][] cssStyle = (BorderStyle[][])value;
//        keyValues.put(cssStyleProperty, cssStyle);
//
//
//        List<StrokeBorder> strokeBorders = StrokeBorderConverter.getInstance().convert(keyValues);
//
//        assertEquals(cssPaints.length, strokeBorders.size());
//
//        for (int f=0; f<strokeBorders.size(); f++) {
//            StrokeBorder border = strokeBorders.get(f);
//            assertEquals(expectedBorderColors[f][0], border.getTopFill());
//            assertEquals(expectedBorderColors[f][1], border.getRightFill());
//            assertEquals(expectedBorderColors[f][2], border.getBottomFill());
//            assertEquals(expectedBorderColors[f][3], border.getLeftFill());
//            assertEquals(expectedBorderInsets[f], border.getOffsets());
//            assertEquals(expectedBorderWidths[f].getTop(), border.getTopWidth(), 0.01);
//            assertEquals(expectedBorderWidths[f].getRight(), border.getRightWidth(), 0.01);
//            assertEquals(expectedBorderWidths[f].getBottom(), border.getBottomWidth(), 0.01);
//            assertEquals(expectedBorderWidths[f].getLeft(), border.getLeftWidth(), 0.01);
//            assertEquals(expectedBorderRadii[f].getTop(), border.getTopLeftCornerRadius(), 0.01);
//            assertEquals(expectedBorderRadii[f].getRight(), border.getTopRightCornerRadius(), 0.01);
//            assertEquals(expectedBorderRadii[f].getBottom(), border.getBottomRightCornerRadius(), 0.01);
//            assertEquals(expectedBorderRadii[f].getLeft(), border.getBottomLeftCornerRadius(), 0.01);
//            assertEquals(expectedBorderStyles[f][0], border.getTopStyle());
//            assertEquals(expectedBorderStyles[f][1], border.getRightStyle());
//            assertEquals(expectedBorderStyles[f][2], border.getBottomStyle());
//            assertEquals(expectedBorderStyles[f][3], border.getLeftStyle());
//        }
//
//        try {
//            CSSProperty styleable = getCssMetaDataByName("-fx-stroke-borders",Region.getClassCssMetaData());
//            styleable.set(new Region(), strokeBorders);
//        } catch (Exception e) {
//            fail(e.toString());
//        }
//     }
//
//     @Test
//     public void testConvertStyleablePropertiesToBackgroundImages() {
//
//         String[] expectedUrls = new String[] {
//            "http://sipi.usc.edu/database/misc/4.2.04.tiff",
//            Region.class.getResource("../image/doc-files/imageview.png").toExternalForm()
//         };
//
//        RepeatStruct[] expectedRepeats = {
//            new RepeatStruct(BackgroundRepeat.REPEAT,BackgroundRepeat.NO_REPEAT),
//            new RepeatStruct(BackgroundRepeat.NO_REPEAT,BackgroundRepeat.NO_REPEAT),
//        };
//
//        BackgroundPosition[] expectedPositions = {
//            new BackgroundPosition(0.5f,0.3f,0f,0f,true, true),
//            new BackgroundPosition(0f,0f,0.1f,0.2f,true, true),
//        };
//
//        BackgroundSize[] expectedSizes = {
//            BackgroundSize.AUTO_SIZE,
//            BackgroundSize.COVER,
//        };
//
//        Stylesheet stylesheet = new CssParser().parse(
//                    "* { " +
//                        "-fx-background-image: url(\"http://sipi.usc.edu/database/misc/4.2.04.tiff\"), url(\"javafx/scene/image/doc-files/imageview.png\");" +
//                        "-fx-background-repeat: repeat no-repeat, no-repeat;" +
//                        "-fx-background-position: right 30% center, left 20% bottom 10%; " +
//                        "-fx-background-size: AUTO, cover; " +
//                    "}");
//
//        Map<CSSProperty,Object> keyValues = new HashMap<CSSProperty,Object>();
//
//        ParsedValue cssBackgroundImageValue = getValueFor(stylesheet, "-fx-background-image");
//        CSSProperty cssBackgroundImageProperty = getCssMetaDataByName("-fx-background-image", BackgroundImage.getClassCssMetaData());
//        Object value = cssBackgroundImageProperty.getConverter().convert(cssBackgroundImageValue,Font.getDefault());
//        assertTrue(value instanceof String[]);
//        String[] cssBackgroundImages = (String[])value;
//        keyValues.put(cssBackgroundImageProperty, cssBackgroundImages);
//
//        ParsedValue cssBackgroundRepeatValue = getValueFor(stylesheet, "-fx-background-repeat");
//        CSSProperty cssBackgroundRepeatProperty = getCssMetaDataByName("-fx-background-repeat", BackgroundImage.getClassCssMetaData());
//        value = cssBackgroundRepeatProperty.getConverter().convert(cssBackgroundRepeatValue,Font.getDefault());
//        assertTrue(value instanceof RepeatStruct[]);
//        RepeatStruct[] cssBackgroundRepeats = (RepeatStruct[])value;
//        keyValues.put(cssBackgroundRepeatProperty, cssBackgroundRepeats);
//
//        ParsedValue cssBackgroundPositionValue = getValueFor(stylesheet, "-fx-background-position");
//        CSSProperty cssBackgroundPositionProperty = getCssMetaDataByName("-fx-background-position", BackgroundImage.getClassCssMetaData());
//        value = cssBackgroundPositionProperty.getConverter().convert(cssBackgroundPositionValue,Font.getDefault());
//        assertTrue(value instanceof BackgroundPosition[]);
//        BackgroundPosition[] cssBackgroundPositions = (BackgroundPosition[])value;
//        keyValues.put(cssBackgroundPositionProperty, cssBackgroundPositions);
//
//        ParsedValue cssBackgroundSizeValue = getValueFor(stylesheet, "-fx-background-size");
//        CSSProperty cssBackgroundSizeProperty = getCssMetaDataByName("-fx-background-size", BackgroundImage.getClassCssMetaData());
//        value = cssBackgroundSizeProperty.getConverter().convert(cssBackgroundSizeValue,Font.getDefault());
//        assertTrue(value instanceof BackgroundSize[]);
//        BackgroundSize[] cssBackgroundSizes = (BackgroundSize[])value;
//        keyValues.put(cssBackgroundSizeProperty, cssBackgroundSizes);
//
//        List<BackgroundImage> images = BackgroundImageConverter.getInstance().convert(keyValues);
//
//        assertEquals(cssBackgroundImages.length, images.size());
//
//        for (int i=0; i<images.size(); i++) {
//            BackgroundImage image = images.get(i);
//--             TODO: fix this - expected is file:/... actual is jar:file:/...
//--            assertEquals(expectedUrls[i], image.getImage().getUrl());
//            assertEquals(expectedRepeats[i].getRepeatX(), image.getRepeatX());
//            assertEquals(expectedRepeats[i].getRepeatY(), image.getRepeatY());
//            assertEquals(expectedPositions[i].getTop(), image.getTop(), 0.01);
//            assertEquals(expectedPositions[i].getRight(), image.getRight(), 0.01);
//            assertEquals(expectedPositions[i].getBottom(), image.getBottom(), 0.01);
//            assertEquals(expectedPositions[i].getLeft(), image.getLeft(), 0.01);
//            assertEquals(expectedSizes[i].getWidth(), image.getWidth(), 0.01);
//            assertEquals(expectedSizes[i].getHeight(), image.getHeight(), 0.01);
//            assertEquals(expectedSizes[i].isCover(), image.isCover());
//            assertEquals(expectedSizes[i].isContain(), image.isContain());
//        }
//
//        try {
//            CSSProperty prop = getCssMetaDataByName("-fx-background-images",Region.getClassCssMetaData());
//            prop.set(new Region(), images);
//        } catch (Exception e) {
//            fail(e.toString());
//        }
//     }
//
//     @Test
//     public void testConvertCSSPropertysToImageBorder() {
//
//--        URI CODEBASE = null;
//--        try {
//--            CODEBASE = URI.create(System.getProperty("javafx.application.codebase"));
//--                //System.out.println("CODEBASE: " + cb);
//--        } catch (Exception e) {
//--            try {
//--                CODEBASE = URI.create(System.getProperty("user.dir"));
//--            } catch (Exception x) {
//--                fail("could not set CODEBASE: " + x.toString());
//--            }
//--        }
//
//         String[] expectedUrls = new String[] {
//            "http://sipi.usc.edu/database/misc/4.2.04.tiff",
//            "http://sipi.usc.edu/database/misc/4.2.03.tiff",
//--            CODEBASE.resolve("scenic-view.png").toString()
//         };
//
//        BorderImageRepeat[] expectedRepeats = {
//            new BorderImageRepeat(BackgroundRepeat.REPEAT,BackgroundRepeat.NO_REPEAT),
//            new BorderImageRepeat(BackgroundRepeat.NO_REPEAT,BackgroundRepeat.NO_REPEAT)
//        };
//
//        BorderImageSlices[] expectedSlices = {
//            new BorderImageSlices(0.3f, 0.2f, 0.3f,   0.2f,   true, true),
//            new BorderImageSlices(0f,   0f,   10f, 10f, false, false)
//        };
//
//        Margins[] expectedWidths = {
//            new Margins(.05f, .05f, .05f, .05f, true),
//            new Margins(10f, 10f, 10f, 10f, false)
//        };
//
//        Insets[] expectedInsets = {
//            new Insets(-1f, -1f, -1f, -1f),
//            new Insets(1f, 1f, 1f, 1f)
//        };
//
//        Stylesheet stylesheet = new CssParser().parse(
//                    "* { " +
//--                        "-fx-border-image-source: url(\"http://sipi.usc.edu/database/misc/4.2.04.tiff\"), url(\"scenic-view.png\");" +
//                        "-fx-border-image-source: url(\"http://sipi.usc.edu/database/misc/4.2.04.tiff\"), url(\"http://sipi.usc.edu/database/misc/4.2.03.tiff\");" +
//                        "-fx-border-image-repeat: repeat no-repeat, no-repeat;" +
//                        "-fx-border-image-slice: 30% 20% fill, 0 0 10 10; " +
//                        "-fx-border-image-width: 5%, 10; " +
//                        "-fx-border-image-insets: -1, 1; " +
//                    "}");
//
//        Map<CSSProperty,Object> keyValues = new HashMap<CSSProperty,Object>();
//
//        ParsedValue cssImageBorderValue = getValueFor(stylesheet, "-fx-border-image-source");
//        CSSProperty cssImageBorderProperty = getCssMetaDataByName("-fx-border-image-source", BorderImage.getClassCssMetaData());
//        Object value = cssImageBorderProperty.getConverter().convert(cssImageBorderValue,Font.getDefault());
//        assertTrue(value instanceof String[]);
//        String[] cssImageBorders = (String[])value;
//        keyValues.put(cssImageBorderProperty,cssImageBorders);
//
//        ParsedValue cssImageRepeatValue = getValueFor(stylesheet, "-fx-border-image-repeat");
//        CSSProperty cssImageRepeatProperty = getCssMetaDataByName("-fx-border-image-repeat", BorderImage.getClassCssMetaData());
//        value = cssImageRepeatProperty.getConverter().convert(cssImageRepeatValue,Font.getDefault());
//        assertTrue(value instanceof BorderImageRepeat[]);
//        BorderImageRepeat[] cssBorderImageRepeats = (BorderImageRepeat[])value;
//        keyValues.put(cssImageRepeatProperty, cssBorderImageRepeats);
//
//        ParsedValue cssSliceValue = getValueFor(stylesheet, "-fx-border-image-slice");
//        CSSProperty cssSliceProperty = getCssMetaDataByName("-fx-border-image-slice", BorderImage.getClassCssMetaData());
//        value = cssSliceProperty.getConverter().convert(cssSliceValue,Font.getDefault());
//        assertTrue(value instanceof BorderImageSlices[]);
//        BorderImageSlices[] cssBorderImageSlices = (BorderImageSlices[])value;
//        keyValues.put(cssSliceProperty, cssBorderImageSlices);
//
//        ParsedValue cssWidthValue = getValueFor(stylesheet, "-fx-border-image-width");
//        CSSProperty cssWidthProperty = getCssMetaDataByName("-fx-border-image-width", BorderImage.getClassCssMetaData());
//        value = cssWidthProperty.getConverter().convert(cssWidthValue,Font.getDefault());
//        assertTrue(value instanceof Margins[]);
//        Margins[] cssBorderImageWidths = (Margins[])value;
//        keyValues.put(cssWidthProperty, cssBorderImageWidths);
//
//        ParsedValue cssInsetsValue = getValueFor(stylesheet, "-fx-border-image-insets");
//        CSSProperty cssInsetsProperty = getCssMetaDataByName("-fx-border-image-insets", BorderImage.getClassCssMetaData());
//        value = cssInsetsProperty.getConverter().convert(cssInsetsValue,Font.getDefault());
//        assertTrue(value instanceof Insets[]);
//        Insets[] cssBorderImageInsets = (Insets[])value;
//        keyValues.put(cssInsetsProperty, cssBorderImageInsets);
//
//        List<BorderImage> images = BorderImageConverter.getInstance().convert(keyValues);
//
//        assertEquals(cssImageBorders.length, images.size());
//
//        for (int i=0; i<images.size(); i++) {
//            BorderImage image = images.get(i);
//            assertEquals(expectedUrls[i], image.getImage().getUrl());
//            assertEquals(expectedRepeats[i].getRepeatX(), image.getRepeatX());
//            assertEquals(expectedRepeats[i].getRepeatY(), image.getRepeatY());
//            assertEquals(expectedSlices[i].getTop(), image.getTopSlice(),0.01);
//            assertEquals(expectedSlices[i].getRight(), image.getRightSlice(),0.01);
//            assertEquals(expectedSlices[i].getBottom(), image.getBottomSlice(),0.01);
//            assertEquals(expectedSlices[i].getLeft(), image.getLeftSlice(),0.01);
//            assertEquals(expectedWidths[i].getTop(), image.getTopWidth(),0.01);
//            assertEquals(expectedWidths[i].getRight(), image.getRightWidth(),0.01);
//            assertEquals(expectedWidths[i].getBottom(), image.getBottomWidth(),0.01);
//            assertEquals(expectedWidths[i].getLeft(), image.getLeftWidth(),0.01);
//            assertEquals(expectedInsets[i].getTop(), image.getOffsets().getTop(),0.01);
//            assertEquals(expectedInsets[i].getRight(), image.getOffsets().getRight(),0.01);
//            assertEquals(expectedInsets[i].getBottom(), image.getOffsets().getBottom(),0.01);
//            assertEquals(expectedInsets[i].getLeft(), image.getOffsets().getLeft(),0.01);
//        }
//
//        try {
//            CSSProperty styleable = getCssMetaDataByName("-fx-image-borders", Region.getClassCssMetaData());
//            styleable.set(new Region(), images);
//        } catch (Exception e) {
//            fail(e.toString());
//        }
//     }

}
