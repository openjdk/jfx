package com.sun.javafx.scene.layout.region;

import com.sun.javafx.css.ParsedValue;
import com.sun.javafx.css.StyleConverterImpl;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;

/*
 * border-color	<paint> | <paint> <paint> <paint> <paint> [ , [<paint> | <paint> <paint> <paint> <paint>] ]*	null
 */
public final class LayeredBorderPaintConverter extends StyleConverterImpl<ParsedValue<ParsedValue<?,Paint>[],Paint[]>[], Paint[][]> {
    /**
     * Convert layers of border paint values to an array of Paint[], where
     * each layer contains one Paint element per border.
     */
    private static final LayeredBorderPaintConverter LAYERED_BORDER_PAINT_CONVERTER =
            new LayeredBorderPaintConverter();

    public static LayeredBorderPaintConverter getInstance() {
        return LAYERED_BORDER_PAINT_CONVERTER;
    }

    private LayeredBorderPaintConverter() {
        super();
    }

    @Override
    public Paint[][] convert(ParsedValue<ParsedValue<ParsedValue<?,Paint>[],Paint[]>[], Paint[][]> value, Font font) {
        ParsedValue<ParsedValue<?,Paint>[],Paint[]>[] layers = value.getValue();
        Paint[][] paints = new Paint[layers.length][0];
        for(int layer=0; layer<layers.length; layer++) {
            paints[layer] = StrokeBorderPaintConverter.getInstance().convert(layers[layer],font);
        }
        return paints;
    }

    @Override
    public String toString() {
        return "LayeredBorderPaintConverter";
    }
}

