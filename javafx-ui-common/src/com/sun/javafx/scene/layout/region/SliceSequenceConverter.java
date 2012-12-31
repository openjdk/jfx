package com.sun.javafx.scene.layout.region;

import javafx.scene.text.Font;
import com.sun.javafx.css.ParsedValue;
import com.sun.javafx.css.StyleConverterImpl;

/**
 * [<size> | <size> <size> <size> <size>] <fill>? [ , [ <size> | <size> <size> <size> <size>] <fill>? ]*
 */
public final class SliceSequenceConverter extends StyleConverterImpl<ParsedValue<ParsedValue[], BorderImageSlices>[], BorderImageSlices[]> {
    private static final SliceSequenceConverter BORDER_IMAGE_SLICE_SEQUENCE_CONVERTER =
            new SliceSequenceConverter();

    public static SliceSequenceConverter getInstance() {
        return BORDER_IMAGE_SLICE_SEQUENCE_CONVERTER;
    }

    @Override
    public BorderImageSlices[] convert(ParsedValue<ParsedValue<ParsedValue[], BorderImageSlices>[], BorderImageSlices[]> value, Font font) {
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
        ParsedValue<ParsedValue[], BorderImageSlices>[] layers = value.getValue();
        BorderImageSlices[] borderImageSlices = new BorderImageSlices[layers.length];
        for (int l = 0; l < layers.length; l++) {
            borderImageSlices[l] = BorderImageSliceConverter.getInstance().convert(layers[l], font);
        }
        return borderImageSlices;
    }

    @Override
    public String toString() {
        return "BorderImageSliceSequenceConverter";
    }
}
