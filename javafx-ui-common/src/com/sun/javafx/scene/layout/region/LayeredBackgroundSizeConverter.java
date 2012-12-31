package com.sun.javafx.scene.layout.region;

import com.sun.javafx.css.ParsedValue;
import com.sun.javafx.css.StyleConverterImpl;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.text.Font;

/**
 * This class appears to be an artifact of the implementation, such that we need
 * to pass values around as ParsedValues, and so we have a parsed value that just
 * holds an array of background sizes, and the converter just pulls those
 * background sizes back out.
 *
 * background-size      <bg-size> [ , <bg-size> ]*
 * <bg-size> = [ <size> | auto ]{1,2} | cover | contain
 * @see <a href="http://www.w3.org/TR/css3-background/#the-background-size">background-size</a>
 */
public final class LayeredBackgroundSizeConverter extends StyleConverterImpl<ParsedValue<ParsedValue[], BackgroundSize>[], BackgroundSize[]> {
    private static final LayeredBackgroundSizeConverter LAYERED_BACKGROUND_SIZE_CONVERTER =
            new LayeredBackgroundSizeConverter();

    public static LayeredBackgroundSizeConverter getInstance() {
        return LAYERED_BACKGROUND_SIZE_CONVERTER;
    }

    private LayeredBackgroundSizeConverter() {
        super();
    }

    @Override
    public BackgroundSize[] convert(ParsedValue<ParsedValue<ParsedValue[], BackgroundSize>[], BackgroundSize[]> value, Font font) {
        ParsedValue<ParsedValue[], BackgroundSize>[] layers = value.getValue();
        BackgroundSize[] sizes = new BackgroundSize[layers.length];
        for (int l = 0; l < layers.length; l++) {
            sizes[l] = layers[l].convert(font);
        }
        return sizes;
    }
}
