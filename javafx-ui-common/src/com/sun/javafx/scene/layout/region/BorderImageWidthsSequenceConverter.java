package com.sun.javafx.scene.layout.region;

import javafx.css.ParsedValue;
import com.sun.javafx.css.StyleConverterImpl;
import com.sun.javafx.css.Size;
import javafx.scene.layout.BorderWidths;
import javafx.scene.text.Font;

/**
 * User: richardbair
 * Date: 8/10/12
 * Time: 8:27 PM
 */
public class BorderImageWidthsSequenceConverter extends StyleConverterImpl<ParsedValue<ParsedValue<?,Size>[], BorderWidths>[], BorderWidths[]> {
    private static final BorderImageWidthsSequenceConverter CONVERTER =
            new BorderImageWidthsSequenceConverter();

    public static BorderImageWidthsSequenceConverter getInstance() {
        return CONVERTER;
    }

    @Override
    public BorderWidths[] convert(ParsedValue<ParsedValue<ParsedValue<?,Size>[], BorderWidths>[], BorderWidths[]> value, Font font) {
        // For 'border-image-slice: 10% fill, 20% 30%', the value arg will be
        // ParsedValue { values: [
        //     ParsedValue { values: [ ParsedValue {parsed: 10%}, ParsedValue {parsed: fill}] } ,
        //     ParsedValue { values: [ ParsedValue {parsed: 20%}, ParsedValue {parsed: 30%}] }
        // ]}
        //
        // For 'border-image-slice: 10% fill', the value arg will be
        // ParsedValue { values: [ ParsedValue {parsed: 10%}, ParsedValue {parsed: fill}] }
        //
        // For 'border-image-slice: 10%', the value arg will be
        // ParsedValue {parsed: 10%}
        //
        // where the sizes are actually Size objects.
        //
        // If the value arg contains multiple layers, unwind the nested
        // values by one level.
        ParsedValue<ParsedValue<?,Size>[], BorderWidths>[] layers = value.getValue();
        BorderWidths[] widths = new BorderWidths[layers.length];
        for (int l = 0; l < layers.length; l++) {
            widths[l] = BorderImageWidthConverter.getInstance().convert(layers[l], font);
        }
        return widths;
    }

    @Override
    public String toString() {
        return "BorderImageWidthsSequenceConverter";
    }
}
