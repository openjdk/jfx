package com.sun.javafx.scene.layout.region;

import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.text.Font;
import com.sun.javafx.css.ParsedValue;
import com.sun.javafx.css.StyleConverterImpl;

/**
 * User: richardbair
 * Date: 8/9/12
 * Time: 4:53 PM
 */
/*
* border-style: <border-style> [, <border-style>]*
* where <border-style> = <dash-style> [phase(<number>)]? [centered | inside | outside]?
*                        [line-join [miter <number> | bevel | round]]?
*                        [line-cap [square | butt | round]]?
* where <dash-style> = none | solid | dotted | dashed | segments(<size>[, <size>]+) ]
*/
public final class LayeredBorderStyleConverter
        extends StyleConverterImpl<ParsedValue<ParsedValue<ParsedValue[],BorderStrokeStyle>[], BorderStrokeStyle[]>[], BorderStrokeStyle[][]> {

    /**
     * Convert layers of border style values to an array of BorderStyle[], where
     * each layer contains one BorderStyle element per border.
     */
    private static final LayeredBorderStyleConverter LAYERED_BORDER_STYLE_CONVERTER =
            new LayeredBorderStyleConverter();

    public static LayeredBorderStyleConverter getInstance() {
        return LAYERED_BORDER_STYLE_CONVERTER;
    }

    private LayeredBorderStyleConverter() {
        super();
    }

    @Override
    public BorderStrokeStyle[][]
    convert(ParsedValue<ParsedValue<ParsedValue<ParsedValue[], BorderStrokeStyle>[],BorderStrokeStyle[]>[], BorderStrokeStyle[][]> value, Font font) {

        ParsedValue<ParsedValue<ParsedValue[], BorderStrokeStyle>[],BorderStrokeStyle[]>[] layers = value.getValue();
        BorderStrokeStyle[][] styles = new BorderStrokeStyle[layers.length][0];

        for (int layer=0; layer<layers.length; layer++) {
            styles[layer] = layers[layer].convert(font);
        }
        return styles;
    }

    @Override
    public String toString() {
        return "LayeredBorderStyleConverter";
    }
}
