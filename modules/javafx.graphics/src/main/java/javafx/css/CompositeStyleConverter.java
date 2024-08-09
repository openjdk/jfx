package javafx.css;

import java.util.Map;

/**
 * Document this TODO
 *
 * @param <T> the result type
 */
public interface CompositeStyleConverter<T> {

    /**
     * Converts a map of CSS values to the target type.
     *
     * @param values the constituent values
     * @return the converted object
     */
    T convert(Map<CssMetaData<? extends Styleable, ?>, Object> values);

    /**
     * Converts an object back to a map of its constituent values (deconstruction).
     * The returned map can be passed into {@link #convert(Map)} to reconstruct the object.
     *
     * @param value the object
     * @return a {@code Map} of the constituent values
     */
    Map<CssMetaData<? extends Styleable, ?>, Object> convertBack(T value);
}
